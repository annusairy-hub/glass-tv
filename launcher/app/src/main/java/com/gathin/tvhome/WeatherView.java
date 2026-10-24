package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.View;

/** Small weather card: animated icon + temperature. Drawn in a 150 x 54 dp box. */
public final class WeatherView extends View {
    private static final int CLEAR = 0, PARTLY = 1, CLOUDY = 2, RAIN = 3, STORM = 4;
    private static final String[] LABELS =
        {"Cerah", "Separa berawan", "Berawan", "Hujan", "Ribut petir"};
    private static final float[] STAR_X = {1.1f, 0.9f, 1.45f};
    private static final float[] STAR_Y = {-0.9f, 0.8f, 0.1f};

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path bolt = new Path();
    private final Path moon = new Path();
    private final float u;
    private int kind = CLEAR;
    private boolean day = true;
    private String temp = "";
    private boolean running;

    public WeatherView(Context ctx) {
        super(ctx);
        u = ctx.getResources().getDisplayMetrics().density;
        setFocusable(false);
        big.setColor(0xF2FFFFFF);
        big.setTextSize(24f);
        big.setTypeface(Typeface.DEFAULT_BOLD);
        big.setShadowLayer(3f, 0f, 1f, 0xFF000000);
        small.setColor(0xB3FFFFFF);
        small.setTextSize(10f);
        small.setShadowLayer(3f, 0f, 1f, 0xFF000000);
        Path a = new Path();
        a.addCircle(0f, 0f, 1f, Path.Direction.CW);
        Path b = new Path();
        b.addCircle(0.55f, -0.25f, 0.85f, Path.Direction.CW);
        moon.op(a, b, Path.Op.DIFFERENCE);
    }

    public void setWeather(float tempC, int code, boolean isDay) {
        if (code == 0) kind = CLEAR;
        else if (code == 1 || code == 2) kind = PARTLY;
        else if (code >= 95) kind = STORM;
        else if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) kind = RAIN;
        else kind = CLOUDY;
        day = isDay;
        temp = Math.round(tempC) + "\u00B0";
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(Math.round(150f * u), Math.round(54f * u));
    }

    @Override
    protected void onWindowVisibilityChanged(int v) {
        super.onWindowVisibilityChanged(v);
        running = v == VISIBLE;
        if (running) invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        running = getWindowVisibility() == VISIBLE;
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        running = false;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas c) {
        long t = SystemClock.uptimeMillis();
        c.save();
        c.scale(u, u);
        switch (kind) {
            case CLEAR:
                if (day) sun(c, t, 27f, 27f, 9f); else moon(c, t, 27f, 27f, 11f);
                break;
            case PARTLY:
                if (day) sun(c, t, 19f, 19f, 7f); else moon(c, t, 19f, 19f, 9f);
                cloud(c, 28f + 2f * (float) Math.sin(t / 1800.0), 35f, 0.85f, 0xFFE8EDF5);
                break;
            case CLOUDY:
                cloud(c, 21f + 3f * (float) Math.sin(t / 2200.0), 20f, 0.7f, 0xFF9AA5B5);
                cloud(c, 30f - 3f * (float) Math.sin(t / 2600.0), 34f, 0.9f, 0xFFD5DCE8);
                break;
            case RAIN:
                cloud(c, 27f, 19f, 0.9f, 0xFF9AA5B5);
                drops(c, t, 6);
                break;
            default:
                cloud(c, 27f, 19f, 0.9f, 0xFF5F6877);
                drops(c, t, 4);
                lightning(c, t);
                break;
        }
        c.drawText(temp, 58f, 28f, big);
        c.drawText(LABELS[kind], 58f, 43f, small);
        c.restore();
        if (running) postInvalidateDelayed(40);
    }

    private void sun(Canvas c, long t, float cx, float cy, float r) {
        float pulse = (float) Math.sin(t / 700.0);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFFFC83D);
        p.setAlpha(60);
        c.drawCircle(cx, cy, r * 1.7f + pulse * r * 0.12f, p);
        p.setColor(0xFFFFC83D);
        c.drawCircle(cx, cy, r, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f);
        p.setStrokeCap(Paint.Cap.ROUND);
        float a0 = (t % 12000L) / 12000f * 6.2831855f;
        for (int i = 0; i < 8; i++) {
            float a = a0 + i * 0.7853982f;
            float cs = (float) Math.cos(a);
            float sn = (float) Math.sin(a);
            c.drawLine(cx + cs * r * 1.45f, cy + sn * r * 1.45f,
                    cx + cs * r * 1.85f, cy + sn * r * 1.85f, p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    private void moon(Canvas c, long t, float cx, float cy, float r) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFEDEFF7);
        c.save();
        c.translate(cx, cy);
        c.scale(r, r);
        c.drawPath(moon, p);
        c.restore();
        for (int i = 0; i < 3; i++) {
            float tw = 0.5f + 0.5f * (float) Math.sin(t / 500.0 + i * 2.1);
            p.setColor(0xFFFFFFFF);
            p.setAlpha((int) (60 + 195 * tw));
            c.drawCircle(cx + r * STAR_X[i], cy + r * STAR_Y[i], 1.2f, p);
        }
    }

    private void cloud(Canvas c, float x, float y, float s, int color) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        c.drawCircle(x - 13f * s, y, 10f * s, p);
        c.drawCircle(x, y - 7f * s, 13f * s, p);
        c.drawCircle(x + 14f * s, y, 9f * s, p);
        c.drawRoundRect(x - 23f * s, y - 1f * s, x + 23f * s, y + 10f * s, 6f * s, 6f * s, p);
    }

    private void drops(Canvas c, long t, int n) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f);
        p.setStrokeCap(Paint.Cap.ROUND);
        float gap = 30f / (n - 1);
        for (int i = 0; i < n; i++) {
            float ph = ((t + i * 113L) % 650L) / 650f;
            float x = 14f + i * gap;
            float y = 31f + ph * 17f;
            p.setColor(0xFF7CC4FF);
            p.setAlpha((int) (255 * (1f - ph)));
            c.drawLine(x, y, x - 1.5f, y + 5f, p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    private void lightning(Canvas c, long t) {
        long m = t % 3200L;
        if (!(m < 120L || (m > 260L && m < 340L))) return;
        bolt.reset();
        bolt.moveTo(29f, 26f);
        bolt.lineTo(22f, 39f);
        bolt.lineTo(27f, 39f);
        bolt.lineTo(23f, 52f);
        bolt.lineTo(34f, 35f);
        bolt.lineTo(29f, 35f);
        bolt.lineTo(33f, 26f);
        bolt.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFFFE066);
        c.drawPath(bolt, p);
    }
}
