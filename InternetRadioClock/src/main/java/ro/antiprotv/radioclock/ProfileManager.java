package ro.antiprotv.radioclock;

import static ro.antiprotv.radioclock.ClockActivity.PREF_NIGHT_MODE;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import timber.log.Timber;

public class ProfileManager implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final static String HUMAN_READABLE_TIME_FORMAT = "%s, %s";
    public static String SETTING_NIGHT_PROFILE_SCHEDULE_ENABLED = "setting.key.enable.night.schedule";
    public static String SETTING_NIGHT_PROFILE_AUTOSTART = "setting.key.night_profile.autostart";
    public static String SETTING_NIGHT_PROFILE_AUTOEND = "setting.key.night_profile.autoend";
    private final ClockActivity clockActivity;
    private final SharedPreferences prefs;
    private final ClockUpdater clockUpdater;
    private final ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture currentScheduledTask;

    public ProfileManager(ClockActivity clockActivity, SharedPreferences prefs, ClockUpdater clockUpdater) {
        this.clockActivity = clockActivity;
        this.prefs = prefs;
        this.clockUpdater = clockUpdater;
        scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
    }

    public static int getHour(String time) {
        String[] pieces=time.split(":");

        return(Integer.parseInt(pieces[0]));
    }

    public static int getMinute(String time) {
        String[] pieces=time.split(":");

        return(Integer.parseInt(pieces[1]));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SETTING_NIGHT_PROFILE_AUTOSTART) ||
        key.equals(SETTING_NIGHT_PROFILE_AUTOEND)
        || key.equals(SETTING_NIGHT_PROFILE_SCHEDULE_ENABLED)) {
            applyProfile();
        } else {
            applyProfile(sharedPreferences.getBoolean(PREF_NIGHT_MODE, false));
        }

    }

    /*Called by button click */
    void toggleNightMode() {
        boolean nightMode = prefs.getBoolean(PREF_NIGHT_MODE, false);
        //this will trigger the listener!!
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(clockActivity);
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
    * Called only at startup
    * If night mode schedule is not enabled, we just apply whichever pfofile is dictated by the button click.
    * The button clck is persistent.
    * Else, if there is a schedule, we check in which period we are and schedule the next change.
    */
    void applyProfile() {
        boolean nightMode = prefs.getBoolean(PREF_NIGHT_MODE, false);

        boolean nightProfileScheduleEnabled = prefs.getBoolean(SETTING_NIGHT_PROFILE_SCHEDULE_ENABLED, false);
        if (!nightProfileScheduleEnabled) {
            applyProfile(nightMode);
            return;
        }

        if (currentScheduledTask != null) {
            currentScheduledTask.cancel(true);
        }
        String nightModeAutoStart = prefs.getString(SETTING_NIGHT_PROFILE_AUTOSTART, "23:00");
        String nightModeAutoEnd = prefs.getString(SETTING_NIGHT_PROFILE_AUTOEND, "07:00");

        // in what period are we? night or day?
        Calendar now = Calendar.getInstance();

        Calendar nightProfile_start = Calendar.getInstance();
        nightProfile_start.set(Calendar.HOUR_OF_DAY, getHour(nightModeAutoStart));
        nightProfile_start.set(Calendar.MINUTE, getMinute(nightModeAutoStart));
        nightProfile_start.set(Calendar.SECOND, 0);

        Calendar nightProfile_end = Calendar.getInstance();
        nightProfile_end.set(Calendar.HOUR_OF_DAY, getHour(nightModeAutoEnd));
        nightProfile_end.set(Calendar.MINUTE, getMinute(nightModeAutoEnd));
        nightProfile_end.set(Calendar.SECOND, 0);



        if (now.after(nightProfile_start)) {
            applyNightProfile();
            nightProfile_end.add(Calendar.DAY_OF_MONTH, 1);
            String h_r_nightProfile_end = getHumanReadableCalendar(nightProfile_end, true);
            Timber.d(h_r_nightProfile_end);

            currentScheduledTask = scheduledExecutorService.schedule(new ChangeProfileTask(), nightProfile_end.getTimeInMillis() - now.getTimeInMillis(), TimeUnit.MILLISECONDS);
            Toast.makeText(clockActivity, "Apply night profile. Next change: " + h_r_nightProfile_end, Toast.LENGTH_LONG).show();
        } else {
            applyDayProfile();

            String h_r_nightProfile_start = getHumanReadableCalendar(nightProfile_start, true);

            Timber.d(h_r_nightProfile_start);
            Timber.d(h_r_nightProfile_start);
            currentScheduledTask = scheduledExecutorService.schedule(new ChangeProfileTask(), nightProfile_start.getTimeInMillis() - now.getTimeInMillis(), TimeUnit.MILLISECONDS);
            Toast.makeText(clockActivity, "Apply day profile. Next change: " + h_r_nightProfile_start, Toast.LENGTH_LONG).show();
        }
    }

    private void applyNightProfile() {
        ImageButton nightButton = clockActivity.findViewById(R.id.night_mode_button);
        String colorCode;
        String clockSize = clockActivity.getResources().getString(R.string.setting_default_clockSize);
        String clockSizeKey;
        String clockBrightnessKey;
        int size;
        int brightness;
        //clock seconds
        //Clock color
        colorCode = prefs.getString(clockActivity.getResources().getString(R.string.setting_key_clockColor_night), clockActivity.getResources().getString(R.string.setting_default_clockColor));
        //clock size
        clockSizeKey = clockActivity.getResources().getString(R.string.setting_key_clockSize_night);

        //clock alpha
        clockBrightnessKey = clockActivity.getResources().getString(R.string.setting_key_clockBrightness_night);

        //clock move
        clockUpdater.setMoveText(prefs.getBoolean(clockActivity.getResources().getString(R.string.setting_key_clockMove_night), true));

        GradientDrawable buttonShape = (GradientDrawable) nightButton.getBackground();
        buttonShape.mutate();
        buttonShape.setStroke(1, clockActivity.getResources().getColor(R.color.color_clock));

        size = Integer.parseInt(prefs.getString(clockSizeKey, clockSize));
        brightness = prefs.getInt(clockBrightnessKey, 100);
        float brightness_ = brightness;
        clockUpdater.setSdf(clockActivity.getClockFormat());
        clockActivity.getmContentView().setTextColor(Color.parseColor(colorCode));
        clockActivity.getmContentView().setTextSize(size);
        clockActivity.getmContentView().setGravity(Gravity.CENTER);
        clockActivity.getmContentView().setAlpha(brightness_/100);
    }

    private void applyDayProfile() {
        ImageButton nightButton = clockActivity.findViewById(R.id.night_mode_button);
        String colorCode;
        String clockSize = clockActivity.getResources().getString(R.string.setting_default_clockSize);
        String clockSizeKey;
        String clockBrightnessKey;
        int size;
        int brightness;
        //Clock color
        colorCode = prefs.getString(clockActivity.getResources().getString(R.string.setting_key_clockColor), clockActivity.getResources().getString(R.string.setting_default_clockColor));

        //clock size
        clockSizeKey = clockActivity.getResources().getString(R.string.setting_key_clockSize);

        //clock alpha
        clockBrightnessKey = clockActivity.getResources().getString(R.string.setting_key_clockBrightness);

        //clock move
        clockUpdater.setMoveText(prefs.getBoolean(clockActivity.getResources().getString(R.string.setting_key_clockMove), true));

        GradientDrawable buttonShape = (GradientDrawable) nightButton.getBackground();
        buttonShape.mutate();
        buttonShape.setStroke(1, clockActivity.getResources().getColor(R.color.button_color));
        size = Integer.parseInt(prefs.getString(clockSizeKey, clockSize));
        brightness = prefs.getInt(clockBrightnessKey, 100);
        float brightness_ = brightness;
        clockUpdater.setSdf(clockActivity.getClockFormat());
        clockActivity.getmContentView().setTextColor(Color.parseColor(colorCode));
        clockActivity.getmContentView().setTextSize(size);
        clockActivity.getmContentView().setGravity(Gravity.CENTER);
        clockActivity.getmContentView().setAlpha(brightness_/100);
    }

    private String today_tomorrow(Calendar calendar) {
        Calendar hour24 = Calendar.getInstance();
        hour24.set(Calendar.HOUR, 23);
        hour24.set(Calendar.MINUTE, 59);
        hour24.set(Calendar.SECOND, 59);
        if (calendar.after(hour24)) {
            return clockActivity.getResources().getString(R.string.text_tomorrow);
        }
        return clockActivity.getResources().getString(R.string.today);
    }

    private String getHumanReadableCalendar(Calendar calendar, boolean extraInfo) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        if (extraInfo) {
            sdf = new SimpleDateFormat("dd/MM/y HH:mm:ss");
        }
        return String.format(HUMAN_READABLE_TIME_FORMAT, today_tomorrow(calendar), sdf.format(calendar.getTime()));

    }

    private class ChangeProfileTask implements Runnable {

        @Override
        public void run() {
            applyProfile();
        }
    }
}
