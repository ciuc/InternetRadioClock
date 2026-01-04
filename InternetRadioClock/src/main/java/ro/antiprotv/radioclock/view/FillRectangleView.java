package ro.antiprotv.radioclock.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import androidx.annotation.NonNull;

public class FillRectangleView extends AbstractVisualTimer {

  public FillRectangleView(Context context, AttributeSet attrs) {
    super(context, attrs);
    fillPaint.setAlpha(255);
  }

  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    super.onDraw(canvas);
    fillPaint.setAlpha(255);
    canvas.drawRoundRect(0, 0, getWidth() - 3, getHeight() - 5, 20, 20, strokePaint);
    canvas.drawRoundRect(0f, 0, currentFill, getHeight() - 5, 20, 20, fillPaint);
  }

  @Override
  public void startAnimate() {
    super.startFadeInOutAnimation();
  }
}
