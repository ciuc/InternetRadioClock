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

/** Created by ciuc on 7/12/16. */
public class SettingsLookAndFeelFragment extends PreferenceFragmentCompat {
  private BrightnessPreference seekBarPref;


  @Override
  public void onDisplayPreferenceDialog(Preference preference) {
    if (preference instanceof BrightnessPreference) {
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
    seekBarPref = findPreference(getString(R.string.setting_key_clockBrightness));

    return super.onCreateView(inflater, container, savedInstanceState);
  }

  @Override
  public void onResume() {
    super.onResume();
    setSummary();
  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
    setPreferencesFromResource(R.xml.preferences_settings_look_and_feel, rootKey);
  }

  private void setSummary() {
    int brightness =
        getPreferenceManager()
            .getSharedPreferences()
            .getInt(getString(R.string.setting_key_clockBrightness), -1);
    seekBarPref.setSummary(BrightnessPreference.getSummary(brightness, getContext()));
  }
}
