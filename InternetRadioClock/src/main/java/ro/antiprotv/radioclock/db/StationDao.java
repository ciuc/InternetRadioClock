package ro.antiprotv.radioclock.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import java.util.List;

/**
 * The saved stations. Their positions always run 1, 2, 3... without gaps: adding puts a station at
 * the end, removing one moves the stations after it up, and moving swaps two neighbours.
 */
@Dao
public interface StationDao {

  @Query("SELECT * FROM stations ORDER BY position")
  List<Station> getAll();

  /** All stations, emitted again whenever any of them changes. */
  @Query("SELECT * FROM stations ORDER BY position")
  LiveData<List<Station>> observeAll();

  @Query("SELECT * FROM stations WHERE position = :position LIMIT 1")
  Station getByPosition(int position);

  @Query("SELECT * FROM stations WHERE id = :id")
  Station getById(long id);

  @Query("SELECT COALESCE(MAX(position), 0) FROM stations")
  int maxPosition();

  @Insert
  long insert(Station station);

  @Update
  void update(Station station);

  @Query("DELETE FROM stations WHERE id = :id")
  void deleteById(long id);

  /** Moves every station after this position up by one. */
  @Query("UPDATE stations SET position = position - 1 WHERE position > :position")
  void closeGapAfter(int position);

  /** Fills in a country; never overwrites one the user's own save already gave the station. */
  @Query("UPDATE stations SET country = :country WHERE id = :id AND country IS NULL")
  void setCountryIfUnknown(long id, String country);

  @Query("UPDATE stations SET station_name = :name WHERE url = :url")
  void setStationName(String url, String name);

  /** Adds a station at the end of the list; returns its id. */
  @Transaction
  default long add(String url, String label) {
    return insert(new Station(maxPosition() + 1, url, label));
  }

  /** Changes a station's stream and label. */
  @Transaction
  default void edit(long id, String url, String label) {
    Station station = getById(id);
    if (station != null) {
      change(station, url, label);
    }
  }

  /**
   * Adds a station found by the Stream Finder at the end of the list, with the name and country
   * the directory gave it; returns its id.
   */
  @Transaction
  default long add(String url, String label, String name, String country) {
    Station station = new Station(maxPosition() + 1, url, label);
    station.stationName = emptyToNull(name);
    station.country = emptyToNull(country);
    return insert(station);
  }

  /** Like {@link #assign(int, String, String)}, with the Stream Finder's name and country. */
  @Transaction
  default void assign(int position, String url, String label, String name, String country) {
    Station station = getByPosition(position);
    if (station == null) {
      add(url, label, name, country);
    } else {
      station.url = url;
      station.label = label;
      station.stationName = emptyToNull(name);
      station.country = emptyToNull(country);
      update(station);
    }
  }

  private static String emptyToNull(String s) {
    return s == null || s.trim().isEmpty() ? null : s.trim();
  }

  /**
   * Puts a station on a memory button, replacing the one there. A memory past the end of the list
   * gets the station added at the end instead, so the list keeps no gaps.
   */
  @Transaction
  default void assign(int position, String url, String label) {
    Station station = getByPosition(position);
    if (station == null) {
      add(url, label);
    } else {
      change(station, url, label);
    }
  }

  private void change(Station station, String url, String label) {
    if (!station.url.equals(url)) {
      // a different stream: the name and country we had belong to the old one
      station.stationName = null;
      station.country = null;
    }
    station.url = url;
    station.label = label;
    update(station);
  }

  /** Changes the label of the station on a memory; does nothing when there is none. */
  @Transaction
  default void setLabel(int position, String label) {
    Station station = getByPosition(position);
    if (station != null) {
      station.label = label;
      update(station);
    }
  }

  /** Removes a station; the ones after it move up a place. */
  @Transaction
  default void remove(long id) {
    Station station = getById(id);
    if (station != null) {
      deleteById(id);
      closeGapAfter(station.position);
    }
  }

  @Transaction
  default void removeAtPosition(int position) {
    Station station = getByPosition(position);
    if (station != null) {
      remove(station.id);
    }
  }

  /**
   * Swaps a station with its neighbour: offset -1 moves it up the list, +1 down. Does nothing at
   * either end.
   */
  @Transaction
  default void move(long id, int offset) {
    Station station = getById(id);
    if (station == null) {
      return;
    }
    Station neighbour = getByPosition(station.position + offset);
    if (neighbour == null) {
      return;
    }
    neighbour.position = station.position;
    station.position += offset;
    update(neighbour);
    update(station);
  }
}
