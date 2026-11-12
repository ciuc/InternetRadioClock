package ro.antiprotv.radioclock;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.devbrackets.android.exomedia.AudioPlayer;
import com.google.android.material.textfield.TextInputEditText;
import androidx.appcompat.widget.TooltipCompat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ro.antiprotv.radioclock.activity.StreamFinderActivity;
import ro.antiprotv.radioclock.db.Station;
import ro.antiprotv.radioclock.db.StationRepository;
import ro.antiprotv.radioclock.service.ButtonManager;
import ro.antiprotv.radioclock.service.MediaPlayerService;
import timber.log.Timber;

public class StreamListAdapter extends RecyclerView.Adapter {
  private final LayoutInflater inflater;
  private final StreamFinderActivity streamFinderActivity;
  private final StationRepository stations;
  private final List<Stream> streams;
  /** The saved stations, by position; the first ones get a "replace" button on every row. */
  private List<Station> saved = new ArrayList<>();

  /** How long a preview plays once the stream has started. */
  private static final long PREVIEW_DURATION_MS = 5_000;

  /** A stream that has not started playing by then is reported as an error. */
  private static final long PREVIEW_CONNECT_TIMEOUT_MS = 15_000;

  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable stopPreviewTask = this::stopPreview;
  private final Runnable connectTimeoutTask = () -> onPreviewError(null);
  /** Outcome of the last preview of each station; stations never previewed are not in here. */
  private final Map<Stream, PreviewStatus> previewStatuses = new HashMap<>();
  private AudioPlayer previewPlayer;
  private Stream previewStream;

  public StreamListAdapter(StreamFinderActivity context, List<Stream> streams) {
    inflater = LayoutInflater.from(context);
    this.streams = streams;
    this.streamFinderActivity = context;
    this.stations = StationRepository.getInstance(context);
  }

  /** My Radio List changed: show a "replace" button for each of its first stations. */
  public void setStations(List<Station> saved) {
    this.saved = saved;
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View itemView = inflater.inflate(R.layout.content_stream_list, parent, false);
    return new StreamViewHolder(itemView);
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    StreamViewHolder streamViewHolder = (StreamViewHolder) holder;
    Stream stream = streams.get(position);
    streamViewHolder.stream = stream;
    streamViewHolder.url = stream.getUrl();
    streamViewHolder.nameView.setText(stream.getName());
    boolean previewing = stream == previewStream;
    streamViewHolder.previewButton.setImageResource(
        previewing
            ? R.drawable.baseline_stop_circle_outline_24
            : R.drawable.baseline_play_circle_outline_24);
    PreviewStatus status = previewStatuses.get(stream);
    if (status == null) {
      streamViewHolder.previewStatus.setVisibility(View.GONE);
    } else {
      streamViewHolder.previewStatus.setText(status.text);
      streamViewHolder.previewStatus.setTextColor(status.color);
      streamViewHolder.previewStatus.setVisibility(View.VISIBLE);
    }
    streamViewHolder.bindStations();
    streamViewHolder.countryView.setText(
        String.format(
            "[FROM: %s | IN: %s | PLAYING: %s%s]",
            stream.getCountry(), stream.getLanguage(), stream.getTags(), quality(stream)));
  }

  /** " | MP3 128 kbps", or as much of it as radio-browser knows. */
  private static String quality(Stream stream) {
    String codec = stream.getCodec();
    String bitrate = stream.getBitrate() > 0 ? stream.getBitrate() + " kbps" : "";
    String both = (codec + " " + bitrate).trim();
    return both.isEmpty() ? "" : " | " + both;
  }

  @Override
  public int getItemCount() {
    return streams.size();
  }

  /**
   * Plays the stream for {@link #PREVIEW_DURATION_MS}, then stops. Tapping the same stream again
   * stops it early; tapping another one switches the preview to it.
   */
  private void togglePreview(Stream stream) {
    if (stream == previewStream) {
      stopPreview();
      return;
    }
    // switching straight to another station: keep the radio paused in between
    stopPreview(false);
    MediaPlayerService.pauseForPreview();
    previewStatuses.put(stream, PreviewStatus.CHECKING);
    previewStream = stream;
    streamFinderActivity.countClick(stream);
    refresh(stream);

    if (previewPlayer == null) {
      previewPlayer = new AudioPlayer(streamFinderActivity.getApplicationContext());
      previewPlayer.setOnPreparedListener(this::onPreviewPrepared);
      previewPlayer.setOnErrorListener(
          e -> {
            onPreviewError(e);
            return true;
          });
    }
    handler.postDelayed(connectTimeoutTask, PREVIEW_CONNECT_TIMEOUT_MS);
    try {
      previewPlayer.setMedia(Uri.parse(stream.getUrl()));
    } catch (RuntimeException e) {
      onPreviewError(e);
    }
  }

  private void onPreviewPrepared() {
    if (previewStream == null || previewPlayer == null) {
      return;
    }
    handler.removeCallbacks(connectTimeoutTask);
    previewStatuses.put(previewStream, PreviewStatus.OK);
    refresh(previewStream);
    previewPlayer.start();
    handler.postDelayed(stopPreviewTask, PREVIEW_DURATION_MS);
  }

  private void onPreviewError(Exception e) {
    Stream failed = previewStream;
    Timber.w(e, "Preview failed for %s", failed == null ? null : failed.getUrl());
    stopPreview();
    if (failed != null) {
      previewStatuses.put(failed, PreviewStatus.ERROR);
      refresh(failed);
      Toast.makeText(streamFinderActivity, R.string.error_playing_stream, Toast.LENGTH_SHORT)
          .show();
    }
  }

  /**
   * Stops the preview, if one is running, and lets the clock's radio play again; the player is kept
   * for the next preview.
   */
  public void stopPreview() {
    stopPreview(true);
  }

  private void stopPreview(boolean resumeRadio) {
    handler.removeCallbacks(stopPreviewTask);
    handler.removeCallbacks(connectTimeoutTask);
    if (previewPlayer != null) {
      previewPlayer.reset();
    }
    Stream stopped = previewStream;
    previewStream = null;
    // stopped by the user before it connected: there is no verdict to show
    if (stopped != null && previewStatuses.get(stopped) == PreviewStatus.CHECKING) {
      previewStatuses.remove(stopped);
    }
    if (stopped != null) {
      refresh(stopped);
    }
    if (resumeRadio) {
      MediaPlayerService.resumeAfterPreview();
    }
  }

  /** Stops the preview and frees the player; call when the list goes away for good. */
  public void releasePreview() {
    stopPreview();
    if (previewPlayer != null) {
      previewPlayer.release();
      previewPlayer = null;
    }
  }

  /** The list is about to be replaced: forget previews and errors of the old stations. */
  public void clearPreviewState() {
    stopPreview();
    previewStatuses.clear();
  }

  private enum PreviewStatus {
    CHECKING(R.string.preview_checking, 0xFF757575),
    OK(R.string.preview_ok, 0xFF388E3C),
    ERROR(R.string.preview_error, 0xFFD32F2F);

    final int text;
    final int color;

    PreviewStatus(int text, int color) {
      this.text = text;
      this.color = color;
    }
  }

  private void refresh(Stream stream) {
    int position = streams.indexOf(stream);
    if (position >= 0) {
      notifyItemChanged(position);
    }
  }

  class StreamViewHolder extends RecyclerView.ViewHolder
      implements View.OnCreateContextMenuListener {

    private final TextView nameView;
    private final TextView countryView;
    private final ImageButton previewButton;
    private final TextView previewStatus;
    private String url;
    private TextView replaceLabel;
    private List<Button> replaceButtons;

    private Stream stream;

    StreamViewHolder(View view) {
      super(view);
      this.nameView = view.findViewById(R.id.name);
      this.countryView = view.findViewById(R.id.radio_description);
      view.setOnCreateContextMenuListener(this);
      this.previewButton = view.findViewById(R.id.sf_preview_button);
      this.previewStatus = view.findViewById(R.id.sf_preview_status);
      previewButton.setOnClickListener(
          v -> {
            if (stream != null) {
              togglePreview(stream);
            }
          });

      replaceLabel = view.findViewById(R.id.sf_replace_label);
      replaceButtons =
          Arrays.asList(
              view.findViewById(R.id.sf_assign_button_stream1),
              view.findViewById(R.id.sf_assign_button_stream2),
              view.findViewById(R.id.sf_assign_button_stream3),
              view.findViewById(R.id.sf_assign_button_stream4),
              view.findViewById(R.id.sf_assign_button_stream5),
              view.findViewById(R.id.sf_assign_button_stream6),
              view.findViewById(R.id.sf_assign_button_stream7),
              view.findViewById(R.id.sf_assign_button_stream8));
      for (int i = 0; i < replaceButtons.size(); i++) {
        final int index = i;
        replaceButtons.get(i).setOnClickListener(v -> showReplaceDialog(saved.get(index)));
      }
      Button addButton = view.findViewById(R.id.sf_add_button);
      addButton.setOnClickListener(v -> showAddDialog());
    }

    /** One button per saved station, up to the clock's 8 memory buttons. */
    void bindStations() {
      int shown = Math.min(saved.size(), ButtonManager.MEMORIES);
      replaceLabel.setVisibility(shown > 0 ? View.VISIBLE : View.GONE);
      for (int i = 0; i < replaceButtons.size(); i++) {
        Button button = replaceButtons.get(i);
        if (i < shown) {
          Station station = saved.get(i);
          button.setVisibility(View.VISIBLE);
          button.setText(String.valueOf(station.position));
          button.setContentDescription(station.label);
          // a long press tells which station sits on the button
          TooltipCompat.setTooltipText(button, station.label);
        } else {
          button.setVisibility(View.GONE);
        }
      }
    }

    @Override
    public void onCreateContextMenu(
        ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {}

    private String shortName() {
      return stream.getName().length() > 10
          ? stream.getName().substring(0, 10) + "..."
          : stream.getName();
    }

    /** Puts this stream in place of a saved station, after asking. */
    private void showReplaceDialog(Station station) {
      // "BOB (RADIO BOB - Livestream Hessen)": the button label, and the name when we know it
      String name =
          station.stationName == null || station.stationName.isEmpty()
              ? station.label
              : station.label + " (" + station.stationName + ")";
      LinearLayout layout = inflateLabelDialog(station.label);
      TextView question = layout.findViewById(R.id.textView);
      question.setText(R.string.sf_replace_confirm);
      final TextInputEditText labelInput =
          layout.findViewById(R.id.streamFinder_textinput_addDialog_label);
      new AlertDialog.Builder(streamFinderActivity)
          .setTitle(streamFinderActivity.getString(R.string.sf_replace_title, name, shortName()))
          .setView(layout)
          .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
          .setPositiveButton(
              R.string.yes,
              (dialog, which) -> {
                String label = labelOrElse(labelInput, station.label);
                stations.assign(
                    station.position,
                    stream.getUrl(),
                    label,
                    stream.getName(),
                    stream.getCountry());
                streamFinderActivity.countClick(stream);
                Toast.makeText(
                        streamFinderActivity,
                        streamFinderActivity.getString(
                            R.string.sf_replaced, stream.getName(), station.position),
                        Toast.LENGTH_SHORT)
                    .show();
              })
          .show();
    }

    /** Adds this stream at the end of My Radio List: the next free memory button. */
    private void showAddDialog() {
      int position = saved.size() + 1;
      LinearLayout layout = inflateLabelDialog(String.valueOf(position));
      TextView note = layout.findViewById(R.id.textView);
      if (position > ButtonManager.MEMORIES) {
        // past the clock's buttons: say so before the user adds it
        note.setText(
            streamFinderActivity.getString(R.string.add_android_auto_only, position));
        note.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        note.setTypeface(null, Typeface.ITALIC);
        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
      } else {
        note.setVisibility(View.GONE);
      }
      final TextInputEditText labelInput =
          layout.findViewById(R.id.streamFinder_textinput_addDialog_label);
      new AlertDialog.Builder(streamFinderActivity)
          .setTitle(streamFinderActivity.getString(R.string.sf_add_title, shortName()))
          .setView(layout)
          .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
          .setPositiveButton(
              R.string.yes,
              (dialog, which) -> {
                stations.add(
                    stream.getUrl(),
                    labelOrElse(labelInput, String.valueOf(position)),
                    stream.getName(),
                    stream.getCountry());
                streamFinderActivity.countClick(stream);
                Toast.makeText(
                        streamFinderActivity,
                        streamFinderActivity.getString(
                            R.string.sf_added, stream.getName(), position),
                        Toast.LENGTH_SHORT)
                    .show();
              })
          .show();
    }

    /** The dialog asking for the button label, suggested from the stream's name. */
    private LinearLayout inflateLabelDialog(String fallbackLabel) {
      LinearLayout layout =
          (LinearLayout)
              LayoutInflater.from(streamFinderActivity)
                  .inflate(R.layout.dialog_streamfinder_add_to_memory, null);
      TextInputEditText labelInput =
          layout.findViewById(R.id.streamFinder_textinput_addDialog_label);
      String suggested = calculateLabel(stream.getName());
      labelInput.setText(suggested.isEmpty() ? fallbackLabel : suggested);
      labelInput.setSelectAllOnFocus(true);
      return layout;
    }

    private String labelOrElse(TextInputEditText input, String fallback) {
      String label = input.getText() == null ? "" : input.getText().toString().trim();
      return label.isEmpty() ? fallback : label;
    }

    private String calculateLabel(String name) {
        String label = "";
        name = name.toUpperCase();
        name = name.replaceAll("FM", "");
        name = name.replaceAll("RADIO", "");
        name = name.replaceAll("THE", "");
        name =
            replaceDoubleChars(
                name, 'b', 'c', 'd', 'f', 'g', 'h', 'h', 'k', 'l', 'm', 'n', 'p', 'r', 's', 't',
                'v', 'w', 'x', 'z', 'y');
        name = name.replaceAll("[0-9.!?\\\\-]", "");

        name = name.replaceAll("[AEIOU ]", "");

        if (name.length() >= 4) {
          label = name.substring(0, 4);
        }
        if (label.equals("")) {
          if (name.length() == 3) {
            label = name.substring(0, 3);
          }
        }
        return label;
      }

      private String replaceDoubleChars(String name, char... chars) {
        for (char c : chars) {
          String regexp = String.format("%c%c", c, c);

          name = name.replaceAll(regexp.toUpperCase(), String.valueOf(c).toUpperCase());
        }
        return name;
      }
  }
}
