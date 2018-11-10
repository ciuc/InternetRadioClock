package ro.antiprotv.radioclock;

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

import timber.log.Timber;

class SleepManager {
    //initialize the sleep timers default list (pressing button will cycle through those)
    private final List<Integer> timers = new ArrayList<>(Arrays.asList(15, 20, 30));
    private int sleepTimerIndex;

    private ExecutorService sleepExecutorService = Executors.newSingleThreadExecutor();
    private final ClockActivity context;
    private ImageButton button;
    private TextView sleepTimerText;
    final Button.OnClickListener sleepOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            sleepTimerText = context.findViewById(R.id.sleep_timer);
            button = context.findViewById(R.id.sleep);
            if (sleepTimerIndex == timers.size()) {
                resetSleepTimer();
                sleepExecutorService.shutdownNow();
            } else {
                //stop the timer thread
                sleepExecutorService.shutdownNow();
                button.setImageResource(R.drawable.sleep_timer_on_white_24dp);
                Integer timer = timers.get(sleepTimerIndex);
                sleepTimerText.setText(String.format(view.getResources().getString(R.string.text_sleep_timer), timer));
                sleepTimerIndex++;
                //now start the thread
                SleepRunner sleepRunner = new SleepRunner(timer);
                sleepExecutorService = Executors.newSingleThreadExecutor();
                sleepExecutorService.execute(sleepRunner);
            }

        }
    };

    SleepManager(ClockActivity context) {
        this.context = context;
    }

    private void resetSleepTimer() {
        sleepTimerText = context.findViewById(R.id.sleep_timer);
        button = context.findViewById(R.id.sleep);
        sleepTimerText.setText("");
        button.setImageResource(R.drawable.sleep_timer_off_white_24dp);
        sleepTimerIndex = 0;
    }

    public List<Integer> getTimers() {
        return timers;
    }

    public ExecutorService getSleepExecutorService() {
        return sleepExecutorService;
    }

    private class SleepRunner implements Runnable {
        final int timer;

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

}
