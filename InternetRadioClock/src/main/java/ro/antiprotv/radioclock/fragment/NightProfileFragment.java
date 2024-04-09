/*
  Copyright Cristian "ciuc" Starasciuc 2016
  Licensed under the Apache license 2.0
  cristi.ciuc@gmail.com
 */
package ro.antiprotv.radioclock.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import ro.antiprotv.radioclock.service.profile.ProfileManager;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.preference.SeekBarPreference;
import ro.antiprotv.radioclock.preference.TimePreference;

/**
 * Created by ciuc on 7/12/16.
 */
public class NightProfileFragment extends PreferenceFragment {
    private SeekBarPreference seekBarPref;
    private TimePreference nightAutostartPref;
    private TimePreference nightAutoEndPref;

    public NightProfileFragment() {
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_night_profile);

        seekBarPref = (SeekBarPreference) findPreference(getString(R.string.setting_key_clockBrightness_night));
        nightAutostartPref = (TimePreference) findPreference(ProfileManager.SETTING_NIGHT_PROFILE_AUTOSTART);
        //nightAutostartPref.setKey(ProfileManager.SETTING_NIGHT_PROFILE_AUTOSTART);
        nightAutoEndPref = (TimePreference) findPreference(ProfileManager.SETTING_NIGHT_PROFILE_AUTOEND);
        //nightAutostartPref.setKey(ProfileManager.SETTING_NIGHT_PROFILE_AUTOEND);
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        int radius = prefs.getInt(getString(R.string.setting_key_clockBrightness_night), 50);
        seekBarPref.setSummary(getString(R.string.setting_summary_clockBrightness).replace("$1", ""+radius));
        nightAutostartPref.setSummary(prefs.getString(ProfileManager.SETTING_NIGHT_PROFILE_AUTOSTART, "23:00"));
        nightAutoEndPref.setSummary(prefs.getString(ProfileManager.SETTING_NIGHT_PROFILE_AUTOEND, "07:00"));
    }


}
