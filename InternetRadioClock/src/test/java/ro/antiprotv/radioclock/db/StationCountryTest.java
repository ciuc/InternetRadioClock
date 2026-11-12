package ro.antiprotv.radioclock.db;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** The "Name | Country" title the car screen shows. */
public class StationCountryTest {

  @Test
  public void shortensTheLongestEverydayNames() {
    assertEquals(
        "UK", Station.shortCountry("The United Kingdom Of Great Britain And Northern Ireland"));
    assertEquals("UK", Station.shortCountry("United Kingdom"));
    assertEquals("USA", Station.shortCountry("The United States Of America"));
    assertEquals("USA", Station.shortCountry("united states"));
  }

  @Test
  public void leavesOtherCountriesAlone() {
    assertEquals("Romania", Station.shortCountry(" Romania "));
    // not the US: only names starting with "United States" are shortened
    assertEquals("The United Arab Emirates", Station.shortCountry("The United Arab Emirates"));
    assertEquals("", Station.shortCountry(null));
  }

  @Test
  public void titleIsNameBarCountry() {
    Station station = new Station(1, "http://x", "BBC");
    station.stationName = "BBC Radio 1";
    station.country = "The United Kingdom Of Great Britain And Northern Ireland";

    assertEquals("BBC Radio 1 | UK", station.nameWithCountry());
  }

  /** Stations saved before this version, or typed in by hand, have no country. */
  @Test
  public void titleWithoutCountryIsJustTheName() {
    Station station = new Station(1, "http://x", "GRL");

    assertEquals("GRL", station.nameWithCountry());

    station.country = "  ";
    assertEquals("GRL", station.nameWithCountry());
  }
}
