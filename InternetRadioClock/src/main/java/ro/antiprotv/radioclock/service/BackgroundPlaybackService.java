package ro.antiprotv.radioclock.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.activity.ClockActivity;
import timber.log.Timber;

/**
 * Keeps the radio playing while the app is not in the foreground.
 *
 * <p>The player itself stays in {@link MediaPlayerService}; this service exists only so the system
 * keeps our process running (and so the user gets an ongoing notification to stop the radio from)
 * once the clock is no longer visible. Without it, Android freezes the cached process and the
 * stream dies.
 *
 * <p>It is started when playback starts with the background play setting on, and stopped as soon as
 * playback stops, the setting is turned off, or the app is closed.
 */
public class BackgroundPlaybackService extends Service {

  /** Notified when the user presses "stop" on the notification, or closes the app. */
  public interface StopListener {
    void onStopRequested();
  }

  private static final String CHANNEL_ID = "radio_playback";
  private static final int NOTIFICATION_ID = 1001;
  private static final String ACTION_START = "ro.antiprotv.radioclock.action.START_BACKGROUND_PLAY";
  private static final String ACTION_STOP = "ro.antiprotv.radioclock.action.STOP_BACKGROUND_PLAY";
  private static final String EXTRA_STATION = "station";

  private static StopListener stopListener;

  /**
   * The player is owned by the activity, so it is the one that knows how to stop playing; it
   * registers itself here.
   */
  public static void setStopListener(StopListener listener) {
    stopListener = listener;
  }

  /** Starts (or updates) the ongoing notification for the given station. */
  public static void start(Context context, String station) {
    Intent intent =
        new Intent(context, BackgroundPlaybackService.class)
            .setAction(ACTION_START)
            .putExtra(EXTRA_STATION, station);
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent);
      } else {
        context.startService(intent);
      }
    } catch (Exception e) {
      // Android refuses foreground service starts made from the background; playback then simply
      // stays foreground-only, as it was before this setting existed.
      Timber.w(e, "Could not start the background playback service");
    }
  }

  public static void stop(Context context) {
    context.stopService(new Intent(context, BackgroundPlaybackService.class));
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null && ACTION_STOP.equals(intent.getAction())) {
      requestStop();
      stopSelf();
      return START_NOT_STICKY;
    }

    String station = intent == null ? null : intent.getStringExtra(EXTRA_STATION);
    createNotificationChannel();
    Notification notification = buildNotification(station);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
          NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
    } else {
      startForeground(NOTIFICATION_ID, notification);
    }
    return START_NOT_STICKY;
  }

  /**
   * The user swiped the app away. The player lives with the activity, so let it go instead of
   * leaving an orphan stream behind.
   */
  @Override
  public void onTaskRemoved(Intent rootIntent) {
    requestStop();
    stopSelf();
    super.onTaskRemoved(rootIntent);
  }

  @Override
  public void onDestroy() {
    stopForeground(true);
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private void requestStop() {
    if (stopListener != null) {
      stopListener.onStopRequested();
    }
  }

  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return;
    }
    NotificationChannel channel =
        new NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_playback_name),
            NotificationManager.IMPORTANCE_LOW);
    channel.setDescription(getString(R.string.notification_channel_playback_description));
    channel.setShowBadge(false);
    getSystemService(NotificationManager.class).createNotificationChannel(channel);
  }

  private Notification buildNotification(String station) {
    int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

    // ACTION_MAIN/CATEGORY_LAUNCHER brings the existing task back to front instead of stacking a
    // second clock on top of it.
    Intent openClock =
        new Intent(this, ClockActivity.class)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openClock, pendingIntentFlags);

    Intent stopIntent =
        new Intent(this, BackgroundPlaybackService.class).setAction(ACTION_STOP);
    PendingIntent stopPendingIntent =
        PendingIntent.getService(this, 1, stopIntent, pendingIntentFlags);

    return new NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.baseline_play_circle_outline_24)
        .setContentTitle(getString(R.string.notification_playback_title))
        .setContentText(station)
        .setContentIntent(contentIntent)
        .addAction(
            R.drawable.ic_close_white_24dp,
            getString(R.string.notification_playback_stop),
            stopPendingIntent)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setShowWhen(false)
        .setOngoing(true)
        .build();
  }
}
