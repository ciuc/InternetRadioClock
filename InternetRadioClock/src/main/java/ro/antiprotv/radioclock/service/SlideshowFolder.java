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
 * Reads the images out of a folder the user granted through {@code ACTION_OPEN_DOCUMENT_TREE}.
 *
 * <p>A tree grant is a single persisted URI permission however many files sit behind it, which is
 * the point of it: Android caps the persisted grants one package may hold (128 on API 24), so
 * picking images one by one silently loses most of a large selection. Listing the children at
 * display time also means images dropped into the folder later turn up on their own.
 */
public final class SlideshowFolder {
  private SlideshowFolder() {}

  /**
   * The images sitting directly in the granted folder, ordered by name so the slideshow keeps a
   * stable order. Sub-folders are not descended into. Queries a content provider, so call it off
   * the main thread.
   */
  public static List<Uri> listImages(Context context, Uri treeUri) {
    List<Uri> images = new ArrayList<>();
    String treeDocId;
    try {
      treeDocId = DocumentsContract.getTreeDocumentId(treeUri);
    } catch (Exception e) {
      Timber.e("Not a usable slideshow folder uri: %s (%s)", treeUri, e.getMessage());
      return images;
    }

    Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId);
    String[] projection = {
      DocumentsContract.Document.COLUMN_DOCUMENT_ID,
      DocumentsContract.Document.COLUMN_MIME_TYPE,
      DocumentsContract.Document.COLUMN_DISPLAY_NAME
    };
    // Sorted here rather than by the query: not every provider honours a sort order.
    List<String[]> found = new ArrayList<>();
    try (Cursor cursor =
        context.getContentResolver().query(childrenUri, projection, null, null, null)) {
      if (cursor == null) {
        Timber.e("Could not list the slideshow folder %s", treeUri);
        return images;
      }
      while (cursor.moveToNext()) {
        String mime = cursor.getString(1);
        if (mime == null || !mime.startsWith("image/")) {
          continue;
        }
        String docId = cursor.getString(0);
        if (docId == null) {
          continue;
        }
        String name = cursor.getString(2);
        found.add(new String[] {name != null ? name : docId, docId});
      }
    } catch (SecurityException e) {
      // The grant is gone: the folder was deleted, or access was revoked from system settings.
      Timber.e("No access to the slideshow folder %s: %s", treeUri, e.getMessage());
      return images;
    } catch (Exception e) {
      Timber.e("Could not read the slideshow folder %s: %s", treeUri, e.getMessage());
      return images;
    }

    // Not Comparator.comparing: that is API 24 and this app still runs on 23.
    Collections.sort(
        found,
        new Comparator<String[]>() {
          @Override
          public int compare(String[] a, String[] b) {
            return String.CASE_INSENSITIVE_ORDER.compare(a[0], b[0]);
          }
        });
    for (String[] entry : found) {
      images.add(DocumentsContract.buildDocumentUriUsingTree(treeUri, entry[1]));
    }
    Timber.d("Slideshow folder %s holds %s images", treeUri, images.size());
    return images;
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
