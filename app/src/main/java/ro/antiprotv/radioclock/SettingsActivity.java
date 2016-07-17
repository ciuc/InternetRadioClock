package ro.antiprotv.radioclock;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by ciuc on 7/12/16.
 */
public class SettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
