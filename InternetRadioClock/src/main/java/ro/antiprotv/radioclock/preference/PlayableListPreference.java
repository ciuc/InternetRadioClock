package ro.antiprotv.radioclock.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.AttributeSet;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;

import java.io.IOException;

import ro.antiprotv.radioclock.R;
import timber.log.Timber;

public class PlayableListPreference extends ListPreference {

  private Ringtone currentRingtone;
  private int selectedIndex;
    private MediaPlayer mediaPlayer;

  public PlayableListPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onClick() {
    AlertDialog.Builder builder =
        new AlertDialog.Builder(getContext())
            .setTitle(getTitle())
            .setSingleChoiceItems(
                getEntries(),
                findIndexOfValue(getValue()),
                new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    playRingtone(getEntryValues()[which].toString());
                    selectedIndex = which;
                  }
                });

    builder.setPositiveButton(
        R.string.ok,
        new DialogInterface.OnClickListener() {

          @Override
          public void onClick(DialogInterface dialog, int which) {
            Timber.d("OK clicked %s", which);
            if (selectedIndex > 0) {
              setValueIndex(selectedIndex);
              setValue(getEntryValues()[selectedIndex].toString());
              if (mediaPlayer != null) {
                  mediaPlayer.stop();
                  mediaPlayer.release();
                  mediaPlayer = null;
              }
            }
          }
        });
    AlertDialog dialog = builder.create();
    dialog.setCancelable(false);

    dialog.show();
  }

  private void playRingtone(String uriString) {
      // Stop currently playing sound
      if (mediaPlayer != null) {
          mediaPlayer.stop();
          mediaPlayer.release();
          mediaPlayer = null;
      }

      mediaPlayer = new MediaPlayer();
      try {
          mediaPlayer.setDataSource(getContext(), Uri.parse(uriString));
          mediaPlayer.setAudioAttributes(
                  new AudioAttributes.Builder()
                          .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                          .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                          .build()
          );
          mediaPlayer.setLooping(false);

          mediaPlayer.setOnCompletionListener(mp -> {
              mp.stop();
              mp.release();
              mediaPlayer = null;
          });

          mediaPlayer.prepare();
          mediaPlayer.start();

      } catch (IOException e) {
          Timber.e(e);
      }
      /*
    if (currentRingtone != null) {
      currentRingtone.stop();
    }
    Uri uri = Uri.parse(uriString);
    currentRingtone = RingtoneManager.getRingtone(getContext(), uri);
    if (currentRingtone != null) {
      currentRingtone.play();
    }

       */
  }

    @Override
    public void onDetached() {
        super.onDetached();
    }
}
