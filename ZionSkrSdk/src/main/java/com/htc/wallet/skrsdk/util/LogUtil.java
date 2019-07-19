/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.htc.wallet.skrsdk.util;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class LogUtil {
    private static final String TAG = "LogUtil";

    private static volatile boolean sIsDebug = false;

    private LogUtil() {
    }

    public static void setIsDebug(boolean isDebug) {
        sIsDebug = isDebug;
    }

    public static void logVerbose(String tag, String message) {
        if (sIsDebug) {
            Log.v(tag, message);
        }
    }

    public static void logDebug(String tag, String message) {
        if (sIsDebug) {
            Log.d(tag, message);
        }
    }

    public static void logInfo(String tag, String message) {
        Log.i(tag, message);
    }

    public static void logWarning(String tag, String message) {
        Log.w(tag, message);
    }

    public static void logError(String tag, String message) {
        Log.e(tag, message);
    }

    public static void logError(String tag, String message, RuntimeException e)
            throws RuntimeException {
        Log.e(tag, message);
        if (sIsDebug) {
            throw e;
        }
    }

    public static void logWtf(String tag, String message, RuntimeException e)
            throws RuntimeException {
        Log.wtf(tag, message, e);
    }

    public static void logPii(String tag, String message) {
        Log.d(TAG, pii(message));
    }

    /**
     * Reduct personally identifiable information for production users. If we are running in debug
     * mode, return the original string, otherwise return a SHA-1 hash of the input string.
     */
    public static String pii(Object pii) {
        return "[" + secureHash(String.valueOf(pii).getBytes()) + "]";
    }

    private static String secureHash(byte[] input) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
        messageDigest.update(input);
        byte[] result = messageDigest.digest();
        return encodeHex(result);
    }

    private static String encodeHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        StringBuffer hex = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            int byteIntValue = bytes[i] & 0xff;
            if (byteIntValue < 0x10) {
                hex.append("0");
            }
            hex.append(Integer.toString(byteIntValue, 16));
        }
        return hex.toString();
    }
}
