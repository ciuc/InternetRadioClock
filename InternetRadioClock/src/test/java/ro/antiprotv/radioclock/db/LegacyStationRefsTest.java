package ro.antiprotv.radioclock.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import ro.antiprotv.radioclock.FakeSharedPreferences;

public class LegacyStationRefsTest {

  private static final String WAKE_UP = "setting.key.wake_up_station";

  private FakeSharedPreferences prefs;
  /** Memories 2, 4 and 7 had stations; their ids differ from the memory numbers. */
  private final Map<Integer, Long> idByMemory = new HashMap<>();

  @Before
  public void setUp() {
    prefs = new FakeSharedPreferences();
    idByMemory.put(2, 21L);
    idByMemory.put(4, 41L);
    idByMemory.put(7, 71L);
  }

  private void convert() {
    LegacyStationRefs.convert(prefs, WAKE_UP, idByMemory);
  }

  @Test
  public void lastPlayedMemoryBecomesItsStation() {
    prefs.edit().putString("LAST_PLAYED", "setting.key.stream7").apply();

    convert();

    assertEquals(71L, prefs.getLong(StationPreferences.LAST_PLAYED, 0));
    // left for a downgrade
    assertEquals("setting.key.stream7", prefs.getString("LAST_PLAYED", null));
  }

  @Test
  public void lastPlayedAlreadyConvertedIsKept() {
    prefs.edit().putString("LAST_PLAYED", "setting.key.stream7").apply();
    prefs.edit().putLong(StationPreferences.LAST_PLAYED, 21L).apply();

    convert();

    assertEquals(21L, prefs.getLong(StationPreferences.LAST_PLAYED, 0));
  }

  @Test
  public void lastPlayedOnAnEmptyMemoryIsDropped() {
    prefs.edit().putString("LAST_PLAYED", "setting.key.stream5").apply();

    convert();

    assertFalse(prefs.contains(StationPreferences.LAST_PLAYED));
  }

  @Test
  public void nothingToConvert() {
    convert();

    assertFalse(prefs.contains(StationPreferences.LAST_PLAYED));
    assertFalse(prefs.contains(WAKE_UP));
  }

  @Test
  public void wakeUpMemoryBecomesItsStation() {
    prefs.edit().putString(WAKE_UP, "setting.key.stream4").apply();

    convert();

    assertEquals("41", prefs.getString(WAKE_UP, null));
  }

  @Test
  public void wakeUpOnAnEmptyMemoryFallsBackToLastPlayed() {
    prefs.edit().putString(WAKE_UP, "setting.key.stream5").apply();

    convert();

    assertEquals(StationPreferences.WAKE_UP_LAST_PLAYED, prefs.getString(WAKE_UP, null));
  }

  @Test
  public void wakeUpAlreadyAnIdIsKept() {
    prefs.edit().putString(WAKE_UP, "4").apply();

    convert();

    assertEquals("4", prefs.getString(WAKE_UP, null));
  }
}
