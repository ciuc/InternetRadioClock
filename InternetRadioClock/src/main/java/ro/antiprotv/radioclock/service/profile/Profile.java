package ro.antiprotv.radioclock.service.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import java.text.SimpleDateFormat;
import ro.antiprotv.radioclock.R;

public abstract class Profile {
  protected final int clockColor;
  protected final float alpha;
  protected float clockSize;
  protected final boolean moveText;
  protected final boolean showSeconds;
  protected final Context context;
  protected boolean clock12h;
  protected boolean clock12hShowAmPm;
  protected SimpleDateFormat clockFormat;
  protected String font;
  protected SharedPreferences prefs;

  public Profile(
      SharedPreferences prefs,
      Context context,
      int clockColor,
      float clockSize,
      float alpha,
      boolean moveText,
      boolean showSeconds,
      String font) {
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

  abstract public void setFont(String font);
  public void setSize(float size) {
    this.clockSize = size;
  }

  abstract public void setClockColor(String color);
  public int getColor(){
    return clockColor;
  }
  public int getSize() {return (int) clockSize;}
}
