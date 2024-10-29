package ro.antiprotv.radioclock.activity;

import android.annotation.SuppressLint;
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
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.Stream;
import ro.antiprotv.radioclock.StreamListAdapter;
import ro.antiprotv.radioclock.service.ButtonManager;
import ro.antiprotv.radioclock.service.HttpRequestManager;

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

    streams = new ArrayList<>();
    // Preselect spinners based on remembered settings
    prefs = PreferenceManager.getDefaultSharedPreferences(this);
    Spinner countrySpinner = findViewById(R.id.streamFinder_dropdown_country);
    countrySpinner.setSelection(prefs.getInt("finder.selected.country", 0));
    Spinner languageSpinner = findViewById(R.id.streamFinder_dropdown_language);
    languageSpinner.setSelection(prefs.getInt("finder.selected.language", 0));

    adapter = new StreamListAdapter(this, new ButtonManager(this), streams);

    final Button findStreamButton = findViewById(R.id.find_stream);
    findStreamButton.setOnClickListener(this::getStreams);
    final ImageButton helpButton = findViewById(R.id.streamFinder_help);
    helpButton.setOnClickListener(new OnHelpClickListener());

    final ImageButton hideKeyboard = findViewById(R.id.streamFinder_hidek);
    hideKeyboard.setOnClickListener(new KeyboardHideOnClickListener(this));
  }

  private void getStreams(View view) {
    // Inflate the custom layout for the dialog
    View dialogView =
        LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_radio_list, null);
    RecyclerView recyclerView = dialogView.findViewById(R.id.stream_list_view);
    recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
    recyclerView.setAdapter(adapter); // Replace myObjectList with your data

    // Build and display the dialog
    AlertDialog dialog =
        new AlertDialog.Builder(view.getContext())
            .setTitle(R.string.dialog_radio_list_title)
            .setView(dialogView)
            .setPositiveButton(R.string.close, null)
            .create();

    HttpRequestManager requestManager = new HttpRequestManager(this);
    Toast.makeText(this, R.string.toast_retrieving_radios, Toast.LENGTH_SHORT).show();
    Spinner countrySpinner = findViewById(R.id.streamFinder_dropdown_country);
    // There is no null here, b/c there is always something selected
    String country = countrySpinner.getSelectedItem().toString();
    prefs
        .edit()
        .putInt("finder.selected.country", countrySpinner.getSelectedItemPosition())
        .apply();

    TextView nameTv = findViewById(R.id.streamFinder_textinput_name);
    String name = nameTv.getText() != null ? nameTv.getText().toString() : "";
    Spinner languageSpinner = findViewById(R.id.streamFinder_dropdown_language);
    // There is no null here, b/c there is always something selected
    String language = languageSpinner.getSelectedItem().toString();
    prefs
        .edit()
        .putInt("finder.selected.language", languageSpinner.getSelectedItemPosition())
        .apply();
    Spinner tagsSpinner = findViewById(R.id.streamFinder_dropdown_tags);
    // There is no null here, b/c there is always something selected
    String tags = tagsSpinner.getSelectedItem().toString();
    requestManager.getStations(country, name, language, tags);

    dialog.show();
  }

  @SuppressLint("NotifyDataSetChanged")
  public void fillInStreams(JSONArray jsonStations) {
    if (jsonStations != null) {
      streams.clear();
      for (int i = 0; i < jsonStations.length(); i++) {
        try {
          JSONObject station = (JSONObject) jsonStations.get(i);

          streams.add(
              new Stream(
                  station.getString("name").trim(),
                  station.getString("url").trim(),
                  station.getString("country").trim(),
                  station.getString("tags").trim(),
                  station.getString("language")));
        } catch (JSONException e) {
          // TODO:HANDLE THIS!
          // e.printStackTrace();
        }
      }
      adapter.notifyDataSetChanged();
    } else {
      Toast.makeText(this, R.string.error_retrieving_station_list, Toast.LENGTH_SHORT).show();
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
      dialogBuilder.setTitle(R.string.about_the_streamfinder);
      dialogBuilder.setView(
          LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_streamfinder_help, null));
      dialogBuilder.setPositiveButton(
          R.string.dialog_button_ok, (dialog, which) -> dialog.cancel());
      dialogBuilder.show();
    }
  }

  private static class KeyboardHideOnClickListener implements View.OnClickListener {
    private final StreamFinderActivity activity;

    KeyboardHideOnClickListener(StreamFinderActivity activity) {
      this.activity = activity;
    }

    @Override
    public void onClick(View view) {
      InputMethodManager imm =
          (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
      // Find the currently focused view, so we can grab the correct window token from it.
      View currentFocus = activity.getCurrentFocus();
      // If no view currently has focus, create a new one, just so we can grab a window token from
      // it
      if (currentFocus == null) {
        currentFocus = new View(activity);
      }
      imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
  }
}
