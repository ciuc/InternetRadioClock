package ro.antiprotv.radioclock.service;

import android.content.Context;

import ro.antiprotv.radioclock.R;

public class TimerService {

  private RingtoneService ringtoneService;
  ButtonManager buttonManager;
  private boolean timer;
  private int timerSeconds = 180;
  private boolean timerStarted;
  private int time = timerSeconds;
  private int currentTimer;

  public TimerService(RingtoneService ringtoneService, ButtonManager buttonManager) {
    this.ringtoneService = ringtoneService;
    this.buttonManager = buttonManager;
  }

  public void setTimer(int buttonId) {
    currentTimer = buttonId;
    if (timerStarted) {
      time = 0;
      timerStarted = false;
      timer = false;
      buttonManager.unlightButton(buttonId);
    } else {
      buttonManager.lightButton(buttonId);
      timer = true;
      timerStarted = true;
      time = timerSeconds;
    }
  }

  public void setTimerSeconds(int timerSeconds) {
    this.timerSeconds = timerSeconds;
  }

  public String getTimerText() {
    if (timer) {
      if (time == 0) {
        ringtoneService.playAlarm(7);
      }
      if (time <= 0) {
        timerStarted = false;
        timer = false;
        buttonManager.unlightButton(currentTimer);
        time = timerSeconds;
      } else {
        if (!timerStarted) {
          timerStarted = true;
        }
      }
      String text = String.format("%02d:%02d", time / 60, time % 60);
      time--;
      return text;
    }
    return null;
  }

}
