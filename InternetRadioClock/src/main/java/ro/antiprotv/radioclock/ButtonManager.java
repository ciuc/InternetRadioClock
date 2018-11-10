package ro.antiprotv.radioclock;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;

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
    //the button we have clicked on
    private Button mButtonClicked;

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
        return mButtonClicked;
    }

    public void setButtonClicked(Button mButtonClicked) {
        this.mButtonClicked = mButtonClicked;
    }

    void setText(int index, SharedPreferences newPrefs) {
        //prefs.getString(getResources().getString(R.string.setting_key_label4), getResources().getString(R.string.button_name_stream4)
        buttons.get(index).setText(newPrefs.getString(resources.getString(settingKeys.get(index)), resources.getString(defaultNames.get(index))));
        this.prefs = newPrefs;
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
