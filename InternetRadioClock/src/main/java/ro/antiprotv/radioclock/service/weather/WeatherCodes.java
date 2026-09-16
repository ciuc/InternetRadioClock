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
 */
public final class WeatherCodes {

  private WeatherCodes() {}

  @DrawableRes
  public static int icon(int code) {
    switch (code) {
      case 0: // clear sky
      case 1: // mainly clear
        return R.drawable.ic_weather_clear;
      case 2: // partly cloudy
        return R.drawable.ic_weather_partly_cloudy;
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

  /** Spoken description of the same nine groups, for the icon's content description. */
  @StringRes
  public static int description(int code) {
    int icon = icon(code);
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
