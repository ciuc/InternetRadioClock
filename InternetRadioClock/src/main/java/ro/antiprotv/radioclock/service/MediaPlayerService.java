package ro.antiprotv.radioclock.service;

import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;
import com.devbrackets.android.exomedia.AudioPlayer;
import com.devbrackets.android.exomedia.listener.OnErrorListener;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.activity.ClockActivity;
import timber.log.Timber;

public class MediaPlayerService {
  private AudioPlayer mMediaPlayer;
  private final ClockActivity clockActivity;
  private final RadioAlarmManager alarmManager;
  private final ButtonManager buttonManager;
  private final VolumeManager volumeManager;
  private final SharedPreferences prefs;

  public MediaPlayerService(
      ClockActivity clockActivity,
      RadioAlarmManager alarmManager,
      ButtonManager buttonManager,
      VolumeManager volumeManager,
      SharedPreferences prefs) {
    this.clockActivity = clockActivity;
    this.alarmManager = alarmManager;
    this.buttonManager = buttonManager;
    this.volumeManager = volumeManager;
    if (mMediaPlayer == null) {
      initMediaPlayer();
    }
    this.prefs = prefs;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Media Player
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // init
  /** Initialize or reinitialize the media player */
  public void initMediaPlayer() {
    if (mMediaPlayer == null) {
      mMediaPlayer = new AudioPlayer(clockActivity);
      mMediaPlayer.setOnPreparedListener(new MediaPlayerService.CustomOnPreparedListener());
      mMediaPlayer.setOnErrorListener(new MediaPlayerService.CustomOnErrorListener());
    }
  }

  public void resetMediaPlayer() {
    if (mMediaPlayer != null) {
      stopPlaying();
      mMediaPlayer = null;
    }
  }

  public void play(int buttonId) {
    if (mMediaPlayer == null) {
      initMediaPlayer();
    }
    // if already playing and comes from alarm -do nothing
    if (mMediaPlayer.isPlaying() && alarmManager.isAlarmPlaying()) {
      alarmManager.changeAlarmIconAndTextOnCancel();
      return;
    }
    // we might have a default alarm playing, so need to shut it off
    alarmManager.shutDownDefaultAlarm();
    boolean isProgressiveSound =
        prefs.getBoolean(
            clockActivity.getResources().getString(R.string.setting_key_alarmProgressiveSound),
            false);
    Timber.d(String.format("progressive: %b", isProgressiveSound));
    if (alarmManager.isAlarmPlaying() && isProgressiveSound) {
      volumeManager.cancelProgressiveVolumeTask();
      volumeManager.setVolume(1);
      // Timber.d("Scheduling progressive volume task");
      volumeManager.setupProgressiveVolumeTask();
    }
    String url = buttonManager.getUrl(buttonId);
    buttonManager.resetButtons();
    Toast.makeText(clockActivity, "Connecting to " + url, Toast.LENGTH_SHORT).show();
    if (url != null) {
      mMediaPlayer.setMedia(Uri.parse(url));
    } else { // Something went wrong, resetting
      resetMediaPlayer();
      buttonManager.enableButtons();
    }
  }

  public void stopPlaying() {
    if (mMediaPlayer != null) {
      if (mMediaPlayer.isPlaying()) {
        Toast.makeText(clockActivity, "Stopping stream", Toast.LENGTH_SHORT).show();
      }
      mMediaPlayer.reset();
    }
    buttonManager.onStopPlaying();

    if (clockActivity.getSupportActionBar() != null) {
      clockActivity
          .getSupportActionBar()
          .setTitle(clockActivity.getResources().getString(R.string.app_name));
    }
    clockActivity.setmPlayingStreamNo(0);
    clockActivity.setmPlayingStreamTag(null);
    clockActivity.setPlaying(false);
    volumeManager.cancelProgressiveVolumeTask();
  }

  private class CustomOnPreparedListener implements OnPreparedListener {
    @Override
    public void onPrepared() {
      mMediaPlayer.start();

      buttonManager.lightButton();
      clockActivity.setmPlayingStreamNo(buttonManager.getButtonClicked().getId());
      clockActivity.setmPlayingStreamTag(buttonManager.getButtonClicked().getTag().toString());
      buttonManager.onPrepared();
      clockActivity.setPlaying(true);
      // default url do not show, b/c they are not present in prefs at first
      String defaultKey = clockActivity.getmPlayingStreamTag().replace("setting.key.stream", "");
      int index = Integer.parseInt(defaultKey) - 1;
      Toast.makeText(
              clockActivity, "Playing " + buttonManager.getmUrls().get(index), Toast.LENGTH_SHORT)
          .show();
      if (clockActivity.getSupportActionBar() != null) {
        clockActivity
            .getSupportActionBar()
            .setTitle(
                clockActivity.getResources().getString(R.string.app_name)
                    + ": "
                    + buttonManager.getmUrls().get(index));
      }
      // setAlarmPlaying(false);
    }
  }

  private class CustomOnErrorListener implements OnErrorListener {
    @Override
    public boolean onError(Exception e) {
      Toast.makeText(clockActivity, "Error playing stream", Toast.LENGTH_SHORT).show();
      resetMediaPlayer();
      initMediaPlayer();
      buttonManager.onError();
      if (clockActivity.getSupportActionBar() != null) {
        clockActivity
            .getSupportActionBar()
            .setTitle(clockActivity.getResources().getString(R.string.app_name));
      }
      if (alarmManager.isAlarmPlaying()) {
        alarmManager.playDefaultAlarmOnStreamError();
      }
      return false;
    }
  }

  public boolean isPlaying() {
    return mMediaPlayer.isPlaying();
  }

  public void onRestart() {
    // Timber.d(TAG_STATE, "onRestart");
    if (mMediaPlayer == null) {
      initMediaPlayer();
    }
  }
}
