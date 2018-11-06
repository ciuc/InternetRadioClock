/**
 * Copyright Cristian "ciuc" Starasciuc 2016
 * Licensed under the Apache license 2.0
 * cristi.ciuc@gmail.com
 */
package ro.antiprotv.radioclock;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;

import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Created by ciuc on 7/12/16.
 */
public class AdvancedSettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.advanced_preferences);

        //need to check the stupid seconds display: if there is not a setting, check it!
        //for some reason "checked" in xml does not work
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity().getBaseContext());
        Map<String, ? > allPrefs = prefs.getAll();
        if(!allPrefs.containsKey(getResources().getString(R.string.setting_key_seconds))) {
            SwitchPreference secondsSwitch = (SwitchPreference) findPreference(getResources().getString(R.string.setting_key_seconds));
            secondsSwitch.setChecked(true);
        }
        if(!allPrefs.containsKey(getResources().getString(R.string.setting_key_clockMove))) {
            SwitchPreference clkMvSwitch = (SwitchPreference) findPreference(getResources().getString(R.string.setting_key_clockMove));
            clkMvSwitch.setChecked(true);
        }

    }
}
