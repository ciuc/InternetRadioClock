package ro.antiprotv.radioclock.service;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.Toaster;
import ro.antiprotv.radioclock.activity.ClockActivity;

public class RadioAlarmManager extends BroadcastReceiver {

  public static final Integer ALARM_ID_1 = 1;
  public static final Integer ALARM_ID_2 = 2;
  private static final String DEFAULT_SNOOZE = "10"; // minutes
  private static final int DEFAULT_ALARM_PLAY_TIME = 300; // Seconds (=5minutes)
  private static final HashMap<Integer, String> calendarDays2Names = new HashMap<>();

  static {
    calendarDays2Names.put(2, "Mo");
    calendarDays2Names.put(3, "Tu");
    calendarDays2Names.put(4, "We");
    calendarDays2Names.put(5, "Th");
    calendarDays2Names.put(6, "Fr");
    calendarDays2Names.put(7, "Sa");
    calendarDays2Names.put(1, "Su");
  }

  private final ImageButton alarmButton1;
  private final ImageButton cancelButton1;
  private final ImageButton snoozeButton1;
  private final ImageButton closeButton1;
  private final ImageButton snoozeCancelButton1;
  private final TextView alarmText1;
  private final ImageButton alarmOffButton1;
  private final ImageButton alarmButton2;
  private final TextView alarmText2;
  private final ImageButton alarmOffButton2;
  private final PendingIntent alarmIntent;
  private final ClockActivity clockActivity;
  private final ButtonManager buttonManager;
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  private AlarmManager alarmMgr;
  private MediaPlayer player;
  private int NEXT_ALARM;
  private SharedPreferences prefs;
  // private List<String> alarmsDays = new ArrayList<>();
  private Toaster toaster = new Toaster();

  public RadioAlarmManager(ClockActivity context, ButtonManager buttonManager) {
    this.buttonManager = buttonManager;
    alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    Intent intent = new Intent("alarmReceiver");
    alarmIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    clockActivity = context;
    alarmButton1 = clockActivity.findViewById(R.id.alarm_icon);
    alarmText1 = clockActivity.findViewById(R.id.alarm_time);
    alarmOffButton1 = clockActivity.findViewById(R.id.alarm_icon_turn_off);
    cancelButton1 = clockActivity.findViewById(R.id.alarm_icon_cancel);
    snoozeButton1 = clockActivity.findViewById(R.id.alarm_icon_snooze);
    snoozeCancelButton1 = clockActivity.findViewById(R.id.alarm_icon_snooze_cancel);
    closeButton1 = clockActivity.findViewById(R.id.alarm_icon_close);
    alarmOffButton1.setOnClickListener(view -> cancelAlarm(1));
    cancelButton1.setOnClickListener(
        view -> {
          shutDownDefaultAlarm();
          shutDownRadioAlarm(true);
          cancelNonRecurringAlarm();
          setAlarm();
        });
    snoozeButton1.setOnClickListener(view -> snooze());
    snoozeCancelButton1.setOnClickListener(view -> cancelSnooze());
    closeButton1.setOnClickListener(
        view -> {
          // shutDownDefaultAlarm();
          // shutDownRadioAlarm(false);
          // This has the same function as the click which hides the clcockActivity
          clockActivity.hide();
          // setAlarm();
        });

    alarmButton2 = clockActivity.findViewById(R.id.alarm_icon2);
    alarmText2 = clockActivity.findViewById(R.id.alarm_time2);
    alarmOffButton2 = clockActivity.findViewById(R.id.alarm_icon_turn_off2);
    alarmOffButton2.setOnClickListener(view -> cancelAlarm(2));
    prefs = PreferenceManager.getDefaultSharedPreferences(clockActivity);
  }

  public void cancelNonRecurringAlarm() {
    if (getAlarmDays(NEXT_ALARM).isEmpty()) {
      cancelAlarm(NEXT_ALARM);
    }
  }

  @SuppressLint("DefaultLocale")
  public void setAlarm() {
    Alarm nextAlarm1 = getNextAlarm(1);
    Alarm nextAlarm2 = getNextAlarm(2);
    if (nextAlarm1 == null && nextAlarm2 == null) {
      // Toast.makeText(clockActivity, "No alarm!", Toast.LENGTH_SHORT).show();
      return;
    }
    NEXT_ALARM = setupAlarmIntent(nextAlarm1, nextAlarm2);

    setupAlarmText(nextAlarm1, nextAlarm2);

    setupAlarmViews(nextAlarm1, nextAlarm2);
    hideAlarmButtons();
  }

  private void setupAlarmViews(Alarm nextAlarm1, Alarm nextAlarm2) {
    if (nextAlarm1 != null) {
      alarmText1.setVisibility(View.VISIBLE);
      alarmButton1.setImageResource(R.drawable.ic_alarm_on_black_24dp);
      alarmOffButton1.setVisibility(View.VISIBLE);
    }
    if (nextAlarm2 != null) {
      alarmText2.setVisibility(View.VISIBLE);
      alarmButton2.setImageResource(R.drawable.ic_alarm_on_black_24dp);
      alarmOffButton2.setVisibility(View.VISIBLE);
    }
  }

  @SuppressLint("DefaultLocale")
  private void setupAlarmText(Alarm nextAlarm1, Alarm nextAlarm2) {
    final String alarmText = "%s %02d:%02d";
    List<String> alarmsDays1 = getAlarmDays(1);
    StringBuilder text1 = new StringBuilder();
    if (nextAlarm1 != null) {
      if (!alarmsDays1.isEmpty()) {
        for (String s : alarmsDays1) {
          text1.append(s).append(",");
        }
      }
      alarmText1.setText(String.format(alarmText, text1, nextAlarm1.hh, nextAlarm1.mm));
    }
    List<String> alarmsDays2 = getAlarmDays(2);
    if (nextAlarm2 != null) {
      StringBuilder text2 = new StringBuilder();
      if (!alarmsDays2.isEmpty()) {
        for (String s : alarmsDays2) {
          text2.append(s).append(",");
        }
      }
      alarmText2.setText(String.format(alarmText, text2, nextAlarm2.hh, nextAlarm2.mm));
    }
  }

  @SuppressLint("DefaultLocale")
  private int setupAlarmIntent(Alarm nextAlarm1, Alarm nextAlarm2) {
    Alarm nextAlarm;
    if (nextAlarm1 != null) {
      nextAlarm = nextAlarm1;
      if (nextAlarm2 != null && nextAlarm2.before(nextAlarm1)) {
        nextAlarm = nextAlarm2;
      }
    } else { // both can't be null
      nextAlarm = nextAlarm2;
    }
    alarmMgr.cancel(alarmIntent);
    try {
      alarmMgr.setExactAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP, nextAlarm.getTimeInMillis(), alarmIntent);
    } catch (SecurityException securityException) {
      toaster.toast(
          clockActivity,
          "Cannot set the alarm: make sure you give Exact Alarm permission to the app.",
          Toast.LENGTH_LONG);
    }
    // TESTING: enable this line to have the alarm in 5 secs;
    // alarmMgr.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, alarmIntent);
    toaster.toast(
        clockActivity,
        String.format(
            "Alarm set for %s at %02d:%02d",
            calendarDays2Names.get(nextAlarm.day), nextAlarm.hh, nextAlarm.mm),
        Toast.LENGTH_SHORT);
    return nextAlarm.id;
  }

  private List<String> getAlarmDays(int alarmId) {
    List<String> alarmsDays = new ArrayList<>();
    for (int day = 1; day <= 7; day++) {
      if (prefs.getBoolean(String.format("setting.alarm.%s.key.%s", alarmId, day), false)) {
        alarmsDays.add(calendarDays2Names.get(day));
      }
    }
    return alarmsDays;
  }

  @SuppressLint("DefaultLocale")
  private Alarm getNextAlarm(int alarmId) {
    SortedSet<Alarm> alarms = new TreeSet<>();
    Calendar now = Calendar.getInstance();
    int day = now.get(Calendar.DAY_OF_WEEK);
    for (int counter = 1; counter <= 7; counter++) {
      if (day > 7) {
        day = 1;
      }
      if (prefs.getBoolean(String.format("setting.alarm.%s.key.%s", alarmId, day), false)) {
        Alarm alarm =
            new Alarm(
                day,
                prefs.getInt(String.format("setting.alarm.%d.hh", alarmId), -1),
                prefs.getInt(String.format("setting.alarm.%d.mm", alarmId), -1),
                alarmId);
        if (alarm.calendar.after(Calendar.getInstance())) {
          return alarm;
        } else {
          alarm.calendar.add(Calendar.DAY_OF_MONTH, 7);
        }
        alarms.add(alarm);
      }
      day++;
    }

    if (alarms.size() != 0) {
      return getAlarm(alarms);
    } else {
      int hh1 = prefs.getInt("setting.alarm." + alarmId + ".hh", -1);
      int mm1 = prefs.getInt("setting.alarm." + alarmId + ".mm", -1);
      if (hh1 != -1 && mm1 != -1) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hh1);
        calendar.set(Calendar.MINUTE, mm1);
        alarms.add(new Alarm(calendar, alarmId));
        Calendar calendar2 = Calendar.getInstance();
        calendar2.set(Calendar.HOUR_OF_DAY, hh1);
        calendar2.set(Calendar.MINUTE, mm1);
        calendar2.add(Calendar.DAY_OF_WEEK, 1);
        alarms.add(new Alarm(calendar2, alarmId));
      }
      if (alarms.size() == 0) {
        return null;
      }
      Alarm alarm = getAlarm(alarms); // ), nowAlarm);
      if (alarm != null) {
        return alarm;
      }
      return alarms.first();
    }
  }

  private Alarm getAlarm(SortedSet<Alarm> alarms) {
    for (Alarm alarm : alarms) {
      if (alarm.calendar.after(Calendar.getInstance())) {
        return alarm;
      }
    }
    return null;
  }

  private void hideAlarmButtons() {
    // not sure how necessary is this here, but we seem to have a race condition
    // in which the runnable started by the Executor (MediaPlayerCanceler)
    // finishes before both actions are completed
    // which makes no sense at the moment
    // LE: I guess this is b/c it fails silently
    // when it tries to manipulate UI
    clockActivity.runOnUiThread(
        () -> {
          cancelButton1.setVisibility(View.GONE);
          snoozeButton1.setVisibility(View.GONE);
          snoozeCancelButton1.setVisibility(View.GONE);
          closeButton1.setVisibility(View.GONE);
        });
  }

  private void showSnoozeAndCancel() {
    snoozeCancelButton1.setVisibility(View.GONE);
    snoozeButton1.setVisibility(View.VISIBLE);
    cancelButton1.setVisibility(View.VISIBLE);
    closeButton1.setVisibility(View.VISIBLE);
  }

  public void cancelAlarm(int alarmId) {
    alarmMgr.cancel(alarmIntent);

    prefs.edit().putBoolean("setting.alarm." + alarmId + ".key.2", false).apply();
    prefs.edit().putBoolean("setting.alarm." + alarmId + ".key.3", false).apply();
    prefs.edit().putBoolean("setting.alarm." + alarmId + ".key.4", false).apply();
    prefs.edit().putBoolean("setting.alarm." + alarmId + ".key.5", false).apply();
    prefs.edit().putBoolean("setting.alarm." + alarmId + ".key.6", false).apply();
    prefs.edit().putBoolean("setting.alarm." + alarmId + ".key.7", false).apply();
    prefs.edit().putBoolean("setting.alarm." + alarmId + ".key.1", false).apply();
    prefs.edit().putInt("setting.alarm." + alarmId + ".hh", -1).apply();
    prefs.edit().putInt("setting.alarm." + alarmId + ".mm", -1).apply();

    Toast.makeText(clockActivity, R.string.text_alarm_canceled, Toast.LENGTH_SHORT).show();
    clockActivity.setAlarmPlaying(false);
    changeAlarmIconAndTextOnCancel();
    setAlarm();
  }

  private void snooze() {
    // prepare snooze:
    int snooze =
        Integer.parseInt(
            prefs.getString(
                clockActivity.getResources().getString(R.string.setting_key_snoozeMinutes),
                DEFAULT_SNOOZE));
    shutDownDefaultAlarm();
    Toast toast =
        Toast.makeText(
            clockActivity,
            clockActivity.getString(R.string.snooze_toast_text, snooze),
            Toast.LENGTH_SHORT);
    toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, 0);
    toast.show();

    shutDownRadioAlarm(true);
    Calendar now = Calendar.getInstance();
    now.setTimeInMillis(System.currentTimeMillis());
    // now.add(Calendar.SECOND, 10);//FOR TESTING
    now.add(Calendar.MINUTE, snooze); // FOR PRODUCTION
    if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      alarmMgr.setExact(AlarmManager.RTC_WAKEUP, now.getTimeInMillis(), alarmIntent);
    } else {
      alarmMgr.setExactAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP, now.getTimeInMillis(), alarmIntent);
    }
    snoozeCancelButton1.setVisibility(View.VISIBLE);
    clockActivity.setAlarmPlaying(false);
    clockActivity.setAlarmSnoozing(true);
  }

  public void shutDownRadioAlarm(boolean stopPlaying) {
    if (stopPlaying) {
      clockActivity.stopPlaying();
    }
    hideAlarmButtons();
  }

  /** restores the alarm view to its original state */
  public void changeAlarmIconAndTextOnCancel() {
    alarmButton1.setImageResource(R.drawable.ic_alarm_add_black_24dp);
    alarmText1.setVisibility(View.GONE);
    alarmOffButton1.setVisibility(View.GONE);
    alarmButton2.setImageResource(R.drawable.ic_alarm_add_black_24dp);
    alarmText2.setVisibility(View.GONE);
    alarmOffButton2.setVisibility(View.GONE);
  }

  public void cancelSnooze() {
    hideAlarmButtons();
    cancelNonRecurringAlarm();
    setAlarm();
    clockActivity.setAlarmSnoozing(false);
  }

  public void playDefaultAlarmOnStreamError() {
    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
    player = MediaPlayer.create(clockActivity, notification);
    player.start();
    executor.schedule(
        new MediaPlayerCanceller(), DEFAULT_ALARM_PLAY_TIME, TimeUnit.MINUTES); // PROD
    // executor.schedule(new MediaPlayerCanceller(), 5, TimeUnit.SECONDS);//TEST
    clockActivity.setAlarmPlaying(false);
    showSnoozeAndCancel();
  }

  public void shutDownDefaultAlarm() {
    if (player != null && player.isPlaying()) {
      player.stop();
      hideAlarmButtons();
    }
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    // what do we play?
    String playMemoryAtWakeup =
        prefs.getString(
            context.getResources().getString(R.string.setting_key_wake_up_station), "0");
    int memory;
    if (!playMemoryAtWakeup.equals("0")) {
      memory = buttonManager.findButtonByTag(playMemoryAtWakeup).getId();
      buttonManager.setButtonClicked(buttonManager.findButtonByTag(playMemoryAtWakeup));
    } else if (buttonManager.getButtonClicked() == null) {
      memory = R.id.stream1;
      buttonManager.setButtonClicked((Button) clockActivity.findViewById(R.id.stream1));
    } else {
      memory = buttonManager.getButtonClicked().getId();
    }
    Toast.makeText(
            context, context.getString(R.string.text_alarm_playing, memory), Toast.LENGTH_SHORT)
        .show();
    changeAlarmIconAndTextOnCancel();
    showSnoozeAndCancel();
    clockActivity.setAlarmPlaying(true);
    clockActivity.play(memory);
    clockActivity.show();
  }

  public void setPrefs(SharedPreferences prefs) {
    this.prefs = prefs;
  }

  public void setAlarmMgr(AlarmManager alarmMgr) {
    this.alarmMgr = alarmMgr;
  }

  public void setToaster(Toaster toaster) {
    this.toaster = toaster;
  }

  protected static class Alarm implements Comparable<Alarm> {
    private final int day;
    private final int hh;
    private final int mm;
    private final int id;
    private final Calendar calendar;

    public Alarm(int day, int hh, int mm, int id) {
      this.day = day;
      this.hh = hh;
      this.mm = mm;
      Calendar calendar = Calendar.getInstance();
      calendar.set(Calendar.DAY_OF_WEEK, day);
      calendar.set(Calendar.HOUR_OF_DAY, hh);
      calendar.set(Calendar.MINUTE, mm);
      calendar.set(Calendar.SECOND, 0);
      this.calendar = calendar;
      this.id = id;
    }

    public Alarm(Calendar calendar, int id) {
      this.day = calendar.get(Calendar.DAY_OF_WEEK);
      this.hh = calendar.get(Calendar.HOUR_OF_DAY);
      this.mm = calendar.get(Calendar.MINUTE);
      calendar.set(Calendar.SECOND, 0);
      this.calendar = calendar;
      this.id = id;
    }

    @Override
    public int compareTo(Alarm alarm) {
      return this.calendar.compareTo(alarm.calendar);
    }

    long getTimeInMillis() {
      return calendar.getTimeInMillis();
    }

    boolean before(Alarm alarm) {
      return this.calendar.compareTo(alarm.calendar) <= 0;
    }

    @Override
    public String toString() {
      return "Alarm{"
          + "day="
          + day
          + ", hh="
          + hh
          + ", mm="
          + mm
          + ", id="
          + id
          + " | "
          + calendar.getTime()
          + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Alarm alarm = (Alarm) o;
      return day == alarm.day && hh == alarm.hh && mm == alarm.mm;
    }

    @Override
    public int hashCode() {
      return Objects.hash(day, hh, mm);
    }
  }

  private class MediaPlayerCanceller implements Runnable {
    public void run() {
      shutDownDefaultAlarm();
    }
  }
}
