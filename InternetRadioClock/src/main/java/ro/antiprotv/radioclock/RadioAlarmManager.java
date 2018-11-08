package ro.antiprotv.radioclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
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

public class RadioAlarmManager {

    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;
    private ClockActivity clockActivity;
    private final ImageButton alarmButton;
    private final TextView alarmText;
    private final ImageButton alarmOffButton;

    public  RadioAlarmManager(ClockActivity context) {
        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent("alarmReceiver");
        alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        clockActivity = context;
        alarmButton = (ImageButton) clockActivity.findViewById(R.id.alarm_icon);
        alarmText = (TextView) clockActivity.findViewById(R.id.alarm_time);
        alarmOffButton = (ImageButton) clockActivity.findViewById(R.id.alarm_icon_turn_off);
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
        Toast.makeText(clockActivity, String.format("Alarm set for %s at %s", (tomorrow)?"tomorrow":"today", sdf.format(next.getTime())), Toast.LENGTH_SHORT).show();
        alarmText.setText(String.format("Alarm set for %d:%d", hour, minute));
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
    }

    protected void changeAlarmIconAndTextOnCancel() {
        alarmButton.setImageResource(R.drawable.ic_alarm_add_black_24dp);
        alarmText.setVisibility(View.GONE);
        alarmOffButton.setVisibility(View.GONE);

    }
    private void setAlarmText(Calendar cal) {

    }
}
