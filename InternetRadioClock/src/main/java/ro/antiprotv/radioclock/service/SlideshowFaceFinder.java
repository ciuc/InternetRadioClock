package ro.antiprotv.radioclock.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.FutureTarget;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import timber.log.Timber;

/**
 * Works out which part of a picture the slideshow should show, for pictures that do not fit the
 * screen the way round they were taken.
 *
 * <p>A portrait photograph on a landscape screen is shown by cropping it to the screen's shape, and
 * cropping to the middle is what loses the faces: heads are near the top of a photograph, and the
 * middle of a tall picture is somewhere around the waist. So the faces are found first and the crop
 * is placed around them instead.
 *
 * <p>Only pictures whose orientation differs from the screen's go through this. One that is already
 * the right way round loses little to a centred crop, and finding faces is not free.
 *
 * <p>Detection uses {@link FaceDetector}, which is part of the platform: no model to ship, no
 * network, nothing leaves the device. It only finds faces looking more or less at the camera, which
 * is what the photographs this feature exists for tend to hold.
 */
public class SlideshowFaceFinder {

  /**
   * Faces are found on a copy no bigger than this along its longer side.
   *
   * <p>It has to be this big. {@link FaceDetector} needs a face's eyes to be something like twenty
   * pixels apart before it sees a face at all, and the pictures this feature exists for are the
   * tall ones: a 924x1907 photograph inside a 640 box is only 310 wide, which leaves everybody but
   * the person nearest the camera too small to find. At 1280 that same photograph is 620 wide and
   * the others come out. Detection is not cheap at this size, but it happens once per file, off
   * the main thread, and the answer is kept.
   */
  private static final int DETECTION_SIZE = 1280;

  private static final int MAX_FACES = 8;

  /**
   * Below this, a reported face is usually not one. Deliberately under {@link
   * FaceDetector.Face#CONFIDENCE_THRESHOLD}: missing somebody costs more here than taking in a
   * little extra picture around a wrong guess.
   */
  private static final float MIN_CONFIDENCE = 0.35f;

  /**
   * How much of a head to keep around the eyes, as multiples of the distance between them: the
   * detector reports where the eyes are, not where the head ends. Above them there is a forehead
   * and hair to keep, below them a chin.
   */
  private static final float HEAD_HALF_WIDTH = 1.5f;

  private static final float HEAD_ABOVE_EYES = 1.8f;
  private static final float HEAD_BELOW_EYES = 2.2f;

  /** A folder can hold more pictures than is worth remembering answers for. */
  private static final int MAX_REMEMBERED = 512;

  private final Context context;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  /** Decoding a picture and looking for faces in it is far too slow for the main thread. */
  private final ExecutorService detector = Executors.newSingleThreadExecutor();

  /**
   * What has already been worked out, keyed by URI. A slideshow comes round to the same files over
   * and over, so this turns the cost of finding the faces into a one-off per file.
   */
  private final Map<String, Detection> detections = new HashMap<>();

  public SlideshowFaceFinder(Context context) {
    this.context = context.getApplicationContext();
  }

  /** Told where the faces are, or {@code null} when this picture needs no help. */
  public interface Callback {
    void onResult(@Nullable FaceRegion region);
  }

  /**
   * Finds the faces in {@code uri} and says where the crop should go.
   *
   * <p>The callback always runs on the main thread, and runs straight away for a picture already
   * looked at.
   *
   * @param viewportRatio width over height of the area the picture will be shown in
   */
  public void find(final Uri uri, final float viewportRatio, final Callback callback) {
    Detection known = detections.get(uri.toString());
    if (known != null && (known.facesLookedFor || !needsFaces(known.sourceRatio, viewportRatio))) {
      callback.onResult(regionOf(known, viewportRatio));
      return;
    }
    try {
      detector.execute(
          () -> {
            final Detection found = detect(uri, viewportRatio);
            mainHandler.post(
                () -> {
                  remember(uri, found);
                  callback.onResult(regionOf(found, viewportRatio));
                });
          });
    } catch (Exception e) {
      // The slideshow is on its way out. Show the picture the plain way rather than not at all.
      Timber.d("Could not look for faces in %s: %s", uri, e.getMessage());
      callback.onResult(null);
    }
  }

  public void shutdown() {
    detector.shutdownNow();
    mainHandler.removeCallbacksAndMessages(null);
  }

  private void remember(Uri uri, @Nullable Detection detection) {
    if (detection == null) {
      return;
    }
    String key = uri.toString();
    if (detections.size() < MAX_REMEMBERED || detections.containsKey(key)) {
      detections.put(key, detection);
    }
  }

  @Nullable
  private Detection detect(Uri uri, float viewportRatio) {
    FutureTarget<Bitmap> target = null;
    Bitmap detectable = null;
    try {
      target =
          Glide.with(context)
              .asBitmap()
              .format(DecodeFormat.PREFER_RGB_565)
              // A hardware bitmap has no pixels this side of the GPU, and both the copy below and
              // the detector need to read them.
              .disallowHardwareConfig()
              .load(uri)
              .override(DETECTION_SIZE, DETECTION_SIZE)
              // Shrinks a big photograph to fit and leaves a small one alone. Blowing one up would
              // only cost time: it cannot put back detail the detector needs.
              .centerInside()
              .submit();
      Bitmap small = target.get();
      float sourceRatio = (float) small.getWidth() / small.getHeight();
      if (!needsFaces(sourceRatio, viewportRatio)) {
        // Nothing to do for this picture, but worth writing down so it is not decoded again.
        return new Detection(sourceRatio, false, null);
      }
      detectable = toDetectableBitmap(small);
      return new Detection(
          sourceRatio, true, detectable == null ? null : findFaces(detectable, sourceRatio));
    } catch (Exception e) {
      // A file that cannot be read here cannot be shown either; the display load will say so.
      Timber.d("Could not look for faces in %s: %s", uri, e.getMessage());
      return null;
    } finally {
      if (detectable != null) {
        detectable.recycle();
      }
      if (target != null) {
        // Glide hands the picture back to its pool from the main thread only.
        final FutureTarget<Bitmap> loaded = target;
        mainHandler.post(() -> Glide.with(context).clear(loaded));
      }
    }
  }

  /**
   * {@link FaceDetector} takes nothing but a 565 bitmap of even width, so give it one whatever
   * Glide decoded.
   */
  @Nullable
  private static Bitmap toDetectableBitmap(Bitmap source) {
    int width = source.getWidth() & ~1;
    int height = source.getHeight();
    if (width < 2 || height < 2) {
      return null;
    }
    Bitmap copy = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
    new Canvas(copy).drawBitmap(source, 0, 0, null);
    return copy;
  }

  /**
   * Every face found, as one box holding all of them, in fractions of the picture's width and
   * height. Fractions rather than pixels because detection runs on a small copy and the crop is
   * worked out on the full-size one.
   *
   * @return the box, or {@code null} when there is no face in the picture
   */
  @Nullable
  private static RectF findFaces(Bitmap bitmap, float sourceRatio) {
    final int width = bitmap.getWidth();
    final int height = bitmap.getHeight();
    FaceDetector.Face[] found = new FaceDetector.Face[MAX_FACES];
    int count;
    try {
      count = new FaceDetector(width, height, MAX_FACES).findFaces(bitmap, found);
    } catch (Exception | UnsatisfiedLinkError e) {
      // Not every device carries a working detector. Without one the picture is simply centred.
      Timber.d("Face detection failed: %s", e.getMessage());
      return null;
    }

    RectF faces = null;
    int accepted = 0;
    PointF eyes = new PointF();
    for (int i = 0; i < count; i++) {
      FaceDetector.Face face = found[i];
      if (face == null || face.confidence() < MIN_CONFIDENCE) {
        continue;
      }
      accepted++;
      face.getMidPoint(eyes);
      float apart = face.eyesDistance();
      RectF head =
          new RectF(
              (eyes.x - apart * HEAD_HALF_WIDTH) / width,
              (eyes.y - apart * HEAD_ABOVE_EYES) / height,
              (eyes.x + apart * HEAD_HALF_WIDTH) / width,
              (eyes.y + apart * HEAD_BELOW_EYES) / height);
      if (faces == null) {
        faces = head;
      } else {
        faces.union(head);
      }
    }
    if (faces == null) {
      // Worth saying out loud: a picture searched at this size and still coming back empty is what
      // a face too small to find looks like, and the size is the thing to change.
      Timber.d(
          "No face found in a %sx%s copy (%s below confidence), picture ratio %s",
          width, height, count, sourceRatio);
      return null;
    }
    faces.set(
        clamp(faces.left, 0f, 1f),
        clamp(faces.top, 0f, 1f),
        clamp(faces.right, 0f, 1f),
        clamp(faces.bottom, 0f, 1f));
    Timber.d(
        "Found %s of %s face(s) in a %sx%s copy, together at %s, picture ratio %s",
        accepted, count, width, height, faces, sourceRatio);
    return faces;
  }

  /**
   * Whether a picture is the other way round from the screen. A square one counts as not landscape,
   * so it gets the same help on a landscape screen, where it loses just as much off the top.
   */
  private static boolean needsFaces(float sourceRatio, float viewportRatio) {
    return (sourceRatio > 1f) != (viewportRatio > 1f);
  }

  @Nullable
  private static FaceRegion regionOf(@Nullable Detection detection, float viewportRatio) {
    if (detection == null
        || detection.faces == null
        || !needsFaces(detection.sourceRatio, viewportRatio)) {
      return null;
    }
    return new FaceRegion(detection.faces, detection.sourceRatio, viewportRatio);
  }

  private static float clamp(float value, float min, float max) {
    return Math.max(min, Math.min(max, value));
  }

  /** What was learnt about one file. */
  private static class Detection {
    /** The picture's width over its height, as decoded - so with any rotation already applied. */
    final float sourceRatio;

    /** False when the picture matched the screen and was never searched. */
    final boolean facesLookedFor;

    /** All the faces as one box in fractions of the picture, or null when there are none. */
    @Nullable final RectF faces;

    Detection(float sourceRatio, boolean facesLookedFor, @Nullable RectF faces) {
      this.sourceRatio = sourceRatio;
      this.facesLookedFor = facesLookedFor;
      this.faces = faces;
    }
  }

  /**
   * Where to crop one picture so its faces stay in the frame, and where those faces end up once it
   * has been cropped.
   */
  public static class FaceRegion {
    private final float focusX;
    private final float focusY;
    private final RectF faceWithinCrop;

    FaceRegion(RectF faces, float sourceRatio, float viewportRatio) {
      focusX = faces.centerX();
      focusY = faces.centerY();

      // The largest part of the picture that is the screen's shape, as fractions of the picture.
      float cropWidth = sourceRatio > viewportRatio ? viewportRatio / sourceRatio : 1f;
      float cropHeight = sourceRatio > viewportRatio ? 1f : sourceRatio / viewportRatio;
      // Centred on the faces, and pushed back inside the picture when they sit near an edge.
      float left = clamp(focusX - cropWidth / 2f, 0f, 1f - cropWidth);
      float top = clamp(focusY - cropHeight / 2f, 0f, 1f - cropHeight);

      faceWithinCrop =
          new RectF(
              clamp((faces.left - left) / cropWidth, 0f, 1f),
              clamp((faces.top - top) / cropHeight, 0f, 1f),
              clamp((faces.right - left) / cropWidth, 0f, 1f),
              clamp((faces.bottom - top) / cropHeight, 0f, 1f));
    }

    /** The middle of the faces, in fractions of the whole picture. */
    public float focusX() {
      return focusX;
    }

    public float focusY() {
      return focusY;
    }

    /** The faces in fractions of the cropped picture, for the pan and zoom to keep in view. */
    public RectF faceWithinCrop() {
      return new RectF(faceWithinCrop);
    }
  }
}
