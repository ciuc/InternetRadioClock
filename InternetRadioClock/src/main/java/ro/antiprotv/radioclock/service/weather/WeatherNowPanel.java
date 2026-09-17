package ro.antiprotv.radioclock.service.weather;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import ro.antiprotv.radioclock.R;

/**
 * Draws the current conditions over the whole screen: the icon and temperature the "Now" column
 * already shows, and under them the readings that never fit in a column an inch wide.
 *
 * <p>Only the drawing lives here, as with {@link WeatherHourlyPanel}. Clearing the clock and the
 * controls off the screen first, and putting them back afterwards, is {@link
 * ro.antiprotv.radioclock.activity.ClockActivity}'s job - it is the one that knows what else is on
 * screen.
 */
public class WeatherNowPanel {

  private final Activity activity;
  private final View panel;
  private final TextView title;
  private final ImageView icon;
  private final TextView temperature;
  private final TextView condition;
  private final LinearLayout details;
  private final ImageButton refresh;
  private final TextView hint;

  /** Turns while a refresh is in flight, so a tap is visibly doing something. */
  private ObjectAnimator spin;

  /** Follows the clock, so the panel reads as part of the same thing. */
  private int color;

  public WeatherNowPanel(Activity activity) {
    this.activity = activity;
    this.panel = activity.findViewById(R.id.weather_now_panel);
    this.title = activity.findViewById(R.id.weather_now_panel_title);
    this.icon = activity.findViewById(R.id.weather_now_panel_icon);
    this.temperature = activity.findViewById(R.id.weather_now_panel_temp);
    this.condition = activity.findViewById(R.id.weather_now_panel_condition);
    this.details = activity.findViewById(R.id.weather_now_panel_details);
    this.refresh = activity.findViewById(R.id.weather_now_panel_refresh);
    this.hint = activity.findViewById(R.id.weather_now_panel_hint);
  }

  public void setColor(int color) {
    this.color = color;
    if (panel == null) {
      return;
    }
    title.setTextColor(color);
    temperature.setTextColor(color);
    condition.setTextColor(color);
    hint.setTextColor(color);
    icon.setColorFilter(color);
    refresh.setColorFilter(color);
    for (int i = 0; i < details.getChildCount(); i++) {
      colorRow(details.getChildAt(i));
    }
  }

  public boolean isShowing() {
    return panel != null && panel.getVisibility() == View.VISIBLE;
  }

  /** Told when the refresh button is tapped; the activity is the one that can fetch. */
  public void setRefreshListener(View.OnClickListener listener) {
    if (refresh != null) {
      refresh.setOnClickListener(listener);
    }
  }

  /**
   * Turns the button while a refresh is in flight, and locks it so a second tap cannot start
   * another one on top of the first.
   */
  public void setRefreshing(boolean refreshing) {
    if (refresh == null) {
      return;
    }
    refresh.setEnabled(!refreshing);
    if (!refreshing) {
      if (spin != null) {
        spin.cancel();
        spin = null;
      }
      refresh.setRotation(0f);
      return;
    }
    if (spin != null) {
      return;
    }
    spin = ObjectAnimator.ofFloat(refresh, View.ROTATION, 0f, 360f);
    spin.setDuration(800);
    spin.setRepeatCount(ValueAnimator.INFINITE);
    spin.setInterpolator(new LinearInterpolator());
    spin.start();
  }

  /**
   * Fills the layer in and puts it up.
   *
   * @param place the town the forecast is for, or empty
   * @param now the current conditions
   * @param windUnit what the wind speed is measured in, as the request asked for it
   */
  public void show(String place, WeatherNow now, String windUnit) {
    if (panel == null || now == null) {
      return;
    }
    String heading = activity.getString(R.string.weather_now);
    title.setText(
        place == null || place.isEmpty()
            ? heading
            : activity.getString(R.string.weather_hourly_title, heading, place));

    icon.setImageResource(WeatherCodes.icon(now.weatherCode, now.isDay));
    icon.setContentDescription(activity.getString(WeatherCodes.description(now.weatherCode)));
    temperature.setText(
        activity.getString(
            R.string.weather_hour_temperature_format, Math.round(now.temperature)));
    condition.setText(WeatherCodes.description(now.weatherCode));

    details.removeAllViews();
    if (!Double.isNaN(now.windSpeed)) {
      addRow(R.string.weather_detail_wind, wind(now, windUnit));
    }
    if (now.humidity >= 0) {
      addRow(
          R.string.weather_detail_humidity,
          activity.getString(R.string.weather_rain_format, now.humidity));
    }
    if (!Double.isNaN(now.pressure)) {
      addRow(
          R.string.weather_detail_pressure,
          activity.getString(R.string.weather_detail_pressure_format, Math.round(now.pressure)));
    }
    if (!Double.isNaN(now.uvIndex)) {
      addRow(R.string.weather_detail_uv, uv(now.uvIndex));
    }
    panel.setVisibility(View.VISIBLE);
  }

  public void hide() {
    if (panel != null) {
      // A refresh may still be in flight; the button it belongs to is going away, so stop it
      // turning rather than leave an animation running against a hidden view.
      setRefreshing(false);
      panel.setVisibility(View.GONE);
      // Nothing here survives a close: the next open rebuilds the rows from a forecast that may
      // have been refreshed in the meantime.
      details.removeAllViews();
    }
  }

  private void addRow(int labelRes, String value) {
    View row = LayoutInflater.from(activity).inflate(R.layout.weather_detail_row, details, false);
    ((TextView) row.findViewById(R.id.weather_detail_label)).setText(labelRes);
    ((TextView) row.findViewById(R.id.weather_detail_value)).setText(value);
    colorRow(row);
    details.addView(row);
  }

  private void colorRow(View row) {
    ((TextView) row.findViewById(R.id.weather_detail_label)).setTextColor(color);
    ((TextView) row.findViewById(R.id.weather_detail_value)).setTextColor(color);
  }

  /** "12 km/h NW", or just "12 km/h" when the service gave no direction. */
  private String wind(WeatherNow now, String windUnit) {
    String speed =
        activity.getString(
            R.string.weather_detail_wind_format, Math.round(now.windSpeed), windUnit);
    if (now.windDirection < 0) {
      return speed;
    }
    return activity.getString(
        R.string.weather_detail_wind_direction_format,
        speed,
        activity.getString(compass(now.windDirection)));
  }

  /**
   * Which of the eight points a bearing falls on. Eight rather than sixteen because the extra
   * eight say more than a glance at a bedside clock is asking for, and NNW is a poor thing to read
   * half awake.
   */
  private static int compass(int degrees) {
    // Offset by half a sector so each name is centred on its own bearing rather than starting at
    // it, then wrap: 349 degrees is north, not north-west.
    int sector = (int) Math.floor(((degrees % 360 + 360) % 360 + 22.5) / 45.0) % 8;
    switch (sector) {
      case 1:
        return R.string.weather_wind_ne;
      case 2:
        return R.string.weather_wind_e;
      case 3:
        return R.string.weather_wind_se;
      case 4:
        return R.string.weather_wind_s;
      case 5:
        return R.string.weather_wind_sw;
      case 6:
        return R.string.weather_wind_w;
      case 7:
        return R.string.weather_wind_nw;
      default:
        return R.string.weather_wind_n;
    }
  }

  /** "3 · Moderate": the number on its own means little to anyone who does not already know it. */
  private String uv(double index) {
    long rounded = Math.round(index);
    int band;
    if (rounded <= 2) {
      band = R.string.weather_uv_low;
    } else if (rounded <= 5) {
      band = R.string.weather_uv_moderate;
    } else if (rounded <= 7) {
      band = R.string.weather_uv_high;
    } else if (rounded <= 10) {
      band = R.string.weather_uv_very_high;
    } else {
      band = R.string.weather_uv_extreme;
    }
    return activity.getString(
        R.string.weather_detail_uv_format, rounded, activity.getString(band));
  }
}
