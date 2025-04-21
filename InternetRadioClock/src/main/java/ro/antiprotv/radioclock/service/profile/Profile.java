package ro.antiprotv.radioclock.service.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import java.text.SimpleDateFormat;
import ro.antiprotv.radioclock.R;

public abstract class Profile {
  protected final int clockColor;
  protected final boolean moveText;
  protected final boolean showSeconds;
  protected final Context context;
  protected int brightness;
  protected float clockSize;
  protected boolean clock12h;
  protected boolean clock12hShowAmPm;
  protected SimpleDateFormat clockFormat;
  protected String font;
  protected boolean showDate;
  protected int dateSize;
  protected SharedPreferences prefs;

  public Profile(
      SharedPreferences prefs,
      Context context,
      int clockColor,
      float clockSize,
      int brightness,
      boolean moveText,
      boolean showSeconds,
      String font,
      boolean showDate,
      int dateSize) {
    this.clockColor = clockColor;
    this.clockSize = clockSize;
    this.brightness = brightness;
    this.moveText = moveText;
    this.showSeconds = showSeconds;
    this.context = context;
    this.prefs = prefs;
    this.font = font;
    this.showDate = showDate;
    this.dateSize = dateSize;

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

  public void saveFont(String font) {
    this.font = font;
  }

  public abstract void saveBrightness(int brightness);

  public abstract void saveClockColor(String color);

  public int getColor() {
    return clockColor;
  }

  public int getSize() {
    return (int) clockSize;
  }

  public void saveSize(float size) {
    this.clockSize = size;
  }

  public String getFont() {
    return font;
  }

  /**
   * it is actually either 2 or 3
   *
   * <p>- 2 means smaller (font size / 2)
   *
   * @return
   */
  public int getDateSize() {
    return dateSize;
  }

  public boolean isShowDate() {
    return showDate;
  }

  protected void saveDateSize(int dateSize) {
    this.dateSize = dateSize;
  }

  public void saveShowDate(boolean showDate) {
    this.showDate = showDate;
  }
}
