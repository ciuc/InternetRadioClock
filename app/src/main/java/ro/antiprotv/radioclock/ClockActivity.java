package ro.antiprotv.radioclock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.MediaCodec;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.devbrackets.android.exomedia.EMAudioPlayer;
import com.devbrackets.android.exomedia.core.listener.InternalErrorListener;
import com.devbrackets.android.exomedia.listener.OnErrorListener;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.audio.AudioTrack;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
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

    public static final String TAG_RADIOCLOCK = "RadioClock";
    private TextView mContentView;
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    private EMAudioPlayer mMediaPlayer;
    private Typeface digital7;
    private List<Button> buttons = new ArrayList<Button>();

    //the button we have clicked on
    private Button mButtonClicked;
    //remember the playing stream number and tag
    //they will have to be reset when stopping
    private int mPlayingStreamNo;
    private String mPlayingStreamTag;

    //the map of urls; it is a map of the setting key > url (String)
    //url(setting_key_stream1 >  http://something)
    private final HashMap<String, String> mUrls = new HashMap<String, String>();

    ///////////////////////////////////////////////////////////////////////////
    // State methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = (TextView) findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        digital7 = Typeface.createFromAsset(getAssets(), "fonts/digital-7.mono.ttf");

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.stream1).setOnTouchListener(mDelayHideTouchListener);
        findViewById(R.id.stream1).setOnClickListener(playOnClickListener);
        findViewById(R.id.stream2).setOnTouchListener(mDelayHideTouchListener);
        findViewById(R.id.stream2).setOnClickListener(playOnClickListener);
        findViewById(R.id.stream3).setOnTouchListener(mDelayHideTouchListener);
        findViewById(R.id.stream3).setOnClickListener(playOnClickListener);

        mContentView.setTypeface(digital7);
        mContentView.setTextSize(TypedValue.COMPLEX_UNIT_FRACTION,200);

        //Clock changing thread
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mContentView.setText(sdf.format(new Date()));
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        t.start();

        //Initialize the buttons list
        Button stream1 = (Button) findViewById(R.id.stream1);
        Button stream2 = (Button) findViewById(R.id.stream2);
        Button stream3 = (Button) findViewById(R.id.stream3);
        buttons = Arrays.asList(new Button[]{stream1,stream2,stream3});

        //make sure the buttons are enabled
        enableButtons();

        //Initialize the preferences
        mUrls.put(getResources().getString(R.string.setting_key_stream1), getPreferences(Context.MODE_PRIVATE).getString(getResources().getString(R.string.setting_key_stream1), getResources().getString(R.string.setting_default_stream1)));
        mUrls.put(getResources().getString(R.string.setting_key_stream2), getPreferences(Context.MODE_PRIVATE).getString(getResources().getString(R.string.setting_key_stream2), getResources().getString(R.string.setting_default_stream2)));
        mUrls.put(getResources().getString(R.string.setting_key_stream3), getPreferences(Context.MODE_PRIVATE).getString(getResources().getString(R.string.setting_key_stream3), getResources().getString(R.string.setting_default_stream3)));

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
        Log.d(TAG_RADIOCLOCK, "onDestroy called");
        stop();
        mMediaPlayer = null;
        super.onDestroy();
    }
    @Override
    protected void onRestart() {
        super.onRestart();
        resetMediaPlayer();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Buttons
    ///////////////////////////////////////////////////////////////////////////

    private final Button.OnClickListener playOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(final View view) {
            Log.d(TAG_RADIOCLOCK, "Play clicked: " + view.getTag());
            mButtonClicked = (Button) view;
            disableButtons();
            if (mMediaPlayer != null) {
                if (mMediaPlayer.isPlaying()) {
                    if (mPlayingStreamNo == mButtonClicked.getId()) {
                        stop();
                    } else {
                        play(mButtonClicked.getId());
                    }
                } else {
                    play(mButtonClicked.getId());
                }
            } else {
                initMediaPlayer();
                enableButtons();
            }
        }

    };

    /* convenience methods to deal with button UI */
    private void disableButtons() {
        for (Button button: buttons) {
            button.setEnabled(false);
        }
    }
    private void enableButtons() {
        for (Button button: buttons) {
            button.setEnabled(true);
        }
    }

    private void resetButtons() {
        for (Button button: buttons) {
            button.setEnabled(true);
            button.setTextColor(getResources().getColor(R.color.button_color_off));
        }
    }
    private int findButtonByTag(String tag) {
        for (Button button: buttons) {
            if (button.getTag().equals(tag)) {
                return button.getId();
            }
        }
        return 0;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Media Player
    ///////////////////////////////////////////////////////////////////////////
    //init
    private void initMediaPlayer() {
        mMediaPlayer = new EMAudioPlayer(getBaseContext());
        mMediaPlayer.setOnPreparedListener(new CustomOnPreparedListener());
        mMediaPlayer.setOnErrorListener(new CustomOnErrorListener());
    }
    private void resetMediaPlayer() {
        if (mMediaPlayer != null) {
            stop();
            mMediaPlayer = null;
        }
    }
    private class CustomOnPreparedListener implements OnPreparedListener{
         @Override
        public void onPrepared() {
            mMediaPlayer.start();
            for (Button button: buttons) {
                button.setTextColor(getResources().getColor(R.color.button_color_off));
             }
             mButtonClicked.setTextColor(getResources().getColor(R.color.color_clock));
             mPlayingStreamNo = mButtonClicked.getId();
             mPlayingStreamTag = mButtonClicked.getTag().toString();
             enableButtons();
             Log.d(TAG_RADIOCLOCK, "tag: " + mPlayingStreamTag);

            getSupportActionBar().setTitle(getResources().getString(R.string.app_name) + ": " + mUrls.get(mPlayingStreamTag));
        }
    }

    private class CustomOnErrorListener implements OnErrorListener {
        @Override
        public boolean onError() {
            Toast.makeText(ClockActivity.this, "Error playing stream", Toast.LENGTH_SHORT).show();
            Toast.makeText(ClockActivity.this, "Error playing stream", Toast.LENGTH_SHORT).show();
            resetMediaPlayer();
            initMediaPlayer();
            resetButtons();
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
            }
            return false;
        }
    }

    private void play(int buttonId) {
        String url;
        switch (buttonId) {
            case R.id.stream1:
                url=mUrls.get(getResources().getString(R.string.setting_key_stream1));
                break;
            case R.id.stream2:
                url=mUrls.get(getResources().getString(R.string.setting_key_stream2));
                break;
            case R.id.stream3:
                url=mUrls.get(getResources().getString(R.string.setting_key_stream3));
                break;
            default:
                url = "http://live.guerrillaradio.ro:8010/guerrilla.aac";
                break;
        }
        Toast.makeText(ClockActivity.this, "Playing " + url, Toast.LENGTH_SHORT).show();
        Log.d(TAG_RADIOCLOCK, "url " + url);
        if (url != null) {
            mMediaPlayer.setDataSource(getBaseContext(), Uri.parse(url));
        } else {//Something went wrong, resetting
            resetMediaPlayer();
            enableButtons();
        }
    }

    private void stop(){
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                Toast.makeText(ClockActivity.this, "Stopping stream", Toast.LENGTH_SHORT).show();
            }
            mMediaPlayer.reset();
        }
        enableButtons();
        if (mButtonClicked != null) {
            mButtonClicked.setTextColor(getResources().getColor(R.color.button_color_off));
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
        }
        mButtonClicked = null;
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
            case R.id.settings:
                Log.d(TAG_RADIOCLOCK, "Settings clicked");
                Intent intent = new Intent();
                intent.setClassName(this, "ro.antiprotv.radioclock.PreferencesActivity");
                startActivity(intent);
                return true;
            case R.id.exit:
                resetMediaPlayer();
                Intent home = new Intent(Intent.ACTION_MAIN);
                home.addCategory(Intent.CATEGORY_HOME);
                home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(home);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener mListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            mUrls.put(key, prefs.getString(key, "aaa"));

            Log.d(TAG_RADIOCLOCK,"tag: " + mPlayingStreamTag + "; key" + key);

                if (key.equals(mPlayingStreamTag)) {
                    stop();
                    play(findButtonByTag(key));
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
