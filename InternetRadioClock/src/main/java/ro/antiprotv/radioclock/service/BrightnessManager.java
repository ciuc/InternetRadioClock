package ro.antiprotv.radioclock.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import androidx.appcompat.app.AlertDialog;
import java.util.Date;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.activity.ClockActivity;
import ro.antiprotv.radioclock.service.profile.ProfileManager;
import timber.log.Timber;

public class BrightnessManager {
  private final SeekBar seekBar;
  ImageButton autoBrightnessButton;
  Context context;
  private final ProfileManager profileManager;
  private ImageView thumb;
  private boolean disableOnProgressChanged = false;

  @SuppressLint("ClickableViewAccessibility")
  public BrightnessManager(Context context, View view, ProfileManager profileManager) {
    this.profileManager = profileManager;
    this.context = context;
    seekBar = view.findViewById(R.id.seekbar_brightness);
    int currentBrightness = profileManager.getBrightness();
    Timber.d(
        "BrightnessManager - saved in settings (currentProfile.Brightness): " + currentBrightness);
    if (seekBar == null) {
      AlertDialog dialog = new AlertDialog.Builder(context).create();
      dialog.setTitle("ERROR");
      dialog.setMessage(
          context.getString(R.string.could_not_find_seekbar)
              + new Date(System.currentTimeMillis()));
      dialog.show();
      return;
    }
    seekBar.setMax(100);
    disableOnProgressChanged = true;
    seekBar.setProgress(currentBrightness > 0 ? currentBrightness : 50);
    seekBar.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {

          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!disableOnProgressChanged) {
              Timber.d("seekbar on progress changed: brightness: " + progress);
              profileManager.setBrightness(progress);
            } else {
              Timber.d("disableOnProgressChanged true");
              disableOnProgressChanged = false;
            }
          }

          @Override
          public void onStartTrackingTouch(SeekBar seekBar) {}

          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    // Set an OnTouchListener for the SeekBar
    seekBar.setOnTouchListener(
        new SeekBar.OnTouchListener() {
          @Override
          public boolean onTouch(View v, MotionEvent event) {
            // Handle the SeekBar touch events here
            switch (event.getAction()) {
              case MotionEvent.ACTION_DOWN:
              // Timber.d("Seekbar ontouch: action ACTION_DOWN ");
              case MotionEvent.ACTION_MOVE:
                // Timber.d("Seekbar ontouch: action ACTION_MOVE");
                ((ClockActivity) context).setDisallowSwipe(true);
                return false;
              case MotionEvent.ACTION_UP:
                // Timber.d("Seekbar ontouch: action ACTION_UP");
                ((ClockActivity) context).setDisallowSwipe(false);
                return false; // Let the SeekBar handle its own events
            }
            return false;
          }
        });
    autoBrightnessButton = view.findViewById(R.id.brightness_auto);
    if (currentBrightness < 0) {
      disableBrightnessSeekbar();
    }
    autoBrightnessButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            toggleAutoBrightnessEnabled();
          }
        });
  }

  private void toggleAutoBrightnessEnabled() {
    if (seekBar.isEnabled()) {
      disableBrightnessSeekbar();
      setBrightness(-1);
    } else {
      enableBrightnessSeekbar();
      setBrightness(seekBar.getProgress());
    }
  }

  private void disableBrightnessSeekbar() {
    seekBar.setEnabled(false);
    seekBar
        .getThumb()
        .mutate()
        .setColorFilter(new PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP));
    autoBrightnessButton.setColorFilter(context.getResources().getColor(R.color.color_clock));
  }

  private void enableBrightnessSeekbar() {
    seekBar.setEnabled(true);
    seekBar.getThumb().mutate().clearColorFilter();
    autoBrightnessButton.clearColorFilter();
  }

  public void setBrightness(int brightness) {
    if (brightness < 0) {
      profileManager.setBrightness(-1);
    } else {
      seekBar.setProgress(brightness);
    }
  }

  public void setupSeekbar(int brightness) {
    disableOnProgressChanged = true;
    seekBar.setProgress(brightness);
    if (brightness < 0) {
      disableBrightnessSeekbar();
    } else {
      enableBrightnessSeekbar();
    }
  }
}
