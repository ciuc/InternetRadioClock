package ro.antiprotv.radioclock;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

/**
 * Created by ciuc on 7/19/16.
 */
public class AboutActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        Log.d("xxx", "ccc");
        super.onCreate(savedInstanceState, persistentState);
        TextView mContentView = (TextView) findViewById(R.id.fullscreen_content);
        setContentView(mContentView);
    }
}
