package ro.antiprotv.radioclock.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ro.antiprotv.radioclock.BuildConfig;
import ro.antiprotv.radioclock.Stream;
import timber.log.Timber;

/**
 * Talks to radio-browser.info, the community-run station directory the Stream Finder searches.
 *
 * <p>radio-browser has no fixed host: it runs several mirrors and asks clients to pick one from the
 * list at {@link #SERVERS_URL}. It also asks every client to send a User-Agent naming the app. The
 * search is paginated with {@code offset} and {@code limit} and does not report a total, so the
 * caller asks for the next page until one comes back short.
 */
public class RadioBrowserClient {

  /** How many stations one search request asks for. */
  public static final int PAGE_SIZE = 50;

  static final String SERVERS_URL = "https://all.api.radio-browser.info/json/servers";

  /**
   * Used when the mirror list itself cannot be fetched. That happens: in November 2026
   * all.api.radio-browser.info did not resolve at all, and of the mirrors that used to exist only
   * these two answered (fi1 and nl1 were gone).
   */
  static final String[] FALLBACK_SERVERS = {
    "de1.api.radio-browser.info", "de2.api.radio-browser.info",
  };

  private static final int TIMEOUT_MILLIS = 15000;

  /** The filter lists change slowly; a week-old copy is good enough and saves three calls. */
  private static final long LIST_CACHE_MILLIS = 7L * 24 * 60 * 60 * 1000;

  /** How many languages and tags the spinners offer: the most used ones. */
  private static final int TOP_LIST_SIZE = 150;

  // The mirror list, shared by every client. Volley delivers on the main thread, so all of this
  // is only touched there.
  private static List<String> servers;
  private static String currentServer;
  private static boolean discovering;
  private static final List<Runnable> waitingForServer = new ArrayList<>();
  private static final Random random = new Random();

  private final RequestQueue queue;
  private final SharedPreferences prefs;
  private final String userAgent;

  /** Identifies this client's requests, so {@link #cancelAll()} leaves other callers alone. */
  private final Object tag = new Object();

  public RadioBrowserClient(Context context) {
    this.queue = VolleyQueue.get(context);
    this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    this.userAgent = "SimpleRadioClock/" + BuildConfig.VERSION_NAME.replaceFirst("^v", "");
  }

  /** The three filter lists the Stream Finder offers. */
  public enum FilterList {
    COUNTRIES("/json/countries?hidebroken=true&order=name"),
    LANGUAGES(
        "/json/languages?hidebroken=true&order=stationcount&reverse=true&limit=" + TOP_LIST_SIZE),
    TAGS("/json/tags?hidebroken=true&order=stationcount&reverse=true&limit=" + TOP_LIST_SIZE);

    final String path;

    FilterList(String path) {
      this.path = path;
    }

    String cacheKey() {
      // ".v2": lists cached by the first build were decoded as ISO-8859-1, mangling diacritics
      return "finder.cache.v2." + name().toLowerCase(Locale.ROOT);
    }
  }

  /** One spinner entry: what the user sees, and what goes into the search. */
  public static final class Option {
    public final String label;
    public final String value;

    public Option(String label, String value) {
      this.label = label;
      this.value = value;
    }

    @Override
    public String toString() {
      return label;
    }
  }

  /** How the results are sorted; {@link #value} is what radio-browser's {@code order} takes. */
  public enum Order {
    /** By click count, highest first: what people actually listen to. */
    POPULAR("clickcount", true),
    ALPHABETICAL("name", false);

    public final String value;
    final boolean reverse;

    Order(String value, boolean reverse) {
      this.value = value;
      this.reverse = reverse;
    }

    /** The order saved as {@code value}, or {@link #POPULAR} for anything else. */
    public static Order fromValue(String value) {
      for (Order order : values()) {
        if (order.value.equals(value)) {
          return order;
        }
      }
      return POPULAR;
    }
  }

  /** What to search for. Empty fields are left out of the request. */
  public static final class Query {
    public final String name;

    /** ISO 3166-1 code, as radio-browser's country list gives it. */
    public final String countryCode;

    /** Only used when the country list could not be loaded and a plain name is all we have. */
    public final String countryName;

    public final String language;
    public final String tag;
    public final Order order;

    public Query(
        String name, String countryCode, String countryName, String language, String tag) {
      this(name, countryCode, countryName, language, tag, Order.POPULAR);
    }

    public Query(
        String name,
        String countryCode,
        String countryName,
        String language,
        String tag,
        Order order) {
      this.name = nullToEmpty(name);
      this.countryCode = nullToEmpty(countryCode);
      this.countryName = nullToEmpty(countryName);
      this.language = nullToEmpty(language);
      this.tag = nullToEmpty(tag);
      this.order = order == null ? Order.POPULAR : order;
    }
  }

  public interface StationsCallback {
    /** One page; shorter than the page size means it was the last. */
    void onStations(List<Stream> page);

    /** {@code statusCode} is 0 when the server could not be reached at all. */
    void onError(int statusCode);
  }

  public interface CountryCallback {
    /** {@code country} is null when no station with this stream was found. */
    void onCountry(String country);

    /** radio-browser could not be asked; worth trying again another time. */
    void onError();
  }

  public interface OptionsCallback {
    void onOptions(List<Option> options);

    void onError();
  }

  private interface BodyListener {
    void onBody(String body);
  }

  private interface ErrorListener {
    void onError(int statusCode);
  }

  public void searchStations(Query query, int offset, int limit, StationsCallback cb) {
    get(
        searchPath(query, offset, limit),
        body -> {
          try {
            cb.onStations(parseStations(new JSONArray(body)));
          } catch (JSONException e) {
            Timber.w(e, "Bad station list from radio-browser");
            cb.onError(500);
          }
        },
        cb::onError);
  }

  /** A filter list, from the cache when it is fresh enough, else from radio-browser. */
  public void loadOptions(FilterList list, OptionsCallback cb) {
    String cached = prefs.getString(list.cacheKey(), null);
    long cachedAt = prefs.getLong(list.cacheKey() + ".time", 0);
    if (cached != null && System.currentTimeMillis() - cachedAt < LIST_CACHE_MILLIS) {
      try {
        List<Option> options = parseOptions(list, new JSONArray(cached));
        if (!options.isEmpty()) {
          cb.onOptions(options);
          return;
        }
      } catch (JSONException e) {
        Timber.w(e, "Dropping a broken cached %s list", list);
      }
    }
    get(
        list.path,
        body -> {
          List<Option> options;
          try {
            options = parseOptions(list, new JSONArray(body));
          } catch (JSONException e) {
            Timber.w(e, "Bad %s list from radio-browser", list);
            options = Collections.emptyList();
          }
          if (options.isEmpty()) {
            cb.onError();
            return;
          }
          prefs
              .edit()
              .putString(list.cacheKey(), body)
              .putLong(list.cacheKey() + ".time", System.currentTimeMillis())
              .apply();
          cb.onOptions(options);
        },
        status -> {
          // an old list beats none at all
          if (cached != null) {
            try {
              List<Option> options = parseOptions(list, new JSONArray(cached));
              if (!options.isEmpty()) {
                cb.onOptions(options);
                return;
              }
            } catch (JSONException ignored) {
              // fall through to the error
            }
          }
          cb.onError();
        });
  }

  /**
   * The country of a saved station, found by its name. radio-browser cannot search by stream
   * address (its "byurl" only matches the address it was first given, character for character), so
   * this searches by each of {@code names} in turn, shortened a few ways, and only believes a result
   * whose stream is on the same host as {@code url}: two stations may share a name, they rarely
   * share a server.
   */
  public void findCountry(String url, List<String> names, CountryCallback cb) {
    String streamHost = host(url);
    List<String> queries = new ArrayList<>();
    for (String name : names) {
      for (String variant : nameVariants(name)) {
        if (!queries.contains(variant)) {
          queries.add(variant);
        }
      }
    }
    if (streamHost.isEmpty() || queries.isEmpty()) {
      cb.onCountry(null);
      return;
    }
    findCountry(streamHost, queries, 0, cb);
  }

  private void findCountry(String streamHost, List<String> queries, int next, CountryCallback cb) {
    if (next >= queries.size()) {
      cb.onCountry(null);
      return;
    }
    // hidebroken=false: a station that fails radio-browser's check today is still the same station
    get(
        "/json/stations/search?hidebroken=false&limit=100&name=" + encode(queries.get(next)),
        body -> {
          String country = null;
          try {
            country = countryOnHost(new JSONArray(body), streamHost);
          } catch (JSONException e) {
            Timber.w(e, "Bad station list from radio-browser");
          }
          if (country != null) {
            cb.onCountry(country);
          } else {
            findCountry(streamHost, queries, next + 1, cb);
          }
        },
        status -> cb.onError());
  }

  /**
   * Tells radio-browser the station was picked. Its usage rules ask for this: the click counts are
   * what orders everyone's search results by popularity.
   */
  public void countClick(String stationUuid) {
    if (stationUuid == null || stationUuid.isEmpty()) {
      return;
    }
    get("/json/url/" + encode(stationUuid), body -> {}, status -> {});
  }

  public void cancelAll() {
    queue.cancelAll(tag);
  }

  // --- requests -----------------------------------------------------------------------------

  private void get(String path, BodyListener ok, ErrorListener fail) {
    withServer(() -> send(currentServer, path, ok, fail, true));
  }

  private void send(
      String server, String path, BodyListener ok, ErrorListener fail, boolean mayRetry) {
    Utf8Request request =
        new Utf8Request(
            "https://" + server + path,
            ok::onBody,
            error -> {
              int status = error.networkResponse != null ? error.networkResponse.statusCode : 0;
              Timber.d("radio-browser %s%s failed: %d %s", server, path, status, error);
              if (mayRetry && (status == 0 || status >= 500)) {
                // that mirror may be down: move everyone to another one and try once more
                currentServer = pickServer(server);
                send(currentServer, path, ok, fail, false);
              } else {
                fail.onError(status);
              }
            });
    // Volley's own retry would hit the same dead mirror again; the retry above switches mirrors
    request.setRetryPolicy(
        new DefaultRetryPolicy(TIMEOUT_MILLIS, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    request.setTag(tag);
    queue.add(request);
  }

  /** Runs {@code then} once a mirror is chosen, asking radio-browser for the list the first time. */
  private void withServer(Runnable then) {
    if (currentServer != null) {
      then.run();
      return;
    }
    waitingForServer.add(then);
    if (discovering) {
      return;
    }
    discovering = true;
    Utf8Request request =
        new Utf8Request(
            SERVERS_URL,
            body -> {
              List<String> found = Collections.emptyList();
              try {
                found = parseServers(new JSONArray(body));
              } catch (JSONException e) {
                Timber.w(e, "Bad mirror list from radio-browser");
              }
              serversFound(found);
            },
            error -> {
              Timber.d("radio-browser mirror list failed: %s", error.toString());
              serversFound(Collections.emptyList());
            });
    request.setRetryPolicy(
        new DefaultRetryPolicy(TIMEOUT_MILLIS, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    queue.add(request);
  }

  private static void serversFound(List<String> found) {
    List<String> list = new ArrayList<>(found);
    if (list.isEmpty()) {
      Collections.addAll(list, FALLBACK_SERVERS);
    }
    servers = list;
    currentServer = pickServer(null);
    discovering = false;
    List<Runnable> waiting = new ArrayList<>(waitingForServer);
    waitingForServer.clear();
    for (Runnable r : waiting) {
      r.run();
    }
  }

  /** A random mirror, other than {@code avoid} when there is a choice. */
  private static String pickServer(String avoid) {
    List<String> choices = new ArrayList<>(servers);
    if (choices.size() > 1) {
      choices.remove(avoid);
    }
    return choices.get(random.nextInt(choices.size()));
  }

  /**
   * A GET that always reads the body as UTF-8 and names the app in its User-Agent.
   *
   * <p>radio-browser answers with a bare "application/json", no charset. Volley then falls back to
   * ISO-8859-1, which turns every diacritic into two wrong letters ("muzică" showed as "muzicÄ"
   * plus a box). JSON is UTF-8 by definition (RFC 8259), so the missing charset needs no guessing.
   */
  private class Utf8Request extends StringRequest {
    Utf8Request(
        String url, Response.Listener<String> listener, Response.ErrorListener errorListener) {
      super(Method.GET, url, listener, errorListener);
    }

    @Override
    public Map<String, String> getHeaders() {
      Map<String, String> headers = new HashMap<>();
      headers.put("User-Agent", userAgent);
      return headers;
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
      return Response.success(
          decodeUtf8(response.data), HttpHeaderParser.parseCacheHeaders(response));
    }
  }

  static String decodeUtf8(byte[] data) {
    return new String(data, StandardCharsets.UTF_8);
  }

  // --- pure helpers, unit tested --------------------------------------------------------------

  static String searchPath(Query query, int offset, int limit) {
    StringBuilder path = new StringBuilder("/json/stations/search?hidebroken=true");
    appendIfSet(path, "name", query.name);
    if (!query.countryCode.isEmpty()) {
      appendIfSet(path, "countrycode", query.countryCode);
    } else {
      appendIfSet(path, "country", query.countryName);
    }
    appendIfSet(path, "language", query.language);
    appendIfSet(path, "tag", query.tag);
    path.append("&order=").append(query.order.value);
    path.append("&reverse=").append(query.order.reverse);
    path.append("&offset=").append(offset);
    path.append("&limit=").append(limit);
    return path.toString();
  }

  /** Stations without a stream address are dropped; there is nothing to play. */
  static List<Stream> parseStations(JSONArray array) {
    List<Stream> stations = new ArrayList<>();
    for (int i = 0; i < array.length(); i++) {
      JSONObject station = array.optJSONObject(i);
      if (station == null) {
        continue;
      }
      // url_resolved has playlists (.pls, .m3u) already opened up to the actual stream
      String url = station.optString("url_resolved", "").trim();
      if (url.isEmpty()) {
        url = station.optString("url", "").trim();
      }
      if (url.isEmpty()) {
        continue;
      }
      stations.add(
          new Stream(
              station.optString("name", "").trim(),
              url,
              station.optString("country", "").trim(),
              station.optString("tags", "").trim().replace(",", ", "),
              station.optString("language", "").trim().replace(",", ", "),
              station.optString("stationuuid", ""),
              station.optString("codec", "").trim(),
              station.optInt("bitrate", 0)));
    }
    return stations;
  }

  /** Mirror host names; each one shows up once per IP address, so duplicates are dropped. */
  static List<String> parseServers(JSONArray array) {
    Set<String> names = new LinkedHashSet<>();
    for (int i = 0; i < array.length(); i++) {
      JSONObject server = array.optJSONObject(i);
      String name = server == null ? "" : server.optString("name", "").trim();
      if (!name.isEmpty()) {
        names.add(name.toLowerCase(Locale.ROOT));
      }
    }
    return new ArrayList<>(names);
  }

  static List<Option> parseOptions(FilterList list, JSONArray array) {
    List<Option> options = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    for (int i = 0; i < array.length(); i++) {
      JSONObject item = array.optJSONObject(i);
      if (item == null) {
        continue;
      }
      String name = item.optString("name", "").trim();
      if (name.isEmpty()) {
        continue;
      }
      String value;
      if (list == FilterList.COUNTRIES) {
        value = item.optString("iso_3166_1", "").trim().toUpperCase(Locale.ROOT);
        if (value.isEmpty()) {
          continue;
        }
      } else {
        value = name;
      }
      if (seen.add(value)) {
        options.add(new Option(list == FilterList.COUNTRIES ? name : capitalize(name), value));
      }
    }
    if (list == FilterList.COUNTRIES) {
      // radio-browser's order=name actually sorts countries by their code (Kosovo, "XK", lands
      // between Wallis and Futuna and Yemen), so put them in alphabetical order here
      Collator collator = Collator.getInstance();
      Collections.sort(options, (a, b) -> collator.compare(a.label, b.label));
    }
    return options;
  }

  /**
   * The names to search for: the name itself, the part before " - ", " / " or a comma ("Radio
   * Zodiac - Dublin, Ireland" is "Radio Zodiac" there), and its first two words. Names of two
   * letters or fewer would match half the directory and are skipped.
   */
  static List<String> nameVariants(String name) {
    List<String> variants = new ArrayList<>();
    if (name == null) {
      return variants;
    }
    // a name read from the stream in the wrong encoding has replacement characters in it
    String clean = name.replace('\uFFFD', ' ').trim();
    String first = clean.split("\\s[-/|]\\s|,")[0].trim();
    String[] words = first.split("\\s+");
    String twoWords = words.length > 2 ? words[0] + " " + words[1] : first;
    for (String v : new String[] {clean, first, twoWords}) {
      if (v.length() > 2 && !variants.contains(v)) {
        variants.add(v);
      }
    }
    return variants;
  }

  /**
   * The country most stations streaming from {@code streamHost} give, or null when none does.
   *
   * <p>A vote, not the first match: anyone can add stations to radio-browser, and some entries are
   * wrong. Of 96 "Radio BOB" entries on streams.radiobob.de, one said Spain; the rest, Germany. On
   * a tie the country seen first wins, and the results come most clicked first.
   */
  static String countryOnHost(JSONArray stations, String streamHost) {
    Map<String, Integer> votes = new HashMap<>();
    String best = null;
    int bestVotes = 0;
    for (int i = 0; i < stations.length(); i++) {
      JSONObject station = stations.optJSONObject(i);
      if (station == null) {
        continue;
      }
      if (streamHost.equals(host(station.optString("url", "")))
          || streamHost.equals(host(station.optString("url_resolved", "")))) {
        String country = station.optString("country", "").trim();
        if (country.isEmpty()) {
          continue;
        }
        int count = votes.containsKey(country) ? votes.get(country) + 1 : 1;
        votes.put(country, count);
        if (count > bestVotes) {
          best = country;
          bestVotes = count;
        }
      }
    }
    return best;
  }

  /** The lower-case host of a stream address; empty when there is none. */
  static String host(String url) {
    try {
      String host = new URI(url.trim()).getHost();
      return host == null ? "" : host.toLowerCase(Locale.ROOT);
    } catch (URISyntaxException | RuntimeException e) {
      return "";
    }
  }

  private static void appendIfSet(StringBuilder path, String key, String value) {
    if (!value.isEmpty()) {
      path.append('&').append(key).append('=').append(encode(value));
    }
  }

  static String encode(String value) {
    try {
      // URLEncoder writes form encoding; %20 is a space in any query string
      return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  private static String capitalize(String s) {
    return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s.trim();
  }
}
