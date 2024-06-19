/*
 Copyright Cristian "ciuc" Starasciuc 2016
 Licensed under the Apache license 2.0
 cristi.ciuc@gmail.com
*/
package ro.antiprotv.radioclock.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.preference.BrightnessPreference;
import ro.antiprotv.radioclock.preference.BrightnessPreferencesDialogCompat;
import ro.antiprotv.radioclock.preference.TimePreference;
import ro.antiprotv.radioclock.preference.TimePreferenceDialogCompat;
import ro.antiprotv.radioclock.service.profile.ProfileManager;

/** Created by ciuc on 7/12/16. */
public class NightProfileFragment extends PreferenceFragmentCompat {
  private BrightnessPreference seekBarPref;
  private TimePreference nightAutostartPref;
  private TimePreference nightAutoEndPref;

  public NightProfileFragment() {}

  @Override
  public void onDisplayPreferenceDialog(Preference preference) {
    if (preference instanceof TimePreference) {
      TimePreferenceDialogCompat dialogFragment =
          TimePreferenceDialogCompat.newInstance(preference.getKey());
      dialogFragment.setTargetFragment(this, 0);
      dialogFragment.show(getParentFragmentManager(), null);

    } else if (preference instanceof BrightnessPreference) {
      BrightnessPreferencesDialogCompat dialogFragment =
          BrightnessPreferencesDialogCompat.newInstance(preference.getKey());
      dialogFragment.setTargetFragment(this, 0);
      dialogFragment.show(getParentFragmentManager(), null);
    } else {
      super.onDisplayPreferenceDialog(preference);
    }
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    seekBarPref = findPreference(getString(R.string.setting_key_clockBrightness_night));
    nightAutostartPref = findPreference(ProfileManager.SETTING_NIGHT_PROFILE_AUTOSTART);
    // nightAutostartPref.setKey(ProfileManager.SETTING_NIGHT_PROFILE_AUTOSTART);
    nightAutoEndPref = findPreference(ProfileManager.SETTING_NIGHT_PROFILE_AUTOEND);
    // nightAutostartPref.setKey(ProfileManager.SETTING_NIGHT_PROFILE_AUTOEND);
    return super.onCreateView(inflater, container, savedInstanceState);
  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
    setPreferencesFromResource(R.xml.preferences_night_profile, rootKey);
  }

  @Override
  public void onResume() {
    super.onResume();
    setSummary();
  }

  private void setSummary() {
    SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
    int brightness = preferences.getInt(getString(R.string.setting_key_clockBrightness_night), -1);

    seekBarPref.setSummary(BrightnessPreference.getSummary(brightness, getContext()));
    nightAutostartPref.setSummary(
        preferences.getString(ProfileManager.SETTING_NIGHT_PROFILE_AUTOSTART, "21:00"));
    nightAutoEndPref.setSummary(
        preferences.getString(ProfileManager.SETTING_NIGHT_PROFILE_AUTOEND, "07:00"));
  }
}
