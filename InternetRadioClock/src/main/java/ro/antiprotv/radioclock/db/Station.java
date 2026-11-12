package ro.antiprotv.radioclock.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.util.Locale;

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

  /**
   * The country the station broadcasts from, as the Stream Finder found it (radio-browser's full
   * name, e.g. "The United Kingdom Of Great Britain And Northern Ireland"); null when unknown.
   */
  @Nullable
  @ColumnInfo(name = "country")
  public String country;

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

  /** "Radio X | UK": the name, and the country when we know it. Used on the car screen. */
  @NonNull
  public String nameWithCountry() {
    String shortCountry = shortCountry(country);
    return shortCountry.isEmpty() ? displayName() : displayName() + " | " + shortCountry;
  }

  /**
   * The country as it fits on a car screen: radio-browser's two longest everyday names become
   * "UK" and "USA"; everything else stays as it is.
   */
  @NonNull
  public static String shortCountry(@Nullable String country) {
    if (country == null) {
      return "";
    }
    String trimmed = country.trim();
    String lower = trimmed.toLowerCase(Locale.ROOT);
    if (lower.startsWith("the ")) {
      lower = lower.substring(4);
    }
    if (lower.startsWith("united kingdom")) {
      return "UK";
    }
    if (lower.startsWith("united states")) {
      return "USA";
    }
    return trimmed;
  }
}
