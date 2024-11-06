package ro.antiprotv.radioclock.service;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RingtoneService {

  private final Ringtone alarmRingtone;
  private final ScheduledExecutorService executorService;

  public RingtoneService(Context context) {
    alarmRingtone =
        RingtoneManager.getRingtone(
            context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
    executorService = Executors.newSingleThreadScheduledExecutor();
  }

  public Ringtone getAlarmRingtone() {
    return alarmRingtone;
  }

  public void playAlarm(int seconds) {
    if (alarmRingtone != null && !alarmRingtone.isPlaying()) {
      alarmRingtone.play();
      executorService.schedule(
              this::stopAlarm,
          seconds,
          TimeUnit.SECONDS);
    }
  }

  public void stopAlarm() {
    if (alarmRingtone != null && alarmRingtone.isPlaying()) {
      alarmRingtone.stop();
    }
  }
}
