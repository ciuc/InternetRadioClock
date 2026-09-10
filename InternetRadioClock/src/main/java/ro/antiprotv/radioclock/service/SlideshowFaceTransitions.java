package ro.antiprotv.radioclock.service;

import android.graphics.RectF;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import com.flaviofaria.kenburnsview.Transition;
import com.flaviofaria.kenburnsview.TransitionGenerator;
import java.util.Random;

/**
 * Ken Burns movement that keeps the faces in the frame.
 *
 * <p>Cropping around the faces is only half the job when an effect is on: the effects show a part
 * of the picture and move it about, so a pan that wanders off, or a zoom that closes in on a
 * corner, would drop the heads the crop was placed to keep. These generators pick the same kind of
 * rectangles as the plain effects do, but only from the ones the faces fit inside.
 *
 * <p>Both expect the picture to have been cropped to the screen's shape already - which is what
 * {@link SlideshowFaceCrop} does whenever these are used - so the faces are certain to be somewhere
 * within the area the effect has to play with.
 */
public final class SlideshowFaceTransitions {

  private SlideshowFaceTransitions() {}

  /** How far in the closest rectangle may be; 0.75 matches the movement of the plain pan-zoom. */
  private static final float MIN_RECT_FACTOR = 0.75f;

  /** The pan and zoom, held to rectangles that still hold the faces. */
  public static class PanZoom implements TransitionGenerator {
    private final RectF faces;
    private final long duration;
    private final Interpolator interpolator = new AccelerateDecelerateInterpolator();
    private final Random random = new Random();
    private RectF lastDestination;
    private RectF lastBounds;

    /**
     * @param faces where the faces are, in fractions of the picture's width and height
     */
    public PanZoom(RectF faces, long duration) {
      this.faces = faces;
      this.duration = duration;
    }

    @Override
    public Transition generateNextTransition(RectF drawableBounds, RectF viewport) {
      // Carrying the last rectangle over makes one move continue into the next instead of
      // jumping - but only while it is still a rectangle of this picture.
      RectF source =
          lastDestination != null && drawableBounds.equals(lastBounds)
              ? lastDestination
              : facingRect(faces, drawableBounds, viewport, random);
      RectF destination = facingRect(faces, drawableBounds, viewport, random);
      lastDestination = destination;
      lastBounds = new RectF(drawableBounds);
      return new Transition(source, destination, duration, interpolator);
    }
  }

  /** The zoom out, starting from a close-up that has the faces in it rather than the middle. */
  public static class ZoomOut implements TransitionGenerator {
    private static final float START_SCALE = 1.2f;

    private final RectF faces;
    private final long duration;
    private final Interpolator interpolator = new AccelerateDecelerateInterpolator();

    public ZoomOut(RectF faces, long duration) {
      this.faces = faces;
      this.duration = duration;
    }

    @Override
    public Transition generateNextTransition(RectF drawableBounds, RectF viewport) {
      RectF end = largestViewportShapedRect(drawableBounds, viewport);
      RectF start = new RectF(end);
      start.inset(
          end.width() * (1 - 1 / START_SCALE) / 2f, end.height() * (1 - 1 / START_SCALE) / 2f);
      place(start, faces, drawableBounds);
      return new Transition(start, end, duration, interpolator);
    }
  }

  /** A rectangle of the viewport's shape, of a random size, positioned to hold the faces. */
  private static RectF facingRect(RectF faces, RectF drawableBounds, RectF viewport, Random random) {
    RectF rect = largestViewportShapedRect(drawableBounds, viewport);
    float factor = MIN_RECT_FACTOR + (1 - MIN_RECT_FACTOR) * random.nextFloat();
    rect.inset(rect.width() * (1 - factor) / 2f, rect.height() * (1 - factor) / 2f);
    place(rect, faces, drawableBounds, random);
    return rect;
  }

  /** The biggest rectangle of the viewport's shape that fits in the picture, sitting in its middle. */
  private static RectF largestViewportShapedRect(RectF drawableBounds, RectF viewport) {
    float viewportRatio =
        viewport.height() > 0 ? viewport.width() / viewport.height() : drawableRatio(drawableBounds);
    float width = drawableBounds.width();
    float height = drawableBounds.height();
    if (drawableRatio(drawableBounds) > viewportRatio) {
      width = height * viewportRatio;
    } else {
      height = width / viewportRatio;
    }
    float left = drawableBounds.left + (drawableBounds.width() - width) / 2f;
    float top = drawableBounds.top + (drawableBounds.height() - height) / 2f;
    return new RectF(left, top, left + width, top + height);
  }

  private static float drawableRatio(RectF drawableBounds) {
    return drawableBounds.height() > 0 ? drawableBounds.width() / drawableBounds.height() : 1f;
  }

  private static void place(RectF rect, RectF faces, RectF drawableBounds) {
    place(rect, faces, drawableBounds, null);
  }

  /**
   * Moves {@code rect} - keeping its size - so that the faces are inside it, staying within the
   * picture. With a {@code random} it lands anywhere that satisfies both, which is what gives the
   * pan somewhere to go; without one it sits in the middle of everywhere it could have gone.
   */
  private static void place(RectF rect, RectF faces, RectF drawableBounds, Random random) {
    float left =
        edge(
            drawableBounds.left + faces.left * drawableBounds.width(),
            drawableBounds.left + faces.right * drawableBounds.width(),
            rect.width(),
            drawableBounds.left,
            drawableBounds.right,
            random);
    float top =
        edge(
            drawableBounds.top + faces.top * drawableBounds.height(),
            drawableBounds.top + faces.bottom * drawableBounds.height(),
            rect.height(),
            drawableBounds.top,
            drawableBounds.bottom,
            random);
    rect.offsetTo(left, top);
  }

  /**
   * Where one edge of the window goes: far enough back to take in {@code to}, no further forward
   * than {@code from}, and never off the picture.
   */
  private static float edge(
      float from, float to, float size, float lowest, float highest, Random random) {
    float roomStart = lowest;
    float roomEnd = highest - size;
    float min = Math.max(roomStart, to - size);
    float max = Math.min(roomEnd, from);
    if (min > max) {
      // The faces are wider than the window - which happens with a group filling the picture.
      // Nothing can hold all of them, so hold the middle of them.
      return clamp((from + to - size) / 2f, roomStart, Math.max(roomStart, roomEnd));
    }
    return random == null ? (min + max) / 2f : min + random.nextFloat() * (max - min);
  }

  private static float clamp(float value, float min, float max) {
    return Math.max(min, Math.min(max, value));
  }
}
