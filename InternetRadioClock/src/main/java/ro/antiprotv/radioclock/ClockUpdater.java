package ro.antiprotv.radioclock;

import static android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM;
import static android.widget.RelativeLayout.ALIGN_PARENT_LEFT;
import static android.widget.RelativeLayout.ALIGN_PARENT_RIGHT;
import static android.widget.RelativeLayout.ALIGN_PARENT_TOP;
import static android.widget.RelativeLayout.CENTER_IN_PARENT;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import ro.antiprotv.radioclock.service.TimerService;

/** Thread to manage the clock (update the clock and move it) */
public class ClockUpdater extends Thread {
  private static final int DO_NOT_MOVE_TEXT = 1;
  private static final int MOVE_TEXT = 2;
  private static final List<int[]> LAYOUT_ALIGNS =
      Arrays.asList(
          new int[] {ALIGN_PARENT_BOTTOM, ALIGN_PARENT_LEFT},
          new int[] {ALIGN_PARENT_TOP, ALIGN_PARENT_RIGHT},
          new int[] {CENTER_IN_PARENT},
          new int[] {ALIGN_PARENT_BOTTOM, ALIGN_PARENT_RIGHT},
          new int[] {ALIGN_PARENT_TOP, ALIGN_PARENT_LEFT},
          new int[] {CENTER_IN_PARENT});
  private final TextView clockView;
  private final TextView dateView;
  private boolean semaphore = true;
  // Threading stuff
  private Handler threadHandler = null;
  private SimpleDateFormat sdf;
  private int sleep = 1000;
  private String clockText;
  private TimerService timerService;
  private boolean isBlinking = false;

  // We create this ui handler to update the clock
  // We need this in order to not block the UI
  @SuppressLint("HandlerLeak")
  private final Handler uiHandler =
      new Handler(Looper.getMainLooper()) {
        int layoutListIndex = 0;
        int[] addedRules = new int[] {-1};

        @Override
        public void handleMessage(Message msg) {
          clockView.setText(getClockText());
          dateView.setText(new SimpleDateFormat("dd.MM.yyyy").format(new Date()));
          if (msg.what == MOVE_TEXT) {
            RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams) clockView.getLayoutParams();
            if (addedRules[0] != -1) {
              for (int addedRule : addedRules) {
                params.removeRule(addedRule);
              }
            }
            addedRules = LAYOUT_ALIGNS.get(layoutListIndex);
            for (int addedRule : addedRules) {
              params.addRule(addedRule);
            }
            if (layoutListIndex == 0 || layoutListIndex == 3) {
              RelativeLayout.LayoutParams paramsDate =
                      (RelativeLayout.LayoutParams) dateView.getLayoutParams();
              paramsDate.removeRule(RelativeLayout.BELOW);
              paramsDate.addRule(RelativeLayout.ABOVE, clockView.getId());
            } else {
              RelativeLayout.LayoutParams paramsDate =
                      (RelativeLayout.LayoutParams) dateView.getLayoutParams();
              paramsDate.removeRule(RelativeLayout.ABOVE);
              paramsDate.addRule(RelativeLayout.BELOW, clockView.getId());
            }
            clockView.setLayoutParams(params);
            layoutListIndex++;
            if (layoutListIndex == LAYOUT_ALIGNS.size()) {
              layoutListIndex = 0;
            }
          }
        }
      };

  private boolean moveText = true;

  public ClockUpdater(TextView clock, TextView dateView) {
    this.clockView = clock;
    this.dateView = dateView;
  }

  /**
   * Sets some text instead of the clock for this number of seconds
   *
   * @param text the text to set
   * @param seconds the number of seconds to keep the text on screen
   */
  public void setClockText(String text, int seconds) {
    clockText = text;
    if (seconds == -1) {
      sleep = 1000;
    } else {
      sleep = seconds * 1000;
    }
  }

  private String getClockText() {
    String timerText = timerService.getTimerText();
    if (timerText != null) {
      if (timerText.equals("00:00") || timerText.startsWith("|| ")) {
        //timerText = timerText.replace("|| ", "");
        startBlinkingAnimation(clockView);
      } else {
        stopBlinkingAnimation(clockView);
      }
      dateView.setVisibility(TextView.GONE);
      return timerText;
    }
    stopBlinkingAnimation(clockView);
    if (clockText == null) {
      return sdf.format(new Date());
    }
    return clockText;
  }

  public Handler getThreadHandler() {
    return threadHandler;
  }

  public void setThreadHandler(Handler threadHandler) {
    this.threadHandler = threadHandler;
  }

  public void run() {
    Looper.prepare();
    threadHandler = new MyHandler(this);
    Looper.loop();
  }

  public void setSemaphore(boolean semaphore) {
    this.semaphore = semaphore;
  }

  public void setSdf(SimpleDateFormat sdf) {
    this.sdf = sdf;
  }

  public void setMoveText(boolean moveText) {
    this.moveText = moveText;
  }

  private static class MyHandler extends Handler {
    private final ClockUpdater clockUpdater;

    public MyHandler(ClockUpdater clockUpdater) {
      this.clockUpdater = clockUpdater;
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
      int count = 0;
      while (clockUpdater.semaphore) {
        try {
          Thread.sleep(clockUpdater.sleep);
          count++;
        } catch (InterruptedException ignored) {
        }
        clockUpdater.uiHandler.sendEmptyMessage(DO_NOT_MOVE_TEXT);
        if (clockUpdater.moveText && count > 300) {
          count = 0;
          clockUpdater.uiHandler.sendEmptyMessage(MOVE_TEXT);
        }
      }
    }
  }

  public void startBlinkingAnimation(TextView textView) {
    if (isBlinking) {
      return;
    }
    // Create an AlphaAnimation instance for blinking effect
    AlphaAnimation blinkAnimation = new AlphaAnimation(0.0f, 1.0f); // From invisible to visible
    blinkAnimation.setDuration(200); // Duration of one blink (500ms)
    blinkAnimation.setStartOffset(10); // Delay before starting the next blink
    blinkAnimation.setRepeatMode(Animation.REVERSE); // Reverse the animation at the end
    blinkAnimation.setRepeatCount(Animation.INFINITE); // Repeat indefinitely

    // Start the animation on the TextView
    textView.startAnimation(blinkAnimation);
    isBlinking = true;
  }

  public void setTimerService(TimerService timerService) {
    this.timerService = timerService;
  }

  public void stopBlinkingAnimation(TextView textView) {
    if (isBlinking) {
      textView.clearAnimation();
      textView.setAlpha(1.0f);
    }
    isBlinking = false;
  }
}
