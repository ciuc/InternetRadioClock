package ro.antiprotv.radioclock;

import android.net.Uri;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;

import timber.log.Timber;

public class HttpRequestManager {

    private StreamFinderActivity context;

    HttpRequestManager(StreamFinderActivity ctx) {
        this.context = ctx;

    }

    protected void getStations(String country, String name, String language, String tags) {
        RequestQueue queue = Volley.newRequestQueue(context);
        Timber.d("requesting stations for %s %s %s %s", country, name, language, tags);
        StringBuilder requestParams = new StringBuilder("http://www.antiprotv.ro/radioclock/api.php?x=list&country=" + Uri.encode(country));
        if (!name.isEmpty()) {
            requestParams.append("&name=" + name);
        }
        if (!language.isEmpty() && !language.equals("Any")) {
            requestParams.append("&language=" + Uri.encode(language));
        }
        if (!tags.isEmpty()) {
            requestParams.append("&tags=" + Uri.encode(tags));
        };
        Timber.d("URL: %s", requestParams.toString());
        ResponseListener responseListener = new ResponseListener(context);
        JsonArrayRequest request = new JsonArrayRequest(requestParams.toString(),
                responseListener, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                int statusCode = 500;
                if (error.networkResponse != null) {
                    statusCode = error.networkResponse.statusCode;
                    Timber.e("Error %d", error.networkResponse.statusCode);
                }

                switch (statusCode) {
                    case 404:
                    Toast.makeText(context, String.format("No radios matching the criteria! Please try again. ", statusCode), Toast.LENGTH_LONG).show();
                    break;
                    case 500:
                    Toast.makeText(context, String.format("Something went wrong while retrieving list. Please try again later. (error %d) ", statusCode), Toast.LENGTH_LONG).show();
                    break;
                }
            }
        });

        queue.add(request);

    }

    private class ResponseListener implements Response.Listener<JSONArray> {
        StreamFinderActivity activity;
        JSONArray response;

        ResponseListener(StreamFinderActivity activity) {
            this.activity = activity;
        }

        public JSONArray getResponse() {
            return response;
        }

        @Override
        public void onResponse(JSONArray response) {
            Toast.makeText(context, String.format("Found %d radios...", response.length()), Toast.LENGTH_LONG).show();
            activity.fillInStreams(response);
        }
    }

    ;
}
