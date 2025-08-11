package ro.antiprotv.radioclock.service;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.SharedPreferences;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Handler;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.flaviofaria.kenburnsview.KenBurnsView;
import com.flaviofaria.kenburnsview.Transition;
import com.flaviofaria.kenburnsview.TransitionGenerator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
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
  private final Handler slideShowhandler = new Handler();
  private final ButtonManager buttonManager;
  private final ProfileManager profileManager;
  private ImageView slideshowView;
  private int imageDuration;

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

          Uri uri = imageUris.get(currentSlideshowIndex);
          Glide.with(clockActivity)
              .load(uri)
              .transition(DrawableTransitionOptions.withCrossFade(1500))
              .into(slideshowView);
          currentSlideshowIndex = (currentSlideshowIndex + 1) % imageUris.size();
          slideShowhandler.postDelayed(this, imageDuration); // change every X seconds
        }
      };

  private void showDialogEmptySlideshowImages() {
    AlertDialog.Builder builder = new AlertDialog.Builder(clockActivity);
    builder
        .setMessage(clockActivity.getString(R.string.slideshow_empty_dialog_msg))
        .setIcon(R.drawable.baseline_cancel_48)
        .setNeutralButton(R.string.dialog_button_ok, (dialog, id) -> dialog.cancel());

    AlertDialog dialog = builder.create();
    dialog.show();
  }

  private void loadSavedImageUris() {
    imageUris.clear();
    try {

      String json =
          prefs.getString(clockActivity.getString(R.string.setting_key_slideshow_images), "[]");
      Timber.d(json);
      JSONArray jsonArray = new JSONArray(json);
      for (int i = 0; i < jsonArray.length(); i++) {
        imageUris.add(Uri.parse(jsonArray.getString(i)));
      }

      if (prefs.getBoolean(
          clockActivity.getString(R.string.setting_key_slideshow_randomize), false)) {
        Collections.shuffle(imageUris);
      }
    } catch (Exception e) {
      Timber.e(e.getMessage());
    }
  }

  public int getImagesCount() {
    return imageUris.size();
  }

  public boolean isSlideshowEnabled() {
    return profileManager.isSlideshowEnabled();
  }

  public void enableSlideshow() {
    profileManager.enableSlideshow();
    if (!imageUris.isEmpty()) {
      startSlideshow();
    } else {
      showDialogEmptySlideshowImages();
    }
  }

  public void disableSlideshow() {
    profileManager.disableSlideshow();
    stopSlideshow();
  }

  public void startSlideshow() {
    loadSavedImageUris();
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
    INSTANCE = null;
  }
}
