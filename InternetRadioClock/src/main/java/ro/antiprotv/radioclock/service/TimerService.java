package ro.antiprotv.radioclock.service;

public class TimerService {

  private RingtoneService ringtoneService;
  ButtonManager buttonManager;
  private boolean timer;
  private boolean timerStarted;
  private int countingTime = 10;
  private int currentTimer;
  private int alarmDuration = 5;

  public TimerService(RingtoneService ringtoneService, ButtonManager buttonManager) {
    this.ringtoneService = ringtoneService;
    this.buttonManager = buttonManager;
  }

  public void startInstantTimer(int buttonId, int seconds) {
    currentTimer = buttonId;
    if (timerStarted) {
      countingTime = 0;
      timerStarted = false;
      timer = false;
      buttonManager.unlightButton(buttonId);
    } else {
      buttonManager.lightButton(buttonId);
      timer = true;
      timerStarted = true;
      countingTime = seconds;
    }
  }

  public String getTimerText() {
    if (timer) {
      if (countingTime == 0) {
        ringtoneService.playAlarm(alarmDuration);
      }
      if (countingTime <= 0) {
        timerStarted = false;
        timer = false;
        buttonManager.unlightButton(currentTimer);
        // countingTime = timerSeconds;
      } else {
        if (!timerStarted) {
          timerStarted = true;
        }
      }
      String text = String.format("%02d:%02d", countingTime / 60, countingTime % 60);
      countingTime--;
      return text;
    }
    return null;
  }

  public void stopAlarm() {
    ringtoneService.stopAlarm();
  }

  public void setAlarmDuration(int alarmDuration) {
    this.alarmDuration = alarmDuration;
  }
}
