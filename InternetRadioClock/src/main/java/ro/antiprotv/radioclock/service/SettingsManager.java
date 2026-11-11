package ro.antiprotv.radioclock.service;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.widget.Button;
import java.util.List;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.SleepManager;
import ro.antiprotv.radioclock.activity.ClockActivity;
import ro.antiprotv.radioclock.db.Station;

public class SettingsManager implements OnSharedPreferenceChangeListener {
  private final ClockActivity clockActivity;
  private final ButtonManager buttonManager;
  private final SleepManager sleepManager;
  private final SharedPreferences prefs;
  private final MediaPlayerService mediaPlayerService;
  private final TimerService timerService;

  public SettingsManager(
      ClockActivity clockActivity,
      ButtonManager buttonManager,
      SleepManager sleepManager,
      MediaPlayerService mediaPlayerService,
      TimerService timerService) {
    this.clockActivity = clockActivity;
    this.buttonManager = buttonManager;
    this.sleepManager = sleepManager;
    this.prefs = PreferenceManager.getDefaultSharedPreferences(clockActivity);
    this.mediaPlayerService = mediaPlayerService;
    this.timerService = timerService;
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    // the memory buttons live in the database now: ClockActivity watches it and calls
    // onStationsChanged
    setupSleepTimers(prefs, key);

    setupTimers(prefs, key);

    setupBackgroundPlay(prefs, key);
  }

  /** Apply the background play setting right away when it is toggled while the radio is playing. */
  private void setupBackgroundPlay(SharedPreferences prefs, String key) {
    if (!key.equals(clockActivity.getResources().getString(R.string.setting_key_backgroundPlay))) {
      return;
    }
    if (!mediaPlayerService.isPlaying()) {
      return;
    }
    if (prefs.getBoolean(key, false)) {
      mediaPlayerService.startBackgroundPlayback();
    } else {
      mediaPlayerService.stopBackgroundPlayback();
    }
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
        clockActivity.getResources().getString(R.string.setting_key_timer_alarm_duration))) {
      timerService.setAlarmDuration(Integer.parseInt(prefs.getString(key, "7")));
    }
  }

  /**
   * The saved stations changed: refresh the memory buttons, and if the stream of the station
   * playing was changed, switch to the new one.
   */
  public void onStationsChanged(List<Station> stations) {
    List<Integer> changed = buttonManager.applyStations(stations);
    if (!mediaPlayerService.isPlaying()) {
      return;
    }
    Button playing = buttonManager.findButtonByTag(clockActivity.getmPlayingStreamTag());
    if (playing == null) {
      return;
    }
    int index = buttonManager.buttonIndex(playing);
    if (!changed.contains(index)) {
      return;
    }
    mediaPlayerService.stopPlaying();
    if (!buttonManager.getmUrls().get(index).isEmpty()) {
      // stopping forgets the clicked button: pick it again
      buttonManager.setButtonClicked(playing);
      mediaPlayerService.play(playing.getId());
    }
  }
}
