package ro.antiprotv.radioclock;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.widget.ImageButton;

import java.text.SimpleDateFormat;

import timber.log.Timber;

import static ro.antiprotv.radioclock.ClockActivity.PREF_NIGHT_MODE;
import static ro.antiprotv.radioclock.ClockActivity.TAG_RADIOCLOCK;

public class SettingsManager implements SharedPreferences.OnSharedPreferenceChangeListener {
    private ClockActivity clockActivity;
    private ButtonManager buttonManager;
    private SleepManager sleepManager;
    private ClockUpdater clockUpdater;
    private SharedPreferences prefs;

    public SettingsManager(ClockActivity clockActivity, ButtonManager buttonManager, SleepManager sleepManager, ClockUpdater clockUpdater) {
        this.clockActivity = clockActivity;
        this.buttonManager = buttonManager;
        this.sleepManager = sleepManager;
        this.clockUpdater = clockUpdater;
        prefs = PreferenceManager.getDefaultSharedPreferences(clockActivity);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
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

        if (key.equals(clockActivity.getResources().getString(R.string.setting_key_sleepMinutes))) {
            Integer customTimer = Integer.parseInt(prefs.getString(clockActivity.getResources().getString(R.string.setting_key_sleepMinutes), "0"));
            if (customTimer == 0) {
                sleepManager.getTimers().remove(0);
            } else {
                sleepManager.getTimers().add(0, customTimer);
            }
        }
        Timber.d(TAG_RADIOCLOCK, "tag: " + clockActivity.getmPlayingStreamTag() + "; key " + key);

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
        int keyClockSeconds = R.string.setting_key_seconds;
        int keyClockMove = R.string.setting_key_clockMove;

        if (nightMode) {
            keyClockColor = R.string.setting_key_clockColor_night;
            keyClockSize = R.string.setting_key_clockSize;
            keyClockSeconds = R.string.setting_key_seconds;
            keyClockMove = R.string.setting_key_clockMove;
        }

        if (key.equals(clockActivity.getResources().getString(keyClockColor))) {
            String colorCode = prefs.getString(clockActivity.getResources().getString(keyClockColor), clockActivity.getResources().getString(R.string.setting_default_clockColor));
            clockActivity.getmContentView().setTextColor(Color.parseColor(colorCode));
        }
        if (key.equals(clockActivity.getResources().getString(keyClockSize))) {
            String clockSizeKey = clockActivity.getResources().getString(keyClockSize);
            String clockSize = clockActivity.getResources().getString(R.string.setting_default_clockSize);
            int size = Integer.parseInt(prefs.getString(clockSizeKey, clockSize));
            clockActivity.getmContentView().setTextSize(size);
        }

        if (key.equals(clockActivity.getResources().getString(keyClockSeconds))) {
            SimpleDateFormat sdf;
            if (prefs.getBoolean(clockActivity.getResources().getString(keyClockSeconds), true)) {
                sdf = new SimpleDateFormat("HH:mm:ss");
            } else {
                sdf = new SimpleDateFormat("HH:mm");
            }
            clockUpdater.setSdf(sdf);
        }
        if (key.equals(clockActivity.getResources().getString(keyClockMove))) {
            clockUpdater.setMoveText(prefs.getBoolean(clockActivity.getResources().getString(keyClockMove), true));
            clockActivity.getmContentView().setGravity(Gravity.CENTER);
        }

    }

    protected void toggleNightMode(boolean nightMode) {
        ImageButton nightButton = clockActivity.findViewById(R.id.night_mode_button);
        Timber.d("Current nightmode is %b", nightMode);
        nightMode = !nightMode;
        if (nightMode) {
            GradientDrawable buttonShape = (GradientDrawable) nightButton.getBackground();
            Timber.d("SET COLOR STROKE");
            buttonShape.setStroke(1, clockActivity.getResources().getColor(R.color.color_clock));
        } else {
            GradientDrawable buttonShape = (GradientDrawable) nightButton.getBackground();
            buttonShape.setStroke(1, clockActivity.getResources().getColor(R.color.button_color));
        }
        //this will trigger the listener!!
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(clockActivity);
        Timber.d("setting nightmode to %b", nightMode);
        prefs.edit().putBoolean(ClockActivity.PREF_NIGHT_MODE, nightMode).apply();
        applyProfile(prefs.getBoolean(PREF_NIGHT_MODE, false));
        //clockActivity.invalidateOptionsMenu();


    }

    protected void applyProfile(boolean nightMode) {
        if (nightMode) {
            //Clock color
            String colorCode = prefs.getString(clockActivity.getResources().getString(R.string.setting_key_clockColor_night), clockActivity.getResources().getString(R.string.setting_default_clockColor));
            clockActivity.getmContentView().setTextColor(Color.parseColor(colorCode));
            //clock size
            String clockSizeKey = clockActivity.getResources().getString(R.string.setting_key_clockSize_night);
            String clockSize = clockActivity.getResources().getString(R.string.setting_default_clockSize);
            int size = Integer.parseInt(prefs.getString(clockSizeKey, clockSize));
            clockActivity.getmContentView().setTextSize(size);
            //clock seconds
            SimpleDateFormat sdf;
            if (prefs.getBoolean(clockActivity.getResources().getString(R.string.setting_key_seconds_night), true)) {
                sdf = new SimpleDateFormat("HH:mm:ss");
            } else {
                sdf = new SimpleDateFormat("HH:mm");
            }
            clockUpdater.setSdf(sdf);
            //clock move
            clockUpdater.setMoveText(prefs.getBoolean(clockActivity.getResources().getString(R.string.setting_key_clockMove_night), true));
            clockActivity.getmContentView().setGravity(Gravity.CENTER);
        } else {
            //Clock color
            String colorCode = prefs.getString(clockActivity.getResources().getString(R.string.setting_key_clockColor), clockActivity.getResources().getString(R.string.setting_default_clockColor));
            clockActivity.getmContentView().setTextColor(Color.parseColor(colorCode));
            //clock size
            String clockSizeKey = clockActivity.getResources().getString(R.string.setting_key_clockSize);
            String clockSize = clockActivity.getResources().getString(R.string.setting_default_clockSize);
            int size = Integer.parseInt(prefs.getString(clockSizeKey, clockSize));
            clockActivity.getmContentView().setTextSize(size);
            //clock seconds
            SimpleDateFormat sdf;
            if (prefs.getBoolean(clockActivity.getResources().getString(R.string.setting_key_seconds), true)) {
                sdf = new SimpleDateFormat("HH:mm:ss");
            } else {
                sdf = new SimpleDateFormat("HH:mm");
            }
            clockUpdater.setSdf(sdf);
            //clock move
            clockUpdater.setMoveText(prefs.getBoolean(clockActivity.getResources().getString(R.string.setting_key_clockMove), true));
            clockActivity.getmContentView().setGravity(Gravity.CENTER);
        }

    }
}
