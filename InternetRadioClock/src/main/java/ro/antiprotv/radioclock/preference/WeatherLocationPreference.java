package ro.antiprotv.radioclock.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceManager;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.service.weather.WeatherSettings;

/**
 * The place the forecast is for. Opens a search box rather than storing what the user typed: the
 * weather service wants coordinates, so the name is resolved once here and only the result is kept.
 */
public class WeatherLocationPreference extends DialogPreference {

  public WeatherLocationPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public WeatherLocationPreference(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  protected void onSetInitialValue(Object defaultValue) {
    refreshSummary();
  }

  /** Shows the chosen place, or an invitation to choose one. */
  public void refreshSummary() {
    Context context = getContext();
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String name = WeatherSettings.getLocationName(context, prefs);
    setSummary(
        name.isEmpty() ? context.getString(R.string.setting_summary_weather_location_none) : name);
  }
}
