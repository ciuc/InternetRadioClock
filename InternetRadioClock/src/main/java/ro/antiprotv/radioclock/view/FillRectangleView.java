package ro.antiprotv.radioclock.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

public class FillRectangleView extends AbstractVisualTimer {

  public FillRectangleView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    canvas.drawRoundRect(0, 0, getWidth() - 3, getHeight() - 5, 20, 20, strokePaint);
    canvas.drawRoundRect(0f, 0, currentFill, getHeight() - 5, 20, 20, fillPaint);
  }
}
