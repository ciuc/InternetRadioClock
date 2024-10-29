package ro.antiprotv.radioclock.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;
import ro.antiprotv.radioclock.service.profile.ProfileManager;

/** Time Picker for use in preference dialog in night profile activity */
public class TimePreference extends DialogPreference {

  public TimePreference(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public TimePreference(Context ctxt, AttributeSet attrs, int defStyle) {
    super(ctxt, attrs, defStyle);
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    return (a.getString(index));
  }

  @Override
  protected void onSetInitialValue(Object defaultValue) {
    setSummary(getSummary());
  }

  public String getDefaultValue() {
    if (getKey().equals(ProfileManager.SETTING_NIGHT_PROFILE_AUTOSTART)) {
      return "21:00";
    }
    return "07:00";
  }
}
