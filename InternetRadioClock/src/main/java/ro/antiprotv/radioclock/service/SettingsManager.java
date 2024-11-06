package ro.antiprotv.radioclock.service;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.widget.ImageButton;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.SleepManager;
import ro.antiprotv.radioclock.activity.ClockActivity;

public class SettingsManager implements OnSharedPreferenceChangeListener {
  private final ClockActivity clockActivity;
  private final ButtonManager buttonManager;
  private final SleepManager sleepManager;
  private final SharedPreferences prefs;
  private final BatteryService batteryService;
  private final MediaPlayerService mediaPlayerService;
  private final TimerService timerService;

  public SettingsManager(
      ClockActivity clockActivity,
      ButtonManager buttonManager,
      SleepManager sleepManager,
      BatteryService batteryService,
      MediaPlayerService mediaPlayerService,
      TimerService timerService) {
    this.clockActivity = clockActivity;
    this.buttonManager = buttonManager;
    this.sleepManager = sleepManager;
    this.prefs = PreferenceManager.getDefaultSharedPreferences(clockActivity);
    this.batteryService = batteryService;
    this.mediaPlayerService = mediaPlayerService;
    this.timerService = timerService;
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    setupButtons(prefs, key);

    setupSleepTimers(prefs, key);

    if (key.equals(clockActivity.getmPlayingStreamTag())) {
      mediaPlayerService.stopPlaying();
      // since we stopped, the clicked button is reset
      // set this one here
      // TODO: find a better solution
      buttonManager.setButtonClicked(buttonManager.findButtonByTag(key));
      mediaPlayerService.play(buttonManager.findButtonByTag(key).getId());
    }

    setupBatteryMonitoring(key);
    setupTimers(prefs, key);
  }

  private void setupSleepTimers(SharedPreferences prefs, String key) {
    if (key.equals(clockActivity.getResources().getString(R.string.setting_key_sleepMinutes))) {
      int customTimer = 0;
      try {
        Integer.parseInt(
            prefs.getString(
                clockActivity.getResources().getString(R.string.setting_key_sleepMinutes), "0"));
      } catch (NumberFormatException e) {
        prefs
            .edit()
            .putString(
                clockActivity.getResources().getString(R.string.setting_key_sleepMinutes), "0")
            .apply();
      }
      if (customTimer == 0) {
        sleepManager.getTimers().remove(0);
      } else {
        sleepManager.getTimers().add(0, customTimer);
      }
    }
  }

  private void setupTimers(SharedPreferences prefs, String key) {
    if (key.equals(
        clockActivity.getResources().getString(R.string.setting_key_timer_long_seconds))) {
      ImageButton timer = clockActivity.findViewById(R.id.timer_long);
      timer.setOnClickListener(
          v -> {
            timerService.setTimerSeconds(Integer.parseInt(prefs.getString(key, "180")));
            timerService.setTimer(R.id.timer_long);
          });
    }
    if (key.equals(
        clockActivity.getResources().getString(R.string.setting_key_timer_short_seconds))) {
      ImageButton timer = clockActivity.findViewById(R.id.timer_short);
      timer.setOnClickListener(
          v -> {
            timerService.setTimerSeconds(Integer.parseInt(prefs.getString(key, "10")));
            timerService.setTimer(R.id.timer_short);
          });
    }
  }

  private void setupButtons(SharedPreferences prefs, String key) {
    // BUTTONS
    int buttonIndex = -1;

    if (key.equals(clockActivity.getResources().getString(R.string.setting_key_label1))) {
      buttonIndex = 0;
    }
    if (key.equals(clockActivity.getResources().getString(R.string.setting_key_label2))) {
      buttonIndex = 1;
    }
    if (key.equals(clockActivity.getResources().getString(R.string.setting_key_label3))) {
      buttonIndex = 2;
    }
    if (key.equals(clockActivity.getResources().getString(R.string.setting_key_label4))) {
      buttonIndex = 3;
    }
    if (key.equals(clockActivity.getResources().getString(R.string.setting_key_label5))) {
      buttonIndex = 4;
    }
    if (key.equals(clockActivity.getResources().getString(R.string.setting_key_label6))) {
      buttonIndex = 5;
    }
    if (key.equals(clockActivity.getResources().getString(R.string.setting_key_label7))) {
      buttonIndex = 6;
    }
    if (key.equals(clockActivity.getResources().getString(R.string.setting_key_label8))) {
      buttonIndex = 7;
    }
    if (key.contains("setting.key.label")) {
      buttonManager.setText(buttonIndex, prefs);
    }

    int streamIndex = -1;
    if (key.equals(clockActivity.getResources().getString(R.string.setting_key_stream1))) {
      streamIndex = 0;
    }
    if (key.equals(clockActivity.getResources().getString(R.string.setting_key_stream2))) {
      streamIndex = 1;
    }
    if (key.equals(clockActivity.getResources().getString(R.string.setting_key_stream3))) {
      streamIndex = 2;
    }
    if (key.equals(clockActivity.getResources().getString(R.string.setting_key_stream4))) {
      streamIndex = 3;
    }
    if (key.equals(clockActivity.getResources().getString(R.string.setting_key_stream5))) {
      streamIndex = 4;
    }
    if (key.equals(clockActivity.getResources().getString(R.string.setting_key_stream6))) {
      streamIndex = 5;
    }
    if (key.equals(clockActivity.getResources().getString(R.string.setting_key_stream7))) {
      streamIndex = 6;
    }
    if (key.equals(clockActivity.getResources().getString(R.string.setting_key_stream8))) {
      streamIndex = 7;
    }
    if (key.contains("stream")) {
      String url = prefs.getString(key, "");
      buttonManager.getmUrls().set(streamIndex, url);
      buttonManager.hideUnhideButtons();
    }
  }

  private void setupBatteryMonitoring(String key) {
    if (key.equals(
        clockActivity.getResources().getString(R.string.setting_key_alwaysDisplayBattery))) {
      boolean show = isAlwaysDisplayBattery();
      if (show) {
        batteryService.registerBatteryLevelReceiver();
      } else {
        batteryService.unregisterBatteryLevelReceiver();
      }
    }
  }

  public boolean isAlwaysDisplayBattery() {
    return prefs.getBoolean(
        clockActivity.getResources().getString(R.string.setting_key_alwaysDisplayBattery), false);
  }
}
