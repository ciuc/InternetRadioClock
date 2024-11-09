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
import androidx.preference.PreferenceFragmentCompat;
import ro.antiprotv.radioclock.R;

/** Created by ciuc on 7/12/16. */
public class SettingsAlarmsFragment extends PreferenceFragmentCompat {

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return super.onCreateView(inflater, container, savedInstanceState);
  }

  @Override
  public void onResume() {
    super.onResume();
    androidx.preference.ListPreference stations =
        findPreference(getString(R.string.setting_key_wake_up_station));
    SharedPreferences prefs = getPreferenceManager().getSharedPreferences();

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
  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
    setPreferencesFromResource(R.xml.preferences_settings_alarms, rootKey);
  }
}
