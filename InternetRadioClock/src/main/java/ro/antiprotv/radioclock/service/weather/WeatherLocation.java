package ro.antiprotv.radioclock.service.weather;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** A place the user can pick, as returned by the Open-Meteo geocoding search. */
public class WeatherLocation {
  public final String name;

  /** Region or state. Empty when the service does not know one. */
  public final String admin1;

  public final String country;
  public final double latitude;
  public final double longitude;

  public WeatherLocation(
      String name, String admin1, String country, double latitude, double longitude) {
    this.name = name;
    this.admin1 = admin1;
    this.country = country;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  /**
   * "Springfield, Illinois, United States" - the region matters, because a plain city name brings
   * back a dozen places with the same one.
   */
  public String displayName() {
    StringBuilder sb = new StringBuilder(name);
    if (!TextUtils.isEmpty(admin1)) {
      sb.append(", ").append(admin1);
    }
    if (!TextUtils.isEmpty(country)) {
      sb.append(", ").append(country);
    }
    return sb.toString();
  }

  /** Parses the {@code results} array of a geocoding response. An empty result set is not an error. */
  static List<WeatherLocation> parse(JSONObject response) {
    List<WeatherLocation> locations = new ArrayList<>();
    JSONArray results = response.optJSONArray("results");
    if (results == null) {
      return locations;
    }
    for (int i = 0; i < results.length(); i++) {
      JSONObject result = results.optJSONObject(i);
      if (result == null || !result.has("latitude") || !result.has("longitude")) {
        continue;
      }
      locations.add(
          new WeatherLocation(
              result.optString("name", ""),
              result.optString("admin1", ""),
              result.optString("country", ""),
              result.optDouble("latitude"),
              result.optDouble("longitude")));
    }
    return locations;
  }
}
