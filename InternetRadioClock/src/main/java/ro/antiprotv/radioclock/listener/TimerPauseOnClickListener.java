package ro.antiprotv.radioclock.listener;

import android.view.View;
import ro.antiprotv.radioclock.service.TimerService;

public class TimerPauseOnClickListener implements View.OnClickListener {
  private final TimerService timerService;

  public TimerPauseOnClickListener(TimerService timerService) {
    this.timerService = timerService;
  }

  @Override
  public void onClick(View v) {
    timerService.toggleTimerPause();
  }
}
