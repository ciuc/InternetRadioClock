package ro.antiprotv.radioclock.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import java.util.Arrays;
import java.util.Random;

/**
 * Fireworks and confetti, drawn over the whole screen for a few seconds.
 *
 * <p>Shown when a slideshow file comes up while the anniversary effect is on. It covers the clock
 * as well, which is the point: for those few seconds the celebration is what the screen is for.
 *
 * <p>Never takes a touch: the view is not clickable, so every tap falls through to the clock it is
 * drawn over and nothing about the normal controls changes while it is up.
 */
public class AnniversaryOverlayView extends View {

  /** What {@link #start(long)} falls back to, and the bounds it holds a caller to. */
  public static final long DEFAULT_DURATION_MS = 3000;

  public static final long MIN_DURATION_MS = 2000;
  public static final long MAX_DURATION_MS = 10000;

  /** The tail end of a run, spent fading out, so the last sparks are not simply cut off. */
  private static final long FADE_OUT_MS = 600;

  /** How long a rocket takes to rise to the point where it goes off. */
  private static final long LAUNCH_MS = 380;

  /** How long the opening rockets are spread over; after that they are recycled as they die. */
  private static final long OPENING_VOLLEY_MS = 1500;

  /** The gap a spent rocket waits before going up again, at its narrowest and its widest. */
  private static final long MIN_RELAUNCH_GAP_MS = 180;

  private static final long MAX_RELAUNCH_GAP_MS = 700;

  private static final int SPARKS_PER_BURST = 70;

  /** What {@link Density#LIGHT} puts in the air; the heavier settings are multiples of these. */
  private static final int BASE_ROCKET_COUNT = 5;

  private static final int BASE_CONFETTI_COUNT = 120;

  /** How much of the celebration there is. */
  public enum Density {
    LIGHT(1),
    MEDIUM(2),
    FULL(3);

    /** How many times as many rockets and pieces of confetti as {@link #LIGHT} gets. */
    final int multiplier;

    Density(int multiplier) {
      this.multiplier = multiplier;
    }

    public static Density fromValue(String value) {
      try {
        return valueOf(value.toUpperCase());
      } catch (IllegalArgumentException | NullPointerException e) {
        return LIGHT;
      }
    }
  }

  /** Pull on everything that is in the air, in dp per second per second. */
  private static final float GRAVITY = 720f;

  /** What is left of a spark's speed after a second of air resistance. */
  private static final float SPARK_DRAG_PER_SECOND = 0.12f;

  /** A frame this long or longer is treated as this long: a stall must not teleport the sparks. */
  private static final long MAX_FRAME_MS = 50;

  private static final int[] COLORS = {
    0xFFFFD54F, // amber
    0xFFFF7043, // deep orange
    0xFFEC407A, // pink
    0xFFAB47BC, // purple
    0xFF42A5F5, // blue
    0xFF26C6DA, // cyan
    0xFF66BB6A, // green
    0xFFFFF176, // pale yellow
    0xFFFFFFFF // white
  };

  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Random random = new Random();
  private final float displayDensity;

  /** Grown to fit when a heavier {@link Density} is asked for, and then kept. */
  private Burst[] bursts = new Burst[0];

  private Confetto[] confetti = new Confetto[0];

  private boolean running;

  /** Set once the particles have been placed, which needs the view size and so waits for it. */
  private boolean seeded;

  /** How long the run now on screen lasts; see {@link #start(long, Density)}. */
  private long durationMs = DEFAULT_DURATION_MS;

  /** How much of a celebration the run now on screen is. */
  private Density density = Density.LIGHT;

  private long startedAt;
  private long lastFrameAt;
  /** Time since the start of the run, kept for the parts that are drawn against the clock. */
  private long elapsed;

  public AnniversaryOverlayView(Context context) {
    this(context, null);
  }

  public AnniversaryOverlayView(Context context, AttributeSet attrs) {
    super(context, attrs);
    displayDensity = getResources().getDisplayMetrics().density;
    paint.setStrokeCap(Paint.Cap.ROUND);
    setVisibility(GONE);
  }

  /**
   * Starts the effect from the beginning, whether or not it is already running: every file that
   * comes up gets a whole run of its own, not what was left of the last one's.
   *
   * <p>The rockets and the confetti keep coming for however long the run lasts - they are recycled
   * as they are spent - so any length within the bounds looks the same as any other, only longer.
   *
   * @param durationMs how long to celebrate for, held to {@link #MIN_DURATION_MS} and {@link
   *     #MAX_DURATION_MS}
   * @param density how much of a celebration to make of it
   */
  public void start(long durationMs, Density density) {
    this.durationMs = Math.max(MIN_DURATION_MS, Math.min(MAX_DURATION_MS, durationMs));
    this.density = density != null ? density : Density.LIGHT;
    allocate(this.density);
    running = true;
    // Nothing is placed yet - onDraw does that, once it knows how big the screen is.
    seeded = false;
    setVisibility(VISIBLE);
    postInvalidateOnAnimation();
  }

  /** Takes the effect off the screen at once, with nothing left running behind it. */
  public void stop() {
    if (!running && getVisibility() == GONE) {
      return;
    }
    running = false;
    seeded = false;
    setVisibility(GONE);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    // A start that arrived before the view had been laid out is waiting for this.
    if (running) {
      postInvalidateOnAnimation();
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    stop();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (!running) {
      return;
    }
    final int width = getWidth();
    final int height = getHeight();
    if (width == 0 || height == 0) {
      // Not laid out yet; onSizeChanged asks for another frame once it is.
      return;
    }

    final long now = AnimationUtils.currentAnimationTimeMillis();
    if (!seeded) {
      seed(width, height);
      startedAt = now;
      lastFrameAt = now;
      seeded = true;
    }

    elapsed = now - startedAt;
    if (elapsed >= durationMs) {
      stop();
      return;
    }

    final float dt = Math.min(now - lastFrameAt, MAX_FRAME_MS) / 1000f;
    lastFrameAt = now;

    // Everything dims together over the last stretch, so the effect leaves rather than stops.
    final float fade =
        elapsed > durationMs - FADE_OUT_MS ? (durationMs - elapsed) / (float) FADE_OUT_MS : 1f;

    final int pieces = confettiCount();
    for (int i = 0; i < pieces; i++) {
      confetti[i].update(dt, width, height);
      confetti[i].draw(canvas, paint, fade);
    }
    final int rockets = rocketCount();
    for (int i = 0; i < rockets; i++) {
      bursts[i].update(elapsed, dt, width, height);
      bursts[i].draw(canvas, paint, fade);
    }

    postInvalidateOnAnimation();
  }

  /**
   * Makes sure there are enough rockets and pieces of confetti for the density asked for.
   *
   * <p>The pools only ever grow. Going back to a lighter setting simply leaves the spare ones
   * unused, which costs a little memory and saves rebuilding them all over again on a setting the
   * user may well be flicking back and forth.
   */
  private void allocate(Density density) {
    int rockets = BASE_ROCKET_COUNT * density.multiplier;
    if (bursts.length < rockets) {
      Burst[] grown = Arrays.copyOf(bursts, rockets);
      for (int i = bursts.length; i < rockets; i++) {
        grown[i] = new Burst();
      }
      bursts = grown;
    }
    int pieces = BASE_CONFETTI_COUNT * density.multiplier;
    if (confetti.length < pieces) {
      Confetto[] grown = Arrays.copyOf(confetti, pieces);
      for (int i = confetti.length; i < pieces; i++) {
        grown[i] = new Confetto();
      }
      confetti = grown;
    }
  }

  /** How many of the pool are in play, which is all of them unless a heavier run grew it. */
  private int rocketCount() {
    return Math.min(bursts.length, BASE_ROCKET_COUNT * density.multiplier);
  }

  private int confettiCount() {
    return Math.min(confetti.length, BASE_CONFETTI_COUNT * density.multiplier);
  }

  /** Places the rockets and the confetti for one run. */
  private void seed(int width, int height) {
    int rockets = rocketCount();
    for (int i = 0; i < rockets; i++) {
      // Spread evenly across the opening, so a heavier setting fills the same stretch of sky more
      // thickly rather than taking longer to get going.
      bursts[i].reset(width, height, (long) (OPENING_VOLLEY_MS * i / (float) rockets));
    }
    int pieces = confettiCount();
    for (int i = 0; i < pieces; i++) {
      confetti[i].reset(width, height);
    }
  }

  private float dp(float value) {
    return value * displayDensity;
  }

  private int pickColor() {
    return COLORS[random.nextInt(COLORS.length)];
  }

  /** A rocket: it rises, and then it goes off into {@link #SPARKS_PER_BURST} sparks. */
  private class Burst {
    private float x;
    private float targetY;
    private float fromY;
    private long launchAt;
    private boolean exploded;
    private int color;

    private final float[] sparkX = new float[SPARKS_PER_BURST];
    private final float[] sparkY = new float[SPARKS_PER_BURST];
    private final float[] sparkPrevX = new float[SPARKS_PER_BURST];
    private final float[] sparkPrevY = new float[SPARKS_PER_BURST];
    private final float[] sparkVx = new float[SPARKS_PER_BURST];
    private final float[] sparkVy = new float[SPARKS_PER_BURST];
    private final float[] sparkLife = new float[SPARKS_PER_BURST];
    private final float[] sparkLifespan = new float[SPARKS_PER_BURST];
    private final int[] sparkColor = new int[SPARKS_PER_BURST];

    void reset(int width, int height, long launchAt) {
      this.launchAt = launchAt;
      this.exploded = false;
      this.color = pickColor();
      this.x = width * (0.15f + random.nextFloat() * 0.7f);
      this.fromY = height;
      this.targetY = height * (0.15f + random.nextFloat() * 0.35f);
      Arrays.fill(sparkLife, 0f);
    }

    void update(long sinceStart, float dt, int width, int height) {
      if (!exploded) {
        if (sinceStart >= launchAt + LAUNCH_MS) {
          explode();
        }
        return;
      }
      final float gravity = dp(GRAVITY);
      final float drag = (float) Math.pow(SPARK_DRAG_PER_SECOND, dt);
      boolean anyLeft = false;
      for (int i = 0; i < SPARKS_PER_BURST; i++) {
        if (sparkLife[i] <= 0) {
          continue;
        }
        anyLeft = true;
        sparkPrevX[i] = sparkX[i];
        sparkPrevY[i] = sparkY[i];
        sparkVx[i] *= drag;
        sparkVy[i] = sparkVy[i] * drag + gravity * dt;
        sparkX[i] += sparkVx[i] * dt;
        sparkY[i] += sparkVy[i] * dt;
        sparkLife[i] -= dt;
      }
      if (!anyLeft) {
        relaunch(sinceStart, width, height);
      }
    }

    /**
     * Sends a spent rocket up again, so that a long run keeps going off rather than emptying the
     * sky after the opening volley. Nothing is started that would still be climbing when the run
     * ends: a rocket the user would never see go off is not worth launching.
     */
    private void relaunch(long sinceStart, int width, int height) {
      long gap =
          MIN_RELAUNCH_GAP_MS
              + (long) (random.nextDouble() * (MAX_RELAUNCH_GAP_MS - MIN_RELAUNCH_GAP_MS));
      if (sinceStart + gap + LAUNCH_MS >= durationMs - FADE_OUT_MS) {
        return;
      }
      reset(width, height, sinceStart + gap);
    }

    private void explode() {
      exploded = true;
      final float baseSpeed = dp(220f + random.nextFloat() * 160f);
      // Every other burst is a single colour, which is what most real ones look like; the rest
      // get a colour per spark, so a run of them does not all look the same.
      final boolean oneColour = random.nextBoolean();
      for (int i = 0; i < SPARKS_PER_BURST; i++) {
        double angle = random.nextDouble() * 2 * Math.PI;
        // The square root spreads the sparks through the disc rather than bunching them at its
        // edge, which is what one speed for all of them would do.
        float speed = baseSpeed * (0.25f + 0.75f * (float) Math.sqrt(random.nextDouble()));
        sparkX[i] = x;
        sparkY[i] = targetY;
        sparkPrevX[i] = x;
        sparkPrevY[i] = targetY;
        sparkVx[i] = (float) Math.cos(angle) * speed;
        sparkVy[i] = (float) Math.sin(angle) * speed;
        sparkLifespan[i] = 0.9f + random.nextFloat() * 0.7f;
        sparkLife[i] = sparkLifespan[i];
        sparkColor[i] = oneColour ? color : pickColor();
      }
    }

    void draw(Canvas canvas, Paint paint, float fade) {
      if (!exploded) {
        drawRocket(canvas, paint, fade);
        return;
      }
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(dp(2.5f));
      for (int i = 0; i < SPARKS_PER_BURST; i++) {
        if (sparkLife[i] <= 0) {
          continue;
        }
        float remaining = sparkLife[i] / sparkLifespan[i];
        int alpha = (int) (255 * fade * remaining * remaining);
        if (alpha <= 0) {
          continue;
        }
        paint.setColor(sparkColor[i]);
        paint.setAlpha(alpha);
        // Drawn as the step just taken rather than as a dot: a fast spark then reads as a streak.
        canvas.drawLine(sparkPrevX[i], sparkPrevY[i], sparkX[i], sparkY[i], paint);
      }
    }

    /** The rising dot, drawn only between the rocket setting off and going off. */
    private void drawRocket(Canvas canvas, Paint paint, float fade) {
      float progress = (elapsed - launchAt) / (float) LAUNCH_MS;
      if (progress < 0 || progress > 1) {
        return;
      }
      // Eased, so the rocket slows as it reaches the top the way a real one does.
      float eased = 1f - (1f - progress) * (1f - progress);
      float y = fromY + (targetY - fromY) * eased;
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(dp(3f));
      paint.setColor(color);
      paint.setAlpha((int) (255 * fade));
      canvas.drawLine(x, y + dp(14f), x, y, paint);
    }
  }

  /** One piece of paper, falling and turning over as it goes. */
  private class Confetto {
    private float x;
    private float y;
    private float fallSpeed;
    private float width;
    private float height;
    private float swayAmplitude;
    private float swayFrequency;
    private float swayPhase;
    private float spin;
    private float spinSpeed;
    private int color;
    private float screenHeight;

    void reset(int screenWidth, int screenHeight) {
      this.screenHeight = screenHeight;
      randomize(screenWidth);
      // Spread well above the screen, so the confetti streams in over the opening moments rather
      // than arriving all at once.
      y = -random.nextFloat() * screenHeight * 1.2f;
    }

    /**
     * Sends a piece that has fallen off the bottom back over the top as a fresh one, which is what
     * keeps the fall going for as long as the run lasts however long that is.
     */
    private void recycle(int screenWidth, int screenHeight) {
      this.screenHeight = screenHeight;
      randomize(screenWidth);
      y = -height - random.nextFloat() * screenHeight * 0.3f;
    }

    private void randomize(int screenWidth) {
      x = random.nextFloat() * screenWidth;
      fallSpeed = dp(200f + random.nextFloat() * 260f);
      width = dp(5f + random.nextFloat() * 5f);
      height = dp(3f + random.nextFloat() * 4f);
      swayAmplitude = dp(6f + random.nextFloat() * 14f);
      swayFrequency = 1.5f + random.nextFloat() * 2.5f;
      swayPhase = random.nextFloat() * (float) Math.PI * 2;
      spin = random.nextFloat() * 360f;
      spinSpeed = (random.nextBoolean() ? 1 : -1) * (120f + random.nextFloat() * 360f);
      color = pickColor();
    }

    void update(float dt, int screenWidth, int screenHeight) {
      this.screenHeight = screenHeight;
      swayPhase += swayFrequency * dt;
      y += fallSpeed * dt;
      x += (float) Math.cos(swayPhase) * swayAmplitude * dt;
      spin += spinSpeed * dt;
      if (y - height > screenHeight) {
        recycle(screenWidth, screenHeight);
      }
    }

    void draw(Canvas canvas, Paint paint, float fade) {
      if (y < 0 || y > screenHeight) {
        return;
      }
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(color);
      paint.setAlpha((int) (255 * fade));
      canvas.save();
      canvas.translate(x, y);
      canvas.rotate(spin);
      // Squashing it by the turn it has made is what makes a flat piece of paper look like one.
      float flip = (float) Math.cos(Math.toRadians(spin * 1.7f));
      float scaleX = flip < 0 ? -1f : 1f;
      canvas.scale(scaleX * Math.max(0.15f, Math.abs(flip)), 1f);
      canvas.drawRect(-width / 2, -height / 2, width / 2, height / 2, paint);
      canvas.restore();
    }
  }
}
