package ro.antiprotv.radioclock.service.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import ro.antiprotv.radioclock.R;
import timber.log.Timber;

public class DayProfile extends Profile {

  public DayProfile(SharedPreferences prefs, Context context) {
    super(
        prefs,
        context,
        Color.parseColor(
            prefs.getString(
                context.getResources().getString(R.string.setting_key_clockColor),
                context.getResources().getString(R.string.setting_default_clockColor))),
        Float.parseFloat(
            prefs.getString(
                context.getResources().getString(R.string.setting_key_clockSize),
                context.getResources().getString(R.string.setting_default_clockSize))),
        prefs.getInt(context.getResources().getString(R.string.setting_key_clockBrightness), -1),
        prefs.getBoolean(context.getResources().getString(R.string.setting_key_clockMove), true),
        prefs.getBoolean(context.getResources().getString(R.string.setting_key_seconds), true),
        prefs.getString(
            context.getResources().getString(R.string.setting_key_typeface), "repet___.ttf"),
        prefs.getBoolean(context.getResources().getString(R.string.setting_key_showdate), false),
        prefs.getInt(context.getResources().getString(R.string.setting_key_datesize), 3));
  }

  @Override
  public void saveFont(String font) {
    prefs
        .edit()
        .putString(context.getResources().getString(R.string.setting_key_typeface), font)
        .apply();
    super.saveFont(font);
  }

  @Override
  public void saveSize(float size) {
    super.saveSize(size);
    prefs
        .edit()
        .putString(
            context.getResources().getString(R.string.setting_key_clockSize), String.valueOf(size))
        .apply();
  }

  @Override
  public void saveBrightness(int brightness) {
    Timber.d("(Day) Save brightness: " + brightness);
    prefs
        .edit()
        .putInt(context.getResources().getString(R.string.setting_key_clockBrightness), brightness)
        .apply();
  }

  @Override
  public void saveClockColor(String color) {
    prefs
        .edit()
        .putString(context.getResources().getString(R.string.setting_key_clockColor), color)
        .apply();
    super.saveClockColor(color);
  }

  public void saveDateSize(int dateSize) {
    prefs
        .edit()
        .putInt(context.getResources().getString(R.string.setting_key_datesize), dateSize)
        .apply();
    super.saveDateSize(dateSize);
  }

  public void saveShowDate(boolean showDate) {
    prefs
        .edit()
        .putBoolean(context.getResources().getString(R.string.setting_key_showdate), showDate)
        .apply();
    super.saveShowDate(showDate);
  }
}
