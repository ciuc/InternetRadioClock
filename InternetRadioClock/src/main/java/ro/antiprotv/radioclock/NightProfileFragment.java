/*
  Copyright Cristian "ciuc" Starasciuc 2016
  Licensed under the Apache license 2.0
  cristi.ciuc@gmail.com
 */
package ro.antiprotv.radioclock;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by ciuc on 7/12/16.
 */
public class NightProfileFragment extends PreferenceFragment {
    private SeekBarPreference seekBarPref;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_night_profile);

        seekBarPref = (SeekBarPreference) findPreference(getString(R.string.setting_key_clockBrightness_night));
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        int radius = prefs.getInt(getString(R.string.setting_key_clockBrightness_night), 50);
        seekBarPref.setSummary(getString(R.string.setting_summary_clockBrightness).replace("$1", ""+radius));
    }
}
