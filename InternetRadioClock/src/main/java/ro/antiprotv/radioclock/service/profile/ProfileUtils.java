package ro.antiprotv.radioclock.service.profile;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import ro.antiprotv.radioclock.R;
import timber.log.BuildConfig;

public class ProfileUtils {
  private static final String HUMAN_READABLE_TIME_FORMAT = "%s, %s";
  private static ProfileUtils INSTANCE;
  private final Context context;

  private ProfileUtils(Context context) {
    this.context = context;
  }

  protected static ProfileUtils getInstance(Context context) {
    if (INSTANCE == null) {
      INSTANCE = new ProfileUtils(context);
    }
    return INSTANCE;
  }

  public static int getHour(String time) {
    String[] pieces = time.split(":");

    return (Integer.parseInt(pieces[0]));
  }

  public static int getMinute(String time) {
    String[] pieces = time.split(":");

    return (Integer.parseInt(pieces[1]));
  }

  public static boolean isNight(Calendar nightStart, Calendar nightEnd, Calendar now) {
    if (nightStart.before(nightEnd)) {
      // | ------- start +++++++++ end -------- |
      if (now.after(nightStart) && now.before(nightEnd)) {
        // | ------- start +++!+++ end -------- |
        return true; // and do nothing with the calendars
      } else if (now.before(nightStart)) {
        // | ----!--- start ++++++++ end -------- |
        return false; // and do nothing with the calendars
      } else {
        // apply night and schedule day tomorrow
        // | ------- start ++++++++ end ----!---- |
        nightStart.add(Calendar.DAY_OF_MONTH, 1);
        return false;
      }
    } else {
      // | +++++++ end ------- start +++++++ |
      if (now.before(nightEnd)) {
        // | +++!++++ end ------- start +++++++ |
        return true; // and do nothing with the calendars
      }
      if (now.after(nightStart)) {
        // | +++++++ end ------- start ++++!+++ |
        nightEnd.add(Calendar.DAY_OF_MONTH, 1);
        return true;
      } else {
        // | +++++++ end ---!---- start +++++++ |
        return false; // and do nothing with the calendars
      }
    }
  }

  protected String getHumanReadableCalendar(Calendar calendar) {
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    if (BuildConfig.DEBUG) {
      sdf = new SimpleDateFormat("dd/MM/y HH:mm:mm");
    }
    return String.format(
        HUMAN_READABLE_TIME_FORMAT, today_tomorrow(calendar), sdf.format(calendar.getTime()));
  }

  private String today_tomorrow(Calendar calendar) {
    Calendar hour24 = Calendar.getInstance();
    hour24.set(Calendar.HOUR_OF_DAY, 23);
    hour24.set(Calendar.MINUTE, 59);
    hour24.set(Calendar.SECOND, 59);
    if (calendar.after(hour24)) {
      return context.getResources().getString(R.string.text_tomorrow);
    }
    return context.getResources().getString(R.string.today);
  }
}
