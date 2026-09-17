package ro.antiprotv.radioclock.service.weather;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import ro.antiprotv.radioclock.R;

/**
 * Maps a WMO weather interpretation code - what Open-Meteo reports - onto one of the nine icons the
 * app ships.
 *
 * <p>The codes are grouped rather than mapped one to one: at the size the bar draws them, "slight
 * rain" and "heavy rain" are the same picture, and the temperatures underneath carry the detail.
 *
 * <p>Two of the nine come in a night version as well. Only those two ever drew the sun, and a sun
 * over a clock at three in the morning is simply wrong; the other seven look the same whatever the
 * hour, so there is nothing to swap.
 */
public final class WeatherCodes {

  private WeatherCodes() {}

  /** The daytime icon. For anything tied to a particular hour, prefer {@link #icon(int, boolean)}. */
  @DrawableRes
  public static int icon(int code) {
    return icon(code, true);
  }

  /**
   * @param isDay whether the sun is up at the moment being drawn, as the service reports it; the
   *     three-day bar passes true, because a whole day is a daytime thing however late it is read
   */
  @DrawableRes
  public static int icon(int code, boolean isDay) {
    switch (code) {
      case 0: // clear sky
      case 1: // mainly clear
        return isDay ? R.drawable.ic_weather_clear : R.drawable.ic_weather_clear_night;
      case 2: // partly cloudy
        return isDay
            ? R.drawable.ic_weather_partly_cloudy
            : R.drawable.ic_weather_partly_cloudy_night;
      case 3: // overcast
        return R.drawable.ic_weather_cloudy;
      case 45: // fog
      case 48: // depositing rime fog
        return R.drawable.ic_weather_fog;
      case 51: // drizzle, light
      case 53: // drizzle, moderate
      case 55: // drizzle, dense
      case 56: // freezing drizzle, light
      case 57: // freezing drizzle, dense
        return R.drawable.ic_weather_drizzle;
      case 61: // rain, slight
      case 63: // rain, moderate
      case 65: // rain, heavy
      case 66: // freezing rain, light
      case 67: // freezing rain, heavy
      case 80: // rain showers, slight
      case 81: // rain showers, moderate
      case 82: // rain showers, violent
        return R.drawable.ic_weather_rain;
      case 71: // snow fall, slight
      case 73: // snow fall, moderate
      case 75: // snow fall, heavy
      case 77: // snow grains
      case 85: // snow showers, slight
      case 86: // snow showers, heavy
        return R.drawable.ic_weather_snow;
      case 95: // thunderstorm
      case 96: // thunderstorm with slight hail
      case 99: // thunderstorm with heavy hail
        return R.drawable.ic_weather_thunderstorm;
      default:
        // Open-Meteo may add codes; an unknown one is still weather, so show the neutral icon
        // rather than an empty slot.
        return R.drawable.ic_weather_cloudy;
    }
  }

  /**
   * Spoken description of the same nine groups, for the icon's content description. The night
   * icons share the description of their daytime twin: it is still clear, or still partly cloudy.
   */
  @StringRes
  public static int description(int code) {
    int icon = icon(code, true);
    if (icon == R.drawable.ic_weather_clear) {
      return R.string.weather_condition_clear;
    }
    if (icon == R.drawable.ic_weather_partly_cloudy) {
      return R.string.weather_condition_partly_cloudy;
    }
    if (icon == R.drawable.ic_weather_fog) {
      return R.string.weather_condition_fog;
    }
    if (icon == R.drawable.ic_weather_drizzle) {
      return R.string.weather_condition_drizzle;
    }
    if (icon == R.drawable.ic_weather_rain) {
      return R.string.weather_condition_rain;
    }
    if (icon == R.drawable.ic_weather_snow) {
      return R.string.weather_condition_snow;
    }
    if (icon == R.drawable.ic_weather_thunderstorm) {
      return R.string.weather_condition_thunderstorm;
    }
    return R.string.weather_condition_cloudy;
  }
}
