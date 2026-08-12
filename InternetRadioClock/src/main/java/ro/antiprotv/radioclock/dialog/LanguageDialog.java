package ro.antiprotv.radioclock.dialog;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import java.util.Locale;
import ro.antiprotv.radioclock.R;
import ro.antiprotv.radioclock.Toaster;
import timber.log.Timber;

/**
 * Lets the user pick one of the languages the app is translated in (see xml/locales_config.xml).
 *
 * <p>The choice is applied with {@link AppCompatDelegate#setApplicationLocales(LocaleListCompat)},
 * which restarts the activity so the new language takes effect. On Android 13+ the system remembers
 * the per-app language; below that appcompat remembers it itself (see AppLocalesMetadataHolderService
 * in the manifest).
 */
public class LanguageDialog {

  private LanguageDialog() {}

  /** Shows the language chooser. */
  public static void show(@NonNull Context context) {
    final String[] tags = context.getResources().getStringArray(R.array.language_tags);
    String[] names = context.getResources().getStringArray(R.array.language_names);

    // The first entry is "System default", the rest are the languages the app is translated in.
    final CharSequence[] items = new CharSequence[names.length + 1];
    items[0] = context.getString(R.string.language_system_default);
    System.arraycopy(names, 0, items, 1, names.length);

    final int current = currentSelection(tags);
    new AlertDialog.Builder(context)
        .setTitle(R.string.language_dialog_title)
        .setSingleChoiceItems(
            items,
            current,
            (dialog, which) -> {
              dialog.dismiss();
              if (which != current) {
                apply(context, which == 0 ? null : tags[which - 1]);
              }
            })
        .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
        .show();
  }

  /** Index of the currently active language in the dialog list; 0 means "system default". */
  private static int currentSelection(String[] tags) {
    LocaleListCompat current = AppCompatDelegate.getApplicationLocales();
    if (current.isEmpty()) {
      return 0;
    }
    Locale locale = current.get(0);
    if (locale == null) {
      return 0;
    }
    String languageTag = locale.toLanguageTag();
    for (int i = 0; i < tags.length; i++) {
      if (tags[i].equalsIgnoreCase(languageTag)) {
        return i + 1;
      }
    }
    // No exact match (for instance pt-PT while we only ship pt-BR); match on the language only.
    for (int i = 0; i < tags.length; i++) {
      if (tags[i].split("-")[0].equalsIgnoreCase(locale.getLanguage())) {
        return i + 1;
      }
    }
    return 0;
  }

  /** Applies the language; a null tag means "follow the system language". */
  private static void apply(Context context, String tag) {
    Timber.d("Setting application locale to %s", tag == null ? "system default" : tag);
    Toaster.toast(context, context.getString(R.string.language_restart_hint), Toast.LENGTH_SHORT);
    AppCompatDelegate.setApplicationLocales(
        tag == null ? LocaleListCompat.getEmptyLocaleList() : LocaleListCompat.forLanguageTags(tag));
  }
}
