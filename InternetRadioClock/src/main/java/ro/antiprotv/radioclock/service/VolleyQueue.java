package ro.antiprotv.radioclock.service;

import android.content.Context;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * One Volley queue for the whole process. Volley starts five dispatcher threads per queue and its
 * disk cache is one directory, so a queue per caller would both pile up threads and leave two
 * caches writing over each other.
 */
public final class VolleyQueue {

  private static RequestQueue queue;

  private VolleyQueue() {}

  public static synchronized RequestQueue get(Context context) {
    if (queue == null) {
      queue = Volley.newRequestQueue(context.getApplicationContext());
    }
    return queue;
  }
}
