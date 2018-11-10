/*
  Copyright Cristian "ciuc" Starasciuc 2016
  Licensed under the Apache license 2.0
  cristi.ciuc@gmail.com
 */
package ro.antiprotv.radioclock;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by ciuc on 7/12/16.
 */
public class NightProfileFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_night_profile);

    }
}
