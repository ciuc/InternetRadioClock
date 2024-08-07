/*
 Copyright Cristian "ciuc" Starasciuc 2016
 <p/>
 Licensed under the Apache license 2.0
 <p/>
 cristi.ciuc@gmail.com
*/
package ro.antiprotv.radioclock.activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.devbrackets.android.exomedia.AudioPlayer;
import com.devbrackets.android.exomedia.listener.OnErrorListener;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import ro.antiprotv.radioclock.BuildConfig;
import ro.antiprotv.radioclock.ClockUpdater;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.SleepManager;
import ro.antiprotv.radioclock.TipsDialog;
import ro.antiprotv.radioclock.service.BatteryService;
import ro.antiprotv.radioclock.service.BrightnessManager;
import ro.antiprotv.radioclock.service.ButtonManager;
import ro.antiprotv.radioclock.service.RadioAlarmManager;
import ro.antiprotv.radioclock.service.SettingsManager;
import ro.antiprotv.radioclock.service.ShowCaseService;
import ro.antiprotv.radioclock.service.VolumeManager;
import ro.antiprotv.radioclock.service.profile.ProfileManager;
import timber.log.Timber;

/** Main Activity. Just displays the clock and buttons */
public class ClockActivity extends AppCompatActivity {
  public static final String PREF_NIGHT_MODE = "NIGHT_MODE";
  public static final String LAST_PLAYED = "LAST_PLAYED";

  public static final String USER_ALARM_PERMISSION_NOT_ALLOWED_PREF = "USER_PERM_NOK";

  /**
   * Whether or not the system UI should be auto-hidden after {@link #AUTO_HIDE_DELAY_MILLIS}
   * milliseconds.
   */
  private static final boolean AUTO_HIDE = true;

  /**
   * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after user interaction before
   * hiding the system UI.
   */
  private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

  /**
   * Some older devices needs a small delay between UI widget updates and a change of the status and
   * navigation bar.
   */
  private static final int UI_ANIMATION_DELAY = 0;

  private static final String TAG_STATE = "ClockActivity | State: %s";
  public static final int FADE_OUT_DURATION_MILLIS = 400;
  public static final int FADE_IN_DURATION_MILLIS = 200;
  private final Handler mHideHandler = new Handler();
  // the map of urls; it is a map of the setting key > url (String)
  // url(setting_key_stream1 >  http://something)
  private final List<String> mUrls = new ArrayList<>();
  private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
  private GestureDetector swipeGestureDetector;
  private ScaleGestureDetector pinchGestureDetector;

  /**
   * The user has explicitly denied setting alarms permissions.
   *
   * <p>TRUE = user explicitly did not allow the alarm
   * <li>- he canceled the dialog
   * <li>- OR
   * <li>- he went to the dialog but did not set the permission
   *
   *     <p>FALSE = (default) user has not explicitly denied
   */
  public boolean USER_ALARM_PERMISSION_NOT_ALLOWED = false;

  public boolean HAS_ALARM_PERMISSIONS = true;
  private SharedPreferences prefs;
  private ClockUpdater clockUpdater;
  private View mControlsView;
  private final Runnable mShowPart2Runnable =
      new Runnable() {
        @Override
        public void run() {
          Animation fadeIn = getFadeInAnimation();

          // Delayed display of UI elements
          ActionBar actionBar = getSupportActionBar();
          if (actionBar != null) {
            actionBar.show();
            toolbar.startAnimation(
                AnimationUtils.loadAnimation(mControlsView.getContext(), R.anim.slide_from_top));
          }
          mControlsView.setVisibility(VISIBLE);
          mControlsView.startAnimation(fadeIn);
        }
      };

  private boolean mVisible;
  private TextView mContentView;
  private final Runnable mHidePart2Runnable =
      new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
          // Delayed removal of status and navigation bar

          // Note that some of these constants are new as of API 16 (Jelly Bean)
          // and API 19 (KitKat). It is safe to use them, as they are inlined
          // at compile-time and do nothing on earlier devices.
          mContentView.setSystemUiVisibility(
              View.SYSTEM_UI_FLAG_LOW_PROFILE
                  | View.SYSTEM_UI_FLAG_FULLSCREEN
                  | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                  | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                  | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                  | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
      };
  private AudioPlayer mMediaPlayer;
  private ButtonManager buttonManager;
  private SleepManager sleepManager;
  private VolumeManager volumeManager;
  private ImageButton onOffButton;
  // remember the playing stream number and tag
  // they will have to be reset when stopping
  private int mPlayingStreamNo;
  private String mPlayingStreamTag;
  // Alarm stuff
  // remember last picked hour
  private int h = 0;
  private int m = 0;
  private boolean alarmPlaying;
  private boolean alarmSnoozing;
  private RadioAlarmManager alarmManager;
  private BatteryService batteryService;
  private ProfileManager profileManager;
  private final Button.OnClickListener nightModeOnClickListener =
      new View.OnClickListener() {

        @Override
        public void onClick(final View view) {
          profileManager.toggleNightMode();
        }
      };
  private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
  private ScheduledFuture progressiveVolumeTask;
  private final Runnable mHideRunnable = () -> hide();

  /**
   * Touch listener to use for in-layout UI controls to delay hiding the system UI. This is to
   * prevent the jarring behavior of controls going away while interacting with activity UI.
   */
  private final View.OnTouchListener mDelayHideTouchListener =
      (view, motionEvent) -> {
        if (AUTO_HIDE) {
          delayedHide(AUTO_HIDE_DELAY_MILLIS);
        }
        return false;
      };

  private final Button.OnClickListener playOnClickListener =
      new View.OnClickListener() {

        @Override
        public void onClick(final View view) {
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
          // I want to hide the snooze button on any interaction with the radio buttons
          if (alarmSnoozing) {
            alarmManager.cancelSnooze();
          }
        }
      };

  ///////////////////////////////////////////////////////////////////////////
  // State methods
  ///////////////////////////////////////////////////////////////////////////
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (BuildConfig.DEBUG && Timber.treeCount() == 0) {
      Timber.plant(new Timber.DebugTree());
    }
    // SOME INITIALIZATIONS
    // Initialize the preferences_buttons
    prefs = PreferenceManager.getDefaultSharedPreferences(this);
    // Migration
    if (prefs
        .getString(getResources().getString(R.string.setting_key_typeface), "repet___.ttf")
        .equals("digital-7.mono.ttf")) {
      prefs
          .edit()
          .putString(getResources().getString(R.string.setting_key_typeface), "repet___.ttf")
          .apply();
    }
    if (prefs
        .getString(getResources().getString(R.string.setting_key_typeface_night), "repet___.ttf")
        .equals("digital-7.mono.ttf")) {
      prefs
          .edit()
          .putString(getResources().getString(R.string.setting_key_typeface_night), "repet___.ttf")
          .apply();
    }
    // --end migration
    setOrientationLandscapeIfLocked();
    int currentOrientation = getResources().getConfiguration().orientation;
    // if reverse enabled we load the reverse layout
    boolean reverse =
        prefs.getBoolean(getResources().getString(R.string.setting_key_reverseButtons), false);
    if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE && reverse) {
      setContentView(R.layout.activity_main_reverse);
    } else if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
      setContentView(R.layout.activity_main);
    } else if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
      setContentView(R.layout.activity_main_portrait);
    } else {
      setContentView(R.layout.activity_main);
    }

    mVisible = true;
    mControlsView = findViewById(R.id.mainLayout);
    mContentView = findViewById(R.id.fullscreen_content);
    RelativeLayout overlay = findViewById(R.id.overlay);

    // Set up the user interaction to manually show or hide the system UI.
    overlay.setOnClickListener(view -> toggle());

    initializeUrls();
    buttonManager =
        new ButtonManager(
            getApplicationContext(),
            mControlsView,
            prefs,
            mDelayHideTouchListener,
            playOnClickListener);
    buttonManager.initializeButtons(mUrls);

    displayDialogsOnOpen();

    // Thread - clock
    // Thread for communicating with the ui handler
    // We start it here , and we sendMessage to the threadHandler in onStart (we might have a race
    // and get threadHandler null if we try it here)
    clockUpdater = new ClockUpdater(mContentView);
    clockUpdater.start();
    initializeSleepFunction();

    initializeAlarmFunction();

    profileManager = new ProfileManager(this, prefs, clockUpdater);

    this.batteryService = new BatteryService(this, profileManager);
    preferenceChangeListener =
        new SettingsManager(this, buttonManager, sleepManager, clockUpdater, batteryService);
    profileManager.clearTask();
    profileManager.applyProfile();
    BrightnessManager brightnessManager =
            new BrightnessManager(this, mControlsView, profileManager);
    profileManager.setBrightnessManager(brightnessManager);
    // Initialize the player
    // TODO: maybe initialize on first run
    if (mMediaPlayer == null) {
      initMediaPlayer();
    }
    // preferences and profile
    ImageButton nightModeButton = findViewById(R.id.night_mode_button);
    nightModeButton.setOnClickListener(nightModeOnClickListener);
    nightModeButton.setOnTouchListener(mDelayHideTouchListener);

    // Volume
    volumeManager = new VolumeManager(this, mControlsView);

    // Play at start?
    // button clicked is either last played, or the first; will also set
    if (prefs.getBoolean(getResources().getString(R.string.setting_key_playAtStart), false)) {
      play(buttonManager.getButtonClicked().getId());
    }

    final ImageButton helpButton = findViewById(R.id.main_help_button);
    helpButton.setOnClickListener(new HelpOnClickListener(this));

    onOffButton = findViewById(R.id.on_off_button);
    onOffButton.setOnClickListener(new OnOnOffClickListener());

    if (((SettingsManager) preferenceChangeListener).isAlwaysDisplayBattery()) {
      batteryService.registerBatteryLevelReceiver();
    } else {
      batteryService.unregisterBatteryLevelReceiver();
    }

    findViewById(R.id.font_cycle_button_fwd)
        .setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                profileManager.cycleThroughFonts();
              }
            });
    findViewById(R.id.font_cycle_button_rev)
        .setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                profileManager.cycleThroughFonts(false);
              }
            });
    findViewById(R.id.text_size_cycle_button_fwd)
        .setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                profileManager.cycleThroughSizes(true);
              }
            });
    findViewById(R.id.text_size_cycle_button_rev)
        .setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                profileManager.cycleThroughSizes(false);
              }
            });

    findViewById(R.id.color_picker_button)
        .setOnClickListener(profileManager.new ColorPickerClickListner());

    swipeGestureDetector = new GestureDetector(this, new SwipeGestureDetector());
    pinchGestureDetector = new ScaleGestureDetector(getApplicationContext(), new ScaleListener());

    // Toolbar
    toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
  }

  private Toolbar toolbar = null;

  private void setOrientationLandscapeIfLocked() {
    if (prefs.getBoolean(
        getResources().getString(R.string.setting_key_lockOrientationLandscape), true)) {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }
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
  protected void onStart() {
    super.onStart();
    if (!clockUpdater.getThreadHandler().hasMessages(0)) {
      clockUpdater.getThreadHandler().sendEmptyMessage(0);
    }
    prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    prefs.registerOnSharedPreferenceChangeListener(profileManager);
  }

  @Override
  protected void onResume() {
    super.onResume();
    IntentFilter filter = new IntentFilter("alarmReceiver");
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      this.registerReceiver(this.alarmManager, filter, Context.RECEIVER_NOT_EXPORTED);
    } else {
      this.registerReceiver(this.alarmManager, filter);
    }
    setOrientationLandscapeIfLocked();
  }

  @Override
  protected void onStop() {
    super.onStop();

    clockUpdater.setSemaphore(false);
    clockUpdater.getThreadHandler().removeMessages(0);
  }

  @Override
  protected void onDestroy() {
    resetMediaPlayer();
    if (clockUpdater != null && !clockUpdater.isInterrupted()) {
      clockUpdater.interrupt();
    }
    clockUpdater.setThreadHandler(null);
    clockUpdater = null;
    sleepManager.stop();
    try {
      this.unregisterReceiver(this.alarmManager);
    } catch (Throwable e) {
      // move along
    }
    try {
      batteryService.unregisterBatteryLevelReceiver();
    } catch (Throwable e) {
      // move along
    }
    prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    prefs.unregisterOnSharedPreferenceChangeListener(profileManager);
    super.onDestroy();
  }

  @Override
  protected void onRestart() {
    // Timber.d(TAG_STATE, "onRestart");
    super.onRestart();
    if (mMediaPlayer == null) {
      initMediaPlayer();
    }
    clockUpdater.setSemaphore(true);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // INITIALIZATIONS
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // ............
  // ALARMS
  // ............
  private void showDialogPermissionAlarm() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder
        .setMessage(
            "In order to use the alarm functionality, you need to \nallow the app to set alarms and reminders. \nYou can always do this later, or revoke the permission at any time.")
        .setTitle("Use alarms?")
        .setIcon(R.drawable.outline_icon_alarm_24)
        .setPositiveButton(
            R.string.dialog_button_permission_OK,
            (dialog, id) -> {
              prefs.edit().putBoolean(USER_ALARM_PERMISSION_NOT_ALLOWED_PREF, true).apply();
              startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
            })
        .setNeutralButton(
            R.string.dialog_button_permission_REMIND_LTR, (dialog, id) -> dialog.cancel())
        .setNegativeButton(
            R.string.dialog_button_permission_CANCEL,
            ((dialog, id) ->
                prefs.edit().putBoolean(USER_ALARM_PERMISSION_NOT_ALLOWED_PREF, true).apply()));

    AlertDialog dialog = builder.create();
    dialog.show();
  }

  private void initializeAlarmFunction() {
    alarmManager = new RadioAlarmManager(this, buttonManager);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      AlarmManager systemAlarmManager = this.getSystemService(AlarmManager.class);
      HAS_ALARM_PERMISSIONS = systemAlarmManager.canScheduleExactAlarms();
    }
    USER_ALARM_PERMISSION_NOT_ALLOWED =
        prefs.getBoolean(USER_ALARM_PERMISSION_NOT_ALLOWED_PREF, false);
    if (!HAS_ALARM_PERMISSIONS) {
      setAlarmDialogOnClick();
      if (!USER_ALARM_PERMISSION_NOT_ALLOWED) {
        showDialogPermissionAlarm();
      }
      return;
    }

    alarmManager.setAlarm();
    ImageButton alarmButton = findViewById(R.id.alarm_icon);
    alarmButton.setOnClickListener(
        view -> {
          final Context context = view.getContext();
          TimePickerDialog timePicker =
              new CustomTimePickerDialog(
                  context,
                  (timePicker12, hh, mm) -> {
                    h = hh;
                    m = mm;
                    DaysDialog daysDialog = new DaysDialog(context, h, m, 1);
                    daysDialog.show();
                  },
                  h,
                  m,
                  true);
          timePicker.show();
        });
    ImageButton alarmButton2 = findViewById(R.id.alarm_icon2);
    alarmButton2.setOnClickListener(
        view -> {
          final Context context = view.getContext();
          TimePickerDialog timePicker =
              new CustomTimePickerDialog(
                  context,
                  (timePicker1, hh, mm) -> {
                    h = hh;
                    m = mm;
                    DaysDialog daysDialog = new DaysDialog(context, h, m, 2);
                    daysDialog.show();
                  },
                  h,
                  m,
                  true);
          timePicker.show();
        });
  }

  private void setAlarmDialogOnClick() {
    ImageButton alarmButton = findViewById(R.id.alarm_icon);
    alarmButton.setOnClickListener(view -> showDialogPermissionAlarm());
    ImageButton alarmButton2 = findViewById(R.id.alarm_icon2);
    alarmButton2.setOnClickListener(view -> showDialogPermissionAlarm());
  }

  // ......................
  // ALARM END
  // ......................
  private void initializeSleepFunction() {
    // sleep timer
    sleepManager = new SleepManager(this, clockUpdater);
    sleepManager.init();

    // sleep buttons
    ImageButton sleep = findViewById(R.id.sleep);
    sleep.setOnClickListener(sleepManager.sleepButtonOnClickListener);
    sleep.setOnTouchListener(mDelayHideTouchListener);
  }

  private void initializeUrls() {
    mUrls.add(
        prefs.getString(
            getResources().getString(R.string.setting_key_stream1),
            getResources().getString(R.string.setting_default_stream1)));
    mUrls.add(
        prefs.getString(
            getResources().getString(R.string.setting_key_stream2),
            getResources().getString(R.string.setting_default_stream2)));
    mUrls.add(
        prefs.getString(
            getResources().getString(R.string.setting_key_stream3),
            getResources().getString(R.string.setting_default_stream3)));
    mUrls.add(
        prefs.getString(
            getResources().getString(R.string.setting_key_stream4),
            getResources().getString(R.string.setting_default_stream4)));
    mUrls.add(
        prefs.getString(
            getResources().getString(R.string.setting_key_stream5),
            getResources().getString(R.string.setting_default_stream5)));
    mUrls.add(prefs.getString(getResources().getString(R.string.setting_key_stream6), ""));
    mUrls.add(prefs.getString(getResources().getString(R.string.setting_key_stream7), ""));
    mUrls.add(prefs.getString(getResources().getString(R.string.setting_key_stream8), ""));
  }

  private void displayDialogsOnOpen() {
    final String currentDialog = "NINTH_TIME";
    if (prefs.getBoolean(currentDialog, true)) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder
          .setMessage(
              "Swipe to change fonts and pinch to change font size.\nThere are two alarms and both can be recurring.\nYou can setup the night profile to start automatically at a certain time.")
          .setTitle("New Stuff!")
          .setIcon(R.drawable.baseline_info_24)
          .setPositiveButton(
              R.string.dialog_button_ok,
              (dialog, id) -> prefs.edit().putBoolean(currentDialog, false).apply());
      AlertDialog dialog = builder.create();

      dialog.show();
    }

    if (prefs.getBoolean("AMOLED_WARN", true)) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder
          .setMessage(
              "Having an image on the screen for a long time might damage your AMOLED screen.\n"
                  + "The text moves by default (like a screen saver) every 5 minutes. You can disable the movement, but if you have an AMOLED screen you are highly discouraged to do so.")
          .setTitle("AMOLED WARNING")
          .setIcon(R.drawable.ic_warning_black_24dp)
          .setPositiveButton(
              getString(R.string.dialog_button_ok_amoled),
              (dialog, id) -> prefs.edit().putBoolean("AMOLED_WARN", false).apply());
      AlertDialog dialog = builder.create();

      dialog.show();
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Media Player
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // init
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

  public void cancelProgressiveVolumeTask() {
    if (progressiveVolumeTask != null && !progressiveVolumeTask.isCancelled()) {
      progressiveVolumeTask.cancel(true);
    }
  }

  public void play(int buttonId) {
    // if already playing and comes from alarm -do nothing
    if (mMediaPlayer == null) {
      initMediaPlayer();
    }
    if (mMediaPlayer.isPlaying() && alarmPlaying) {
      alarmManager.changeAlarmIconAndTextOnCancel();
      return;
    }
    // we might have a default alarm playing, so need to shut it off
    alarmManager.shutDownDefaultAlarm();
    boolean isProgressiveSound =
        prefs.getBoolean(
            getResources().getString(R.string.setting_key_alarmProgressiveSound), false);
    Timber.d(String.format("progressive: %b", isProgressiveSound));
    if (alarmPlaying && isProgressiveSound) {
      cancelProgressiveVolumeTask();
      volumeManager.setVolume(1);
      // Timber.d("Scheduling progressive volume task");
      progressiveVolumeTask =
          executorService.scheduleWithFixedDelay(
              new AlarmProgressiveVolume(), 10, 20, TimeUnit.SECONDS);
    }
    String url;
    // index in th list
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
    if (url != null) {
      mMediaPlayer.setDataSource(Uri.parse(url));
    } else { // Something went wrong, resetting
      resetMediaPlayer();
      buttonManager.enableButtons();
    }
  }

  public void stopPlaying() {
    if (mMediaPlayer != null) {
      if (mMediaPlayer.isPlaying()) {
        Toast.makeText(ClockActivity.this, "Stopping stream", Toast.LENGTH_SHORT).show();
      }
      mMediaPlayer.reset();
    }
    buttonManager.enableButtons();
    buttonManager.unlightButton();
    onOffButton.setColorFilter(getResources().getColor(R.color.color_clock_red));

    if (getSupportActionBar() != null) {
      getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
    }
    mPlayingStreamNo = 0;
    mPlayingStreamTag = null;
    cancelProgressiveVolumeTask();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // MENU
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main_menu, menu);
    /*AppCompatActivity activity = this;
    new Handler()
        .post(
            new Runnable() {
              @Override
              public void run() {
                boolean showCasePlayed = prefs.getBoolean("SHOW_CASE_PLAYED_1", false);
                //if (!showCasePlayed) {
                  ShowCaseService showCaseService = new ShowCaseService(activity);
                  showCaseService.showCase();
                  prefs.edit().putBoolean("SHOW_CASE_PLAYED_1", true).apply();
                //}
              }
            });*/
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    switch (item.getItemId()) {
      case R.id.close:
        finish();
        return true;
      case R.id.settings:
        Intent settings = new Intent();
        settings.setClassName(this, "ro.antiprotv.radioclock.activity.SettingsActivity");
        startActivity(settings);
        return true;
      case R.id.reverse:
        setRequestedOrientation(-1 * (getRequestedOrientation() - 8));
        return true;
      case R.id.buttons:
        Intent intent = new Intent();
        intent.setClassName(this, "ro.antiprotv.radioclock.activity.ConfigureButtonsActivity");
        startActivity(intent);
        return true;
      case R.id.night:
        Intent night = new Intent();
        night.setClassName(this, "ro.antiprotv.radioclock.activity.NightProfileActivity");
        startActivity(night);
        return true;
      case R.id.about:
        Intent about = new Intent();
        about.setClassName(this, "ro.antiprotv.radioclock.activity.AboutActivity");
        startActivity(about);
        return true;
      case R.id.cheers:
        Intent cheers = new Intent();
        cheers.setClassName(this, "ro.antiprotv.radioclock.activity.CheersActivity");
        startActivity(cheers);
        return true;
      case R.id.streamFinder:
        Intent streamFinder = new Intent();
        streamFinder.setClassName(this, "ro.antiprotv.radioclock.activity.StreamFinderActivity");
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

  public void hide() {
    if (alarmPlaying) {
      cancelProgressiveVolumeTask();
      alarmManager.shutDownRadioAlarm(false);
      alarmManager.cancelNonRecurringAlarm();
      alarmManager.setAlarm();
      setAlarmPlaying(false);
    }
    Animation fadeOut = getFadeOutAnimation();
    // Hide UI first
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      toolbar.startAnimation(
          AnimationUtils.loadAnimation(mControlsView.getContext(), R.anim.slide_to_top));
      actionBar.hide();
    }
    mControlsView.setVisibility(GONE);
    mControlsView.startAnimation(fadeOut);
    mVisible = false;

    // Schedule a runnable to remove the status and navigation bar after a delay
    mHideHandler.removeCallbacks(mShowPart2Runnable);
    mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
  }

  @SuppressLint("InlinedApi")
  public void show() {
    // Show the system bar
    mContentView.setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    mVisible = true;

    // Schedule a runnable to display UI elements after a delay
    mHideHandler.removeCallbacks(mHidePart2Runnable);
    mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
  }

  /**
   * Schedules a call to hide() in [delay] milliseconds, canceling any previously scheduled calls.
   */
  private void delayedHide(int delayMillis) {
    mHideHandler.removeCallbacks(mHideRunnable);
    mHideHandler.postDelayed(mHideRunnable, delayMillis);
  }

  // --/////////////////////////////////////////////////////////////////////////
  // END Delaying removal of nav bar (android studio default stuff)
  // --/////////////////////////////////////////////////////////////////////////
  public void setAlarmPlaying(boolean alarmPlaying) {
    this.alarmPlaying = alarmPlaying;
  }

  public void setAlarmSnoozing(boolean alarmSnoozing) {
    this.alarmSnoozing = alarmSnoozing;
  }

  public TextView getmContentView() {
    return mContentView;
  }

  public View getmControlsView() {
    return mControlsView;
  }
  public List<String> getmUrls() {
    return mUrls;
  }

  public String getmPlayingStreamTag() {
    return mPlayingStreamTag;
  }

  private class HelpOnClickListener implements View.OnClickListener {
    private AppCompatActivity activity;

    public HelpOnClickListener(AppCompatActivity activity) {
      this.activity = activity;
    }
    @Override
    public void onClick(View view) {
      android.app.AlertDialog tipsDialog = new TipsDialog(view.getContext());
      tipsDialog.show();
      //ShowCaseService service = new ShowCaseService(activity);
      //service.showCase();
    }

  }

  /**
   * Necessary just to inhibit onStop to prevent double onTimeSet call on certain versions of
   * android
   */
  private static class CustomTimePickerDialog extends TimePickerDialog {

    public CustomTimePickerDialog(
        Context context,
        OnTimeSetListener listener,
        int hourOfDay,
        int minute,
        boolean is24HourView) {
      super(context, listener, hourOfDay, minute, is24HourView);
    }

    @Override
    protected void onStop() {
      // inhibit
    }
  }

  private class AlarmProgressiveVolume implements Runnable {
    public void run() {
      if (volumeManager.getVolume() > volumeManager.getMaxVolume()) {
        cancelProgressiveVolumeTask();
        return;
      }
      volumeManager.volumeUp();
    }
  }

  private class OnOnOffClickListener implements View.OnClickListener {
    @Override
    public void onClick(View view) {
      if (mMediaPlayer.isPlaying()) {
        stopPlaying();
      } else {
        play(buttonManager.getButtonClicked().getId());
      }
      delayedHide(AUTO_HIDE_DELAY_MILLIS);
    }
  }

  private class DaysDialog extends AlertDialog {
    protected DaysDialog(@NonNull Context context, final int hh, final int mm, int alarmId) {
      super(context);
      Map<Integer, Map<String, Integer>> alarmKeys = new HashMap<>();
      Map<String, Integer> alarmIds1 = new HashMap<>();
      alarmIds1.put("MON", R.id.setting_alarm_1_key_2);
      alarmIds1.put("TUE", R.id.setting_alarm_1_key_3);
      alarmIds1.put("WED", R.id.setting_alarm_1_key_4);
      alarmIds1.put("THU", R.id.setting_alarm_1_key_5);
      alarmIds1.put("FRI", R.id.setting_alarm_1_key_6);
      alarmIds1.put("SAT", R.id.setting_alarm_1_key_7);
      alarmIds1.put("SUN", R.id.setting_alarm_1_key_1);
      alarmKeys.put(RadioAlarmManager.ALARM_ID_1, alarmIds1);
      Map<String, Integer> alarmIds2 = new HashMap<>();
      alarmIds2.put("MON", R.id.setting_alarm_1_key_2);
      alarmIds2.put("TUE", R.id.setting_alarm_1_key_3);
      alarmIds2.put("WED", R.id.setting_alarm_1_key_4);
      alarmIds2.put("THU", R.id.setting_alarm_1_key_5);
      alarmIds2.put("FRI", R.id.setting_alarm_1_key_6);
      alarmIds2.put("SAT", R.id.setting_alarm_1_key_7);
      alarmIds2.put("SUN", R.id.setting_alarm_1_key_1);
      alarmKeys.put(RadioAlarmManager.ALARM_ID_2, alarmIds2);
      final LinearLayout layout =
          (LinearLayout) LayoutInflater.from(context).inflate(R.layout.dialog_alarm_days, null);

      final CheckBox MON = layout.findViewById(alarmKeys.get(alarmId).get("MON"));
      final CheckBox TUE = layout.findViewById(alarmKeys.get(alarmId).get("TUE"));
      final CheckBox WED = layout.findViewById(alarmKeys.get(alarmId).get("WED"));
      final CheckBox THU = layout.findViewById(alarmKeys.get(alarmId).get("THU"));
      final CheckBox FRI = layout.findViewById(alarmKeys.get(alarmId).get("FRI"));
      final CheckBox SAT = layout.findViewById(alarmKeys.get(alarmId).get("SAT"));
      final CheckBox SUN = layout.findViewById(alarmKeys.get(alarmId).get("SUN"));
      MON.setChecked(prefs.getBoolean("setting.alarm." + alarmId + ".key.2", false));
      TUE.setChecked(prefs.getBoolean("setting.alarm." + alarmId + ".key.3", false));
      WED.setChecked(prefs.getBoolean("setting.alarm." + alarmId + ".key.4", false));
      THU.setChecked(prefs.getBoolean("setting.alarm." + alarmId + ".key.5", false));
      FRI.setChecked(prefs.getBoolean("setting.alarm." + alarmId + ".key.6", false));
      SAT.setChecked(prefs.getBoolean("setting.alarm." + alarmId + ".key.7", false));
      SUN.setChecked(prefs.getBoolean("setting.alarm." + alarmId + ".key.1", false));
      setView(layout);
      setButton(
          AlertDialog.BUTTON_POSITIVE,
          getString(R.string.ok),
          (dialog, which) -> {
            prefs.edit().putBoolean("setting.alarm." + alarmId + ".key.2", MON.isChecked()).apply();
            prefs.edit().putBoolean("setting.alarm." + alarmId + ".key.3", TUE.isChecked()).apply();
            prefs.edit().putBoolean("setting.alarm." + alarmId + ".key.4", WED.isChecked()).apply();
            prefs.edit().putBoolean("setting.alarm." + alarmId + ".key.5", THU.isChecked()).apply();
            prefs.edit().putBoolean("setting.alarm." + alarmId + ".key.6", FRI.isChecked()).apply();
            prefs.edit().putBoolean("setting.alarm." + alarmId + ".key.7", SAT.isChecked()).apply();
            prefs.edit().putBoolean("setting.alarm." + alarmId + ".key.1", SUN.isChecked()).apply();
            prefs.edit().putInt("setting.alarm." + alarmId + ".hh", hh).apply();
            prefs.edit().putInt("setting.alarm." + alarmId + ".mm", mm).apply();
            alarmManager.setAlarm();
          });
      setTitle(R.string.title_dialog_alarm_days);
    }
  }

  private class CustomOnPreparedListener implements OnPreparedListener {
    @Override
    public void onPrepared() {
      mMediaPlayer.start();

      buttonManager.lightButton();
      mPlayingStreamNo = buttonManager.getButtonClicked().getId();
      mPlayingStreamTag = buttonManager.getButtonClicked().getTag().toString();
      buttonManager.enableButtons();
      onOffButton.setColorFilter(getResources().getColor(R.color.color_clock));
      // default url do not show, b/c they are not present in prefs at first
      String defaultKey = mPlayingStreamTag.replace("setting.key.stream", "");
      int index = Integer.parseInt(defaultKey) - 1;
      Toast.makeText(ClockActivity.this, "Playing " + mUrls.get(index), Toast.LENGTH_SHORT).show();
      if (getSupportActionBar() != null) {
        getSupportActionBar()
            .setTitle(getResources().getString(R.string.app_name) + ": " + mUrls.get(index));
      }
      // setAlarmPlaying(false);
    }
  }

  private class CustomOnErrorListener implements OnErrorListener {
    @Override
    public boolean onError(Exception e) {
      Toast.makeText(ClockActivity.this, "Error playing stream", Toast.LENGTH_SHORT).show();
      resetMediaPlayer();
      initMediaPlayer();
      buttonManager.resetButtons();
      onOffButton.setColorFilter(getResources().getColor(R.color.color_clock_red));
      if (getSupportActionBar() != null) {
        getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
      }
      if (alarmPlaying) {
        alarmManager.playDefaultAlarmOnStreamError();
      }
      return false;
    }
  }

  // --/////////////////////////////////////////////////////////////////////////
  // --- GESTURES ---
  // --/////////////////////////////////////////////////////////////////////////
  private boolean disallowSwipe = false;

  public void setDisallowSwipe(boolean disallowSwipe) {
    Timber.d("set disallow swipe: " + disallowSwipe);
    this.disallowSwipe = disallowSwipe;
  }

  private boolean isScaling = false;

  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {

    Timber.d("Motion disallowed: " + disallowSwipe);
    if (disallowSwipe) {
      return super.dispatchTouchEvent(event);
    }
    // Pass the touch event to the scale gesture detector first
    pinchGestureDetector.onTouchEvent(event);
    swipeGestureDetector.onTouchEvent(event);

    // If scaling, consume the event, otherwise let it propagate
    if (isScaling) {
      return true; // Consume the event
    } else {
      return super.dispatchTouchEvent(event); // Propagate the event
    }
  }

  private class SwipeGestureDetector extends GestureDetector.SimpleOnGestureListener {

    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
      if (!isScaling) {
        // Handle single tap
        Timber.d("Single tap detected");
        return true;
      }
      return super.onSingleTapUp(e);
    }

    @Override
    public boolean onDown(MotionEvent e) {
      return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
      float diffX = e2.getX() - e1.getX();
      float diffY = e2.getY() - e1.getY();
      if (Math.abs(diffX) > Math.abs(diffY)) {
        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
          if (diffX > 0) {
            onSwipeRight();
          } else {
            onSwipeLeft();
          }
          return true;
        }
      }
      return false;
    }
  }

  private void onSwipeRight() {
    profileManager.cycleThroughFonts(false);
  }

  private void onSwipeLeft() {
    profileManager.cycleThroughFonts(true);
  }

  private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    // private float scaleFactor = 1.0f;

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
      isScaling = true;
      return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
      isScaling = false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
      Timber.d("Scaling: detected " + detector.getScaleFactor());

      float scaleFactor = detector.getScaleFactor();
      scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 8.0f));
      Timber.d("Scale, factor: " + scaleFactor);
      if (scaleFactor < 1.0) {
        scaleFactor *= -1;
      }
      profileManager.changeSize(scaleFactor);
      return true;
    }
  }

  // --^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  // --- END GESTURES ---
  // --^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

  // --/////////////////////////////////////////////////////////////////////////
  // --- ANIMATION ---
  // --/////////////////////////////////////////////////////////////////////////
  @NonNull
  private static Animation getFadeInAnimation() {
    Animation fadeIn = new AlphaAnimation(0, 1);
    fadeIn.setStartOffset(0);
    fadeIn.setDuration(FADE_IN_DURATION_MILLIS);
    return fadeIn;
  }

  @NonNull
  private static Animation getFadeOutAnimation() {
    Animation fadeOut = new AlphaAnimation(1, 0);
    fadeOut.setStartOffset(0);
    fadeOut.setDuration(FADE_OUT_DURATION_MILLIS);
    return fadeOut;
  }
  // --^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  // --- END ANIMATIONS ---
  // --^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
}
