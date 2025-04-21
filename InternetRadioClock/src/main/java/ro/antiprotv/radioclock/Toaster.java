package ro.antiprotv.radioclock;

import android.content.Context;
import android.widget.Toast;

public class Toaster {
  public static void toast(Context context, String text, int length) {
    Toast.makeText(context, text, length).show();
  }
}
