package ro.antiprotv.radioclock.db;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.HashMap;
import java.util.Map;
import ro.antiprotv.radioclock.R;
import timber.log.Timber;

@Database(
    entities = {Station.class},
    version = 2,
    exportSchema = true)
public abstract class RadioClockDatabase extends RoomDatabase {

  private static final String NAME = "radioclock.db";
  private static volatile RadioClockDatabase instance;

  public abstract StationDao stationDao();

  public static RadioClockDatabase getInstance(Context context) {
    if (instance == null) {
      synchronized (RadioClockDatabase.class) {
        if (instance == null) {
          Context app = context.getApplicationContext();
          instance =
              Room.databaseBuilder(app, RadioClockDatabase.class, NAME)
                  // the station list is a handful of rows read when the clock and the car
                  // screen are built, the way the shared preferences used to be
                  .allowMainThreadQueries()
                  .addCallback(new ImportLegacyMemories(app))
                  .addMigrations(new CloseGaps(app))
                  .build();
        }
      }
    }
    return instance;
  }

  /** Moves the memory buttons out of the shared preferences, once, when the database is made. */
  private static class ImportLegacyMemories extends Callback {
    private final Context context;

    ImportLegacyMemories(Context context) {
      this.context = context;
    }

    @Override
    public void onCreate(@NonNull SupportSQLiteDatabase db) {
      Resources res = context.getResources();
      String[] defaultUrls = {
        res.getString(R.string.setting_default_stream1),
        res.getString(R.string.setting_default_stream2),
        res.getString(R.string.setting_default_stream3),
        res.getString(R.string.setting_default_stream4),
        res.getString(R.string.setting_default_stream5),
        "",
        "",
        ""
      };
      String[] defaultLabels = new String[LegacyMemoryImport.LEGACY_MEMORIES];
      for (int i = 0; i < defaultLabels.length; i++) {
        defaultLabels[i] = String.valueOf(i + 1);
      }
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      Map<Integer, Station> stations =
          LegacyMemoryImport.read(prefs.getAll(), defaultUrls, defaultLabels);
      Map<Integer, Long> idByMemory = new HashMap<>();
      for (Map.Entry<Integer, Station> memory : stations.entrySet()) {
        Station station = memory.getValue();
        ContentValues values = new ContentValues();
        values.put("position", station.position);
        values.put("url", station.url);
        values.put("label", station.label);
        values.put("station_name", station.stationName);
        idByMemory.put(
            memory.getKey(), db.insert("stations", SQLiteDatabase.CONFLICT_ABORT, values));
      }
      LegacyStationRefs.convert(
          prefs, context.getString(R.string.setting_key_wake_up_station), idByMemory);
      Timber.i("Imported %d stations from the old settings", stations.size());
    }
  }

  /**
   * Version 1 kept a station on the position of its memory button, so a memory left empty was a
   * gap. From version 2 the stations run 1, 2, 3... without gaps. The old button references are
   * converted first, while positions still match the buttons they were saved for.
   */
  private static class CloseGaps extends Migration {
    private final Context context;

    CloseGaps(Context context) {
      super(1, 2);
      this.context = context;
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
      Map<Integer, Long> idByMemory = new HashMap<>();
      try (Cursor cursor = db.query("SELECT id, position FROM stations")) {
        while (cursor.moveToNext()) {
          idByMemory.put(cursor.getInt(1), cursor.getLong(0));
        }
      }
      LegacyStationRefs.convert(
          PreferenceManager.getDefaultSharedPreferences(context),
          context.getString(R.string.setting_key_wake_up_station),
          idByMemory);
      db.execSQL(
          "UPDATE stations SET position = 1 + (SELECT COUNT(*) FROM stations AS s"
              + " WHERE s.position < stations.position"
              + " OR (s.position = stations.position AND s.id < stations.id))");
    }
  }
}
