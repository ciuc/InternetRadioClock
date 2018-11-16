package ro.antiprotv.radioclock;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

public class TimePreference extends DialogPreference {
    private int lastHour=0;
    private int lastMinute=0;
    private TimePicker picker=null;
    Context context;

    public static int getHour(String time) {
        String[] pieces=time.split(":");

        return(Integer.parseInt(pieces[0]));
    }

    public static int getMinute(String time) {
        String[] pieces=time.split(":");

        return(Integer.parseInt(pieces[1]));
    }

    public TimePreference(Context ctxt, AttributeSet attrs) {
        super(ctxt, attrs);
        this.context = ctxt;
        //setPositiveButtonText("Set");
        //setNegativeButtonText("Cancel");
    }

    @Override
    protected View onCreateDialogView() {
        picker=new TimePicker(getContext());
        picker.setIs24HourView(true);
        return(picker);
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        lastHour = getSharedPreferences().getInt(v.getContext().getString(R.string.setting_key_night_alarm_hour), 0);
        lastMinute = getSharedPreferences().getInt(v.getContext().getString(R.string.setting_key_night_alarm_minute), 0);
        picker.setCurrentHour(lastHour);
        picker.setCurrentMinute(lastMinute);

    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            lastHour=picker.getCurrentHour();
            lastMinute=picker.getCurrentMinute();

            String time=String.valueOf(lastHour)+":"+String.valueOf(lastMinute);

            if (callChangeListener(time)) {
                persistString(time);
                setNightModeAlarm(lastHour, lastMinute);
                setSummary(String.format("%02d:%02d", lastHour, lastMinute));
            }
        }
    }

    private void setNightModeAlarm(int hour, int minute) {
        SharedPreferences prefs = getSharedPreferences();
        prefs.edit().putInt(context.getResources().getString(R.string.setting_key_night_alarm_hour), hour).apply();
        prefs.edit().putInt(context.getResources().getString(R.string.setting_key_night_alarm_minute), minute).apply();
    }

}