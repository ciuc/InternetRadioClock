package ro.antiprotv.radioclock.service;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.flaviofaria.kenburnsview.KenBurnsView;
import com.flaviofaria.kenburnsview.Transition;
import com.flaviofaria.kenburnsview.TransitionGenerator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.activity.ClockActivity;
import ro.antiprotv.radioclock.service.profile.ProfileManager;
import timber.log.Timber;

public class SlideshowManager {
  private static SlideshowManager INSTANCE;
  private final SharedPreferences prefs;
  private final ClockActivity clockActivity;
  private final KenBurnsView kenBurnsView;
  private final ImageView slideshowSimpleView;
  private final List<Uri> imageUris = new ArrayList<>();
  private int currentSlideshowIndex = 0;
  /** Consecutive images Glide could not load; reset by the first one that does load. */
  private int unreadableStreak = 0;
  private final Handler slideShowhandler = new Handler();
  private final ButtonManager buttonManager;
  private final ProfileManager profileManager;
  private ImageView slideshowView;
  private int imageDuration;
  /** Listing a folder hits a content provider, which is too slow for the main thread. */
  private final ExecutorService folderLoader = Executors.newSingleThreadExecutor();
  /** Bumped by every load so a folder listing that arrives late can tell it is stale. */
  private int loadGeneration = 0;

  private enum EFFECT {
    NONE,
    PAN_ZOOM,
    ZOOM_OUT;

    public static EFFECT fromValue(String value) {
      try {
        return valueOf(value.toUpperCase());
      } catch (IllegalArgumentException e) {
        return NONE;
      }
    }
  }

  public static SlideshowManager getInstance(
      ClockActivity activity,
      SharedPreferences prefs,
      KenBurnsView kenBurnsView,
      ImageView slideshowSimpleView,
      ButtonManager buttonManager,
      ProfileManager profileManager) {
    if (INSTANCE == null) {
      INSTANCE =
          new SlideshowManager(
              activity, prefs, kenBurnsView, slideshowSimpleView, buttonManager, profileManager);
    }
    return INSTANCE;
  }

  public static SlideshowManager getInstance() {
    if (INSTANCE == null) {
      Timber.e("SlideshowManager not initialized");
    }
    return INSTANCE;
  }

  private SlideshowManager(
      ClockActivity activity,
      SharedPreferences prefs,
      KenBurnsView kenBurnsView,
      ImageView slideshowSimpleView,
      ButtonManager buttonManager,
      ProfileManager profileManager) {
    this.prefs = prefs;
    this.clockActivity = activity;
    this.kenBurnsView = kenBurnsView;
    this.slideshowSimpleView = slideshowSimpleView;
    this.buttonManager = buttonManager;
    this.profileManager = profileManager;
    imageDuration = 15000;
  }

  private final Runnable slideshowRunnable =
      new Runnable() {
        @Override
        public void run() {
          if (imageUris.isEmpty()) {
            return;
          }
          if (clockActivity.isDestroyed() || clockActivity.isFinishing()) {
            return;
          }
          // Keeps exactly one pending tick: a failed image reschedules straight away, and Glide can
          // report that failure before the postDelayed below has even run.
          slideShowhandler.removeCallbacks(this);

          Uri uri = imageUris.get(currentSlideshowIndex);
          Glide.with(clockActivity)
              .load(uri)
              .transition(DrawableTransitionOptions.withCrossFade(1500))
              .listener(imageLoadListener)
              .into(slideshowView);
          currentSlideshowIndex = (currentSlideshowIndex + 1) % imageUris.size();
          slideShowhandler.postDelayed(this, imageDuration); // change every X seconds
        }
      };

  /**
   * Watches for images the app can no longer read. A saved URI outlives the access to it: some
   * providers hand out grants that stop working once the picking session is gone, and a picture the
   * user has since deleted or moved is gone for good. Either way the slideshow would just show a
   * blank screen, so skip ahead and, once nothing at all loads, say so instead.
   */
  private final RequestListener<Drawable> imageLoadListener =
      new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(
            @Nullable GlideException e,
            @Nullable Object model,
            @NonNull Target<Drawable> target,
            boolean isFirstResource) {
          Timber.e("Slideshow image could not be loaded: %s (%s)", model, e);
          unreadableStreak++;
          if (unreadableStreak >= imageUris.size()) {
            stopSlideshow();
            showDialogUnreadableSlideshowImages();
          } else {
            // Do not sit on a blank screen for the whole image duration; try the next one.
            slideShowhandler.removeCallbacks(slideshowRunnable);
            slideShowhandler.post(slideshowRunnable);
          }
          return false;
        }

        @Override
        public boolean onResourceReady(
            @NonNull Drawable resource,
            @NonNull Object model,
            Target<Drawable> target,
            @NonNull DataSource dataSource,
            boolean isFirstResource) {
          unreadableStreak = 0;
          return false;
        }
      };

  private void showDialogEmptySlideshowImages() {
    showSlideshowDialog(
        savedFolder().isEmpty()
            ? R.string.slideshow_empty_dialog_msg
            : R.string.slideshow_folder_empty_dialog_msg);
  }

  private void showDialogUnreadableSlideshowImages() {
    showSlideshowDialog(R.string.slideshow_unreadable_dialog_msg);
  }

  private void showSlideshowDialog(int messageId) {
    if (clockActivity.isDestroyed() || clockActivity.isFinishing()) {
      return;
    }
    AlertDialog.Builder builder = new AlertDialog.Builder(clockActivity);
    builder
        .setMessage(clockActivity.getString(messageId))
        .setIcon(R.drawable.baseline_cancel_48)
        .setNeutralButton(R.string.dialog_button_ok, (dialog, id) -> dialog.cancel());

    AlertDialog dialog = builder.create();
    dialog.show();
  }

  /** @return the picked folder, or an empty string when images were picked one by one. */
  private String savedFolder() {
    String folder =
        prefs.getString(clockActivity.getString(R.string.setting_key_slideshow_folder), "");
    return folder != null ? folder : "";
  }

  /**
   * Fills {@link #imageUris}, then runs {@code onReady} on the main thread. Reading a folder means
   * querying a provider for every file in it, so that path goes through a background thread; the
   * saved-list path is only a JSON parse and stays inline.
   */
  private void loadImageUris(Runnable onReady) {
    String folder = savedFolder();
    if (folder.isEmpty()) {
      loadSavedImageUris();
      onReady.run();
      return;
    }
    final int generation = ++loadGeneration;
    Context appContext = clockActivity.getApplicationContext();
    Uri treeUri = Uri.parse(folder);
    folderLoader.execute(
        () -> {
          List<Uri> found = SlideshowFolder.listImages(appContext, treeUri);
          clockActivity.runOnUiThread(
              () -> {
                // A newer load, or an activity on its way out, makes this listing stale.
                if (generation != loadGeneration
                    || clockActivity.isDestroyed()
                    || clockActivity.isFinishing()) {
                  return;
                }
                imageUris.clear();
                imageUris.addAll(found);
                maybeShuffle();
                onReady.run();
              });
        });
  }

  private void loadSavedImageUris() {
    ++loadGeneration;
    imageUris.clear();
    try {
      String json =
          prefs.getString(clockActivity.getString(R.string.setting_key_slideshow_images), "[]");
      JSONArray jsonArray = new JSONArray(json);
      for (int i = 0; i < jsonArray.length(); i++) {
        imageUris.add(Uri.parse(jsonArray.getString(i)));
      }
      Timber.d("Loaded %s saved slideshow images", imageUris.size());
      maybeShuffle();
    } catch (Exception e) {
      Timber.e(e.getMessage());
    }
  }

  private void maybeShuffle() {
    if (prefs.getBoolean(
        clockActivity.getString(R.string.setting_key_slideshow_randomize), false)) {
      Collections.shuffle(imageUris);
    }
  }

  /** How many images the running slideshow has loaded; 0 until {@link #startSlideshow()} runs. */
  public int getImagesCount() {
    return imageUris.size();
  }

  /**
   * How many images the user has saved, read straight from the preference. Unlike {@link
   * #getImagesCount()} this does not depend on the slideshow having been started, so it is what the
   * settings screen should show.
   */
  public static int getSavedImageCount(Context context, SharedPreferences prefs) {
    try {
      String json =
          prefs.getString(context.getString(R.string.setting_key_slideshow_images), "[]");
      return new JSONArray(json).length();
    } catch (Exception e) {
      Timber.e(e.getMessage());
      return 0;
    }
  }

  public boolean isSlideshowEnabled() {
    return profileManager.isSlideshowEnabled();
  }

  public void enableSlideshow() {
    // The saved list has to be read here: imageUris is only filled by startSlideshow(), so right
    // after the user picks images for the first time it is still empty and the button would report
    // an empty slideshow. Leave the profile alone when there really is nothing to show, otherwise
    // the next tap on the button would be read as "disable".
    loadImageUris(
        () -> {
          if (imageUris.isEmpty()) {
            showDialogEmptySlideshowImages();
            return;
          }
          profileManager.enableSlideshow();
          startLoadedSlideshow();
        });
  }

  public void disableSlideshow() {
    profileManager.disableSlideshow();
    stopSlideshow();
  }

  public void startSlideshow() {
    loadImageUris(this::startLoadedSlideshow);
  }

  /** The part of starting that needs {@link #imageUris} to be filled already. */
  private void startLoadedSlideshow() {
    if (imageUris.isEmpty()) {
      showDialogEmptySlideshowImages();
      return;
    }
    slideshowView = slideshowSimpleView;
    EFFECT effect =
        EFFECT.fromValue(
            prefs.getString(
                clockActivity.getString(R.string.setting_key_slideshow_effect), "NONE"));
    if (effect == EFFECT.PAN_ZOOM) {
      slideshowView = kenBurnsView;
    } else if (effect == EFFECT.ZOOM_OUT) {
      slideshowView = kenBurnsView;
      kenBurnsView.setTransitionGenerator(new ZoomOnlyTransitionGenerator(10000));
    }
    imageDuration =
        Integer.parseInt(
            prefs.getString(
                clockActivity.getString(R.string.setting_key_slideshow_image_stay_duration),
                "15000"));
    currentSlideshowIndex = 0;
    unreadableStreak = 0;
    slideShowhandler.removeCallbacks(slideshowRunnable);
    slideShowhandler.post(slideshowRunnable);
    slideshowView.setVisibility(VISIBLE);
    buttonManager.lightButton(R.id.button_slideshow_enable);
    Toast.makeText(
            clockActivity,
            String.format("Slideshow {%s}, {%s}, {%s}", effect, imageUris.size(), imageDuration),
            Toast.LENGTH_SHORT)
        .show();
  }

  public void stopSlideshow() {
    slideShowhandler.removeCallbacks(slideshowRunnable);
    kenBurnsView.setVisibility(GONE);
    slideshowSimpleView.setVisibility(GONE);
    buttonManager.unlightButton(R.id.button_slideshow_enable);
  }

  public static class ZoomOnlyTransitionGenerator implements TransitionGenerator {
    private final long duration;
    private final Random random = new Random();

    public ZoomOnlyTransitionGenerator(long durationMillis) {
      this.duration = durationMillis;
    }

    @Override
    public Transition generateNextTransition(RectF drawableBounds, RectF viewport) {
      float scaleStart = 1.2f;
      float scaleEnd = 1.0f;

      // Compute center of the viewport (no pan)
      float centerX = viewport.centerX();
      float centerY = viewport.centerY();

      // Calculate start and end rects
      float startWidth = viewport.width() / scaleStart;
      float startHeight = viewport.height() / scaleStart;
      RectF startRect =
          new RectF(
              centerX - startWidth / 2,
              centerY - startHeight / 2,
              centerX + startWidth / 2,
              centerY + startHeight / 2);

      RectF endRect = new RectF(viewport); // Full size, no pan

      return new Transition(startRect, endRect, duration, new AccelerateDecelerateInterpolator());
    }
  }

  public void destroy() {
    folderLoader.shutdownNow();
    INSTANCE = null;
  }
}
