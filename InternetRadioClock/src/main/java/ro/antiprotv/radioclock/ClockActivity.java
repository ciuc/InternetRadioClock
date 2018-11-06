/**
 * Copyright Cristian "ciuc" Starasciuc 2016
 * <p/>
 * Licensed under the Apache license 2.0
 * <p/>
 * cristi.ciuc@gmail.com
 */
package ro.antiprotv.radioclock;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.devbrackets.android.exomedia.AudioPlayer;
import com.devbrackets.android.exomedia.listener.OnErrorListener;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import timber.log.Timber;

/**
 * Main Activity. Just displays the clock and buttons
 */
public class ClockActivity extends AppCompatActivity {
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
    private final Handler mHideHandler = new Handler();
    private View mControlsView;
    private boolean mVisible;
    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;

    public static final String TAG_RADIOCLOCK = "ClockActivity: %s";
    public static final String TAG_STATE = "ClockActivity | State: %s";
    private TextView mContentView;
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    private AudioPlayer mMediaPlayer;
    private Typeface digital7;

    private List<Button> buttons = new ArrayList<Button>();
    private ButtonManager buttonManager;
    //remember the playing stream number and tag
    //they will have to be reset when stopping
    private int mPlayingStreamNo;
    private String mPlayingStreamTag;

    //the map of urls; it is a map of the setting key > url (String)
    //url(setting_key_stream1 >  http://something)
    private final List<String> mUrls = new ArrayList<>();

    ClockUpdater clockUpdater;

    ///////////////////////////////////////////////////////////////////////////
    // State methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Initialize the preferences_buttons
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_main);

        //Set up Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        mVisible = true;
        mControlsView = findViewById(R.id.mainLayout);
        mContentView = (TextView) findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        digital7 = Typeface.createFromAsset(getAssets(), "fonts/digital-7.mono.ttf");

        buttonManager = new ButtonManager(getApplicationContext(), mControlsView,prefs,mDelayHideTouchListener, playOnClickListener);

        mContentView.setTypeface(digital7);

        String clockSizeKey = getResources().getString(R.string.setting_key_clockSize);
        String clockSize = getResources().getString(R.string.setting_default_clockSize);
        Timber.d(TAG_RADIOCLOCK, clockSizeKey);
        Timber.d(TAG_RADIOCLOCK, clockSize);

        int size = Integer.parseInt(prefs.getString(clockSizeKey, clockSize));

        mContentView.setTextSize(size);
        mContentView.setTextColor(Color.parseColor(prefs.getString(getResources().getString(R.string.setting_key_clockColor), getResources().getString(R.string.setting_default_clockColor))));

        boolean displaySeconds = prefs.getBoolean(getResources().getString(R.string.setting_key_seconds), true);
        if (!displaySeconds) {
            sdf = new SimpleDateFormat("HH:mm");
        }
        //Thread - clock

        boolean moveText = prefs.getBoolean(getResources().getString(R.string.setting_key_clockMove), true);
        //Thread for communicating with the ui handler
        //We start it here , and we sendMessage to the threadHandler in onStart (we might have a race and get threadHandler null if we try it here)
        clockUpdater = new ClockUpdater(sdf, mContentView, moveText);
        clockUpdater.start();

        if (prefs.getBoolean("FOURTH_TIME",true)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("You can set the clock size and color or disable seconds from the settings.\n" +
                    "Also you can add more buttons from the \"Configure Buttons\" section: by setting the stream url they will appear automatically.\n (Deleting the stream url will make the button dissapear.)")
                    .setTitle("Thanks for using this app!").setPositiveButton(R.string.dialog_button_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    prefs.edit().putBoolean("FOURTH_TIME", false).apply();
                }
            });
            AlertDialog dialog = builder.create();

            dialog.show();
        }

        if (prefs.getBoolean("AMOLED_WARN",true)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Having an image on the screen for a long time might damage your AMOLED screen.\n" +
                    "The text moves by default (like a screen saver) every 5 minutes. You can disable the movement, but if you have an AMOLED screen it is highly discouraged to do so.")
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

        mUrls.add(prefs.getString(getResources().getString(R.string.setting_key_stream1), getResources().getString(R.string.setting_default_stream1)));
        mUrls.add(prefs.getString(getResources().getString(R.string.setting_key_stream2), getResources().getString(R.string.setting_default_stream2)));
        mUrls.add(prefs.getString(getResources().getString(R.string.setting_key_stream3), getResources().getString(R.string.setting_default_stream3)));
        mUrls.add(prefs.getString(getResources().getString(R.string.setting_key_stream4), getResources().getString(R.string.setting_default_stream4)));
        mUrls.add(prefs.getString(getResources().getString(R.string.setting_key_stream5), ""));
        mUrls.add(prefs.getString(getResources().getString(R.string.setting_key_stream6), ""));
        mUrls.add(prefs.getString(getResources().getString(R.string.setting_key_stream7), ""));
        mUrls.add(prefs.getString(getResources().getString(R.string.setting_key_stream8), ""));

        buttons = buttonManager.initializeButtons(mUrls);

        //sleep timer
        Integer customTimer = Integer.parseInt(prefs.getString(getResources().getString(R.string.setting_key_sleepMinutes), "0"));
        if (customTimer != 0) {
            timers.add(0, customTimer);
        }
        //we use 2 buttons here when buttons are 8 we use the one top right, if < 8 bottom right
        //the 2 buttons just hide/unhide
        ImageButton sleep = (ImageButton) findViewById(R.id.sleep);
        ImageButton sleep8 = (ImageButton) findViewById(R.id.sleep8);
        sleep.setOnClickListener(sleepOnClickListener);
        sleep.setOnTouchListener(mDelayHideTouchListener);
        sleep8.setOnClickListener(sleepOnClickListener);
        sleep8.setOnTouchListener(mDelayHideTouchListener);
        //we hide/unhide
        hideUnhideSleepButtons();

        //Initialize the player
        if (mMediaPlayer == null) {
            initMediaPlayer();
        }
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mListener);

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    protected void onDestroy() {
        Timber.d(TAG_STATE, "onDestroy");
        resetMediaPlayer();
        clockUpdater.setSemaphore(false);
        if (clockUpdater != null && !clockUpdater.isInterrupted()) {
            clockUpdater.interrupt();
        }
        clockUpdater.setThreadHandler(null);
        clockUpdater = null;
        sleepExecutorService.shutdownNow();
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
    protected void onStop() {
        Timber.d(TAG_STATE, "onStop");
        super.onStop();
        clockUpdater.setSemaphore(false);
        clockUpdater.getThreadHandler().removeMessages(0);
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

    ///////////////////////////////////////////////////////////////////////////
    // Buttons
    ///////////////////////////////////////////////////////////////////////////

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
        }

    };


    ///////////////////////////////////////////////////////////////////////////
    //SLEEP
    ///////////////////////////////////////////////////////////////////////////
    //initialize the sleep timers default list (pressing button will cycle through those)
    private final List<Integer> timers = new ArrayList<>(Arrays.asList(15,20,30));
    private int sleepTimerIndex;
    ExecutorService sleepExecutorService = Executors.newSingleThreadExecutor();

    private final Button.OnClickListener sleepOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            TextView sleepTimerText = findViewById(R.id.sleep_timer);
            TextView sleepTimerText8 = findViewById(R.id.sleep_timer8);
            ImageButton button = findViewById(R.id.sleep);
            ImageButton button8 = findViewById(R.id.sleep8);
            if (sleepTimerIndex == timers.size()) {
                resetSleepTimer();
                sleepExecutorService.shutdownNow();
            } else {
                //stop the timer thread
                sleepExecutorService.shutdownNow();
                button.setImageResource(R.drawable.sleep_timer_on_white_24dp);
                button8.setImageResource(R.drawable.sleep_timer_on_white_24dp);
                Integer timer = timers.get(sleepTimerIndex);
                sleepTimerText.setText(String.format(getResources().getString(R.string.text_sleep_timer),timer));
                sleepTimerText8.setText(String.format(getResources().getString(R.string.text_sleep_timer),timer));
                sleepTimerIndex++;
                //now start the thread
                SleepRunner sleepRunner = new SleepRunner(timer);
                sleepExecutorService = Executors.newSingleThreadExecutor();
                sleepExecutorService.execute(sleepRunner);
            }

        }
    };

    private void resetSleepTimer() {
        TextView sleepTimerText = findViewById(R.id.sleep_timer);
        TextView sleepTimerText8 = findViewById(R.id.sleep_timer8);
        ImageButton button = findViewById(R.id.sleep);
        ImageButton button8 = findViewById(R.id.sleep8);
        sleepTimerText.setText("");
        sleepTimerText8.setText("");
        button.setImageResource(R.drawable.sleep_timer_off_white_24dp);
        button8.setImageResource(R.drawable.sleep_timer_off_white_24dp);
        sleepTimerIndex = 0;
    }
    private class SleepRunner implements Runnable {
        int timer;
        SleepRunner(int timer) {
            Timber.d(TAG_RADIOCLOCK, "Starting thread with timer: " + timer);
            this.timer = timer;
        }
        @Override
        public void run() {
            int seconds = timer * 60;
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    for (int i = seconds; i >= 0; i--) {
                        Timber.d(TAG_RADIOCLOCK, "Thread sleep; seconds: " + i);
                        Thread.sleep(1000);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ClockActivity.this, "Time's up", Toast.LENGTH_SHORT).show();
                            stopPlaying();
                            resetSleepTimer();
                        }
                    });
                    Thread.currentThread().interrupt();
                }
            } catch (InterruptedException e) {
                Timber.d(TAG_RADIOCLOCK, "Sleep Thread interrupted ");
            }
        }
    }
    private void hideUnhideSleepButtons() {
        TextView sleepTimerText = findViewById(R.id.sleep_timer);
        TextView sleepTimerText8 = findViewById(R.id.sleep_timer8);
        ImageButton button = findViewById(R.id.sleep);
        ImageButton button8 = findViewById(R.id.sleep8);
        for (String url : mUrls) {
            if (url.isEmpty()) {
                sleepTimerText.setVisibility(View.VISIBLE);
                button.setVisibility(View.VISIBLE);
                sleepTimerText8.setVisibility(View.GONE);
                button8.setVisibility(View.GONE);
                return;
            }
        }
        sleepTimerText8.setVisibility(View.VISIBLE);
        button8.setVisibility(View.VISIBLE);
        sleepTimerText.setVisibility(View.GONE);
        button.setVisibility(View.GONE);
    }
    ///////////////////////////////////////////////////////////////////////////
    // Media Player
    ///////////////////////////////////////////////////////////////////////////
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
            int index = Integer.parseInt(defaultKey) -1;
            Toast.makeText(ClockActivity.this, "Playing " + mUrls.get(index), Toast.LENGTH_SHORT).show();
            getSupportActionBar().setTitle(getResources().getString(R.string.app_name) + ": " + mUrls.get(index));
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
            return false;
        }
    }

    private void play(int buttonId) {
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
                url = "http://live.guerrillaradio.ro:8010/guerrilla.aac";
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

    private void stopPlaying() {
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
        buttonManager.setButtonClicked(null);
        mPlayingStreamNo = 0;
        mPlayingStreamTag = null;
    }
    ///////////////////////////////////////////////////////////////////////////
    // Settings
    ///////////////////////////////////////////////////////////////////////////

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

    private final SharedPreferences.OnSharedPreferenceChangeListener mListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            int buttonIndex = -1;

            if (key.equals(getResources().getString(R.string.setting_key_label1))) {
                buttonIndex = 0;
            }
            if (key.equals(getResources().getString(R.string.setting_key_label2))) {
                buttonIndex = 1;
            }
            if (key.equals(getResources().getString(R.string.setting_key_label3))) {
                buttonIndex = 2;
            }
            if (key.equals(getResources().getString(R.string.setting_key_label4))) {
                buttonIndex = 3;
            }
            if (key.equals(getResources().getString(R.string.setting_key_label5))) {
                buttonIndex = 4;
            }
            if (key.equals(getResources().getString(R.string.setting_key_label6))) {
                buttonIndex = 5;
            }
            if (key.equals(getResources().getString(R.string.setting_key_label7))) {
                buttonIndex = 6;
            }
            if (key.equals(getResources().getString(R.string.setting_key_label8))) {
                buttonIndex = 7;
            }
            if (key.contains("setting.key.label")){
                buttonManager.setText(buttonIndex, prefs);
            }
            if (key.equals(getResources().getString(R.string.setting_key_clockColor))) {
                String colorCode = prefs.getString(getResources().getString(R.string.setting_key_clockColor), getResources().getString(R.string.setting_default_clockColor));
                Timber.d(TAG_RADIOCLOCK, "Setting color clock to " + colorCode);
                mContentView.setTextColor(Color.parseColor(colorCode));
            }
            if (key.equals(getResources().getString(R.string.setting_key_clockSize))) {
                String clockSizeKey = getResources().getString(R.string.setting_key_clockSize);
                String clockSize = getResources().getString(R.string.setting_default_clockSize);
                Timber.d(TAG_RADIOCLOCK, clockSizeKey);
                Timber.d(TAG_RADIOCLOCK, clockSize);
                int size = Integer.parseInt(prefs.getString(clockSizeKey, clockSize));
                Timber.d(TAG_RADIOCLOCK, "Setting size clock to " + size);
                mContentView.setTextSize(size);
            }
            int streamIndex = -1;
            if (key.equals(getResources().getString(R.string.setting_key_stream1))){
                streamIndex = 0;
            }
            if (key.equals(getResources().getString(R.string.setting_key_stream2))){
                streamIndex = 1;
            }
            if (key.equals(getResources().getString(R.string.setting_key_stream3))){
                streamIndex = 2;
            }
            if (key.equals(getResources().getString(R.string.setting_key_stream4))){
                streamIndex = 3;
            }
            if (key.equals(getResources().getString(R.string.setting_key_stream5))){
                streamIndex = 4;
            }
            if (key.equals(getResources().getString(R.string.setting_key_stream6))){
                streamIndex = 5;
            }
            if (key.equals(getResources().getString(R.string.setting_key_stream7))){
                streamIndex = 6;
            }
            if (key.equals(getResources().getString(R.string.setting_key_stream8))){
                streamIndex = 7;
            }
            if (key.contains("stream")) {
                String url = prefs.getString(key, "");
                mUrls.set(streamIndex, url);
                buttonManager.hideUnhideButtons(mUrls);
                hideUnhideSleepButtons();
            }

            if (key.equals(getResources().getString(R.string.setting_key_sleepMinutes))) {
                Integer customTimer = Integer.parseInt(prefs.getString(getResources().getString(R.string.setting_key_sleepMinutes), "0"));
                if (customTimer == 0) {
                    timers.remove(0);
                } else {
                    timers.add(0, customTimer);
                }
            }
            Timber.d(TAG_RADIOCLOCK, "tag: " + mPlayingStreamTag + "; key " + key);

            if (key.equals(mPlayingStreamTag)) {
                stopPlaying();
                //since we stopped, the clicked button is reset
                //set this one here
                //TODO: find a better solution
                buttonManager.setButtonClicked(buttonManager.findButtonByTag(key));
                play(buttonManager.findButtonByTag(key).getId());
            }
            if (key.equals(getResources().getString(R.string.setting_key_seconds))){
                if (prefs.getBoolean(getResources().getString(R.string.setting_key_seconds), true)) {
                    sdf = new SimpleDateFormat("HH:mm:ss");
                } else {
                    sdf = new SimpleDateFormat("HH:mm");
                }
                clockUpdater.setSdf(sdf);
            }
            if (key.equals(getResources().getString(R.string.setting_key_clockMove))){
                clockUpdater.setMoveText(prefs.getBoolean(getResources().getString(R.string.setting_key_clockMove), true));
                mContentView.setGravity(Gravity.CENTER);
            }
        }
    };

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

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
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
    private void show() {
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

}
