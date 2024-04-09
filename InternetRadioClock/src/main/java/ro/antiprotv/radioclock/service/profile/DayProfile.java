package ro.antiprotv.radioclock.service.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;

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
                    context.getResources().getString(R.string.setting_key_clockBrightness), 100)
            / 100,
        prefs.getBoolean(context.getResources().getString(R.string.setting_key_clockMove), true),
        prefs.getBoolean(context.getResources().getString(R.string.setting_key_seconds), true),
        Typeface.createFromAsset(
            context.getAssets(),
            "fonts/"
                + prefs.getString(
                    context.getResources().getString(R.string.setting_key_typeface),
                    "digital-7.mono.ttf")));
  }
}
