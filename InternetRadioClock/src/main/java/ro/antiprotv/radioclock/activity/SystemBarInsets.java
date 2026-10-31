package ro.antiprotv.radioclock.activity;

import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import ro.antiprotv.radioclock.R;

/**
 * Lays a toolbar activity out around the system bars, now that the app is drawn edge to edge. The
 * toolbar grows by the height of the status bar, so its color sits behind the white status bar
 * icons instead of the light page background; the rest of the page is kept clear of the
 * navigation bar.
 */
final class SystemBarInsets {

  private SystemBarInsets() {}

  static void apply(AppCompatActivity activity) {
    ViewGroup content = activity.findViewById(android.R.id.content);
    View root = content.getChildAt(0);
    Toolbar toolbar = activity.findViewById(R.id.toolbar);
    // Insets can be dispatched more than once, so always grow from the height the layout asked for.
    int toolbarHeight = toolbar.getLayoutParams().height;
    ViewCompat.setOnApplyWindowInsetsListener(
        root,
        (v, insets) -> {
          Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
          toolbar.setPadding(
              toolbar.getPaddingLeft(),
              systemBars.top,
              toolbar.getPaddingRight(),
              toolbar.getPaddingBottom());
          toolbar.getLayoutParams().height = toolbarHeight + systemBars.top;
          toolbar.requestLayout();
          return insets;
        });
  }
}
