package ro.antiprotv.radioclock.service.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import ro.antiprotv.radioclock.R;
import timber.log.Timber;

public class NightProfile extends Profile {

  public NightProfile(SharedPreferences prefs, Context context) {
    super(
        prefs,
        context,
        Color.parseColor(
            prefs.getString(
                context.getResources().getString(R.string.setting_key_clockColor_night),
                context.getResources().getString(R.string.setting_default_clockColor))),
        Float.parseFloat(
            prefs.getString(
                context.getResources().getString(R.string.setting_key_clockSize_night),
                context.getResources().getString(R.string.setting_default_clockSize))),
        prefs.getInt(
            context.getResources().getString(R.string.setting_key_clockBrightness_night), -1),
        prefs.getBoolean(
            context.getResources().getString(R.string.setting_key_clockMove_night), true),
        prefs.getBoolean(
            context.getResources().getString(R.string.setting_key_seconds_night), true),
        prefs.getString(
            context.getResources().getString(R.string.setting_key_typeface_night), "repet___.ttf"),
        prefs.getBoolean(
            context.getResources().getString(R.string.setting_key_showdate_night), false),
        prefs.getInt(context.getResources().getString(R.string.setting_key_datesize_night), 3),
        prefs.getBoolean(context.getString(R.string.setting_key_slideshow_enabled_night), false));
  }

  @Override
  public void saveFont(String font) {
    prefs
        .edit()
        .putString(context.getResources().getString(R.string.setting_key_typeface_night), font)
        .apply();
    super.saveFont(font);
  }

  @Override
  public void saveSize(float size) {
    super.saveSize(size);
    prefs
        .edit()
        .putString(
            context.getResources().getString(R.string.setting_key_clockSize_night),
            String.valueOf(size))
        .apply();
  }

  @Override
  public void saveBrightness(int brightness) {
    Timber.d("(Night) Save brightness: " + brightness);
    prefs
        .edit()
        .putInt(
            context.getResources().getString(R.string.setting_key_clockBrightness_night),
            brightness)
        .apply();
  }

  @Override
  public void saveClockColor(String color) {
    prefs
        .edit()
        .putString(context.getResources().getString(R.string.setting_key_clockColor_night), color)
        .apply();
    super.saveClockColor(color);
  }

  public void saveDateSize(int dateSize) {
    prefs
        .edit()
        .putInt(context.getResources().getString(R.string.setting_key_datesize_night), dateSize)
        .apply();
    super.saveDateSize(dateSize);
  }

  public void saveShowDate(boolean showDate) {
    prefs
        .edit()
        .putBoolean(context.getResources().getString(R.string.setting_key_showdate_night), showDate)
        .apply();
    super.saveShowDate(showDate);
  }

  public void saveSlideshowEnabled(boolean slideShowEnabled) {
    prefs
        .edit()
        .putBoolean(
            context.getString(R.string.setting_key_slideshow_enabled_night), slideShowEnabled)
        .apply();
  }

  @Override
  public void saveShowSeconds(boolean showSeconds) {
    prefs
        .edit()
        .putBoolean(
            context.getResources().getString(R.string.setting_key_seconds_night), showSeconds)
        .apply();
    super.saveShowSeconds(showSeconds);
  }

}
