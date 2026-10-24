package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.TimeZone;

/** One hadith above the prayer panel, a different one each day. */
public final class QuoteView extends LinearLayout {
    private static final float RIGHT_DP = 48f;
    private static final float BOTTOM_FRAC = 0.55f;

    // {source, Arabic, Malay}. Add more rows here, each one checked against a trusted source.
    private static final String[][] QUOTES = {
        {"HR. BUKHARI & MUSLIM",
         "إِنَّمَا الْأَعْمَالُ بِالنِّيَّاتِ وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى",
         "Sesungguhnya setiap amalan itu bergantung pada niatnya, dan setiap orang akan mendapat apa yang dia niatkan."},
        {"HR. MUSLIM",
         "أَنْ تَعْبُدَ اللَّهَ كَأَنَّكَ تَرَاهُ فَإِنَّكَ إِنْ لَا تَرَاهُ فَإِنَّهُ يَرَاكَ",
         "Ihsan ialah engkau beribadah kepada Allah seolah-olah engkau melihat-Nya; jika engkau tidak melihat-Nya, sesungguhnya Dia melihatmu."}
    };

    public static void attach(Activity a) {
        DisplayMetrics m = a.getResources().getDisplayMetrics();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        lp.rightMargin = Math.round(RIGHT_DP * m.density);
        lp.bottomMargin = Math.round(m.heightPixels * BOTTOM_FRAC);
        a.addContentView(new QuoteView(a), lp);
    }

    private QuoteView(Context c) {
        super(c);
        DisplayMetrics m = c.getResources().getDisplayMetrics();
        float h = m.heightPixels;
        int maxW = Math.round(m.widthPixels * 0.5f);
        setOrientation(VERTICAL);
        setGravity(Gravity.END);
        setFocusable(false);

        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
        String[] q = QUOTES[now.get(Calendar.DAY_OF_YEAR) % QUOTES.length];

        TextView src = text(c, q[0], h * 0.019f, 0xB3FFFFFF, true, maxW);
        src.setLetterSpacing(0.12f);
        TextView ar = text(c, q[1], h * 0.034f, 0xFFFFFFFF, true, maxW);
        ar.setTextDirection(View.TEXT_DIRECTION_RTL);
        ar.setLineSpacing(0f, 1.25f);
        TextView ms = text(c, "\u201C" + q[2] + "\u201D", h * 0.023f, 0xE6FFFFFF, false, maxW);

        addView(src, params(0));
        addView(ar, params(Math.round(h * 0.008f)));
        addView(ms, params(Math.round(h * 0.008f)));
    }

    private static LinearLayout.LayoutParams params(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.gravity = Gravity.END;
        p.topMargin = top;
        return p;
    }

    private static TextView text(Context c, String s, float px, int color, boolean bold, int maxW) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        t.setTextColor(color);
        t.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        t.setShadowLayer(4f, 0f, 1f, 0xFF000000);
        t.setGravity(Gravity.END);
        t.setMaxWidth(maxW);
        return t;
    }
}
