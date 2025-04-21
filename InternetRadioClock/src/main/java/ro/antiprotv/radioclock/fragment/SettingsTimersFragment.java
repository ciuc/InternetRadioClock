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
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.listener.TimerOnBindEditTextListener;
import ro.antiprotv.radioclock.listener.TimerOnPreferenceChangeListener;
import ro.antiprotv.radioclock.utils.AlarmSoundsHelper;
import timber.log.Timber;

/** Created by ciuc on 7/12/16. */
public class SettingsTimersFragment extends PreferenceFragmentCompat {

  public static final String TIMER_FORMAT = "%02dm %02ds";

  public static int convertToSeconds(String formattedTime) {
    Pattern pattern = Pattern.compile("(\\d+)m (\\d+)s");
    Matcher matcher = pattern.matcher(formattedTime);

    if (matcher.find()) {
      int minutes = Integer.parseInt(matcher.group(1)); // Extract minutes
      int seconds = Integer.parseInt(matcher.group(2)); // Extract seconds
      Timber.d("minutes: %d, seconds: %d, total %d", minutes, seconds, (minutes * 60) + seconds);
      return (minutes * 60) + seconds;
    }
    return 10; // Default in case of a format error
  }

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
    SharedPreferences prefs = getPreferenceManager().getSharedPreferences();

    EditTextPreference timerShort =
        findPreference(getString(R.string.setting_key_timer_short_seconds));
    if (timerShort != null) {
      timerShort.setOnBindEditTextListener(new TimerOnBindEditTextListener());
    }
    try {
      int shortTimer =
          Integer.parseInt(
              prefs.getString(getString(R.string.setting_key_timer_short_seconds), "10"));
      timerShort.setSummary(String.format(TIMER_FORMAT, shortTimer / 60, shortTimer % 60));
    } catch (NumberFormatException e) {
      timerShort.setSummary(String.format(TIMER_FORMAT, 10 / 60, 10 % 60));
    }
    timerShort.setOnPreferenceChangeListener(
        new TimerOnPreferenceChangeListener(
            timerShort, prefs, R.string.setting_key_timer_short_seconds, this.getContext()));

    EditTextPreference timerLong =
        findPreference(getString(R.string.setting_key_timer_long_seconds));
    if (timerLong != null) {
      timerLong.setOnBindEditTextListener(new TimerOnBindEditTextListener());
    }
    try {

      int longTimer =
          Integer.parseInt(
              prefs.getString(getString(R.string.setting_key_timer_long_seconds), "180"));
      timerLong.setSummary(String.format(TIMER_FORMAT, longTimer / 60, longTimer % 60));
    } catch (NumberFormatException e) {
      timerLong.setSummary(String.format(TIMER_FORMAT, 180 / 60, 180 % 60));
    }
    timerLong.setOnPreferenceChangeListener(
        new TimerOnPreferenceChangeListener(
            timerLong, prefs, R.string.setting_key_timer_long_seconds, this.getContext()));

    EditTextPreference timerLonger =
            findPreference(getString(R.string.setting_key_timer_longer_seconds));
    if (timerLonger != null) {
      timerLonger.setOnBindEditTextListener(new TimerOnBindEditTextListener());
    }
    try {

      int longerTimer =
              Integer.parseInt(
                      prefs.getString(getString(R.string.setting_key_timer_longer_seconds), "180"));
      timerLonger.setSummary(String.format(TIMER_FORMAT, longerTimer / 60, longerTimer % 60));
    } catch (NumberFormatException e) {
      timerLonger.setSummary(String.format(TIMER_FORMAT, 180 / 60, 180 % 60));
    }
    timerLonger.setOnPreferenceChangeListener(
            new TimerOnPreferenceChangeListener(
                    timerLonger, prefs, R.string.setting_key_timer_longer_seconds, this.getContext()));
  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
    setPreferencesFromResource(R.xml.preferences_settings_timers, rootKey);
    ListPreference alarmSoundPreference = findPreference("setting.key.timer_ringtone");
    if (alarmSoundPreference != null) {
      // Get the list of alarm sounds
      List<AlarmSoundsHelper.AlarmSound> alarmSounds =
          AlarmSoundsHelper.getAlarmSounds(requireContext());

      // Prepare the entries (titles) and entryValues (URIs)
      String[] titles = new String[alarmSounds.size()];
      String[] uris = new String[alarmSounds.size()];

      for (int i = 0; i < alarmSounds.size(); i++) {
        titles[i] = alarmSounds.get(i).getTitle();
        uris[i] = alarmSounds.get(i).getUri().toString();
      }

      // Set the entries and entryValues dynamically
      alarmSoundPreference.setEntries(titles);
      alarmSoundPreference.setEntryValues(uris);

      // Optionally set the summary to show the currently selected sound
      alarmSoundPreference.setSummaryProvider(
          preference -> {
            String selectedValue = ((ListPreference) preference).getValue();
            int index = alarmSoundPreference.findIndexOfValue(selectedValue);
            return (index >= 0) ? titles[index] : "Choose an alarm sound";
          });
    }
  }
}
