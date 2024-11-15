package ro.antiprotv.radioclock.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import ro.antiprotv.radioclock.R;
import timber.log.Timber;

public class AbstractVisualTimer extends View {
  protected final Paint fillPaint;
  protected final Paint strokePaint;
  protected int currentFill = 0;
  protected final int strokeWidth = 1; // Set your desired stroke width in pixels
  private ValueAnimator colorAnimator;
  private AlphaAnimation blinkAnimation;

  public AbstractVisualTimer(Context context, AttributeSet attrs) {
    super(context, attrs);

    // Initialize the fill paint
    fillPaint = new Paint();
    fillPaint.setColor(
        getResources().getColor(R.color.color_clock)); // Fill color for the rectangle
    fillPaint.setStyle(Paint.Style.FILL);

    strokePaint = new Paint();
    strokePaint.setColor(
        getResources().getColor(R.color.color_clock)); // Fill color for the rectangle
    strokePaint.setStyle(Paint.Style.STROKE);
    strokePaint.setStrokeWidth(strokeWidth);
  }

  public void updateFillWidth(int originalCounter, int countingTime) {
    if (countingTime < 0) {
      return;
    }

    currentFill = (originalCounter - countingTime) * getWidth() / originalCounter;
    Timber.d(
        "originalCounter: %d, countingTime: %d, currentFill: %d",
        originalCounter, countingTime, currentFill);
    invalidate(); // Redraw the view with the updated width
  }

  public void reset() {
    currentFill = 0;
  }

  boolean isBlinking;

  public void startAnimate() {
    startColorAnimation();
  }

  public void startBlinkingAnimation() {
    if (isBlinking) {
      return;
    }
    // Create an AlphaAnimation instance for blinking effect
    blinkAnimation = new AlphaAnimation(0.0f, 1.0f); // From invisible to visible
    blinkAnimation.setDuration(200); // Duration of one blink (500ms)
    blinkAnimation.setStartOffset(10); // Delay before starting the next blink
    blinkAnimation.setRepeatMode(Animation.REVERSE); // Reverse the animation at the end
    blinkAnimation.setRepeatCount(Animation.INFINITE); // Repeat indefinitely

    // Start the animation on the TextView
    startAnimation(blinkAnimation);
    isBlinking = true;
  }

  public void startColorAnimation() {
    if (isBlinking) {
      return;
    }
    // Create an AlphaAnimation instance for blinking effect
    colorAnimator = ValueAnimator.ofArgb(Color.BLUE, Color.RED);
    colorAnimator.setDuration(200); // Duration of the animation (1000 ms = 1 second)
    colorAnimator.setRepeatCount(ValueAnimator.INFINITE); // Repeat indefinitely
    colorAnimator.setRepeatMode(ValueAnimator.REVERSE); // Reverse the animation to go back to green

    // Update the TextView's text color with each frame of the animation
    colorAnimator.addUpdateListener(
        animator -> {
          fillPaint.setColor((int) animator.getAnimatedValue());
          invalidate();
        });

    // Start the animation
    colorAnimator.start();
    isBlinking = true;
  }

  public void stopAnimation() {
    isBlinking = false;
    if (colorAnimator != null) {
      colorAnimator.cancel();
    }
    if (blinkAnimation != null) {
      blinkAnimation.cancel();
    }
    fillPaint.setColor(getResources().getColor(R.color.color_clock));
    invalidate();
  }
}
