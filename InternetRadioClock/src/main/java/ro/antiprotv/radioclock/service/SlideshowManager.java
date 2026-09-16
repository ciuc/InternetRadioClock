package ro.antiprotv.radioclock.service;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.devbrackets.android.exomedia.core.video.scale.ScaleType;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.flaviofaria.kenburnsview.KenBurnsView;
import com.flaviofaria.kenburnsview.RandomTransitionGenerator;
import com.flaviofaria.kenburnsview.Transition;
import com.flaviofaria.kenburnsview.TransitionGenerator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.activity.ClockActivity;
import ro.antiprotv.radioclock.service.profile.ProfileManager;
import ro.antiprotv.radioclock.view.AnniversaryOverlayView;
import timber.log.Timber;

public class SlideshowManager {
  private static SlideshowManager INSTANCE;

  /** "Play in full" is stored as a zero-length cap. */
  private static final long VIDEO_FULL_LENGTH = 0;

  /**
   * How long a video gets to open before we give up on it. A file the decoder cannot handle usually
   * reports an error, but not always - some just never prepare, and without this the slideshow
   * would sit on them for good.
   */
  private static final long VIDEO_PREPARE_TIMEOUT = 15000;

  private final SharedPreferences prefs;
  private final ClockActivity clockActivity;
  private final KenBurnsView kenBurnsView;
  private final ImageView slideshowSimpleView;
  private final VideoView videoView;
  private final AnniversaryOverlayView anniversaryOverlay;
  private final List<SlideshowItem> items = new ArrayList<>();
  private int currentSlideshowIndex = 0;

  /**
   * Whether a run is going. What tells reapplying a profile - which happens for reasons that have
   * nothing to do with the slideshow - apart from starting one.
   */
  private boolean running;

  /**
   * The randomize setting the order in {@link #items} was built under. A change to it has to
   * rebuild the order, which is otherwise kept for as long as the files behind it are the same.
   */
  private boolean orderRandomized;
  /** Consecutive files that could not be opened; reset by the first one that does. */
  private int unreadableStreak = 0;
  private final Handler slideShowhandler = new Handler();
  private final ButtonManager buttonManager;
  private final ProfileManager profileManager;
  private ImageView slideshowView;
  // Must match android:min/android:max/android:defaultValue on the slider in
  // res/xml/preferences_settings_slideshow.xml.
  private static final int MIN_IMAGE_DURATION_SECONDS = 10;
  private static final int MAX_IMAGE_DURATION_SECONDS = 120;
  private static final int DEFAULT_IMAGE_DURATION_SECONDS = 20;
  private int imageDuration;
  /** Listing a folder hits a content provider, which is too slow for the main thread. */
  private final ExecutorService folderLoader = Executors.newSingleThreadExecutor();
  /** Bumped by every load so a folder listing that arrives late can tell it is stale. */
  private int loadGeneration = 0;

  /**
   * Bumped every time the slideshow moves on, so that a callback belonging to a file we have
   * already left behind can tell and do nothing. See {@link #advance(int)}.
   */
  private int turn = 0;

  /** The turn that started the video now loaded, for the player's listeners to compare against. */
  private int videoOwnerTurn = -1;

  private boolean videoPrepared;
  /** How long to stay on a video, or {@link #VIDEO_FULL_LENGTH} to play it to the end. */
  private long videoLength;

  private boolean videoSoundEnabled;

  /** Whether to drop into a video at a random point rather than always at its opening. */
  private boolean videoRandomStart;

  /** Set when a video had to be started without knowing where in it to begin. */
  private boolean videoStartUnresolved;

  /**
   * Video lengths already learnt, keyed by URI. A slideshow comes round to the same files over and
   * over, so this turns the cost of finding out into a one-off per file.
   */
  private final Map<String, Long> videoDurations = new HashMap<>();

  private static final int MAX_REMEMBERED_LENGTHS = 512;
  /** Reading a video's length opens the file, which is far too slow for the main thread. */
  private final ExecutorService videoProbe = Executors.newSingleThreadExecutor();

  private final Random random = new Random();

  /**
   * Records what the slideshow does, for working out why files come round as often as they do. Null
   * in a release build; see {@link SlideshowDebugLog}.
   */
  @Nullable private final SlideshowDebugLog debugLog;

  /** Set while the app is in the background, so a video does not keep playing (or sounding). */
  private boolean backgrounded;

  /**
   * Set while the user is holding the slideshow on one file from the hand controls. Nothing but
   * those controls moves it on again: no turn is timed, and a video stays where it was stopped.
   */
  private boolean paused;

  /** How long one Ken Burns move lasts. */
  private static final long KEN_BURNS_DURATION = 10000;

  private EFFECT effect = EFFECT.NONE;

  /** The movement the chosen effect makes on its own, for pictures with no faces to follow. */
  private TransitionGenerator defaultTransitionGenerator;

  private boolean faceDetectionEnabled;

  /** Whether every file that comes up is greeted with fireworks and confetti. */
  private boolean anniversaryEnabled;

  /** How long that greeting lasts, in milliseconds. */
  private long anniversaryDuration;

  /** How much of a celebration to make of it. */
  private AnniversaryOverlayView.Density anniversaryDensity =
      AnniversaryOverlayView.Density.LIGHT;

  // Must match android:defaultValue on the slider in res/xml/preferences_settings_slideshow.xml.
  // The bounds are the overlay's own; see AnniversaryOverlayView.
  private static final int DEFAULT_ANNIVERSARY_SECONDS = 3;

  /** Works out where the faces are; see {@link SlideshowFaceFinder} for why that is worth doing. */
  private final SlideshowFaceFinder faceFinder;

  private enum EFFECT {
    NONE,
    PAN_ZOOM,
    ZOOM_OUT;

    public static EFFECT fromValue(String value) {
      try {
        return valueOf(value.toUpperCase());
      } catch (IllegalArgumentException e) {
        return NONE;
      }
    }
  }

  public static SlideshowManager getInstance(
      ClockActivity activity,
      SharedPreferences prefs,
      KenBurnsView kenBurnsView,
      ImageView slideshowSimpleView,
      VideoView videoView,
      AnniversaryOverlayView anniversaryOverlay,
      ButtonManager buttonManager,
      ProfileManager profileManager) {
    if (INSTANCE == null) {
      INSTANCE =
          new SlideshowManager(
              activity,
              prefs,
              kenBurnsView,
              slideshowSimpleView,
              videoView,
              anniversaryOverlay,
              buttonManager,
              profileManager);
    }
    return INSTANCE;
  }

  public static SlideshowManager getInstance() {
    if (INSTANCE == null) {
      Timber.e("SlideshowManager not initialized");
    }
    return INSTANCE;
  }

  private SlideshowManager(
      ClockActivity activity,
      SharedPreferences prefs,
      KenBurnsView kenBurnsView,
      ImageView slideshowSimpleView,
      VideoView videoView,
      AnniversaryOverlayView anniversaryOverlay,
      ButtonManager buttonManager,
      ProfileManager profileManager) {
    this.prefs = prefs;
    this.clockActivity = activity;
    this.kenBurnsView = kenBurnsView;
    this.slideshowSimpleView = slideshowSimpleView;
    this.videoView = videoView;
    this.anniversaryOverlay = anniversaryOverlay;
    this.buttonManager = buttonManager;
    this.profileManager = profileManager;
    this.faceFinder = new SlideshowFaceFinder(activity);
    this.debugLog = SlideshowDebugLog.create(activity);
    imageDuration = DEFAULT_IMAGE_DURATION_SECONDS * 1000;
    // Which of the two image views gets used is settled when the slideshow starts, but never leave
    // it unset: a resume can reach showTurn() before any start has run.
    this.slideshowView = slideshowSimpleView;
    setUpVideoView();
  }

  private void setUpVideoView() {
    // Without this the player installs its own play/pause/seek bar over the clock, and that bar
    // takes the taps that are meant to toggle the app's own controls.
    videoView.setVideoControls(null);
    // The radio is the point of this app; a background video must never take the audio off it.
    videoView.setHandleAudioFocus(false);
    videoView.setScaleType(ScaleType.CENTER_CROP);
    // The slideshow owns the player's lifetime, not the window it happens to be attached to.
    videoView.setReleaseOnDetachFromWindow(false);
    videoView.setOnPreparedListener(
        () -> {
          // A file we have already moved on from can still finish opening; it must not count as
          // this turn's file having opened, or the watchdog below would be disarmed for nothing.
          if (videoOwnerTurn != turn) {
            return;
          }
          videoPrepared = true;
          unreadableStreak = 0;
          applyVideoVolume();
          // Stepped onto while the slideshow is held: the file had to be started to get a picture
          // out of it, so stop it now that there is one.
          if (paused) {
            pauseVideo();
          }
          // The player's own figure is the authority, and it is free now that the file is open.
          // Files whose length could not be read up front get positioned properly from here on.
          rememberLength(currentUri(), videoView.getDuration());
          if (videoStartUnresolved) {
            resolveVideoStart();
          }
        });
    videoView.setOnCompletionListener(
        () -> {
          // The player tears itself down straight after this callback returns, and that teardown
          // stops whatever is loaded - including the next file, if we started it from in here. The
          // video that ended would then stay frozen on its last frame until its turn timed out.
          // Going round the handler lets the teardown happen first.
          final int endedTurn = videoOwnerTurn;
          slideShowhandler.post(() -> advance(endedTurn));
        });
    videoView.setOnErrorListener(
        e -> {
          onItemUnreadable(videoOwnerTurn, currentUri(), e);
          return true;
        });
    videoView.setVisibility(GONE);
  }

  /**
   * Shows the file at {@link #currentSlideshowIndex} and arranges for the slideshow to move on
   * afterwards.
   *
   * <p>Every call takes a fresh turn, which is what makes anything still pending from the previous
   * file harmless: a timer that has already been posted, a video still finishing, an image load
   * being cancelled. They all check their turn before acting, and only the current one counts.
   */
  private void showTurn() {
    if (items.isEmpty()
        || clockActivity.isDestroyed()
        || clockActivity.isFinishing()
        || backgrounded) {
      return;
    }
    final int myTurn = ++turn;
    slideShowhandler.removeCallbacksAndMessages(null);

    // Started here rather than in showImage(), so that the file being a video, or one that turns
    // out not to open at all, still gets its welcome.
    if (anniversaryEnabled) {
      anniversaryOverlay.start(anniversaryDuration, anniversaryDensity);
    }

    SlideshowItem item = items.get(currentSlideshowIndex);
    if (debugLog != null) {
      debugLog.show(currentSlideshowIndex, items.size(), item.uri, item.video, myTurn);
    }
    if (item.video) {
      showVideo(myTurn, item.uri);
    } else {
      showImage(myTurn, item.uri);
    }
  }

  /**
   * Moves on to the next file, unless the caller has been overtaken.
   *
   * @param fromTurn the turn the caller belongs to; anything but the current one is ignored
   */
  private void advance(int fromTurn) {
    if (fromTurn != turn || items.isEmpty()) {
      return;
    }
    if (++currentSlideshowIndex >= items.size()) {
      currentSlideshowIndex = 0;
      startNextPass("wrapped");
    }
    showTurn();
  }

  /**
   * Steps on to the next file by hand, from the slideshow controls.
   *
   * <p>Unlike {@link #advance(int)} this takes no turn to check against: a tap on the button is the
   * user asking for the next file whatever happens to be on screen at the time.
   */
  public void showNext() {
    step(1);
  }

  /** Steps back to the file before this one, from the slideshow controls. */
  public void showPrevious() {
    step(-1);
  }

  /**
   * @param direction 1 to go on, -1 to go back; either way the list wraps round
   */
  private void step(int direction) {
    if (items.isEmpty()) {
      return;
    }
    if (debugLog != null) {
      debugLog.event("MANUAL", "direction=" + direction);
    }
    currentSlideshowIndex += direction;
    if (currentSlideshowIndex >= items.size()) {
      currentSlideshowIndex = 0;
      startNextPass("stepped-past-end");
    } else if (currentSlideshowIndex < 0) {
      currentSlideshowIndex = items.size() - 1;
    }
    // showTurn() takes a fresh turn of its own, which is what makes everything left over from the
    // file being stepped away from - a pending tick, a video still opening - harmless.
    showTurn();
  }

  /**
   * Holds the slideshow on the file it is showing, or lets it go again.
   *
   * @return true if the slideshow is now held
   */
  public boolean togglePause() {
    if (paused) {
      letGo();
    } else {
      hold();
    }
    return paused;
  }

  public boolean isPaused() {
    return paused;
  }

  /** Keeps the current file up: no tick to end its turn, and a video stops where it is. */
  private void hold() {
    if (debugLog != null) {
      debugLog.event("HOLD", "index=" + currentSlideshowIndex);
    }
    paused = true;
    slideShowhandler.removeCallbacksAndMessages(null);
    if (videoView.getVisibility() == VISIBLE) {
      pauseVideo();
    } else if (slideshowView == kenBurnsView) {
      kenBurnsView.pause();
    }
  }

  /**
   * Lets the slideshow run again. The file it was held on gets a whole turn from here rather than
   * whatever was left of its own: what the user sees is a full look at the picture they stopped on,
   * not a moment of it before the slideshow jumps ahead.
   */
  private void letGo() {
    if (debugLog != null) {
      debugLog.event("LET_GO", "index=" + currentSlideshowIndex);
    }
    paused = false;
    if (items.isEmpty()) {
      return;
    }
    if (videoView.getVisibility() == VISIBLE) {
      try {
        videoView.start();
      } catch (Exception e) {
        Timber.e("Could not restart the slideshow video: %s", e.getMessage());
      }
      if (videoLength != VIDEO_FULL_LENGTH) {
        scheduleTurnEnd(turn, videoLength);
      }
    } else {
      if (slideshowView == kenBurnsView) {
        kenBurnsView.resume();
      }
      scheduleTurnEnd(turn, imageDuration);
    }
  }

  private void pauseVideo() {
    try {
      videoView.pause();
    } catch (Exception e) {
      Timber.e("Could not hold the slideshow video: %s", e.getMessage());
    }
  }

  /**
   * Ends the pass just finished and opens the next one, which means giving the files a fresh order.
   *
   * @param reason what brought the pass to an end, for the debug log
   */
  private void startNextPass(String reason) {
    if (debugLog != null) {
      debugLog.event("PASS_END", "reason=" + reason);
      debugLog.passEnded();
    }
    reshuffleForNextPass();
    if (debugLog != null) {
      debugLog.order(isRandomizeEnabled() ? "reshuffled" : "unchanged", items);
    }
  }

  /**
   * Gives the next pass through the files a fresh order. The list is shuffled once when it is
   * loaded and then walked in order, so without this every pass after the first would replay the
   * order the slideshow opened with.
   */
  private void reshuffleForNextPass() {
    if (items.size() < 2 || !isRandomizeEnabled()) {
      return;
    }
    SlideshowItem closedThePass = items.get(items.size() - 1);
    Collections.shuffle(items);
    // The file that ended the last pass must not also open this one. A reshuffle is not much use
    // if the seam between passes is where the same picture shows up twice in a row.
    if (items.get(0) == closedThePass) {
      Collections.swap(items, 0, 1 + random.nextInt(items.size() - 1));
    }
  }

  private void showImage(final int myTurn, Uri uri) {
    stopVideoPlayback();
    videoView.setVisibility(GONE);
    slideshowView.setVisibility(VISIBLE);
    // Posted before the load, not after: Glide can report a failure from inside into(), and that
    // failure starts the next file. Anything done here afterwards would belong to the wrong turn.
    scheduleTurnEnd(myTurn, imageDuration);
    if (!faceDetectionEnabled) {
      loadImage(uri, null);
      return;
    }
    // Where the faces are decides which part of the picture is shown, so it has to be known before
    // the picture goes up. Only the first showing of a file waits; the answer is kept after that.
    faceFinder.find(
        uri,
        viewportRatio(),
        region -> {
          if (myTurn == turn) {
            loadImage(uri, region);
          }
        });
  }

  /**
   * @param region where the faces are, or null when this picture is shown the plain way - because
   *     the setting is off, it already fits the screen, or nobody is in it
   */
  private void loadImage(Uri uri, @Nullable SlideshowFaceFinder.FaceRegion region) {
    applyTransitionGenerator(region);
    RequestBuilder<Drawable> request =
        Glide.with(clockActivity)
            .load(uri)
            .transition(DrawableTransitionOptions.withCrossFade(1500))
            .listener(imageLoadListener);
    if (region != null) {
      // Replaces the centre crop the view's scale type would otherwise get: same frame, moved off
      // the middle of the picture and onto the faces.
      request = request.transform(new SlideshowFaceCrop(region.focusX(), region.focusY()));
    }
    request.into(slideshowView);
    // Stepped onto while the slideshow is held: the arriving picture starts a Ken Burns move of its
    // own, and a held slideshow should not be moving.
    if (paused && slideshowView == kenBurnsView) {
      kenBurnsView.pause();
    }
  }

  /**
   * Points the Ken Burns movement at the faces, or puts the effect's own movement back for a
   * picture that has none.
   *
   * <p>Set before the picture is handed over rather than after: the drawable arriving is what
   * starts a transition, and it has to be the new generator that provides it.
   */
  private void applyTransitionGenerator(@Nullable SlideshowFaceFinder.FaceRegion region) {
    // Left alone entirely while the setting is off, so nothing about the effects changes for
    // everyone who does not use this.
    if (!faceDetectionEnabled || slideshowView != kenBurnsView) {
      return;
    }
    if (region == null) {
      if (defaultTransitionGenerator != null) {
        kenBurnsView.setTransitionGenerator(defaultTransitionGenerator);
      }
    } else if (effect == EFFECT.ZOOM_OUT) {
      kenBurnsView.setTransitionGenerator(
          new SlideshowFaceTransitions.ZoomOut(region.faceWithinCrop(), KEN_BURNS_DURATION));
    } else {
      kenBurnsView.setTransitionGenerator(
          new SlideshowFaceTransitions.PanZoom(region.faceWithinCrop(), KEN_BURNS_DURATION));
    }
  }

  /**
   * The shape of the area a picture is shown in, as width over height. The view has usually been
   * laid out by the time this is asked for, but a slideshow that starts with the activity has not
   * had one yet, and the screen it fills is the same answer.
   */
  private float viewportRatio() {
    int width = slideshowView.getWidth();
    int height = slideshowView.getHeight();
    if (width <= 0 || height <= 0) {
      DisplayMetrics metrics = clockActivity.getResources().getDisplayMetrics();
      width = metrics.widthPixels;
      height = metrics.heightPixels;
    }
    return height > 0 ? (float) width / height : 1f;
  }

  private void showVideo(final int myTurn, Uri uri) {
    kenBurnsView.setVisibility(GONE);
    slideshowSimpleView.setVisibility(GONE);
    videoView.setVisibility(VISIBLE);
    videoPrepared = false;

    if (videoLength != VIDEO_FULL_LENGTH) {
      scheduleTurnEnd(myTurn, videoLength);
    }
    // A video that never opens would otherwise hold the slideshow for as long as it is playing -
    // forever, when it is set to play in full.
    if (videoLength == VIDEO_FULL_LENGTH || videoLength > VIDEO_PREPARE_TIMEOUT) {
      slideShowhandler.postDelayed(
          () -> {
            if (myTurn == turn && !videoPrepared) {
              onItemUnreadable(myTurn, uri, null);
            }
          },
          VIDEO_PREPARE_TIMEOUT);
    }

    if (!wantsRandomStart()) {
      play(myTurn, uri, 0);
      return;
    }
    Long known = videoDurations.get(uri.toString());
    if (known != null) {
      play(myTurn, uri, known);
    } else {
      probeLengthThenPlay(myTurn, uri);
    }
  }

  /**
   * Hands the file to the player, starting it partway in when that is what the user asked for.
   *
   * <p>The seek goes in before playback rather than after it, which is the whole reason the length
   * is worked out in advance: seeking once the video is already on screen puts its opening frames
   * up for a moment first, and that shows. There is no timeline to seek within this early, so the
   * player simply holds the position and honours it when the file opens.
   *
   * @param knownDuration the video's length, or 0 when it could not be found out
   */
  private void play(int myTurn, Uri uri, long knownDuration) {
    long startAt = wantsRandomStart() ? randomStartWithin(knownDuration) : 0;
    // Nothing to go on: let it start at the beginning, and try again once the player reports a
    // length of its own. That costs the flash this method exists to avoid, but only on files whose
    // length cannot be read up front, and only until one has played once.
    videoStartUnresolved = wantsRandomStart() && knownDuration <= 0;

    // Same ordering as the image path: the player can report an error straight out of setMedia().
    videoView.reset();
    videoOwnerTurn = myTurn;
    videoView.setMedia(uri);
    if (startAt > 0) {
      Timber.d("Starting video at %s of %s ms", startAt, knownDuration);
      videoView.seekTo(startAt);
    }
    applyVideoVolume();
    videoView.start();
  }

  /**
   * Finds out how long a video is, off the main thread, and then starts it. Only the first showing
   * of a file pays for this; after that its length is remembered.
   */
  private void probeLengthThenPlay(final int myTurn, final Uri uri) {
    final Context appContext = clockActivity.getApplicationContext();
    videoProbe.execute(
        () -> {
          final long duration = readVideoDuration(appContext, uri);
          clockActivity.runOnUiThread(
              () -> {
                // The slideshow can have moved on, or gone away, while the file was being read.
                if (myTurn != turn) {
                  return;
                }
                rememberLength(uri, duration);
                play(myTurn, uri, duration);
              });
        });
  }

  /** The length of a video in milliseconds, or 0 when it cannot be read. Opens the file. */
  private static long readVideoDuration(Context context, Uri uri) {
    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    try {
      retriever.setDataSource(context, uri);
      String value = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
      return value != null ? Long.parseLong(value) : 0;
    } catch (Exception e) {
      // Not fatal, and not even unusual: plenty of files the player handles will not give up their
      // length this way. It only means this one cannot be positioned before it starts.
      Timber.d("Could not read the length of %s: %s", uri, e.getMessage());
      return 0;
    } finally {
      try {
        retriever.release();
      } catch (Exception e) {
        Timber.d("Could not release the metadata reader: %s", e.getMessage());
      }
    }
  }

  private void rememberLength(Uri uri, long duration) {
    if (uri == null || duration <= 0) {
      return;
    }
    // A folder can hold more videos than is worth keeping lengths for. Stopping at the limit costs
    // nothing: the ones already learnt are the ones being played.
    if (videoDurations.size() < MAX_REMEMBERED_LENGTHS
        || videoDurations.containsKey(uri.toString())) {
      videoDurations.put(uri.toString(), duration);
    }
  }

  /** The tick that ends a file's turn: whichever of this and the video finishing comes first. */
  private Runnable timeout(final int myTurn) {
    return () -> advance(myTurn);
  }

  /**
   * Arms the tick that ends a file's turn. Does nothing while the slideshow is held: a file the
   * user has stopped on stays up until they move on from it themselves.
   */
  private void scheduleTurnEnd(final int myTurn, long delay) {
    if (paused) {
      return;
    }
    slideShowhandler.postDelayed(timeout(myTurn), delay);
  }

  private Uri currentUri() {
    return items.isEmpty() ? null : items.get(currentSlideshowIndex).uri;
  }

  /**
   * The radio always wins. Sound on a background video is a nice extra, but talking over the
   * station the user actually chose to listen to is not, so a playing stream mutes the video
   * whatever the setting says.
   */
  private void applyVideoVolume() {
    boolean sound = videoSoundEnabled && !backgrounded && !clockActivity.isRadioPlaying();
    videoView.setVolume(sound ? 1f : 0f);
  }

  /**
   * Whether videos should be dropped into partway through. Playing one in full means showing it
   * from end to end, so there is nothing to choose a start point within.
   */
  private boolean wantsRandomStart() {
    return videoRandomStart && videoLength != VIDEO_FULL_LENGTH;
  }

  /**
   * A start point that still leaves a full turn's worth of video after it, so that a long film
   * shown twenty seconds at a time is not twenty seconds of its opening every time: with a twenty
   * second turn, a minute-long video starts somewhere in its first forty seconds. A video no
   * longer than the turn, or one of unknown length, starts at the beginning.
   */
  private long randomStartWithin(long duration) {
    if (duration <= videoLength) {
      return 0;
    }
    return (long) (random.nextDouble() * (duration - videoLength));
  }

  /**
   * Positions a video that had to be started blind, now that the player knows how long it is. Late
   * enough to be seen happening, which is why it is the fallback rather than the way this works.
   */
  private void resolveVideoStart() {
    videoStartUnresolved = false;
    long startAt = randomStartWithin(videoView.getDuration());
    if (startAt > 0) {
      Timber.d("Starting video at %s ms, worked out late", startAt);
      videoView.seekTo(startAt);
    }
  }

  /** Called when the radio starts or stops, so a video already playing mutes or unmutes at once. */
  public void onRadioPlayingChanged(boolean playing) {
    applyVideoVolume();
  }

  /**
   * Watches for files the app can no longer read. A saved URI outlives the access to it: some
   * providers hand out grants that stop working once the picking session is gone, and a picture the
   * user has since deleted or moved is gone for good. Either way the slideshow would just show a
   * blank screen, so skip ahead and, once nothing at all loads, say so instead.
   */
  private final RequestListener<Drawable> imageLoadListener =
      new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(
            @Nullable GlideException e,
            @Nullable Object model,
            @NonNull Target<Drawable> target,
            boolean isFirstResource) {
          onItemUnreadable(turn, model, e);
          return false;
        }

        @Override
        public boolean onResourceReady(
            @NonNull Drawable resource,
            @NonNull Object model,
            Target<Drawable> target,
            @NonNull DataSource dataSource,
            boolean isFirstResource) {
          unreadableStreak = 0;
          return false;
        }
      };

  /**
   * A file that could not be shown, from whichever of the two players hit it. Skips ahead rather
   * than sitting on a blank screen, and gives up once a whole pass has failed.
   *
   * @param fromTurn the turn the failure belongs to; a stale one is ignored
   */
  private void onItemUnreadable(int fromTurn, Object model, Object cause) {
    if (fromTurn != turn) {
      return;
    }
    Timber.e("Slideshow file could not be shown: %s (%s)", model, cause);
    if (debugLog != null) {
      debugLog.event("UNREADABLE", "streak=" + (unreadableStreak + 1) + "\tfile=" + model);
    }
    unreadableStreak++;
    if (unreadableStreak >= items.size()) {
      stopSlideshow();
      showDialogUnreadableSlideshowImages();
    } else {
      // Round the handler rather than straight on: Glide refuses to have a load started from
      // inside one of its own callbacks, and a picture that fails reports it from in there.
      final int failedTurn = fromTurn;
      slideShowhandler.post(() -> advance(failedTurn));
    }
  }

  private void showDialogEmptySlideshowImages() {
    showSlideshowDialog(
        savedFolder().isEmpty()
            ? R.string.slideshow_empty_dialog_msg
            : R.string.slideshow_folder_empty_dialog_msg);
  }

  private void showDialogUnreadableSlideshowImages() {
    showSlideshowDialog(R.string.slideshow_unreadable_dialog_msg);
  }

  private void showSlideshowDialog(int messageId) {
    if (clockActivity.isDestroyed() || clockActivity.isFinishing()) {
      return;
    }
    AlertDialog.Builder builder = new AlertDialog.Builder(clockActivity);
    builder
        .setMessage(clockActivity.getString(messageId))
        .setIcon(R.drawable.baseline_cancel_48)
        .setNeutralButton(R.string.dialog_button_ok, (dialog, id) -> dialog.cancel());

    AlertDialog dialog = builder.create();
    dialog.show();
  }

  /** @return the picked folder, or an empty string when files were picked one by one. */
  private String savedFolder() {
    String folder =
        prefs.getString(clockActivity.getString(R.string.setting_key_slideshow_folder), "");
    return folder != null ? folder : "";
  }

  /** What {@link #loadItems} reports back once the files are in. */
  private interface OnItemsLoaded {
    /**
     * @param keptOrder whether the files that came back were the ones already loaded, so that the
     *     order being walked and the place reached in it are still there
     */
    void ready(boolean keptOrder);
  }

  /**
   * Fills {@link #items}, then runs {@code onReady} on the main thread. Reading a folder means
   * querying a provider for every file in it, so that path goes through a background thread; the
   * saved-list path is only a JSON parse and stays inline.
   */
  private void loadItems(OnItemsLoaded onReady) {
    String folder = savedFolder();
    if (debugLog != null) {
      debugLog.event(
          "LOAD",
          "source="
              + (folder.isEmpty() ? "picked-files" : "folder")
              + "\tby="
              + SlideshowDebugLog.callers(8));
    }
    if (folder.isEmpty()) {
      onReady.ready(loadSavedItems());
      return;
    }
    final int generation = ++loadGeneration;
    Context appContext = clockActivity.getApplicationContext();
    Uri treeUri = Uri.parse(folder);
    folderLoader.execute(
        () -> {
          List<SlideshowItem> found = SlideshowFolder.listMedia(appContext, treeUri);
          clockActivity.runOnUiThread(
              () -> {
                // A newer load, or an activity on its way out, makes this listing stale.
                if (generation != loadGeneration
                    || clockActivity.isDestroyed()
                    || clockActivity.isFinishing()) {
                  if (debugLog != null) {
                    debugLog.event(
                        "LOAD_DROPPED",
                        "generation="
                            + generation
                            + "\tcurrent="
                            + loadGeneration
                            + "\tfound="
                            + found.size());
                  }
                  return;
                }
                onReady.ready(adopt(found));
              });
        });
  }

  /**
   * Takes on a freshly read list of files - unless it holds the very files already loaded, under
   * the randomize setting the current order was built under. Then the order and the place reached
   * in it are kept, because they are worth more than a fresh shuffle: they are what stops a file
   * coming round again before the rest of the folder has had its turn.
   *
   * @return true if the list already loaded was kept
   */
  private boolean adopt(List<SlideshowItem> found) {
    boolean randomize = isRandomizeEnabled();
    if (!items.isEmpty() && randomize == orderRandomized && sameFiles(found)) {
      return true;
    }
    items.clear();
    items.addAll(found);
    orderRandomized = randomize;
    if (randomize) {
      Collections.shuffle(items);
    }
    currentSlideshowIndex = 0;
    return false;
  }

  /** Whether a freshly read list holds exactly the files already loaded, in any order. */
  private boolean sameFiles(List<SlideshowItem> found) {
    if (found.size() != items.size()) {
      return false;
    }
    Set<String> loaded = new HashSet<>();
    for (SlideshowItem item : items) {
      loaded.add(item.uri.toString());
    }
    for (SlideshowItem item : found) {
      // A file the loaded list does not have, or one it has fewer copies of: not the same list.
      if (!loaded.remove(item.uri.toString())) {
        return false;
      }
    }
    return loaded.isEmpty();
  }

  private boolean loadSavedItems() {
    ++loadGeneration;
    List<SlideshowItem> found =
        SlideshowItems.parse(
            prefs.getString(clockActivity.getString(R.string.setting_key_slideshow_images), "[]"));
    Timber.d("Loaded %s saved slideshow files", found.size());
    return adopt(found);
  }

  private boolean isRandomizeEnabled() {
    return prefs.getBoolean(
        clockActivity.getString(R.string.setting_key_slideshow_randomize), false);
  }

  /** How many files the running slideshow has loaded; 0 until {@link #startSlideshow()} runs. */
  public int getImagesCount() {
    return items.size();
  }

  /**
   * How many files the user has saved, read straight from the preference. Unlike {@link
   * #getImagesCount()} this does not depend on the slideshow having been started, so it is what the
   * settings screen should show.
   */
  public static int getSavedImageCount(Context context, SharedPreferences prefs) {
    int[] counts = getSavedCounts(context, prefs);
    return counts[0] + counts[1];
  }

  /** The saved selection split as {@code {images, videos}}, for the settings summary. */
  public static int[] getSavedCounts(Context context, SharedPreferences prefs) {
    return SlideshowItems.counts(
        prefs.getString(context.getString(R.string.setting_key_slideshow_images), "[]"));
  }

  public boolean isSlideshowEnabled() {
    return profileManager.isSlideshowEnabled();
  }

  public void enableSlideshow() {
    // The saved list has to be read here: items is only filled by startSlideshow(), so right
    // after the user picks files for the first time it is still empty and the button would report
    // an empty slideshow. Leave the profile alone when there really is nothing to show, otherwise
    // the next tap on the button would be read as "disable".
    loadItems(
        keptOrder -> {
          if (items.isEmpty()) {
            showDialogEmptySlideshowImages();
            return;
          }
          profileManager.enableSlideshow();
          startLoadedSlideshow(keptOrder);
        });
  }

  public void disableSlideshow() {
    profileManager.disableSlideshow();
    stopSlideshow();
  }

  public void startSlideshow() {
    loadItems(this::startLoadedSlideshow);
  }

  /**
   * The part of starting that needs {@link #items} to be filled already.
   *
   * <p>Reapplying a profile comes through here for reasons that have nothing to do with the
   * slideshow: a colour picked, the weather bar turned on, a timer finishing, the night profile
   * coming round. A run already going is therefore left where it is - the settings are read again,
   * and the file on screen keeps its turn - rather than being started afresh. Starting afresh would
   * throw away the pass: the order is shuffled again and walked from the top, which brings files
   * back long before the rest of the folder has had its turn.
   *
   * @param keptOrder whether the load left the running order and the place in it alone
   */
  private void startLoadedSlideshow(boolean keptOrder) {
    if (items.isEmpty()) {
      showDialogEmptySlideshowImages();
      return;
    }
    applySettings();
    if (keptOrder && running) {
      // Nothing is touched here on purpose: not the turn, so the tick that ends the file on screen
      // still stands; not paused, so a slideshow being held by hand stays held. The settings just
      // read take hold from the next file on.
      if (debugLog != null) {
        debugLog.event(
            "CONTINUE",
            "index="
                + currentSlideshowIndex
                + "\tcount="
                + items.size()
                + "\tby="
                + SlideshowDebugLog.callers(8));
      }
      return;
    }
    ++turn;
    slideShowhandler.removeCallbacksAndMessages(null);
    unreadableStreak = 0;
    paused = false;
    running = true;
    if (debugLog != null) {
      debugLog.start(
          SlideshowDebugLog.callers(8),
          String.format(
              Locale.US,
              "count=%d\trandomize=%b\teffect=%s\timageSeconds=%d\tvideoSeconds=%d\tsource=%s\tindex=%d",
              items.size(),
              isRandomizeEnabled(),
              effect,
              imageDuration / 1000,
              videoLength / 1000,
              savedFolder().isEmpty() ? "picked-files" : "folder",
              currentSlideshowIndex));
      debugLog.order("start", items);
    }
    // backgrounded is deliberately not cleared here. A folder listing started before the app went
    // away can land after it, and starting a video then would play it - sound and all - behind
    // whatever the user is now looking at. onResume is what picks the slideshow back up.
    // No blanket setVisibility(VISIBLE) here: each file decides which view it needs, and showing
    // the image view up front would flash the last picture when the slideshow opens on a video.
    slideShowhandler.post(this::showTurn);
    buttonManager.lightButton(R.id.button_slideshow_enable);
    Toast.makeText(
            clockActivity,
            String.format("Slideshow {%s}, {%s}, {%s}", effect, items.size(), imageDuration),
            Toast.LENGTH_SHORT)
        .show();
  }

  /** Reads everything the run is steered by. Safe to call again on a slideshow already going. */
  private void applySettings() {
    slideshowView = slideshowSimpleView;
    effect =
        EFFECT.fromValue(
            prefs.getString(
                clockActivity.getString(R.string.setting_key_slideshow_effect), "NONE"));
    defaultTransitionGenerator = null;
    if (effect == EFFECT.PAN_ZOOM) {
      slideshowView = kenBurnsView;
      defaultTransitionGenerator = new RandomTransitionGenerator();
      // Set rather than left to the view's own: a previous run under another effect, or one that
      // was following faces, has already put a generator of its own on this view.
      kenBurnsView.setTransitionGenerator(defaultTransitionGenerator);
    } else if (effect == EFFECT.ZOOM_OUT) {
      slideshowView = kenBurnsView;
      defaultTransitionGenerator = new ZoomOnlyTransitionGenerator(KEN_BURNS_DURATION);
      kenBurnsView.setTransitionGenerator(defaultTransitionGenerator);
    }
    faceDetectionEnabled =
        prefs.getBoolean(
            clockActivity.getString(R.string.setting_key_slideshow_face_detection), false);
    anniversaryEnabled =
        prefs.getBoolean(
            clockActivity.getString(R.string.setting_key_slideshow_anniversary), false);
    anniversaryDuration =
        prefs.getInt(
                clockActivity.getString(R.string.setting_key_slideshow_anniversary_seconds),
                DEFAULT_ANNIVERSARY_SECONDS)
            * 1000L;
    anniversaryDensity =
        AnniversaryOverlayView.Density.fromValue(
            prefs.getString(
                clockActivity.getString(R.string.setting_key_slideshow_anniversary_density),
                "LIGHT"));
    if (!anniversaryEnabled) {
      // The setting can have been turned off while a run was going; nothing should be left over.
      anniversaryOverlay.stop();
    }
    imageDuration = readImageDuration(clockActivity, prefs);
    videoLength = readVideoLength();
    videoSoundEnabled =
        prefs.getBoolean(
            clockActivity.getString(R.string.setting_key_slideshow_video_sound), false);
    videoRandomStart =
        prefs.getBoolean(
            clockActivity.getString(R.string.setting_key_slideshow_video_random_start), false);
  }

  /**
   * Image duration in milliseconds. The setting is a slider in seconds; {@link
   * #migrateImageDuration} has already brought any older millisecond value across.
   */
  private static int readImageDuration(Context context, SharedPreferences prefs) {
    migrateImageDuration(context, prefs);
    return prefs.getInt(
            context.getString(R.string.setting_key_slideshow_image_stay_duration_seconds),
            DEFAULT_IMAGE_DURATION_SECONDS)
        * 1000;
  }

  /**
   * Carries the old free-text millisecond setting over to the seconds slider, once. Without this an
   * upgrading user silently drops back to the default, and the two keys hold different types, so
   * the old one cannot simply be reused.
   */
  public static void migrateImageDuration(Context context, SharedPreferences prefs) {
    String secondsKey =
        context.getString(R.string.setting_key_slideshow_image_stay_duration_seconds);
    if (prefs.contains(secondsKey)) {
      return;
    }
    String millisKey = context.getString(R.string.setting_key_slideshow_image_stay_duration);
    int seconds = DEFAULT_IMAGE_DURATION_SECONDS;
    try {
      String millis = prefs.getString(millisKey, null);
      if (millis != null) {
        seconds = Math.round(Integer.parseInt(millis.trim()) / 1000f);
      }
    } catch (Exception e) {
      // Free text: it could be anything. The default is a better answer than a crash.
      Timber.e("Unusable image duration setting: %s", e.getMessage());
    }
    seconds = Math.max(MIN_IMAGE_DURATION_SECONDS, Math.min(MAX_IMAGE_DURATION_SECONDS, seconds));
    prefs.edit().putInt(secondsKey, seconds).remove(millisKey).apply();
  }

  private long readVideoLength() {
    try {
      return Long.parseLong(
          prefs.getString(
              clockActivity.getString(R.string.setting_key_slideshow_video_length), "20000"));
    } catch (Exception e) {
      Timber.e("Unusable video length setting: %s", e.getMessage());
      return 20000;
    }
  }

  public void stopSlideshow() {
    if (debugLog != null) {
      debugLog.event("STOP", "by=" + SlideshowDebugLog.callers(8));
    }
    paused = false;
    running = false;
    ++turn;
    slideShowhandler.removeCallbacksAndMessages(null);
    stopVideoPlayback();
    videoView.setVisibility(GONE);
    kenBurnsView.setVisibility(GONE);
    slideshowSimpleView.setVisibility(GONE);
    anniversaryOverlay.stop();
    buttonManager.unlightButton(R.id.button_slideshow_enable);
  }

  private void stopVideoPlayback() {
    try {
      videoView.stop();
    } catch (Exception e) {
      // Stopping a player that was never given anything to play should not take the clock down.
      Timber.e("Could not stop the slideshow video: %s", e.getMessage());
    }
  }

  /**
   * Holds the slideshow while the app is not in front. Without this a video would carry on behind
   * the settings screen, or keep sounding after the user has left the app altogether.
   */
  public void pauseForBackground() {
    if (debugLog != null) {
      debugLog.event("BACKGROUND", "index=" + currentSlideshowIndex);
    }
    backgrounded = true;
    ++turn;
    slideShowhandler.removeCallbacksAndMessages(null);
    // Nothing should be drawing frame after frame behind whatever the user has moved on to.
    anniversaryOverlay.stop();
    videoView.setVolume(0f);
    try {
      videoView.pause();
    } catch (Exception e) {
      Timber.e("Could not pause the slideshow video: %s", e.getMessage());
    }
  }

  /** Picks the slideshow back up, restarting the file it was on. */
  public void resumeFromBackground() {
    if (!backgrounded) {
      return;
    }
    backgrounded = false;
    if (debugLog != null) {
      debugLog.event("FOREGROUND", "index=" + currentSlideshowIndex + "\tcount=" + items.size());
    }
    if (items.isEmpty() || !isSlideshowEnabled()) {
      return;
    }
    slideShowhandler.post(this::showTurn);
  }

  public static class ZoomOnlyTransitionGenerator implements TransitionGenerator {
    private final long duration;
    private final Random random = new Random();

    public ZoomOnlyTransitionGenerator(long durationMillis) {
      this.duration = durationMillis;
    }

    @Override
    public Transition generateNextTransition(RectF drawableBounds, RectF viewport) {
      float scaleStart = 1.2f;
      float scaleEnd = 1.0f;

      // Compute center of the viewport (no pan)
      float centerX = viewport.centerX();
      float centerY = viewport.centerY();

      // Calculate start and end rects
      float startWidth = viewport.width() / scaleStart;
      float startHeight = viewport.height() / scaleStart;
      RectF startRect =
          new RectF(
              centerX - startWidth / 2,
              centerY - startHeight / 2,
              centerX + startWidth / 2,
              centerY + startHeight / 2);

      RectF endRect = new RectF(viewport); // Full size, no pan

      return new Transition(startRect, endRect, duration, new AccelerateDecelerateInterpolator());
    }
  }

  public void destroy() {
    ++turn;
    slideShowhandler.removeCallbacksAndMessages(null);
    anniversaryOverlay.stop();
    videoView.setOnPreparedListener(null);
    videoView.setOnCompletionListener(null);
    videoView.setOnErrorListener(null);
    // Dropping the reference is not enough: the decoder and playback thread stay alive otherwise.
    try {
      videoView.release();
    } catch (Exception e) {
      Timber.e("Could not release the slideshow video player: %s", e.getMessage());
    }
    folderLoader.shutdownNow();
    videoProbe.shutdownNow();
    faceFinder.shutdown();
    INSTANCE = null;
  }
}
