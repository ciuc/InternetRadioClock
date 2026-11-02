package ro.antiprotv.radioclock.service;

import android.util.TypedValue;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import ro.antiprotv.radioclock.ClockUpdater;
import ro.antiprotv.radioclock.service.profile.Profile;

/**
 * Shows the song the station says it plays (ICY metadata, by convention "Artist - Title") along an
 * edge of the clock screen.
 *
 * <p>It sits along the bottom, unless the clock has wandered down there to save the screen, in which
 * case it moves to the top so the two never overlap. It is shown only while the radio plays, the
 * station has told us the song, and the current profile asks for it.
 */
public class NowPlayingManager implements ClockUpdater.ClockPositionListener {

  private final TextView view;
  /** What the station says it plays right now; null until it does, and after the radio stops. */
  private String streamTitle;
  private boolean playing;
  private boolean enabled;
  private boolean clockAlongBottom;

  public NowPlayingManager(TextView view) {
    this.view = view;
  }

  /** Every radio start and stop; a station that stops takes its song with it. */
  public void setPlaying(boolean playing) {
    this.playing = playing;
    if (!playing) {
      streamTitle = null;
    }
    sync();
  }

  /** The song, as the station sent it; null or blank clears it. */
  public void setStreamTitle(String streamTitle) {
    this.streamTitle =
        streamTitle == null || streamTitle.trim().isEmpty() ? null : streamTitle.trim();
    sync();
  }

  /**
   * Shown or not is part of the profile, like the weather. The colour comes from the clock so it
   * reads as part of it, and it scales with the clock so it stays a footnote to the time.
   */
  public void applyProfile(Profile profile) {
    if (profile == null) {
      return;
    }
    enabled = profile.isShowNowPlaying();
    view.setTextColor(profile.getColor());
    view.setTextSize(
        TypedValue.COMPLEX_UNIT_SP, Math.max(14f, Math.min(28f, profile.getSize() / 7f)));
    sync();
  }

  /**
   * {@link ClockUpdater} flips the date above the clock exactly when it pins the clock along the
   * bottom edge, so that flag tells us where the clock is.
   */
  @Override
  public void onClockPositionChanged(boolean clockAtTop, boolean dateAboveClock) {
    clockAlongBottom = dateAboveClock;
    applyPosition();
  }

  private void applyPosition() {
    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();
    params.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
    params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
    params.addRule(
        clockAlongBottom ? RelativeLayout.ALIGN_PARENT_TOP : RelativeLayout.ALIGN_PARENT_BOTTOM);
    view.setLayoutParams(params);
  }

  private void sync() {
    boolean show = enabled && playing && streamTitle != null;
    if (show && !streamTitle.contentEquals(view.getText())) {
      view.setText(streamTitle);
    }
    view.setVisibility(show ? View.VISIBLE : View.GONE);
    // A song too long for the screen scrolls, which a single line only does while selected.
    view.setSelected(show);
  }
}
