/*
 Copyright Cristian "ciuc" Starasciuc 2016
 cristi.ciuc@gmail.com
*/
package ro.antiprotv.radioclock.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.Objects;
import ro.antiprotv.radioclock.R;

/** Created by ciuc on 7/19/16. */
public class AboutActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_about);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    WebView foo = findViewById(R.id.aboutText);
    foo.loadDataWithBaseURL(
        null, getResources().getString(R.string.about_text), "text/html", "utf-8", null);
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
