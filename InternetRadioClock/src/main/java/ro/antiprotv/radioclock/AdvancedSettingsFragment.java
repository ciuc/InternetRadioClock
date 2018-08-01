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

/**
 * Created by ciuc on 7/12/16.
 */
public class AdvancedSettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.advanced_preferences);
    }
}
