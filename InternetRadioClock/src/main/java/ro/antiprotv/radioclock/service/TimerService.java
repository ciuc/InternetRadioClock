package ro.antiprotv.radioclock.service;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.activity.ClockActivity;
import ro.antiprotv.radioclock.view.AbstractVisualTimer;
import ro.antiprotv.radioclock.view.FillCircleView;
import ro.antiprotv.radioclock.view.FillRectangleView;
import timber.log.Timber;

public class TimerService {

  private final RingtoneService ringtoneService;
  private final ButtonManager buttonManager;
  //private ClockActivity clockActivity;
  private boolean timerEnabled;
  private boolean timerStarted;
  private int countingTime = 10;
  private int currentTimer;
  private int alarmDuration = 5;
  // stuff for the fill stuff
  private final FillRectangleView fillRectangleView;
  private final FillCircleView fillCircleView;
  private int originalCounter = 0;
  private AbstractVisualTimer abstractVisualTimer;
  private boolean animate;

  public TimerService(
      ClockActivity clockActivity, RingtoneService ringtoneService, ButtonManager buttonManager) {
    this.ringtoneService = ringtoneService;
    this.buttonManager = buttonManager;
    this.fillRectangleView = clockActivity.findViewById(R.id.fill_rectangle);
    this.fillCircleView = clockActivity.findViewById(R.id.fill_pie);
  }

  public void startInstantTimer(int buttonId, int seconds, String visual, boolean animate) {
    currentTimer = buttonId;
    this.animate = animate;
    abstractVisualTimer = getVisualView(visual);
    if (timerStarted) {
      stopTimer();
    } else {
      startTimer(buttonId, seconds);
    }
  }

  public String getTimerText() {
    if (!timerEnabled) {
      return null;
    }
    if (!timerStarted) {
      timerStarted = true;
    }
    if (countingTime < -5) {
      stopTimer();
      return null;
    }
    if (countingTime < 0) {
      countingTime--;
      return "00:00";
    }

    if (countingTime == 0) {
      ringtoneService.playAlarm(alarmDuration);
      if (animate) {
        abstractVisualTimer.startAnimate();
      }
    }

    String text = String.format("%02d:%02d", countingTime / 60, countingTime % 60);
    if (abstractVisualTimer != null) {
      abstractVisualTimer.updateFillWidth(originalCounter, countingTime);
    }
    countingTime--;
    return text;
  }

  private void startTimer(int buttonId, int seconds) {
    Timber.d("Starting timer");
    buttonManager.lightButton(buttonId);
    timerEnabled = true;
    timerStarted = true;
    countingTime = seconds;
    originalCounter = seconds;
    if (abstractVisualTimer != null) {
      Timber.d(
          "Setting visible; currently:  "
              + (abstractVisualTimer.getVisibility() == VISIBLE ? "VISIBLE" : "INVISIBLE"));
      abstractVisualTimer.setVisibility(VISIBLE);
    }
  }

  private void stopTimer() {
    Timber.d("Stopping timer");
    timerStarted = false;
    timerEnabled = false;
    buttonManager.unlightButton(currentTimer);
    if (abstractVisualTimer != null) {
      Timber.d(
          "Setting invisible ; currently: "
              + (abstractVisualTimer.getVisibility() == VISIBLE ? "VISIBLE" : "INVISIBLE"));
      abstractVisualTimer.stopAnimation();
      abstractVisualTimer.setVisibility(INVISIBLE);
      abstractVisualTimer.reset();
    }
    countingTime = 10;
  }

  public void stopAlarm() {
    ringtoneService.stopAlarm();
    /*
    if (!timerEnabled) {
      return;
    }

    if(abstractVisualTimer != null){
     abstractVisualTimer.stopAnimation();
    }

     */
  }

  public void setAlarmDuration(int alarmDuration) {
    this.alarmDuration = alarmDuration;
  }

  private AbstractVisualTimer getVisualView(String savedPref) {
    if (savedPref == null) {
      return null;
    }
    if (savedPref.equalsIgnoreCase("circle")) {
      return fillCircleView;
    }
    return fillRectangleView;
  }
}
