package ro.antiprotv.radioclock.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

public class FillCircleView extends AbstractVisualTimer {

  public FillCircleView(Context context, AttributeSet attrs) {
    super(context, attrs);
    fillPaint.setAlpha(50);
  }

  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    super.onDraw(canvas);
    int maxDim = Math.min(getWidth(), getHeight());
    int radius = maxDim / 2;
    canvas.drawArc(
        (float) getWidth() / 2 - radius,
        0,
        (float) getWidth() / 2 + radius,
        maxDim,
        270,
        currentFill,
        true,
        fillPaint);
    canvas.drawArc(
        (float) getWidth() / 2 - radius,
        0,
        (float) getWidth() / 2 + radius,
        maxDim,
        0,
        360,
        true,
        strokePaint);
  }

  public void updateFillWidth(int originalCounter, int countingTime) {
    currentFill = (originalCounter - countingTime) * 360 / originalCounter;
    invalidate(); // Redraw the view with the updated width
  }

  @Override
  public void stopAnimation() {
    super.stopAnimation();
    fillPaint.setAlpha(50);
  }
}
