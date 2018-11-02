package ro.antiprotv.radioclock;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class StreamFinderActivity extends AppCompatActivity {
    List<Stream> streams;
    StreamListAdapter adapter;
    private Logger logger = Logger.getLogger(StreamFinderActivity.class.getName());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream_finder);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final RecyclerView recyclerView = findViewById(R.id.stream_list_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        streams = new ArrayList<>();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        streams.add(new Stream("Guerrilla", getResources().getString(R.string.setting_default_stream1),"Romania", "Rock, Classic"));
        streams.add(new Stream("TNN", getResources().getString(R.string.setting_default_stream2), "Romania", "Rock, Classic"));
        streams.add(new Stream("BOB", getResources().getString(R.string.setting_default_stream3), "Romania", "Rock, Classic"));
        streams.add(new Stream("RCK", getResources().getString(R.string.setting_default_stream4), "Romania", "Rock, Classic"));

        adapter = new StreamListAdapter(this, streams);
        recyclerView.setAdapter(adapter);

        final Button findStreamButton = findViewById(R.id.find_stream);
        findStreamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fillInStreams();
            }
        });


    }

    private void fillInStreams() {
        streams.add(new Stream("Rock Fm", "USA", "www.rock.us", "Cool music"));
        adapter.notifyDataSetChanged();
    }

    protected void assignUrlToMemory(String url, String key) {

        int index = new Integer(key);
        switch (index) {
            case 1:
                key = getResources().getString(R.string.setting_key_stream1);
                break;
            case 2:
                key = getResources().getString(R.string.setting_key_stream2);
                break;
            case 3:
                key = getResources().getString(R.string.setting_key_stream3);
                break;
            case 4:
                key = getResources().getString(R.string.setting_key_stream4);
                break;
            case 5:
                key = getResources().getString(R.string.setting_key_stream5);
                break;
            case 6:
                key = getResources().getString(R.string.setting_key_stream6);
                break;
            case 7:
                key = getResources().getString(R.string.setting_key_stream7);
                break;
            case 8:
                key = getResources().getString(R.string.setting_key_stream8);
                break;
        }
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putString(key, url).apply();
    }

}
