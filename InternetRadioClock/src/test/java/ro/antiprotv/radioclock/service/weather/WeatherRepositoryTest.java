package ro.antiprotv.radioclock.service.weather;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.json.JSONObject;
import org.junit.Test;

/** Checks the forecast parser against the shape Open-Meteo actually returns. */
public class WeatherRepositoryTest {

  /** A real response, trimmed to the fields the app asks for. */
  private static final String RESPONSE =
      "{\"timezone\":\"Europe/Bucharest\",\"daily\":{"
          + "\"time\":[\"2026-09-15\",\"2026-09-16\",\"2026-09-17\"],"
          + "\"weather_code\":[3,3,2],"
          + "\"temperature_2m_max\":[24.6,25.8,26.0],"
          + "\"temperature_2m_min\":[13.0,13.9,13.7],"
          + "\"precipitation_probability_max\":[0,40,95]}}";

  @Test
  public void parsesEveryDay() throws Exception {
    List<WeatherDay> days = WeatherRepository.parseForecast(new JSONObject(RESPONSE));

    assertEquals(3, days.size());
    assertEquals("2026-09-15", days.get(0).date);
    assertEquals(3, days.get(0).weatherCode);
    assertEquals(24.6, days.get(0).tempMax, 0.001);
    assertEquals(13.0, days.get(0).tempMin, 0.001);
    assertEquals(0, days.get(0).precipitationChance);
    assertEquals(95, days.get(2).precipitationChance);
  }

  /** Open-Meteo leaves the chance of rain null where it has no model for it. */
  @Test
  public void missingRainChanceBecomesMinusOne() throws Exception {
    String body =
        "{\"daily\":{\"time\":[\"2026-09-15\"],\"weather_code\":[61],"
            + "\"temperature_2m_max\":[10.0],\"temperature_2m_min\":[2.0],"
            + "\"precipitation_probability_max\":[null]}}";

    List<WeatherDay> days = WeatherRepository.parseForecast(new JSONObject(body));

    assertEquals(1, days.size());
    assertEquals(-1, days.get(0).precipitationChance);
  }

  /** The whole field can be absent too, for locations the probability model does not cover. */
  @Test
  public void absentRainChanceBecomesMinusOne() throws Exception {
    String body =
        "{\"daily\":{\"time\":[\"2026-09-15\"],\"weather_code\":[0],"
            + "\"temperature_2m_max\":[10.0],\"temperature_2m_min\":[2.0]}}";

    List<WeatherDay> days = WeatherRepository.parseForecast(new JSONObject(body));

    assertEquals(1, days.size());
    assertEquals(-1, days.get(0).precipitationChance);
  }

  /** A day without temperatures is skipped rather than drawn half empty. */
  @Test
  public void skipsDaysMissingTemperatures() throws Exception {
    String body =
        "{\"daily\":{\"time\":[\"2026-09-15\",\"2026-09-16\"],\"weather_code\":[0,0],"
            + "\"temperature_2m_max\":[null,11.0],\"temperature_2m_min\":[2.0,3.0],"
            + "\"precipitation_probability_max\":[0,0]}}";

    List<WeatherDay> days = WeatherRepository.parseForecast(new JSONObject(body));

    assertEquals(1, days.size());
    assertEquals("2026-09-16", days.get(0).date);
  }

  /** An error body, or anything else unexpected, must come back empty rather than throw. */
  @Test
  public void unexpectedBodyYieldsNothing() throws Exception {
    assertTrue(
        WeatherRepository.parseForecast(
                new JSONObject("{\"error\":true,\"reason\":\"No data\"}"))
            .isEmpty());
    assertTrue(WeatherRepository.parseForecast(new JSONObject("{\"daily\":{}}")).isEmpty());
  }

  // -----------------------------------------------------------------------------------------
  // The hourly block, which the hour-by-hour panel draws from
  // -----------------------------------------------------------------------------------------

  /** The same response, with the hourly block Open-Meteo returns alongside the daily one. */
  private static final String HOURLY_RESPONSE =
      "{\"utc_offset_seconds\":10800,\"timezone\":\"Europe/Bucharest\",\"hourly\":{"
          + "\"time\":[\"2026-09-15T00:00\",\"2026-09-15T01:00\",\"2026-09-16T00:00\"],"
          + "\"weather_code\":[3,45,2],"
          + "\"temperature_2m\":[15.3,14.9,16.1],"
          + "\"precipitation_probability\":[0,7,40]}}";

  @Test
  public void parsesEveryHour() throws Exception {
    List<WeatherHour> hours = WeatherRepository.parseHourly(new JSONObject(HOURLY_RESPONSE));

    assertEquals(3, hours.size());
    assertEquals("2026-09-15T01:00", hours.get(1).time);
    assertEquals(45, hours.get(1).weatherCode);
    assertEquals(14.9, hours.get(1).temperature, 0.001);
    assertEquals(7, hours.get(1).precipitationChance);
  }

  /** The panel groups the hours by day and heads each column with the hour alone. */
  @Test
  public void splitsTheTimestampIntoDayAndHour() throws Exception {
    List<WeatherHour> hours = WeatherRepository.parseHourly(new JSONObject(HOURLY_RESPONSE));

    assertEquals("2026-09-15", hours.get(1).date());
    assertEquals("01", hours.get(1).hourOfDay());
    assertEquals("2026-09-16", hours.get(2).date());
    assertEquals("00", hours.get(2).hourOfDay());
  }

  /** As in the daily block, an absent chance of rain is -1 rather than a misleading zero. */
  @Test
  public void hourlyMissingRainChanceBecomesMinusOne() throws Exception {
    String body =
        "{\"hourly\":{\"time\":[\"2026-09-15T00:00\",\"2026-09-15T01:00\"],"
            + "\"weather_code\":[0,0],\"temperature_2m\":[10.0,11.0],"
            + "\"precipitation_probability\":[null,5]}}";

    List<WeatherHour> hours = WeatherRepository.parseHourly(new JSONObject(body));

    assertEquals(-1, hours.get(0).precipitationChance);
    assertEquals(5, hours.get(1).precipitationChance);
  }

  /** An hour without a temperature has nothing worth a column. */
  @Test
  public void skipsHoursMissingTemperature() throws Exception {
    String body =
        "{\"hourly\":{\"time\":[\"2026-09-15T00:00\",\"2026-09-15T01:00\"],"
            + "\"weather_code\":[0,0],\"temperature_2m\":[null,11.0]}}";

    List<WeatherHour> hours = WeatherRepository.parseHourly(new JSONObject(body));

    assertEquals(1, hours.size());
    assertEquals("2026-09-15T01:00", hours.get(0).time);
  }

  /**
   * A forecast cached by a build from before the hourly block was asked for still parses; the
   * panel then says there is nothing yet rather than falling over.
   */
  @Test
  public void bodyWithoutHourlyYieldsNothing() throws Exception {
    assertTrue(WeatherRepository.parseHourly(new JSONObject(RESPONSE)).isEmpty());
    assertTrue(WeatherRepository.parseHourly(new JSONObject("{\"hourly\":{}}")).isEmpty());
  }

  /** Which hour is "now" is worked out from the forecast's own offset, not the device's. */
  @Test
  public void readsTheForecastsUtcOffset() throws Exception {
    assertEquals(10800, WeatherRepository.utcOffsetSeconds(new JSONObject(HOURLY_RESPONSE)));
    // Absent - an error body, or an older cache - counts as UTC rather than throwing.
    assertEquals(0, WeatherRepository.utcOffsetSeconds(new JSONObject("{}")));
  }
}
