package ro.antiprotv.radioclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Button;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {
    private ClockActivity clockActivity;
    private ButtonManager buttonManager;
    private RadioAlarmManager radioAlarmManager;

    protected AlarmReceiver(ClockActivity clockActivity, ButtonManager buttonManager, RadioAlarmManager radioAlarmManager){
        this.buttonManager = buttonManager;
        this.clockActivity = clockActivity;
        this.radioAlarmManager = radioAlarmManager;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        //what do we play?
        int memory = 0;
        if (buttonManager.getButtonClicked() == null) {
            memory = R.id.stream1;
            buttonManager.setButtonClicked((Button) clockActivity.findViewById(R.id.stream1));
        } else {
            memory = buttonManager.getButtonClicked().getId();
        }
        Toast.makeText(context, "Alarm! playing: ", Toast.LENGTH_SHORT).show();
        radioAlarmManager.changeAlarmIconAndTextOnCancel();
        clockActivity.play(memory);
    }

    //TODO: FALLBACK PLAY DEFAULT ALARM!

    //TODO: gradually increase sound
}
