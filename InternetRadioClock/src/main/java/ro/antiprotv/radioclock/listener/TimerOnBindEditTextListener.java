package ro.antiprotv.radioclock.listener;

import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import ro.antiprotv.radioclock.fragment.TimerFormatterTextWatcher;

public class TimerOnBindEditTextListener implements EditTextPreference.OnBindEditTextListener {
  @Override
  public void onBindEditText(@NonNull EditText editText) {
    TimerFormatterTextWatcher textWatcher = new TimerFormatterTextWatcher(editText);
    // Set input type to number
    editText.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);
    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
    editText.setGravity(Gravity.LEFT);
    // Optionally, restrict input to only digits (0-9)
    editText.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
    editText.addTextChangedListener(textWatcher);
    editText.setOnFocusChangeListener(
        (v, hasFocus) -> {
          if (hasFocus) {
            editText.removeTextChangedListener(textWatcher);
            editText.addTextChangedListener(textWatcher);
          }
        });
  }
}
