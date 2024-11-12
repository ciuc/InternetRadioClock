package ro.antiprotv.radioclock.view;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import ro.antiprotv.radioclock.R;

public class AbstractVisualTimer extends View {
    protected final Paint fillPaint;
    protected final Paint strokePaint;
    protected int currentFill = 0;
    protected final int strokeWidth = 1; // Set your desired stroke width in pixels

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
        currentFill = (originalCounter - countingTime) * getWidth() / originalCounter;
        invalidate(); // Redraw the view with the updated width
    }
    public void reset() {
        currentFill = 0;
    }
}
