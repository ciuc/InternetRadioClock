/**
 * Copyright Cristian "ciuc" Starasciuc 2016
 * Licensed under the Apache license 2.0
 * cristi.ciuc@gmail.com
 */
package ro.antiprotv.radioclock;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

/**
 * Created by ciuc on 7/12/16.
 */
public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        getPreferenceScreen()
                .getSharedPreferences().registerOnSharedPreferenceChangeListener(mListener);
        for (String key : getPreferenceScreen().getSharedPreferences().getAll().keySet()) {
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
