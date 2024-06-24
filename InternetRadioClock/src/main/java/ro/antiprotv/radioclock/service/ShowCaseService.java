package ro.antiprotv.radioclock.service;

import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import ro.antiprotv.radioclock.R;

public class ShowCaseService {
  private static final int TITLE_SIZE = 40;
  private static final int DESCRIPTION_SIZE = 20;
  public static final int TARGET_RADIUS = 60;
  public static final float OUTER_CIRCLE_ALPHA = 0.96f;
  private final AppCompatActivity activity;

  public ShowCaseService(AppCompatActivity activity) {
    this.activity = activity;
    Toolbar toolbar = activity.findViewById(R.id.toolbar);
    ImageButton nightModeButton = activity.findViewById(R.id.night_mode_button);
    ImageButton onOffButton = activity.findViewById(R.id.on_off_button);

    // tap targets
    TapTarget tap_tgt_on_off =
        TapTarget.forView(onOffButton, "On/Off", "You can use this button to close the radio.")
            .outerCircleColor(R.color.colorPrimary) // Specify a color for the outer circle
            .outerCircleAlpha(OUTER_CIRCLE_ALPHA) // Specify the alpha amount for the outer circle
            .targetCircleColor(R.color.white) // Specify a color for the target circle
            .titleTextSize(TITLE_SIZE) // Specify the size (in sp) of the title text
            .titleTextColor(R.color.white) // Specify the color of the title text
            .descriptionTextSize(
                DESCRIPTION_SIZE) // Specify the size (in sp) of the description text
            .descriptionTextColor(R.color.white) // Specify the color of the description text
            .textColor(R.color.white) // Specify a color for both the title and description text
            .dimColor(
                R.color
                    .color_clock) // If set, will dim behind the view with 30% opacity of the given
            // color
            .drawShadow(true) // Whether to draw a drop shadow or not
            .cancelable(true) // Whether tapping outside the outer circle dismisses the view
            .tintTarget(true) // Whether to tint the target view's color
            .transparentTarget(
                true) // Specify whether the target is transparent (displays the content underneath)
            .targetRadius(TARGET_RADIUS); // Specify the target radius (in dp)

      TapTarget tap_tgt_layout_settings =
              TapTarget.forView(activity.findViewById(R.id.ui_settings_panel), "Ssss", "You can use this button to close the radio.")
                      .outerCircleColor(R.color.colorPrimary) // Specify a color for the outer circle
                      .outerCircleAlpha(OUTER_CIRCLE_ALPHA) // Specify the alpha amount for the outer circle
                      .targetCircleColor(R.color.white) // Specify a color for the target circle
                      .titleTextSize(TITLE_SIZE) // Specify the size (in sp) of the title text
                      .titleTextColor(R.color.white) // Specify the color of the title text
                      .descriptionTextSize(
                              DESCRIPTION_SIZE) // Specify the size (in sp) of the description text
                      .descriptionTextColor(R.color.white) // Specify the color of the description text
                      .textColor(R.color.white) // Specify a color for both the title and description text
                      .dimColor(
                              R.color
                                      .color_clock) // If set, will dim behind the view with 30% opacity of the given
                      // color
                      .drawShadow(true) // Whether to draw a drop shadow or not
                      .cancelable(true) // Whether tapping outside the outer circle dismisses the view
                      .tintTarget(true) // Whether to tint the target view's color
                      .transparentTarget(
                              true) // Specify whether the target is transparent (displays the content underneath)
                      .targetRadius(200); // Specify the target radius (in dp)

    TapTarget tap_tgt_night =
        TapTarget.forView(
                nightModeButton,
                "Night/Day Mode",
                "Click to toggle between night and day modes.")
            .outerCircleColor(R.color.colorPrimary) // Specify a color for the outer circle
            .outerCircleAlpha(OUTER_CIRCLE_ALPHA) // Specify the alpha amount for the outer circle
            .targetCircleColor(R.color.white) // Specify a color for the target circle
            .titleTextSize(TITLE_SIZE) // Specify the size (in sp) of the title text
            .titleTextColor(R.color.white) // Specify the color of the title text
            .descriptionTextSize(
                DESCRIPTION_SIZE) // Specify the size (in sp) of the description text
            .descriptionTextColor(R.color.white) // Specify the color of the description text
            .textColor(R.color.white) // Specify a color for both the title and description text
            .dimColor(
                R.color
                    .color_clock) // If set, will dim behind the view with 30% opacity of the given
            // color
            .drawShadow(true) // Whether to draw a drop shadow or not
            .tintTarget(true) // Whether to tint the target view's color
            .transparentTarget(
                true) // Specify whether the target is transparent (displays the content underneath)
            .cancelable(true)
            .targetRadius(TARGET_RADIUS); // Specify the target radius (in dp)

    TapTarget tap_tgt_close =
        TapTarget.forToolbarMenuItem(
                toolbar, R.id.close, "Exit", "This will exit the app and cleanup.")
            .outerCircleColor(R.color.colorPrimary) // Specify a color for the outer circle
            .outerCircleAlpha(OUTER_CIRCLE_ALPHA) // Specify the alpha amount for the outer circle
            .targetCircleColor(R.color.white) // Specify a color for the target circle
            .titleTextSize(TITLE_SIZE) // Specify the size (in sp) of the title text
            .titleTextColor(R.color.white) // Specify the color of the title text
            .descriptionTextSize(
                DESCRIPTION_SIZE) // Specify the size (in sp) of the description text
            .descriptionTextColor(R.color.white) // Specify the color of the description text
            .textColor(R.color.white) // Specify a color for both the title and description text
            .dimColor(
                R.color
                    .color_clock) // If set, will dim behind the view with 30% opacity of the given
            // color
            .drawShadow(true) // Whether to draw a drop shadow or not
            .tintTarget(true) // Whether to tint the target view's color
            .transparentTarget(
                true) // Specify whether the target is transparent (displays the content underneath)
            .cancelable(true)
            .targetRadius(TARGET_RADIUS); // Specify the target radius (in dp)

    tapTargetSequence =
        new TapTargetSequence(activity)
            .targets(tap_tgt_on_off, tap_tgt_night, tap_tgt_layout_settings, tap_tgt_close)
            .listener(
                new TapTargetSequence.Listener() {
                  @Override
                  public void onSequenceFinish() {
                      activity.findViewById(R.id.toolbar).setVisibility(View.GONE);
                  }

                  @Override
                  public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                    currentStep++;
                  }

                  @Override
                  public void onSequenceCanceled(TapTarget lastTarget) {
                      activity.findViewById(R.id.toolbar).setVisibility(View.GONE);
                  }
                });
  }

  private int currentStep = 0;
  private TapTargetSequence tapTargetSequence;

  public void showCase() {
    activity.findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
    tapTargetSequence.start();
  }

}
