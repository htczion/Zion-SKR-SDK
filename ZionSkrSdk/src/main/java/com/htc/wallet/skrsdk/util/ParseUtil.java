package com.htc.wallet.skrsdk.util;

import android.text.TextUtils;

public final class ParseUtil {
    private static final String TAG = "ParseUtil";

    private ParseUtil() {
    }

    // TODO: Change use ParseUtil, all Integer.parseInt() and Long.parseLong()

    public static int tryParseInt(String intStr, int defaultValue) {
        if (TextUtils.isEmpty(intStr)) {
            LogUtil.logDebug(TAG, "intStr is null or empty");
            return defaultValue;
        }
        try {
            return Integer.parseInt(intStr);
        } catch (NumberFormatException e) {
            LogUtil.logError(TAG, "tryParseInt() failed, intStr=" + intStr);
            return defaultValue;
        }
    }

    public static long tryParseLong(String longStr, long defaultValue) {
        if (TextUtils.isEmpty(longStr)) {
            LogUtil.logDebug(TAG, "longStr is null or empty");
            return defaultValue;
        }
        try {
            return Long.parseLong(longStr);
        } catch (NumberFormatException e) {
            LogUtil.logError(TAG, "tryParseLong() failed, longStr=" + longStr);
            return defaultValue;
        }
    }
}