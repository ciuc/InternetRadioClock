package ro.antiprotv.radioclock.service.profile;

import static ro.antiprotv.radioclock.activity.ClockActivity.PREF_NIGHT_MODE;

import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.mrudultora.colorpicker.ColorPickerPopUp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import ro.antiprotv.radioclock.ClockUpdater;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.activity.ClockActivity;
import ro.antiprotv.radioclock.service.BatteryService;
import timber.log.Timber;

public class ProfileManager implements SharedPreferences.OnSharedPreferenceChangeListener {

  public static String SETTING_NIGHT_PROFILE_SCHEDULE_ENABLED = "setting.key.enable.night.schedule";
  public static String SETTING_NIGHT_PROFILE_AUTOSTART = "setting.key.night_profile.autostart";
  public static String SETTING_NIGHT_PROFILE_AUTOEND = "setting.key.night_profile.autoend";
  private final ClockActivity clockActivity;
  private final SharedPreferences prefs;
  private final ClockUpdater clockUpdater;
  private final ScheduledExecutorService scheduledExecutorService;
  private final ProfileUtils profileUtils;
  private final ImageView battery_icon;
  private final TextView battery_pct;
  private final String[] fonts_files;
  private final Map<String, Typeface> fonts = new HashMap<>();
  private final List<Integer> sizes = new ArrayList<>();
  private ScheduledFuture currentScheduledNightTask;
  private ScheduledFuture currentScheduledDayTask;
  private Profile currentProfile;

  private boolean disableProfileChangeOnSettingChange = false;
  private int font_index = 0;
  private int size_index = 0;

  public ProfileManager(
      ClockActivity clockActivity, SharedPreferences prefs, ClockUpdater clockUpdater) {
    this.clockActivity = clockActivity;
    this.prefs = prefs;
    this.clockUpdater = clockUpdater;
    scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
    this.profileUtils = ProfileUtils.getInstance(clockActivity);
    battery_pct = clockActivity.findViewById(R.id.batteryPct);
    battery_icon = clockActivity.findViewById(R.id.battery_icon);

    fonts_files = clockActivity.getResources().getStringArray(R.array.clock_typefaces);

    for (String fontsFile : fonts_files) {
      fonts.put(
          fontsFile, Typeface.createFromAsset(clockActivity.getAssets(), "fonts/" + fontsFile));
    }
    for (String size : clockActivity.getResources().getStringArray(R.array.clock_sizes)) {
      sizes.add(Integer.parseInt(size));
    }
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (disableProfileChangeOnSettingChange) {
      disableProfileChangeOnSettingChange = false;
      return;
    }
    if (key.equals(SETTING_NIGHT_PROFILE_AUTOSTART)
        || key.equals(SETTING_NIGHT_PROFILE_AUTOEND)
        || key.equals(SETTING_NIGHT_PROFILE_SCHEDULE_ENABLED)
        || key.equals(clockActivity.getResources().getString(R.string.setting_key_clock24))
        || key.equals(clockActivity.getResources().getString(R.string.setting_key_clock24ampm))
        || key.equals(clockActivity.getResources().getString(R.string.setting_key_clockColor))
        || key.equals(clockActivity.getResources().getString(R.string.setting_key_clockSize))
        || key.equals(clockActivity.getResources().getString(R.string.setting_key_clockBrightness))
        || key.equals(clockActivity.getResources().getString(R.string.setting_key_seconds))
        || key.equals(clockActivity.getResources().getString(R.string.setting_key_typeface))
        || key.equals(clockActivity.getResources().getString(R.string.setting_key_clockMove))
        || key.equals(clockActivity.getResources().getString(R.string.setting_key_clockColor_night))
        || key.equals(clockActivity.getResources().getString(R.string.setting_key_clockSize_night))
        || key.equals(
            clockActivity.getResources().getString(R.string.setting_key_clockBrightness_night))
        || key.equals(clockActivity.getResources().getString(R.string.setting_key_seconds_night))
        || key.equals(clockActivity.getResources().getString(R.string.setting_key_typeface_night))
        || key.equals(clockActivity.getResources().getString(R.string.setting_key_clockMove_night))
        || key.equals(
            clockActivity.getResources().getString(R.string.setting_key_batteryInClockColor))) {

      Timber.d("Pref changed: " + key);
      clearTask();
      applyProfile();
    } else if (key.equals(ClockActivity.PREF_NIGHT_MODE)) {
      applyProfile(prefs.getBoolean(ClockActivity.PREF_NIGHT_MODE, false));
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
   * If night mode schedule is not enabled, we just apply whichever profile is dictated by the button click.
   * The button click is persistent.
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

    String nightModeAutoStart = prefs.getString(SETTING_NIGHT_PROFILE_AUTOSTART, "21:00");
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
    Timber.d("now: " + profileUtils.getHumanReadableCalendar(now));
    Timber.d("night profile start: " + profileUtils.getHumanReadableCalendar(nightProfile_start));
    Timber.d("night profile end: " + profileUtils.getHumanReadableCalendar(nightProfile_end));

    if (isNight(nightProfile_start, nightProfile_end, now)) {
      Timber.d("apply night");
      applyNightProfile();

      String h_r_nightProfile_end = profileUtils.getHumanReadableCalendar(nightProfile_end);
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
      String h_r_nightProfile_start = profileUtils.getHumanReadableCalendar(nightProfile_start);

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
    clockActivity.getmContentView().setTypeface(fonts.get(nightProfile.font));
    clockActivity.getmContentView().setTextColor(nightProfile.clockColor);
    Window window = clockActivity.getWindow();
    WindowManager.LayoutParams layoutParams = window.getAttributes();
    layoutParams.screenBrightness = nightProfile.alpha;
    window.setAttributes(layoutParams);

    clockUpdater.setMoveText(nightProfile.moveText);
    clockActivity.getmContentView().setGravity(Gravity.CENTER);
    applyBatteryProfile(nightProfile.clockColor);
    currentProfile = nightProfile;
    size_index = sizes.indexOf((int) nightProfile.clockSize);
  }

  private void applyDayProfile() {
    ImageButton nightButton = clockActivity.findViewById(R.id.night_mode_button);

    GradientDrawable buttonShape = (GradientDrawable) nightButton.getBackground();
    buttonShape.mutate();
    buttonShape.setStroke(1, clockActivity.getResources().getColor(R.color.button_color));

    Profile dayProfile = new DayProfile(prefs, clockActivity);
    dayProfile.setup();
    clockActivity.getmContentView().setTextSize(dayProfile.clockSize);
    clockActivity.getmContentView().setTypeface(fonts.get(dayProfile.font));
    clockUpdater.setSdf(dayProfile.clockFormat);
    clockActivity.getmContentView().setTextColor(dayProfile.clockColor);

    Window window = clockActivity.getWindow();
    WindowManager.LayoutParams layoutParams = window.getAttributes();
    layoutParams.screenBrightness = dayProfile.alpha;
    window.setAttributes(layoutParams);
    clockUpdater.setMoveText(dayProfile.moveText);
    clockActivity.getmContentView().setGravity(Gravity.CENTER);
    applyBatteryProfile(dayProfile.clockColor);
    currentProfile = dayProfile;
    size_index = sizes.indexOf((int) dayProfile.clockSize);
  }

  public void applyBatteryProfile(int currentProfileColor) {
    int color = clockActivity.getResources().getColor(R.color.color_clock, null);
    boolean batteryInClockColor =
        prefs.getBoolean(
            clockActivity.getResources().getString(R.string.setting_key_batteryInClockColor),
            false);
    if (BatteryService.low && !BatteryService.charging) {
      color = clockActivity.getResources().getColor(R.color.color_clock_red, null);
      battery_icon.setImageResource(R.drawable.ic_baseline_battery_alert_16);
    } else {
      battery_icon.setImageResource(R.drawable.ic_baseline_battery_std_16);
      if (batteryInClockColor) {
        if (currentProfileColor == -1) {
          boolean nightmode = prefs.getBoolean(PREF_NIGHT_MODE, false);
          Profile profile = new DayProfile(prefs, clockActivity);
          if (nightmode) {
            profile = new NightProfile(prefs, clockActivity);
          }
          currentProfileColor = profile.clockColor;
        }
        color = currentProfileColor;
      }
    }
    battery_icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
    battery_pct.setTextColor(color);
  }

  public void cycleThroughFonts() {
    cycleThroughFonts(true);
  }

  public void cycleThroughFonts(boolean forward) {
    if (forward && font_index == fonts_files.length - 1) {
      font_index = 0;
    } else if (!forward && font_index == 0) {
      font_index = fonts_files.length - 1;
    } else {
      if (forward) {
        font_index++;
      } else {
        font_index--;
      }
    }
    String font = fonts_files[font_index];
    Timber.d("font index: " + font_index);
    Timber.d("font file: " + font);
    clockActivity.getmContentView().setTypeface(fonts.get(font));
    disableProfileChangeOnSettingChange = true;
    currentProfile.setFont(font);
    // Toast.makeText(clockActivity,fonts_files[font_index], Toast.LENGTH_SHORT).show();
  }

  public void cycleThroughSizes(boolean forward) {
    if (forward && size_index == sizes.size() - 1) {
      size_index = 0;
    } else if (!forward && size_index == 0) {
      size_index = sizes.size() - 1;
    } else {
      if (forward) {
        size_index++;
      } else {
        size_index--;
      }
    }
    int size = sizes.get(size_index);
    Timber.d("size index: " + size_index);
    Timber.d("size: " + size);
    clockActivity.getmContentView().setTextSize(size);
    disableProfileChangeOnSettingChange = true;
    currentProfile.setSize(size);
    // Toast.makeText(clockActivity,fonts_files[font_index], Toast.LENGTH_SHORT).show();
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

  public class ColorPickerClickListner implements View.OnClickListener{

    @Override
    public void onClick(View v) {
      ColorPickerPopUp colorPickerPopUp = new ColorPickerPopUp(v.getContext());	// Pass the context.
      colorPickerPopUp.setShowAlpha(false)			// By default show alpha is true.
              .setDefaultColor(currentProfile.getColor())			// By default red color is set.
              .setDialogTitle("Pick a Color")
              .setOnPickColorListener(new ColorPickerPopUp.OnPickColorListener() {
                @Override
                public void onColorPicked(int color) {
                  String hexColor = String.format("#%06X", (0xFFFFFF & color));
                  disableProfileChangeOnSettingChange = true;
                  clockActivity.getmContentView().setTextColor(color);
                  currentProfile.setClockColor(hexColor);
                }

                @Override
                public void onCancel() {
                  colorPickerPopUp.dismissDialog();	// Dismiss the dialog.
                }
              })
              .show();
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
