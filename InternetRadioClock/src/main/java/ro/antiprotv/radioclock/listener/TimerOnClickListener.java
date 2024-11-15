package ro.antiprotv.radioclock.listener;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.service.TimerService;

public class TimerOnClickListener implements View.OnClickListener {
  private final int setting;
  private final int button;
  private final TimerService timerService;
  private final String defaultTimer;
  private final SharedPreferences prefs;
  private final Context context;

  public TimerOnClickListener(
      int setting,
      int button,
      TimerService timerService,
      String defaultTimer,
      SharedPreferences prefs,
      Context context) {
    this.setting = setting;
    this.button = button;
    this.timerService = timerService;
    this.defaultTimer = defaultTimer;
    this.prefs = prefs;
    this.context = context;
  }

  @Override
  public void onClick(View v) {
    int seconds = Integer.parseInt(prefs.getString(context.getString(setting), defaultTimer));
    boolean useVisual =
        prefs.getBoolean(context.getString(R.string.setting_key_timer_visual_enabled), false);
    String visual = null;
    if (useVisual) {
      visual = prefs.getString(context.getString(R.string.setting_key_timer_visual), "RECTANGLE");
    }
    boolean animate =
        prefs.getBoolean(context.getString(R.string.setting_key_timer_animate), false);

    timerService.startInstantTimer(button, seconds, visual, animate);
  }
}
