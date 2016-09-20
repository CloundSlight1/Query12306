package com.wuyz.query12306;

import android.util.Log;

public class Log2 {

    private static final String TAG = "query12306";

    public static void v(String className, String format, Object... args) {
        if (BuildConfig.DEBUG)
            Log.v(TAG, className + ", " + String.format(format, args));
    }

    public static void i(String className, String format, Object... args) {
        if (BuildConfig.DEBUG)
            Log.i(TAG, className + ", " + String.format(format, args));
    }

    public static void d(String className, String format, Object... args) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, className + ", " + String.format(format, args));
    }

    public static void w(String className, String format, Object... args) {
        Log.w(TAG, className + ", " + String.format(format, args));
    }

    public static void e(String className, String msg, Throwable tr) {
        Log.e(TAG, className + ", " + msg, tr);
    }

    public static void e(String className, String msg) {
        e(className, msg, null);
    }

    public static void e(String className, Throwable tr) {
        Log.e(TAG, className, tr);
    }
}
