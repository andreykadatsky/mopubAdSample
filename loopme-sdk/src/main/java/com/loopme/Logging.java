package com.loopme;

import android.util.Log;

public class Logging {

    public enum LogLevel {
        INFO,
        DEBUG,
        ERROR
    }

    private Logging() {
    }

    public static void out(String tag, String text, LogLevel logLevel) {
        if (StaticParams.DEBUG_MODE) {
            Log.v("Debug.LoopMe." + tag, text);
        } else {
            if (logLevel == LogLevel.INFO) {
                Log.v("Debug.LoopMe." + tag, text);
            }
        }
    }
}
