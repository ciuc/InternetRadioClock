package ro.antiprotv.radioclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RadioAlarmManager extends BroadcastReceiver {

    private final static String DEFAULT_SNOOZE = "10";//minutes
    private final static int DEFAULT_ALARM_PLAY_TIME = 300;//Seconds (=5minutes)
    private final ImageButton alarmButton;
    private final ImageButton cancelButton;
    private final ImageButton snoozeButton;
    private final TextView alarmText;
    private final ImageButton alarmOffButton;
    private final AlarmManager alarmMgr;
    private final PendingIntent alarmIntent;
    private final ClockActivity clockActivity;
    private final ButtonManager buttonManager;
    private MediaPlayer player;
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

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
        alarmOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelAlarm();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shutDownDefaultAlarm();
                shutDownRadioAlarm();
            }
        });
        snoozeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snooze();
            }
        });
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

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            alarmMgr.setExact(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), alarmIntent);
        } else {
            alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), alarmIntent);
        }
        //TESTING: enable this line to have the alarm in 5 secs;
        //alarmMgr.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, alarmIntent);
        Toast.makeText(clockActivity, String.format("Alarm set for %s at %s", (tomorrow) ? clockActivity.getString(R.string.text_tomorrow) : clockActivity.getString(R.string.today), sdf.format(next.getTime())), Toast.LENGTH_SHORT).show();
        alarmText.setText(clockActivity.getString(R.string.text_alarm_set_for, sdf.format(next.getTime())));
        alarmText.setVisibility(View.VISIBLE);
        alarmButton.setImageResource(R.drawable.ic_alarm_on_black_24dp);
        alarmOffButton.setVisibility(View.VISIBLE);
        hideSnoozeAndCancel();
    }

    private void hideSnoozeAndCancel() {
        //not sure how necessary is this here, but we seem to have a race condition
        //in which the runnable started by the Executor (MediaPlayerCanceler)
        //finishes before both actions are completed
        //which makes no sense at the moment
        clockActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cancelButton.setVisibility(View.GONE);
                snoozeButton.setVisibility(View.GONE);
            }
        });

    }

    private void showSnoozeAndCancel() {
        snoozeButton.setVisibility(View.VISIBLE);
        cancelButton.setVisibility(View.VISIBLE);
    }


    private void cancelAlarm() {
        alarmMgr.cancel(alarmIntent);
        Toast.makeText(clockActivity, R.string.text_alarm_canceled, Toast.LENGTH_SHORT).show();
        clockActivity.setAlarmPlaying(false);
        changeAlarmIconAndTextOnCancel();
    }

    private void snooze() {
        //prepare snooze:
        int snooze = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(clockActivity).getString(clockActivity.getResources().getString(R.string.setting_key_snoozeMinutes), DEFAULT_SNOOZE));
        shutDownDefaultAlarm();
        Toast toast = Toast.makeText(clockActivity, clockActivity.getString(R.string.snooze_toast_text, snooze), Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, 0);
        toast.show();

        shutDownRadioAlarm();
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(System.currentTimeMillis());
        //now.add(Calendar.SECOND, DEFAULT_SNOOZE);//FOR TESTING
        now.add(Calendar.MINUTE, snooze);//FOR PRODUCTION
        setAlarm(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
    }

    private void shutDownRadioAlarm() {
        clockActivity.stopPlaying();
        hideSnoozeAndCancel();
    }

    /**
     * restores the alarm view
     * to its original state
     */
    void changeAlarmIconAndTextOnCancel() {
        alarmButton.setImageResource(R.drawable.ic_alarm_add_black_24dp);
        alarmText.setVisibility(View.GONE);
        alarmOffButton.setVisibility(View.GONE);
    }

    void cancelSnooze() {
        hideSnoozeAndCancel();
    }

    void playDefaultAlarmOnStreamError() {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        player = MediaPlayer.create(clockActivity, notification);
        player.start();
        executor.schedule(new MediaPlayerCanceller(), DEFAULT_ALARM_PLAY_TIME, TimeUnit.MINUTES);//PROD
        //executor.schedule(new MediaPlayerCanceller(), 5, TimeUnit.SECONDS);//TEST
        clockActivity.setAlarmPlaying(false);
        showSnoozeAndCancel();
    }

    void shutDownDefaultAlarm() {
        if (player != null && player.isPlaying()) {
            player.stop();
            hideSnoozeAndCancel();
        }
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
        showSnoozeAndCancel();
        clockActivity.setAlarmPlaying(true);
        clockActivity.play(memory);
        clockActivity.show();
    }

    private class MediaPlayerCanceller implements Runnable {
        public void run() {
            shutDownDefaultAlarm();
        }
    }
}
