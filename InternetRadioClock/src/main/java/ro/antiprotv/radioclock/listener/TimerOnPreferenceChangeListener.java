package ro.antiprotv.radioclock.listener;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import ro.antiprotv.radioclock.fragment.SettingsTimersFragment;
import timber.log.Timber;

public class TimerOnPreferenceChangeListener implements Preference.OnPreferenceChangeListener {

  private final EditTextPreference timerShort;
  private final SharedPreferences prefs;
  private final int key;
  private final Context context;

  public TimerOnPreferenceChangeListener(
      EditTextPreference timerShort, SharedPreferences prefs, int key, Context context) {
    this.timerShort = timerShort;
    this.prefs = prefs;
    this.key = key;
    this.context = context;
  }

  @Override
  public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
    Timber.d(newValue.toString());
    int timer = SettingsTimersFragment.convertToSeconds(newValue.toString());
    Timber.d("timer: %d", timer);
    Timber.d(context.getString(key));
    timerShort.setSummary(
        String.format(SettingsTimersFragment.TIMER_FORMAT, timer / 60, timer % 60));

    //prefs.edit().putString(context.getString(key), String.valueOf(timer)).apply();
    ((EditTextPreference) preference).setText(String.valueOf(timer));
    return false;
  }
}
