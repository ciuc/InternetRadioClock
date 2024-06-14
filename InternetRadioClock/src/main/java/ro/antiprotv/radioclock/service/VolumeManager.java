package ro.antiprotv.radioclock.service;

import static android.content.Context.AUDIO_SERVICE;

import android.app.Dialog;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DecimalFormat;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.activity.ClockActivity;
import timber.log.Timber;

// import timber.log.Timber;

/**
 * Manages the volume of the player It does not manage the overall device volume set by hardware
 * buttons
 */
public class VolumeManager {
  private final ImageButton volumeUpButton;
  private final Context ctx;
  private final TextView volumeText;
  private final DecimalFormat fmt = new DecimalFormat("#");
  private final AudioManager audioManager;
  private final int maxVolume;

  public VolumeManager(Context ctx, View view) {
    this.ctx = ctx;
    volumeUpButton = view.findViewById(R.id.volumeup_button);
    this.volumeText = view.findViewById(R.id.volume);
    volumeUpButton.setOnClickListener(new VolumeUpOnClickListener());

    audioManager = (AudioManager) ctx.getApplicationContext().getSystemService(AUDIO_SERVICE);
    maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    setVolumeText(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
    ctx.getApplicationContext()
        .getContentResolver()
        .registerContentObserver(
            android.provider.Settings.System.CONTENT_URI,
            true,
            new SettingsContentObserver(new Handler()));
  }

  /** increase volume */
  public void volumeUp() {
    int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    audioManager.adjustVolume(
        AudioManager.ADJUST_RAISE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    Timber.d("vol up: current " + String.valueOf(currentVolume));
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
    Timber.d("vol dn: current " + String.valueOf(currentVolume));
    if (currentVolume <= 0) {
      ((ClockActivity) ctx)
          .runOnUiThread(() -> Toast.makeText(ctx, "Muted", Toast.LENGTH_SHORT).show());
    }
    audioManager.adjustVolume(
        AudioManager.ADJUST_LOWER, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    Timber.d("vol dn: after change " + audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
    // setVolumeText(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC), maxVolume);
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

  public int getVolume() {
    return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
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

  private class VolumeUpOnClickListener implements View.OnClickListener {

    @Override
    public void onClick(View v) {
      // earlier on we had soem fancy stuff here, with onTouch and setting
      // stroke
      Dialog dialog = new Dialog(ctx);
      dialog.setContentView(R.layout.dialog_volume);
      dialog.setTitle("Set size!");
      dialog.setCancelable(true);

      dialog.show();

      final TextView volume = dialog.findViewById(R.id.volume_pct);
      int currentVolume = getVolume();

      SeekBar seekBar = dialog.findViewById(R.id.volume_seekbar);
      seekBar.setMax(maxVolume);
      seekBar.setProgress(currentVolume);
      volume.setText(getVolumePct(currentVolume));
      seekBar.setOnSeekBarChangeListener(
          new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
              setVolume(progress);
              volume.setText(getVolumePct(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
          });
    }
  }

  public class SettingsContentObserver extends ContentObserver {
    public SettingsContentObserver(Handler handler) {
      super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
      int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
      Timber.d("Volume now " + currentVolume);
      setVolumeText(currentVolume);
    }
  }
}
