package ro.antiprotv.radioclock.service.profile;

import static ro.antiprotv.radioclock.activity.ClockActivity.PREF_NIGHT_MODE;

import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.Toast;
import java.util.Calendar;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import ro.antiprotv.radioclock.ClockUpdater;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.activity.ClockActivity;
import timber.log.Timber;

public class ProfileManager implements SharedPreferences.OnSharedPreferenceChangeListener {

  public static String SETTING_NIGHT_PROFILE_SCHEDULE_ENABLED = "setting.key.enable.night.schedule";
  public static String SETTING_NIGHT_PROFILE_AUTOSTART = "setting.key.night_profile.autostart";
  public static String SETTING_NIGHT_PROFILE_AUTOEND = "setting.key.night_profile.autoend";
  private final ClockActivity clockActivity;
  private final SharedPreferences prefs;
  private final ClockUpdater clockUpdater;
  private final ScheduledExecutorService scheduledExecutorService;
  private ScheduledFuture currentScheduledNightTask;
  private ScheduledFuture currentScheduledDayTask;
  private final ProfileUtils profileUtils;

  public ProfileManager(
      ClockActivity clockActivity, SharedPreferences prefs, ClockUpdater clockUpdater) {
    this.clockActivity = clockActivity;
    this.prefs = prefs;
    this.clockUpdater = clockUpdater;
    scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
    this.profileUtils = ProfileUtils.getInstance(clockActivity);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (key.equals(SETTING_NIGHT_PROFILE_AUTOSTART)
        || key.equals(SETTING_NIGHT_PROFILE_AUTOEND)
        || key.equals(SETTING_NIGHT_PROFILE_SCHEDULE_ENABLED)
       ) {
      Timber.d("Pref changed: " + key);
      clearTask();
      applyProfile();
    }
  }

  /*
  Since the tasks are scheduled when the tasks run as well (which run the appluProfile method),
  we need to take care of the task cancelling upon other uses of the applyProfile method
     -> namely, from the main activty onCreate and from onSharedPreferencesChanged
  */
  public void clearTask() {
    if (currentScheduledNightTask != null) {
      currentScheduledNightTask.cancel(false);
    }
    if (currentScheduledDayTask != null) {
      currentScheduledDayTask.cancel(false);
    }
  }

  /*Called by button click */
  public void toggleNightMode() {
    boolean nightMode = prefs.getBoolean(PREF_NIGHT_MODE, false);
    // this will trigger the listener!!
    // SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(clockActivity);
    prefs.edit().putBoolean(ClockActivity.PREF_NIGHT_MODE, !nightMode).apply();
  }

  private void applyProfile(boolean nightMode) {
    if (!nightMode) {
      applyDayProfile();
    } else {
      applyNightProfile();
    }
  }

  /*
   * Called at startup and when the change profile task kicks in.
   * If night mode schedule is not enabled, we just apply whichever pfofile is dictated by the button click.
   * The button clck is persistent.
   * Else, if there is a schedule, we check in which period we are and schedule the next change.
   */
  public synchronized void applyProfile() {
    boolean nightMode = prefs.getBoolean(PREF_NIGHT_MODE, false);

    boolean nightProfileScheduleEnabled =
        prefs.getBoolean(SETTING_NIGHT_PROFILE_SCHEDULE_ENABLED, false);
    if (!nightProfileScheduleEnabled) {
      applyProfile(nightMode);
      return;
    }

    String nightModeAutoStart = prefs.getString(SETTING_NIGHT_PROFILE_AUTOSTART, "23:00");
    String nightModeAutoEnd = prefs.getString(SETTING_NIGHT_PROFILE_AUTOEND, "07:00");

    // in what period are we? night or day?
    Calendar now = Calendar.getInstance();

    Calendar nightProfile_start = Calendar.getInstance();
    nightProfile_start.set(Calendar.HOUR_OF_DAY, ProfileUtils.getHour(nightModeAutoStart));
    nightProfile_start.set(Calendar.MINUTE, ProfileUtils.getMinute(nightModeAutoStart));
    nightProfile_start.set(Calendar.MILLISECOND, 0);
    nightProfile_start.set(Calendar.SECOND, 0);
    Timber.d(String.valueOf(Thread.currentThread().getId()));
    Calendar nightProfile_end = Calendar.getInstance();
    nightProfile_end.set(Calendar.HOUR_OF_DAY, ProfileUtils.getHour(nightModeAutoEnd));
    nightProfile_end.set(Calendar.MINUTE, ProfileUtils.getMinute(nightModeAutoEnd));
    nightProfile_end.set(Calendar.SECOND, 0);
    nightProfile_end.set(Calendar.MILLISECOND, 0);

    // Timber.d("Night prof start" + nightProfile_start);
    if (currentScheduledNightTask != null) {
      Timber.d("Task night change= " + System.identityHashCode(currentScheduledNightTask));
    } else {
      Timber.d("Task night change = null");
    }
    if (currentScheduledDayTask != null) {
      Timber.d("Task day change= " + System.identityHashCode(currentScheduledDayTask));
    } else {
      Timber.d("Task day change = null");
    }
    /*for (StackTraceElement ste : Thread.currentThread().getTrace()) {
      Timber.d(ste.toString());
    }*/
    Timber.d("Configured:");
    Timber.d("now: " + profileUtils.getHumanReadableCalendar(now, false));
    Timber.d(
        "night profile start: " + profileUtils.getHumanReadableCalendar(nightProfile_start, true));
    Timber.d("night profile end: " + profileUtils.getHumanReadableCalendar(nightProfile_end, true));

    if (isNight(nightProfile_start, nightProfile_end, now)) {
      Timber.d("apply night");
      applyNightProfile();

      String h_r_nightProfile_end = profileUtils.getHumanReadableCalendar(nightProfile_end, true);
      Timber.d("Night profile end: " + h_r_nightProfile_end);
      Timber.d(
          "Schedule next task (day change) in "
              + (nightProfile_end.getTimeInMillis() - now.getTimeInMillis()) / 1000
              + " seconds");

      currentScheduledDayTask =
          scheduledExecutorService.schedule(
              new ChangeToDayProfileTask(),
              nightProfile_end.getTimeInMillis() - now.getTimeInMillis(),
              TimeUnit.MILLISECONDS);
      Toast.makeText(
              clockActivity,
              "Apply night profile. Next change: " + h_r_nightProfile_end,
              Toast.LENGTH_LONG)
          .show();
    } else {
      Timber.d("apply day");
      applyDayProfile();
      String h_r_nightProfile_start =
          profileUtils.getHumanReadableCalendar(nightProfile_start, true);

      Timber.d("Night profile start: " + h_r_nightProfile_start);
      Timber.d(
          "Schedule next task (night change) in "
              + (nightProfile_start.getTimeInMillis() - now.getTimeInMillis()) / 1000
              + " seconds");

      currentScheduledNightTask =
          scheduledExecutorService.schedule(
              new ChangeToNightProfileTask(),
              nightProfile_start.getTimeInMillis() - now.getTimeInMillis(),
              TimeUnit.MILLISECONDS);
      Toast.makeText(
              clockActivity,
              "Apply day profile. Next change: " + h_r_nightProfile_start,
              Toast.LENGTH_LONG)
          .show();
    }
  }

  private void applyNightProfile() {
    ImageButton nightButton = clockActivity.findViewById(R.id.night_mode_button);

    GradientDrawable buttonShape = (GradientDrawable) nightButton.getBackground();
    buttonShape.mutate();
    buttonShape.setStroke(1, clockActivity.getResources().getColor(R.color.color_clock));

    Profile nightProfile = new NightProfile(prefs, clockActivity);
    nightProfile.setup();

    clockUpdater.setSdf(nightProfile.clockFormat);
    clockActivity.getmContentView().setTextSize(nightProfile.clockSize);
    clockActivity.getmContentView().setTypeface(nightProfile.font);
    clockActivity.getmContentView().setTextColor(nightProfile.textColor);
    clockActivity.getmContentView().setAlpha(nightProfile.alpha);
    clockUpdater.setMoveText(nightProfile.moveText);
    clockActivity.getmContentView().setGravity(Gravity.CENTER);
  }

  private void applyDayProfile() {
    ImageButton nightButton = clockActivity.findViewById(R.id.night_mode_button);

    GradientDrawable buttonShape = (GradientDrawable) nightButton.getBackground();
    buttonShape.mutate();
    buttonShape.setStroke(1, clockActivity.getResources().getColor(R.color.button_color));

    Profile dayProfile = new DayProfile(prefs, clockActivity);
    dayProfile.setup();
    clockActivity.getmContentView().setTextSize(dayProfile.clockSize);
    clockActivity.getmContentView().setTypeface(dayProfile.font);
    clockUpdater.setSdf(dayProfile.clockFormat);
    clockActivity.getmContentView().setTextColor(dayProfile.textColor);
    clockActivity.getmContentView().setAlpha(dayProfile.alpha);
    clockUpdater.setMoveText(dayProfile.moveText);
    clockActivity.getmContentView().setGravity(Gravity.CENTER);
  }

  private boolean isNight(Calendar nightStart, Calendar nightEnd, Calendar now) {
    if (nightStart.before(nightEnd)) {
      // | ------- start +++++++++ end -------- |
      if (now.after(nightStart) && now.before(nightEnd)) {
        // | ------- start +++!+++ end -------- |
        return true; // and do nothing with the calendars
      } else if (now.before(nightStart)) {
        // | ----!--- start ++++++++ end -------- |
        return false; // and do nothing with the calendars
      } else {
        // apply night and schedule day tomorrow
        // | ------- start ++++++++ end ----!---- |
        nightStart.add(Calendar.DAY_OF_MONTH, 1);
        return false;
      }
    } else {
      // | +++++++ end ------- start +++++++ |
      if (now.before(nightEnd)) {
        // | +++!++++ end ------- start +++++++ |
        return true; // and do nothing with the calendars
      }
      if (now.after(nightStart)) {
        // | +++++++ end ------- start ++++!+++ |
        nightEnd.add(Calendar.DAY_OF_MONTH, 1);
        return true;
      } else {
        // | +++++++ end ---!---- start +++++++ |
        return false; // and do nothing with the calendars
      }
    }
  }

  private class ChangeToNightProfileTask implements Runnable {
    private final Handler mHandler;

    public ChangeToNightProfileTask() {
      mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void run() {
      Timber.d("Change to night task started");
      mHandler.post(ProfileManager.this::applyProfile);
      Timber.d("Change to night task ended");
    }
  }

  private class ChangeToDayProfileTask implements Runnable {
    private final Handler mHandler;

    public ChangeToDayProfileTask() {
      mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void run() {
      Timber.d("Change to day task started");
      mHandler.post(ProfileManager.this::applyProfile);
      Timber.d("Change to day task ended");
    }
  }
}
