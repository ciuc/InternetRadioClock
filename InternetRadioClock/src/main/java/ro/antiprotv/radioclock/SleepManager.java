package ro.antiprotv.radioclock;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ro.antiprotv.radioclock.ClockActivity;
import ro.antiprotv.radioclock.R;
import timber.log.Timber;

public class SleepManager {
    //initialize the sleep timers default list (pressing button will cycle through those)
    private final List<Integer> timers = new ArrayList<>(Arrays.asList(15,20,30));
    private int sleepTimerIndex;

    private ExecutorService sleepExecutorService = Executors.newSingleThreadExecutor();
    private ClockActivity context;
    private List<String> mUrls;


    protected SleepManager(ClockActivity context, List<String> mUrls) {
        this.context = context;
        this.mUrls = mUrls;
    }

    protected final Button.OnClickListener sleepOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            TextView sleepTimerText = context.findViewById(R.id.sleep_timer);
            TextView sleepTimerText8 = context.findViewById(R.id.sleep_timer8);
            ImageButton button = context.findViewById(R.id.sleep);
            ImageButton button8 = context.findViewById(R.id.sleep8);
            if (sleepTimerIndex == timers.size()) {
                resetSleepTimer();
                sleepExecutorService.shutdownNow();
            } else {
                //stop the timer thread
                sleepExecutorService.shutdownNow();
                button.setImageResource(R.drawable.sleep_timer_on_white_24dp);
                button8.setImageResource(R.drawable.sleep_timer_on_white_24dp);
                Integer timer = timers.get(sleepTimerIndex);
                sleepTimerText.setText(String.format(view.getResources().getString(R.string.text_sleep_timer),timer));
                sleepTimerText8.setText(String.format(view.getResources().getString(R.string.text_sleep_timer),timer));
                sleepTimerIndex++;
                //now start the thread
                SleepRunner sleepRunner = new SleepRunner(timer);
                sleepExecutorService = Executors.newSingleThreadExecutor();
                sleepExecutorService.execute(sleepRunner);
            }

        }
    };

    private void resetSleepTimer() {
        TextView sleepTimerText = context.findViewById(R.id.sleep_timer);
        TextView sleepTimerText8 = context.findViewById(R.id.sleep_timer8);
        ImageButton button = context.findViewById(R.id.sleep);
        ImageButton button8 = context.findViewById(R.id.sleep8);
        sleepTimerText.setText("");
        sleepTimerText8.setText("");
        button.setImageResource(R.drawable.sleep_timer_off_white_24dp);
        button8.setImageResource(R.drawable.sleep_timer_off_white_24dp);
        sleepTimerIndex = 0;
    }
    private class SleepRunner implements Runnable {
        int timer;
        SleepRunner(int timer) {
            Timber.d(ClockActivity.TAG_RADIOCLOCK, "Starting thread with timer: " + timer);
            this.timer = timer;
        }
        @Override
        public void run() {
            int seconds = timer * 60;
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    for (int i = seconds; i >= 0; i--) {
                        Timber.d(ClockActivity.TAG_RADIOCLOCK, "Thread sleep; seconds: " + i);
                        Thread.sleep(1000);
                    }
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Time's up", Toast.LENGTH_SHORT).show();
                            context.stopPlaying();
                            resetSleepTimer();
                        }
                    });
                    Thread.currentThread().interrupt();
                }
            } catch (InterruptedException e) {
                Timber.d(ClockActivity.TAG_RADIOCLOCK, "Sleep Thread interrupted ");
            }
        }
    }
    protected void hideUnhideSleepButtons() {
        TextView sleepTimerText = context.findViewById(R.id.sleep_timer);
        TextView sleepTimerText8 = context.findViewById(R.id.sleep_timer8);
        ImageButton button = context.findViewById(R.id.sleep);
        ImageButton button8 = context.findViewById(R.id.sleep8);
        for (String url : mUrls) {
            if (url.isEmpty()) {
                sleepTimerText.setVisibility(View.VISIBLE);
                button.setVisibility(View.VISIBLE);
                sleepTimerText8.setVisibility(View.GONE);
                button8.setVisibility(View.GONE);
                return;
            }
        }
        sleepTimerText8.setVisibility(View.VISIBLE);
        button8.setVisibility(View.VISIBLE);
        sleepTimerText.setVisibility(View.GONE);
        button.setVisibility(View.GONE);
    }

    public List<Integer> getTimers() {
        return timers;
    }
    public ExecutorService getSleepExecutorService() {
        return sleepExecutorService;
    }

}
