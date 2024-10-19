package ro.antiprotv.radioclock.preference;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;
import ro.antiprotv.radioclock.R;

public class BrightnessPreference extends DialogPreference {

  public BrightnessPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public BrightnessPreference(Context ctxt, AttributeSet attrs, int defStyle) {
    super(ctxt, attrs, defStyle);
  }

  public static String getSummary(int brightness, Context context) {

    if (brightness == -1) {
      return context
          .getString(R.string.setting_summary_clockBrightness)
          .replace("$1%", "AUTO (SYSTEM)");
    } else {
      return context
          .getString(R.string.setting_summary_clockBrightness)
          .replace("$1", "" + brightness);
    }
  }
}
