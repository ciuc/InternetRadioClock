package ro.antiprotv.radioclock.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.service.SlideshowManager;
import timber.log.Timber;

public class SettingsSlideshowFragment extends PreferenceFragmentCompat {
  ExecutorService executorService = Executors.newSingleThreadExecutor();
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
                executorService.execute(() -> handleImageSelection(result.getData()));
              }
            });

    if (selectImagesPref != null) {
      selectImagesPref.setOnPreferenceClickListener(
          preference -> {
            openImagePicker();
            return true;
          });
    }
    SlideshowManager slideshowManager = SlideshowManager.getInstance();
    if (slideshowManager != null) {
      updateImageCountSummary(selectImagesPref, slideshowManager.getImagesCount());
    } else {
      updateImageCountSummary(selectImagesPref, 0);
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
    // Set<String> uriSet = new HashSet<>();
    int duplicates = 0;
    int permissions = 0;
    JSONArray jsonArray = new JSONArray();
    if (data.getClipData() != null) {
      int count = data.getClipData().getItemCount();
      Timber.d("count: " + count + " selected");
      for (int i = 0; i < count; i++) {
        Uri uri = data.getClipData().getItemAt(i).getUri();
        try {
          requireContext()
              .getContentResolver()
              .takePersistableUriPermission(
                  uri,
                  Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

          /*          if (uriSet.contains(uri.toString())) {
            Cursor cursor =
                requireContext().getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
              String displayName =
                  cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));

              Timber.d("duplicate: " + uri + " :: " + displayName);
              duplicates++;
              cursor.close();
            }
          } else {
            uriSet.add(uri.toString());
          }*/
          jsonArray.put(uri.toString());
        } catch (SecurityException e) {

          Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
          if (cursor != null && cursor.moveToFirst()) {
            String displayName =
                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));

            Timber.d(uri + " :: " + displayName);
            cursor.close();
          }
          permissions++;
          Timber.e(e.getMessage());
        }
      }

    } else if (data.getData() != null) {
      Uri uri = data.getData();
      requireContext()
          .getContentResolver()
          .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
      // uriSet.add(uri.toString());
      jsonArray.put(uri.toString());
    }

    Timber.d(
        "uriSet: "
            + jsonArray.length()
            + " duplicates: "
            + duplicates
            + " permissions: "
            + permissions);
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

    // updateImageCountSummary(
    //    findPreference(getString(R.string.setting_key_slideshow_images)), jsonArray.length());

    prefs
        .edit()
        .putString(getString(R.string.setting_key_slideshow_images), jsonArray.toString())
        .apply();
  }

  private void updateImageCountSummary(Preference selectImagesPref, int count) {
    if (selectImagesPref == null) {
      return;
    }
    selectImagesPref.setSummary(
        requireContext().getString(R.string.setting_summary_slideshow_images, count));
  }
}
