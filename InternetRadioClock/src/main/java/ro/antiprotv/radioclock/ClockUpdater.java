package ro.antiprotv.radioclock;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

/**
 * Thread to manage the clock (update the clock and move it)
 */
class ClockUpdater extends Thread {
    private static final int DO_NOT_MOVE_TEXT = 1;
    private static final int MOVE_TEXT = 2;
    private static final List<Integer> GRAVITIES = Arrays.asList(Gravity.TOP, Gravity.BOTTOM, Gravity.LEFT, Gravity.RIGHT, Gravity.CENTER, Gravity.BOTTOM | Gravity.RIGHT);
    private boolean semaphore = true;
    //Threading stuff
    private Handler threadHandler = null;
    private final TextView mContentView;
    private SimpleDateFormat sdf;
    //We create this ui handler to update the clock
    //We need this in order to not block the UI
    private final Handler uiHandler = new Handler() {
        int gravityIndex = 0;

        @Override
        public void handleMessage(Message msg) {
            mContentView.setText(sdf.format(new Date()));
            if (msg.what == MOVE_TEXT) {
                mContentView.setGravity(GRAVITIES.get(gravityIndex));
                gravityIndex++;
                if (gravityIndex == GRAVITIES.size()) {
                    gravityIndex = 0;
                }
            }
        }
    };
    private boolean moveText = true;

    ClockUpdater(TextView tv) {
        //this.sdf = sdf;
        this.mContentView = tv;
        //this.moveText = moveText;
    }

    public Handler getThreadHandler() {
        return threadHandler;
    }

    public void setThreadHandler(Handler threadHandler) {
        this.threadHandler = threadHandler;
    }

    public void run() {
        Looper.prepare();
        Timber.d("Starting thread");
        threadHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int count = 0;
                while (semaphore) {
                    {
                        try {
                            Thread.sleep(1000);
                            count++;
                        } catch (Exception e) {
                            Timber.e("Error: ", e.toString());
                        }
                    }
                    uiHandler.sendEmptyMessage(DO_NOT_MOVE_TEXT);
                    if (moveText && count > 300) {
                        count = 0;
                        uiHandler.sendEmptyMessage(MOVE_TEXT);
                    }
                }
            }

        };
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
}
