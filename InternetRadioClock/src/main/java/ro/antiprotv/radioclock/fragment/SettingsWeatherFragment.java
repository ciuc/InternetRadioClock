package ro.antiprotv.radioclock.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.preference.WeatherLocationPreference;
import ro.antiprotv.radioclock.preference.WeatherLocationPreferenceDialog;

/** Settings for the three-day forecast shown next to the clock. */
public class SettingsWeatherFragment extends PreferenceFragmentCompat {

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
    setPreferencesFromResource(R.xml.preferences_settings_weather, rootKey);
  }

  @Override
  public void onDisplayPreferenceDialog(@NonNull Preference preference) {
    if (preference instanceof WeatherLocationPreference) {
      WeatherLocationPreferenceDialog dialog =
          WeatherLocationPreferenceDialog.newInstance(preference.getKey());
      dialog.setTargetFragment(this, 0);
      dialog.show(getParentFragmentManager(), null);
    } else {
      super.onDisplayPreferenceDialog(preference);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    // The location dialog writes straight to the preferences, so the summary is refreshed here
    // as well for the case where the dialog outlived this fragment.
    Preference location = findPreference(getString(R.string.setting_key_weather_location));
    if (location instanceof WeatherLocationPreference) {
      ((WeatherLocationPreference) location).refreshSummary();
    }
  }
}
