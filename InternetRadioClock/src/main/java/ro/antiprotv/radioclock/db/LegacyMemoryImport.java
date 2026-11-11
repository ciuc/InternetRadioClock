package ro.antiprotv.radioclock.db;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads the stations the app kept in the shared preferences before it had a database: the 8
 * memory buttons, each with a url ("setting.key.streamN") and a label ("setting.key.labelN"), plus
 * the name each stream announced ("car.station.name.URL"), cached for Android Auto.
 *
 * <p>The old preferences are left in place, so a downgrade still finds them.
 */
public final class LegacyMemoryImport {

  public static final int LEGACY_MEMORIES = 8;
  public static final String URL_KEY_PREFIX = "setting.key.stream";
  public static final String LABEL_KEY_PREFIX = "setting.key.label";
  static final String STATION_NAME_PREFIX = "car.station.name.";

  private LegacyMemoryImport() {}

  /**
   * @param prefs everything in the default shared preferences
   * @param defaultUrls the url a memory had when the user never touched it ("" for none), by
   *     memory number - 1
   * @param defaultLabels the label a memory had when the user never touched it, by memory number -
   *     1
   * @return the configured memories by memory number, in order; a memory with no url is left
   *     out, and the stations after it close the gap (their position is their place in the list)
   */
  public static Map<Integer, Station> read(
      Map<String, ?> prefs, String[] defaultUrls, String[] defaultLabels) {
    Map<Integer, Station> stations = new LinkedHashMap<>();
    for (int memory = 1; memory <= LEGACY_MEMORIES; memory++) {
      // like getString(key, default): the default only counts when the key was never written
      String url = string(prefs, URL_KEY_PREFIX + memory, defaultUrls[memory - 1]).trim();
      if (url.isEmpty()) {
        continue;
      }
      String label = string(prefs, LABEL_KEY_PREFIX + memory, defaultLabels[memory - 1]);
      Station station = new Station(stations.size() + 1, url, label);
      Object name = prefs.get(STATION_NAME_PREFIX + url);
      if (name instanceof String && !((String) name).isEmpty()) {
        station.stationName = (String) name;
      }
      stations.put(memory, station);
    }
    return stations;
  }

  private static String string(Map<String, ?> prefs, String key, String defaultValue) {
    if (!prefs.containsKey(key)) {
      return defaultValue;
    }
    Object value = prefs.get(key);
    return value instanceof String ? (String) value : "";
  }
}
