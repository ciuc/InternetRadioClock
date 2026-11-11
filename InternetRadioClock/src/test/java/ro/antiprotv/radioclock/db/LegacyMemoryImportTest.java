package ro.antiprotv.radioclock.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class LegacyMemoryImportTest {

  private static final String[] DEFAULT_URLS = {
    "http://default/1", "http://default/2", "http://default/3", "http://default/4",
    "http://default/5", "", "", ""
  };
  private static final String[] DEFAULT_LABELS = {"1", "2", "3", "4", "5", "6", "7", "8"};

  @Test
  public void freshInstallGetsTheDefaultStations() {
    Map<Integer, Station> stations =
        LegacyMemoryImport.read(new HashMap<>(), DEFAULT_URLS, DEFAULT_LABELS);

    assertEquals(Arrays.asList(1, 2, 3, 4, 5), new ArrayList<>(stations.keySet()));
    for (int memory = 1; memory <= 5; memory++) {
      Station station = stations.get(memory);
      assertEquals(memory, station.position);
      assertEquals(DEFAULT_URLS[memory - 1], station.url);
      assertEquals(DEFAULT_LABELS[memory - 1], station.label);
      assertNull(station.stationName);
    }
  }

  @Test
  public void keepsWhatTheUserConfigured() {
    Map<String, Object> prefs = new HashMap<>();
    prefs.put("setting.key.stream2", "  http://mine/2  ");
    prefs.put("setting.key.label2", "MINE");
    prefs.put("setting.key.stream7", "http://mine/7");
    prefs.put("setting.key.label7", "SEVN");
    prefs.put("car.station.name.http://mine/7", "Radio Seven");

    Map<Integer, Station> stations = LegacyMemoryImport.read(prefs, DEFAULT_URLS, DEFAULT_LABELS);

    assertEquals(Arrays.asList(1, 2, 3, 4, 5, 7), new ArrayList<>(stations.keySet()));
    Station two = stations.get(2);
    assertEquals("http://mine/2", two.url);
    assertEquals("MINE", two.label);
    Station seven = stations.get(7);
    assertEquals("http://mine/7", seven.url);
    assertEquals("SEVN", seven.label);
    assertEquals("Radio Seven", seven.stationName);
  }

  @Test
  public void clearedMemoriesAreLeftOutAndTheGapsClosed() {
    Map<String, Object> prefs = new HashMap<>();
    // the user removed the default station from memory 1
    prefs.put("setting.key.stream1", "");
    prefs.put("setting.key.label1", "OLD");
    prefs.put("setting.key.stream3", "   ");
    prefs.put("setting.key.stream7", "http://mine/7");

    Map<Integer, Station> stations = LegacyMemoryImport.read(prefs, DEFAULT_URLS, DEFAULT_LABELS);

    // memories 2, 4, 5 and 7 become the first four stations
    assertEquals(Arrays.asList(2, 4, 5, 7), new ArrayList<>(stations.keySet()));
    assertEquals(1, stations.get(2).position);
    assertEquals(2, stations.get(4).position);
    assertEquals(3, stations.get(5).position);
    assertEquals(4, stations.get(7).position);
  }

  @Test
  public void labelFallsBackToTheMemoryNumber() {
    Map<String, Object> prefs = new HashMap<>();
    prefs.put("setting.key.stream6", "http://mine/6");

    Map<Integer, Station> stations = LegacyMemoryImport.read(prefs, DEFAULT_URLS, DEFAULT_LABELS);

    assertEquals("6", stations.get(6).label);
  }
}
