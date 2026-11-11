package ro.antiprotv.radioclock.service;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
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
import ro.antiprotv.radioclock.db.Station;
import ro.antiprotv.radioclock.db.StationPreferences;
import ro.antiprotv.radioclock.db.StationRepository;

public class ButtonManager {
  private final Resources resources;
  private final Context context;
  private final View view;
  private final View.OnTouchListener onTouchListener;
  private final View.OnClickListener playListener;
  /** The number of memory buttons on the clock. */
  public static final int MEMORIES = 8;

  private final StationRepository stations;
  // the stream of each memory button, by button index; "" for an empty memory
  private final List<String> mUrls = new ArrayList<>();
  // the label of each memory button, by button index
  private final List<String> mLabels = new ArrayList<>();
  // the database id of the station on each memory button, by button index; 0 for an empty memory
  private final List<Long> mStationIds = new ArrayList<>();
  private final StationPreferences stationPreferences;
  private List<Button> buttons;
  // the button we have clicked on
  private Button mButtonClicked;
  private ImageButton onOffButton;
  private ImageButton timerPauseButton;
  private Button timerPlus10sButton;
  private Button timerMinus10sButton;
  private Button timerPlus1mButton;
  private Button timerMinus1mButton;

  public ButtonManager(Context ctx) {
    this(ctx, null, null, null);
  }

  public ButtonManager(
      Context ctx,
      View view,
      View.OnTouchListener onTouchListener,
      View.OnClickListener playListener) {
    this.context = ctx;
    this.view = view;
    this.onTouchListener = onTouchListener;
    this.playListener = playListener;
    resources = context.getResources();
    stations = StationRepository.getInstance(context);
    stationPreferences = new StationPreferences(context);
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

    for (int i = 0; i < buttons.size(); i++) {
      Button b = buttons.get(i);
      b.setText(mLabels.get(i));
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

  /** The button picked last; the station played last, or the first button. */
  public Button getButtonClicked() {
    if (mButtonClicked == null) {
      Button lastPlayed = findButtonForStation(stationPreferences.getLastPlayed());
      mButtonClicked = lastPlayed != null ? lastPlayed : buttons.get(0);
    }
    return mButtonClicked;
  }

  public void setButtonClicked(Button mButtonClicked) {
    this.mButtonClicked = mButtonClicked;
    rememberLastPlayed(mButtonClicked);
  }

  public void setButtonClicked(int id) {
    this.mButtonClicked = findButtonById(id);
    rememberLastPlayed(mButtonClicked);
  }

  private void rememberLastPlayed(Button button) {
    int index = buttonIndex(button);
    if (index < 0) {
      return;
    }
    Station station = stations.getStationById(mStationIds.get(index));
    if (station != null) {
      stationPreferences.setLastPlayed(station);
    }
  }

  /**
   * The button the alarm should play: the wake-up station picked in the settings, or null to play
   * whatever played last (also when that station was removed).
   */
  public Button getWakeUpButton() {
    return findButtonForStation(stationPreferences.getWakeUpStation());
  }

  /** The memory button holding this station, or null when it is not on one. */
  Button findButtonForStation(Station station) {
    if (station == null || buttons == null) {
      return null;
    }
    int index = mStationIds.indexOf(station.id);
    return index < 0 ? null : buttons.get(index);
  }

  /**
   * Shows the stations as they are now in the database on the memory buttons.
   *
   * @return the indexes of the buttons whose stream changed
   */
  public List<Integer> applyStations(List<Station> saved) {
    List<String> oldUrls = new ArrayList<>(mUrls);
    loadStations(saved);
    List<Integer> changed = new ArrayList<>();
    for (int i = 0; i < MEMORIES; i++) {
      if (i >= oldUrls.size() || !oldUrls.get(i).equals(mUrls.get(i))) {
        changed.add(i);
      }
    }
    if (buttons != null) {
      for (int i = 0; i < buttons.size(); i++) {
        buttons.get(i).setText(mLabels.get(i));
      }
      hideUnhideButtons();
    }
    return changed;
  }

  private void loadStations(List<Station> saved) {
    mUrls.clear();
    mLabels.clear();
    mStationIds.clear();
    for (int i = 0; i < MEMORIES; i++) {
      mUrls.add("");
      mLabels.add(String.valueOf(i + 1));
      mStationIds.add(StationPreferences.NONE);
    }
    for (Station station : saved) {
      int index = station.position - 1;
      if (index >= 0 && index < MEMORIES) {
        mUrls.set(index, station.url);
        mLabels.set(index, station.label);
        mStationIds.set(index, station.id);
      }
    }
  }

  public void assignUrlToMemory(String url, int streamNo, String label) {
    stations.assign(streamNo, url, label);
    Toast.makeText(
            context,
            String.format(context.getString(R.string.assigned_to_memory), url, streamNo),
            Toast.LENGTH_SHORT)
        .show();
  }

  void setButtonLabel(int streamNo, String label) {
    stations.setLabel(streamNo, label);
  }

  void clearMemory(int streamNo) {
    stations.removeAtPosition(streamNo);
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

  /** The button's place among the memory buttons (0-7), or -1. */
  public int buttonIndex(Button button) {
    return buttons == null ? -1 : buttons.indexOf(button);
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

  /** Reads the memories from the database; call before {@link #initializeButtons()}. */
  public void initializeUrls() {
    loadStations(stations.getStations());
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
      builder.setNeutralButton(R.string.remove, (dialog, which) -> clearMemory(streamNo));
      builder.show();
      return true;
    }
  }
}
