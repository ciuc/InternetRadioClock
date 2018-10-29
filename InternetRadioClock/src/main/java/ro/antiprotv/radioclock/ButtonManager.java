package ro.antiprotv.radioclock;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.prefs.Preferences;
import timber.log.Timber;

public class ButtonManager {
    private Context context;
    private View view;
    private SharedPreferences prefs;
    private View.OnTouchListener onTouchListener;
    private View.OnClickListener playListener;
    final Resources resources;
    List<Button> buttons;

    protected ButtonManager(Context ctx, View view, SharedPreferences prefs,View.OnTouchListener onTouchListener, View.OnClickListener playListener) {
        this.context = ctx;
        this.view = view;
        this.prefs = prefs;
        this.onTouchListener = onTouchListener;
        this.playListener = playListener;
        resources = context.getResources();
    }

    protected List<Button> initializeButtons(HashMap<String, String> mUrls){
        Button stream1 = (Button) view.findViewById(R.id.stream1);
        Button stream2 = (Button) view.findViewById(R.id.stream2);
        Button stream3 = (Button) view.findViewById(R.id.stream3);
        Button stream4 = (Button) view.findViewById(R.id.stream4);
        Button stream5 = (Button) view.findViewById(R.id.stream5);
        Button stream6 = (Button) view.findViewById(R.id.stream6);
        Button stream7 = (Button) view.findViewById(R.id.stream7);
        Button stream8 = (Button) view.findViewById(R.id.stream8);
        buttons = Arrays.asList(stream1, stream2, stream3, stream4);

        List<Integer> settingKeys = Arrays.asList(
                R.string.setting_key_label1,
                R.string.setting_key_label2,
                R.string.setting_key_label3,
                R.string.setting_key_label4,
                R.string.setting_key_label5,
                R.string.setting_key_label6,
                R.string.setting_key_label7,
                R.string.setting_key_label8
        );
        List<Integer> defaultNames = Arrays.asList(
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
        return buttons;
    }

    protected void hideUnhideButtons(HashMap<String, String> mUrls) {
        List<String> urls = Arrays.asList(
                mUrls.get(resources.getString(R.string.setting_key_stream1)),
                mUrls.get(resources.getString(R.string.setting_key_stream2)),
                mUrls.get(resources.getString(R.string.setting_key_stream3)),
                mUrls.get(resources.getString(R.string.setting_key_stream4)),
                mUrls.get(resources.getString(R.string.setting_key_stream5)),
                mUrls.get(resources.getString(R.string.setting_key_stream6)),
                mUrls.get(resources.getString(R.string.setting_key_stream7)),
                mUrls.get(resources.getString(R.string.setting_key_stream8))
        );
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

    //the button we have clicked on
    private Button mButtonClicked;
    public Button getButtonClicked() {
        return mButtonClicked;
    }

    public void setButtonClicked(Button mButtonClicked) {
        this.mButtonClicked = mButtonClicked;
    }

    /**
     *  Set disabled to all buttons
     *  (cycle through buttons and .setEnabled false)
     */
    protected void disableButtons() {
        for (Button button : buttons) {
            button.setEnabled(false);
        }
    }

    /**
     *  Set enabled to all buttons
     *  (cycle through buttons and .setEnabled)
     */
    protected void enableButtons() {
        for (Button button : buttons) {
            button.setEnabled(true);
        }
    }

    protected void resetButtons() {
        for (Button button : buttons) {
            button.setEnabled(true);
            button.setTextColor(resources.getColor(R.color.button_color_off));
        }
    }

    protected Button findButtonByTag(String tag) {
        for (Button button : buttons) {
            if (button.getTag().equals(tag)) {
                return button;
            }
        }
        return null;
    }

    protected void lightButton() {
        for (Button button : buttons) {
            button.setTextColor(resources.getColor(R.color.button_color_off));
            GradientDrawable buttonShape = (GradientDrawable) button.getBackground();
            buttonShape.setStroke(1, resources.getColor(R.color.button_color));
        }
        Timber.d(ClockActivity.TAG_RADIOCLOCK, mButtonClicked);
        mButtonClicked.setTextColor(resources.getColor(R.color.color_clock));
        GradientDrawable buttonShape = (GradientDrawable) mButtonClicked.getBackground();
        buttonShape.setStroke(1, resources.getColor(R.color.color_clock));
    }

}
