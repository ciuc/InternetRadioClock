package ro.antiprotv.radioclock.service.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import java.text.SimpleDateFormat;
import ro.antiprotv.radioclock.R;

public abstract class Profile {
  protected int clockColor;
  protected boolean moveText;
  protected boolean showSeconds;
  protected final Context context;
  protected int brightness;
  protected float clockSize;
  protected boolean clock24h;
  protected boolean clock12hShowAmPm;
  protected SimpleDateFormat clockFormat;
  protected String font;
  protected boolean showDate;
  protected int dateSize;
  protected SharedPreferences prefs;
  protected boolean slideShowEnabled;

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
      int dateSize,
      boolean slideShowEnabled) {
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

    this.clock24h =
        prefs.getBoolean(context.getResources().getString(R.string.setting_key_clock24), false);
    this.clock12hShowAmPm =
        prefs.getBoolean(context.getResources().getString(R.string.setting_key_clock24ampm), true);
    this.slideShowEnabled = slideShowEnabled;
  }

  protected void setup() {
    StringBuilder clockPattern = new StringBuilder();
    if (clock24h) {
      clockPattern.append("hh:mm");
    } else {
      clockPattern.append("HH:mm");
    }
    if (showSeconds) {
      clockPattern.append(":ss");
    }
    if (clock24h && clock12hShowAmPm) {
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

  public void saveClockColor(String color) {
    this.clockColor = Color.parseColor(color);
  }

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

  public void saveSlideshowEnabled(boolean slideShowEnabled) {
    this.slideShowEnabled = slideShowEnabled;
  }

  public boolean isSlideshowEnabled() {
    return slideShowEnabled;
  }

  public void saveShowSeconds(boolean showSeconds) {
    this.showSeconds = showSeconds;
  }

  public void saveClock24(boolean clock24h) {
    prefs
        .edit()
        .putBoolean(context.getResources().getString(R.string.setting_key_clock24), clock24h)
        .apply();
    this.clock24h = clock24h;
  }
}
