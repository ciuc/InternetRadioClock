package ro.antiprotv.radioclock;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
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
        streamViewHolder.tagsView.setText(stream.getTags());
        streamViewHolder.languageView.setText(stream.getLanguage());
    }

    @Override
    public int getItemCount() {
        return streams.size();
    }

    class StreamViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {

        private final TextView nameView;
        private final TextView countryView;
        private final TextView tagsView;
        private final TextView languageView;
        private final View view;
        private String url;

        private Stream stream;

        StreamViewHolder(View view) {
            super(view);
            this.view = view;
            this.nameView = view.findViewById(R.id.name);
            this.countryView = view.findViewById(R.id.country);
            this.tagsView = view.findViewById(R.id.tags);
            this.languageView = view.findViewById(R.id.language);
            this.view.setOnCreateContextMenuListener(this);

            final Button assignToMemoryButton1 = view.findViewById(R.id.sf_assign_button_stream1);
            assignToMemoryButton1.setOnClickListener(new OnAssignToMemoryClickListener("1"));
            final Button assignToMemoryButton2 = view.findViewById(R.id.sf_assign_button_stream2);
            assignToMemoryButton2.setOnClickListener(new OnAssignToMemoryClickListener("2"));
            final Button assignToMemoryButton3 = view.findViewById(R.id.sf_assign_button_stream3);
            assignToMemoryButton3.setOnClickListener(new OnAssignToMemoryClickListener("3"));
            final Button assignToMemoryButton4 = view.findViewById(R.id.sf_assign_button_stream4);
            assignToMemoryButton4.setOnClickListener(new OnAssignToMemoryClickListener("4"));
            final Button assignToMemoryButton5 = view.findViewById(R.id.sf_assign_button_stream5);
            assignToMemoryButton5.setOnClickListener(new OnAssignToMemoryClickListener("5"));
            final Button assignToMemoryButton6 = view.findViewById(R.id.sf_assign_button_stream6);
            assignToMemoryButton6.setOnClickListener(new OnAssignToMemoryClickListener("6"));
            final Button assignToMemoryButton7 = view.findViewById(R.id.sf_assign_button_stream7);
            assignToMemoryButton7.setOnClickListener(new OnAssignToMemoryClickListener("7"));
            final Button assignToMemoryButton8 = view.findViewById(R.id.sf_assign_button_stream8);
            assignToMemoryButton8.setOnClickListener(new OnAssignToMemoryClickListener("8"));

            final LinearLayout radioInfo = view.findViewById(R.id.radio_info);
            final TextView name = view.findViewById(R.id.name);
            name.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(StreamListAdapter.class.getName(), radioInfo.toString());
                    if (radioInfo.isShown()) {
                        slide_up(streamFinderActivity, radioInfo);
                        radioInfo.setVisibility(View.GONE);
                    } else {
                        slide_down(streamFinderActivity, radioInfo);
                        radioInfo.setVisibility(View.VISIBLE);
                    }
                }
            });

        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {

        }

        private class OnAssignToMemoryClickListener implements View.OnClickListener {
            String streamNo;

            OnAssignToMemoryClickListener(String streamNo) {
                this.streamNo = streamNo;
            }

            @Override
            public void onClick(View view) {

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(streamFinderActivity);
                dialogBuilder.setTitle(String.format("Are you sure want to assign %s to memory %s?", stream.getName().length() > 10 ? stream.getName().substring(0, 10) : stream.getName(), streamNo));
                dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                dialogBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        streamFinderActivity.assignUrlToMemory(stream.getUrl(), streamNo);
                    }
                });
                dialogBuilder.show();
            }
        }

    }

    public static void slide_down(StreamFinderActivity ctx, View v) {
        Animation a = AnimationUtils.loadAnimation(ctx, R.anim.slide_down);
        if (a != null) {
            a.reset();
            if (v != null) {
                v.clearAnimation();
                v.startAnimation(a);
            }
        }
    }

    public static void slide_up(StreamFinderActivity ctx, View v) {
        Animation a = AnimationUtils.loadAnimation(ctx, R.anim.slide_up);
        if (a != null) {
            a.reset();
            if (v != null) {
                v.clearAnimation();
                v.startAnimation(a);
            }
        }
    }
}