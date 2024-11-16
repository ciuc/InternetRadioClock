package ro.antiprotv.radioclock.listener;

import android.view.View;
import ro.antiprotv.radioclock.service.TimerService;

public class TimerAddTimeOnClickListener implements View.OnClickListener {
  private final TimerService timerService;
  private final int seconds;

  public TimerAddTimeOnClickListener(TimerService timerService, int seconds) {
    this.timerService = timerService;
    this.seconds = seconds;
  }

  @Override
  public void onClick(View v) {
    timerService.addTime(seconds);
  }
}
