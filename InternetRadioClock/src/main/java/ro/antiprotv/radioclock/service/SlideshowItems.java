package ro.antiprotv.radioclock.service;

import android.net.Uri;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import timber.log.Timber;

/**
 * Reads and writes the individually picked slideshow files, stored as JSON in a preference.
 *
 * <p>Two formats are in circulation. The original one is a plain array of URI strings, written
 * before the slideshow could show videos; the current one is an array of objects that also records
 * which of the two a URI is. The kind is stored rather than worked out at display time because
 * asking the provider costs a binder call per file, and a selection can run to hundreds.
 *
 * <p>Both formats are read. An entry from the old format is always a picture: the picker that wrote
 * it only ever offered {@code image/*}, so there is nothing to guess at. Nothing rewrites the
 * preference on upgrade either - the pickers replace it wholesale anyway, and the next selection
 * lands in the new format on its own.
 */
public final class SlideshowItems {
  private static final String KEY_URI = "u";
  private static final String KEY_KIND = "k";
  private static final String KIND_VIDEO = "v";
  private static final String KIND_IMAGE = "i";

  private SlideshowItems() {}

  /** The saved selection, in the order it was picked. Never null; unreadable JSON gives an empty list. */
  public static List<SlideshowItem> parse(String json) {
    List<SlideshowItem> items = new ArrayList<>();
    JSONArray array = toArray(json);
    if (array == null) {
      return items;
    }
    for (int i = 0; i < array.length(); i++) {
      Object entry = array.opt(i);
      if (entry instanceof String) {
        // Old format: a bare URI, and always a picture.
        items.add(new SlideshowItem(Uri.parse((String) entry), false));
      } else if (entry instanceof JSONObject) {
        JSONObject object = (JSONObject) entry;
        String uri = object.optString(KEY_URI, "");
        if (uri.isEmpty()) {
          continue;
        }
        String kind = object.optString(KEY_KIND, "");
        boolean video =
            KIND_VIDEO.equals(kind)
                // A kind we do not recognise; fall back on the file name.
                || (!KIND_IMAGE.equals(kind) && SlideshowItem.looksLikeVideoName(uri));
        items.add(new SlideshowItem(Uri.parse(uri), video));
      }
    }
    return items;
  }

  public static String toJson(List<SlideshowItem> items) {
    JSONArray array = new JSONArray();
    for (SlideshowItem item : items) {
      JSONObject object = new JSONObject();
      try {
        object.put(KEY_URI, item.uri.toString());
        object.put(KEY_KIND, item.video ? KIND_VIDEO : KIND_IMAGE);
      } catch (Exception e) {
        Timber.e("Could not store slideshow item %s: %s", item.uri, e.getMessage());
        continue;
      }
      array.put(object);
    }
    return array.toString();
  }

  /**
   * The saved URIs as strings, for reconciling the persisted access grants.
   *
   * @return the URIs, or null if the JSON could not be read - which callers must treat as "no idea",
   *     not as "nothing saved".
   */
  public static Set<String> uriStrings(String json) {
    JSONArray array = toArray(json);
    if (array == null) {
      return null;
    }
    Set<String> uris = new HashSet<>();
    for (SlideshowItem item : parse(json)) {
      uris.add(item.uri.toString());
    }
    return uris;
  }

  /** How many pictures and how many videos are saved, as {@code {images, videos}}. */
  public static int[] counts(String json) {
    int images = 0;
    int videos = 0;
    for (SlideshowItem item : parse(json)) {
      if (item.video) {
        videos++;
      } else {
        images++;
      }
    }
    return new int[] {images, videos};
  }

  private static JSONArray toArray(String json) {
    try {
      return new JSONArray(json == null ? "[]" : json);
    } catch (Exception e) {
      Timber.e("Could not read the saved slideshow selection: %s", e.getMessage());
      return null;
    }
  }
}
