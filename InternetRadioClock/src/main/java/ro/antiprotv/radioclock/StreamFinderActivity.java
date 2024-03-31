package ro.antiprotv.radioclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class StreamFinderActivity extends AppCompatActivity {
    private List<Stream> streams;
    private StreamListAdapter adapter;
    private SharedPreferences prefs;

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
        //Preselect spinners based on remembered settings
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Spinner countrySpinner = findViewById(R.id.streamFinder_dropdown_country);
        countrySpinner.setSelection(prefs.getInt("finder.selected.country", 0));
        Spinner languageSpinner = findViewById(R.id.streamFinder_dropdown_language);
        languageSpinner.setSelection(prefs.getInt("finder.selected.language", 0));

        adapter = new StreamListAdapter(this, new ButtonManager(this), streams);
        recyclerView.setAdapter(adapter);

        final Button findStreamButton = findViewById(R.id.find_stream);
        findStreamButton.setOnClickListener(view -> getStreams());
        final ImageButton helpButton = findViewById(R.id.streamFinder_help);
        helpButton.setOnClickListener(new OnHelpClickListener());

        final ImageButton hideKeyboard = findViewById(R.id.streamFinder_hidek);
        hideKeyboard.setOnClickListener(new KeyboardHideOnClickListener(this));

    }

    private void getStreams() {
        HttpRequestManager requestManager = new HttpRequestManager(this);
        Toast.makeText(this, "Retrieving radios. This might take a while...", Toast.LENGTH_SHORT).show();
        Spinner countrySpinner = findViewById(R.id.streamFinder_dropdown_country);
        //There is no null here, b/c there is always something selected
        String country = countrySpinner.getSelectedItem().toString();
        prefs.edit().putInt("finder.selected.country", countrySpinner.getSelectedItemPosition()).apply();

        TextView nameTv = findViewById(R.id.streamFinder_textinput_name);
        String name = nameTv.getText() != null ? nameTv.getText().toString() : "";
        Spinner languageSpinner = findViewById(R.id.streamFinder_dropdown_language);
        //There is no null here, b/c there is always something selected
        String language = languageSpinner.getSelectedItem().toString();
        prefs.edit().putInt("finder.selected.language", languageSpinner.getSelectedItemPosition()).apply();
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



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class OnHelpClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(view.getContext());
            dialogBuilder.setTitle("About the StreamFinder");
            dialogBuilder.setView(LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_streamfinder_help, null));
            dialogBuilder.setPositiveButton(R.string.dialog_button_ok, (dialog, which) -> dialog.cancel());
            dialogBuilder.show();
        }
    }

    private static class KeyboardHideOnClickListener implements View.OnClickListener{
        private final StreamFinderActivity activity;
        KeyboardHideOnClickListener(StreamFinderActivity activity) {
            this.activity = activity;

        }

        @Override
        public void onClick(View view) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            //Find the currently focused view, so we can grab the correct window token from it.
            View currentFocus = activity.getCurrentFocus();
            //If no view currently has focus, create a new one, just so we can grab a window token from it
            if (currentFocus == null) {
                currentFocus = new View(activity);
            }
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}
