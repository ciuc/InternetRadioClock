package ro.antiprotv.radioclock.listener;

import static android.view.View.FOCUS_LEFT;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.fragment.SettingsTimersFragment;
import ro.antiprotv.radioclock.fragment.TimerFormatterTextWatcher;
import ro.antiprotv.radioclock.service.TimerService;

public class InstantTimerOnLongClickListener implements View.OnLongClickListener {
  private final TimerService timerService;
  private String lastUsed = "30";

  public InstantTimerOnLongClickListener(TimerService timerService) {
    this.timerService = timerService;
  }

  @Override
  public boolean onLongClick(View view) {
    InputMethodManager imm =
        (InputMethodManager) view.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
    AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
    LinearLayout layout =
        (LinearLayout)
            LayoutInflater.from(view.getContext())
                .inflate(R.layout.dialog_edit_instant_timer, null);
    final EditText timerInput = layout.findViewById(R.id.instant_timer);
    timerInput.setText(lastUsed);
    TimerFormatterTextWatcher textWatcher = new TimerFormatterTextWatcher(timerInput);

    timerInput.setGravity(Gravity.LEFT);
    timerInput.addTextChangedListener(textWatcher);
    timerInput.setFocusable(true);
    timerInput.requestFocus(FOCUS_LEFT);
    timerInput.setFocusableInTouchMode(true);
    timerInput.requestFocusFromTouch();
    timerInput.setSelection(0);

    imm.showSoftInput(timerInput, InputMethodManager.SHOW_FORCED);
    builder.setView(layout);
    builder.setNegativeButton(
        R.string.cancel,
        (dialog, which) -> {
          dialog.cancel();
        });
    builder.setPositiveButton(
        R.string.yes,
        (dialog, which) -> {
          String value = timerInput.getText().toString();
          int timer = SettingsTimersFragment.convertToSeconds(value);
          timerService.startInstantTimer(view.getId(), timer);
          lastUsed = String.valueOf(timer);
        });
    builder.setOnCancelListener(
        new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
          }
        });
    builder.setOnDismissListener(
        new DialogInterface.OnDismissListener() {
          @Override
          public void onDismiss(DialogInterface dialog) {
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
          }
        });

    timerInput.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            timerInput.setSelection(0);
          }
        });
    builder.show();

    return true;
  }
}
