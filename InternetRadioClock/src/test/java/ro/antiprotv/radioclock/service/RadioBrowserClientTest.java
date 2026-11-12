package ro.antiprotv.radioclock.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.junit.Test;
import ro.antiprotv.radioclock.Stream;
import ro.antiprotv.radioclock.service.RadioBrowserClient.FilterList;
import ro.antiprotv.radioclock.service.RadioBrowserClient.Option;
import ro.antiprotv.radioclock.service.RadioBrowserClient.Query;

/** Checks the request building and the parsers against the shapes radio-browser returns. */
public class RadioBrowserClientTest {

  @Test
  public void searchPathCarriesPagingAndFilters() {
    String path =
        RadioBrowserClient.searchPath(new Query("Kiss FM", "RO", "", "romanian", "pop"), 100, 50);

    assertTrue(path.startsWith("/json/stations/search?hidebroken=true"));
    assertTrue(path.contains("&name=Kiss%20FM"));
    assertTrue(path.contains("&countrycode=RO"));
    assertTrue(path.contains("&language=romanian"));
    assertTrue(path.contains("&tag=pop"));
    assertTrue(path.contains("&order=clickcount&reverse=true"));
    assertTrue(path.endsWith("&offset=100&limit=50"));
    assertFalse(path.contains("&country="));
  }

  @Test
  public void emptyFiltersAreLeftOut() {
    String path = RadioBrowserClient.searchPath(new Query("", "", "", null, " "), 0, 50);

    assertEquals(
        "/json/stations/search?hidebroken=true&order=clickcount&reverse=true&offset=0&limit=50",
        path);
  }

  @Test
  public void alphabeticalOrderSortsByNameAscending() {
    String path =
        RadioBrowserClient.searchPath(
            new Query("", "RO", "", "", "", RadioBrowserClient.Order.ALPHABETICAL), 0, 50);

    assertTrue(path.contains("&order=name&reverse=false"));
    assertFalse(path.contains("clickcount"));
  }

  @Test
  public void unknownSavedOrderFallsBackToMostPopular() {
    assertEquals(RadioBrowserClient.Order.POPULAR, RadioBrowserClient.Order.fromValue("votes"));
    assertEquals(RadioBrowserClient.Order.POPULAR, RadioBrowserClient.Order.fromValue(null));
    assertEquals(
        RadioBrowserClient.Order.ALPHABETICAL, RadioBrowserClient.Order.fromValue("name"));
  }

  /** Without radio-browser's country list there are only names, so the name is searched. */
  @Test
  public void countryNameIsUsedWhenThereIsNoCode() {
    String path =
        RadioBrowserClient.searchPath(new Query("", "", "Côte d'Ivoire", "", ""), 0, 50);

    assertTrue(path.contains("&country=C%C3%B4te%20d%27Ivoire"));
    assertFalse(path.contains("countrycode"));
  }

  /** radio-browser sends no charset; the body must still come out as UTF-8, not ISO-8859-1. */
  @Test
  public void bodyIsReadAsUtf8() throws Exception {
    byte[] wire =
        "[{\"url\":\"http://x\",\"tags\":\"muzică de bun gust,străină și românească\"}]"
            .getBytes("UTF-8");

    List<Stream> stations =
        RadioBrowserClient.parseStations(new JSONArray(RadioBrowserClient.decodeUtf8(wire)));

    assertEquals("muzică de bun gust, străină și românească", stations.get(0).getTags());
  }

  @Test
  public void nameVariantsShortenLongNames() {
    assertEquals(
        Arrays.asList("Radio Zodiac - Dublin, Ireland", "Radio Zodiac"),
        RadioBrowserClient.nameVariants("Radio Zodiac - Dublin, Ireland"));
    assertEquals(
        Arrays.asList("Antenne Niedersachsen On Air", "Antenne Niedersachsen"),
        RadioBrowserClient.nameVariants("Antenne Niedersachsen On Air"));
    // a name garbled by the stream's encoding still finds the station
    assertEquals(Arrays.asList("Apparati"), RadioBrowserClient.nameVariants("Apparati�"));
    // too short to search for
    assertTrue(RadioBrowserClient.nameVariants("9").isEmpty());
    assertTrue(RadioBrowserClient.nameVariants(null).isEmpty());
  }

  @Test
  public void hostIsLowerCaseAndEmptyWhenUnreadable() {
    assertEquals(
        "icecast.radiobremen.de",
        RadioBrowserClient.host("https://IceCast.RadioBremen.de/rb/bremeneins/live/mp3/128"));
    assertEquals("live7.okradio.net", RadioBrowserClient.host("http://live7.okradio.net/;*.mp3"));
    assertEquals("", RadioBrowserClient.host("not a url"));
  }

  /** Two stations may share a name; only the one on the saved stream's server counts. */
  @Test
  public void countryComesOnlyFromAStationOnTheSameHost() throws Exception {
    JSONArray body =
        new JSONArray(
            "[{\"name\":\"ffn\",\"url\":\"http://other.example/ffn\",\"country\":\"Austria\"},"
                + "{\"name\":\"ffn\",\"url\":\"http://x/list.pls\","
                + "\"url_resolved\":\"https://stream.ffn.de/ffn/mp3-128\","
                + "\"country\":\"Germany\"}]");

    assertEquals("Germany", RadioBrowserClient.countryOnHost(body, "stream.ffn.de"));
    assertEquals(null, RadioBrowserClient.countryOnHost(body, "elsewhere.example"));
  }

  /** One mislabeled entry must not outvote the rest; this happened with Radio BOB. */
  @Test
  public void mostStationsOnTheHostDecideTheCountry() throws Exception {
    JSONArray body =
        new JSONArray(
            "[{\"url\":\"http://streams.radiobob.de/a\",\"country\":\"Spain\"},"
                + "{\"url\":\"http://streams.radiobob.de/b\",\"country\":\"\"},"
                + "{\"url\":\"http://streams.radiobob.de/c\",\"country\":\"Germany\"},"
                + "{\"url\":\"http://streams.radiobob.de/d\",\"country\":\"Germany\"}]");

    assertEquals("Germany", RadioBrowserClient.countryOnHost(body, "streams.radiobob.de"));
  }

  @Test
  public void encodesReservedCharacters() {
    assertEquals("rock%26roll%3Dyes", RadioBrowserClient.encode("rock&roll=yes"));
  }

  @Test
  public void parsesStationsPreferringResolvedUrl() throws Exception {
    JSONArray body =
        new JSONArray(
            "[{\"stationuuid\":\"abc\",\"name\":\" Radio X \",\"url\":\"http://x/list.pls\","
                + "\"url_resolved\":\"http://x/stream\",\"country\":\"Romania\","
                + "\"language\":\"romanian,english\",\"tags\":\"pop,rock\","
                + "\"codec\":\"MP3\",\"bitrate\":128},"
                + "{\"stationuuid\":\"def\",\"name\":\"Radio Y\",\"url\":\"http://y/stream\","
                + "\"url_resolved\":\"\"},"
                + "{\"stationuuid\":\"ghi\",\"name\":\"No address\",\"url\":\"\"}]");

    List<Stream> stations = RadioBrowserClient.parseStations(body);

    assertEquals(2, stations.size());
    Stream x = stations.get(0);
    assertEquals("Radio X", x.getName());
    assertEquals("http://x/stream", x.getUrl());
    assertEquals("abc", x.getStationUuid());
    assertEquals("pop, rock", x.getTags());
    assertEquals("romanian, english", x.getLanguage());
    assertEquals("MP3", x.getCodec());
    assertEquals(128, x.getBitrate());
    // missing fields come back empty, not as "null"
    Stream y = stations.get(1);
    assertEquals("http://y/stream", y.getUrl());
    assertEquals("", y.getCountry());
    assertEquals(0, y.getBitrate());
  }

  @Test
  public void mirrorListDropsDuplicates() throws Exception {
    JSONArray body =
        new JSONArray(
            "[{\"ip\":\"1.2.3.4\",\"name\":\"de1.api.radio-browser.info\"},"
                + "{\"ip\":\"2a01::1\",\"name\":\"de1.api.radio-browser.info\"},"
                + "{\"ip\":\"5.6.7.8\",\"name\":\"NL1.api.radio-browser.info\"}]");

    assertEquals(
        Arrays.asList("de1.api.radio-browser.info", "nl1.api.radio-browser.info"),
        RadioBrowserClient.parseServers(body));
  }

  @Test
  public void countriesAreSearchedByCode() throws Exception {
    JSONArray body =
        new JSONArray(
            "[{\"name\":\"Germany\",\"iso_3166_1\":\"DE\",\"stationcount\":5000},"
                + "{\"name\":\"Nowhere\",\"iso_3166_1\":\"\",\"stationcount\":3},"
                + "{\"name\":\"Romania\",\"iso_3166_1\":\"ro\",\"stationcount\":800}]");

    List<Option> options = RadioBrowserClient.parseOptions(FilterList.COUNTRIES, body);

    assertEquals(2, options.size());
    assertEquals("Germany", options.get(0).label);
    assertEquals("DE", options.get(0).value);
    assertEquals("RO", options.get(1).value);
  }

  /** radio-browser's own order follows the codes, which puts Kosovo ("XK") before Yemen. */
  @Test
  public void countriesAreSortedByName() throws Exception {
    JSONArray body =
        new JSONArray(
            "[{\"name\":\"Wallis And Futuna\",\"iso_3166_1\":\"WF\"},"
                + "{\"name\":\"Kosovo\",\"iso_3166_1\":\"XK\"},"
                + "{\"name\":\"Yemen\",\"iso_3166_1\":\"YE\"}]");

    List<Option> options = RadioBrowserClient.parseOptions(FilterList.COUNTRIES, body);

    assertEquals("Kosovo", options.get(0).label);
    assertEquals("Wallis And Futuna", options.get(1).label);
    assertEquals("Yemen", options.get(2).label);
  }

  @Test
  public void languagesAreCapitalizedForDisplayOnly() throws Exception {
    JSONArray body =
        new JSONArray("[{\"name\":\"english\",\"stationcount\":9000},{\"name\":\"\"}]");

    List<Option> options = RadioBrowserClient.parseOptions(FilterList.LANGUAGES, body);

    assertEquals(1, options.size());
    assertEquals("English", options.get(0).label);
    assertEquals("english", options.get(0).value);
  }
}
