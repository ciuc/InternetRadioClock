/*
  Copyright Cristian "ciuc" Starasciuc 2016
  <p/>
  Licensed under the Apache license 2.0
  <p/>
  cristi.ciuc@gmail.com
 */
package ro.antiprotv.radioclock;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.devbrackets.android.exomedia.AudioPlayer;
import com.devbrackets.android.exomedia.listener.OnErrorListener;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import timber.log.Timber;

/**
 * Main Activity. Just displays the clock and buttons
 */
public class ClockActivity extends AppCompatActivity {
    public static final String TAG_RADIOCLOCK = "ClockActivity: %s";
    public final static String PREF_NIGHT_MODE = "NIGHT_MODE";
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;
    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private static final String TAG_STATE = "ClockActivity | State: %s";
    private final Handler mHideHandler = new Handler();
    //the map of urls; it is a map of the setting key > url (String)
    //url(setting_key_stream1 >  http://something)
    private final List<String> mUrls = new ArrayList<>();
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }

    };
    private SharedPreferences prefs;
    private ClockUpdater clockUpdater;
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private TextView mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    private AudioPlayer mMediaPlayer;
    private ButtonManager buttonManager;
    private SleepManager sleepManager;
    //remember the playing stream number and tag
    //they will have to be reset when stopping
    private int mPlayingStreamNo;
    private String mPlayingStreamTag;
    //Alarm stuff
    //remember last picked hour
    private int h = 0;
    private int m = 0;

    private boolean alarmPlaying;
    private RadioAlarmManager alarmManager;
    private final Button.OnClickListener playOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(final View view) {
            Timber.d(TAG_RADIOCLOCK, "Play clicked: " + view.getTag());
            buttonManager.setButtonClicked((Button) view);
            buttonManager.disableButtons();
            if (mMediaPlayer != null) {
                if (mMediaPlayer.isPlaying()) {
                    if (mPlayingStreamNo == buttonManager.getButtonClicked().getId()) {
                        stopPlaying();
                    } else {
                        play(buttonManager.getButtonClicked().getId());
                    }
                } else {
                    play(buttonManager.getButtonClicked().getId());
                }
            } else {
                initMediaPlayer();
                buttonManager.enableButtons();
            }
            //I want to hide the snooze button on any interaction with the radio buttons
            alarmManager.cancelSnooze();
        }
    };
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private final Button.OnClickListener nightModeOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(final View view) {
            Timber.d(TAG_RADIOCLOCK, "night mode clicked ");
            ((SettingsManager) preferenceChangeListener).toggleNightMode();
        }
    };

    ///////////////////////////////////////////////////////////////////////////
    // State methods
    ///////////////////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d(TAG_STATE, "onCreate");
        //SOME INITIALIZATIONS
        //Initialize the preferences_buttons
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.activity_main);

        //Set up Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        mVisible = true;
        mControlsView = findViewById(R.id.mainLayout);
        mContentView = findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });
        Typeface digital7 = Typeface.createFromAsset(getAssets(), "fonts/digital-7.mono.ttf");
        mContentView.setTypeface(digital7);

        initializeUrls();
        buttonManager = new ButtonManager(getApplicationContext(), mControlsView, prefs, mDelayHideTouchListener, playOnClickListener);
        buttonManager.initializeButtons(mUrls);

        displayDialogsOnOpen();

        //Thread - clock
        //Thread for communicating with the ui handler
        //We start it here , and we sendMessage to the threadHandler in onStart (we might have a race and get threadHandler null if we try it here)
        clockUpdater = new ClockUpdater(mContentView);
        clockUpdater.start();

        preferenceChangeListener = new SettingsManager(this, buttonManager, sleepManager, clockUpdater);
        ((SettingsManager) preferenceChangeListener).applyProfile();

        initializeSleepFunction();
        initializeAlarmFunction();

        //Initialize the player
        //TODO: maybe initialize on first run
        if (mMediaPlayer == null) {
            initMediaPlayer();
        }
        //preferences and profile
        ImageButton nightModeButton = findViewById(R.id.night_mode_button);
        nightModeButton.setOnClickListener(nightModeOnClickListener);
        nightModeButton.setOnTouchListener(mDelayHideTouchListener);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Timber.d(TAG_STATE, "onPostCreate");
        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    protected void onStart() {
        Timber.d(TAG_STATE, "onStart");
        super.onStart();
        //semaphore = true;
        if (!clockUpdater.getThreadHandler().hasMessages(0)) {
            clockUpdater.getThreadHandler().sendEmptyMessage(0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Timber.d(TAG_STATE, "onResume");
        IntentFilter filter = new IntentFilter("alarmReceiver");
        this.registerReceiver(this.alarmManager, filter);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        IntentFilter alarmFilter = new IntentFilter("alarmManager");
        this.registerReceiver(this.alarmManager, alarmFilter);
    }

    @Override
    protected void onStop() {
        Timber.d(TAG_STATE, "onStop");
        super.onStop();
        clockUpdater.setSemaphore(false);
        clockUpdater.getThreadHandler().removeMessages(0);
        try {
            this.unregisterReceiver(this.alarmManager);
        } catch (IllegalArgumentException e) {
            Timber.e("receiver already unregistered");
        }
    }

    @Override
    protected void onDestroy() {
        Timber.d(TAG_STATE, "onDestroy");
        resetMediaPlayer();
        if (clockUpdater != null && !clockUpdater.isInterrupted()) {
            clockUpdater.interrupt();
        }
        clockUpdater.setThreadHandler(null);
        clockUpdater = null;
        sleepManager.getSleepExecutorService().shutdownNow();
        try {
            this.unregisterReceiver(this.alarmManager);
        } catch (IllegalArgumentException e) {
            Timber.e("receiver already unregistered");
        }
        super.onDestroy();
    }

    @Override
    protected void onRestart() {
        Timber.d(TAG_STATE, "onRestart");
        super.onRestart();
        if (mMediaPlayer == null) {
            initMediaPlayer();
        }
        clockUpdater.setSemaphore(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Timber.d(TAG_STATE, "onPause");
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //INITIALIZATIONS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void initializeAlarmFunction() {
        alarmManager = new RadioAlarmManager(this, buttonManager);
        ImageButton alarmButton = findViewById(R.id.alarm_icon);
        alarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog timePicker = new TimePickerDialog(view.getContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hh, int mm) {
                        Timber.d("Setting alarm to %d: %d", hh, mm);
                        h = hh;
                        m = mm;
                        alarmManager.setAlarm(hh, mm);
                    }
                }, h, m, true);
                timePicker.show();
            }
        });
    }

    private void initializeSleepFunction() {
        //sleep timer
        sleepManager = new SleepManager(this);
        Integer customTimer = Integer.parseInt(prefs.getString(getResources().getString(R.string.setting_key_sleepMinutes), "0"));
        if (customTimer != 0) {
            sleepManager.getTimers().add(0, customTimer);
        }
        //sleep buttons
        ImageButton sleep = findViewById(R.id.sleep);
        sleep.setOnClickListener(sleepManager.sleepOnClickListener);
        sleep.setOnTouchListener(mDelayHideTouchListener);
    }

    private void initializeUrls() {
        mUrls.add(prefs.getString(getResources().getString(R.string.setting_key_stream1), getResources().getString(R.string.setting_default_stream1)));
        mUrls.add(prefs.getString(getResources().getString(R.string.setting_key_stream2), getResources().getString(R.string.setting_default_stream2)));
        mUrls.add(prefs.getString(getResources().getString(R.string.setting_key_stream3), getResources().getString(R.string.setting_default_stream3)));
        mUrls.add(prefs.getString(getResources().getString(R.string.setting_key_stream4), getResources().getString(R.string.setting_default_stream4)));
        mUrls.add(prefs.getString(getResources().getString(R.string.setting_key_stream5), getResources().getString(R.string.setting_default_stream5)));
        mUrls.add(prefs.getString(getResources().getString(R.string.setting_key_stream6), ""));
        mUrls.add(prefs.getString(getResources().getString(R.string.setting_key_stream7), ""));
        mUrls.add(prefs.getString(getResources().getString(R.string.setting_key_stream8), ""));
    }

    private void displayDialogsOnOpen() {
        if (prefs.getBoolean("FOURTH_TIME", true)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Check out the \"About\" section to find out what you can do with this thing.")
                    .setTitle("Thanks for using this app!").setPositiveButton(R.string.dialog_button_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    prefs.edit().putBoolean("FOURTH_TIME", false).apply();
                }
            });
            AlertDialog dialog = builder.create();

            dialog.show();
        }

        if (prefs.getBoolean("AMOLED_WARN", true)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Having an image on the screen for a long time might damage your AMOLED screen.\n" +
                    "The text moves by default (like a screen saver) every 5 minutes. You can disable the movement, but if you have an AMOLED screen you are highly discouraged to do so.")
                    .setTitle("AMOLED WARNING")
                    .setIcon(R.drawable.ic_warning_black_24dp)
                    .setPositiveButton(R.string.dialog_button_ok_amoled, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            prefs.edit().putBoolean("AMOLED_WARN", false).apply();
                        }
                    });
            AlertDialog dialog = builder.create();

            dialog.show();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Media Player
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //init
    private void initMediaPlayer() {
        mMediaPlayer = new AudioPlayer(getBaseContext());
        mMediaPlayer.setOnPreparedListener(new CustomOnPreparedListener());
        mMediaPlayer.setOnErrorListener(new CustomOnErrorListener());
    }

    private void resetMediaPlayer() {
        if (mMediaPlayer != null) {
            stopPlaying();
            mMediaPlayer = null;
        }
    }

    void play(int buttonId) {
        //if already playing and comes from alarm -do nothing
        if (mMediaPlayer.isPlaying() && alarmPlaying) {
            alarmManager.changeAlarmIconAndTextOnCancel();
            return;
        }
        String url;
        //index in th list
        int index = -1;
        switch (buttonId) {
            case R.id.stream1:
                index = 0;
                break;
            case R.id.stream2:
                index = 1;
                break;
            case R.id.stream3:
                index = 2;
                break;
            case R.id.stream4:
                index = 3;
                break;
            case R.id.stream5:
                index = 4;
                break;
            case R.id.stream6:
                index = 5;
                break;
            case R.id.stream7:
                index = 6;
                break;
            case R.id.stream8:
                index = 7;
                break;
            default:
                break;
        }
        url = mUrls.get(index);
        buttonManager.resetButtons();
        Toast.makeText(ClockActivity.this, "Connecting to " + url, Toast.LENGTH_SHORT).show();
        Timber.d(TAG_RADIOCLOCK, "url " + url);
        if (url != null) {
            mMediaPlayer.setDataSource(Uri.parse(url));
        } else {//Something went wrong, resetting
            resetMediaPlayer();
            buttonManager.enableButtons();
        }
    }

    void stopPlaying() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                Toast.makeText(ClockActivity.this, "Stopping stream", Toast.LENGTH_SHORT).show();
            }
            mMediaPlayer.reset();
        }
        buttonManager.enableButtons();
        buttonManager.unlightButton();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
        }
        mPlayingStreamNo = 0;
        mPlayingStreamTag = null;
    }

    private class CustomOnPreparedListener implements OnPreparedListener {
        @Override
        public void onPrepared() {
            Timber.d(TAG_RADIOCLOCK, "Attempting to start mediaplayer ");
            mMediaPlayer.start();

            buttonManager.lightButton();
            mPlayingStreamNo = buttonManager.getButtonClicked().getId();
            mPlayingStreamTag = buttonManager.getButtonClicked().getTag().toString();
            buttonManager.enableButtons();
            Timber.d(TAG_RADIOCLOCK, "tag: " + mPlayingStreamTag);
            //default url do not show, b/c they are not present in prefs at first
            String defaultKey = mPlayingStreamTag.replace("setting.key.stream", "");
            int index = Integer.parseInt(defaultKey) - 1;
            Toast.makeText(ClockActivity.this, "Playing " + mUrls.get(index), Toast.LENGTH_SHORT).show();
            Objects.requireNonNull(getSupportActionBar()).setTitle(getResources().getString(R.string.app_name) + ": " + mUrls.get(index));
            alarmPlaying = false;
        }
    }

    private class CustomOnErrorListener implements OnErrorListener {
        @Override
        public boolean onError(Exception e) {
            Toast.makeText(ClockActivity.this, "Error playing stream", Toast.LENGTH_SHORT).show();
            Timber.e("Error playing stream: %s", e.getMessage());
            resetMediaPlayer();
            initMediaPlayer();
            buttonManager.resetButtons();
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
            }
            if (alarmPlaying) {
                alarmManager.playDefaultAlarmOnStreamError();
            }
            return false;
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // MENU
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.buttons:
                Timber.d(TAG_RADIOCLOCK, "Settings clicked");
                Intent intent = new Intent();
                intent.setClassName(this, "ro.antiprotv.radioclock.ConfigureButtonsActivity");
                startActivity(intent);
                return true;
            case R.id.settings:
                Timber.d(TAG_RADIOCLOCK, "Settings clicked");
                Intent settings = new Intent();
                settings.setClassName(this, "ro.antiprotv.radioclock.SettingsActivity");
                startActivity(settings);
                return true;
            case R.id.night:
                Timber.d(TAG_RADIOCLOCK, "Settings clicked");
                Intent night = new Intent();
                night.setClassName(this, "ro.antiprotv.radioclock.NightProfileActivity");
                startActivity(night);
                return true;
            case R.id.about:
                Timber.d(TAG_RADIOCLOCK, "about clicked");
                Intent about = new Intent();
                about.setClassName(this, "ro.antiprotv.radioclock.AboutActivity");
                startActivity(about);
                return true;
            case R.id.streamFinder:
                Intent streamFinder = new Intent();
                streamFinder.setClassName(this, "ro.antiprotv.radioclock.StreamFinderActivity");
                startActivity(streamFinder);
                return true;
            case R.id.exit:
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Delaying removal of nav bar (android studio default stuff)
    ///////////////////////////////////////////////////////////////////////////
    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    //--/////////////////////////////////////////////////////////////////////////
    // END Delaying removal of nav bar (android studio default stuff)
    //--/////////////////////////////////////////////////////////////////////////
    public void setAlarmPlaying(boolean alarmPlaying) {
        this.alarmPlaying = alarmPlaying;
    }

    public TextView getmContentView() {
        return mContentView;
    }

    public List<String> getmUrls() {
        return mUrls;
    }

    public String getmPlayingStreamTag() {
        return mPlayingStreamTag;
    }

}
