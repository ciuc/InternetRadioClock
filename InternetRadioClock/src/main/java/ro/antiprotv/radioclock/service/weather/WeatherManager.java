package ro.antiprotv.radioclock.service.weather;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import org.json.JSONException;
import org.json.JSONObject;
import ro.antiprotv.radioclock.ClockUpdater;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.service.profile.Profile;
import timber.log.Timber;

/**
 * Draws today's weather and the next two days next to the clock, and keeps it out of the clock's
 * way.
 *
 * <p>The clock does not stay put: to spare AMOLED panels it hops between the corners and the middle
 * of the screen every few minutes. The forecast is therefore anchored to the clock rather than to
 * the screen, and {@link #onClockPositionChanged} re-anchors it on every hop - above the clock
 * normally, below it once the clock has moved to the top edge and left no room up there.
 */
public class WeatherManager
    implements SharedPreferences.OnSharedPreferenceChangeListener,
        ClockUpdater.ClockPositionListener {

  /**
   * How often the forecast is refetched while the clock screen is up.
   *
   * <p>An hour, because what the bar draws - a day's high, low and chance of rain - barely moves
   * inside one. The hour-by-hour panel is drawn from the same response, and is no more urgent.
   */
  private static final long REFRESH_INTERVAL_MILLIS = 60 * 60 * 1000L;

  /**
   * How long to wait before trying again after a failed fetch. Short enough to catch the wifi
   * coming back, long enough not to hammer the service while it is down.
   */
  private static final long RETRY_INTERVAL_MILLIS = 5 * 60 * 1000L;

  /**
   * How much smaller the bar's "small" setting draws than its "big" one. Applied to the size the
   * clamp below has already settled on, so the two stay visibly apart even at the clock sizes where
   * that clamp has bottomed out.
   */
  private static final float SMALL_SCALE = 0.75f;

  private final Activity activity;
  private final SharedPreferences prefs;
  private final WeatherRepository repository;
  private final Handler handler = new Handler(Looper.getMainLooper());

  private final View weatherBar;
  private final View clockView;
  private final View dateView;
  private final View[] dayViews = new View[WeatherRepository.FORECAST_DAYS];
  private final TextView[] dayLabels = new TextView[WeatherRepository.FORECAST_DAYS];
  private final ImageView[] dayIcons = new ImageView[WeatherRepository.FORECAST_DAYS];
  private final TextView[] dayTemps = new TextView[WeatherRepository.FORECAST_DAYS];
  private final ImageView[] rainIcons = new ImageView[WeatherRepository.FORECAST_DAYS];
  private final TextView[] rainTexts = new TextView[WeatherRepository.FORECAST_DAYS];

  /** Where the clock currently sits; both start at what the layout file says. */
  private boolean clockAtTop = false;

  private boolean dateAboveClock = false;

  /** Kept so a profile change can restyle or re-show the bar without refetching. */
  private List<WeatherDay> currentDays;

  /**
   * The response {@link #currentDays} came out of, kept whole because it also carries the hours
   * the detail panel draws. Parsing those out is left until the panel is actually opened - it is
   * three days of them, and most of the time nobody ever asks.
   */
  private String currentBody;

  /** Told when one of the three days is tapped; the activity puts the detail panel up. */
  private DayClickListener dayClickListener;

  /** Whether the profile in force wants the bar at all; set by {@link #applyProfile}. */
  private boolean showWeather;

  private boolean started;

  private final Runnable refreshTask =
      new Runnable() {
        @Override
        public void run() {
          fetch();
        }
      };

  public WeatherManager(Activity activity, SharedPreferences prefs) {
    this.activity = activity;
    this.prefs = prefs;
    this.repository = new WeatherRepository(activity);

    this.weatherBar = activity.findViewById(R.id.weather_bar);
    this.clockView = activity.findViewById(R.id.fullscreen_content);
    this.dateView = activity.findViewById(R.id.date_text);

    dayViews[0] = activity.findViewById(R.id.weather_day_0);
    dayViews[1] = activity.findViewById(R.id.weather_day_1);
    dayViews[2] = activity.findViewById(R.id.weather_day_2);
    dayLabels[0] = activity.findViewById(R.id.weather_day_0_label);
    dayLabels[1] = activity.findViewById(R.id.weather_day_1_label);
    dayLabels[2] = activity.findViewById(R.id.weather_day_2_label);
    dayIcons[0] = activity.findViewById(R.id.weather_day_0_icon);
    dayIcons[1] = activity.findViewById(R.id.weather_day_1_icon);
    dayIcons[2] = activity.findViewById(R.id.weather_day_2_icon);
    dayTemps[0] = activity.findViewById(R.id.weather_day_0_temp);
    dayTemps[1] = activity.findViewById(R.id.weather_day_1_temp);
    dayTemps[2] = activity.findViewById(R.id.weather_day_2_temp);
    rainIcons[0] = activity.findViewById(R.id.weather_day_0_rain_icon);
    rainIcons[1] = activity.findViewById(R.id.weather_day_1_rain_icon);
    rainIcons[2] = activity.findViewById(R.id.weather_day_2_rain_icon);
    rainTexts[0] = activity.findViewById(R.id.weather_day_0_rain);
    rainTexts[1] = activity.findViewById(R.id.weather_day_1_rain);
    rainTexts[2] = activity.findViewById(R.id.weather_day_2_rain);

    // The whole column rather than the icon alone: it is one target, and at the sizes the bar
    // draws at the icon on its own is a poor thing to have to hit in the dark.
    for (int i = 0; i < dayViews.length; i++) {
      final int index = i;
      dayViews[i].setOnClickListener(
          v -> {
            if (dayClickListener != null) {
              dayClickListener.onWeatherDayClicked(index);
            }
          });
    }
  }

  /** Told which of the three days the user tapped. */
  public interface DayClickListener {
    void onWeatherDayClicked(int dayIndex);
  }

  public void setDayClickListener(DayClickListener listener) {
    this.dayClickListener = listener;
  }

  // ---------------------------------------------------------------------------------------------
  // Lifecycle
  // ---------------------------------------------------------------------------------------------

  public void start() {
    started = true;
    sync();
  }

  /**
   * Brings the bar and the refresh loop into line with the profile in force and the settings.
   *
   * <p>Called from everywhere something might have changed - the screen coming up, the profile
   * switching between day and night, a new location - so that one place decides whether the bar is
   * on screen and whether the refresh loop is running.
   */
  private void sync() {
    if (!started) {
      // The clock screen is down. Whatever changed gets picked up by start().
      return;
    }
    if (!shouldShow()) {
      hide();
      handler.removeCallbacks(refreshTask);
      return;
    }
    if (currentDays == null || currentDays.isEmpty()) {
      currentDays = readCache();
    }
    boolean drawn = currentDays != null && !currentDays.isEmpty();
    if (drawn) {
      show(currentDays);
    }
    // Every trip to the settings screen and back comes through here, as does every switch between
    // the day and night profiles. Refetching each time would hit the service far more often than
    // the forecast changes, so a cache young enough to still be on screen is kept, and the next
    // refresh scheduled for when it comes due.
    long age = WeatherSettings.getCacheAgeMillis(activity, prefs);
    if (drawn && age >= 0 && age < REFRESH_INTERVAL_MILLIS) {
      scheduleNext(REFRESH_INTERVAL_MILLIS - age);
    } else {
      fetch();
    }
  }

  /** The last forecast fetched, if it is still usable, or null. */
  private List<WeatherDay> readCache() {
    String cached = WeatherSettings.getCachedForecast(activity, prefs);
    if (cached == null) {
      return null;
    }
    try {
      List<WeatherDay> days = WeatherRepository.parseForecast(new JSONObject(cached));
      currentBody = cached;
      return days;
    } catch (JSONException e) {
      Timber.d("Cached forecast is unreadable: %s", e.getMessage());
      return null;
    }
  }

  public void stop() {
    started = false;
    handler.removeCallbacks(refreshTask);
  }

  public void destroy() {
    stop();
    repository.cancelAll();
  }

  // ---------------------------------------------------------------------------------------------
  // Fetching
  // ---------------------------------------------------------------------------------------------

  /** A forecast needs both a profile that asks for one and somewhere to fetch it for. */
  private boolean shouldShow() {
    return showWeather && WeatherSettings.hasLocation(activity, prefs);
  }

  private void fetch() {
    handler.removeCallbacks(refreshTask);
    if (!started || !shouldShow()) {
      return;
    }
    repository.fetchForecast(
        WeatherSettings.getLatitude(activity, prefs),
        WeatherSettings.getLongitude(activity, prefs),
        WeatherSettings.getUnit(activity, prefs),
        new WeatherRepository.ForecastCallback() {
          @Override
          public void onForecast(List<WeatherDay> days, String body) {
            WeatherSettings.saveForecast(activity, prefs, body);
            currentBody = body;
            show(days);
            scheduleNext(REFRESH_INTERVAL_MILLIS);
          }

          @Override
          public void onError(String message) {
            // Nothing is said on screen. This is a bedside clock, and a toast in the small hours
            // because the wifi blinked is worse than a slightly old forecast; whatever is already
            // drawn stays drawn.
            Timber.d("Weather refresh failed, retrying later: %s", message);
            scheduleNext(RETRY_INTERVAL_MILLIS);
          }
        });
  }

  private void scheduleNext(long delayMillis) {
    handler.removeCallbacks(refreshTask);
    if (started) {
      handler.postDelayed(refreshTask, delayMillis);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Drawing
  // ---------------------------------------------------------------------------------------------

  private void hide() {
    if (weatherBar != null) {
      weatherBar.setVisibility(View.GONE);
    }
  }

  private void show(List<WeatherDay> days) {
    if (weatherBar == null || days == null || days.isEmpty()) {
      return;
    }
    currentDays = days;
    for (int i = 0; i < dayViews.length; i++) {
      if (i >= days.size()) {
        dayViews[i].setVisibility(View.GONE);
        continue;
      }
      dayViews[i].setVisibility(View.VISIBLE);
      bindDay(i, days.get(i));
    }
    weatherBar.setVisibility(View.VISIBLE);
    applyPosition();
  }

  private void bindDay(int index, WeatherDay day) {
    dayLabels[index].setText(label(index, day.date));

    dayIcons[index].setImageResource(WeatherCodes.icon(day.weatherCode));
    dayIcons[index].setContentDescription(
        activity.getString(WeatherCodes.description(day.weatherCode)));

    dayTemps[index].setText(
        activity.getString(
            R.string.weather_temperature_format,
            Math.round(day.tempMax),
            Math.round(day.tempMin)));

    if (day.precipitationChance < 0) {
      rainIcons[index].setVisibility(View.GONE);
      rainTexts[index].setVisibility(View.GONE);
    } else {
      rainIcons[index].setVisibility(View.VISIBLE);
      rainTexts[index].setVisibility(View.VISIBLE);
      rainTexts[index].setText(
          activity.getString(R.string.weather_rain_format, day.precipitationChance));
      rainTexts[index].setContentDescription(
          activity.getString(R.string.weather_rain_description, day.precipitationChance));
    }
  }

  /** "Today" for the first column, the short weekday name for the two after it. */
  private String label(int index, String isoDate) {
    if (index == 0) {
      return activity.getString(R.string.weather_today);
    }
    try {
      Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDate);
      if (date != null) {
        return new SimpleDateFormat("EEE", Locale.getDefault()).format(date);
      }
    } catch (ParseException e) {
      Timber.d("Unparseable forecast date %s", isoDate);
    }
    return isoDate;
  }

  // ---------------------------------------------------------------------------------------------
  // Keeping out of the clock's way
  // ---------------------------------------------------------------------------------------------

  @Override
  public void onClockPositionChanged(boolean clockAtTop, boolean dateAboveClock) {
    this.clockAtTop = clockAtTop;
    this.dateAboveClock = dateAboveClock;
    applyPosition();
  }

  /**
   * Anchors the bar to whichever side of the clock block still has screen beyond it.
   *
   * <p>The date moves with the clock as well - it flips to above the clock whenever the clock is
   * along the bottom - so the anchor is the date when the date is showing on the side the bar wants
   * to sit, and the clock itself otherwise.
   */
  private void applyPosition() {
    if (weatherBar == null || clockView == null) {
      return;
    }
    RelativeLayout.LayoutParams params =
        (RelativeLayout.LayoutParams) weatherBar.getLayoutParams();
    params.removeRule(RelativeLayout.ABOVE);
    params.removeRule(RelativeLayout.BELOW);
    params.addRule(RelativeLayout.CENTER_HORIZONTAL);

    boolean dateShowing = dateView != null && dateView.getVisibility() == View.VISIBLE;
    if (clockAtTop) {
      // The clock is against the top edge; there is nothing above it to sit in, so drop below.
      params.addRule(
          RelativeLayout.BELOW,
          dateShowing && !dateAboveClock ? dateView.getId() : clockView.getId());
    } else {
      params.addRule(
          RelativeLayout.ABOVE,
          dateShowing && dateAboveClock ? dateView.getId() : clockView.getId());
    }
    weatherBar.setLayoutParams(params);
  }

  // ---------------------------------------------------------------------------------------------
  // Profile
  // ---------------------------------------------------------------------------------------------

  /**
   * Whether the bar is shown at all is part of the profile, like the clock colour or the seconds,
   * so the night profile can drop it while the day profile keeps it. Its colour comes from the
   * clock so it reads as part of it, and it scales with the clock size so it stays a footnote
   * rather than competing with the time.
   */
  public void applyProfile(Profile profile) {
    if (weatherBar == null || profile == null) {
      return;
    }
    showWeather = profile.isShowWeather();
    float fullSp = Math.max(11f, Math.min(22f, profile.getSize() / 9f));
    float tempSp =
        profile.getWeatherSize() == Profile.WEATHER_SIZE_BIG ? fullSp : fullSp * SMALL_SCALE;
    float labelSp = tempSp * 0.82f;
    int iconPx = Math.round(spToPx(tempSp) * 1.9f);
    int rainIconPx = Math.round(spToPx(tempSp) * 0.85f);
    int color = profile.getColor();

    for (int i = 0; i < dayViews.length; i++) {
      dayLabels[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, labelSp);
      dayLabels[i].setTextColor(color);
      dayTemps[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, tempSp);
      dayTemps[i].setTextColor(color);
      rainTexts[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, labelSp);
      rainTexts[i].setTextColor(color);
      dayIcons[i].setColorFilter(color);
      rainIcons[i].setColorFilter(color);
      resize(dayIcons[i], iconPx);
      resize(rainIcons[i], rainIconPx);
    }
    // The date may have just been switched on or off, which changes what the bar anchors to.
    applyPosition();
    // And the new profile may want the bar where the old one did not, or the other way about.
    sync();
  }

  private static void resize(View view, int sizePx) {
    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
    params.width = sizePx;
    params.height = sizePx;
    view.setLayoutParams(params);
  }

  private float spToPx(float sp) {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, sp, activity.getResources().getDisplayMetrics());
  }

  // ---------------------------------------------------------------------------------------------
  // Settings
  // ---------------------------------------------------------------------------------------------

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (key == null) {
      return;
    }
    // Only the settings the two profiles share are watched here. The on/off switches belong to
    // the profiles and reach us through applyProfile(), when ProfileManager reapplies one.
    Context context = activity;
    if (!key.equals(context.getString(R.string.setting_key_weather_latitude))
        && !key.equals(context.getString(R.string.setting_key_weather_longitude))
        && !key.equals(context.getString(R.string.setting_key_weather_units))) {
      return;
    }
    // Whatever is on hand is for the old place or the old unit.
    currentDays = null;
    currentBody = null;
    sync();
  }

  /** Whether a town has been picked yet; the bar cannot show anything until one has. */
  public boolean hasLocation() {
    return WeatherSettings.hasLocation(activity, prefs);
  }

  /** The forecast currently on screen, or null when there is none. */
  public List<WeatherDay> getCurrentDays() {
    return currentDays;
  }

  /** One of the three days the bar is showing, or null if it is not showing that many. */
  public WeatherDay getDay(int index) {
    if (currentDays == null || index < 0 || index >= currentDays.size()) {
      return null;
    }
    return currentDays.get(index);
  }

  /**
   * The hours belonging to one of the three days, in order, or an empty list when the response had
   * no hourly block - a forecast cached by a build before the panel existed, for one.
   */
  public List<WeatherHour> getHoursFor(int index) {
    WeatherDay day = getDay(index);
    JSONObject response = currentBody == null ? null : body();
    if (day == null || response == null) {
      return Collections.emptyList();
    }
    List<WeatherHour> matching = new ArrayList<>();
    for (WeatherHour hour : WeatherRepository.parseHourly(response)) {
      if (day.date.equals(hour.date())) {
        matching.add(hour);
      }
    }
    return matching;
  }

  /**
   * The hour it is now where the forecast is for, as {@code yyyy-MM-ddTHH}, or null when that
   * cannot be worked out. Not necessarily the hour the clock is showing: the device may be
   * somewhere else, or deliberately left on another town's time.
   */
  public String nowHour() {
    if (currentBody == null) {
      return null;
    }
    JSONObject response = body();
    if (response == null) {
      return null;
    }
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH", Locale.US);
    format.setTimeZone(TimeZone.getTimeZone("UTC"));
    // Shifting the instant by the forecast's own offset and then formatting it as UTC gives the
    // wall clock at the forecast's location, without needing a zone database entry for it.
    return format.format(
        new Date(
            System.currentTimeMillis() + WeatherRepository.utcOffsetSeconds(response) * 1000L));
  }

  private JSONObject body() {
    try {
      return new JSONObject(currentBody);
    } catch (JSONException e) {
      Timber.d("Forecast body is unreadable: %s", e.getMessage());
      return null;
    }
  }
}
