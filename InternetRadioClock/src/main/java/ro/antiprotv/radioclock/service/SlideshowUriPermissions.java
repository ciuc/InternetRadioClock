package ro.antiprotv.radioclock.service;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ro.antiprotv.radioclock.R;
import timber.log.Timber;

/**
 * Keeps the persisted SAF grants in step with the files actually stored in the slideshow
 * preference.
 *
 * <p>Android caps the number of persistable URI grants a package may hold and silently trims the
 * oldest ones once that cap is passed, so grants for files the user has since replaced have to be
 * handed back. Otherwise every re-pick leaks a whole set of grants and, eventually, the trimming
 * revokes access to files the slideshow is still using.
 */
public final class SlideshowUriPermissions {
  private static final ExecutorService executor = Executors.newSingleThreadExecutor();

  private SlideshowUriPermissions() {}

  /**
   * Persists read access to a folder the user granted with {@code ACTION_OPEN_DOCUMENT_TREE}. Only
   * read is taken: write is not needed to show a slideshow, and asking for a mode the provider did
   * not make persistable would fail the whole call.
   *
   * @return whether access was persisted; the folder should not be saved if not
   */
  public static boolean takePersistableTree(Context context, Uri treeUri) {
    try {
      context
          .getContentResolver()
          .takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
      return true;
    } catch (SecurityException e) {
      Timber.e("Could not persist access to folder %s: %s", treeUri, e.getMessage());
      return false;
    }
  }

  /**
   * Persists access to a picked file, taking exactly the modes the picker actually granted.
   *
   * @param result the picker's result intent, whose flags say what was granted
   * @return whether access was persisted; the file should be dropped from the list if not
   */
  public static boolean takePersistable(Context context, Intent result, Uri uri) {
    int takeFlags =
        result.getFlags()
            & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    if ((takeFlags & Intent.FLAG_GRANT_READ_URI_PERMISSION) == 0) {
      takeFlags |= Intent.FLAG_GRANT_READ_URI_PERMISSION;
    }
    try {
      context.getContentResolver().takePersistableUriPermission(uri, takeFlags);
      return true;
    } catch (SecurityException e) {
      Timber.e("Could not persist access to %s: %s", uri, e.getMessage());
      return false;
    }
  }

  /** Runs {@link #reconcile} off the main thread; safe to call from {@code onCreate}. */
  public static void reconcileAsync(Context context, SharedPreferences prefs) {
    Context appContext = context.getApplicationContext();
    executor.execute(() -> reconcile(appContext, prefs));
  }

  /**
   * Releases every persisted grant the saved slideshow list no longer refers to. Makes one binder
   * call per stale grant, so call it off the main thread.
   */
  public static void reconcile(Context context, SharedPreferences prefs) {
    Set<String> keep = readSavedUris(context, prefs);
    if (keep == null) {
      // The preference could not be read; releasing now could revoke images still in use.
      return;
    }
    ContentResolver resolver = context.getContentResolver();
    int released = 0;
    for (UriPermission permission : resolver.getPersistedUriPermissions()) {
      Uri uri = permission.getUri();
      if (keep.contains(uri.toString())) {
        continue;
      }
      int flags = 0;
      if (permission.isReadPermission()) {
        flags |= Intent.FLAG_GRANT_READ_URI_PERMISSION;
      }
      if (permission.isWritePermission()) {
        flags |= Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
      }
      if (flags == 0) {
        continue;
      }
      try {
        resolver.releasePersistableUriPermission(uri, flags);
        released++;
      } catch (SecurityException e) {
        Timber.e("Could not release %s: %s", uri, e.getMessage());
      }
    }
    Timber.d("Released %s stale slideshow grants, kept %s", released, keep.size());
  }

  /**
   * The URIs the slideshow still needs access to: the saved files, plus the picked folder. The
   * folder has to be in here, otherwise reconciling would hand back the one grant that the whole
   * folder mode rests on.
   *
   * @return the URIs to keep, or null if the preference could not be parsed.
   */
  private static Set<String> readSavedUris(Context context, SharedPreferences prefs) {
    String json = prefs.getString(context.getString(R.string.setting_key_slideshow_images), "[]");
    Set<String> uris = SlideshowItems.uriStrings(json);
    if (uris == null) {
      Timber.e("Could not parse the saved slideshow files; leaving grants untouched");
      return null;
    }
    String folder = prefs.getString(context.getString(R.string.setting_key_slideshow_folder), "");
    if (folder != null && !folder.isEmpty()) {
      uris.add(folder);
    }
    return uris;
  }
}
