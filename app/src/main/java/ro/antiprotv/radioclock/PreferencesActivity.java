/**
 * Copyright Cristian "ciuc" Starasciuc 2016
 * cristi.ciuc@gmail.com
 */
package ro.antiprotv.radioclock;

import android.preference.PreferenceActivity;

import java.util.List;

/**
 * Created by ciuc on 7/17/16.
 */
public class PreferencesActivity extends PreferenceActivity
    {
        @Override
        public void onBuildHeaders(List<Header> target)
        {
            loadHeadersFromResource(R.xml.preferences_header, target);
        }

        @Override
        protected boolean isValidFragment(String fragmentName)
        {
            return SettingsFragment.class.getName().equals(fragmentName);
        }
    }

