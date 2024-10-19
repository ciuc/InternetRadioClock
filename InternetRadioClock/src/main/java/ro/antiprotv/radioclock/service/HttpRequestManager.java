package ro.antiprotv.radioclock.service;

import android.net.Uri;
import android.widget.Toast;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;

import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.activity.StreamFinderActivity;

public class HttpRequestManager {

  private final StreamFinderActivity context;

  public HttpRequestManager(StreamFinderActivity ctx) {
    this.context = ctx;
  }

  public void getStations(String country, String name, String language, String tags) {
    RequestQueue queue = Volley.newRequestQueue(context);
    StringBuilder requestParams =
        new StringBuilder("http://www.antiprotv.ro/radioclock/api.php?x=list&country=")
            .append(Uri.encode(country));
    if (!name.isEmpty()) {
      requestParams.append("&name=").append(name);
    }
    if (!language.isEmpty() && !language.equals(context.getString(R.string.any))) {
      requestParams.append("&language=").append(Uri.encode(language));
    }
    if (!tags.isEmpty() && !tags.equals(context.getString(R.string.any))) {
      requestParams.append("&tags=").append(Uri.encode(tags));
    }
    ResponseListener responseListener = new ResponseListener(context);
    JsonArrayRequest request =
        new JsonArrayRequest(
            requestParams.toString(),
            responseListener,
            error -> {
              int statusCode = 500;
              if (error.networkResponse != null) {
                statusCode = error.networkResponse.statusCode;
              }

              switch (statusCode) {
                case 404:
                  Toast.makeText(
                          context,
                          context.getString(R.string.no_radios_matching),
                          Toast.LENGTH_LONG)
                      .show();
                  break;
                case 500:
                  Toast.makeText(
                          context,
                          String.format(
                              context.getString(R.string.error_retrieving_radios), statusCode),
                          Toast.LENGTH_LONG)
                      .show();
                  break;
              }
            });

    queue.add(request);
  }

  private class ResponseListener implements Response.Listener<JSONArray> {
    final StreamFinderActivity activity;
    JSONArray response;

    ResponseListener(StreamFinderActivity activity) {
      this.activity = activity;
    }

    @Override
    public void onResponse(JSONArray response) {
      Toast.makeText(
              context,
              String.format(activity.getString(R.string.found_radios), response.length()),
              Toast.LENGTH_LONG)
          .show();
      activity.fillInStreams(response);
    }
  }
}
