/**
 * Copyright Cristian "ciuc" Starasciuc 2016
 * cristi.ciuc@gmail.com
 */
package ro.antiprotv.radioclock;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;

/**
 * Created by ciuc on 7/19/16.
 */
public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("AboutActivity", "oncreate");
        setContentView(R.layout.activity_about);
        TextView foo = (TextView) findViewById(R.id.aboutText);
        foo.setText(Html.fromHtml(getString(R.string.about_text)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("AboutActivity", "oinstart");
    }
}
