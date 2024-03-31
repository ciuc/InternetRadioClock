/*
 Copyright Cristian "ciuc" Starasciuc 2016
 Licensed under the Apache license 2.0
 cristi.ciuc@gmail.com
*/
package ro.antiprotv.radioclock;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

/** Created by ciuc on 7/12/16. */
public class ConfigureButtonsFragment extends PreferenceFragment {

  private final SharedPreferences.OnSharedPreferenceChangeListener mListener =
      (prefs, key) -> {
        Preference pref = findPreference(key);
        if (pref != null) {
          pref.setSummary(prefs.getString(key, ""));
        }
      };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences_buttons);
    PreferenceManager.getDefaultSharedPreferences(this.getActivity().getBaseContext())
        .registerOnSharedPreferenceChangeListener(mListener);
    for (String key : getPreferenceScreen().getSharedPreferences().getAll().keySet()) {
      if (findPreference(key) != null) {
        findPreference(key)
            .setSummary(getPreferenceScreen().getSharedPreferences().getString(key, ""));
      }
    }
  }
}
