package ro.antiprotv.radioclock.listener;

import android.view.View;
import ro.antiprotv.radioclock.activity.ClockActivity;
import ro.antiprotv.radioclock.service.ButtonManager;
import ro.antiprotv.radioclock.service.MediaPlayerService;

public class OnOnOffClickListener implements View.OnClickListener {

  private final MediaPlayerService mediaPlayerService;
  private final ButtonManager buttonManager;
  private final ClockActivity clockActivity;

  public OnOnOffClickListener(
      MediaPlayerService mediaPlayerService,
      ButtonManager buttonManager,
      ClockActivity clockActivity) {
    this.mediaPlayerService = mediaPlayerService;
    this.buttonManager = buttonManager;
    this.clockActivity = clockActivity;
  }

  @Override
  public void onClick(View view) {
    if (mediaPlayerService.isPlaying()) {
      mediaPlayerService.stopPlaying();
    } else {
      mediaPlayerService.play(buttonManager.getButtonClicked().getId());
    }
    clockActivity.delayedHide(ClockActivity.AUTO_HIDE_DELAY_MILLIS);
  }
}
