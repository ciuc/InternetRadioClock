package ro.antiprotv.radioclock.service;

import android.net.Uri;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** One thing the slideshow can show: a picture, or a video to play in its place. */
public final class SlideshowItem {
  public final Uri uri;
  public final boolean video;

  public SlideshowItem(Uri uri, boolean video) {
    this.uri = uri;
    this.video = video;
  }

  private static final Set<String> VIDEO_EXTENSIONS =
      new HashSet<>(
          Arrays.asList(
              "mp4", "m4v", "mkv", "webm", "3gp", "3gpp", "mov", "avi", "mpg", "mpeg", "ts", "wmv",
              "flv"));

  public static boolean isVideoMime(String mime) {
    return mime != null && mime.startsWith("video/");
  }

  /**
   * Whether a name looks like a video, judged by its extension. Only a fallback: providers are
   * supposed to report a mime type, but some hand back nothing at all or a blanket
   * {@code application/octet-stream} for the less common containers.
   */
  public static boolean looksLikeVideoName(String name) {
    if (name == null) {
      return false;
    }
    int dot = name.lastIndexOf('.');
    if (dot < 0 || dot + 1 >= name.length()) {
      return false;
    }
    return VIDEO_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.US));
  }

  /** Whether a mime type is unhelpful enough to be worth second-guessing by name. */
  public static boolean isVagueMime(String mime) {
    return mime == null || mime.isEmpty() || "application/octet-stream".equals(mime);
  }
}
