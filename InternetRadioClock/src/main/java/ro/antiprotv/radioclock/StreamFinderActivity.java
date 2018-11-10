package ro.antiprotv.radioclock;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class StreamFinderActivity extends AppCompatActivity {
    private List<Stream> streams;
    private StreamListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream_finder);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        final RecyclerView recyclerView = findViewById(R.id.stream_list_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        streams = new ArrayList<>();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        adapter = new StreamListAdapter(this, streams);
        recyclerView.setAdapter(adapter);

        final Button findStreamButton = findViewById(R.id.find_stream);
        findStreamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getStreams();
            }
        });
        final ImageButton helpButton = findViewById(R.id.streamFinder_help);
        helpButton.setOnClickListener(new OnHelpClickListener());

    }

    private void getStreams() {
        HttpRequestManager requestManager = new HttpRequestManager(this);
        Toast.makeText(this, "Retrieving radios. This might take a while...", Toast.LENGTH_SHORT).show();
        Spinner countrySpinner = findViewById(R.id.streamFinder_dropdown_country);
        //There is no null here, b/c there is always something selected
        String country = countrySpinner.getSelectedItem().toString();
        TextView nameTv = findViewById(R.id.streamFinder_textinput_name);
        String name = nameTv.getText() != null ? nameTv.getText().toString() : "";
        Spinner languageSpinner = findViewById(R.id.streamFinder_dropdown_language);
        //There is no null here, b/c there is always something selected
        String language = languageSpinner.getSelectedItem().toString();
        Spinner tagsSpinner = findViewById(R.id.streamFinder_dropdown_tags);
        //There is no null here, b/c there is always something selected
        String tags = tagsSpinner.getSelectedItem().toString();
        requestManager.getStations(country, name, language, tags);
    }

    void fillInStreams(JSONArray jsonStations) {
        if (jsonStations != null) {
            streams.clear();
            for (int i = 0; i < jsonStations.length(); i++) {
                try {
                    JSONObject station = (JSONObject) jsonStations.get(i);

                    streams.add(new Stream(station.getString("name").trim(), station.getString("url").trim(), station.getString("country").trim(), station.getString("tags").trim(), station.getString("language")));
                } catch (JSONException e) {
                    //TODO:HANDLE THIS!
                    //e.printStackTrace();
                }
            }
            adapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "Error retrieving station list!", Toast.LENGTH_SHORT).show();
        }
    }

    void assignUrlToMemory(String url, String key) {

        int index = Integer.valueOf(key);
        switch (index) {
            case 1:
                key = getResources().getString(R.string.setting_key_stream1);
                break;
            case 2:
                key = getResources().getString(R.string.setting_key_stream2);
                break;
            case 3:
                key = getResources().getString(R.string.setting_key_stream3);
                break;
            case 4:
                key = getResources().getString(R.string.setting_key_stream4);
                break;
            case 5:
                key = getResources().getString(R.string.setting_key_stream5);
                break;
            case 6:
                key = getResources().getString(R.string.setting_key_stream6);
                break;
            case 7:
                key = getResources().getString(R.string.setting_key_stream7);
                break;
            case 8:
                key = getResources().getString(R.string.setting_key_stream8);
                break;
        }
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putString(key, url).apply();
        Toast.makeText(this, String.format("%s assigned to memory %d", url, index), Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class OnHelpClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(view.getContext());
            dialogBuilder.setTitle("About the StreamFinder");
            dialogBuilder.setView(LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_streamfinder_help, null));
            dialogBuilder.setPositiveButton(R.string.dialog_button_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            dialogBuilder.show();
        }
    }

}
