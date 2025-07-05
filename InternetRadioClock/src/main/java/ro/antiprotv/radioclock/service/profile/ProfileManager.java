package ro.antiprotv.radioclock.service.profile;

import static ro.antiprotv.radioclock.R.string.apply_day_profile_next_change;
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
import ro.antiprotv.radioclock.service.BrightnessManager;
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
  public static final Map<String, Typeface> fonts = new HashMap<>();
  private final List<Integer> sizes = new ArrayList<>();
  private BrightnessManager brightnessManager;
  private ScheduledFuture currentScheduledNightTask;
  private ScheduledFuture currentScheduledDayTask;
  private Profile currentProfile;
  // SeekBar seekbar_brightness;

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
        }
        Timber.d("Configured:");
        Timber.d("now: " + profileUtils.getHumanReadableCalendar(now));
        Timber.d("night profile start: " + profileUtils.getHumanReadableCalendar(nightProfile_start));
        Timber.d("night profile end: " + profileUtils.getHumanReadableCalendar(nightProfile_end));
    */
    if (ProfileUtils.isNight(nightProfile_start, nightProfile_end, now)) {
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
              clockActivity.getString(R.string.apply_night_profile_next_change)
                  + h_r_nightProfile_end,
              Toast.LENGTH_LONG)
          .show();
    } else {
      // Timber.d("apply day");
      applyDayProfile();
      String h_r_nightProfile_start = profileUtils.getHumanReadableCalendar(nightProfile_start);

      /*
            Timber.d("Night profile start: " + h_r_nightProfile_start);
            Timber.d(
                "Schedule next task (night change) in "
                    + (nightProfile_start.getTimeInMillis() - now.getTimeInMillis()) / 1000
                    + " seconds");
      */
      currentScheduledNightTask =
          scheduledExecutorService.schedule(
              new ChangeToNightProfileTask(),
              nightProfile_start.getTimeInMillis() - now.getTimeInMillis(),
              TimeUnit.MILLISECONDS);
      Toast.makeText(
              clockActivity,
              clockActivity.getString(apply_day_profile_next_change) + h_r_nightProfile_start,
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
    // clockActivity.getmContentView().setTextSize(nightProfile.clockSize);
    // clockActivity.getmContentView().setTypeface(fonts.get(nightProfile.font));
    // clockActivity.getmContentView().setTextColor(nightProfile.clockColor);
    clockActivity.applyProfile(nightProfile);

    clockUpdater.setMoveText(nightProfile.moveText);
    clockActivity.getClockTextView().setGravity(Gravity.CENTER);
    applyBatteryProfile(nightProfile.clockColor);
    currentProfile = nightProfile;
    size_index = sizes.indexOf((int) nightProfile.clockSize);

    Window window = clockActivity.getWindow();
    WindowManager.LayoutParams layoutParams = window.getAttributes();
    Timber.d("(applyNightProfile) brightness: " + nightProfile.brightness);
    layoutParams.screenBrightness = nightProfile.brightness / 100f;
    window.setAttributes(layoutParams);
    if (brightnessManager != null) {
      brightnessManager.setupSeekbar(currentProfile.brightness);
    } else {
      Timber.d("brightness manager null");
    }
  }

  private void applyDayProfile() {
    ImageButton nightButton = clockActivity.findViewById(R.id.night_mode_button);

    GradientDrawable buttonShape = (GradientDrawable) nightButton.getBackground();
    buttonShape.mutate();
    buttonShape.setStroke(1, clockActivity.getResources().getColor(R.color.button_color));

    Profile dayProfile = new DayProfile(prefs, clockActivity);
    dayProfile.setup();
    clockUpdater.setSdf(dayProfile.clockFormat);
    // clockActivity.getmContentView().setTextSize(dayProfile.clockSize);
    // clockActivity.getmContentView().setTypeface(fonts.get(dayProfile.font));
    // clockActivity.getmContentView().setTextColor(dayProfile.clockColor);
    clockActivity.applyProfile(dayProfile);

    clockUpdater.setMoveText(dayProfile.moveText);
    clockActivity.getClockTextView().setGravity(Gravity.CENTER);
    applyBatteryProfile(dayProfile.clockColor);
    currentProfile = dayProfile;
    size_index = sizes.indexOf((int) dayProfile.clockSize);

    Window window = clockActivity.getWindow();
    WindowManager.LayoutParams layoutParams = window.getAttributes();
    Timber.d("(applyDayProfile) " + dayProfile.brightness);
    layoutParams.screenBrightness = dayProfile.brightness / 100f;
    window.setAttributes(layoutParams);
    if (brightnessManager != null) {
      brightnessManager.setupSeekbar(currentProfile.brightness);
    } else {
      Timber.d("brightness manager null");
    }
  }

  public void applyBatteryProfile(int currentProfileColor) {
    int color = clockActivity.getResources().getColor(R.color.color_clock, null);
    boolean batteryInClockColor =
        prefs.getBoolean(
            clockActivity.getResources().getString(R.string.setting_key_batteryInClockColor),
            false);
    if (BatteryService.low && !BatteryService.charging) {
      color = clockActivity.getResources().getColor(R.color.color_clock_red, null);
      if (BatteryService.status > 5) {
        battery_icon.setImageResource(R.drawable.baseline_battery_1_bar_24);
        battery_pct.setTextSize(20);
      } else {
        battery_icon.setImageResource(R.drawable.baseline_battery_0_bar_24);
        battery_pct.setTextSize(20);
      }
    } else {
      battery_icon.setImageResource(R.drawable.ic_baseline_battery_std_16);
      battery_pct.setTextSize(12);
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
    disableProfileChangeOnSettingChange = true;
    currentProfile.saveFont(font);
    // clockActivity.getClockTextView().setTypeface(fonts.get(font));
    clockActivity.applyProfile(currentProfile);
    // Toast.makeText(clockActivity,fonts_files[font_index], Toast.LENGTH_SHORT).show();
  }

  @Deprecated
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
    // clockActivity.getClockTextView().setTextSize(size);
    disableProfileChangeOnSettingChange = true;
    currentProfile.saveSize(size);
    clockActivity.applyProfile(currentProfile);
    // Toast.makeText(clockActivity,fonts_files[font_index], Toast.LENGTH_SHORT).show();
  }

  /**
   * increases/decreases the size of the font, from buttons, with small increments
   *
   * @param forward
   */
  public void changeSize(boolean forward) {
    int increment = 1;
    if (!forward) {
      increment = -increment;
    }

    disableProfileChangeOnSettingChange = true;
    currentProfile.saveSize(currentProfile.getSize() + increment);
    clockActivity.applyProfile(currentProfile);
    // Toast.makeText(clockActivity,fonts_files[font_index], Toast.LENGTH_SHORT).show();
  }

  public void changeSize(float diff) {
    diff *= 1.5f;
    Timber.d(
        "size now : "
            + currentProfile.getSize()
            + "; diff "
            + diff
            + "; applied diff "
            + diff * 10);
    float size = currentProfile.getSize();
    size += diff * 2;
    if (size < 0) {
      size *= -1;
    }
    size = Math.min(size, 300);
    size = Math.max(size, 20);
    Timber.d("new size: " + size);
    clockActivity.applyProfile(currentProfile);
    disableProfileChangeOnSettingChange = true;
    currentProfile.saveSize(size);
  }

  public int getBrightness() {
    Timber.d("brightness: " + currentProfile.brightness);
    return currentProfile.brightness;
  }

  public void setBrightness(int brightness) {
    Timber.d("brightness: " + brightness);
    disableProfileChangeOnSettingChange = true;
    currentProfile.saveBrightness(brightness);
    Window window = clockActivity.getWindow();
    WindowManager.LayoutParams layoutParams = window.getAttributes();
    layoutParams.screenBrightness = brightness / 100f;
    window.setAttributes(layoutParams);
  }

  public boolean isDateEnabled() {
    return currentProfile.showDate;
  }

  public void setDateEnabled(boolean showDate) {
    currentProfile.saveShowDate(showDate);
    clockActivity.applyProfile(currentProfile);
  }

  public int dateSize() {
    return currentProfile.dateSize;
  }

  public void setDateSize(int dateSize) {
    currentProfile.saveDateSize(dateSize);
    clockActivity.applyProfile(currentProfile);
  }

  public void setBrightnessManager(BrightnessManager brightnessManager) {
    this.brightnessManager = brightnessManager;
  }

  public Profile getCurrentProfile() {
    return currentProfile;
  }

  public class ColorPickerClickListner implements View.OnClickListener {

    @Override
    public void onClick(View v) {
      ColorPickerPopUp colorPickerPopUp = new ColorPickerPopUp(v.getContext()); // Pass the context.
      colorPickerPopUp
          .setShowAlpha(false) // By default show alpha is true.
          .setDefaultColor(currentProfile.getColor()) // By default red color is set.
          .setDialogTitle(clockActivity.getString(R.string.pick_a_color))
          .setOnPickColorListener(
              new ColorPickerPopUp.OnPickColorListener() {
                @Override
                public void onColorPicked(int color) {
                  String hexColor = String.format("#%06X", (0xFFFFFF & color));
                  disableProfileChangeOnSettingChange = true;
                  currentProfile.clockColor = color;
                  currentProfile.saveClockColor(hexColor);
                  clockActivity.applyProfile(currentProfile);
                }

                @Override
                public void onCancel() {
                  colorPickerPopUp.dismissDialog(); // Dismiss the dialog.
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
