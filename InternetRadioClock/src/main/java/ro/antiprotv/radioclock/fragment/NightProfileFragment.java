/*
 Copyright Cristian "ciuc" Starasciuc 2016
 Licensed under the Apache license 2.0
 cristi.ciuc@gmail.com
*/
package ro.antiprotv.radioclock.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import androidx.annotation.Nullable;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.preference.BrightnessPreference;
import ro.antiprotv.radioclock.preference.TimePreference;
import ro.antiprotv.radioclock.service.profile.ProfileManager;

/** Created by ciuc on 7/12/16. */
public class NightProfileFragment extends PreferenceFragment
    implements SharedPreferences.OnSharedPreferenceChangeListener {
  private BrightnessPreference seekBarPref;
  private TimePreference nightAutostartPref;
  private TimePreference nightAutoEndPref;

  public NightProfileFragment() {}

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences_night_profile);

    seekBarPref =
        (BrightnessPreference)
            findPreference(getString(R.string.setting_key_clockBrightness_night));
    nightAutostartPref =
        (TimePreference) findPreference(ProfileManager.SETTING_NIGHT_PROFILE_AUTOSTART);
    // nightAutostartPref.setKey(ProfileManager.SETTING_NIGHT_PROFILE_AUTOSTART);
    nightAutoEndPref =
        (TimePreference) findPreference(ProfileManager.SETTING_NIGHT_PROFILE_AUTOEND);
    // nightAutostartPref.setKey(ProfileManager.SETTING_NIGHT_PROFILE_AUTOEND);
  }

  @Override
  public void onResume() {
    super.onResume();
    SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
    prefs.registerOnSharedPreferenceChangeListener(this);
    setSummary();
    nightAutostartPref.setSummary(
        prefs.getString(ProfileManager.SETTING_NIGHT_PROFILE_AUTOSTART, "23:00"));
    nightAutoEndPref.setSummary(
        prefs.getString(ProfileManager.SETTING_NIGHT_PROFILE_AUTOEND, "07:00"));
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
    if (key == null) {
      return;
    }
    if (!isAdded()) {
      return;
    }
    if (key.equals(getString(R.string.setting_key_clockBrightness_night))) {
      setSummary();
    }
  }

  private void setSummary() {
    int radius =
        getPreferenceManager()
            .getSharedPreferences()
            .getInt(getString(R.string.setting_key_clockBrightness_night), -1);
    if (radius == -1) {
      seekBarPref.setSummary(
          getString(R.string.setting_summary_clockBrightness).replace("$1%", "AUTO (SYSTEM)"));
    } else {
      seekBarPref.setSummary(
          getString(R.string.setting_summary_clockBrightness).replace("$1", "" + radius));
    }
  }
}
