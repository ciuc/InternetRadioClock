package ro.antiprotv.radioclock;

import android.app.AlertDialog;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ro.antiprotv.radioclock.activity.StreamFinderActivity;
import ro.antiprotv.radioclock.service.ButtonManager;
import ro.antiprotv.radioclock.service.MediaPlayerService;
import timber.log.Timber;

public class StreamListAdapter extends RecyclerView.Adapter {
  private final LayoutInflater inflater;
  private final StreamFinderActivity streamFinderActivity;
  private final ButtonManager buttonManager;
  private final List<Stream> streams;

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

  public StreamListAdapter(
      StreamFinderActivity context, ButtonManager buttonManager, List<Stream> streams) {
    inflater = LayoutInflater.from(context);
    this.streams = streams;
    this.streamFinderActivity = context;
    this.buttonManager = buttonManager;
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
    streamViewHolder.countryView.setText(
        String.format(
            "[FROM: %s | IN: %s | PLAYING: %s]",
            stream.getCountry(), stream.getLanguage(), stream.getTags()));
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

      final Button assignToMemoryButton1 = view.findViewById(R.id.sf_assign_button_stream1);
      assignToMemoryButton1.setOnClickListener(new OnAssignToMemoryClickListener(1));
      final Button assignToMemoryButton2 = view.findViewById(R.id.sf_assign_button_stream2);
      assignToMemoryButton2.setOnClickListener(new OnAssignToMemoryClickListener(2));
      final Button assignToMemoryButton3 = view.findViewById(R.id.sf_assign_button_stream3);
      assignToMemoryButton3.setOnClickListener(new OnAssignToMemoryClickListener(3));
      final Button assignToMemoryButton4 = view.findViewById(R.id.sf_assign_button_stream4);
      assignToMemoryButton4.setOnClickListener(new OnAssignToMemoryClickListener(4));
      final Button assignToMemoryButton5 = view.findViewById(R.id.sf_assign_button_stream5);
      assignToMemoryButton5.setOnClickListener(new OnAssignToMemoryClickListener(5));
      final Button assignToMemoryButton6 = view.findViewById(R.id.sf_assign_button_stream6);
      assignToMemoryButton6.setOnClickListener(new OnAssignToMemoryClickListener(6));
      final Button assignToMemoryButton7 = view.findViewById(R.id.sf_assign_button_stream7);
      assignToMemoryButton7.setOnClickListener(new OnAssignToMemoryClickListener(7));
      final Button assignToMemoryButton8 = view.findViewById(R.id.sf_assign_button_stream8);
      assignToMemoryButton8.setOnClickListener(new OnAssignToMemoryClickListener(8));
    }

    @Override
    public void onCreateContextMenu(
        ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {}

    private class OnAssignToMemoryClickListener implements View.OnClickListener {
      final int streamNo;

      OnAssignToMemoryClickListener(int streamNo) {
        this.streamNo = streamNo;
      }

      @Override
      public void onClick(View view) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(streamFinderActivity);
        dialogBuilder.setTitle(
            String.format(
                "Assign %s to memory %s",
                stream.getName().length() > 10
                    ? stream.getName().substring(0, 10) + "..."
                    : stream.getName(),
                streamNo));
        LinearLayout layout =
            (LinearLayout)
                LayoutInflater.from(view.getContext())
                    .inflate(R.layout.dialog_streamfinder_add_to_memory, null);
        final TextInputEditText labelInput =
            layout.findViewById(R.id.streamFinder_textinput_addDialog_label);
        labelInput.setText(calculateLabel(stream.getName()));
        labelInput.setSelectAllOnFocus(true);
        dialogBuilder.setView(layout);

        dialogBuilder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        dialogBuilder.setPositiveButton(
            R.string.yes,
            (dialog, which) ->
                buttonManager.assignUrlToMemory(
                    stream.getUrl(), streamNo, labelInput.getText().toString()));
        dialogBuilder.show();
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
}
