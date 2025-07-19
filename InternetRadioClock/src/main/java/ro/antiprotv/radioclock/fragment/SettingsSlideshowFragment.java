package ro.antiprotv.radioclock.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ro.antiprotv.radioclock.R;

public class SettingsSlideshowFragment extends PreferenceFragmentCompat {
    private ActivityResultLauncher<Intent> imagePickerLauncher;

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
    setPreferencesFromResource(R.xml.preferences_settings_slideshow, rootKey);
      Preference selectImagesPref = findPreference(getString(R.string.setting_key_slideshow_images));
    // Set up ActivityResultLauncher
    imagePickerLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                handleImageSelection(result.getData());
              }
            });

    if (selectImagesPref != null) {
      selectImagesPref.setOnPreferenceClickListener(preference -> {
        openImagePicker();
        return true;
      });
    }

  }

  private void openImagePicker() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.setType("image/*");
    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    imagePickerLauncher.launch(intent);
  }


  private void handleImageSelection(Intent data) {
    List<String> uriStrings = new ArrayList<>();

    if (data.getClipData() != null) {
      int count = data.getClipData().getItemCount();
      for (int i = 0; i < count; i++) {
        Uri uri = data.getClipData().getItemAt(i).getUri();
        requireContext().getContentResolver().takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        uriStrings.add(uri.toString());
      }
    } else if (data.getData() != null) {
      Uri uri = data.getData();
      requireContext().getContentResolver().takePersistableUriPermission(
              uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
      uriStrings.add(uri.toString());
    }

    // Save to preferences
    Set<String> uriSet = new HashSet<>(uriStrings);
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
    prefs.edit().putStringSet(getString(R.string.setting_key_slideshow_images), uriSet).apply();

    //updateImageCountSummary();
  }

}
