package ro.antiprotv.radioclock.service.weather;

import android.content.Context;
import android.net.Uri;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import timber.log.Timber;

/**
 * Talks to Open-Meteo.
 *
 * <p>Open-Meteo was picked over the other free services because it needs no API key: there is no
 * account to register, nothing to embed in the APK, and nothing that can be revoked out from under
 * an installed copy of the app. Non-commercial use is free and unmetered.
 */
public class WeatherRepository {

  private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";
  private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";

  /** How many days the bar shows: today plus the next two. */
  public static final int FORECAST_DAYS = 3;

  private static final int TIMEOUT_MILLIS = 15000;

  /**
   * One queue for the whole process. Volley starts five dispatcher threads per queue and its disk
   * cache is one directory, so a queue per caller would both pile up threads and leave two caches
   * writing over each other - the settings screen searches for a town while the clock screen is
   * still refreshing its forecast.
   */
  private static RequestQueue queue;

  /** Identifies this instance's requests, so cancelling one caller does not cancel the other. */
  private final Object tag = new Object();

  private final RequestQueue requestQueue;

  public WeatherRepository(Context context) {
    this.requestQueue = sharedQueue(context);
  }

  private static synchronized RequestQueue sharedQueue(Context context) {
    if (queue == null) {
      queue = Volley.newRequestQueue(context.getApplicationContext());
    }
    return queue;
  }

  public interface ForecastCallback {
    /** {@code body} is the raw response, for the cache; {@code days} is it already parsed. */
    void onForecast(List<WeatherDay> days, String body);

    void onError(String message);
  }

  public interface SearchCallback {
    /** An empty list means the search ran and matched nothing, which is not an error. */
    void onResults(List<WeatherLocation> results);

    void onError(String message);
  }

  public void fetchForecast(double latitude, double longitude, String unit, ForecastCallback cb) {
    String url =
        Uri.parse(FORECAST_URL)
            .buildUpon()
            .appendQueryParameter("latitude", String.valueOf(latitude))
            .appendQueryParameter("longitude", String.valueOf(longitude))
            .appendQueryParameter(
                "daily",
                "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max")
            // Asked for in the same call as the daily block rather than when the user opens the
            // hour-by-hour panel: three days of it is a few kilobytes, it rides along in the same
            // cache, and the panel then draws the moment it is tapped instead of after a round
            // trip that may not come back at all.
            .appendQueryParameter(
                "hourly", "weather_code,temperature_2m,precipitation_probability")
            // Without this the days come back in UTC, and "today" would turn over at the wrong
            // hour for most of the world.
            .appendQueryParameter("timezone", "auto")
            .appendQueryParameter("forecast_days", String.valueOf(FORECAST_DAYS))
            .appendQueryParameter("temperature_unit", unit)
            .build()
            .toString();

    JsonObjectRequest request =
        new JsonObjectRequest(
            url,
            null,
            response -> {
              List<WeatherDay> days = parseForecast(response);
              if (days.isEmpty()) {
                cb.onError("empty forecast");
              } else {
                cb.onForecast(days, response.toString());
              }
            },
            error -> {
              Timber.d("Weather fetch failed: %s", error.toString());
              cb.onError(error.getMessage() != null ? error.getMessage() : "network error");
            });
    request.setRetryPolicy(
        new DefaultRetryPolicy(TIMEOUT_MILLIS, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    request.setTag(tag);
    requestQueue.add(request);
  }

  public void searchCity(String query, String language, SearchCallback cb) {
    String url =
        Uri.parse(GEOCODING_URL)
            .buildUpon()
            .appendQueryParameter("name", query)
            .appendQueryParameter("count", "10")
            .appendQueryParameter("language", language)
            .appendQueryParameter("format", "json")
            .build()
            .toString();

    JsonObjectRequest request =
        new JsonObjectRequest(
            url,
            null,
            response -> cb.onResults(WeatherLocation.parse(response)),
            error -> {
              Timber.d("City search failed: %s", error.toString());
              cb.onError(error.getMessage() != null ? error.getMessage() : "network error");
            });
    request.setRetryPolicy(
        new DefaultRetryPolicy(TIMEOUT_MILLIS, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    request.setTag(tag);
    requestQueue.add(request);
  }

  /** Drops this caller's requests still in flight; its screen is going away. */
  public void cancelAll() {
    requestQueue.cancelAll(tag);
  }

  /**
   * Turns the {@code daily} block - parallel arrays, one entry per day - into a list. A day missing
   * either its code or its temperatures is skipped rather than drawn half empty.
   */
  public static List<WeatherDay> parseForecast(JSONObject response) {
    List<WeatherDay> days = new ArrayList<>();
    JSONObject daily = response.optJSONObject("daily");
    if (daily == null) {
      return days;
    }
    JSONArray time = daily.optJSONArray("time");
    JSONArray code = daily.optJSONArray("weather_code");
    JSONArray max = daily.optJSONArray("temperature_2m_max");
    JSONArray min = daily.optJSONArray("temperature_2m_min");
    JSONArray rain = daily.optJSONArray("precipitation_probability_max");
    if (time == null || code == null || max == null || min == null) {
      return days;
    }
    int count = Math.min(time.length(), Math.min(code.length(), Math.min(max.length(), min.length())));
    for (int i = 0; i < count; i++) {
      if (max.isNull(i) || min.isNull(i)) {
        continue;
      }
      try {
        days.add(
            new WeatherDay(
                time.getString(i),
                code.optInt(i, -1),
                max.getDouble(i),
                min.getDouble(i),
                // Open-Meteo leaves this null for places and days it has no model for.
                rain == null || rain.isNull(i) ? -1 : rain.getInt(i)));
      } catch (JSONException e) {
        Timber.d("Skipping malformed forecast day %d: %s", i, e.getMessage());
      }
    }
    return days;
  }

  /**
   * Turns the {@code hourly} block - the same parallel-array shape as the daily one, one entry per
   * hour across the whole forecast - into a list. An hour with no temperature is skipped.
   */
  public static List<WeatherHour> parseHourly(JSONObject response) {
    List<WeatherHour> hours = new ArrayList<>();
    JSONObject hourly = response.optJSONObject("hourly");
    if (hourly == null) {
      return hours;
    }
    JSONArray time = hourly.optJSONArray("time");
    JSONArray code = hourly.optJSONArray("weather_code");
    JSONArray temp = hourly.optJSONArray("temperature_2m");
    JSONArray rain = hourly.optJSONArray("precipitation_probability");
    if (time == null || temp == null) {
      return hours;
    }
    int count = Math.min(time.length(), temp.length());
    for (int i = 0; i < count; i++) {
      if (temp.isNull(i)) {
        continue;
      }
      try {
        hours.add(
            new WeatherHour(
                time.getString(i),
                code == null || code.isNull(i) ? -1 : code.getInt(i),
                temp.getDouble(i),
                rain == null || rain.isNull(i) ? -1 : rain.getInt(i)));
      } catch (JSONException e) {
        Timber.d("Skipping malformed forecast hour %d: %s", i, e.getMessage());
      }
    }
    return hours;
  }

  /**
   * How far the forecast's own clock is from UTC, in seconds.
   *
   * <p>Used to work out which hour is "now" at the place the forecast is for, which is not
   * necessarily the hour the device is showing: someone may well keep a clock set to their home
   * town while they are away from it.
   */
  public static int utcOffsetSeconds(JSONObject response) {
    return response.optInt("utc_offset_seconds", 0);
  }
}
