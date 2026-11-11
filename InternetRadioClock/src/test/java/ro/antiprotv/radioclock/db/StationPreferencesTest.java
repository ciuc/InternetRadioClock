package ro.antiprotv.radioclock.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import ro.antiprotv.radioclock.FakeSharedPreferences;

public class StationPreferencesTest {

  private static final String WAKE_UP = "setting.key.wake_up_station";

  private final List<Station> stations = new ArrayList<>();
  private FakeSharedPreferences prefs;
  private StationPreferences stationPreferences;

  @Before
  public void setUp() {
    prefs = new FakeSharedPreferences();
    // ids deliberately differ from positions, as they do once stations are added and removed
    stations.add(station(11, 1, "GRL"));
    stations.add(station(12, 2, "RTLD"));
    stations.add(station(17, 3, "BRMN"));
    stationPreferences = new StationPreferences(prefs, this::find, WAKE_UP);
  }

  private static Station station(long id, int position, String label) {
    Station station = new Station(position, "http://" + label, label);
    station.id = id;
    return station;
  }

  private Station find(long id) {
    for (Station s : stations) {
      if (s.id == id) {
        return s;
      }
    }
    return null;
  }

  // last played

  @Test
  public void nothingPlayedYet() {
    assertNull(stationPreferences.getLastPlayed());
  }

  @Test
  public void remembersTheLastPlayedStation() {
    stationPreferences.setLastPlayed(find(12));

    assertSame(find(12), stationPreferences.getLastPlayed());
  }

  @Test
  public void lastPlayedFollowsTheStationWhenItMoves() {
    stationPreferences.setLastPlayed(find(12));
    find(12).position = 3;
    find(17).position = 2;

    assertSame(find(12), stationPreferences.getLastPlayed());
  }

  @Test
  public void removedLastPlayedStationIsNothing() {
    stationPreferences.setLastPlayed(find(12));
    stations.remove(find(12));

    assertNull(stationPreferences.getLastPlayed());
  }

  // wake-up station

  @Test
  public void wakeUpDefaultsToLastPlayed() {
    assertEquals(StationPreferences.NONE, stationPreferences.getWakeUpStationId());
    assertNull(stationPreferences.getWakeUpStation());
  }

  @Test
  public void wakeUpOnAStation() {
    prefs.edit().putString(WAKE_UP, "17").apply();

    assertSame(find(17), stationPreferences.getWakeUpStation());
  }

  @Test
  public void removedWakeUpStationFallsBackToLastPlayed() {
    prefs.edit().putString(WAKE_UP, "12").apply();
    stations.remove(find(12));

    assertNull(stationPreferences.getWakeUpStation());
  }

  @Test
  public void unreadableWakeUpFallsBackToLastPlayed() {
    prefs.edit().putString(WAKE_UP, "setting.key.stream2").apply();

    assertEquals(StationPreferences.NONE, stationPreferences.getWakeUpStationId());
  }
}
