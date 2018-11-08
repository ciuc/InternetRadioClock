package ro.antiprotv.radioclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import ro.antiprotv.radioclock.ClockActivity;
import ro.antiprotv.radioclock.R;
import timber.log.Timber;

public class RadioAlarmManager extends BroadcastReceiver {

    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;
    private ClockActivity clockActivity;
    private final ImageButton alarmButton;
    private final ImageButton cancelButton;
    private final ImageButton snoozeButton;
    private final TextView alarmText;
    private final ImageButton alarmOffButton;
    private MediaPlayer player;
    private ButtonManager buttonManager;

    public  RadioAlarmManager(ClockActivity context, ButtonManager buttonManager) {
        this.buttonManager = buttonManager;
        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent("alarmReceiver");
        alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        clockActivity = context;
        alarmButton = (ImageButton) clockActivity.findViewById(R.id.alarm_icon);
        alarmText = (TextView) clockActivity.findViewById(R.id.alarm_time);
        alarmOffButton = (ImageButton) clockActivity.findViewById(R.id.alarm_icon_turn_off);
        cancelButton = (ImageButton) clockActivity.findViewById(R.id.alarm_icon_cancel);
        snoozeButton = (ImageButton) clockActivity.findViewById(R.id.alarm_icon_snooze);
    }

    public void setAlarm(int hour, int minute) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(System.currentTimeMillis());

        int today = now.get(Calendar.DAY_OF_MONTH);

        Calendar next = Calendar.getInstance();
        next.set(Calendar.DAY_OF_MONTH, today);
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);


        Timber.d("NOW : %d", now.getTimeInMillis());
        Timber.d("THEN: %d", next.getTimeInMillis());
        boolean tomorrow = false;
        if (next.getTimeInMillis() - now.getTimeInMillis() <=0) {
            tomorrow = true;
            next.add(Calendar.DAY_OF_MONTH, 1);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

        Timber.d("Alarm set for: %s", sdf.format(next.getTime()));
        alarmMgr.setExact( AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), alarmIntent);
        clockActivity.setAlarmScheduled(true);
        //TESTING: enable this line to have the alarm in 5 secs;
        //alarmMgr.setExact( AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+5000, alarmIntent);
        Toast.makeText(clockActivity, String.format("Alarm set for %s at %s", (tomorrow)?"tomorrow":"today", sdf.format(next.getTime())), Toast.LENGTH_SHORT).show();
        alarmText.setText(String.format("Alarm set for %s", sdf.format(next.getTime())));
        alarmText.setVisibility(View.VISIBLE);
        alarmButton.setImageResource(R.drawable.ic_alarm_on_black_24dp);
        alarmOffButton.setVisibility(View.VISIBLE);
        alarmOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Timber.d("Alarm canceled");
                alarmMgr.cancel(alarmIntent);
                Toast.makeText(clockActivity, "Alarm canceled!", Toast.LENGTH_SHORT).show();
                clockActivity.setAlarmPlaying(false);
                changeAlarmIconAndTextOnCancel();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shutDownDefaultAlarm();
            }
        });
    }

    protected void changeAlarmIconAndTextOnCancel() {
        alarmButton.setImageResource(R.drawable.ic_alarm_add_black_24dp);
        alarmText.setVisibility(View.GONE);
        alarmOffButton.setVisibility(View.GONE);

    }
    protected void playDefaultAlarmOnStreamError() {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        player = MediaPlayer.create(clockActivity, notification);
        player.start();
        Thread t = new MediaPlayerCanceller();
        t.start();
        clockActivity.setAlarmPlaying(false);
        snoozeButton.setVisibility(View.VISIBLE);
        cancelButton.setVisibility(View.VISIBLE);
    }

    protected void shutDownDefaultAlarm() {
        if (player.isPlaying()) {
            player.stop();
        }
        cancelButton.setVisibility(View.GONE);
        snoozeButton.setVisibility(View.GONE);
    }

    private class MediaPlayerCanceller extends Thread {
        public void run() {
                    int count = 0;
                    while (count < 300) {
                        {
                            try {
                                Thread.sleep(1000);
                                count ++;
                            } catch (Exception e) {
                                Timber.e("Error: ", e.toString());
                            }
                        }
                    }
                    shutDownDefaultAlarm();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //what do we play?
        int memory = 0;
        if (buttonManager.getButtonClicked() == null) {
            memory = R.id.stream1;
            buttonManager.setButtonClicked((Button) clockActivity.findViewById(R.id.stream1));
        } else {
            memory = buttonManager.getButtonClicked().getId();
        }
        Toast.makeText(context, "Alarm! playing: ", Toast.LENGTH_SHORT).show();
        changeAlarmIconAndTextOnCancel();
        clockActivity.setAlarmPlaying(true);
        clockActivity.play(memory);
        clockActivity.show();
    }
}
