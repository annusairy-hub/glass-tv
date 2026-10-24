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

/** Current weather and air quality for Langkawi from Open-Meteo (no key). */
public final class Weather {
    public static final class Now {
        public final float temp;
        public final int code;
        public final boolean day;
        public final int aqi; // US AQI, -1 when unknown

        Now(float temp, int code, boolean day, int aqi) {
            this.temp = temp;
            this.code = code;
            this.day = day;
            this.aqi = aqi;
        }
    }

    private static final String API =
        "https://api.open-meteo.com/v1/forecast?latitude=6.35&longitude=99.80"
        + "&current=temperature_2m,weather_code,is_day&timezone=Asia%2FKuala_Lumpur";
    private static final String AIR =
        "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=6.35&longitude=99.80"
        + "&current=us_aqi&timezone=Asia%2FKuala_Lumpur";
    private static final long REFRESH_MS = 30L * 60L * 1000L;
    private static final long KEEP_MS = 3L * 60L * 60L * 1000L;
    private static volatile boolean running;

    private Weather() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("weather", Context.MODE_PRIVATE);
    }

    public static void refreshIfDue(final Context ctx, final Runnable onDone) {
        final Context app = ctx.getApplicationContext();
        long age = System.currentTimeMillis() - prefs(app).getLong("at", 0L);
        if (running || (age >= 0 && age < REFRESH_MS)) return;
        running = true;
        new Thread(() -> {
            boolean ok = false;
            try {
                JSONObject cur = new JSONObject(fetch(API)).getJSONObject("current");
                double temp = cur.getDouble("temperature_2m");
                int code = cur.getInt("weather_code");
                int day = cur.optInt("is_day", 1);
                int aqi = -1;
                try {
                    aqi = (int) Math.round(new JSONObject(fetch(AIR))
                            .getJSONObject("current").getDouble("us_aqi"));
                } catch (Exception ignored) {
                }
                prefs(app).edit()
                        .putFloat("temp", (float) temp)
                        .putInt("code", code)
                        .putInt("day", day)
                        .putInt("aqi", aqi)
                        .putLong("at", System.currentTimeMillis())
                        .apply();
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

    /** Last good reading, or null when there is none or it is older than 3 hours. */
    public static Now read(Context ctx) {
        SharedPreferences p = prefs(ctx);
        if (!p.contains("at")) return null;
        long age = System.currentTimeMillis() - p.getLong("at", 0L);
        if (age < 0 || age > KEEP_MS) return null;
        return new Now(p.getFloat("temp", 0f), p.getInt("code", -1),
                p.getInt("day", 1) == 1, p.getInt("aqi", -1));
    }

    public static String label(int code) {
        if (code == 0) return "Cerah";
        if (code == 1 || code == 2) return "Separa berawan";
        if (code == 3) return "Mendung";
        if (code == 45 || code == 48) return "Berkabus";
        if (code >= 51 && code <= 57) return "Gerimis";
        if ((code >= 61 && code <= 67) || (code >= 80 && code <= 82)) return "Hujan";
        if (code >= 95) return "Ribut petir";
        return "Berawan";
    }

    public static String aqiLabel(int aqi) {
        if (aqi <= 50) return "Baik";
        if (aqi <= 100) return "Sederhana";
        if (aqi <= 150) return "Kurang sihat";
        if (aqi <= 200) return "Tidak sihat";
        return "Sangat tidak sihat";
    }

    public static int aqiColor(int aqi) {
        if (aqi <= 50) return 0xFF4ADE80;
        if (aqi <= 100) return 0xFFFACC15;
        if (aqi <= 150) return 0xFFFB923C;
        if (aqi <= 200) return 0xFFF87171;
        return 0xFFC084FC;
    }

    private static String fetch(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
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
}
