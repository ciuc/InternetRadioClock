package ro.antiprotv.radioclock.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import ro.antiprotv.radioclock.R;

public class RingtoneService {

  private Ringtone ringtone;
  private SharedPreferences preferences;
  private final ScheduledExecutorService executorService;
  private Context context;

  public RingtoneService(Context context, SharedPreferences preferences) {
    executorService = Executors.newSingleThreadScheduledExecutor();
    this.context = context;
    this.preferences = preferences;
  }

  public void playAlarm(int seconds) {
    ringtone =
        RingtoneManager.getRingtone(
            context,
            Uri.parse(
                preferences.getString(
                    context.getString(R.string.setting_key_timer_ringtone),
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString())));
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      ringtone.setLooping(true);
    }
    if (ringtone != null && !ringtone.isPlaying()) {
      ringtone.play();
      executorService.schedule(this::stopAlarm, seconds, TimeUnit.SECONDS);
    }
  }

  public void stopAlarm() {
    if (ringtone != null && ringtone.isPlaying()) {
      ringtone.stop();
    }
  }
}
