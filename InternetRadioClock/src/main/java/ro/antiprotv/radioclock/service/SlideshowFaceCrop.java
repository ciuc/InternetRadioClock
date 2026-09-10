package ro.antiprotv.radioclock.service;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

/**
 * A centre crop that is not centred: it fills the view the same way, but around a chosen point
 * rather than the middle of the picture. The slideshow points it at the faces, so that a portrait
 * photograph on a landscape screen keeps the heads instead of a band across the middle.
 *
 * <p>The crop still cannot leave the picture, so a point near an edge only moves the frame as far
 * as there is picture to move it over.
 */
public class SlideshowFaceCrop extends BitmapTransformation {

  private static final String ID = "ro.antiprotv.radioclock.service.SlideshowFaceCrop";
  private static final byte[] ID_BYTES = ID.getBytes(Key.CHARSET);

  private final float focusX;
  private final float focusY;

  /**
   * @param focusX where the frame should be centred horizontally, as a fraction of the width
   * @param focusY the same vertically
   */
  public SlideshowFaceCrop(float focusX, float focusY) {
    this.focusX = focusX;
    this.focusY = focusY;
  }

  @Override
  protected Bitmap transform(
      @NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
    final int sourceWidth = toTransform.getWidth();
    final int sourceHeight = toTransform.getHeight();
    if (outWidth <= 0 || outHeight <= 0 || sourceWidth <= 0 || sourceHeight <= 0) {
      return toTransform;
    }

    // The same scale a centre crop would use: whichever of the two axes has to stretch further.
    float scale = Math.max((float) outWidth / sourceWidth, (float) outHeight / sourceHeight);
    int cropWidth = Math.min(sourceWidth, Math.max(1, Math.round(outWidth / scale)));
    int cropHeight = Math.min(sourceHeight, Math.max(1, Math.round(outHeight / scale)));
    int left = clamp(Math.round(focusX * sourceWidth - cropWidth / 2f), 0, sourceWidth - cropWidth);
    int top = clamp(Math.round(focusY * sourceHeight - cropHeight / 2f), 0, sourceHeight - cropHeight);

    Bitmap.Config config =
        toTransform.getConfig() != null ? toTransform.getConfig() : Bitmap.Config.ARGB_8888;
    Bitmap result = pool.get(outWidth, outHeight, config);
    result.setHasAlpha(toTransform.hasAlpha());
    Canvas canvas = new Canvas(result);
    canvas.drawBitmap(
        toTransform,
        new Rect(left, top, left + cropWidth, top + cropHeight),
        new Rect(0, 0, outWidth, outHeight),
        new Paint(Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG));
    // The canvas outlives this method otherwise, and with it the bitmap it is drawing into.
    canvas.setBitmap(null);
    return result;
  }

  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  @Override
  public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
    messageDigest.update(ID_BYTES);
    messageDigest.update(ByteBuffer.allocate(8).putFloat(focusX).putFloat(focusY).array());
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof SlideshowFaceCrop)) {
      return false;
    }
    SlideshowFaceCrop that = (SlideshowFaceCrop) other;
    return that.focusX == focusX && that.focusY == focusY;
  }

  @Override
  public int hashCode() {
    return ID.hashCode() + 31 * Float.floatToIntBits(focusX) + 961 * Float.floatToIntBits(focusY);
  }
}
