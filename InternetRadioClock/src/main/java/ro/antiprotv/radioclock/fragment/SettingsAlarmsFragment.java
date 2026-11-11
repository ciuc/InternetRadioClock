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
import androidx.preference.PreferenceFragmentCompat;
import java.util.ArrayList;
import java.util.List;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.db.Station;
import ro.antiprotv.radioclock.db.StationPreferences;
import ro.antiprotv.radioclock.db.StationRepository;
import ro.antiprotv.radioclock.service.ButtonManager;

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
    // also turns a wake-up memory saved by an older version into its station id
    long wakeUpId = new StationPreferences(requireContext()).getWakeUpStationId();
    List<CharSequence> labels = new ArrayList<>();
    List<CharSequence> ids = new ArrayList<>();
    labels.add(getString(R.string.lastPlayed));
    ids.add(StationPreferences.WAKE_UP_LAST_PLAYED);
    String selected = StationPreferences.WAKE_UP_LAST_PLAYED;
    for (Station station : StationRepository.getInstance(requireContext()).getStations()) {
      // the alarm plays through the clock, which only has the memory buttons
      if (station.position > ButtonManager.MEMORIES) {
        continue;
      }
      labels.add(station.label);
      ids.add(String.valueOf(station.id));
      if (station.id == wakeUpId) {
        selected = String.valueOf(station.id);
      }
    }
    stations.setEntries(labels.toArray(new CharSequence[0]));
    stations.setEntryValues(ids.toArray(new CharSequence[0]));
    // a station that was removed since: the alarm falls back to the last played one
    stations.setValue(selected);
  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
    setPreferencesFromResource(R.xml.preferences_settings_alarms, rootKey);
  }
}
