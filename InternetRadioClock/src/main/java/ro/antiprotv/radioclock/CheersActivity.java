/*
  Copyright Cristian "ciuc" Starasciuc 2016
  cristi.ciuc@gmail.com
 */
package ro.antiprotv.radioclock;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Objects;

/**
 * Created by ciuc on 7/19/16.
 */
public class CheersActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cheers);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        TextView foo = findViewById(R.id.cheersText);
        foo.setText(Html.fromHtml(getString(R.string.cheers_text)));
        Button back = findViewById(R.id.about_back);
        back.setOnClickListener(v -> onBackPressed());
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
}
