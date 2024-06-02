package ro.antiprotv.radioclock.service.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;

import java.text.SimpleDateFormat;

import ro.antiprotv.radioclock.R;

public class Profile {
  protected boolean clock12h;
  protected boolean clock12hShowAmPm;
  protected final int clockColor;
  protected final float alpha;
  protected final float clockSize;
  protected final boolean moveText;
  protected final boolean showSeconds;
  private final Context context;
  protected SimpleDateFormat clockFormat;
  protected Typeface font;
  private SharedPreferences prefs;

  public Profile(
      SharedPreferences prefs,
      Context context,
      int clockColor,
      float clockSize,
      float alpha,
      boolean moveText,
      boolean showSeconds,
      Typeface font) {
    this.clockColor = clockColor;
    this.clockSize = clockSize;
    this.alpha = alpha;
    this.moveText = moveText;
    this.showSeconds = showSeconds;
    this.context = context;
    this.prefs = prefs;
    this.font = font;

    this.clock12h =
        prefs.getBoolean(context.getResources().getString(R.string.setting_key_clock24), false);
    this.clock12hShowAmPm =
        prefs.getBoolean(context.getResources().getString(R.string.setting_key_clock24ampm), true);
  }

  protected void setup() {
    StringBuilder clockPattern = new StringBuilder();
    if (!clock12h) {
      clockPattern.append("HH:mm");
    } else {
      clockPattern.append("hh:mm");
    }
    if (showSeconds) {
      clockPattern.append(":ss");
    }
    if (clock12h && clock12hShowAmPm) {
      clockPattern.append(" a");
    }
    String pattern = clockPattern.toString();
    if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
        && prefs.getBoolean(
            context.getResources().getString(R.string.setting_key_clock_vertical), true)) {
      pattern = pattern.replaceAll(":", ":\n");
      pattern = pattern.replaceAll(" ", "\n");
    }

    if (!prefs.getBoolean(
        context.getResources().getString(R.string.setting_key_clock_dots), true)) {
      pattern = pattern.replaceAll(":", "");
    }
    clockFormat = new SimpleDateFormat(pattern);

  }
}
