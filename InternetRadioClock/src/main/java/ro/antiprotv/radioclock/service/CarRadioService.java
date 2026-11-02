package ro.antiprotv.radioclock.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.AudioAttributesCompat;
import androidx.media.AudioFocusRequestCompat;
import androidx.media.AudioManagerCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media3.common.Format;
import androidx.media3.common.Metadata;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DecoderReuseEvaluation;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.extractor.metadata.icy.IcyHeaders;
import androidx.media3.extractor.metadata.icy.IcyInfo;
import androidx.preference.PreferenceManager;
import com.devbrackets.android.exomedia.AudioPlayer;
import java.util.ArrayList;
import java.util.List;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.activity.ClockActivity;
import timber.log.Timber;

/**
 * Plays the radio memories from the car display (Android Auto).
 *
 * <p>Android Auto draws the whole UI itself: this service only hands it the stations configured on
 * the 8 memory buttons (shown as a grid of buttons) and reacts to play / stop / next / previous.
 *
 * <p>It owns its own player: the clock's player lives with {@link ClockActivity}, which is usually
 * not running while the phone sits in the car. Starting a station here stops the clock's radio, and
 * the other way around, so the two never play over each other.
 */
public class CarRadioService extends MediaBrowserServiceCompat {

  private static final String ROOT_ID = "root";
  private static final String CHANNEL_ID = "radio_playback";
  private static final int NOTIFICATION_ID = 1002;
  private static final String ACTION_STOP = "ro.antiprotv.radioclock.action.STOP_CAR_PLAY";
  private static final float DUCK_VOLUME = 0.2f;
  /** The clock's station buttons are tagged with this prefix plus the memory number. */
  private static final String STREAM_TAG_PREFIX = "setting.key.stream";

  // Android Auto's browse content style hints: show the stations as a grid of big buttons
  private static final String CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED";
  private static final String CONTENT_STYLE_PLAYABLE_HINT =
      "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT";
  private static final int CONTENT_STYLE_GRID_ITEM = 2;

  private static final int[] URL_KEYS = {
    R.string.setting_key_stream1,
    R.string.setting_key_stream2,
    R.string.setting_key_stream3,
    R.string.setting_key_stream4,
    R.string.setting_key_stream5,
    R.string.setting_key_stream6,
    R.string.setting_key_stream7,
    R.string.setting_key_stream8
  };
  // memories 6-8 have no default station
  private static final int[] DEFAULT_URLS = {
    R.string.setting_default_stream1,
    R.string.setting_default_stream2,
    R.string.setting_default_stream3,
    R.string.setting_default_stream4,
    R.string.setting_default_stream5,
    0,
    0,
    0
  };
  private static final int[] LABEL_KEYS = {
    R.string.setting_key_label1,
    R.string.setting_key_label2,
    R.string.setting_key_label3,
    R.string.setting_key_label4,
    R.string.setting_key_label5,
    R.string.setting_key_label6,
    R.string.setting_key_label7,
    R.string.setting_key_label8
  };
  private static final int[] DEFAULT_LABELS = {
    R.string.button_name_stream1,
    R.string.button_name_stream2,
    R.string.button_name_stream3,
    R.string.button_name_stream4,
    R.string.button_name_stream5,
    R.string.button_name_stream6,
    R.string.button_name_stream7,
    R.string.button_name_stream8
  };

  /** The running instance, so the clock can stop the car radio when it starts its own. */
  private static CarRadioService instance;

  private SharedPreferences prefs;
  private MediaSessionCompat session;
  private AudioPlayer player;
  private AudioManager audioManager;
  private AudioFocusRequestCompat focusRequest;
  /** Memory being played (1-8), or the one to resume; 0 when nothing was picked yet. */
  private int currentMemory;
  /** The button picture of the station playing, shown next to the song. */
  private Bitmap currentIcon;
  /** What the station says it plays right now (usually "Artist - Title"); null until it does. */
  private String streamTitle;
  /** The station's own name, as it sends it; null when it does not. */
  private String streamStationName;
  /** Set when a phone call or another app took the audio away for a moment. */
  private boolean resumeOnFocusGain;

  /** Stops the car radio, if it is playing. */
  public static void stopIfPlaying() {
    if (instance != null) {
      instance.stopPlaying();
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();
    instance = this;
    prefs = PreferenceManager.getDefaultSharedPreferences(this);
    audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    focusRequest =
        new AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                new AudioAttributesCompat.Builder()
                    .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                    .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
                    .build())
            .setOnAudioFocusChangeListener(this::onAudioFocusChange)
            .build();

    player = new AudioPlayer(getApplicationContext());
    player.setOnPreparedListener(this::onPrepared);
    player.setOnErrorListener(this::onError);
    player.setMetadataListener(this::onMetadata);
    player.setAnalyticsListener(new StationNameListener());

    session = new MediaSessionCompat(this, "CarRadioService");
    session.setCallback(new SessionCallback());
    session.setSessionActivity(
        PendingIntent.getActivity(
            this,
            0,
            new Intent(this, ClockActivity.class)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    setSessionToken(session.getSessionToken());
    setPlaybackState(PlaybackStateCompat.STATE_NONE, null);
    session.setActive(true);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null && ACTION_STOP.equals(intent.getAction())) {
      stopPlaying();
      return START_NOT_STICKY;
    }
    // started by ourselves when playback starts: promote to foreground so the stream survives
    // while the phone is locked in the car
    Notification notification = buildNotification();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
          NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
    } else {
      startForeground(NOTIFICATION_ID, notification);
    }
    return START_NOT_STICKY;
  }

  @Override
  public void onDestroy() {
    instance = null;
    stopPlaying();
    player.release();
    session.setActive(false);
    session.release();
    super.onDestroy();
  }

  @Override
  public BrowserRoot onGetRoot(
      @NonNull String clientPackageName, int clientUid, Bundle rootHints) {
    Bundle extras = new Bundle();
    extras.putBoolean(CONTENT_STYLE_SUPPORTED, true);
    extras.putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_GRID_ITEM);
    return new BrowserRoot(ROOT_ID, extras);
  }

  @Override
  public void onLoadChildren(
      @NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
    List<MediaBrowserCompat.MediaItem> items = new ArrayList<>();
    if (ROOT_ID.equals(parentId)) {
      for (int memory = 1; memory <= URL_KEYS.length; memory++) {
        if (!getUrl(memory).isEmpty()) {
          items.add(
              new MediaBrowserCompat.MediaItem(
                  describe(memory), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
        }
      }
    }
    result.sendResult(items);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Stations
  ////////////////////////////////////////////////////////////////////////////////////////////////
  private String getUrl(int memory) {
    Resources res = getResources();
    int defaultUrl = DEFAULT_URLS[memory - 1];
    String url =
        prefs.getString(
            res.getString(URL_KEYS[memory - 1]), defaultUrl == 0 ? "" : res.getString(defaultUrl));
    return url == null ? "" : url.trim();
  }

  private String getLabel(int memory) {
    Resources res = getResources();
    return prefs.getString(
        res.getString(LABEL_KEYS[memory - 1]), res.getString(DEFAULT_LABELS[memory - 1]));
  }

  private List<Integer> configuredMemories() {
    List<Integer> memories = new ArrayList<>();
    for (int memory = 1; memory <= URL_KEYS.length; memory++) {
      if (!getUrl(memory).isEmpty()) {
        memories.add(memory);
      }
    }
    return memories;
  }

  private MediaDescriptionCompat describe(int memory) {
    return new MediaDescriptionCompat.Builder()
        .setMediaId(String.valueOf(memory))
        .setTitle(getLabel(memory))
        .setIconBitmap(buttonIcon(memory))
        .build();
  }

  /** A round button with the memory number, looking like the clock's own station buttons. */
  private Bitmap buttonIcon(int memory) {
    int size = 192;
    Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setColor(0xFF202020);
    canvas.drawRoundRect(new RectF(0, 0, size, size), size / 5f, size / 5f, paint);
    paint.setColor(ContextCompat.getColor(this, R.color.color_clock));
    paint.setTextSize(size * 0.55f);
    paint.setTextAlign(Paint.Align.CENTER);
    float baseline = size / 2f - (paint.descent() + paint.ascent()) / 2f;
    canvas.drawText(String.valueOf(memory), size / 2f, baseline, paint);
    return bitmap;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Playback
  ////////////////////////////////////////////////////////////////////////////////////////////////
  private void play(int memory) {
    String url = getUrl(memory);
    if (url.isEmpty()) {
      return;
    }
    if (AudioManagerCompat.requestAudioFocus(audioManager, focusRequest)
        != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
      Timber.w("Car radio did not get the audio focus");
      return;
    }
    // the clock and the car must not play two stations at once
    BackgroundPlaybackService.requestClockStop();

    currentMemory = memory;
    resumeOnFocusGain = false;
    prefs
        .edit()
        .putString(ClockActivity.LAST_PLAYED, STREAM_TAG_PREFIX + memory)
        .apply();
    currentIcon = buttonIcon(memory);
    streamTitle = null;
    streamStationName = null;
    showNowPlaying();
    setPlaybackState(PlaybackStateCompat.STATE_BUFFERING, null);
    player.setVolume(1f);
    player.setMedia(Uri.parse(url));
    startForegroundPlayback();
  }

  private void onPrepared() {
    player.start();
    setPlaybackState(PlaybackStateCompat.STATE_PLAYING, null);
  }

  /**
   * Most stations send the song in the stream itself (ICY metadata, "StreamTitle='Artist -
   * Title'"); the player picks it up for us whenever the song changes.
   */
  @OptIn(markerClass = UnstableApi.class)
  private void onMetadata(Metadata metadata) {
    for (int i = 0; i < metadata.length(); i++) {
      Metadata.Entry entry = metadata.get(i);
      if (entry instanceof IcyInfo) {
        String title = ((IcyInfo) entry).title;
        if (title != null && !title.trim().isEmpty()) {
          streamTitle = title.trim();
          showNowPlaying();
        }
      }
    }
  }

  /**
   * The station also sends its own name when the stream starts (the "icy-name" header); the player
   * attaches it to the audio format.
   */
  @OptIn(markerClass = UnstableApi.class)
  private void onAudioFormat(Format format) {
    Metadata metadata = format.metadata;
    if (metadata == null) {
      return;
    }
    for (int i = 0; i < metadata.length(); i++) {
      Metadata.Entry entry = metadata.get(i);
      if (entry instanceof IcyHeaders) {
        String name = ((IcyHeaders) entry).name;
        if (name != null && !name.trim().isEmpty()) {
          streamStationName = name.trim();
          showNowPlaying();
        }
      }
    }
  }

  /**
   * The name shown for the station: the button label, unless the user never named the button (it
   * is then just its number) and the station told us its own name.
   */
  private String stationName() {
    String label = getLabel(currentMemory);
    boolean labelIsJustTheNumber =
        label.equals(getString(DEFAULT_LABELS[currentMemory - 1]));
    return labelIsJustTheNumber && streamStationName != null ? streamStationName : label;
  }

  /**
   * Fills the car's now playing screen: the station on the top line and, underneath, the song when
   * the station tells us what it plays (by convention "Artist - Title").
   */
  private void showNowPlaying() {
    session.setMetadata(
        new MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(currentMemory))
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, stationName())
            .putString(
                MediaMetadataCompat.METADATA_KEY_ARTIST,
                streamTitle != null ? streamTitle : getString(R.string.app_name))
            .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, currentIcon)
            .build());
  }

  private boolean onError(Exception e) {
    Timber.w(e, "Car radio could not play the stream");
    player.reset();
    abandonAudioFocus();
    stopForegroundPlayback();
    setPlaybackState(PlaybackStateCompat.STATE_ERROR, getString(R.string.error_playing_stream));
    return false;
  }

  /** Live radio cannot be paused, so pausing stops the stream; play starts it again. */
  private void stopPlaying() {
    resumeOnFocusGain = false;
    player.reset();
    abandonAudioFocus();
    stopForegroundPlayback();
    setPlaybackState(PlaybackStateCompat.STATE_STOPPED, null);
  }

  private void skip(int direction) {
    List<Integer> memories = configuredMemories();
    if (memories.isEmpty()) {
      return;
    }
    int position = memories.indexOf(currentMemory);
    int next =
        position < 0 ? 0 : (position + direction + memories.size()) % memories.size();
    play(memories.get(next));
  }

  private int lastPlayedMemory() {
    if (currentMemory > 0) {
      return currentMemory;
    }
    String tag = prefs.getString(ClockActivity.LAST_PLAYED, STREAM_TAG_PREFIX + 1);
    try {
      return Integer.parseInt(tag.replace(STREAM_TAG_PREFIX, ""));
    } catch (NumberFormatException e) {
      return 1;
    }
  }

  private void onAudioFocusChange(int focusChange) {
    switch (focusChange) {
      case AudioManager.AUDIOFOCUS_GAIN:
        player.setVolume(1f);
        if (resumeOnFocusGain) {
          play(currentMemory);
        }
        break;
      case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
        // navigation prompts: keep playing, just quieter
        player.setVolume(DUCK_VOLUME);
        break;
      case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
        // a phone call: stop the stream and pick it up again afterwards
        if (player.isPlaying()) {
          player.reset();
          resumeOnFocusGain = true;
          setPlaybackState(PlaybackStateCompat.STATE_PAUSED, null);
        }
        break;
      case AudioManager.AUDIOFOCUS_LOSS:
      default:
        stopPlaying();
        break;
    }
  }

  private void abandonAudioFocus() {
    AudioManagerCompat.abandonAudioFocusRequest(audioManager, focusRequest);
  }

  private void setPlaybackState(int state, String error) {
    PlaybackStateCompat.Builder builder =
        new PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY
                    | PlaybackStateCompat.ACTION_PAUSE
                    | PlaybackStateCompat.ACTION_STOP
                    | PlaybackStateCompat.ACTION_PLAY_PAUSE
                    | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                    | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
            .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f);
    if (error != null) {
      builder.setErrorMessage(PlaybackStateCompat.ERROR_CODE_APP_ERROR, error);
    }
    session.setPlaybackState(builder.build());
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Foreground service
  ////////////////////////////////////////////////////////////////////////////////////////////////
  private void startForegroundPlayback() {
    try {
      ContextCompat.startForegroundService(this, new Intent(this, CarRadioService.class));
    } catch (Exception e) {
      // the stream still plays as long as Android Auto stays bound to us
      Timber.w(e, "Could not start the car radio as a foreground service");
    }
  }

  private void stopForegroundPlayback() {
    stopForeground(true);
    stopSelf();
  }

  private Notification buildNotification() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel channel =
          new NotificationChannel(
              CHANNEL_ID,
              getString(R.string.notification_channel_playback_name),
              NotificationManager.IMPORTANCE_LOW);
      channel.setDescription(getString(R.string.notification_channel_playback_description));
      channel.setShowBadge(false);
      getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
    PendingIntent stopIntent =
        PendingIntent.getService(
            this,
            2,
            new Intent(this, CarRadioService.class).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    String station = currentMemory > 0 ? getLabel(currentMemory) : null;
    return new NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.baseline_play_circle_outline_24)
        .setContentTitle(getString(R.string.notification_playback_title))
        .setContentText(station)
        .setContentIntent(session.getController().getSessionActivity())
        .addAction(
            R.drawable.ic_close_white_24dp, getString(R.string.notification_playback_stop), stopIntent)
        .setStyle(
            new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(session.getSessionToken())
                .setShowActionsInCompactView(0))
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setShowWhen(false)
        .setOngoing(true)
        .build();
  }

  @OptIn(markerClass = UnstableApi.class)
  private class StationNameListener implements AnalyticsListener {
    @Override
    public void onAudioInputFormatChanged(
        @NonNull EventTime eventTime,
        @NonNull Format format,
        DecoderReuseEvaluation decoderReuseEvaluation) {
      onAudioFormat(format);
    }
  }

  private class SessionCallback extends MediaSessionCompat.Callback {
    @Override
    public void onPlayFromMediaId(String mediaId, Bundle extras) {
      try {
        play(Integer.parseInt(mediaId));
      } catch (NumberFormatException e) {
        Timber.w("Unknown car media id %s", mediaId);
      }
    }

    @Override
    public void onPlay() {
      play(lastPlayedMemory());
    }

    @Override
    public void onPause() {
      stopPlaying();
    }

    @Override
    public void onStop() {
      stopPlaying();
    }

    @Override
    public void onSkipToNext() {
      skip(1);
    }

    @Override
    public void onSkipToPrevious() {
      skip(-1);
    }
  }
}
