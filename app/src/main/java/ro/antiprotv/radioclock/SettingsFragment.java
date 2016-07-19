package ro.antiprotv.radioclock;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

/**
 * Created by ciuc on 7/12/16.
 */
public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(ClockActivity.TAG_RADIOCLOCK,"settings fragment oncreate");
        addPreferencesFromResource(R.xml.preferences);
        getPreferenceScreen()
                .getSharedPreferences().registerOnSharedPreferenceChangeListener(mListener);
        for (String key : getPreferenceScreen().getSharedPreferences().getAll().keySet()) {
            Log.d(ClockActivity.TAG_RADIOCLOCK, "Setting value of " + key);
            if (findPreference(key) != null) {
                findPreference(key).setSummary(getPreferenceScreen().getSharedPreferences().getString(key, ""));
            }
        }
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener mListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            Preference pref = findPreference(key);
            pref.setSummary(prefs.getString(key, ""));
        }
    };

}
