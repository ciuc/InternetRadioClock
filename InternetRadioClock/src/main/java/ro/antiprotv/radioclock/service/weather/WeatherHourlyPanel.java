package ro.antiprotv.radioclock.service.weather;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import ro.antiprotv.radioclock.R;
import timber.log.Timber;

/**
 * Draws the hour-by-hour forecast for one day over the whole screen.
 *
 * <p>Only the drawing lives here. Clearing the clock and the controls off the screen first, and
 * putting them back afterwards, is {@link ro.antiprotv.radioclock.activity.ClockActivity}'s job -
 * it is the one that knows what else is on screen - so this class is handed a day and a list of
 * hours and fills the layer in.
 */
public class WeatherHourlyPanel {

  private final Activity activity;
  private final View panel;
  private final TextView title;
  private final HorizontalScrollView scroll;
  private final LinearLayout hoursRow;
  private final TextView hint;

  /** Follows the clock, so the panel reads as part of the same thing. */
  private int color;

  public WeatherHourlyPanel(Activity activity) {
    this.activity = activity;
    this.panel = activity.findViewById(R.id.weather_hourly);
    this.title = activity.findViewById(R.id.weather_hourly_title);
    this.scroll = activity.findViewById(R.id.weather_hourly_scroll);
    this.hoursRow = activity.findViewById(R.id.weather_hourly_hours);
    this.hint = activity.findViewById(R.id.weather_hourly_hint);
  }

  public void setColor(int color) {
    this.color = color;
    if (title != null) {
      title.setTextColor(color);
      hint.setTextColor(color);
    }
  }

  public boolean isShowing() {
    return panel != null && panel.getVisibility() == View.VISIBLE;
  }

  /**
   * Fills the layer in and puts it up.
   *
   * @param heading what the day is called, as the bar labels it
   * @param place the town the forecast is for, or empty
   * @param hours the hours of that day, in order
   * @param nowHour the hour that is current where the forecast is for, as {@code yyyy-MM-ddTHH},
   *     or null when that is not known; it is drawn in bold and scrolled to
   */
  public void show(String heading, String place, List<WeatherHour> hours, String nowHour) {
    if (panel == null || hours.isEmpty()) {
      return;
    }
    title.setText(
        place == null || place.isEmpty()
            ? heading
            : activity.getString(R.string.weather_hourly_title, heading, place));

    hoursRow.removeAllViews();
    LayoutInflater inflater = LayoutInflater.from(activity);
    int nowIndex = -1;
    for (int i = 0; i < hours.size(); i++) {
      WeatherHour hour = hours.get(i);
      boolean isNow = nowHour != null && hour.time.startsWith(nowHour);
      if (isNow) {
        nowIndex = i;
      }
      hoursRow.addView(cell(inflater, hour, isNow));
    }

    panel.setVisibility(View.VISIBLE);
    scrollToNow(nowIndex);
  }

  public void hide() {
    if (panel != null) {
      panel.setVisibility(View.GONE);
      // Nothing here survives a close: the next open rebuilds the row from a forecast that may
      // have been refreshed in the meantime.
      hoursRow.removeAllViews();
    }
  }

  private View cell(LayoutInflater inflater, WeatherHour hour, boolean isNow) {
    View cell = inflater.inflate(R.layout.weather_hour_cell, hoursRow, false);

    TextView time = cell.findViewById(R.id.weather_hour_time);
    time.setText(hour.hourOfDay());
    time.setTextColor(color);

    ImageView icon = cell.findViewById(R.id.weather_hour_icon);
    icon.setImageResource(WeatherCodes.icon(hour.weatherCode));
    icon.setColorFilter(color);
    icon.setContentDescription(activity.getString(WeatherCodes.description(hour.weatherCode)));

    TextView temp = cell.findViewById(R.id.weather_hour_temp);
    temp.setText(
        activity.getString(R.string.weather_hour_temperature_format, Math.round(hour.temperature)));
    temp.setTextColor(color);

    View rainGroup = cell.findViewById(R.id.weather_hour_rain_group);
    if (hour.precipitationChance < 0) {
      // The service has no precipitation model for this place; an empty slot would read as zero.
      rainGroup.setVisibility(View.INVISIBLE);
    } else {
      rainGroup.setVisibility(View.VISIBLE);
      ImageView rainIcon = cell.findViewById(R.id.weather_hour_rain_icon);
      rainIcon.setColorFilter(color);
      TextView rain = cell.findViewById(R.id.weather_hour_rain);
      rain.setText(activity.getString(R.string.weather_rain_format, hour.precipitationChance));
      rain.setTextColor(color);
      rain.setContentDescription(
          activity.getString(R.string.weather_rain_description, hour.precipitationChance));
    }

    if (isNow) {
      // The only thing setting one hour apart from the rest, since a second colour would clash
      // with whatever the clock has been set to.
      time.setTypeface(time.getTypeface(), Typeface.BOLD);
      temp.setTypeface(temp.getTypeface(), Typeface.BOLD);
    }
    return cell;
  }

  /** Puts the current hour a little in from the left edge, so the hours before it stay reachable. */
  private void scrollToNow(int nowIndex) {
    if (nowIndex <= 0) {
      scroll.post(() -> scroll.scrollTo(0, 0));
      return;
    }
    scroll.post(
        () -> {
          View cell = hoursRow.getChildAt(nowIndex);
          if (cell == null) {
            return;
          }
          scroll.scrollTo(Math.max(0, cell.getLeft() - cell.getWidth()), 0);
        });
  }

  /** "Today" for the day the bar calls today, the full weekday name otherwise. */
  public static String heading(Activity activity, int dayIndex, String isoDate) {
    if (dayIndex == 0) {
      return activity.getString(R.string.weather_today);
    }
    try {
      Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDate);
      if (date != null) {
        return new SimpleDateFormat("EEEE", Locale.getDefault()).format(date);
      }
    } catch (ParseException e) {
      Timber.d("Unparseable forecast date %s", isoDate);
    }
    return isoDate;
  }
}
