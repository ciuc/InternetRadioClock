package ro.antiprotv.radioclock.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.Stream;
import ro.antiprotv.radioclock.StreamListAdapter;
import ro.antiprotv.radioclock.db.StationRepository;
import ro.antiprotv.radioclock.service.RadioBrowserClient;
import ro.antiprotv.radioclock.service.RadioBrowserClient.FilterList;
import ro.antiprotv.radioclock.service.RadioBrowserClient.Option;

/**
 * Searches radio-browser.info for stations. Results come in pages of {@link
 * RadioBrowserClient#PAGE_SIZE}; the next page loads as the list is scrolled near its end, until a
 * page comes back short.
 */
public class StreamFinderActivity extends AppCompatActivity {
  // The selected filter values. Older versions kept spinner positions under
  // "finder.selected.*"; those positions mean nothing in the lists loaded from radio-browser, so
  // they are left alone and new keys are used.
  private static final String PREF_COUNTRY = "finder.country";
  private static final String PREF_LANGUAGE = "finder.language";
  private static final String PREF_TAG = "finder.tag";
  private static final String PREF_ORDER = "finder.order";

  /** Load the next page when the last visible row is this close to the end of the list. */
  private static final int LOAD_AHEAD = 10;

  private final List<Stream> streams = new ArrayList<>();
  /** radio-browser ids already in the list; popularity can shift a station across a page edge. */
  private final Set<String> seenUuids = new HashSet<>();

  private StreamListAdapter adapter;
  private SharedPreferences prefs;
  private RadioBrowserClient searchClient;
  private RadioBrowserClient listClient;

  private Spinner countrySpinner;
  private Spinner languageSpinner;
  private Spinner tagSpinner;
  private Spinner orderSpinner;

  /** Whether the country spinner holds radio-browser's codes, or the built-in names. */
  private boolean countriesAreCodes;

  // paging state of the search shown in the dialog
  private RadioBrowserClient.Query query;
  private int nextOffset;
  private boolean loading;
  private boolean endReached;
  /** Bumped on every new search and on close, so late answers to an old one are dropped. */
  private int generation;

  private AlertDialog resultsDialog;
  private RecyclerView resultsList;
  private ProgressBar resultsProgress;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_stream_finder);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    SystemBarInsets.apply(this);
    Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

    prefs = PreferenceManager.getDefaultSharedPreferences(this);
    searchClient = new RadioBrowserClient(this);
    listClient = new RadioBrowserClient(this);

    countrySpinner = findViewById(R.id.streamFinder_dropdown_country);
    languageSpinner = findViewById(R.id.streamFinder_dropdown_language);
    tagSpinner = findViewById(R.id.streamFinder_dropdown_tags);
    orderSpinner = findViewById(R.id.streamFinder_dropdown_order);
    // the built-in lists first, so the screen works at once and offline; radio-browser's own
    // lists replace them when they arrive
    setOptions(countrySpinner, fallbackOptions(R.array.countries), PREF_COUNTRY, null, false);
    setOptions(languageSpinner, fallbackOptions(R.array.languages), PREF_LANGUAGE, null, false);
    setOptions(tagSpinner, fallbackOptions(R.array.genres), PREF_TAG, null, false);
    setOptions(orderSpinner, orderOptions(), PREF_ORDER, null, false);
    loadFilterLists();

    adapter = new StreamListAdapter(this, streams);
    // every row offers to replace one of the saved stations, or to add one
    StationRepository.getInstance(this).observeStations().observe(this, adapter::setStations);

    final Button findStreamButton = findViewById(R.id.find_stream);
    findStreamButton.setOnClickListener(v -> startSearch());
    final ImageButton helpButton = findViewById(R.id.streamFinder_help);
    helpButton.setOnClickListener(new OnHelpClickListener());

    final ImageButton hideKeyboard = findViewById(R.id.streamFinder_hidek);
    hideKeyboard.setOnClickListener(new KeyboardHideOnClickListener(this));
  }

  // --- filters --------------------------------------------------------------------------------

  private void loadFilterLists() {
    listClient.loadOptions(
        FilterList.COUNTRIES,
        new RadioBrowserClient.OptionsCallback() {
          @Override
          public void onOptions(List<Option> options) {
            if (isFinishing()) {
              return;
            }
            countriesAreCodes = true;
            // first visit: the phone's own country is the likeliest wish
            setOptions(
                countrySpinner,
                withAny(options),
                PREF_COUNTRY,
                guessCountry(),
                // a built-in name never matches a code
                false);
          }

          @Override
          public void onError() {
            // keep the built-in names
          }
        });
    loadInto(FilterList.LANGUAGES, languageSpinner, PREF_LANGUAGE);
    loadInto(FilterList.TAGS, tagSpinner, PREF_TAG);
  }

  /**
   * Where the phone is, as an ISO code: from the mobile network, else the SIM, else the language
   * setting. The language comes last because many phones run in English (US) far from the US.
   */
  private String guessCountry() {
    TelephonyManager telephony = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
    if (telephony != null) {
      try {
        String network = telephony.getNetworkCountryIso();
        if (network != null && network.length() == 2) {
          return network.toUpperCase(Locale.ROOT);
        }
        String sim = telephony.getSimCountryIso();
        if (sim != null && sim.length() == 2) {
          return sim.toUpperCase(Locale.ROOT);
        }
      } catch (RuntimeException e) {
        // no telephony on this device; fall through
      }
    }
    return Locale.getDefault().getCountry();
  }

  private void loadInto(FilterList list, Spinner spinner, String prefKey) {
    listClient.loadOptions(
        list,
        new RadioBrowserClient.OptionsCallback() {
          @Override
          public void onOptions(List<Option> options) {
            if (!isFinishing()) {
              setOptions(spinner, withAny(options), prefKey, null, true);
            }
          }

          @Override
          public void onError() {
            // keep the built-in list
          }
        });
  }

  /**
   * Fills the spinner and selects, in order: what the user picked in the list being replaced (when
   * {@code keepCurrent}), the remembered value, {@code preferred}, or "Any".
   */
  private void setOptions(
      Spinner spinner,
      List<Option> options,
      String prefKey,
      String preferred,
      boolean keepCurrent) {
    Option current = (Option) spinner.getSelectedItem();
    String wanted = prefs.getString(prefKey, preferred);
    if (keepCurrent && current != null && !current.value.isEmpty()) {
      wanted = current.value;
    }
    ArrayAdapter<Option> spinnerAdapter =
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(spinnerAdapter);
    spinner.setSelection(indexOf(options, wanted));
  }

  private static int indexOf(List<Option> options, String value) {
    if (value == null) {
      return 0;
    }
    for (int i = 0; i < options.size(); i++) {
      if (options.get(i).value.equalsIgnoreCase(value)) {
        return i;
      }
    }
    return 0;
  }

  /** A built-in list; its own "Any" entry is replaced by ours, so the value is always empty. */
  private List<Option> fallbackOptions(int arrayRes) {
    String any = getString(R.string.any);
    List<Option> options = new ArrayList<>();
    for (String item : getResources().getStringArray(arrayRes)) {
      if (!item.equals(any)) {
        options.add(new Option(item, item));
      }
    }
    return withAny(options);
  }

  /** Most popular first: it is what the search did before there was a choice. */
  private List<Option> orderOptions() {
    List<Option> options = new ArrayList<>();
    options.add(
        new Option(
            getString(R.string.streamFinder_order_popular),
            RadioBrowserClient.Order.POPULAR.value));
    options.add(
        new Option(
            getString(R.string.streamFinder_order_alphabetical),
            RadioBrowserClient.Order.ALPHABETICAL.value));
    return options;
  }

  private List<Option> withAny(List<Option> options) {
    List<Option> all = new ArrayList<>(options.size() + 1);
    all.add(new Option(getString(R.string.any), ""));
    all.addAll(options);
    return all;
  }

  private String selectedValue(Spinner spinner) {
    Option option = (Option) spinner.getSelectedItem();
    return option == null ? "" : option.value;
  }

  // --- search and paging ----------------------------------------------------------------------

  @SuppressLint("NotifyDataSetChanged")
  private void startSearch() {
    String country = selectedValue(countrySpinner);
    String language = selectedValue(languageSpinner);
    String tag = selectedValue(tagSpinner);
    String order = selectedValue(orderSpinner);
    prefs
        .edit()
        .putString(PREF_COUNTRY, country)
        .putString(PREF_LANGUAGE, language)
        .putString(PREF_TAG, tag)
        .putString(PREF_ORDER, order)
        .apply();
    TextView nameTv = findViewById(R.id.streamFinder_textinput_name);
    String name = nameTv.getText() != null ? nameTv.getText().toString() : "";

    query =
        new RadioBrowserClient.Query(
            name,
            countriesAreCodes ? country : "",
            countriesAreCodes ? "" : country,
            language,
            tag,
            RadioBrowserClient.Order.fromValue(order));
    generation++;
    searchClient.cancelAll();
    nextOffset = 0;
    loading = false;
    endReached = false;
    adapter.clearPreviewState();
    streams.clear();
    seenUuids.clear();
    adapter.notifyDataSetChanged();

    showResultsDialog();
    loadNextPage();
  }

  private void showResultsDialog() {
    View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_radio_list, null);
    resultsList = dialogView.findViewById(R.id.stream_list_view);
    resultsProgress = dialogView.findViewById(R.id.stream_list_progress);
    resultsList.setLayoutManager(new LinearLayoutManager(this));
    resultsList.setAdapter(adapter);
    resultsList.addOnScrollListener(
        new RecyclerView.OnScrollListener() {
          @Override
          public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            if (dy > 0) {
              loadMoreIfNearEnd();
            }
          }
        });

    resultsDialog =
        new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_radio_list_title)
            .setView(dialogView)
            .setPositiveButton(R.string.close, null)
            .create();
    resultsDialog.setOnDismissListener(
        d -> {
          adapter.stopPreview();
          // nobody is looking any more: drop the page that may still be on its way
          generation++;
          searchClient.cancelAll();
          loading = false;
        });
    resultsDialog.show();
  }

  private void loadNextPage() {
    if (loading || endReached || query == null) {
      return;
    }
    loading = true;
    showProgress(true);
    final int requestGeneration = generation;
    final int pageSize = RadioBrowserClient.PAGE_SIZE;
    searchClient.searchStations(
        query,
        nextOffset,
        pageSize,
        new RadioBrowserClient.StationsCallback() {
          @Override
          public void onStations(List<Stream> page) {
            if (requestGeneration != generation || isFinishing()) {
              return;
            }
            loading = false;
            showProgress(false);
            // the offset counts what radio-browser sent, duplicates included
            nextOffset += page.size();
            endReached = page.size() < pageSize;
            appendStreams(page);
            if (streams.isEmpty() && endReached) {
              Toast.makeText(
                      StreamFinderActivity.this, R.string.no_radios_matching, Toast.LENGTH_LONG)
                  .show();
            } else if (resultsList != null) {
              // a short list may not scroll at all; fill it until it does, or the results end
              resultsList.post(StreamFinderActivity.this::loadMoreIfNearEnd);
            }
          }

          @Override
          public void onError(int statusCode) {
            if (requestGeneration != generation || isFinishing()) {
              return;
            }
            // not endReached: scrolling again tries the same page again
            loading = false;
            showProgress(false);
            Toast.makeText(
                    StreamFinderActivity.this,
                    getString(R.string.error_retrieving_radios, statusCode),
                    Toast.LENGTH_LONG)
                .show();
          }
        });
  }

  private void appendStreams(List<Stream> page) {
    int start = streams.size();
    for (Stream stream : page) {
      String uuid = stream.getStationUuid();
      if (uuid.isEmpty() || seenUuids.add(uuid)) {
        streams.add(stream);
      }
    }
    adapter.notifyItemRangeInserted(start, streams.size() - start);
    updateTitle();
  }

  private void loadMoreIfNearEnd() {
    if (resultsList == null || loading || endReached) {
      return;
    }
    LinearLayoutManager layout = (LinearLayoutManager) resultsList.getLayoutManager();
    if (layout != null
        && layout.findLastVisibleItemPosition() >= streams.size() - LOAD_AHEAD) {
      loadNextPage();
    }
  }

  private void updateTitle() {
    if (resultsDialog == null) {
      return;
    }
    // radio-browser does not say how many there are in all; "+" means more may follow
    resultsDialog.setTitle(
        getString(
            endReached ? R.string.radios_found : R.string.radios_found_more, streams.size()));
  }

  private void showProgress(boolean show) {
    if (resultsProgress != null) {
      resultsProgress.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }
  }

  /** The user previewed or saved this station; radio-browser counts it toward its popularity. */
  public void countClick(Stream stream) {
    searchClient.countClick(stream.getStationUuid());
  }

  @Override
  protected void onStop() {
    super.onStop();
    adapter.stopPreview();
  }

  @Override
  protected void onDestroy() {
    generation++;
    searchClient.cancelAll();
    listClient.cancelAll();
    adapter.releasePreview();
    super.onDestroy();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == android.R.id.home) {
      onBackPressed();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private static class OnHelpClickListener implements View.OnClickListener {
    @Override
    public void onClick(View view) {
      AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(view.getContext());
      dialogBuilder.setTitle(R.string.about_the_streamfinder);
      dialogBuilder.setView(
          LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_streamfinder_help, null));
      dialogBuilder.setPositiveButton(
          R.string.dialog_button_ok, (dialog, which) -> dialog.cancel());
      dialogBuilder.show();
    }
  }

  private static class KeyboardHideOnClickListener implements View.OnClickListener {
    private final StreamFinderActivity activity;

    KeyboardHideOnClickListener(StreamFinderActivity activity) {
      this.activity = activity;
    }

    @Override
    public void onClick(View view) {
      InputMethodManager imm =
          (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
  }
}
