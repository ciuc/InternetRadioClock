package ro.antiprotv.radioclock.service;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ro.antiprotv.radioclock.BuildConfig;
import timber.log.Timber;

/**
 * Writes what the slideshow does to a file, so that a run left going for hours can be looked at
 * afterwards. Only for finding out why files come round more often than a walk through the folder
 * would bring them: nothing in the app reads this back, and it does nothing at all in a release
 * build.
 *
 * <p>The log is a tab-separated file, one event per line, in {@code
 * Android/data/&lt;package&gt;/files/slideshow-debug.log} - app-specific storage, so it needs no
 * permission and goes when the app is uninstalled. Pull it with:
 *
 * <pre>adb pull /sdcard/Android/data/ro.antiprotv.radioclock2/files/slideshow-debug.log</pre>
 *
 * <p>Every start of the slideshow opens a numbered session and writes the whole list in the order
 * it was built, so a file that keeps reappearing can be told apart from one the shuffle merely
 * happened to place early. Writing is done on a thread of its own: the events come from the main
 * thread, between showing one picture and the next, and must not wait on the disk.
 */
public final class SlideshowDebugLog {
  private static final String FILE_NAME = "slideshow-debug.log";
  /** Rotated at this size, keeping one older file. Hours of running come to a few hundred KB. */
  private static final long MAX_BYTES = 4L * 1024 * 1024;

  private final File file;
  private final ExecutorService writer = Executors.newSingleThreadExecutor();
  private final SimpleDateFormat stamp =
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
  /** Which run of the slideshow the events belong to; 0 until the first start. */
  private int session = 0;
  /** Which pass through the list, within the session. */
  private int pass = 0;

  /**
   * A log to write to, or null when there should not be one - which is every release build. Callers
   * hold the result as a nullable field and check it, so that a release build carries no more than
   * that check.
   */
  public static SlideshowDebugLog create(Context context) {
    if (!BuildConfig.DEBUG) {
      return null;
    }
    File dir = context.getApplicationContext().getExternalFilesDir(null);
    if (dir == null) {
      // No external storage mounted: fall back on the private directory, which adb can still
      // reach on a debuggable build through run-as.
      dir = context.getApplicationContext().getFilesDir();
    }
    return new SlideshowDebugLog(new File(dir, FILE_NAME));
  }

  private SlideshowDebugLog(File file) {
    this.file = file;
    Timber.d("Slideshow debug log at %s", file.getAbsolutePath());
  }

  /** Where the file is, for saying so on screen. */
  public String path() {
    return file.getAbsolutePath();
  }

  /**
   * Opens a session: a start of the slideshow, with what it was started with and what set it off.
   *
   * @param trigger what called into the slideshow, as a short stack of the app's own frames
   */
  public void start(String trigger, String settings) {
    session++;
    pass = 1;
    write(
        String.format(
            Locale.US,
            "START\tsession=%d\ttrigger=%s\t%s",
            session,
            trigger,
            settings));
  }

  /**
   * The whole list in the order it will be walked. Written once per pass - at the start and again
   * after every reshuffle - which is what makes a file that turns up too often visible: its
   * positions across passes are all there to count.
   */
  public void order(String reason, List<SlideshowItem> items) {
    StringBuilder line = new StringBuilder();
    line.append(
        String.format(
            Locale.US,
            "ORDER\tsession=%d\tpass=%d\treason=%s\tcount=%d",
            session,
            pass,
            reason,
            items.size()));
    for (int i = 0; i < items.size(); i++) {
      SlideshowItem item = items.get(i);
      line.append('\n')
          .append(
              String.format(
                  Locale.US,
                  "  %d\t%s\t%s",
                  i,
                  item.video ? "video" : "image",
                  name(item.uri)));
    }
    write(line.toString());
  }

  /** A file going up on screen. */
  public void show(int index, int size, Uri uri, boolean video, int turn) {
    write(
        String.format(
            Locale.US,
            "SHOW\tsession=%d\tpass=%d\tindex=%d/%d\tturn=%d\tkind=%s\tname=%s",
            session,
            pass,
            index,
            size,
            turn,
            video ? "video" : "image",
            name(uri)));
  }

  /** The end of a pass through the list; the next {@link #order} belongs to the new one. */
  public void passEnded() {
    pass++;
  }

  /** Anything else worth a line: a stop, a skip, the app going away and coming back. */
  public void event(String type, String detail) {
    write(String.format(Locale.US, "%s\tsession=%d\tpass=%d\t%s", type, session, pass, detail));
  }

  /**
   * The app's own frames from the current stack, top first, as {@code Class.method:line} joined by
   * {@code <-}. What actually answers the question of why the slideshow restarted.
   */
  public static String callers(int depth) {
    StackTraceElement[] frames = Thread.currentThread().getStackTrace();
    StringBuilder trace = new StringBuilder();
    int taken = 0;
    for (StackTraceElement frame : frames) {
      String className = frame.getClassName();
      if (!className.startsWith("ro.antiprotv.")
          || className.equals(SlideshowDebugLog.class.getName())) {
        continue;
      }
      if (taken > 0) {
        trace.append("<-");
      }
      trace
          .append(className.substring(className.lastIndexOf('.') + 1))
          .append('.')
          .append(frame.getMethodName())
          .append(':')
          .append(frame.getLineNumber());
      if (++taken >= depth) {
        break;
      }
    }
    return taken == 0 ? "?" : trace.toString();
  }

  /** The tail of a URI, which for a picked file or a folder entry is its name. */
  private static String name(Uri uri) {
    if (uri == null) {
      return "null";
    }
    String text = Uri.decode(uri.toString());
    int cut = Math.max(text.lastIndexOf('/'), text.lastIndexOf(':'));
    return cut >= 0 && cut + 1 < text.length() ? text.substring(cut + 1) : text;
  }

  private void write(String body) {
    final String line = stamp.format(new Date()) + "\t" + body;
    try {
      writer.execute(() -> append(line));
    } catch (Exception e) {
      // Shut down, or out of room in the queue. A debug log is not worth a crash.
      Timber.e("Could not queue a slideshow log line: %s", e.getMessage());
    }
  }

  private void append(String line) {
    try {
      if (file.length() > MAX_BYTES) {
        File previous = new File(file.getAbsolutePath() + ".1");
        //noinspection ResultOfMethodCallIgnored
        previous.delete();
        //noinspection ResultOfMethodCallIgnored
        file.renameTo(previous);
      }
      try (PrintWriter out = new PrintWriter(new FileWriter(file, true))) {
        out.println(line);
      }
    } catch (Exception e) {
      Timber.e("Could not write the slideshow log: %s", e.getMessage());
    }
  }
}
