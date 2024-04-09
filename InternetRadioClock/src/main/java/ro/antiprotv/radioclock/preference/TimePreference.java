package ro.antiprotv.radioclock.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.service.profile.ProfileManager;
import ro.antiprotv.radioclock.service.profile.ProfileUtils;

public class TimePreference extends DialogPreference {
  private static final String TIME_FORMAT = "%d:%d";
  private int lastHour = 0;
  private int lastMinute = 0;
  private TimePicker picker = null;

  public TimePreference(Context ctxt) {
    this(ctxt, null);
  }

  public TimePreference(Context ctxt, AttributeSet attrs) {
    this(ctxt, attrs, android.R.attr.dialogPreferenceStyle);
  }

  public TimePreference(Context ctxt, AttributeSet attrs, int defStyle) {
    super(ctxt, attrs, defStyle);

    setPositiveButtonText(R.string.ok);
    setNegativeButtonText(R.string.cancel);
  }

  @Override
  protected View onCreateDialogView() {
    picker = new TimePicker(getContext());
    picker.setIs24HourView(true);
    return (picker);
  }

  @Override
  protected void onBindDialogView(View v) {
    super.onBindDialogView(v);
    picker.setCurrentHour(lastHour);
    picker.setCurrentMinute(lastMinute);
  }

  @Override
  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);

    if (positiveResult) {
      lastHour = picker.getCurrentHour();
      lastMinute = picker.getCurrentMinute();
      Calendar now = Calendar.getInstance();
      now.set(Calendar.HOUR_OF_DAY, lastHour);
      now.set(Calendar.MINUTE, lastMinute);

      String pref = new SimpleDateFormat("HH:mm").format(now.getTime());
      setSummary(pref);
      if (callChangeListener(pref)) {
        persistString(pref);
        notifyChanged();
      }
    }
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    return (a.getString(index));
  }

  @Override
  protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
    String time = null;
    if (restoreValue) {
      if (defaultValue == null) {
        time = getPersistedString("00:00");
      } else {
        time = getPersistedString(defaultValue.toString());
      }
    } else {
      time = defaultValue.toString();
    }

    lastHour = ProfileUtils.getHour(time);
    lastMinute = ProfileUtils.getMinute(time);
    setSummary(getSummary());
  }
}
