package ro.antiprotv.radioclock.service;

import static android.content.Context.AUDIO_SERVICE;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.activity.ClockActivity;
import timber.log.Timber;

// import timber.log.Timber;

/**
 * Manages the volume of the player It does not manage the overall device volume set by hardware
 * buttons
 */
public class VolumeManager {
  private final Context ctx;
  private final TextView volumeText;
  private final DecimalFormat fmt = new DecimalFormat("#");
  private final AudioManager audioManager;
  private final int maxVolume;
  private VerticalSeekBar seekBar;
  private ScheduledFuture progressiveVolumeTask;
  private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);

  public VolumeManager(Context ctx, View view) {
    this.ctx = ctx;
    this.volumeText = view.findViewById(R.id.volume);

    audioManager = (AudioManager) ctx.getApplicationContext().getSystemService(AUDIO_SERVICE);
    maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    setVolumeText(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
    int currentVolume = getVolume();
    ctx.getApplicationContext()
        .getContentResolver()
        .registerContentObserver(
            android.provider.Settings.System.CONTENT_URI,
            true,
            new SettingsContentObserver(new Handler()));

    seekBar = view.findViewById(R.id.volume_seekbar);
    if (seekBar == null) {
      AlertDialog dialog = new AlertDialog.Builder(ctx).create();
      dialog.setTitle("ERROR");
      dialog.setMessage("Could not find seekbar " + new Date(System.currentTimeMillis()));
      dialog.show();
      return;
    }
    seekBar.setMax(maxVolume);
    seekBar.setProgress(currentVolume);
    seekBar.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {

          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            // Timber.d("vol: " + progress);
            setVolume(progress);
          }

          @Override
          public void onStartTrackingTouch(SeekBar seekBar) {}

          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {}
        });
  }

  /** increase volume */
  public void volumeUp() {
    int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    audioManager.adjustVolume(
        AudioManager.ADJUST_RAISE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    Timber.d("vol up: current " + currentVolume);
    if (currentVolume >= maxVolume) {
      // run on ui thread, b/c this is accessed by the progressive volume task
      ((ClockActivity) ctx)
          .runOnUiThread(() -> Toast.makeText(ctx, "Volume MAX", Toast.LENGTH_SHORT).show());
    }
    // setVolumeText(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC), maxVolume);
    Timber.d("vol up: after change " + audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
  }

  /** decrease volume */
  protected void volumeDown() {
    int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    Timber.d("vol dn: current " + currentVolume);
    if (currentVolume <= 0) {
      ((ClockActivity) ctx)
          .runOnUiThread(() -> Toast.makeText(ctx, "Muted", Toast.LENGTH_SHORT).show());
    }
    audioManager.adjustVolume(
        AudioManager.ADJUST_LOWER, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    Timber.d("vol dn: after change " + audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
    // setVolumeText(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC), maxVolume);
  }

  public int getVolume() {
    return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
  }

  /**
   * Set exact volume
   *
   * @param volume
   */
  public void setVolume(final int volume) {
    // run on ui thread, b/c this is accessed by the progressive volume task
    audioManager.setStreamVolume(
        AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    ((ClockActivity) ctx).runOnUiThread(() -> volumeText.setText(getVolumePct(volume)));
  }

  public int getMaxVolume() {
    return maxVolume;
  }

  private String getVolumePct(int volume) {
    double volumePct = Math.ceil((float) volume / (float) maxVolume * 100);
    return fmt.format(volumePct) + "%";
  }

  private void setVolumeText(int volume) {
    // Timber.d(String.valueOf(volumePct));
    // Timber.d(fmt.format(volumePct));
    ((ClockActivity) ctx).runOnUiThread(() -> volumeText.setText(getVolumePct(volume)));
  }

  public void cancelProgressiveVolumeTask() {
    if (progressiveVolumeTask != null && !progressiveVolumeTask.isCancelled()) {
      progressiveVolumeTask.cancel(true);
    }
  }

  public void setupProgressiveVolumeTask() {
    progressiveVolumeTask =
        executorService.scheduleWithFixedDelay(
            new AlarmProgressiveVolume(), 10, 20, TimeUnit.SECONDS);
  }

  public class SettingsContentObserver extends ContentObserver {
    public SettingsContentObserver(Handler handler) {
      super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
      int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
      // Timber.d("Volume now " + currentVolume);
      setVolumeText(currentVolume);
      seekBar.setProgress(currentVolume);
    }
  }

  private class AlarmProgressiveVolume implements Runnable {
    public void run() {
      if (getVolume() > getMaxVolume()) {
        cancelProgressiveVolumeTask();
        return;
      }
      volumeUp();
    }
  }
}
