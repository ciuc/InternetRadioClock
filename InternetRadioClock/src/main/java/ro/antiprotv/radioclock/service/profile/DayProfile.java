package ro.antiprotv.radioclock.service.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import ro.antiprotv.radioclock.R;

public class DayProfile extends Profile {

  public DayProfile(SharedPreferences prefs, Context context) {
    super(
        prefs,
        context,
        Color.parseColor(
            prefs.getString(
                context.getResources().getString(R.string.setting_key_clockColor),
                context.getResources().getString(R.string.setting_default_clockColor))),
        Integer.parseInt(
            prefs.getString(
                context.getResources().getString(R.string.setting_key_clockSize),
                context.getResources().getString(R.string.setting_default_clockSize))),
        (float)
                prefs.getInt(
                    context.getResources().getString(R.string.setting_key_clockBrightness), -1)
            / 100,
        prefs.getBoolean(context.getResources().getString(R.string.setting_key_clockMove), true),
        prefs.getBoolean(context.getResources().getString(R.string.setting_key_seconds), true),
        prefs.getString(
            context.getResources().getString(R.string.setting_key_typeface), "repet___.ttf"));
  }

  @Override
  public void setFont(String font) {
    prefs
        .edit()
        .putString(context.getResources().getString(R.string.setting_key_typeface), font)
        .apply();
  }

  @Override
  public void setSize(int size) {
    prefs
        .edit()
        .putString(
            context.getResources().getString(R.string.setting_key_clockSize), String.valueOf(size))
        .apply();
  }

  @Override
  public void setClockColor(String color) {
    prefs
        .edit()
        .putString(context.getResources().getString(R.string.setting_key_clockColor), color)
        .apply();
  }
}
