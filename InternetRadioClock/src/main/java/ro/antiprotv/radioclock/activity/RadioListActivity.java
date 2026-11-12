/*
 Copyright Cristian "ciuc" Starasciuc 2016
 cristi.ciuc@gmail.com
*/
package ro.antiprotv.radioclock.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Objects;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.StationListAdapter;
import ro.antiprotv.radioclock.db.Station;
import ro.antiprotv.radioclock.db.StationRepository;
import ro.antiprotv.radioclock.service.ButtonManager;

/**
 * My Radio List: every saved station as a card. The order is the order of the clock's memory
 * buttons (the first card is button 1), so moving a card moves the station to another button.
 * Stations past the last button are only played from Android Auto.
 */
public class RadioListActivity extends AppCompatActivity implements StationListAdapter.Listener {

  private StationRepository stations;
  private StationListAdapter adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_radio_list);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    SystemBarInsets.apply(this);
    Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

    stations = StationRepository.getInstance(this);
    adapter = new StationListAdapter(this);
    RecyclerView list = findViewById(R.id.radio_list);
    list.setAdapter(adapter);
    View empty = findViewById(R.id.radio_list_empty);
    stations
        .observeStations()
        .observe(
            this,
            saved -> {
              adapter.submitList(saved);
              empty.setVisibility(saved.isEmpty() ? View.VISIBLE : View.GONE);
            });

    FloatingActionButton add = findViewById(R.id.radio_list_add);
    add.setOnClickListener(v -> showStationDialog(null));
  }

  @Override
  public void onEdit(Station station) {
    showStationDialog(station);
  }

  @Override
  public void onMove(Station station, int offset) {
    stations.move(station.id, offset);
  }

  @Override
  public void onRemove(Station station) {
    new AlertDialog.Builder(this)
        .setMessage(getString(R.string.radio_list_remove_confirm, station.label))
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.radio_list_remove, (d, w) -> stations.remove(station.id))
        .show();
  }

  /** Edits a station, or adds a new one at the end of the list when {@code station} is null. */
  private void showStationDialog(@Nullable Station station) {
    View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_station, null);
    TextInputLayout urlLayout = view.findViewById(R.id.station_url_layout);
    TextInputEditText urlInput = view.findViewById(R.id.station_url_input);
    TextInputEditText labelInput = view.findViewById(R.id.station_label_input);
    if (station != null) {
      urlInput.setText(station.url);
      labelInput.setText(station.label);
    }
    // the "enter the url" error goes away as soon as there is something to save
    urlInput.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            urlLayout.setError(null);
          }

          @Override
          public void afterTextChanged(Editable s) {}
        });
    AlertDialog.Builder builder =
        new AlertDialog.Builder(this)
            .setTitle(station == null ? R.string.radio_list_add : R.string.radio_list_edit)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.radio_list_save, null);
    int newPosition = adapter.getItemCount() + 1;
    if (station == null && newPosition > ButtonManager.MEMORIES) {
      // past the clock's buttons: say so before the user adds it
      builder.setMessage(getString(R.string.add_android_auto_only, newPosition));
    }
    AlertDialog dialog = builder.show();
    // set after show(), so a missing url keeps the dialog open
    dialog
        .getButton(AlertDialog.BUTTON_POSITIVE)
        .setOnClickListener(
            v -> {
              String url = text(urlInput);
              if (url.isEmpty()) {
                urlLayout.setError(getString(R.string.radio_list_url_required));
                return;
              }
              String label = text(labelInput);
              if (station == null) {
                stations.add(
                    url, label.isEmpty() ? String.valueOf(adapter.getItemCount() + 1) : label);
              } else {
                stations.edit(station.id, url, label.isEmpty() ? station.label : label);
              }
              dialog.dismiss();
            });
  }

  private static String text(TextInputEditText input) {
    return input.getText() == null ? "" : input.getText().toString().trim();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      getOnBackPressedDispatcher().onBackPressed();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
