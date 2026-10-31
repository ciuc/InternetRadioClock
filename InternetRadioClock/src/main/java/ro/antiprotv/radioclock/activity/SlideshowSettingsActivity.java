package ro.antiprotv.radioclock.activity;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.Objects;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.fragment.SettingsSlideshowFragment;

public class SlideshowSettingsActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_slideshow_settings);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    SystemBarInsets.apply(this);
    Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.slideshow_settings_content, new SettingsSlideshowFragment())
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
