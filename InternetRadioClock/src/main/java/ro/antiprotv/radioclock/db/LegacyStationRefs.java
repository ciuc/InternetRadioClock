package ro.antiprotv.radioclock.db;

import android.content.SharedPreferences;
import java.util.Map;

/**
 * Older versions pointed at a memory button ("setting.key.streamN") for the last played station
 * and for the alarm's wake-up station. Stations can now change buttons, so those are turned into
 * station ids once, while it is still known which station sat on which button: when the stations
 * are imported from the old settings, and when an early database closes its gaps.
 */
final class LegacyStationRefs {

  private LegacyStationRefs() {}

  /**
   * @param idByMemory the id of the station on each memory button, by button number, as the
   *     buttons were when the old values were saved
   */
  static void convert(SharedPreferences prefs, String wakeUpKey, Map<Integer, Long> idByMemory) {
    SharedPreferences.Editor editor = prefs.edit();
    if (!prefs.contains(StationPreferences.LAST_PLAYED)) {
      long id = idOf(prefs.getString(StationPreferences.LEGACY_LAST_PLAYED, null), idByMemory);
      if (id != StationPreferences.NONE) {
        editor.putLong(StationPreferences.LAST_PLAYED, id);
      }
    }
    String wakeUp = prefs.getString(wakeUpKey, null);
    if (isLegacy(wakeUp)) {
      // a memory that is empty now: wake up on the last played station instead
      editor.putString(wakeUpKey, String.valueOf(idOf(wakeUp, idByMemory)));
    }
    editor.apply();
  }

  private static boolean isLegacy(String value) {
    return value != null && value.startsWith(LegacyMemoryImport.URL_KEY_PREFIX);
  }

  private static long idOf(String tag, Map<Integer, Long> idByMemory) {
    if (!isLegacy(tag)) {
      return StationPreferences.NONE;
    }
    try {
      Long id =
          idByMemory.get(Integer.parseInt(tag.substring(LegacyMemoryImport.URL_KEY_PREFIX.length())));
      return id == null ? StationPreferences.NONE : id;
    } catch (NumberFormatException e) {
      return StationPreferences.NONE;
    }
  }
}
