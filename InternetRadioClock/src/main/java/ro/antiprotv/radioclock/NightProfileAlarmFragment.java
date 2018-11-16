/*
  Copyright Cristian "ciuc" Starasciuc 2016
  Licensed under the Apache license 2.0
  cristi.ciuc@gmail.com
 */
package ro.antiprotv.radioclock;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

/**
 * Created by ciuc on 7/12/16.
 */
public class NightProfileAlarmFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_night_profile_alarm);
        SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
        String keyHour = getResources().getString(R.string.setting_key_night_alarm_hour);
        String keyMinute = getResources().getString(R.string.setting_key_night_alarm_minute);
        if (prefs.contains(keyHour) && prefs.contains(keyMinute)) {
            int hour = prefs.getInt(getResources().getString(R.string.setting_key_night_alarm_hour), 0);
            int minute = prefs.getInt(getResources().getString(R.string.setting_key_night_alarm_minute), 0);
            findPreference(getString(R.string.setting_key_alarm_night_time)).setSummary(String.format("%02d:%02d", hour, minute));
        }


    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getResources().getString(R.string.setting_key_night_alarm_enable))) {
            boolean alarmEnabled = sharedPreferences.getBoolean(getString(R.string.setting_key_night_alarm_enable), false);

        }
    }
}
