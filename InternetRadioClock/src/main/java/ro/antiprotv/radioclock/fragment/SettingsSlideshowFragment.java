package ro.antiprotv.radioclock.fragment;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.service.SlideshowFolder;
import ro.antiprotv.radioclock.service.SlideshowManager;
import ro.antiprotv.radioclock.service.SlideshowUriPermissions;
import timber.log.Timber;

/**
 * Lets the user choose slideshow images either one by one or as a whole folder.
 *
 * <p>The two are deliberately exclusive. Picking images one by one costs one persisted URI grant
 * each and Android caps those per package (128 on API 24), so a large selection silently loses most
 * of itself; a folder costs a single grant however many images are behind it. Keeping both at once
 * would only make it harder to tell which images the slideshow is actually showing.
 */
public class SettingsSlideshowFragment extends PreferenceFragmentCompat {
  ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private ActivityResultLauncher<Intent> imagePickerLauncher;
  private ActivityResultLauncher<Intent> folderPickerLauncher;

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
    setPreferencesFromResource(R.xml.preferences_settings_slideshow, rootKey);
    Preference selectImagesPref = findPreference(getString(R.string.setting_key_slideshow_images));
    Preference selectFolderPref = findPreference(getString(R.string.setting_key_slideshow_folder));
    // Set up ActivityResultLauncher
    imagePickerLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                // Resolved here, on the main thread: the fragment may be detached by the time the
                // executor runs, and requireContext() would then throw.
                Context appContext = requireContext().getApplicationContext();
                executorService.execute(() -> handleImageSelection(appContext, result.getData()));
              }
            });
    folderPickerLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if (result.getResultCode() == Activity.RESULT_OK
                  && result.getData() != null
                  && result.getData().getData() != null) {
                Context appContext = requireContext().getApplicationContext();
                Uri treeUri = result.getData().getData();
                executorService.execute(() -> handleFolderSelection(appContext, treeUri));
              }
            });

    if (selectImagesPref != null) {
      selectImagesPref.setOnPreferenceClickListener(
          preference -> {
            openImagePicker();
            return true;
          });
    }
    if (selectFolderPref != null) {
      selectFolderPref.setOnPreferenceClickListener(
          preference -> {
            openFolderPicker();
            return true;
          });
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    PreferenceManager.getDefaultSharedPreferences(requireContext())
        .registerOnSharedPreferenceChangeListener(imagesPrefListener);
    refreshImageCountSummary();
    refreshFolderSummary();
  }

  @Override
  public void onPause() {
    super.onPause();
    PreferenceManager.getDefaultSharedPreferences(requireContext())
        .unregisterOnSharedPreferenceChangeListener(imagesPrefListener);
  }

  /**
   * The pickers write the new selection from a background thread; SharedPreferences always delivers
   * this callback on the main thread, so the summaries can be touched directly here.
   */
  private final SharedPreferences.OnSharedPreferenceChangeListener imagesPrefListener =
      (prefs, key) -> {
        if (!isAdded()) {
          return;
        }
        if (getString(R.string.setting_key_slideshow_images).equals(key)) {
          refreshImageCountSummary();
        } else if (getString(R.string.setting_key_slideshow_folder).equals(key)) {
          refreshFolderSummary();
        }
      };

  private void refreshImageCountSummary() {
    Context context = requireContext();
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    updateImageCountSummary(
        findPreference(context.getString(R.string.setting_key_slideshow_images)),
        SlideshowManager.getSavedImageCount(context, prefs));
  }

  /**
   * Shows the folder name straight away and fills the image count in once it is known: counting
   * means listing the folder, which is a provider query.
   */
  private void refreshFolderSummary() {
    Context context = requireContext();
    Preference pref = findPreference(context.getString(R.string.setting_key_slideshow_folder));
    if (pref == null) {
      return;
    }
    String folder = savedFolder(context);
    if (folder.isEmpty()) {
      pref.setSummary(context.getString(R.string.setting_summary_slideshow_folder_none));
      return;
    }
    Uri treeUri = Uri.parse(folder);
    String name = SlideshowFolder.displayName(treeUri);
    pref.setSummary(context.getString(R.string.setting_summary_slideshow_folder, name));

    Context appContext = context.getApplicationContext();
    executorService.execute(
        () -> {
          int count = SlideshowFolder.listImages(appContext, treeUri).size();
          mainHandler.post(
              () -> {
                if (!isAdded() || !folder.equals(savedFolder(requireContext()))) {
                  return;
                }
                pref.setSummary(
                    requireContext()
                        .getString(R.string.setting_summary_slideshow_folder_count, name, count));
              });
        });
  }

  private static String savedFolder(Context context) {
    String folder =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString(context.getString(R.string.setting_key_slideshow_folder), "");
    return folder != null ? folder : "";
  }

  private void openImagePicker() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.setType("image/*");
    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    try {
      imagePickerLauncher.launch(intent);
    } catch (ActivityNotFoundException e) {
      // No document provider on the device; rare, but it would otherwise take the app down.
      Timber.e("No document picker available: %s", e.getMessage());
      Toast.makeText(requireContext(), R.string.slideshow_no_picker, Toast.LENGTH_LONG).show();
    }
  }

  private void openFolderPicker() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    try {
      folderPickerLauncher.launch(intent);
    } catch (ActivityNotFoundException e) {
      Timber.e("No folder picker available: %s", e.getMessage());
      Toast.makeText(requireContext(), R.string.slideshow_no_folder_picker, Toast.LENGTH_LONG)
          .show();
    }
  }

  private void handleImageSelection(Context context, Intent data) {
    int failed = 0;
    int duplicates = 0;
    Set<String> seen = new HashSet<>();
    JSONArray jsonArray = new JSONArray();
    if (data.getClipData() != null) {
      int count = data.getClipData().getItemCount();
      Timber.d("count: " + count + " selected");
      for (int i = 0; i < count; i++) {
        Uri uri = data.getClipData().getItemAt(i).getUri();
        if (!seen.add(uri.toString())) {
          // Pickers can hand back the same image twice; saving it twice would show it twice and
          // overstate the count in the settings summary.
          duplicates++;
        } else if (SlideshowUriPermissions.takePersistable(context, data, uri)) {
          jsonArray.put(uri.toString());
        } else {
          failed++;
        }
      }
    } else if (data.getData() != null) {
      Uri uri = data.getData();
      if (SlideshowUriPermissions.takePersistable(context, data, uri)) {
        jsonArray.put(uri.toString());
      } else {
        failed++;
      }
    }

    Timber.d("persisted: " + jsonArray.length() + " failed: " + failed + " dupes: " + duplicates);
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    boolean hadFolder = !savedFolder(context).isEmpty();
    prefs
        .edit()
        .putString(context.getString(R.string.setting_key_slideshow_images), jsonArray.toString())
        // Only one source of images at a time; see the note on this class.
        .putString(context.getString(R.string.setting_key_slideshow_folder), "")
        .commit();

    if (hadFolder) {
      toastOnMainThread(R.string.slideshow_images_replace_folder);
    }
    // This selection replaces the previous one, so hand back the grants it dropped. Already off
    // the main thread here.
    SlideshowUriPermissions.reconcile(context, prefs);
  }

  private void handleFolderSelection(Context context, Uri treeUri) {
    if (!SlideshowUriPermissions.takePersistableTree(context, treeUri)) {
      toastOnMainThread(R.string.slideshow_no_folder_picker);
      return;
    }
    List<Uri> images = SlideshowFolder.listImages(context, treeUri);
    Timber.d("folder %s selected, %s images", treeUri, images.size());

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    boolean hadImages = SlideshowManager.getSavedImageCount(context, prefs) > 0;
    prefs
        .edit()
        .putString(context.getString(R.string.setting_key_slideshow_folder), treeUri.toString())
        // Only one source of images at a time; see the note on this class.
        .putString(context.getString(R.string.setting_key_slideshow_images), "[]")
        .commit();

    if (hadImages) {
      toastOnMainThread(R.string.slideshow_folder_replaces_images);
    }
    // Hands back the per-image grants the folder just replaced. Already off the main thread here.
    SlideshowUriPermissions.reconcile(context, prefs);
  }

  private void toastOnMainThread(int messageId) {
    mainHandler.post(
        () -> {
          if (isAdded()) {
            Toast.makeText(requireContext(), messageId, Toast.LENGTH_LONG).show();
          }
        });
  }

  private void updateImageCountSummary(Preference selectImagesPref, int count) {
    if (selectImagesPref == null) {
      return;
    }
    selectImagesPref.setSummary(
        requireContext().getString(R.string.setting_summary_slideshow_images, count));
  }
}
