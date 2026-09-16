package ro.antiprotv.radioclock.preference;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.service.weather.WeatherLocation;
import ro.antiprotv.radioclock.service.weather.WeatherRepository;
import ro.antiprotv.radioclock.service.weather.WeatherSettings;

/**
 * Search box plus results list for picking the forecast's location.
 *
 * <p>Picking from the list is what saves, so the dialog has no OK button: a half typed city name is
 * never a location, and the results carry the coordinates the forecast actually needs.
 */
public class WeatherLocationPreferenceDialog extends PreferenceDialogFragmentCompat {

  public static WeatherLocationPreferenceDialog newInstance(String key) {
    WeatherLocationPreferenceDialog fragment = new WeatherLocationPreferenceDialog();
    Bundle bundle = new Bundle(1);
    bundle.putString(ARG_KEY, key);
    fragment.setArguments(bundle);
    return fragment;
  }

  private final List<WeatherLocation> results = new ArrayList<>();
  private WeatherRepository repository;
  private ArrayAdapter<String> adapter;
  private ListView resultsView;
  private TextView messageView;
  private EditText queryView;

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    Context context = requireContext();
    repository = new WeatherRepository(context);

    View view = LayoutInflater.from(context).inflate(R.layout.dialog_weather_location, null);
    queryView = view.findViewById(R.id.weather_city_query);
    messageView = view.findViewById(R.id.weather_city_message);
    resultsView = view.findViewById(R.id.weather_city_results);
    Button searchButton = view.findViewById(R.id.weather_city_search);

    adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, new ArrayList<>());
    resultsView.setAdapter(adapter);

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    queryView.setText(WeatherSettings.getLocationName(context, prefs).split(",")[0]);
    queryView.setSelection(queryView.getText().length());

    searchButton.setOnClickListener(v -> search());
    queryView.setOnEditorActionListener(
        (v, actionId, event) -> {
          if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            search();
            return true;
          }
          return false;
        });
    resultsView.setOnItemClickListener((parent, itemView, position, id) -> choose(position));

    return new AlertDialog.Builder(context)
        .setTitle(R.string.setting_name_weather_location)
        .setView(view)
        .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
        .create();
  }

  private void search() {
    String query = queryView.getText().toString().trim();
    if (TextUtils.isEmpty(query)) {
      return;
    }
    messageView.setText(R.string.weather_location_searching);
    resultsView.setVisibility(View.GONE);
    repository.searchCity(
        query,
        Locale.getDefault().getLanguage(),
        new WeatherRepository.SearchCallback() {
          @Override
          public void onResults(List<WeatherLocation> found) {
            if (!isAdded()) {
              return;
            }
            results.clear();
            results.addAll(found);
            adapter.clear();
            for (WeatherLocation location : found) {
              adapter.add(location.displayName());
            }
            adapter.notifyDataSetChanged();
            if (found.isEmpty()) {
              messageView.setText(getString(R.string.weather_location_no_results, query));
              resultsView.setVisibility(View.GONE);
            } else {
              messageView.setText(R.string.weather_location_pick);
              resultsView.setVisibility(View.VISIBLE);
            }
          }

          @Override
          public void onError(String message) {
            if (!isAdded()) {
              return;
            }
            messageView.setText(R.string.weather_location_search_failed);
            resultsView.setVisibility(View.GONE);
          }
        });
  }

  private void choose(int position) {
    if (position < 0 || position >= results.size()) {
      return;
    }
    Context context = requireContext();
    WeatherSettings.saveLocation(
        context, PreferenceManager.getDefaultSharedPreferences(context), results.get(position));
    if (getPreference() instanceof WeatherLocationPreference) {
      ((WeatherLocationPreference) getPreference()).refreshSummary();
    }
    requireDialog().dismiss();
  }

  @Override
  public void onDestroy() {
    if (repository != null) {
      repository.cancelAll();
    }
    super.onDestroy();
  }

  @Override
  public void onDialogClosed(boolean positiveResult) {
    // Nothing to do: picking a result is what saves, and there is no positive button.
  }
}
