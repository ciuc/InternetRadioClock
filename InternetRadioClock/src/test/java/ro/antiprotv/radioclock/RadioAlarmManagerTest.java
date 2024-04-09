package ro.antiprotv.radioclock;

import android.app.AlarmManager;
import android.content.SharedPreferences;
import android.widget.ImageButton;
import android.widget.TextView;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import ro.antiprotv.radioclock.activity.ClockActivity;
import ro.antiprotv.radioclock.service.ButtonManager;
import ro.antiprotv.radioclock.service.RadioAlarmManager;


public class RadioAlarmManagerTest {

    private SharedPreferences prefs = mock(SharedPreferences.class);
    private final ClockActivity clockActivity = mock(ClockActivity.class);
    private RadioAlarmManager radioAlarmManager;

    @Before
    public void setUp() throws Exception {
        reset(prefs, clockActivity);
        when(clockActivity.findViewById(R.id.alarm_icon_turn_off)).thenReturn(mock(ImageButton.class));
        when(clockActivity.findViewById(R.id.alarm_icon_cancel)).thenReturn(mock(ImageButton.class));
        when(clockActivity.findViewById(R.id.alarm_icon_snooze)).thenReturn(mock(ImageButton.class));
        when(clockActivity.findViewById(R.id.alarm_icon_close)).thenReturn(mock(ImageButton.class));
        when(clockActivity.findViewById(R.id.alarm_time)).thenReturn(mock(TextView.class));
        when(clockActivity.findViewById(R.id.alarm_icon)).thenReturn(mock(ImageButton.class));
        when(clockActivity.findViewById(R.id.alarm_icon_turn_off)).thenReturn(mock(ImageButton.class));
        when(clockActivity.findViewById(R.id.alarm_icon_turn_off2)).thenReturn(mock(ImageButton.class));
        radioAlarmManager = new RadioAlarmManager(clockActivity, mock(ButtonManager.class));

        radioAlarmManager.setPrefs(prefs);
        radioAlarmManager.setAlarmMgr(mock(AlarmManager.class));
        Toaster toaster = mock(Toaster.class);
        radioAlarmManager.setToaster(toaster);
    }

    @Test
    public void setAlarm() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        when(prefs.getBoolean("setting.alarm.1.key.2", false)).thenReturn(true);
        when(prefs.getBoolean("setting.alarm.1.key.5", false)).thenReturn(true);
        when(prefs.getBoolean("setting.alarm.1.key.7", false)).thenReturn(true);

        when(prefs.getInt("setting.alarm.1.hh", -1)).thenReturn(11);
        when(prefs.getInt("setting.alarm.1.mm", -1)).thenReturn(11);

        Method method = RadioAlarmManager.class.getDeclaredMethod("getNextAlarm", Integer.TYPE);
        method.setAccessible(true);
        RadioAlarmManager.Alarm alarm = (RadioAlarmManager.Alarm) method.invoke(radioAlarmManager, 1);

        Assert.assertEquals(new RadioAlarmManager.Alarm(7, 11, 11, 1), alarm);
    }

    @Test
    public void setAlarm2() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        when(prefs.getBoolean("setting.alarm.1.key.2", false)).thenReturn(true);
        when(prefs.getBoolean("setting.alarm.1.key.5", false)).thenReturn(false);
        when(prefs.getBoolean("setting.alarm.1.key.7", false)).thenReturn(false);

        Calendar now = Calendar.getInstance();
        when(prefs.getInt("setting.alarm.1.hh", -1)).thenReturn(now.get(Calendar.HOUR_OF_DAY) - 1);
        when(prefs.getInt("setting.alarm.1.mm", -1)).thenReturn(11);

        Method method = RadioAlarmManager.class.getDeclaredMethod("getNextAlarm", Integer.TYPE);
        method.setAccessible(true);
        RadioAlarmManager.Alarm alarm = (RadioAlarmManager.Alarm) method.invoke(radioAlarmManager, 1);

        Assert.assertEquals(new RadioAlarmManager.Alarm(1, now.get(Calendar.HOUR_OF_DAY) - 1, 11, 1), alarm);
    }

    @Test
    public void setAlarm3() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        when(prefs.getBoolean("setting.alarm.1.key.2", false)).thenReturn(true);
        when(prefs.getBoolean("setting.alarm.1.key.5", false)).thenReturn(false);
        when(prefs.getBoolean("setting.alarm.1.key.7", false)).thenReturn(false);

        Calendar now = Calendar.getInstance();
        when(prefs.getInt("setting.alarm.1.hh", -1)).thenReturn(now.get(Calendar.HOUR_OF_DAY) + 1);
        when(prefs.getInt("setting.alarm.1.mm", -1)).thenReturn(11);

        Method method = RadioAlarmManager.class.getDeclaredMethod("getNextAlarm", Integer.TYPE);
        method.setAccessible(true);
        RadioAlarmManager.Alarm alarm = (RadioAlarmManager.Alarm) method.invoke(radioAlarmManager, 1);

        Assert.assertEquals(new RadioAlarmManager.Alarm(7, now.get(Calendar.HOUR_OF_DAY) + 1, 11, 1), alarm);
    }

    @Test
    public void setAlarm_5() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        when(prefs.getBoolean("setting.alarm.1.key.2", false)).thenReturn(false);
        when(prefs.getBoolean("setting.alarm.1.key.5", false)).thenReturn(false);
        when(prefs.getBoolean("setting.alarm.1.key.7", false)).thenReturn(false);

        Calendar now = Calendar.getInstance();
        when(prefs.getInt("setting.alarm.1.hh", -1)).thenReturn(now.get(Calendar.HOUR_OF_DAY) + 1);
        when(prefs.getInt("setting.alarm.1.mm", -1)).thenReturn(11);

        Method method = RadioAlarmManager.class.getDeclaredMethod("getNextAlarm", Integer.TYPE);
        method.setAccessible(true);
        RadioAlarmManager.Alarm alarm = (RadioAlarmManager.Alarm) method.invoke(radioAlarmManager, 1);

        System.out.println("result: " + alarm);
        Assert.assertEquals(new RadioAlarmManager.Alarm(7, now.get(Calendar.HOUR_OF_DAY) + 1, 11, 1), alarm);
    }

    @Test
    public void setAlarm_6() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        when(prefs.getBoolean("setting.alarm.1.key.2", false)).thenReturn(false);
        when(prefs.getBoolean("setting.alarm.1.key.5", false)).thenReturn(false);
        when(prefs.getBoolean("setting.alarm.1.key.7", false)).thenReturn(false);

        Calendar now = Calendar.getInstance();
        when(prefs.getInt("setting.alarm.1.hh", -1)).thenReturn(now.get(Calendar.HOUR_OF_DAY) - 1);
        when(prefs.getInt("setting.alarm.1.mm", -1)).thenReturn(11);

        Method method = RadioAlarmManager.class.getDeclaredMethod("getNextAlarm", Integer.TYPE);
        method.setAccessible(true);
        RadioAlarmManager.Alarm alarm = (RadioAlarmManager.Alarm) method.invoke(radioAlarmManager, 1);

        System.out.println("result: " + alarm);
        Assert.assertEquals(new RadioAlarmManager.Alarm(1, now.get(Calendar.HOUR_OF_DAY) - 1, 11, 1), alarm);
    }

    @Test
    public void setAlarm_NULL() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        when(prefs.getBoolean("setting.alarm.1.key.2", false)).thenReturn(false);
        when(prefs.getBoolean("setting.alarm.2.key.5", false)).thenReturn(false);
        when(prefs.getBoolean("setting.alarm.1.key.7", false)).thenReturn(false);

        when(prefs.getInt("setting.alarm.1.hh", -1)).thenReturn(-1);
        when(prefs.getInt("setting.alarm.1.mm", -1)).thenReturn(-1);

        when(prefs.getInt("setting.alarm.2.hh", -1)).thenReturn(-1);
        when(prefs.getInt("setting.alarm.2.mm", -1)).thenReturn(-1);
        Calendar now = Calendar.getInstance();

        Method method = RadioAlarmManager.class.getDeclaredMethod("getNextAlarm", Integer.TYPE);
        method.setAccessible(true);
        RadioAlarmManager.Alarm alarm = (RadioAlarmManager.Alarm) method.invoke(radioAlarmManager, 1);

        System.out.println("result: " + alarm);
        Assert.assertNull(alarm);

        alarm = (RadioAlarmManager.Alarm) method.invoke(radioAlarmManager, 2);

        System.out.println("result: " + alarm);
        Assert.assertNull(alarm);
    }
}