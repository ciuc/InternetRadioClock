package ro.antiprotv.radioclock.db;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import ro.antiprotv.radioclock.R;

/**
 * The settings that point at a station: the one played last, and the one the alarm wakes up on.
 *
 * <p>They hold the station's database id, so they follow the station when it changes buttons.
 * Older versions saved the memory button instead; {@link LegacyStationRefs} converts those once.
 */
public class StationPreferences {

  /** Where older versions kept the last played memory button. */
  static final String LEGACY_LAST_PLAYED = "LAST_PLAYED";

  static final String LAST_PLAYED = "last_played_station_id";

  /** The wake-up setting's value for "whatever played last". */
  public static final String WAKE_UP_LAST_PLAYED = "0";

  /** No station: nothing saved yet, or the station was removed. */
  public static final long NONE = 0;

  /** Finds stations by id; the repository in the app, a fake in tests. */
  public interface Lookup {
    @Nullable
    Station byId(long id);
  }

  private final SharedPreferences prefs;
  private final Lookup stations;
  private final String wakeUpKey;

  public StationPreferences(Context context) {
    this(
        PreferenceManager.getDefaultSharedPreferences(context),
        StationRepository.getInstance(context)::getStationById,
        context.getString(R.string.setting_key_wake_up_station));
  }

  StationPreferences(SharedPreferences prefs, Lookup stations, String wakeUpKey) {
    this.prefs = prefs;
    this.stations = stations;
    this.wakeUpKey = wakeUpKey;
  }

  /** The station played last, or null when there is none (or it was removed since). */
  @Nullable
  public Station getLastPlayed() {
    long id = prefs.getLong(LAST_PLAYED, NONE);
    return id == NONE ? null : stations.byId(id);
  }

  public void setLastPlayed(Station station) {
    prefs.edit().putLong(LAST_PLAYED, station.id).apply();
  }

  /**
   * The id of the station the alarm wakes up on, or {@link #NONE} for "whatever played last". The
   * station may have been removed since it was picked.
   */
  public long getWakeUpStationId() {
    String value = prefs.getString(wakeUpKey, WAKE_UP_LAST_PLAYED);
    try {
      return value == null ? NONE : Long.parseLong(value);
    } catch (NumberFormatException e) {
      return NONE;
    }
  }

  /** The station the alarm wakes up on, or null for "whatever played last". */
  @Nullable
  public Station getWakeUpStation() {
    long id = getWakeUpStationId();
    return id == NONE ? null : stations.byId(id);
  }
}
