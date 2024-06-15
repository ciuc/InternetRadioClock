/*
 Copyright Cristian "ciuc" Starasciuc 2016
 Licensed under the Apache license 2.0
 cristi.ciuc@gmail.com
*/
package ro.antiprotv.radioclock.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import androidx.annotation.Nullable;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.preference.BrightnessPreference;

/** Created by ciuc on 7/12/16. */
public class SettingsFragment extends PreferenceFragment
    implements SharedPreferences.OnSharedPreferenceChangeListener {
  private BrightnessPreference seekBarPref;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences_settings);

    seekBarPref =
        (BrightnessPreference) findPreference(getString(R.string.setting_key_clockBrightness));
  }

  @Override
  public void onResume() {
    super.onResume();
    ListPreference stations =
        (ListPreference) findPreference(getString(R.string.setting_key_wake_up_station));
    SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
    prefs.registerOnSharedPreferenceChangeListener(this);
    CharSequence[] labels =
        new CharSequence[] {
          getString(R.string.lastPlayed),
          prefs.getString(getString(R.string.setting_key_label1), "1"),
          prefs.getString(getString(R.string.setting_key_label2), "2"),
          prefs.getString(getString(R.string.setting_key_label3), "3"),
          prefs.getString(getString(R.string.setting_key_label4), "4"),
          prefs.getString(getString(R.string.setting_key_label5), "5"),
          prefs.getString(getString(R.string.setting_key_label6), "6"),
          prefs.getString(getString(R.string.setting_key_label7), "7"),
          prefs.getString(getString(R.string.setting_key_label8), "8"),
        };
    CharSequence[] tags =
        new CharSequence[] {
          "0",
          getString(R.string.setting_key_stream1),
          getString(R.string.setting_key_stream2),
          getString(R.string.setting_key_stream3),
          getString(R.string.setting_key_stream4),
          getString(R.string.setting_key_stream5),
          getString(R.string.setting_key_stream6),
          getString(R.string.setting_key_stream7),
          getString(R.string.setting_key_stream8)
        };
    stations.setEntries(labels);
    stations.setEntryValues(tags);

    setSummary();
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
    if (key == null) {
      return;
    }
    if (!isAdded()) {
      return;
    }
    if (key.equals(getString(R.string.setting_key_clockBrightness))) {
      setSummary();
    }
  }

  private void setSummary() {
    int radius =
        getPreferenceManager()
            .getSharedPreferences()
            .getInt(getString(R.string.setting_key_clockBrightness), -1);
    if (radius == -1) {
      seekBarPref.setSummary(
          getString(R.string.setting_summary_clockBrightness).replace("$1%", "AUTO (SYSTEM)"));
    } else {
      seekBarPref.setSummary(
          getString(R.string.setting_summary_clockBrightness).replace("$1", "" + radius));
    }
  }
}
