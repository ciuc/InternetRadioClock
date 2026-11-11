package ro.antiprotv.radioclock.db;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The one place the app reads and changes the saved stations.
 *
 * <p>Reads answer right away (the list is tiny). Changes are written in the background, in the
 * order they were asked for; whoever shows stations watches {@link #observeStations()} to pick
 * them up.
 */
public class StationRepository {

  private static volatile StationRepository instance;

  private final StationDao dao;
  private final ExecutorService writes = Executors.newSingleThreadExecutor();

  StationRepository(StationDao dao) {
    this.dao = dao;
  }

  public static StationRepository getInstance(Context context) {
    if (instance == null) {
      synchronized (StationRepository.class) {
        if (instance == null) {
          instance = new StationRepository(RadioClockDatabase.getInstance(context).stationDao());
        }
      }
    }
    return instance;
  }

  /** All saved stations, by position. */
  public List<Station> getStations() {
    return dao.getAll();
  }

  /** The station on a memory, or null when the memory is empty. */
  @Nullable
  public Station getStation(int position) {
    return dao.getByPosition(position);
  }

  /** The station with this id, or null when it was removed. */
  @Nullable
  public Station getStationById(long id) {
    return dao.getById(id);
  }

  public LiveData<List<Station>> observeStations() {
    return dao.observeAll();
  }

  /** Puts a station on a memory button, replacing the one there (or adding it at the end). */
  public void assign(int position, String url, String label) {
    writes.execute(() -> dao.assign(position, url.trim(), label));
  }

  /** Adds a station at the end of the list. */
  public void add(String url, String label) {
    writes.execute(() -> dao.add(url.trim(), label));
  }

  public void edit(long id, String url, String label) {
    writes.execute(() -> dao.edit(id, url.trim(), label));
  }

  /** Changes a memory's label; ignored when the memory is empty. */
  public void setLabel(int position, String label) {
    writes.execute(() -> dao.setLabel(position, label));
  }

  /** Removes a station; the ones after it move up a place (and a memory button). */
  public void remove(long id) {
    writes.execute(() -> dao.remove(id));
  }

  public void removeAtPosition(int position) {
    writes.execute(() -> dao.removeAtPosition(position));
  }

  /** Swaps a station with the one above (offset -1) or below (+1) it. */
  public void move(long id, int offset) {
    writes.execute(() -> dao.move(id, offset));
  }

  /** Remembers the name a stream announced, for every station playing that stream. */
  public void setStationName(String url, String name) {
    writes.execute(() -> dao.setStationName(url, name));
  }
}
