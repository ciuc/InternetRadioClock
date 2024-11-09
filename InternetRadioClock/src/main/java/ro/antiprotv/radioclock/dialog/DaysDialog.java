package ro.antiprotv.radioclock.dialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import java.util.HashMap;
import java.util.Map;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.service.RadioAlarmManager;

public class DaysDialog extends AlertDialog {
  private final SharedPreferences prefs;

  public DaysDialog(
      final int hh,
      final int mm,
      int alarmId,
      @NonNull Context context,
      RadioAlarmManager alarmManager,
      SharedPreferences prefs) {
    super(context);
    this.prefs = prefs;
    Map<Integer, Map<String, Integer>> alarmKeys = new HashMap<>();
    Map<String, Integer> alarmIds1 = new HashMap<>();
    alarmIds1.put("MON", R.id.setting_alarm_1_key_2);
    alarmIds1.put("TUE", R.id.setting_alarm_1_key_3);
    alarmIds1.put("WED", R.id.setting_alarm_1_key_4);
    alarmIds1.put("THU", R.id.setting_alarm_1_key_5);
    alarmIds1.put("FRI", R.id.setting_alarm_1_key_6);
    alarmIds1.put("SAT", R.id.setting_alarm_1_key_7);
    alarmIds1.put("SUN", R.id.setting_alarm_1_key_1);
    alarmKeys.put(RadioAlarmManager.ALARM_ID_1, alarmIds1);
    Map<String, Integer> alarmIds2 = new HashMap<>();
    alarmIds2.put("MON", R.id.setting_alarm_1_key_2);
    alarmIds2.put("TUE", R.id.setting_alarm_1_key_3);
    alarmIds2.put("WED", R.id.setting_alarm_1_key_4);
    alarmIds2.put("THU", R.id.setting_alarm_1_key_5);
    alarmIds2.put("FRI", R.id.setting_alarm_1_key_6);
    alarmIds2.put("SAT", R.id.setting_alarm_1_key_7);
    alarmIds2.put("SUN", R.id.setting_alarm_1_key_1);
    alarmKeys.put(RadioAlarmManager.ALARM_ID_2, alarmIds2);
    final LinearLayout layout =
        (LinearLayout) LayoutInflater.from(context).inflate(R.layout.dialog_alarm_days, null);

    final CheckBox MON = layout.findViewById(alarmKeys.get(alarmId).get("MON"));
    final CheckBox TUE = layout.findViewById(alarmKeys.get(alarmId).get("TUE"));
    final CheckBox WED = layout.findViewById(alarmKeys.get(alarmId).get("WED"));
    final CheckBox THU = layout.findViewById(alarmKeys.get(alarmId).get("THU"));
    final CheckBox FRI = layout.findViewById(alarmKeys.get(alarmId).get("FRI"));
    final CheckBox SAT = layout.findViewById(alarmKeys.get(alarmId).get("SAT"));
    final CheckBox SUN = layout.findViewById(alarmKeys.get(alarmId).get("SUN"));
    MON.setChecked(prefs.getBoolean("setting.alarm." + alarmId + ".key.2", false));
    TUE.setChecked(prefs.getBoolean("setting.alarm." + alarmId + ".key.3", false));
    WED.setChecked(prefs.getBoolean("setting.alarm." + alarmId + ".key.4", false));
    THU.setChecked(prefs.getBoolean("setting.alarm." + alarmId + ".key.5", false));
    FRI.setChecked(prefs.getBoolean("setting.alarm." + alarmId + ".key.6", false));
    SAT.setChecked(prefs.getBoolean("setting.alarm." + alarmId + ".key.7", false));
    SUN.setChecked(prefs.getBoolean("setting.alarm." + alarmId + ".key.1", false));
    setView(layout);
    setButton(
        AlertDialog.BUTTON_POSITIVE,
        context.getString(R.string.ok),
        (dialog, which) -> {
          prefs.edit().putBoolean("setting.alarm." + alarmId + ".key.2", MON.isChecked()).apply();
          prefs.edit().putBoolean("setting.alarm." + alarmId + ".key.3", TUE.isChecked()).apply();
          prefs.edit().putBoolean("setting.alarm." + alarmId + ".key.4", WED.isChecked()).apply();
          prefs.edit().putBoolean("setting.alarm." + alarmId + ".key.5", THU.isChecked()).apply();
          prefs.edit().putBoolean("setting.alarm." + alarmId + ".key.6", FRI.isChecked()).apply();
          prefs.edit().putBoolean("setting.alarm." + alarmId + ".key.7", SAT.isChecked()).apply();
          prefs.edit().putBoolean("setting.alarm." + alarmId + ".key.1", SUN.isChecked()).apply();
          prefs.edit().putInt("setting.alarm." + alarmId + ".hh", hh).apply();
          prefs.edit().putInt("setting.alarm." + alarmId + ".mm", mm).apply();
          alarmManager.setAlarm();
        });
    setTitle(R.string.title_dialog_alarm_days);
  }
}
