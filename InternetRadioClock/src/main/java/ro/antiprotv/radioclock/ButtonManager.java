package ro.antiprotv.radioclock;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import com.google.android.material.textfield.TextInputEditText;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

class ButtonManager {
    private final Resources resources;
    private List<Button> buttons;
    private List<Integer> settingKeys;
    private List<Integer> defaultNames;
    private final Context context;
    private final View view;
    private SharedPreferences prefs;
    private final View.OnTouchListener onTouchListener;
    private final View.OnClickListener playListener;
    private View.OnLongClickListener addEditLabelClickListener;
    //the button we have clicked on
    private Button mButtonClicked;

    ButtonManager(Context ctx) {
        this(ctx, null, null, null, null);
    }
    ButtonManager(Context ctx, View view, SharedPreferences prefs, View.OnTouchListener onTouchListener, View.OnClickListener playListener) {
        this.context = ctx;
        this.view = view;
        this.prefs = prefs;
        this.onTouchListener = onTouchListener;
        this.playListener = playListener;
        resources = context.getResources();
    }

    void initializeButtons(List<String> mUrls) {
        Button stream1 = view.findViewById(R.id.stream1);
        Button stream2 = view.findViewById(R.id.stream2);
        Button stream3 = view.findViewById(R.id.stream3);
        Button stream4 = view.findViewById(R.id.stream4);
        Button stream5 = view.findViewById(R.id.stream5);
        Button stream6 = view.findViewById(R.id.stream6);
        Button stream7 = view.findViewById(R.id.stream7);
        Button stream8 = view.findViewById(R.id.stream8);
        buttons = Arrays.asList(stream1, stream2, stream3, stream4, stream5, stream6, stream7, stream8);

        settingKeys = Arrays.asList(
                R.string.setting_key_label1,
                R.string.setting_key_label2,
                R.string.setting_key_label3,
                R.string.setting_key_label4,
                R.string.setting_key_label5,
                R.string.setting_key_label6,
                R.string.setting_key_label7,
                R.string.setting_key_label8
        );
        defaultNames = Arrays.asList(
                R.string.button_name_stream1,
                R.string.button_name_stream2,
                R.string.button_name_stream3,
                R.string.button_name_stream4,
                R.string.button_name_stream5,
                R.string.button_name_stream6,
                R.string.button_name_stream7,
                R.string.button_name_stream8
        );

        for (int i = 0; i < buttons.size(); i++) {
            Button b = buttons.get(i);
            b.setText(prefs.getString(resources.getString(settingKeys.get(i)), resources.getString(defaultNames.get(i))));
            b.setOnClickListener(playListener);
            b.setOnTouchListener(onTouchListener);
            b.setOnLongClickListener(addEditLabelClickListener);
            b.setOnLongClickListener(new AddLabelOnLongClickListener(i+1));
        }
        hideUnhideButtons(mUrls);
        enableButtons();
    }

    void hideUnhideButtons(List<String> urls) {
        for (int i = 0; i < buttons.size(); i++) {
            Button b = buttons.get(i);
            String url = urls.get(i);
            if (b.getVisibility() == View.GONE && !url.isEmpty()) {
                b.setVisibility(View.VISIBLE);
            } else if (b.getVisibility() == View.VISIBLE && url.isEmpty()) {
                b.setVisibility(View.GONE);
            }

        }
    }

    public Button getButtonClicked() {
        if (mButtonClicked == null) {
            mButtonClicked = findButtonByTag(prefs.getString(ClockActivity.LAST_PLAYED,"setting.key.stream1"));
        }
        return mButtonClicked;
    }

    public void setButtonClicked(Button mButtonClicked) {
        this.mButtonClicked = mButtonClicked;
        prefs.edit().putString(ClockActivity.LAST_PLAYED, mButtonClicked.getTag().toString()).apply();
    }

    void setText(int index, SharedPreferences newPrefs) {
        buttons.get(index).setText(newPrefs.getString(resources.getString(settingKeys.get(index)), resources.getString(defaultNames.get(index))));
        this.prefs = newPrefs;
    }

    void assignUrlToMemory(String url, int streamNo, String label) {
        String key = "setting.key.stream"+ streamNo;
        String labelKey = "setting.key.label"+ streamNo;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(key, url).apply();
        prefs.edit().putString(labelKey, label).apply();
        Toast.makeText(context, String.format("%s assigned to memory %d", url, streamNo), Toast.LENGTH_SHORT).show();
    }

    private class AddLabelOnLongClickListener implements View.OnLongClickListener {
        int streamNo;

        AddLabelOnLongClickListener(int stream) {
            streamNo = stream;
        }
        @Override
        public boolean onLongClick(View view) {
            final Button button = (Button) view;
            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            LinearLayout layout = (LinearLayout) LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_edit_radio_label, null);
            final TextInputEditText labelInput = layout.findViewById(R.id.streamFinder_textinput_labelDialog_label);
            labelInput.setText(button.getText());
            labelInput.setSelectAllOnFocus(true);
            builder.setView(layout);
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setButtonLabel(streamNo, labelInput.getText().toString());
                }
            });
            builder.setNeutralButton(R.string.remove, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setButtonUrl(streamNo, "");
                }
            });
            builder.show();
            return true;
        }
    }

    void setButtonLabel(int streamNo, String label) {
        String labelKey = "setting.key.label"+ streamNo;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(labelKey, label).apply();
    }

    void setButtonUrl(int streamNo, String url) {
        String labelKey = "setting.key.stream"+ streamNo;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(labelKey, url).apply();
    }

    /**
     * Set disabled to all buttons
     * (cycle through buttons and .setEnabled false)
     */
    void disableButtons() {
        for (Button button : buttons) {
            button.setEnabled(false);
        }
    }

    /**
     * Set enabled to all buttons
     * (cycle through buttons and .setEnabled)
     */
    void enableButtons() {
        for (Button button : buttons) {
            button.setEnabled(true);
        }
    }

    void resetButtons() {
        for (Button button : buttons) {
            button.setEnabled(true);
            button.setTextColor(resources.getColor(R.color.button_color_off));
            GradientDrawable buttonShape = (GradientDrawable) button.getBackground();
            buttonShape.setStroke(1, resources.getColor(R.color.button_color));
        }
    }

    Button findButtonByTag(String tag) {
        for (Button button : buttons) {
            if (button.getTag().equals(tag)) {
                return button;
            }
        }
        return null;
    }

    void lightButton() {
        resetButtons();
        mButtonClicked.setTextColor(resources.getColor(R.color.color_clock));
        GradientDrawable buttonShape = (GradientDrawable) mButtonClicked.getBackground();
        buttonShape.setStroke(1, resources.getColor(R.color.color_clock));
    }

    void unlightButton() {
        Button clicked = getButtonClicked();
        if (clicked != null) {
            clicked.setTextColor(context.getResources().getColor(R.color.button_color_off));
            GradientDrawable buttonShape = (GradientDrawable) clicked.getBackground();
            buttonShape.setStroke(1, context.getResources().getColor(R.color.button_color));
        }
    }
}
