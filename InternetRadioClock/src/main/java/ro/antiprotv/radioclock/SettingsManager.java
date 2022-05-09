package ro.antiprotv.radioclock;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.widget.ImageButton;

import java.text.SimpleDateFormat;

import static ro.antiprotv.radioclock.ClockActivity.PREF_NIGHT_MODE;

class SettingsManager implements OnSharedPreferenceChangeListener {
    private final ClockActivity clockActivity;
    private final ButtonManager buttonManager;
    private final SleepManager sleepManager;
    private final ClockUpdater clockUpdater;
    private final SharedPreferences prefs;
    private final BatteryService batteryService;

    public SettingsManager(ClockActivity clockActivity, ButtonManager buttonManager, SleepManager sleepManager, ClockUpdater clockUpdater, BatteryService batteryService) {
        this.clockActivity = clockActivity;
        this.buttonManager = buttonManager;
        this.sleepManager = sleepManager;
        this.clockUpdater = clockUpdater;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(clockActivity);
        this.batteryService = batteryService;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        setupButtons(prefs, key);

        setupTimers(prefs, key);

        if (key.equals(clockActivity.getmPlayingStreamTag())) {
            clockActivity.stopPlaying();
            //since we stopped, the clicked button is reset
            //set this one here
            //TODO: find a better solution
            buttonManager.setButtonClicked(buttonManager.findButtonByTag(key));
            clockActivity.play(buttonManager.findButtonByTag(key).getId());
        }

        //SETTINGS DOUBLED IN NIGHT MODE
        boolean nightMode = prefs.getBoolean(ClockActivity.PREF_NIGHT_MODE, false);
        int keyClockColor = R.string.setting_key_clockColor;
        int keyClockSize = R.string.setting_key_clockSize;
        int keyClockMove = R.string.setting_key_clockMove;
        int keyClockBrightness = R.string.setting_key_clockBrightness;
        int keyClockTypeface = R.string.setting_key_typeface;

        if (nightMode) {
            keyClockColor = R.string.setting_key_clockColor_night;
            keyClockSize = R.string.setting_key_clockSize_night;
            keyClockMove = R.string.setting_key_clockMove_night;
            keyClockBrightness = R.string.setting_key_clockBrightness_night;
            keyClockTypeface = R.string.setting_key_typeface_night;
        }

        setupClockFormatting(prefs, key, keyClockColor, keyClockSize, keyClockMove, keyClockBrightness, keyClockTypeface);

        setupBatteryMonitoring(key);

    }

    private void setupTimers(SharedPreferences prefs, String key) {
        if (key.equals(clockActivity.getResources().getString(R.string.setting_key_sleepMinutes))) {
            int customTimer = 0;
            try {
                Integer.parseInt(prefs.getString(clockActivity.getResources().getString(R.string.setting_key_sleepMinutes), "0"));
            } catch (NumberFormatException e) {
                prefs.edit().putString(clockActivity.getResources().getString(R.string.setting_key_sleepMinutes), "0").apply();
            }
            if (customTimer == 0) {
                sleepManager.getTimers().remove(0);
            } else {
                sleepManager.getTimers().add(0, customTimer);
            }
        }
    }

    private void setupButtons(SharedPreferences prefs, String key) {
        //BUTTONS
        int buttonIndex = -1;

        if (key.equals(clockActivity.getResources().getString(R.string.setting_key_label1))) {
            buttonIndex = 0;
        }
        if (key.equals(clockActivity.getResources().getString(R.string.setting_key_label2))) {
            buttonIndex = 1;
        }
        if (key.equals(clockActivity.getResources().getString(R.string.setting_key_label3))) {
            buttonIndex = 2;
        }
        if (key.equals(clockActivity.getResources().getString(R.string.setting_key_label4))) {
            buttonIndex = 3;
        }
        if (key.equals(clockActivity.getResources().getString(R.string.setting_key_label5))) {
            buttonIndex = 4;
        }
        if (key.equals(clockActivity.getResources().getString(R.string.setting_key_label6))) {
            buttonIndex = 5;
        }
        if (key.equals(clockActivity.getResources().getString(R.string.setting_key_label7))) {
            buttonIndex = 6;
        }
        if (key.equals(clockActivity.getResources().getString(R.string.setting_key_label8))) {
            buttonIndex = 7;
        }
        if (key.contains("setting.key.label")) {
            buttonManager.setText(buttonIndex, prefs);
        }

        int streamIndex = -1;
        if (key.equals(clockActivity.getResources().getString(R.string.setting_key_stream1))) {
            streamIndex = 0;
        }
        if (key.equals(clockActivity.getResources().getString(R.string.setting_key_stream2))) {
            streamIndex = 1;
        }
        if (key.equals(clockActivity.getResources().getString(R.string.setting_key_stream3))) {
            streamIndex = 2;
        }
        if (key.equals(clockActivity.getResources().getString(R.string.setting_key_stream4))) {
            streamIndex = 3;
        }
        if (key.equals(clockActivity.getResources().getString(R.string.setting_key_stream5))) {
            streamIndex = 4;
        }
        if (key.equals(clockActivity.getResources().getString(R.string.setting_key_stream6))) {
            streamIndex = 5;
        }
        if (key.equals(clockActivity.getResources().getString(R.string.setting_key_stream7))) {
            streamIndex = 6;
        }
        if (key.equals(clockActivity.getResources().getString(R.string.setting_key_stream8))) {
            streamIndex = 7;
        }
        if (key.contains("stream")) {
            String url = prefs.getString(key, "");
            clockActivity.getmUrls().set(streamIndex, url);
            buttonManager.hideUnhideButtons(clockActivity.getmUrls());
        }
    }

    private void setupClockFormatting(SharedPreferences prefs, String key, int keyClockColor, int keyClockSize, int keyClockMove, int keyClockBrightness, int keyClockTypeface) {
        if (key.equals(clockActivity.getResources().getString(keyClockColor))) {
            String colorCode = prefs.getString(clockActivity.getResources().getString(keyClockColor), clockActivity.getResources().getString(R.string.setting_default_clockColor));
            clockActivity.getmContentView().setTextColor(Color.parseColor(colorCode));
        }
        if (key.equals(clockActivity.getResources().getString(keyClockBrightness))) {
            String clockBrightnessKey = clockActivity.getResources().getString(keyClockBrightness);
            float alpha_ = prefs.getInt(clockBrightnessKey, 100);
            clockActivity.getmContentView().setAlpha(alpha_/100);
        }
        if (key.equals(clockActivity.getResources().getString(keyClockSize))) {
            String clockSizeKey = clockActivity.getResources().getString(keyClockSize);
            String clockSize = clockActivity.getResources().getString(R.string.setting_default_clockSize);
            int size = Integer.parseInt(prefs.getString(clockSizeKey, clockSize));
            clockActivity.getmContentView().setTextSize(size);
        }

        if (key.equals(clockActivity.getResources().getString(R.string.setting_key_seconds)) ||
                key.equals(clockActivity.getResources().getString(R.string.setting_key_seconds_night)) ||
                key.equals(clockActivity.getResources().getString(R.string.setting_key_clock24)) ||
                key.equals(clockActivity.getResources().getString(R.string.setting_key_clock_dots)) ||
                key.equals(clockActivity.getResources().getString(R.string.setting_key_clock_vertical)) ||
                key.equals(clockActivity.getResources().getString(R.string.setting_key_clock24ampm))
        ) {
            clockUpdater.setSdf(getClockFormat());
        }
        if (key.equals(clockActivity.getResources().getString(keyClockMove))) {
            clockUpdater.setMoveText(prefs.getBoolean(clockActivity.getResources().getString(keyClockMove), true));
            clockActivity.getmContentView().setGravity(Gravity.CENTER);
        }
        String typefacePref = prefs.getString(clockActivity.getResources().getString(keyClockTypeface), "digital-7.mono.ttf");
        Typeface font = Typeface.createFromAsset(clockActivity.getAssets(), "fonts/"+typefacePref);
        clockActivity.getmContentView().setTypeface(font);

    }

    private void setupBatteryMonitoring(String key) {
        if (key.equals(clockActivity.getResources().getString(R.string.setting_key_alwaysDisplayBattery))) {
            boolean show = isAlwaysDisplayBattery();
            if (show) {
                batteryService.registerBatteryLevelReceiver();
            } else {
                batteryService.unregisterBatteryLevelReceiver();
            }
        }
    }

    void toggleNightMode() {
        boolean nightMode = prefs.getBoolean(PREF_NIGHT_MODE, false);
        //this will trigger the listener!!
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(clockActivity);
        prefs.edit().putBoolean(ClockActivity.PREF_NIGHT_MODE, !nightMode).apply();
        applyProfile();
    }

    void applyProfile() {
        boolean nightMode = prefs.getBoolean(PREF_NIGHT_MODE, false);
        ImageButton nightButton = clockActivity.findViewById(R.id.night_mode_button);

        String colorCode;
        String clockSize = clockActivity.getResources().getString(R.string.setting_default_clockSize);
        String clockSizeKey;
        String clockBrightnessKey;
        int size;
        int brightness;

        if (nightMode) {
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
        } else {
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
        }
        size = Integer.parseInt(prefs.getString(clockSizeKey, clockSize));
        brightness = prefs.getInt(clockBrightnessKey, 100);
        float brightness_ = brightness;
        clockUpdater.setSdf(getClockFormat());
        clockActivity.getmContentView().setTextColor(Color.parseColor(colorCode));
        clockActivity.getmContentView().setTextSize(size);
        clockActivity.getmContentView().setGravity(Gravity.CENTER);
        clockActivity.getmContentView().setAlpha(brightness_/100);
    }

    protected SimpleDateFormat getClockFormat() {
        boolean nightMode = prefs.getBoolean(ClockActivity.PREF_NIGHT_MODE, false);
        boolean keyClockSeconds = prefs.getBoolean(clockActivity.getResources().getString(R.string.setting_key_seconds), true);
        if (nightMode) {
            //clock seconds
            keyClockSeconds = prefs.getBoolean(clockActivity.getResources().getString(R.string.setting_key_seconds_night), true);
        }

        boolean clock12 = prefs.getBoolean(clockActivity.getResources().getString(R.string.setting_key_clock24), false);
        StringBuilder clockPattern = new StringBuilder();
        if (!clock12) {
            clockPattern.append("HH:mm");
        } else {
            clockPattern.append("hh:mm");
        }
        if (keyClockSeconds) {
            clockPattern.append(":ss");
        }
        if(clock12 && prefs.getBoolean(clockActivity.getResources().getString(R.string.setting_key_clock24ampm), true)) {
            clockPattern.append(" a");
        }
        String pattern = clockPattern.toString();
        if (clockActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT &&
                prefs.getBoolean(clockActivity.getResources().getString(R.string.setting_key_clock_vertical), true)) {
            pattern  = pattern.replaceAll(":",":\n");
            pattern  = pattern.replaceAll(" ","\n");
        }

        if (!prefs.getBoolean(clockActivity.getResources().getString(R.string.setting_key_clock_dots), true)) {
            pattern = pattern.replaceAll(":","");
        }
        return new SimpleDateFormat(pattern);
    }

    public boolean isAlwaysDisplayBattery() {
        return prefs.getBoolean(clockActivity.getResources().getString(R.string.setting_key_alwaysDisplayBattery), false);
    }
}
