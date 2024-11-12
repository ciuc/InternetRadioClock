package ro.antiprotv.radioclock.service;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.view.View;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.activity.ClockActivity;
import ro.antiprotv.radioclock.view.AbstractVisualTimer;
import ro.antiprotv.radioclock.view.FillCircleView;
import ro.antiprotv.radioclock.view.FillRectangleView;

public class TimerService {

  private RingtoneService ringtoneService;
  private ButtonManager buttonManager;
  private ClockActivity clockActivity;
  private boolean timer;
  private boolean timerStarted;
  private int countingTime = 10;
  private int currentTimer;
  private int alarmDuration = 5;
  // stuff for the fill stuff
  private final FillRectangleView fillRectangleView;
  private final FillCircleView fillCircleView;
  private int originalCounter = 0;
  private String visual = "rectangle";
  private AbstractVisualTimer abstractVisualTimer;

  public TimerService(
      ClockActivity clockActivity, RingtoneService ringtoneService, ButtonManager buttonManager) {
    this.ringtoneService = ringtoneService;
    this.buttonManager = buttonManager;
    this.clockActivity = clockActivity;
    this.fillRectangleView = clockActivity.findViewById(R.id.fill_rectangle);
    this.fillCircleView = clockActivity.findViewById(R.id.fill_pie);
  }

  public void startInstantTimer(int buttonId, int seconds, String visual) {
    currentTimer = buttonId;
    abstractVisualTimer = getVisualView(visual);
    if (timerStarted) {
      stopTimer();
    } else {
      startTimer(buttonId, seconds);
    }
  }

  public String getTimerText() {
    if (!timer) {
      return null;
    } else {
      if (countingTime == 0) {
        ringtoneService.playAlarm(alarmDuration);
      }
      if (countingTime <= 0) {
        stopTimer();

      } else {
        if (!timerStarted) {
          timerStarted = true;
        }
      }
      String text = String.format("%02d:%02d", countingTime / 60, countingTime % 60);
      countingTime--;
      if (abstractVisualTimer != null) {
        abstractVisualTimer.updateFillWidth(originalCounter, countingTime);
      }
      return text;
    }
  }

  private void startTimer(int buttonId, int seconds) {
    buttonManager.lightButton(buttonId);
    timer = true;
    timerStarted = true;
    countingTime = seconds;
    originalCounter = seconds;
    if (abstractVisualTimer != null) {
      abstractVisualTimer.setVisibility(VISIBLE);
    }
  }

  private void stopTimer() {
    timerStarted = false;
    timer = false;
    buttonManager.unlightButton(currentTimer);
    countingTime = 10;
    if (abstractVisualTimer != null) {
      abstractVisualTimer.setVisibility(INVISIBLE);
      abstractVisualTimer.reset();
    }
  }

  public void stopAlarm() {
    ringtoneService.stopAlarm();
  }

  public void setAlarmDuration(int alarmDuration) {
    this.alarmDuration = alarmDuration;
  }

  private AbstractVisualTimer getVisualView(String savedPref) {
    if(savedPref == null) {
      return null;
    }
    if (savedPref.equalsIgnoreCase("circle")) {
      return fillCircleView;
    }
    return fillRectangleView;
  }

}
