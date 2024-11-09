package ro.antiprotv.radioclock.dialog;

import android.app.TimePickerDialog;
import android.content.Context;

/**
 * Necessary just to inhibit onStop to prevent double onTimeSet call on certain versions of android
 */
public class CustomTimePickerDialog extends TimePickerDialog {

  public CustomTimePickerDialog(
      Context context,
      OnTimeSetListener listener,
      int hourOfDay,
      int minute,
      boolean is24HourView) {
    super(context, listener, hourOfDay, minute, is24HourView);
  }

  @Override
  protected void onStop() {
    // inhibit
  }
}
