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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.service.SlideshowFolder;
import ro.antiprotv.radioclock.service.SlideshowItem;
import ro.antiprotv.radioclock.service.SlideshowItems;
import ro.antiprotv.radioclock.service.SlideshowManager;
import ro.antiprotv.radioclock.service.SlideshowUriPermissions;
import timber.log.Timber;

/**
 * Lets the user choose slideshow pictures and videos either one by one or as a whole folder.
 *
 * <p>The two are deliberately exclusive. Picking files one by one costs one persisted URI grant
 * each and Android caps those per package (128 on API 24), so a large selection silently loses most
 * of itself; a folder costs a single grant however many files are behind it. Keeping both at once
 * would only make it harder to tell what the slideshow is actually showing.
 */
public class SettingsSlideshowFragment extends PreferenceFragmentCompat {
  ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private ActivityResultLauncher<Intent> imagePickerLauncher;
  private ActivityResultLauncher<Intent> folderPickerLauncher;

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
    // Before the screen is inflated: the slider reads its value as an int, and an upgrading user
    // still has the old duration stored as a millisecond string under a different key.
    SlideshowManager.migrateImageDuration(
        requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()));
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
    refreshMediaCountSummary();
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
          refreshMediaCountSummary();
        } else if (getString(R.string.setting_key_slideshow_folder).equals(key)) {
          refreshFolderSummary();
        }
      };

  private void refreshMediaCountSummary() {
    Context context = requireContext();
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    int[] counts = SlideshowManager.getSavedCounts(context, prefs);
    updateMediaCountSummary(
        findPreference(context.getString(R.string.setting_key_slideshow_images)),
        counts[0],
        counts[1]);
  }

  /**
   * Shows the folder name straight away and fills the counts in once they are known: counting means
   * listing the folder, which is a provider query.
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
          List<SlideshowItem> media = SlideshowFolder.listMedia(appContext, treeUri);
          int videos = 0;
          for (SlideshowItem item : media) {
            if (item.video) {
              videos++;
            }
          }
          final int images = media.size() - videos;
          final int videoCount = videos;
          mainHandler.post(
              () -> {
                if (!isAdded() || !folder.equals(savedFolder(requireContext()))) {
                  return;
                }
                pref.setSummary(
                    requireContext()
                        .getString(
                            R.string.setting_summary_slideshow_folder_count,
                            name,
                            images,
                            videoCount));
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
    // Two kinds at once means a wildcard type plus the list of what we actually take. Some older
    // providers ignore the list, which is the other reason each pick is checked below.
    intent.setType("*/*");
    intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"image/*", "video/*"});
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
    List<SlideshowItem> picked = new ArrayList<>();
    if (data.getClipData() != null) {
      int count = data.getClipData().getItemCount();
      Timber.d("count: " + count + " selected");
      for (int i = 0; i < count; i++) {
        Uri uri = data.getClipData().getItemAt(i).getUri();
        if (!seen.add(uri.toString())) {
          // Pickers can hand back the same file twice; saving it twice would show it twice and
          // overstate the count in the settings summary.
          duplicates++;
        } else if (SlideshowUriPermissions.takePersistable(context, data, uri)) {
          picked.add(new SlideshowItem(uri, isVideo(context, uri)));
        } else {
          failed++;
        }
      }
    } else if (data.getData() != null) {
      Uri uri = data.getData();
      if (SlideshowUriPermissions.takePersistable(context, data, uri)) {
        picked.add(new SlideshowItem(uri, isVideo(context, uri)));
      } else {
        failed++;
      }
    }

    Timber.d("persisted: " + picked.size() + " failed: " + failed + " dupes: " + duplicates);
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    boolean hadFolder = !savedFolder(context).isEmpty();
    prefs
        .edit()
        .putString(
            context.getString(R.string.setting_key_slideshow_images),
            SlideshowItems.toJson(picked))
        // Only one source of files at a time; see the note on this class.
        .putString(context.getString(R.string.setting_key_slideshow_folder), "")
        .commit();

    if (hadFolder) {
      toastOnMainThread(R.string.slideshow_images_replace_folder);
    }
    // This selection replaces the previous one, so hand back the grants it dropped. Already off
    // the main thread here.
    SlideshowUriPermissions.reconcile(context, prefs);
  }

  /**
   * Whether a picked file is a video. Asked of the provider here, while we are already off the main
   * thread and already looping, rather than at display time - the slideshow would otherwise have to
   * make one of these calls per file every time it starts.
   */
  private static boolean isVideo(Context context, Uri uri) {
    String mime = context.getContentResolver().getType(uri);
    if (SlideshowItem.isVagueMime(mime)) {
      return SlideshowItem.looksLikeVideoName(uri.getLastPathSegment());
    }
    return SlideshowItem.isVideoMime(mime);
  }

  private void handleFolderSelection(Context context, Uri treeUri) {
    if (!SlideshowUriPermissions.takePersistableTree(context, treeUri)) {
      toastOnMainThread(R.string.slideshow_no_folder_picker);
      return;
    }
    List<SlideshowItem> media = SlideshowFolder.listMedia(context, treeUri);
    Timber.d("folder %s selected, %s files", treeUri, media.size());

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    boolean hadImages = SlideshowManager.getSavedImageCount(context, prefs) > 0;
    prefs
        .edit()
        .putString(context.getString(R.string.setting_key_slideshow_folder), treeUri.toString())
        // Only one source of files at a time; see the note on this class.
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

  private void updateMediaCountSummary(Preference selectImagesPref, int images, int videos) {
    if (selectImagesPref == null) {
      return;
    }
    selectImagesPref.setSummary(
        requireContext().getString(R.string.setting_summary_slideshow_media, images, videos));
  }
}
