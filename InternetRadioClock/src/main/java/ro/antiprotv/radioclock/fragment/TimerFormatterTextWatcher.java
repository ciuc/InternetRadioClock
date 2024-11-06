package ro.antiprotv.radioclock.fragment;

import android.text.Editable;
import android.widget.EditText;
import java.util.Arrays;
import timber.log.Timber;

class TimerFormatterTextWatcher implements android.text.TextWatcher {

  private final EditText editText;
  private final char[] chars = {'0', '0', '0', '0'};
  private boolean isUpdating = false;

  public TimerFormatterTextWatcher(EditText editText) {
    this.editText = editText;
    String value = editText.getText().toString();
    int totalSeconds = 10;
    try {
      totalSeconds = Integer.parseInt(value);
    } catch (Exception e) {
      Timber.d("error: %s", e.getMessage());
      value = "10";
    }
    Timber.d("text: %s", value);
    Timber.d(Arrays.toString(chars));
    for (int i = 0; i < value.length(); i++) {
      chars[i] = value.charAt(i);
    }
    Timber.d(Arrays.toString(chars));
    isUpdating = true;
    editText.setText(
        String.format(SettingsAlarmsFragment.TIMER_FORMAT, totalSeconds / 60, totalSeconds % 60));
    isUpdating = false;
  }

  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {
    if (isUpdating) {
      return;
    }
    isUpdating = true;

    Timber.d("onTextChanged: %b %s", isUpdating, s.toString());
    Timber.d("chars: %s", new String(chars));
    for (int i = 1; i < chars.length; i++) {
      chars[i - 1] = chars[i];
    }
    Timber.d("chars: %s", new String(chars));
    chars[3] = s.toString().replaceAll("\\D", "").charAt(0);
    String newValue = new String(chars);
    StringBuilder formatted = new StringBuilder();
    if (newValue.length() >= 2) {
      formatted.append(newValue.substring(0, 2)).append("m ");
      formatted.append(newValue.substring(2)).append("s");
    } else {
      formatted.append(newValue).append("s");
    }
    editText.setText(formatted.toString());
    isUpdating = false;
  }

  @Override
  public void afterTextChanged(Editable s) {}
}
