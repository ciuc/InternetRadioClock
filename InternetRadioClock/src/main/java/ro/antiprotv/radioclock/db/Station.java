package ro.antiprotv.radioclock.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * A radio station the user saved.
 *
 * <p>Only configured stations have a row: a memory with no station simply has none. {@code
 * position} is the station's place in the list; positions 1-8 are the clock's memory buttons.
 */
@Entity(tableName = "stations", indices = {@Index("position")})
public class Station {

  @PrimaryKey(autoGenerate = true)
  public long id;

  /** Where the station sits in the list; 1-8 are the memory buttons on the clock. */
  public int position;

  @NonNull public String url = "";

  /** The short text on the memory button. */
  @NonNull public String label = "";

  /** The name the station gives itself (its "icy-name"), once we asked it; null until then. */
  @Nullable
  @ColumnInfo(name = "station_name")
  public String stationName;

  public Station() {}

  @Ignore
  public Station(int position, @NonNull String url, @NonNull String label) {
    this.position = position;
    this.url = url;
    this.label = label;
  }

  /** The station's own name when it told us one, otherwise the button label. */
  @NonNull
  public String displayName() {
    return stationName != null && !stationName.isEmpty() ? stationName : label;
  }
}
