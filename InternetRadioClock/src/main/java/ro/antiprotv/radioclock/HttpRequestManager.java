package ro.antiprotv.radioclock;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;

import timber.log.Timber;

public class HttpRequestManager {

    private static String URL = "http://www.radio-browser.info/webservice/json/stations/bycountry/%s?limit=20";
    private StreamFinderActivity context;

    HttpRequestManager(StreamFinderActivity ctx) {
        this.context = ctx;

    }
    protected void getStations(String country){
        RequestQueue queue = Volley.newRequestQueue(context);
        Timber.d("requesting stations for %s", country);
        String url = String.format(URL, country);
        ResponseListener responseListener = new ResponseListener(context);
        JsonArrayRequest request = new JsonArrayRequest(url,
                responseListener, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Timber.e("Error %d", error.networkResponse);
            }
        });

        queue.add(request);

    }

    private class ResponseListener implements Response.Listener<JSONArray> {
        StreamFinderActivity activity;
        ResponseListener(StreamFinderActivity activity) {
            this.activity = activity;
        }

        public JSONArray getResponse() {
            return response;
        }

        JSONArray response;
        @Override
        public void onResponse(JSONArray response) {
            activity.fillInStreams(response);
        }
    };
}
