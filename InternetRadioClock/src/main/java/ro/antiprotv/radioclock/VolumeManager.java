package ro.antiprotv.radioclock;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.devbrackets.android.exomedia.AudioPlayer;

import java.text.DecimalFormat;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

//import timber.log.Timber;

/**
 * Manages the volume of the player
 * It does not manage the overall device volume set by hardware buttons
 */
public class VolumeManager {
    private final View view;
    private ImageButton volumeUpButton;
    private ImageButton volumeDownButton;
    private AudioPlayer mediaPlayer;
    private final Context ctx;
    private TextView volumeText;
    private final DecimalFormat fmt = new DecimalFormat("#%");

    VolumeManager(Context ctx, View view, AudioPlayer mediaPlayer) {
        this.ctx = ctx;
        this.view = view;
        this.mediaPlayer = mediaPlayer;
        volumeUpButton = view.findViewById(R.id.volumeup_button);
        volumeDownButton = view.findViewById(R.id.volumedown_button);
        this.volumeText = view.findViewById(R.id.volume);
        volumeUpButton.setOnTouchListener(new VolumeUpOnClickListener());
        volumeDownButton.setOnTouchListener(new VolumeDownOnClickListener());
        setVolume(1);
    }

    protected void volumeUp(float pct) {
        float volume = mediaPlayer.getVolumeLeft() + pct;
        //Timber.d(String.valueOf(volume));
        if (volume > 1) {
            //run on ui thread, b/c this is accessed by the progressive volume task
            ((ClockActivity) ctx).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ctx, "Volume MAX", Toast.LENGTH_SHORT).show();
                }
            });
            volume = 1;
        }
        setVolume(volume);
    }

    protected void volumeDown(float pct){
        float volume = mediaPlayer.getVolumeLeft() - pct;
        if (volume < 0) {
            ((ClockActivity) ctx).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ctx, "Muted", Toast.LENGTH_SHORT).show();
                }
            });
            volume = 0;
        }
        setVolume(volume);
    }

    /**
     * Set exact volume
     * @param volume
     */
    protected void setVolume(final float volume) {
        mediaPlayer.setVolume(volume, volume);
        //volumeText.setText(fmt.format(volume));
        //run on ui thread, b/c this is accessed by the progressive volume task
        ((ClockActivity) ctx).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                volumeText.setText(String.valueOf(fmt.format(volume)));
            }
        });
    }
    private class VolumeUpOnClickListener implements View.OnTouchListener{

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            GradientDrawable buttonShape = (GradientDrawable) volumeUpButton.getBackground();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    buttonShape.setStroke(1, ctx.getResources().getColor(R.color.color_clock));
                    break;
                case MotionEvent.ACTION_UP:
                    buttonShape.setStroke(1, ctx.getResources().getColor(R.color.button_color));
                    volumeUp(0.1f);
                    break;
            }
            return true;
        }
    }

    private class VolumeDownOnClickListener implements View.OnTouchListener{

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            GradientDrawable buttonShape = (GradientDrawable) volumeDownButton.getBackground();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    buttonShape.setStroke(1, ctx.getResources().getColor(R.color.color_clock));
                    break;
                case MotionEvent.ACTION_UP:
                    buttonShape.setStroke(1, ctx.getResources().getColor(R.color.button_color));
                    volumeDown(0.1f);
                    break;
            }

            return true;
        }
    }
}
