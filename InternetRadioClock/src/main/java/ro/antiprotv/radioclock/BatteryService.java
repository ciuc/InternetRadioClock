package ro.antiprotv.radioclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import androidx.core.content.ContextCompat;

import android.widget.ImageView;
import android.widget.TextView;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static ro.antiprotv.radioclock.ClockActivity.PREF_NIGHT_MODE;

public class BatteryService extends BroadcastReceiver {
    private final TextView pct;
    private final SharedPreferences prefs;
    private final ClockActivity clockActivity;
    private final ImageView icon;

    public BatteryService(ClockActivity clockActivity, ImageView icon, TextView pct) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(clockActivity);
        this.pct = pct;
        this.clockActivity = clockActivity;
        this.icon = icon;
    }

    public void registerBatteryLevelReceiver() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        clockActivity.registerReceiver(this, ifilter);
        pct.setVisibility(VISIBLE);
        icon.setVisibility(VISIBLE);
    }


    public void unregisterBatteryLevelReceiver() {
        try {
            pct.setVisibility(GONE);
            icon.setVisibility(GONE);
            clockActivity.unregisterReceiver(this);
        } catch (Throwable t) {
            //nothing
        }
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        pct.setText(String.format("%d%%", status));
        boolean low = intent.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false);
        boolean charging = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING;
        if (low && !charging) {
            pct.setTextColor(clockActivity.getResources().getColor(R.color.color_clock_red));
            icon.setColorFilter(ContextCompat.getColor(context, R.color.color_clock_red), android.graphics.PorterDuff.Mode.SRC_IN);
            icon.setImageResource(R.drawable.ic_baseline_battery_alert_16);
        } else {
            icon.setImageResource(R.drawable.ic_baseline_battery_std_16);
            int color = clockActivity.getResources().getColor(R.color.color_clock);
            if (prefs.getBoolean(clockActivity.getResources().getString(R.string.setting_key_batteryInClockColor), false)) {
                String colorCode;
                boolean nightmode = prefs.getBoolean(PREF_NIGHT_MODE, false);
                if (nightmode) {
                    colorCode = prefs.getString(clockActivity.getResources().getString(R.string.setting_key_clockColor_night), clockActivity.getResources().getString(R.string.setting_default_clockColor));
                } else {
                    colorCode = prefs.getString(clockActivity.getResources().getString(R.string.setting_key_clockColor), clockActivity.getResources().getString(R.string.setting_default_clockColor));
                }
                color = Color.parseColor(colorCode);
                icon.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
            }
            icon.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
            pct.setTextColor(color);
        }
    }
}
