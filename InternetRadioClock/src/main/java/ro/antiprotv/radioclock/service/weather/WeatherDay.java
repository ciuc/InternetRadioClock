package ro.antiprotv.radioclock.service.weather;

/** One day of the forecast, as the weather bar needs it. */
public class WeatherDay {
  /** ISO date (yyyy-MM-dd) the day belongs to, in the location's own time zone. */
  public final String date;

  /** WMO weather interpretation code; see {@link WeatherCodes}. */
  public final int weatherCode;

  /** Already in the unit the user picked, so nothing downstream has to convert. */
  public final double tempMax;

  public final double tempMin;

  /** Chance of precipitation in percent, or -1 when the service did not supply one. */
  public final int precipitationChance;

  public WeatherDay(
      String date, int weatherCode, double tempMax, double tempMin, int precipitationChance) {
    this.date = date;
    this.weatherCode = weatherCode;
    this.tempMax = tempMax;
    this.tempMin = tempMin;
    this.precipitationChance = precipitationChance;
  }
}
