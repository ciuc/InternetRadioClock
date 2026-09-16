package ro.antiprotv.radioclock.service.weather;

/** One hour of the forecast, as the hour-by-hour panel needs it. */
public class WeatherHour {

  /**
   * Local time of the hour as Open-Meteo gives it, {@code yyyy-MM-ddTHH:mm}. It carries no zone
   * because it is already in the location's own time - see the {@code timezone=auto} on the
   * request - so the first ten characters are the day it belongs to and the next two the hour.
   */
  public final String time;

  /** WMO weather interpretation code; see {@link WeatherCodes}. */
  public final int weatherCode;

  /** Already in the unit the user picked, so nothing downstream has to convert. */
  public final double temperature;

  /** Chance of precipitation in percent, or -1 when the service did not supply one. */
  public final int precipitationChance;

  public WeatherHour(String time, int weatherCode, double temperature, int precipitationChance) {
    this.time = time;
    this.weatherCode = weatherCode;
    this.temperature = temperature;
    this.precipitationChance = precipitationChance;
  }

  /** The {@code yyyy-MM-dd} this hour falls on, to match against a {@link WeatherDay#date}. */
  public String date() {
    return time.length() >= 10 ? time.substring(0, 10) : time;
  }

  /** The hour as {@code HH}, for the column heading. */
  public String hourOfDay() {
    return time.length() >= 13 ? time.substring(11, 13) : "";
  }
}
