package com.app.classsicradio.utils;

import android.content.Context;
import android.os.Build;
import android.webkit.WebSettings;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utilities {

    private static ArrayList<EventListener> listeners;
    Context context;


    public Utilities(Context context) {
        this.context = context;
    }

    public static void registerAsListener(EventListener listener) {
        if (listeners == null) listeners = new ArrayList<>();
        listeners.add(listener);
    }

    public static void unregisterAsListener(EventListener listener) {
        listeners.remove(listener);
    }

    public static void onEvent(String status) {
        if (listeners == null) return;
        for (EventListener listener : listeners) {
            listener.onEvent(status);
        }
    }

    public static void onAudioSessionId(Integer id) {
        if (listeners == null) return;
        for (EventListener listener : listeners) {
            listener.onAudioSessionId(id);
        }
    }

    public interface EventListener {
        void onEvent(String status);
        void onAudioSessionId(Integer i);
    }

    public static String getUserAgent(Context context) {
        try {
            return WebSettings.getDefaultUserAgent(context);
        } catch (Exception e) {
            return System.getProperty("http.agent");
        }
    }

    public static long convertToMilliSeconds(String s) {

        long ms = 0;
        Pattern p;
        if (s.contains(("\\:"))) {
            p = Pattern.compile("(\\d+):(\\d+)");
        } else {
            p = Pattern.compile("(\\d+).(\\d+)");
        }
        Matcher m = p.matcher(s);
        if (m.matches()) {
            int h = Integer.parseInt(m.group(1));
            int min = Integer.parseInt(m.group(2));
            // int sec = Integer.parseInt(m.group(2));
            ms = (long) h * 60 * 60 * 1000 + min * 60 * 1000;
        }
        return ms;
    }

}

