package com.htc.wallet.skrsdk.whisper;

import android.text.TextUtils;

import com.htc.wallet.skrsdk.util.LogUtil;

import java.io.ByteArrayOutputStream;

public final class StringConverter {
    private static final String TAG = "StringConverter";

    private static final String HEX_STRING = "0123456789ABCDEF";

    private StringConverter() {
    }

    public static String encodeToHex(String str) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("str is empty");
        }
        byte[] bytes = str.getBytes();
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        sb.append("0x");
        for (int i = 0; i < bytes.length; i++) {
            sb.append(HEX_STRING.charAt((bytes[i] & 0xf0) >> 4));
            sb.append(HEX_STRING.charAt((bytes[i] & 0x0f) >> 0));
        }
        return sb.toString();
    }

    public static String decodeFromHex(String bytes) {
        if (TextUtils.isEmpty(bytes)) {
            throw new IllegalArgumentException("bytes is empty");
        }
        bytes = bytes.substring(2);
        bytes = bytes.toUpperCase();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(bytes.length() / 2);
        for (int i = 0; i < bytes.length(); i += 2) {
            byteArrayOutputStream.write(
                    (HEX_STRING.indexOf(bytes.charAt(i)) << 4
                            | HEX_STRING.indexOf(bytes.charAt(i + 1))));
        }
        String unHexStr = "";
        try {
            unHexStr = new String(byteArrayOutputStream.toByteArray(), "UTF-8");
        } catch (Exception e) {
            LogUtil.logError(TAG, "decodeFromHex Exception: " + e);
        }
        return unHexStr;
    }
}
