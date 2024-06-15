package ro.antiprotv.radioclock.service;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static ro.antiprotv.radioclock.activity.ClockActivity.PREF_NIGHT_MODE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.BatteryManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.activity.ClockActivity;
import ro.antiprotv.radioclock.service.profile.ProfileManager;

public class BatteryService extends BroadcastReceiver {
  public static boolean charging = false;
  public static boolean low = false;
  private final TextView battery_pct;
  private final SharedPreferences prefs;
  private final ClockActivity clockActivity;
  private final ImageView battery_icon;
  private final ProfileManager profileManager;

  public BatteryService(ClockActivity clockActivity, ProfileManager profileManager) {
    this.prefs = PreferenceManager.getDefaultSharedPreferences(clockActivity);
    this.clockActivity = clockActivity;
    battery_pct = clockActivity.findViewById(R.id.batteryPct);
    battery_icon = clockActivity.findViewById(R.id.battery_icon);
    this.profileManager = profileManager;
  }

  public void registerBatteryLevelReceiver() {
    IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    clockActivity.registerReceiver(this, ifilter);
    battery_pct.setVisibility(VISIBLE);
    battery_icon.setVisibility(VISIBLE);
  }

  public void unregisterBatteryLevelReceiver() {
    try {
      battery_pct.setVisibility(GONE);
      battery_icon.setVisibility(GONE);
      clockActivity.unregisterReceiver(this);
    } catch (Throwable t) {
      // nothing
    }
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    int status = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
    low = status <= 15;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      low = intent.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false);
    }

    charging =
        intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            == BatteryManager.BATTERY_STATUS_CHARGING;

    profileManager.applyBatteryProfile(-1);
    battery_pct.setText(String.format("%d%%", status));
  }
}
