package ro.antiprotv.radioclock.preference;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceDialogFragmentCompat;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.service.profile.ProfileUtils;

public class TimePreferenceDialogCompat extends PreferenceDialogFragmentCompat {
  private static String this_key;

  public static TimePreferenceDialogCompat newInstance(String key) {
    this_key = key;

    TimePreferenceDialogCompat fragment = new TimePreferenceDialogCompat();
    Bundle bundle = new Bundle(1);
    bundle.putString(ARG_KEY, key);
    fragment.setArguments(bundle);
    return fragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    SharedPreferences prefs = getPreference().getSharedPreferences();
    String currentTime =
        prefs.getString(this_key, ((TimePreference) getPreference()).getDefaultValue());
    int currentHour = ProfileUtils.getHour(currentTime);
    int currentMinute = ProfileUtils.getMinute(currentTime);
    try {
      TimePicker picker = new TimePicker(getContext());
      picker.setIs24HourView(true);
      return new TimePickerDialog(
          getContext(),
          new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
              Calendar now = Calendar.getInstance();
              now.set(Calendar.HOUR_OF_DAY, hourOfDay);
              now.set(Calendar.MINUTE, minute);

              String pref = new SimpleDateFormat("HH:mm").format(now.getTime());
              prefs.edit().putString(this_key, pref).apply();
              getPreference().setSummary(pref);
            }
          },
          currentHour,
          currentMinute,
          true);
    } catch (Throwable t) {
      AlertDialog customTimeText = new CustomTimeDialog(getContext(), prefs, currentTime);

      return customTimeText;
    }
  }

  @Override
  public void onDialogClosed(boolean positiveResult) {}

  private class CustomTimeDialog extends AlertDialog {
    public CustomTimeDialog(Context context, SharedPreferences prefs, String currentTime) {
      super(context);
      View view = LayoutInflater.from(context).inflate(R.layout.dialog_custom_time_text, null);
      setView(view);
      setTitle("On some old devices you cannot use the time picker.");
      setIcon(R.drawable.baseline_calendar_month_24);
      Button ok = view.findViewById(R.id.dialog_custom_time_button_ok);
      Button cancel = view.findViewById(R.id.dialog_custom_time_button_cancel);
      TextInputEditText input = view.findViewById(R.id.dialog_custom_time_time);
      input.setText(currentTime);
      TextView error = view.findViewById(R.id.dialog_custom_time_error);
      AlertDialog dialog = this;
      ok.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              String time = input.getText().toString();
              // vallidate input
              Pattern timeValidator = Pattern.compile("[0-2][0-9]:[0-5][0-9]");
              Matcher matcher = timeValidator.matcher(time);
              if (!matcher.matches()) {
                error.setVisibility(View.VISIBLE);
              } else {
                error.setVisibility(View.GONE);
                prefs.edit().putString(this_key, time).apply();
                dialog.cancel();
              }
            }
          });
      cancel.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              dialog.cancel();
            }
          });
    }
  }
}
