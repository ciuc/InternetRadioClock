/*
 Copyright Cristian "ciuc" Starasciuc 2016
 cristi.ciuc@gmail.com
*/
package ro.antiprotv.radioclock.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import java.util.Objects;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.fragment.SettingsFragment;

/** Created by ciuc on 7/17/16. */
public class SettingsActivity extends AppCompatActivity
    implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_settings);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    setTitle(R.string.title_settings);
    Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.settings_content, new SettingsFragment())
        .commit();
    View rootLayout = findViewById(R.id.layout_root_activity_settings);
    ViewCompat.setOnApplyWindowInsetsListener(rootLayout, new OnApplyWindowInsetsListener() {
      @NonNull
      @Override
      public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
        Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
        v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
        return insets;
      }
    });
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == android.R.id.home) {
      Toolbar toolbar = findViewById(R.id.toolbar);
      toolbar.setTitle(R.string.title_settings);
      onBackPressed();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
    // Instantiate the new Fragment.
    Toolbar toolbar = findViewById(R.id.toolbar);
    toolbar.setTitle(pref.getTitle());
    final Bundle args = pref.getExtras();
    final Fragment fragment =
        getSupportFragmentManager()
            .getFragmentFactory()
            .instantiate(getClassLoader(), pref.getFragment());
    fragment.setArguments(args);
    fragment.setTargetFragment(caller, 0);
    // Replace the existing Fragment with the new Fragment.
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.settings_content, fragment)
        .addToBackStack(null)
        .commit();
    return true;
  }

}
