package ro.antiprotv.radioclock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Objects;
import ro.antiprotv.radioclock.db.Station;
import ro.antiprotv.radioclock.service.ButtonManager;

/** My Radio List: one card per saved station, in the order of the clock's memory buttons. */
public class StationListAdapter extends ListAdapter<Station, StationListAdapter.ViewHolder> {

  /** What the user asked to do with a station. */
  public interface Listener {
    void onEdit(Station station);

    void onMove(Station station, int offset);

    void onRemove(Station station);
  }

  private static final DiffUtil.ItemCallback<Station> DIFF =
      new DiffUtil.ItemCallback<Station>() {
        @Override
        public boolean areItemsTheSame(@NonNull Station a, @NonNull Station b) {
          return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Station a, @NonNull Station b) {
          return a.position == b.position
              && a.url.equals(b.url)
              && a.label.equals(b.label)
              && Objects.equals(a.stationName, b.stationName);
        }
      };

  private final Listener listener;

  public StationListAdapter(Listener listener) {
    super(DIFF);
    this.listener = listener;
    setHasStableIds(true);
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).id;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new ViewHolder(
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_station, parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.bind(getItem(position), position, getItemCount());
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    private final TextView button;
    private final TextView label;
    private final TextView name;
    private final TextView url;
    private final TextView offClock;
    private final ImageButton moveUp;
    private final ImageButton moveDown;
    private final ImageButton remove;

    ViewHolder(View view) {
      super(view);
      button = view.findViewById(R.id.station_button);
      label = view.findViewById(R.id.station_label);
      name = view.findViewById(R.id.station_name);
      url = view.findViewById(R.id.station_url);
      offClock = view.findViewById(R.id.station_off_clock);
      moveUp = view.findViewById(R.id.station_move_up);
      moveDown = view.findViewById(R.id.station_move_down);
      remove = view.findViewById(R.id.station_remove);
    }

    void bind(Station station, int index, int count) {
      boolean onClock = station.position <= ButtonManager.MEMORIES;
      button.setText(String.valueOf(station.position));
      button.setBackgroundResource(
          onClock ? R.drawable.station_badge : R.drawable.station_badge_off_clock);
      button.setContentDescription(
          itemView.getContext().getString(R.string.radio_list_button, station.position));
      offClock.setVisibility(onClock ? View.GONE : View.VISIBLE);
      label.setText(station.label);
      boolean hasName = station.stationName != null && !station.stationName.isEmpty();
      name.setVisibility(hasName ? View.VISIBLE : View.GONE);
      name.setText(station.stationName);
      url.setText(station.url);

      setEnabled(moveUp, index > 0);
      setEnabled(moveDown, index < count - 1);
      itemView.setOnClickListener(v -> listener.onEdit(station));
      moveUp.setOnClickListener(v -> listener.onMove(station, -1));
      moveDown.setOnClickListener(v -> listener.onMove(station, 1));
      remove.setOnClickListener(v -> listener.onRemove(station));
    }

    private void setEnabled(ImageButton button, boolean enabled) {
      button.setEnabled(enabled);
      // a disabled arrow at the top or bottom of the list fades out
      button.setAlpha(enabled ? 1f : 0.25f);
    }
  }
}
