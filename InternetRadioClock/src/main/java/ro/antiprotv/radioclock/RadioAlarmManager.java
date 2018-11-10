package ro.antiprotv.radioclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class RadioAlarmManager extends BroadcastReceiver {

    private final static int DEFAULT_SNOOZE = 10;//minutes
    private final static int DEFAULT_ALARM_PLAY_TIME = 300;//Seconds (=5minutes)
    private final ImageButton alarmButton;
    private final ImageButton cancelButton;
    private final ImageButton snoozeButton;
    private final TextView alarmText;
    private final ImageButton alarmOffButton;
    private final AlarmManager alarmMgr;
    private final PendingIntent alarmIntent;
    private final ClockActivity clockActivity;
    private MediaPlayer player;
    private final ButtonManager buttonManager;

    public RadioAlarmManager(ClockActivity context, ButtonManager buttonManager) {
        this.buttonManager = buttonManager;
        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent("alarmReceiver");
        alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        clockActivity = context;
        alarmButton = clockActivity.findViewById(R.id.alarm_icon);
        alarmText = clockActivity.findViewById(R.id.alarm_time);
        alarmOffButton = clockActivity.findViewById(R.id.alarm_icon_turn_off);
        cancelButton = clockActivity.findViewById(R.id.alarm_icon_cancel);
        snoozeButton = clockActivity.findViewById(R.id.alarm_icon_snooze);
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


        boolean tomorrow = false;
        if (next.getTimeInMillis() - now.getTimeInMillis() <= 0) {
            tomorrow = true;
            next.add(Calendar.DAY_OF_MONTH, 1);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

        alarmMgr.setExact(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), alarmIntent);
        //TESTING: enable this line to have the alarm in 5 secs;
        //alarmMgr.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, alarmIntent);
        Toast.makeText(clockActivity, String.format("Alarm set for %s at %s", (tomorrow) ? clockActivity.getString(R.string.text_tomorrow) : clockActivity.getString(R.string.today), sdf.format(next.getTime())), Toast.LENGTH_SHORT).show();
        alarmText.setText(clockActivity.getString(R.string.text_alarm_set_for, sdf.format(next.getTime())));
        alarmText.setVisibility(View.VISIBLE);
        alarmButton.setImageResource(R.drawable.ic_alarm_on_black_24dp);
        alarmOffButton.setVisibility(View.VISIBLE);
        alarmOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alarmMgr.cancel(alarmIntent);
                Toast.makeText(clockActivity, R.string.text_alarm_canceled, Toast.LENGTH_SHORT).show();
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
        snoozeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snooze();
            }
        });
        snoozeButton.setVisibility(View.GONE);
    }

    private void snooze() {
        shutDownDefaultAlarm();
        Toast toast = Toast.makeText(clockActivity, clockActivity.getString(R.string.snooze_toast_text, 10), Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, 0);
        toast.show();

        clockActivity.stopPlaying();
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(System.currentTimeMillis());
        //now.add(Calendar.SECOND, DEFAULT_SNOOZE);//FOR TESTING
        now.add(Calendar.MINUTE, DEFAULT_SNOOZE);//FOR PRODUCTION
        setAlarm(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
    }

    void changeAlarmIconAndTextOnCancel() {
        alarmButton.setImageResource(R.drawable.ic_alarm_add_black_24dp);
        alarmText.setVisibility(View.GONE);
        alarmOffButton.setVisibility(View.GONE);

    }

    void cancelSnooze() {
        snoozeButton.setVisibility(View.GONE);
    }

    void playDefaultAlarmOnStreamError() {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        player = MediaPlayer.create(clockActivity, notification);
        player.start();
        Thread t = new MediaPlayerCanceller();
        t.start();
        clockActivity.setAlarmPlaying(false);
        snoozeButton.setVisibility(View.VISIBLE);
        cancelButton.setVisibility(View.VISIBLE);
    }

    void shutDownDefaultAlarm() {
        if (player != null && player.isPlaying()) {
            player.stop();
        }
        cancelButton.setVisibility(View.GONE);
        snoozeButton.setVisibility(View.GONE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //what do we play?
        int memory;
        if (buttonManager.getButtonClicked() == null) {
            memory = R.id.stream1;
            buttonManager.setButtonClicked((Button) clockActivity.findViewById(R.id.stream1));
        } else {
            memory = buttonManager.getButtonClicked().getId();
        }
        Toast.makeText(context, context.getString(R.string.text_alarm_playing, memory), Toast.LENGTH_SHORT).show();
        changeAlarmIconAndTextOnCancel();
        snoozeButton.setVisibility(View.VISIBLE);
        clockActivity.setAlarmPlaying(true);
        clockActivity.play(memory);
        clockActivity.show();
    }

    private class MediaPlayerCanceller extends Thread {
        public void run() {
            int count = 0;
            while (count < DEFAULT_ALARM_PLAY_TIME) {
                {
                    try {
                        Thread.sleep(1000);
                        count++;
                    } catch (Exception e) {
                        //TODO: have no idea what to do here!
                    }
                }
            }
            shutDownDefaultAlarm();
        }
    }
}
