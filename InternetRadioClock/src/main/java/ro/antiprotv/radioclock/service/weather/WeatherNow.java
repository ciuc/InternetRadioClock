package ro.antiprotv.radioclock.service.weather;

/**
 * The weather at this moment, as Open-Meteo's {@code current} block reports it.
 *
 * <p>Everything past the temperature and the code is only ever read by the detail panel, but it all
 * arrives in the same response the bar is drawn from, so it is parsed here rather than fetched
 * again when the panel opens.
 *
 * <p>Fields the service left out come back as {@link Double#NaN} or -1 rather than as zero: a
 * missing humidity and a humidity of nought look the same on screen otherwise, and the first is a
 * row that should not be drawn at all.
 */
public class WeatherNow {

  /** WMO weather interpretation code; see {@link WeatherCodes}. */
  public final int weatherCode;

  /** Already in the unit the user picked, so nothing downstream has to convert. */
  public final double temperature;

  /** Relative humidity in percent, or -1 when the service did not supply one. */
  public final int humidity;

  /** Mean sea level pressure in hPa - what forecasts quote - or {@link Double#NaN}. */
  public final double pressure;

  /** Wind speed in the unit asked for alongside the temperature, or {@link Double#NaN}. */
  public final double windSpeed;

  /** Where the wind is coming from, in degrees clockwise from north, or -1. */
  public final int windDirection;

  /** UV index, or {@link Double#NaN}. Zero is a real reading - it is nought all night. */
  public final double uvIndex;

  /**
   * Whether the sun is up where the forecast is for. Decides between the day and night icon, and
   * is the service's own answer rather than a guess from the hour, so it holds at the latitudes
   * where the two part company.
   */
  public final boolean isDay;

  public WeatherNow(
      int weatherCode,
      double temperature,
      int humidity,
      double pressure,
      double windSpeed,
      int windDirection,
      double uvIndex,
      boolean isDay) {
    this.weatherCode = weatherCode;
    this.temperature = temperature;
    this.humidity = humidity;
    this.pressure = pressure;
    this.windSpeed = windSpeed;
    this.windDirection = windDirection;
    this.uvIndex = uvIndex;
    this.isDay = isDay;
  }
}
