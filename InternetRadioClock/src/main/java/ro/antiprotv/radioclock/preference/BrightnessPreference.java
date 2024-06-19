package ro.antiprotv.radioclock.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;
import ro.antiprotv.radioclock.R;

public class BrightnessPreference extends DialogPreference {
  // ------------------------------------------------------------------------------------------
  // Private attributes :
  private static final String androidns = "http://schemas.android.com/apk/res/android";
  // private final Context mContext;
  // private final String mDialogMessage;
  // private final String mSuffix;
  // private final int mDefault;
  private SeekBar mSeekBar;
  private TextView mValueText;
  private int mMax;
  private int mValue = 0;
  private CheckBox checkboxAuto;

  // ------------------------------------------------------------------------------------------

  // ------------------------------------------------------------------------------------------
  // Constructor :

  public BrightnessPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public BrightnessPreference(Context ctxt, AttributeSet attrs, int defStyle) {
    super(ctxt, attrs, defStyle);
  }

  // ------------------------------------------------------------------------------------------

  // ------------------------------------------------------------------------------------------
  // DialogPreference methods :
  /*@Override
  protected View onCreateDialogView() {

    LinearLayout.LayoutParams params;
    LinearLayout layout = new LinearLayout(mContext);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(6, 6, 6, 6);

    TextView mSplashText = new TextView(mContext);
    // mSplashText.setPadding(30, 10, 30, 10);
    if (mDialogMessage != null) {
      mSplashText.setText(mDialogMessage);
    }
    layout.addView(mSplashText);

    mValueText = new TextView(mContext);
    mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
    mValueText.setTextSize(32);
    mValueText.setTextColor(Color.DKGRAY);
    params =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    layout.addView(mValueText, params);

    mSeekBar = new SeekBar(mContext);
    mSeekBar.setOnSeekBarChangeListener(this);
    layout.addView(
        mSeekBar,
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

    if (shouldPersist()) {
      mValue = getPersistedInt(mDefault);
    }

    mSeekBar.setMax(mMax);
    mSeekBar.setProgress(mValue);

    checkboxAuto = new CheckBox(mContext);
    checkboxAuto.setText(R.string.auto_brightness_description);

    checkboxAuto.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          if (isChecked) {
            mSeekBar.setEnabled(false);
            mValueText.setTextColor(Color.LTGRAY);
            // mValueText.setText("AUTO");
          } else {
            mSeekBar.setEnabled(true);
            mValueText.setTextColor(Color.DKGRAY);
            // mValueText.setText(String.format("%d%%", mValue < 0 ? 100 : mSeekBar.getProgress()));

          }
        });
    layout.addView(
        checkboxAuto,
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

    return layout;
  }

  @Override
  protected void onBindDialogView(View v) {
    super.onBindDialogView(v);
    mSeekBar.setMax(mMax);
    mSeekBar.setProgress(mValue);
    if (mValue == -1) {
      checkboxAuto.setChecked(true);
    }
  }

  @Override
  public void onDialogClosed(boolean positiveResult) {

  }

  @Override
  protected void onSetInitialValue(boolean restore, Object defaultValue) {
    super.onSetInitialValue(restore, defaultValue);
    if (restore) {
      mValue = shouldPersist() ? getPersistedInt(mDefault) : -1;
    } else {
      mValue = (Integer) defaultValue;
    }
  }

  // ------------------------------------------------------------------------------------------

  // ------------------------------------------------------------------------------------------
  // OnSeekBarChangeListener methods :
  @Override
  public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
    String t = String.valueOf(value);
    mValueText.setText(mSuffix == null ? t : t.concat(" " + mSuffix));
  }

  @Override
  public void onStartTrackingTouch(SeekBar seek) {}

  @Override
  public void onStopTrackingTouch(SeekBar seek) {}

  public int getMax() {
    return mMax;
  }

  public void setMax(int max) {
    mMax = max;
  }

  public int getProgress() {
    return mValue;
  }

  public void setProgress(int progress) {
    mValue = progress;
    if (mSeekBar != null) {
      mSeekBar.setProgress(progress);
    }
  }

  // ------------------------------------------------------------------------------------------

  // ------------------------------------------------------------------------------------------
  // Set the positive button listener and onClick action :
  @Override
  public void showDialog(Bundle state) {

    super.showDialog(state);

    Button positiveButton = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
    positiveButton.setOnClickListener(this);
  }

  @Override
  public void onClick(View v) {

    mValue = mSeekBar.getProgress();
    // if (shouldPersist()) {
    if (checkboxAuto.isChecked()) {
      mValue = -1;
    }
    persistInt(mValue);
    callChangeListener(mValue);
    // }

    getDialog().dismiss();
  }
  // ------------------------------------------------------------------------------------------

   */

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
