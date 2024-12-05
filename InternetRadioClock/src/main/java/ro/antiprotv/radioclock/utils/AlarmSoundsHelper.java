package ro.antiprotv.radioclock.utils;

import android.content.Context;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

public class AlarmSoundsHelper {

  public static List<AlarmSound> getAlarmSounds(Context context) {
    List<AlarmSound> alarmSounds = new ArrayList<>();

    // Initialize the RingtoneManager
    RingtoneManager ringtoneManager = new RingtoneManager(context);
    ringtoneManager.setType(RingtoneManager.TYPE_ALARM);

    // Get the Cursor
    Cursor cursor = ringtoneManager.getCursor();

    // Iterate through the Cursor to fetch ringtones
    while (cursor.moveToNext()) {
      // Get the title of the alarm sound
      String title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);

      // Get the URI of the alarm sound
      Uri uri = ringtoneManager.getRingtoneUri(cursor.getPosition());

      // Add to the list
      alarmSounds.add(new AlarmSound(title, uri));
    }

    return alarmSounds;
  }

  // Helper class to represent an alarm sound
  public static class AlarmSound {
    private String title;
    private Uri uri;

    public AlarmSound(String title, Uri uri) {
      this.title = title;
      this.uri = uri;
    }

    public String getTitle() {
      return title;
    }

    public Uri getUri() {
      return uri;
    }
  }
}
