package ro.antiprotv.radioclock.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.AttributeSet;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import ro.antiprotv.radioclock.R;
import timber.log.Timber;

public class PlayableListPreference extends ListPreference {

  private Ringtone currentRingtone;
  private int selectedIndex;

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
            Timber.d("OK clicked " + which);
            if (selectedIndex > 0) {
              setValueIndex(selectedIndex);
              setValue(getEntryValues()[selectedIndex].toString());
            }
          }
        });
    AlertDialog dialog = builder.create();
    dialog.setCancelable(false);

    dialog.show();
  }

  private void playRingtone(String uriString) {
    if (currentRingtone != null) {
      currentRingtone.stop();
    }
    Uri uri = Uri.parse(uriString);
    currentRingtone = RingtoneManager.getRingtone(getContext(), uri);
    if (currentRingtone != null) {
      currentRingtone.play();
    }
  }
}
