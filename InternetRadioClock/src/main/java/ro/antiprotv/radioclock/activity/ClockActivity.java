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
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import ro.antiprotv.radioclock.BuildConfig;
import ro.antiprotv.radioclock.ClockUpdater;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.SleepManager;
import ro.antiprotv.radioclock.TipsDialog;
import ro.antiprotv.radioclock.service.BatteryService;
import ro.antiprotv.radioclock.service.BrightnessManager;
import ro.antiprotv.radioclock.service.ButtonManager;
import ro.antiprotv.radioclock.service.MediaPlayerService;
import ro.antiprotv.radioclock.service.RadioAlarmManager;
import ro.antiprotv.radioclock.service.RingtoneService;
import ro.antiprotv.radioclock.service.SettingsManager;
import ro.antiprotv.radioclock.service.TimerService;
import ro.antiprotv.radioclock.service.VolumeManager;
import ro.antiprotv.radioclock.service.profile.ProfileManager;
import timber.log.Timber;

/** Main Activity. Just displays the clock and buttons */
public class ClockActivity extends AppCompatActivity {

  public static final String PREF_NIGHT_MODE = "NIGHT_MODE";
  public static final String LAST_PLAYED = "LAST_PLAYED";
  public static final String IS_PLAYING = "IS_PLAYING";
  public static final String PLAYING_STREAM_NO = "PLAYING_STREAM_NO";
  public static final String USER_ALARM_PERMISSION_NOT_ALLOWED_PREF = "USER_PERM_NOK";
  public static final int FADE_OUT_DURATION_MILLIS = 400;
  public static final int FADE_IN_DURATION_MILLIS = 200;
  private static final String TAG_STATE = "ClockActivity | State: %s";

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

  private final Handler mHideHandler = new Handler();

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
  // some initializations
  private ClockUpdater clockUpdater;
  private SharedPreferences prefs;
  // private AudioPlayer mMediaPlayer;
  private ButtonManager buttonManager;
  private SleepManager sleepManager;
  private VolumeManager volumeManager;
  private RadioAlarmManager alarmManager;
  private BatteryService batteryService;
  private ProfileManager profileManager;
  // LISTENERS
  private final Button.OnClickListener nightModeOnClickListener =
      new View.OnClickListener() {

        @Override
        public void onClick(final View view) {
          profileManager.toggleNightMode();
        }
      };
  private MediaPlayerService mediaPlayerService;
  private GestureDetector swipeGestureDetector;
  private ScaleGestureDetector pinchGestureDetector;
  // stuff for the hide/unhide ui elements
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
  private View mControlsView;
  // stuff to remember (like state stuff)
  // remember the playing stream number and tag
  // they will have to be reset when stopping
  private int mPlayingStreamNo;
  private String mPlayingStreamTag;

  // ^^^^^
  // Alarm stuff
  // remember last picked hour
  private int h = 0;
  private int m = 0;
  private boolean alarmPlaying;
  private boolean alarmSnoozing;
  private boolean isPlaying;
  private final Button.OnClickListener playOnClickListener =
      new View.OnClickListener() {

        @Override
        public void onClick(final View view) {
          buttonManager.setButtonClicked((Button) view);
          buttonManager.disableButtons();
          mediaPlayerService.initMediaPlayer();
          // if (mMediaPlayer != null) {
          if (mediaPlayerService.isPlaying()) {
            if (mPlayingStreamNo == buttonManager.getButtonClicked().getId()) {
              mediaPlayerService.stopPlaying();
            } else {
              mediaPlayerService.play(buttonManager.getButtonClicked().getId());
            }
          } else {
            mediaPlayerService.play(buttonManager.getButtonClicked().getId());
          }
          // } else {
          //  initMediaPlayer();
          //  buttonManager.enableButtons();
          // }
          // I want to hide the snooze button on any interaction with the radio buttons
          if (alarmSnoozing) {
            alarmManager.cancelSnooze();
          }
        }
      };
  private RingtoneService ringtoneService;
  private TimerService timerService;
  // the saved state; used for keep playing if it the case
  private Bundle savedInstanceState;
  private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
  private Toolbar toolbar = null;
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
  private final Runnable mHideRunnable = this::hide;

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

  // --/////////////////////////////////////////////////////////////////////////
  // --- GESTURES ---
  // --/////////////////////////////////////////////////////////////////////////
  private boolean disallowSwipe = false;
  private boolean isScaling = false;

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

  ///////////////////////////////////////////////////////////////////////////
  // State methods
  ///////////////////////////////////////////////////////////////////////////
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if (BuildConfig.DEBUG && Timber.treeCount() == 0) {
      Timber.plant(new Timber.DebugTree());
    }
    this.savedInstanceState = savedInstanceState;
    Timber.d(TAG_STATE, "onCreate");
    super.onCreate(savedInstanceState);
    StringBuilder sb = new StringBuilder(new Date(System.currentTimeMillis()).toString());
    if (savedInstanceState != null) {
      sb.append("is playing ")
          .append(savedInstanceState.getBoolean(IS_PLAYING, false))
          .append("\n");
      sb.append("mem ").append(savedInstanceState.getInt(PLAYING_STREAM_NO, 1)).append("\n");
      Timber.d(sb.toString());
    } else {
      sb.append("savedInstanceState is null\n");
    }

    Timber.d(sb.toString());
    /* AlertDialog.Builder builder =
        new AlertDialog.Builder(this)
            .setTitle("Stack trace")
            .setPositiveButton("OK", (dialog, id) -> dialog.cancel())
            .setMessage(sb.toString());
    AlertDialog stacktracedialog = builder.create();
    stacktracedialog.show();*/

    /*    Executors.newSingleThreadExecutor().execute(new Runnable() {
          @Override
          public void run() {
            EmailSender.sendEmail("cristi.ciuc@gmail.com",
                    "100LuftBallons1!", "cristi.ciuc@gmail.com", "Test", "Test");
          }
        });
    */
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

    buttonManager =
        new ButtonManager(
            getApplicationContext(),
            mControlsView,
            prefs,
            mDelayHideTouchListener,
            playOnClickListener);
    buttonManager.initializeUrls();
    buttonManager.initializeButtons();

    displayDialogsOnOpen();

    // Thread - clock
    // Thread for communicating with the ui handler
    // We start it here , and we sendMessage to the threadHandler in onStart (we might have a race
    // and get threadHandler null if we try it here)
    clockUpdater = new ClockUpdater(mContentView);
    clockUpdater.start();

    initializeAlarmFunction();

    profileManager = new ProfileManager(this, prefs, clockUpdater);

    this.batteryService = new BatteryService(this, profileManager);

    profileManager.clearTask();
    profileManager.applyProfile();
    BrightnessManager brightnessManager =
        new BrightnessManager(this, mControlsView, profileManager);
    profileManager.setBrightnessManager(brightnessManager);

    // preferences and profile
    ImageButton nightModeButton = findViewById(R.id.night_mode_button);
    nightModeButton.setOnClickListener(nightModeOnClickListener);
    nightModeButton.setOnTouchListener(mDelayHideTouchListener);

    // Volume
    volumeManager = new VolumeManager(this, mControlsView);

    // Initialize the player
    initializeSleepFunction();

    initializeTimerFunction(clockUpdater);

    mediaPlayerService =
        new MediaPlayerService(this, alarmManager, buttonManager, volumeManager, prefs);
    preferenceChangeListener =
        new SettingsManager(
            this, buttonManager, sleepManager, batteryService, mediaPlayerService, timerService);
    alarmManager.setMediaPlayerService(mediaPlayerService);
    // Play at start?
    // button clicked is either last played, or the first; will also set
    if (prefs.getBoolean(getResources().getString(R.string.setting_key_playAtStart), false)) {
      mediaPlayerService.play(buttonManager.getButtonClicked().getId());
    }

    final ImageButton helpButton = findViewById(R.id.main_help_button);
    helpButton.setOnClickListener(new HelpOnClickListener(this));

    ImageButton onOffButton = findViewById(R.id.on_off_button);
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

    if (savedInstanceState != null) {
      if (savedInstanceState.getBoolean(IS_PLAYING, false)) {
        int buttonClicked = savedInstanceState.getInt(PLAYING_STREAM_NO, 1);

        buttonManager.setButtonClicked(buttonClicked);
        mediaPlayerService.play(buttonClicked);
      }
    }
  }

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
  protected void onPause() {
    Timber.d(TAG_STATE, "onPause");
    if (savedInstanceState != null) {
      Timber.d(savedInstanceState.toString());
      onSaveInstanceState(savedInstanceState);
    }
    super.onPause();
  }

  @Override
  protected void onStop() {
    Timber.d(TAG_STATE, "onStop");
    super.onStop();
    clockUpdater.setSemaphore(false);
    clockUpdater.getThreadHandler().removeMessages(0);
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putBoolean(IS_PLAYING, isPlaying);
    outState.putInt(PLAYING_STREAM_NO, mPlayingStreamNo);
    Timber.d(TAG_STATE, "onSaveInstanceState");
    Timber.d(outState.toString());
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onDestroy() {
    Timber.d(TAG_STATE, "onDestroy");
    mediaPlayerService.resetMediaPlayer();
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

  // ^^^^^^^^^^^^^^^^^^^^^^^^^^^
  // ALARM END
  // ^^^^^^^^^^^^^^^^^^^^^^^^^^^

  @Override
  protected void onRestart() {
    Timber.d(TAG_STATE, "onRestart");
    super.onRestart();
    mediaPlayerService.onRestart();
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
        .setMessage(getString(R.string.alarm_permission_message_info))
        .setTitle(getString(R.string.use_alarms))
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

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // MENU
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

  private void initializeSleepFunction() {
    // sleep timer
    sleepManager = new SleepManager(this, clockUpdater, mediaPlayerService);
    sleepManager.init();

    // sleep buttons
    ImageButton sleep = findViewById(R.id.sleep);
    sleep.setOnClickListener(sleepManager.sleepButtonOnClickListener);
    sleep.setOnTouchListener(mDelayHideTouchListener);
  }

  private void initializeTimerFunction(ClockUpdater clockUpdater) {
    ringtoneService = new RingtoneService(this);
    timerService = new TimerService(ringtoneService, buttonManager);
    clockUpdater.setTimerService(timerService);
    ImageButton timerLong = findViewById(R.id.timer_long);
    timerLong.setOnClickListener(
        v -> {
          timerService.setTimerSeconds(
              Integer.parseInt(
                  prefs.getString(getString(R.string.setting_key_timer_long_seconds), "180")));
          timerService.setTimer(R.id.timer_long);
        });
    ImageButton timerShort = findViewById(R.id.timer_short);
    timerShort.setOnClickListener(
        v -> {
          timerService.setTimerSeconds(
              Integer.parseInt(
                  prefs.getString(getString(R.string.setting_key_timer_short_seconds), "10")));
          timerService.setTimer(R.id.timer_short);
        });
  }

  private void displayDialogsOnOpen() {
    final String currentDialog = "NINTH_TIME";
    if (prefs.getBoolean(currentDialog, true)) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder
          .setMessage(getString(R.string.display_dialog_text_swipe_to_change))
          .setTitle(R.string.new_stuff)
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
          .setMessage(R.string.dialog_amoled_warning)
          .setTitle(R.string.amoled_warning)
          .setIcon(R.drawable.ic_warning_black_24dp)
          .setPositiveButton(
              getString(R.string.dialog_button_ok_amoled),
              (dialog, id) -> prefs.edit().putBoolean("AMOLED_WARN", false).apply());
      AlertDialog dialog = builder.create();

      dialog.show();
    }
  }

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

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // GETTERS AND SETTERS
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  public void setAlarmPlaying(boolean alarmPlaying) {
    this.alarmPlaying = alarmPlaying;
  }

  public boolean isAlarmPlaying() {
    return alarmPlaying;
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

  // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  // GETTERS AND SETTERS END
  // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^

  public String getmPlayingStreamTag() {
    return mPlayingStreamTag;
  }

  public void setmPlayingStreamTag(String mPlayingStreamTag) {
    this.mPlayingStreamTag = mPlayingStreamTag;
  }

  public void setmPlayingStreamNo(int mPlayingStreamNo) {
    this.mPlayingStreamNo = mPlayingStreamNo;
  }

  public void setPlaying(boolean playing) {
    isPlaying = playing;
  }

  public void setDisallowSwipe(boolean disallowSwipe) {
    // Timber.d("set disallow swipe: " + disallowSwipe);
    this.disallowSwipe = disallowSwipe;
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {

    // Timber.d("Motion disallowed: " + disallowSwipe);
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

  private void onSwipeRight() {
    profileManager.cycleThroughFonts(false);
  }

  private void onSwipeLeft() {
    profileManager.cycleThroughFonts(true);
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
      volumeManager.cancelProgressiveVolumeTask();
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

  // --^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  // --- END GESTURES ---
  // --^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

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

  private class HelpOnClickListener implements View.OnClickListener {
    private final AppCompatActivity activity;

    public HelpOnClickListener(AppCompatActivity activity) {
      this.activity = activity;
    }

    @Override
    public void onClick(View view) {
      android.app.AlertDialog tipsDialog = new TipsDialog(view.getContext());
      tipsDialog.show();
      WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
      lp.copyFrom(tipsDialog.getWindow().getAttributes());
      lp.width = WindowManager.LayoutParams.MATCH_PARENT;
      lp.height = WindowManager.LayoutParams.MATCH_PARENT;
      tipsDialog.getWindow().setAttributes(lp);

      // ShowCaseService service = new ShowCaseService(activity);
      // service.showCase();
      /*      TutoShowcase.from(activity)
      .on(mContentView)
      .displaySwipableLeft()
      .delayed(250)
      .animated(true)
      //.show()
      .on(mContentView)
      .displaySwipableRight()
      .delayed(500)
      .show();*/
    }
  }

  // --^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  // --- END ANIMATIONS ---
  // --^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

  private class OnOnOffClickListener implements View.OnClickListener {
    @Override
    public void onClick(View view) {
      if (mediaPlayerService.isPlaying()) {
        mediaPlayerService.stopPlaying();
      } else {
        mediaPlayerService.play(buttonManager.getButtonClicked().getId());
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

  private class SwipeGestureDetector extends GestureDetector.SimpleOnGestureListener {

    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
      if (!isScaling) {
        // Handle single tap
        // Timber.d("Single tap detected");
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
      // Timber.d("Scaling: detected " + detector.getScaleFactor());

      float scaleFactor = detector.getScaleFactor();
      scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 8.0f));
      // Timber.d("Scale, factor: " + scaleFactor);
      if (scaleFactor < 1.0) {
        scaleFactor *= -1;
      }
      profileManager.changeSize(scaleFactor);
      return true;
    }
  }
}
