package ro.antiprotv.radioclock.service.weather;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
          + "\"precipitation_probability\":[0,7,40],"
          + "\"is_day\":[0,0,1]}}";

  @Test
  public void parsesEveryHour() throws Exception {
    List<WeatherHour> hours = WeatherRepository.parseHourly(new JSONObject(HOURLY_RESPONSE));

    assertEquals(3, hours.size());
    assertEquals("2026-09-15T01:00", hours.get(1).time);
    assertEquals(45, hours.get(1).weatherCode);
    assertEquals(14.9, hours.get(1).temperature, 0.001);
    assertEquals(7, hours.get(1).precipitationChance);
  }

  /** Which icon an hour gets - the sun one or the moon one - comes from the service, not the hour. */
  @Test
  public void parsesWhetherEachHourIsDaylight() throws Exception {
    List<WeatherHour> hours = WeatherRepository.parseHourly(new JSONObject(HOURLY_RESPONSE));

    assertFalse(hours.get(0).isDay);
    assertFalse(hours.get(1).isDay);
    assertTrue(hours.get(2).isDay);
  }

  /** A forecast cached before daylight was asked for draws as it always did, in daytime icons. */
  @Test
  public void hoursWithoutDaylightAreTreatedAsDay() throws Exception {
    String body =
        "{\"hourly\":{\"time\":[\"2026-09-15T02:00\"],\"weather_code\":[0],"
            + "\"temperature_2m\":[10.0]}}";

    List<WeatherHour> hours = WeatherRepository.parseHourly(new JSONObject(body));

    assertTrue(hours.get(0).isDay);
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

  // -----------------------------------------------------------------------------------------
  // The current block, which the "Now" column and its detail panel draw from
  // -----------------------------------------------------------------------------------------

  /** The current block as Open-Meteo returns it, trimmed to the fields the app asks for. */
  private static final String CURRENT_RESPONSE =
      "{\"current\":{\"time\":\"2026-09-17T20:30\",\"weather_code\":1,"
          + "\"temperature_2m\":21.8,\"relative_humidity_2m\":48,\"pressure_msl\":1014.4,"
          + "\"wind_speed_10m\":4.7,\"wind_direction_10m\":148,\"uv_index\":0.0,"
          + "\"is_day\":0}}";

  @Test
  public void parsesTheCurrentReading() throws Exception {
    WeatherNow now = WeatherRepository.parseCurrent(new JSONObject(CURRENT_RESPONSE));

    assertEquals(1, now.weatherCode);
    assertEquals(21.8, now.temperature, 0.001);
    assertEquals(48, now.humidity);
    assertEquals(1014.4, now.pressure, 0.001);
    assertEquals(4.7, now.windSpeed, 0.001);
    assertEquals(148, now.windDirection);
    // Nought all night, and a real reading rather than a missing one.
    assertEquals(0.0, now.uvIndex, 0.001);
    assertFalse(now.isDay);
  }

  /** A reading the service left out is a row the panel should leave out, not one that reads zero. */
  @Test
  public void absentReadingsAreNotZero() throws Exception {
    String body = "{\"current\":{\"weather_code\":0,\"temperature_2m\":5.0,\"is_day\":1}}";

    WeatherNow now = WeatherRepository.parseCurrent(new JSONObject(body));

    assertEquals(-1, now.humidity);
    assertTrue(Double.isNaN(now.pressure));
    assertTrue(Double.isNaN(now.windSpeed));
    assertEquals(-1, now.windDirection);
    assertTrue(Double.isNaN(now.uvIndex));
    assertTrue(now.isDay);
  }

  /**
   * A forecast cached by a build from before the "Now" column existed has no such block. The
   * manager takes null as its cue to drop that cache and fetch a fresh one.
   */
  @Test
  public void bodyWithoutCurrentYieldsNull() throws Exception {
    assertNull(WeatherRepository.parseCurrent(new JSONObject(RESPONSE)));
    assertNull(WeatherRepository.parseCurrent(new JSONObject("{\"current\":{}}")));
    assertNull(
        WeatherRepository.parseCurrent(
            new JSONObject("{\"current\":{\"temperature_2m\":null}}")));
  }

  /** Which hour is "now" is worked out from the forecast's own offset, not the device's. */
  @Test
  public void readsTheForecastsUtcOffset() throws Exception {
    assertEquals(10800, WeatherRepository.utcOffsetSeconds(new JSONObject(HOURLY_RESPONSE)));
    // Absent - an error body, or an older cache - counts as UTC rather than throwing.
    assertEquals(0, WeatherRepository.utcOffsetSeconds(new JSONObject("{}")));
  }
}
