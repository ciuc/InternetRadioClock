package ro.antiprotv.radioclock.service.weather;

import android.content.Context;
import android.content.SharedPreferences;
import ro.antiprotv.radioclock.R;

/**
 * What the weather feature keeps in the shared preferences: the place and units the two profiles
 * share, and the last forecast we managed to fetch.
 *
 * <p>Whether the bar is shown at all is not here - it is part of the day and night profiles, like
 * the clock colour, and is read through {@link ro.antiprotv.radioclock.service.profile.Profile}.
 *
 * <p>The forecast is cached so the bar has something to draw the moment the clock comes up, instead
 * of a blank gap for as long as the network takes. It is kept as the raw response body - parsing it
 * again on the way out costs nothing and means there is only one parser to keep right.
 */
public final class WeatherSettings {

  /** Caches older than this are stale enough that yesterday's forecast could still be in them. */
  private static final long CACHE_MAX_AGE_MILLIS = 6 * 60 * 60 * 1000L;

  private static final String CACHE_BODY = "weather.cache.body";
  private static final String CACHE_TIME = "weather.cache.time";
  private static final String CACHE_FOR = "weather.cache.for";

  private WeatherSettings() {}

  public static String getLocationName(Context context, SharedPreferences prefs) {
    return prefs.getString(context.getString(R.string.setting_key_weather_location_name), "");
  }

  /** {@link Double#NaN} when no place has been picked yet. */
  public static double getLatitude(Context context, SharedPreferences prefs) {
    return parse(prefs.getString(context.getString(R.string.setting_key_weather_latitude), ""));
  }

  public static double getLongitude(Context context, SharedPreferences prefs) {
    return parse(prefs.getString(context.getString(R.string.setting_key_weather_longitude), ""));
  }

  public static boolean hasLocation(Context context, SharedPreferences prefs) {
    return !Double.isNaN(getLatitude(context, prefs)) && !Double.isNaN(getLongitude(context, prefs));
  }

  /** Either {@code celsius} or {@code fahrenheit}; the value goes straight into the request. */
  public static String getUnit(Context context, SharedPreferences prefs) {
    String unit =
        prefs.getString(context.getString(R.string.setting_key_weather_units), "celsius");
    return "fahrenheit".equals(unit) ? "fahrenheit" : "celsius";
  }

  public static String getUnitSymbol(Context context, SharedPreferences prefs) {
    return "fahrenheit".equals(getUnit(context, prefs)) ? "\u00b0F" : "\u00b0C";
  }

  /**
   * What wind speeds come back in, spelled the way the detail panel prints it. There is no
   * separate setting for it: the request asks for mph alongside Fahrenheit and km/h alongside
   * Celsius, and this has to say the same thing.
   */
  public static String getWindUnitLabel(Context context, SharedPreferences prefs) {
    return context.getString(
        "fahrenheit".equals(getUnit(context, prefs))
            ? R.string.weather_unit_mph
            : R.string.weather_unit_kmh);
  }

  public static void saveLocation(
      Context context, SharedPreferences prefs, WeatherLocation location) {
    prefs
        .edit()
        .putString(
            context.getString(R.string.setting_key_weather_location_name), location.displayName())
        .putString(
            context.getString(R.string.setting_key_weather_latitude),
            String.valueOf(location.latitude))
        .putString(
            context.getString(R.string.setting_key_weather_longitude),
            String.valueOf(location.longitude))
        // The cached forecast belongs to the old place; dropping it here stops the bar from
        // showing another town's weather until the new fetch lands.
        .remove(CACHE_BODY)
        .remove(CACHE_TIME)
        .remove(CACHE_FOR)
        .apply();
  }

  /**
   * The cached response body, or null when there is none, when it is too old, or when it was
   * fetched for a different place or unit.
   */
  public static String getCachedForecast(Context context, SharedPreferences prefs) {
    String body = prefs.getString(CACHE_BODY, null);
    if (body == null) {
      return null;
    }
    if (!cacheKey(context, prefs).equals(prefs.getString(CACHE_FOR, ""))) {
      return null;
    }
    long age = System.currentTimeMillis() - prefs.getLong(CACHE_TIME, 0);
    // A clock set backwards would otherwise make a fresh cache look infinitely old.
    if (age < 0 || age > CACHE_MAX_AGE_MILLIS) {
      return null;
    }
    return body;
  }

  /** How old the usable cache is, or -1 when {@link #getCachedForecast} would return null. */
  public static long getCacheAgeMillis(Context context, SharedPreferences prefs) {
    if (getCachedForecast(context, prefs) == null) {
      return -1;
    }
    return System.currentTimeMillis() - prefs.getLong(CACHE_TIME, 0);
  }

  public static void saveForecast(Context context, SharedPreferences prefs, String body) {
    prefs
        .edit()
        .putString(CACHE_BODY, body)
        .putLong(CACHE_TIME, System.currentTimeMillis())
        .putString(CACHE_FOR, cacheKey(context, prefs))
        .apply();
  }

  private static String cacheKey(Context context, SharedPreferences prefs) {
    return getLatitude(context, prefs)
        + ","
        + getLongitude(context, prefs)
        + ","
        + getUnit(context, prefs);
  }

  private static double parse(String value) {
    if (value == null || value.isEmpty()) {
      return Double.NaN;
    }
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      return Double.NaN;
    }
  }
}
