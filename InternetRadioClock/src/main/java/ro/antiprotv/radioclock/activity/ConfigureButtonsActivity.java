/*
 Copyright Cristian "ciuc" Starasciuc 2016
 cristi.ciuc@gmail.com
*/
package ro.antiprotv.radioclock.activity;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.Objects;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.fragment.ConfigureButtonsFragment;

/** Created by ciuc on 7/17/16. */
public class ConfigureButtonsActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_configure_buttons);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

    getFragmentManager()
        .beginTransaction()
        .replace(R.id.configure_buttons_content, new ConfigureButtonsFragment())
        .commit();
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
