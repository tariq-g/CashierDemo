package com.wiseasy.cashier.demo.util;

import android.util.Log;

public class CLog {

    private static String TAG = "CLog";
    private static String className;
    private static String methodName;
    private static int lineNumber;
    private static boolean isDebug = true;

    private static void getMethodNames() {
        StackTraceElement[] sElements = new Throwable().getStackTrace();
        className = sElements[2].getFileName();
        methodName = sElements[2].getMethodName();
        lineNumber = sElements[2].getLineNumber();
    }

    private static String createLog(String log) {
        return "[" + methodName + "]" + log;
    }

    public static void log(String msg) {
        log(TAG, msg);
    }

    public static void log(String tag, String msg) {
        if (isDebug) {
            getMethodNames();
            Log.i(tag, "(" + className + ":" + lineNumber + ")" + createLog(msg));
        }
    }

    public static void loge(String msg) {
        loge(TAG, msg);
    }

    public static void loge(String tag, String msg) {
        if (isDebug) {
            getMethodNames();
            Log.e(tag, "(" + className + ":" + lineNumber + ")" + createLog(msg));
        }
    }
}
