/*
 Copyright Cristian "ciuc" Starasciuc 2016
 Licensed under the Apache license 2.0
 cristi.ciuc@gmail.com
*/
package ro.antiprotv.radioclock.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import ro.antiprotv.radioclock.R;

/** Created by ciuc on 7/12/16. */
public class SettingsFragment extends PreferenceFragmentCompat {

  /**
   * The background play notification is the only way to stop the radio from outside the app, so ask
   * for the permission to show it. Playback works either way, hence the empty callback.
   */
  private final ActivityResultLauncher<String> notificationPermissionLauncher =
      registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {});

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
    setPreferencesFromResource(R.xml.preferences_settings, rootKey);

    Preference backgroundPlay = findPreference(getString(R.string.setting_key_backgroundPlay));
    if (backgroundPlay != null) {
      backgroundPlay.setOnPreferenceChangeListener(
          (preference, newValue) -> {
            if (Boolean.TRUE.equals(newValue)) {
              requestNotificationPermissionIfNeeded();
            }
            return true;
          });
    }
  }

  private void requestNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      return;
    }
    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }
  }
}
