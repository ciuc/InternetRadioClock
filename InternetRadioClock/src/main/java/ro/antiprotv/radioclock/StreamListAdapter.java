package ro.antiprotv.radioclock;

import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;
import java.util.logging.Logger;

public class StreamListAdapter extends RecyclerView.Adapter {
    private final LayoutInflater inflater;
    StreamFinderActivity streamFinderActivity;
    private List<Stream> streams;
    private Logger logger = Logger.getLogger(StreamListAdapter.class.getName());

    public StreamListAdapter(StreamFinderActivity context, List<Stream> streams) {
        inflater = LayoutInflater.from(context);
        this.streams = streams;
        this.streamFinderActivity = context;
    }

    public List<Stream> getStreams() {
        return streams;
    }

    public void setStreams(List<Stream> streams) {
        this.streams = streams;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.content_stream_list, parent, false);
        return new StreamViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        StreamViewHolder streamViewHolder = (StreamViewHolder) holder;
        Stream stream = streams.get(position);
        streamViewHolder.stream = stream;
        streamViewHolder.url = stream.getUrl();
        streamViewHolder.nameView.setText(stream.getName());
        streamViewHolder.countryView.setText(stream.getCountry());

    }

    @Override
    public int getItemCount() {
        return streams.size();
    }

    class StreamViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {

        private final TextView nameView;
        private final TextView countryView;
        private final TextView tagsView;
        private final View view;
        private String url;

        private Stream stream;

        StreamViewHolder(View view) {
            super(view);
            this.view = view;
            this.nameView = view.findViewById(R.id.name);
            this.countryView = view.findViewById(R.id.country);
            this.tagsView = view.findViewById(R.id.tags);
            final Spinner memory = view.findViewById(R.id.memory);

            this.view.setOnCreateContextMenuListener(this);

            final Button assignToMemoryButton = view.findViewById(R.id.assign_to_memory);
            assignToMemoryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    streamFinderActivity.assignUrlToMemory(stream.getUrl(), memory.getSelectedItem().toString());
                }
            });


        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {

        }

    }
}