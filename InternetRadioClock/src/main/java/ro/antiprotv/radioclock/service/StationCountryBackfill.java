package ro.antiprotv.radioclock.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.List;
import ro.antiprotv.radioclock.db.Station;
import ro.antiprotv.radioclock.db.StationRepository;
import timber.log.Timber;

/**
 * Finds the country of stations saved before the app remembered countries, so the car screen can
 * show "Name | Country" for them too.
 *
 * <p>Runs once. The stations are looked up one after another, a few requests each, so the
 * directory is not hit with dozens at once. A station that cannot be found keeps no country; if
 * radio-browser could not be reached at all the run is not marked done and is tried again on the
 * next start.
 */
public final class StationCountryBackfill {

  private static final String PREF_DONE = "stations.country_backfill.v1.done";

  /** Main thread only, like everything Volley calls back on. */
  private static boolean running;

  private final RadioBrowserClient client;
  private final StationRepository stations;
  private final SharedPreferences prefs;
  private final List<Station> pending;
  private int found;
  private boolean failed;

  private StationCountryBackfill(Context context, List<Station> pending) {
    this.client = new RadioBrowserClient(context);
    this.stations = StationRepository.getInstance(context);
    this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    this.pending = pending;
  }

  /** Starts the lookup unless it already ran (or is running). Call on the main thread. */
  public static void runOnce(Context context) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    if (running || prefs.getBoolean(PREF_DONE, false)) {
      return;
    }
    List<Station> pending = new ArrayList<>();
    for (Station station : StationRepository.getInstance(context).getStations()) {
      if (station.country == null && !station.url.isEmpty()) {
        pending.add(station);
      }
    }
    if (pending.isEmpty()) {
      prefs.edit().putBoolean(PREF_DONE, true).apply();
      return;
    }
    running = true;
    new StationCountryBackfill(context.getApplicationContext(), pending).next(0);
  }

  private void next(int index) {
    if (index >= pending.size()) {
      finish();
      return;
    }
    Station station = pending.get(index);
    List<String> names = new ArrayList<>();
    if (station.stationName != null) {
      names.add(station.stationName);
    }
    names.add(station.label);
    client.findCountry(
        station.url,
        names,
        new RadioBrowserClient.CountryCallback() {
          @Override
          public void onCountry(String country) {
            if (country != null) {
              found++;
              stations.setCountryIfUnknown(station.id, country);
            }
            next(index + 1);
          }

          @Override
          public void onError() {
            failed = true;
            next(index + 1);
          }
        });
  }

  private void finish() {
    running = false;
    Timber.i(
        "Country backfill: found %d of %d stations%s",
        found, pending.size(), failed ? ", some lookups failed; will retry" : "");
    if (!failed) {
      prefs.edit().putBoolean(PREF_DONE, true).apply();
    }
  }
}
