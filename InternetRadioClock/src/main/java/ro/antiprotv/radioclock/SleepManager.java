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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class SleepManager {
    //initialize the sleep timers default list (pressing button will cycle through those)
    private final List<Integer> timers = new ArrayList<>(Arrays.asList(15, 20, 30));
    private int sleepTimerIndex;

    private final ExecutorService sleepExecutorService = Executors.newScheduledThreadPool(2);
    private final ClockActivity context;
    private final ImageButton button;
    private final TextView sleepTimerText;
    private ClockUpdater clockUpdater;
    private ScheduledFuture sleepFuture;
    private ScheduledFuture sleepCounterFuture;
    private final SleepCounterUpdater sleepCounterUpdater = new SleepCounterUpdater();
    private static final int MOVEMENT_SECCONDS = 5;//don';t know what the hell this does, must document

    final Button.OnClickListener sleepOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            if (sleepTimerIndex == timers.size()) {
                clockUpdater.interrupt();
                clockUpdater.setClockText(null,-1);
                resetSleepTimer();
                sleepFuture.cancel(true);
                sleepCounterUpdater.setStartFrom(0);
                clockUpdater.setClockText(view.getResources().getString(R.string.text_sleep_off), MOVEMENT_SECCONDS);
                scheduleClockSleepTimerReset();
            } else {
                clockUpdater.interrupt();
                clockUpdater.setClockText(null,-1);
                //stop the timer thread
                if (sleepFuture != null) {
                    sleepFuture.cancel(true);
                }
                button.setImageResource(R.drawable.sleep_timer_on_white_24dp);
                sleepTimerText.setVisibility(View.VISIBLE);
                long timer = timers.get(sleepTimerIndex);
                sleepTimerText.setText(String.format(view.getResources().getString(R.string.text_sleep_timer), timer));
                clockUpdater.setClockText(String.format(view.getResources().getString(R.string.text_sleep_timer_short), timer),MOVEMENT_SECCONDS);
                scheduleClockSleepTimerReset();
                sleepTimerIndex++;
                //now start the thread
                SleepRunner sleepRunner = new SleepRunner();
                sleepFuture = ((ScheduledExecutorService) sleepExecutorService).schedule(sleepRunner, timer, TimeUnit.MINUTES);

                sleepCounterUpdater.setStartFrom(timer);
                if (sleepCounterFuture == null || sleepCounterFuture.isDone()) {
                    sleepCounterFuture = ((ScheduledExecutorService) sleepExecutorService).scheduleAtFixedRate(sleepCounterUpdater, 1, 1, TimeUnit.MINUTES);
                }
            }
        }
    };

    private void scheduleClockSleepTimerReset() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(new Runnable() {
            @Override
            public void run() {
                clockUpdater.setClockText(null,-1);
            }
        }, MOVEMENT_SECCONDS, TimeUnit.SECONDS );
    }

    SleepManager(ClockActivity context, ClockUpdater clockUpdater) {
        this.context = context;
        this.clockUpdater = clockUpdater;
        sleepTimerText = context.findViewById(R.id.sleep_timer);
        button = context.findViewById(R.id.sleep);
    }

    private void resetSleepTimer() {
        sleepTimerText.setText("");
        sleepTimerText.setVisibility(View.GONE);
        button.setImageResource(R.drawable.sleep_timer_off_white_24dp);
        sleepTimerIndex = 0;
    }

    public List<Integer> getTimers() {
        return timers;
    }

    /**
     * Cleanup method - to execute when application exits
     */
    public void stop() {
        sleepExecutorService.shutdownNow();
    }

    /**
     * THis task is responsible with executing the "sleep"
     * IT will stop playing the music
     * and do some cleanup of the UI
     */
    private class SleepRunner implements Runnable {

        @Override
        public void run() {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Time's up", Toast.LENGTH_SHORT).show();
                        context.stopPlaying();
                        resetSleepTimer();
                    }
                });
        }
    }

    /**
     * This task is responssible for updating the sleep timer with the remaining time till sleep.
     * It is scheduled first time and it will run continously - the only thing the click logic does is to update the timer.
     * Due to the nature of the cycling through the predefined timers, I've found somewhat cumbersome to stop-reschedule the task (very unreliable).
     * Exiting the app will stop this future.
     */
    private class SleepCounterUpdater implements Runnable {
        long startFrom;

        public void setStartFrom(long start) {
            this.startFrom = start;
        }

        public long getTimer(){
            return startFrom;
        }

        @Override
        public void run() {
            if (startFrom > 0) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sleepTimerText.setText(String.format(context.getResources().getString(R.string.text_sleep_timer), startFrom--));
                    }
                });
            }
        }
    }

}
