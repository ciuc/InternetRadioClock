package ro.antiprotv.radioclock;

import android.media.MediaPlayer;
import android.util.Log;

/**
 * Created by ciuc on 7/17/16.
 */
public class RcMediaPlayer extends MediaPlayer {

    private STATUS status = STATUS.STOPPED;

    public enum STATUS {
        PLAYING, STOPPED, PREPARING

    }

    public void setStatus(STATUS status) {
        Log.d("Status update", getStatus().toString()+" > "+status.toString());
        this.status = status;
    }

    public STATUS getStatus() {
        return status;
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        setStatus(STATUS.PREPARING);
        Log.d("RcMediaPlayer", "Start prepareAsync");
        super.prepareAsync();
        Log.d("RcMediaPlayer", "End prepareAsync");
    }

    public boolean isPreparing() {
        if (getStatus() == STATUS.PREPARING) {
            return true;
        }
        return false;
    }

    @Override
    public void stop() throws IllegalStateException {
        setStatus(STATUS.STOPPED);
        super.stop();
    }

    @Override
    public void reset() {
        setStatus(STATUS.STOPPED);
        super.reset();
    }
}
