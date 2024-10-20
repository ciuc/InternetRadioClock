/*
 Copyright Cristian "ciuc" Starasciuc 2016
 Licensed under the Apache license 2.0
 cristi.ciuc@gmail.com
*/
package ro.antiprotv.radioclock.fragment;

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
public class SettingsSnoozeSleepFragment extends PreferenceFragmentCompat {

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
    setPreferencesFromResource(R.xml.preferences_settings_snooze_sleep, rootKey);
  }

}
