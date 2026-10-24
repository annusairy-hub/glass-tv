package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** Waktu solat (JAKIM e-Solat, Langkawi). Fetched once a day in the background. */
public final class PrayerTimes {
    private static final String ZONE = "KDH06";
    private static final String API =
        "https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=today&zone=" + ZONE;
    private static final String[] KEYS = {"fajr", "dhuhr", "asr", "maghrib", "isha"};
    private static final String[] NAMES = {"Subuh", "Zohor", "Asar", "Maghrib", "Isyak"};
    private static final TimeZone MY = TimeZone.getTimeZone("Asia/Kuala_Lumpur");
    private static volatile boolean running;

    private PrayerTimes() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("prayer_times", Context.MODE_PRIVATE);
    }

    private static String today() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(MY);
        return f.format(Calendar.getInstance(MY).getTime());
    }

    public static void refreshIfDue(final Context ctx, final Runnable onDone) {
        final Context app = ctx.getApplicationContext();
        if (running || today().equals(prefs(app).getString("day", ""))) return;
        running = true;
        new Thread(() -> {
            boolean ok = false;
            try {
                JSONObject row = new JSONObject(fetch())
                        .getJSONArray("prayerTime").getJSONObject(0);
                SharedPreferences.Editor e = prefs(app).edit();
                for (String k : KEYS) {
                    String v = row.getString(k);
                    if (!v.matches("\\d\\d:\\d\\d(:\\d\\d)?")) {
                        throw new IllegalStateException("bad time");
                    }
                    e.putString(k, v.substring(0, 5));
                }
                e.putString("day", today()).apply();
                ok = true;
            } catch (Exception ignored) {
            } finally {
                running = false;
            }
            if (ok && onDone != null) {
                new Handler(Looper.getMainLooper()).post(onDone);
            }
        }).start();
    }

    private static String fetch() throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(API).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setRequestProperty("Accept", "application/json");
        try {
            if (c.getResponseCode() != 200) {
                throw new IllegalStateException("HTTP " + c.getResponseCode());
            }
            try (InputStream in = c.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int r;
                while ((r = in.read(buf)) > 0) {
                    out.write(buf, 0, r);
                    if (out.size() > 16384) throw new IllegalStateException("too large");
                }
                return out.toString("UTF-8");
            }
        } finally {
            c.disconnect();
        }
    }

    /** {"Seterusnya: Maghrib 19:04", "Subuh 05:57   Zohor 13:07 ..."} or null if not cached yet. */
    public static String[] lines(Context ctx) {
        SharedPreferences p = prefs(ctx);
        if (!today().equals(p.getString("day", ""))) return null;
        Calendar now = Calendar.getInstance(MY);
        int cur = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int next = 0; // after Isyak the next one is Subuh
        boolean found = false;
        StringBuilder all = new StringBuilder();
        for (int i = 0; i < KEYS.length; i++) {
            String t = p.getString(KEYS[i], "");
            if (t.length() < 5) return null;
            if (all.length() > 0) all.append("   ");
            all.append(NAMES[i]).append(' ').append(t);
            if (!found && minutes(t) > cur) {
                next = i;
                found = true;
            }
        }
        String nt = p.getString(KEYS[next], "");
        return new String[]{"Seterusnya: " + NAMES[next] + " " + nt, all.toString()};
    }

    private static int minutes(String hhmm) {
        return Integer.parseInt(hhmm.substring(0, 2)) * 60
                + Integer.parseInt(hhmm.substring(3, 5));
    }
}
