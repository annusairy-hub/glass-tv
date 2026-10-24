package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;

/** Weather and air-quality pill under the clock, prayer-times panel above the tiles. */
public final class HomeInfo {
    private static final float PILL_LEFT_DP = 24f;
    private static final float PILL_TOP_DP = 122f;
    private static final float PANEL_RIGHT_DP = 48f;
    private static final float PANEL_BOTTOM_FRAC = 0.42f;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private static WeakReference<Pill> pillRef;
    private static WeakReference<Panel> panelRef;

    private HomeInfo() {}

    public static void attach(Activity a) {
        PrayerTimes.legacyOff = true;
        DisplayMetrics m = a.getResources().getDisplayMetrics();

        Pill pill = new Pill(a);
        FrameLayout.LayoutParams pl =
                new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP | Gravity.START);
        pl.leftMargin = Math.round(PILL_LEFT_DP * m.density);
        pl.topMargin = Math.round(PILL_TOP_DP * m.density);
        a.addContentView(pill, pl);

        Panel panel = new Panel(a);
        FrameLayout.LayoutParams ql =
                new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM | Gravity.END);
        ql.rightMargin = Math.round(PANEL_RIGHT_DP * m.density);
        ql.bottomMargin = Math.round(m.heightPixels * PANEL_BOTTOM_FRAC);
        a.addContentView(panel, ql);

        pillRef = new WeakReference<>(pill);
        panelRef = new WeakReference<>(panel);
    }

    public static void refresh(Activity a) {
        nudge();
        PrayerTimes.refreshIfDue(a, HomeInfo::nudge);
        Weather.refreshIfDue(a, HomeInfo::nudge);
    }

    private static void nudge() {
        Pill p = pillRef == null ? null : pillRef.get();
        if (p != null) p.update();
        Panel q = panelRef == null ? null : panelRef.get();
        if (q != null) q.update();
    }

    private static TextView label(Context c, float px, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        t.setTextColor(color);
        t.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        t.setShadowLayer(4f, 0f, 1f, 0xFF000000);
        t.setSingleLine(true);
        return t;
    }

    private static final class Pill extends LinearLayout {
        private final TextView text;

        Pill(Context c) {
            super(c);
            float h = c.getResources().getDisplayMetrics().heightPixels;
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setFocusable(false);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0x59000000);
            bg.setCornerRadius(h);
            setBackground(bg);
            int px = Math.round(h * 0.016f);
            int py = Math.round(h * 0.007f);
            setPadding(px, py, px, py);
            text = label(c, h * 0.026f, 0xF2FFFFFF, false);
            addView(text, new LinearLayout.LayoutParams(WRAP, WRAP));
            setVisibility(GONE);
        }

        void update() {
            Weather.Now n = Weather.read(getContext());
            if (n == null) {
                setVisibility(GONE);
                return;
            }
            SpannableStringBuilder sb = new SpannableStringBuilder();
            sb.append(Math.round(n.temp) + "\u00B0 " + Weather.label(n.code));
            sb.setSpan(new ForegroundColorSpan(0xF2FFFFFF), 0, sb.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (n.aqi >= 0) {
                int start = sb.length();
                sb.append("  \u00B7  AQI " + n.aqi + " " + Weather.aqiLabel(n.aqi));
                sb.setSpan(new ForegroundColorSpan(0x99FFFFFF), start, start + 5,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new ForegroundColorSpan(Weather.aqiColor(n.aqi)), start + 5,
                        sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            text.setText(sb);
            setVisibility(VISIBLE);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            update();
        }
    }

    private static final class Panel extends LinearLayout {
        private final TextView header;
        private final TextView[] names = new TextView[6];
        private final TextView[] times = new TextView[6];
        private final Runnable tick = new Runnable() {
            @Override
            public void run() {
                Context c = getContext();
                PrayerTimes.refreshIfDue(c, HomeInfo::nudge);
                Weather.refreshIfDue(c, HomeInfo::nudge);
                update();
                postDelayed(this, 30000L);
            }
        };

        Panel(Context c) {
            super(c);
            float h = c.getResources().getDisplayMetrics().heightPixels;
            setOrientation(VERTICAL);
            setGravity(Gravity.END);
            setFocusable(false);

            header = label(c, h * 0.019f, 0xB3FFFFFF, true);
            header.setLetterSpacing(0.12f);
            LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(WRAP, WRAP);
            hp.gravity = Gravity.END;
            hp.bottomMargin = Math.round(h * 0.012f);
            addView(header, hp);

            LinearLayout row = new LinearLayout(c);
            row.setOrientation(HORIZONTAL);
            for (int i = 0; i < 6; i++) {
                LinearLayout cell = new LinearLayout(c);
                cell.setOrientation(VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                names[i] = label(c, h * 0.021f, 0xB3FFFFFF, false);
                times[i] = label(c, h * 0.036f, 0xFFFFFFFF, true);
                cell.addView(names[i], new LinearLayout.LayoutParams(WRAP, WRAP));
                cell.addView(times[i], new LinearLayout.LayoutParams(WRAP, WRAP));
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(WRAP, WRAP);
                cp.leftMargin = i == 0 ? 0 : Math.round(h * 0.022f);
                row.addView(cell, cp);
            }
            addView(row, new LinearLayout.LayoutParams(WRAP, WRAP));
            setVisibility(GONE);
        }

        void update() {
            PrayerTimes.Day d = PrayerTimes.today(getContext());
            if (d == null) {
                setVisibility(GONE);
                return;
            }
            header.setText(d.hijri.isEmpty()
                    ? "WAKTU SOLAT" : "WAKTU SOLAT  \u00B7  " + d.hijri);
            for (int i = 0; i < 6; i++) {
                boolean nx = i == d.next;
                names[i].setText(d.names[i]);
                times[i].setText(d.times[i]);
                names[i].setTextColor(nx ? 0xFF4ADE80 : 0xB3FFFFFF);
                times[i].setTextColor(nx ? 0xFF4ADE80 : 0xFFFFFFFF);
            }
            setVisibility(VISIBLE);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            removeCallbacks(tick);
            post(tick);
        }

        @Override
        protected void onDetachedFromWindow() {
            removeCallbacks(tick);
            super.onDetachedFromWindow();
        }
    }
}
