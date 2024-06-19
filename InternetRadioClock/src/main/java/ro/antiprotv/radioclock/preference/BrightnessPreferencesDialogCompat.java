package ro.antiprotv.radioclock.preference;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceDialogFragmentCompat;
import ro.antiprotv.radioclock.R;

public class BrightnessPreferencesDialogCompat extends PreferenceDialogFragmentCompat
    implements SeekBar.OnSeekBarChangeListener {
  private TextView mValueText;
  private SeekBar seekBar;
  private CheckBox checkboxAuto;

  public static BrightnessPreferencesDialogCompat newInstance(String key) {

    Bundle bundle = new Bundle(1);
    bundle.putString(ARG_KEY, key);
    BrightnessPreferencesDialogCompat fragment = new BrightnessPreferencesDialogCompat();
    fragment.setArguments(bundle);
    return fragment;
  }

  @Nullable
  @Override
  protected View onCreateDialogView(@NonNull Context mContext) {
    LinearLayout.LayoutParams params;
    LinearLayout layout = new LinearLayout(mContext);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(6, 6, 6, 6);

    TextView mSplashText = new TextView(mContext);

    layout.addView(mSplashText);

    mValueText = new TextView(mContext);
    mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
    mValueText.setTextSize(32);
    mValueText.setTextColor(Color.DKGRAY);
    params =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    layout.addView(mValueText, params);

    seekBar = new SeekBar(mContext);
    seekBar.setOnSeekBarChangeListener(this);
    layout.addView(
        seekBar,
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

    seekBar.setMax(100);
    int brightness = getPreference().getSharedPreferences().getInt(getPreference().getKey(), 100);
    seekBar.setProgress(brightness);
    if (brightness == -1) {
      seekBar.setEnabled(false);
    }

    getCheckBox(mContext, brightness);
    layout.addView(
        checkboxAuto,
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

    return layout;
  }

  private void getCheckBox(@NonNull Context mContext, int brightness) {
    checkboxAuto = new CheckBox(mContext);
    checkboxAuto.setText(R.string.auto_brightness_description);
    checkboxAuto.setChecked(brightness == -1);

    checkboxAuto.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          if (isChecked) {
            seekBar.setEnabled(false);
            mValueText.setTextColor(Color.LTGRAY);
            // mValueText.setText("AUTO");
          } else {
            seekBar.setEnabled(true);
            mValueText.setTextColor(Color.DKGRAY);
            // mValueText.setText(String.format("%d%%", mValue < 0 ? 100 : mSeekBar.getProgress()));

          }
        });
  }

  @Override
  public void onDialogClosed(boolean positiveResult) {
    int brightness = seekBar.getProgress();
    if (checkboxAuto.isChecked()) {
      brightness = -1;
    }
    getPreference()
        .getSharedPreferences()
        .edit()
        .putInt(getPreference().getKey(), brightness)
        .apply();

    getPreference().setSummary(BrightnessPreference.getSummary(brightness, getContext()));

    getDialog().dismiss();
  }

  @Override
  public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
    String t = String.valueOf(value);
    mValueText.setText(t);
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {}

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {}
}
