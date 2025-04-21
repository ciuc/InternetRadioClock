package ro.antiprotv.radioclock.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.activity.ClockActivity;

public class ButtonManager {
  private final Resources resources;
  private final Context context;
  private final View view;
  private final View.OnTouchListener onTouchListener;
  private final View.OnClickListener playListener;
  // the map of urls; it is a map of the setting key > url (String)
  // url(setting_key_stream1 >  http://something)
  private final List<String> mUrls = new ArrayList<>();
  private List<Button> buttons;
  private List<Integer> settingKeys;
  private List<Integer> defaultNames;
  private SharedPreferences prefs;
  // the button we have clicked on
  private Button mButtonClicked;
  private ImageButton onOffButton;
  private ImageButton timerPauseButton;
  private Button timerPlus10sButton;
  private Button timerMinus10sButton;
  private Button timerPlus1mButton;
  private Button timerMinus1mButton;

  public ButtonManager(Context ctx) {
    this(ctx, null, null, null, null);
  }

  public ButtonManager(
      Context ctx,
      View view,
      SharedPreferences prefs,
      View.OnTouchListener onTouchListener,
      View.OnClickListener playListener) {
    this.context = ctx;
    this.view = view;
    this.prefs = prefs;
    this.onTouchListener = onTouchListener;
    this.playListener = playListener;
    resources = context.getResources();
  }

  public void initializeButtons() {
    Button stream1 = view.findViewById(R.id.stream1);
    Button stream2 = view.findViewById(R.id.stream2);
    Button stream3 = view.findViewById(R.id.stream3);
    Button stream4 = view.findViewById(R.id.stream4);
    Button stream5 = view.findViewById(R.id.stream5);
    Button stream6 = view.findViewById(R.id.stream6);
    Button stream7 = view.findViewById(R.id.stream7);
    Button stream8 = view.findViewById(R.id.stream8);
    buttons = Arrays.asList(stream1, stream2, stream3, stream4, stream5, stream6, stream7, stream8);

    settingKeys =
        Arrays.asList(
            R.string.setting_key_label1,
            R.string.setting_key_label2,
            R.string.setting_key_label3,
            R.string.setting_key_label4,
            R.string.setting_key_label5,
            R.string.setting_key_label6,
            R.string.setting_key_label7,
            R.string.setting_key_label8);
    defaultNames =
        Arrays.asList(
            R.string.button_name_stream1,
            R.string.button_name_stream2,
            R.string.button_name_stream3,
            R.string.button_name_stream4,
            R.string.button_name_stream5,
            R.string.button_name_stream6,
            R.string.button_name_stream7,
            R.string.button_name_stream8);

    for (int i = 0; i < buttons.size(); i++) {
      Button b = buttons.get(i);
      b.setText(
          prefs.getString(
              resources.getString(settingKeys.get(i)), resources.getString(defaultNames.get(i))));
      b.setOnClickListener(playListener);
      b.setOnTouchListener(onTouchListener);
      b.setOnLongClickListener(new AddLabelOnLongClickListener(i + 1));
    }
    hideUnhideButtons();
    enableButtons();
    onOffButton = view.findViewById(R.id.on_off_button);
    timerPauseButton = view.findViewById(R.id.timer_pause);
    timerPlus10sButton = view.findViewById(R.id.timer_plus10);
    timerMinus10sButton = view.findViewById(R.id.timer_minus10);
    timerPlus1mButton = view.findViewById(R.id.timer_plus1m);
    timerMinus1mButton = view.findViewById(R.id.timer_minus1m);
  }

  void hideUnhideButtons() {
    for (int i = 0; i < buttons.size(); i++) {
      Button b = buttons.get(i);
      String url = mUrls.get(i);
      if (b.getVisibility() == View.GONE && !url.isEmpty()) {
        b.setVisibility(View.VISIBLE);
      } else if (b.getVisibility() == View.VISIBLE && url.isEmpty()) {
        b.setVisibility(View.GONE);
      }
    }
  }

  public Button getButtonClicked() {
    if (mButtonClicked == null) {
      mButtonClicked =
          findButtonByTag(prefs.getString(ClockActivity.LAST_PLAYED, "setting.key.stream1"));
    }
    return mButtonClicked;
  }

  public void setButtonClicked(Button mButtonClicked) {
    this.mButtonClicked = mButtonClicked;
    prefs.edit().putString(ClockActivity.LAST_PLAYED, mButtonClicked.getTag().toString()).apply();
  }

  public void setButtonClicked(int id) {
    this.mButtonClicked = findButtonById(id);
    prefs.edit().putString(ClockActivity.LAST_PLAYED, mButtonClicked.getTag().toString()).apply();
  }

  void setText(int index, SharedPreferences newPrefs) {
    buttons
        .get(index)
        .setText(
            newPrefs.getString(
                resources.getString(settingKeys.get(index)),
                resources.getString(defaultNames.get(index))));
    this.prefs = newPrefs;
  }

  public void assignUrlToMemory(String url, int streamNo, String label) {
    String key = "setting.key.stream" + streamNo;
    String labelKey = "setting.key.label" + streamNo;
    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    prefs.edit().putString(key, url).apply();
    prefs.edit().putString(labelKey, label).apply();
    Toast.makeText(
            context,
            String.format(context.getString(R.string.assigned_to_memory), url, streamNo),
            Toast.LENGTH_SHORT)
        .show();
  }

  void setButtonLabel(int streamNo, String label) {
    String labelKey = "setting.key.label" + streamNo;
    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    prefs.edit().putString(labelKey, label).apply();
  }

  void setButtonUrl(int streamNo) {
    String labelKey = "setting.key.stream" + streamNo;
    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    prefs.edit().putString(labelKey, "").apply();
  }

  /** Set disabled to all buttons (cycle through buttons and .setEnabled false) */
  public void disableButtons() {
    for (Button button : buttons) {
      button.setEnabled(false);
    }
  }

  /** Set enabled to all buttons (cycle through buttons and .setEnabled) */
  public void enableButtons() {
    for (Button button : buttons) {
      button.setEnabled(true);
    }
  }

  public void resetButtons() {
    for (Button button : buttons) {
      button.setEnabled(true);
      button.setTextColor(resources.getColor(R.color.button_color_off));
      GradientDrawable buttonShape = (GradientDrawable) button.getBackground();
      buttonShape.setStroke(1, resources.getColor(R.color.button_color));
    }
  }

  Button findButtonByTag(String tag) {
    for (Button button : buttons) {
      if (button.getTag().equals(tag)) {
        return button;
      }
    }
    return null;
  }

  Button findButtonById(int id) {
    for (Button button : buttons) {
      if (button.getId() == id) {
        return button;
      }
    }
    return null;
  }

  public void lightButton() {
    resetButtons();
    mButtonClicked.setTextColor(resources.getColor(R.color.color_clock));
    GradientDrawable buttonShape = (GradientDrawable) mButtonClicked.getBackground();
    buttonShape.setStroke(1, resources.getColor(R.color.color_clock));
  }

  public void lightButton(int buttonId) {
    ImageButton button = view.findViewById(buttonId);
    GradientDrawable buttonShape = (GradientDrawable) button.getBackground();
    buttonShape.setStroke(1, resources.getColor(R.color.color_clock));
    button.setColorFilter(context.getResources().getColor(R.color.color_clock));
  }

  public void unlightButton(int buttonId) {
    ImageButton button = view.findViewById(buttonId);
    GradientDrawable buttonShape = (GradientDrawable) button.getBackground();
    buttonShape.setStroke(1, context.getResources().getColor(R.color.button_color));
    button.clearColorFilter();
  }

  public void unlightButton() {
    Button clicked = getButtonClicked();
    if (clicked != null) {
      clicked.setTextColor(context.getResources().getColor(R.color.button_color_off));
      GradientDrawable buttonShape = (GradientDrawable) clicked.getBackground();
      buttonShape.setStroke(1, context.getResources().getColor(R.color.button_color));
    }
  }

  public void toggleTimerButtonsVisibility(int visibility) {
    timerPauseButton.setVisibility(visibility);
    timerPlus10sButton.setVisibility(visibility);
    timerMinus10sButton.setVisibility(visibility);
    timerPlus1mButton.setVisibility(visibility);
    timerMinus1mButton.setVisibility(visibility);
  }

  public void toggleTimerPause(boolean isPaused) {
    if (isPaused) {
      timerPauseButton.setImageResource(R.drawable.baseline_play_circle_outline_24);
    } else {
      timerPauseButton.setImageResource(R.drawable.baseline_pause_circle_outline_24);
    }
  }

  public void onStopPlaying() {
    enableButtons();
    unlightButton();
    onOffButton.setColorFilter(context.getResources().getColor(R.color.color_clock_red));
  }

  public void onPrepared() {
    enableButtons();
    onOffButton.setColorFilter(context.getResources().getColor(R.color.color_clock));
  }

  public void onError() {
    resetButtons();
    onOffButton.setColorFilter(context.getResources().getColor(R.color.color_clock_red));
  }

  public void initializeUrls() {
    mUrls.add(
        prefs.getString(
            context.getResources().getString(R.string.setting_key_stream1),
            context.getResources().getString(R.string.setting_default_stream1)));
    mUrls.add(
        prefs.getString(
            context.getResources().getString(R.string.setting_key_stream2),
            context.getResources().getString(R.string.setting_default_stream2)));
    mUrls.add(
        prefs.getString(
            context.getResources().getString(R.string.setting_key_stream3),
            context.getResources().getString(R.string.setting_default_stream3)));
    mUrls.add(
        prefs.getString(
            context.getResources().getString(R.string.setting_key_stream4),
            context.getResources().getString(R.string.setting_default_stream4)));
    mUrls.add(
        prefs.getString(
            context.getResources().getString(R.string.setting_key_stream5),
            context.getResources().getString(R.string.setting_default_stream5)));
    mUrls.add(prefs.getString(context.getResources().getString(R.string.setting_key_stream6), ""));
    mUrls.add(prefs.getString(context.getResources().getString(R.string.setting_key_stream7), ""));
    mUrls.add(prefs.getString(context.getResources().getString(R.string.setting_key_stream8), ""));
  }

  public List<String> getmUrls() {
    return mUrls;
  }

  String getUrl(int buttonId) {
    String url;
    // index in th list
    int index = -1;
    switch (buttonId) {
      case R.id.stream1:
        index = 0;
        break;
      case R.id.stream2:
        index = 1;
        break;
      case R.id.stream3:
        index = 2;
        break;
      case R.id.stream4:
        index = 3;
        break;
      case R.id.stream5:
        index = 4;
        break;
      case R.id.stream6:
        index = 5;
        break;
      case R.id.stream7:
        index = 6;
        break;
      case R.id.stream8:
        index = 7;
        break;
      default:
        break;
    }
    url = mUrls.get(index);
    return url;
  }

  private class AddLabelOnLongClickListener implements View.OnLongClickListener {
    private final int streamNo;

    AddLabelOnLongClickListener(int stream) {
      streamNo = stream;
    }

    @Override
    public boolean onLongClick(View view) {
      final Button button = (Button) view;
      AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
      LinearLayout layout =
          (LinearLayout)
              LayoutInflater.from(view.getContext())
                  .inflate(R.layout.dialog_edit_radio_label, null);
      final TextInputEditText labelInput =
          layout.findViewById(R.id.streamFinder_textinput_labelDialog_label);
      labelInput.setText(button.getText());
      labelInput.setSelectAllOnFocus(true);
      builder.setView(layout);
      builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
      builder.setPositiveButton(
          R.string.yes,
          (dialog, which) -> setButtonLabel(streamNo, labelInput.getText().toString()));
      builder.setNeutralButton(R.string.remove, (dialog, which) -> setButtonUrl(streamNo));
      builder.show();
      return true;
    }
  }
}
