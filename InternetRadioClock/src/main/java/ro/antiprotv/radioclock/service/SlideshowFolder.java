package ro.antiprotv.radioclock.service;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import timber.log.Timber;

/**
 * Reads the pictures and videos out of a folder the user granted through {@code
 * ACTION_OPEN_DOCUMENT_TREE}.
 *
 * <p>A tree grant is a single persisted URI permission however many files sit behind it, which is
 * the point of it: Android caps the persisted grants one package may hold (128 on API 24), so
 * picking files one by one silently loses most of a large selection. Listing the children at
 * display time also means files dropped into the folder later turn up on their own.
 */
public final class SlideshowFolder {
  private SlideshowFolder() {}

  /**
   * The pictures and videos sitting directly in the granted folder, ordered by name so the
   * slideshow keeps a stable order. One sort over both kinds is what interleaves them. Sub-folders
   * are not descended into. Queries a content provider, so call it off the main thread.
   */
  public static List<SlideshowItem> listMedia(Context context, Uri treeUri) {
    List<SlideshowItem> media = new ArrayList<>();
    String treeDocId;
    try {
      treeDocId = DocumentsContract.getTreeDocumentId(treeUri);
    } catch (Exception e) {
      Timber.e("Not a usable slideshow folder uri: %s (%s)", treeUri, e.getMessage());
      return media;
    }

    Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId);
    String[] projection = {
      DocumentsContract.Document.COLUMN_DOCUMENT_ID,
      DocumentsContract.Document.COLUMN_MIME_TYPE,
      DocumentsContract.Document.COLUMN_DISPLAY_NAME
    };
    // Sorted here rather than by the query: not every provider honours a sort order.
    List<Child> found = new ArrayList<>();
    try (Cursor cursor =
        context.getContentResolver().query(childrenUri, projection, null, null, null)) {
      if (cursor == null) {
        Timber.e("Could not list the slideshow folder %s", treeUri);
        return media;
      }
      while (cursor.moveToNext()) {
        String docId = cursor.getString(0);
        if (docId == null) {
          continue;
        }
        String mime = cursor.getString(1);
        // Read before the filter, because the fallback below judges by file name.
        String name = cursor.getString(2);
        boolean video = SlideshowItem.isVideoMime(mime);
        if (!video && (mime == null || !mime.startsWith("image/"))) {
          // Some providers report nothing useful for the less common video containers. A folder
          // entry that is merely vague gets a second look; anything else is not ours (sub-folders
          // land here too, and never look like a video).
          if (!SlideshowItem.isVagueMime(mime)
              || !SlideshowItem.looksLikeVideoName(name != null ? name : docId)) {
            continue;
          }
          video = true;
        }
        found.add(new Child(name != null ? name : docId, docId, video));
      }
    } catch (SecurityException e) {
      // The grant is gone: the folder was deleted, or access was revoked from system settings.
      Timber.e("No access to the slideshow folder %s: %s", treeUri, e.getMessage());
      return media;
    } catch (Exception e) {
      Timber.e("Could not read the slideshow folder %s: %s", treeUri, e.getMessage());
      return media;
    }

    // Not Comparator.comparing: that is API 24 and this app still runs on 23.
    Collections.sort(
        found,
        new Comparator<Child>() {
          @Override
          public int compare(Child a, Child b) {
            return String.CASE_INSENSITIVE_ORDER.compare(a.name, b.name);
          }
        });
    int videos = 0;
    for (Child child : found) {
      media.add(
          new SlideshowItem(
              DocumentsContract.buildDocumentUriUsingTree(treeUri, child.docId), child.video));
      if (child.video) {
        videos++;
      }
    }
    Timber.d(
        "Slideshow folder %s holds %s images and %s videos",
        treeUri, media.size() - videos, videos);
    return media;
  }

  /** A folder entry, held until the whole listing can be sorted by name. */
  private static final class Child {
    final String name;
    final String docId;
    final boolean video;

    Child(String name, String docId, boolean video) {
      this.name = name;
      this.docId = docId;
      this.video = video;
    }
  }

  /** A short name for the folder, for the settings summary. Cheap: no provider query. */
  public static String displayName(Uri treeUri) {
    String docId;
    try {
      docId = DocumentsContract.getTreeDocumentId(treeUri);
    } catch (Exception e) {
      String last = treeUri.getLastPathSegment();
      return last != null ? last : treeUri.toString();
    }
    int cut = Math.max(docId.lastIndexOf('/'), docId.lastIndexOf(':'));
    if (cut >= 0 && cut + 1 < docId.length()) {
      return docId.substring(cut + 1);
    }
    return docId;
  }
}
