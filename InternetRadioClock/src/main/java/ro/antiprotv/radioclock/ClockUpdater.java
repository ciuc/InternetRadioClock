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
public class ClockUpdater extends Thread{
    private Thread clockUpdater;

    public void setSemaphore(boolean semaphore) {
        this.semaphore = semaphore;
    }

    private boolean semaphore = true;

    public Handler getThreadHandler() {
        return threadHandler;
    }

    public void setThreadHandler(Handler threadHandler) {
        this.threadHandler = threadHandler;
    }

    //Threading stuff
    private Handler threadHandler = null;
    private TextView mContentView;

    public SimpleDateFormat getSdf() {
        return sdf;
    }

    public void setSdf(SimpleDateFormat sdf) {
        this.sdf = sdf;
    }

    SimpleDateFormat sdf;
    private static final int DO_NOT_MOVE_TEXT = 1;
    private static final int MOVE_TEXT = 2;

    public void setMoveText(boolean moveText) {
        this.moveText = moveText;
    }

    private boolean moveText = true;
    private static final List<Integer> GRAVITIES = Arrays.asList(Gravity.TOP, Gravity.BOTTOM, Gravity.LEFT, Gravity.RIGHT, Gravity.CENTER, Gravity.BOTTOM | Gravity.RIGHT);

    ClockUpdater(SimpleDateFormat sdf, TextView tv, boolean moveText){
        this.sdf = sdf;
        this.mContentView = tv;
        this.moveText = moveText;
    }
    //We create this ui handler to update the clock
    //We need this in order to not block the UI
    Handler uiHandler = new Handler() {
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
                                count ++;
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
                };
            };
            Looper.loop();

        }


}
