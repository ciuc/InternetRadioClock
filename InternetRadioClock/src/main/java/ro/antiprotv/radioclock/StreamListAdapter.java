package ro.antiprotv.radioclock;

import android.app.AlertDialog;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import com.google.android.material.textfield.TextInputEditText;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

class StreamListAdapter extends RecyclerView.Adapter {
    private final LayoutInflater inflater;
    private final StreamFinderActivity streamFinderActivity;
    private final ButtonManager buttonManager;
    private List<Stream> streams;

    public StreamListAdapter(StreamFinderActivity context, ButtonManager buttonManager, List<Stream> streams) {
        inflater = LayoutInflater.from(context);
        this.streams = streams;
        this.streamFinderActivity = context;
        this.buttonManager = buttonManager;
    }

    public void setStreams(List<Stream> streams) {
        this.streams = streams;
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
        streamViewHolder.countryView.setText(String.format("[FROM: %s | IN: %s | PLAYING: %s]", stream.getCountry(), stream.getLanguage(), stream.getTags()));
    }

    @Override
    public int getItemCount() {
        return streams.size();
    }

    class StreamViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {

        private final TextView nameView;
        private final TextView countryView;
        private final View view;
        private String url;

        private Stream stream;

        StreamViewHolder(View view) {
            super(view);
            this.view = view;
            this.nameView = view.findViewById(R.id.name);
            this.countryView = view.findViewById(R.id.radio_description);
            this.view.setOnCreateContextMenuListener(this);

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
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {

        }

        private class OnAssignToMemoryClickListener implements View.OnClickListener {
            final int streamNo;

            OnAssignToMemoryClickListener(int streamNo) {
                this.streamNo = streamNo;
            }

            @Override
            public void onClick(View view) {

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(streamFinderActivity);
                dialogBuilder.setTitle(String.format("Assign %s to memory %s", stream.getName().length() > 10 ? stream.getName().substring(0, 10)+"..." : stream.getName(), streamNo));
                LinearLayout layout = (LinearLayout) LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_streamfinder_add_to_memory, null);
                final TextInputEditText labelInput = layout.findViewById(R.id.streamFinder_textinput_addDialog_label);
                labelInput.setText(calculateLabel(stream.getName()));
                labelInput.setSelectAllOnFocus(true);
                dialogBuilder.setView(layout);

                dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                dialogBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        buttonManager.assignUrlToMemory(stream.getUrl(), streamNo, labelInput.getText().toString());
                    }
                });
                dialogBuilder.show();
            }

            private String calculateLabel(String name) {
                String label ="";
                name = name.toUpperCase();
                name = name.replaceAll("FM", "");
                name = name.replaceAll("RADIO", "");
                name = name.replaceAll("THE", "");
                name = replaceDoubleChars(name, 'b','c','d','f','g','h','h','k','l','m','n','p','r','s','t','v','w','x','z','y');
                name = name.replaceAll("[0-9.!?\\\\-]", "");

                name = name.replaceAll("[AEIOU ]","");


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
                for (char c: chars) {
                    String regexp = String.format("%c%c", c,c);

                    name = name.replaceAll(regexp.toUpperCase(), String.valueOf(c).toUpperCase());
                }
                return name;
            }
        }

    }
}