package ro.antiprotv.radioclock.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Button;
import android.widget.Toast;
import com.devbrackets.android.exomedia.AudioPlayer;
import com.devbrackets.android.exomedia.listener.OnErrorListener;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.activity.ClockActivity;
import timber.log.Timber;

public class MediaPlayerService {
  private final ClockActivity clockActivity;
  private final RadioAlarmManager alarmManager;
  private final ButtonManager buttonManager;
  private final VolumeManager volumeManager;
  private final SharedPreferences prefs;
  private final Context appContext;
  private AudioPlayer mMediaPlayer;
  /** Label of the station currently playing; shown in the background play notification. */
  private String playingStationLabel;

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
    this.prefs = prefs;
    this.appContext = clockActivity.getApplicationContext();
    if (mMediaPlayer == null) {
      initMediaPlayer();
    }
    BackgroundPlaybackService.setStopListener(this::stopPlaying);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Media Player
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // init
  /** Initialize or reinitialize the media player */
  public void initMediaPlayer() {
    if (mMediaPlayer == null) {
      mMediaPlayer = new AudioPlayer(appContext);
      mMediaPlayer.setOnPreparedListener(new MediaPlayerService.CustomOnPreparedListener());
      mMediaPlayer.setOnErrorListener(new MediaPlayerService.CustomOnErrorListener());
    }
  }

  /**
   * Tears the player down for good. Since we drop the reference here, the player has to be released
   * as well: otherwise its playback thread, audio track and decoder stay alive for the life of the
   * process. To merely stop the radio, use {@link #stopPlaying()} - the player stays reusable, a
   * later {@link #play(int)} prepares it again.
   */
  public void resetMediaPlayer() {
    if (mMediaPlayer != null) {
      stopPlaying();
      mMediaPlayer.release();
      mMediaPlayer = null;
    }
  }

  public void play(int buttonId) {
    if (mMediaPlayer == null) {
      initMediaPlayer();
    }
    // if already playing and comes from alarm -do nothing
    if (mMediaPlayer.isPlaying() && clockActivity.isAlarmPlaying()) {
      alarmManager.changeAlarmIconAndTextOnCancel();
      return;
    }
    // we might have a default alarm playing, so need to shut it off
    alarmManager.shutDownDefaultAlarm();
    boolean isProgressiveSound =
        prefs.getBoolean(
            clockActivity.getResources().getString(R.string.setting_key_alarmProgressiveSound),
            false);
    Timber.d(String.format("alarm playing %b, progressive: %b", clockActivity.isAlarmPlaying(), isProgressiveSound));
    if (clockActivity.isAlarmPlaying() && isProgressiveSound) {
      volumeManager.cancelProgressiveVolumeTask();
      volumeManager.setVolume(1);
      Timber.d("Scheduling progressive volume task");
      volumeManager.setupProgressiveVolumeTask();
    }
    String url = buttonManager.getUrl(buttonId);
    buttonManager.resetButtons();
    Toast.makeText(
            clockActivity,
            clockActivity.getString(R.string.connecting_to) + url,
            Toast.LENGTH_SHORT)
        .show();
    if (url != null) {
      Button button = buttonManager.findButtonById(buttonId);
      playingStationLabel = button == null ? url : button.getText().toString();
      startBackgroundPlayback();
      mMediaPlayer.setMedia(Uri.parse(url));
    } else { // Something went wrong, resetting
      resetMediaPlayer();
      buttonManager.enableButtons();
    }
  }

  /** Is the radio allowed to keep playing once the app is no longer in the foreground? */
  public boolean isBackgroundPlayEnabled() {
    return prefs.getBoolean(
        clockActivity.getResources().getString(R.string.setting_key_backgroundPlay), false);
  }

  /**
   * Hands playback over to the foreground service so the system keeps our process (and the stream)
   * alive when the clock is not visible. Does nothing when the setting is off.
   *
   * <p>No wake lock is needed on top of this: the audio server holds one on our behalf ('AudioMix')
   * for as long as a track is actually playing, which covers a sleeping screen.
   */
  public void startBackgroundPlayback() {
    if (mMediaPlayer == null || !isBackgroundPlayEnabled()) {
      return;
    }
    BackgroundPlaybackService.start(appContext, playingStationLabel);
  }

  public void stopBackgroundPlayback() {
    BackgroundPlaybackService.stop(appContext);
  }

  public void stopPlaying() {
    stopBackgroundPlayback();
    playingStationLabel = null;
    if (mMediaPlayer != null) {
      if (mMediaPlayer.isPlaying()) {
        Toast.makeText(clockActivity, R.string.stopping_stream, Toast.LENGTH_SHORT).show();
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

  public boolean isPlaying() {
    return mMediaPlayer != null && mMediaPlayer.isPlaying();
  }

  public void onRestart() {
    // Timber.d(TAG_STATE, "onRestart");
    if (mMediaPlayer == null) {
      initMediaPlayer();
    }
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
              clockActivity,
              clockActivity.getString(R.string.playing) + buttonManager.getmUrls().get(index),
              Toast.LENGTH_SHORT)
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
      Toast.makeText(clockActivity, R.string.error_playing_stream, Toast.LENGTH_SHORT).show();
      // keep the player: it is reusable after a stop, and throwing it away on every failed stream
      // (dead station, network drop) would abandon a live playback thread each time
      stopPlaying();
      buttonManager.onError();
      if (clockActivity.getSupportActionBar() != null) {
        clockActivity
            .getSupportActionBar()
            .setTitle(clockActivity.getResources().getString(R.string.app_name));
      }
      if (clockActivity.isAlarmPlaying()) {
        alarmManager.playDefaultAlarmOnStreamError();
      }
      return false;
    }
  }
}
