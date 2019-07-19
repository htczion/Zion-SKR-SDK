package com.htc.wallet.skrsdk.demoapp;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;

public final class Hex {
    private final static char[] hexUpperArray = "0123456789ABCDEF".toCharArray();
    private final static char[] hexLowerArray = "0123456789abcdef".toCharArray();

    private Hex() {
    }

    public static String encodeToUppercase(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexUpperArray[v >>> 4];
            hexChars[j * 2 + 1] = hexUpperArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String encodeToLowercase(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexLowerArray[v >>> 4];
            hexChars[j * 2 + 1] = hexLowerArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Nullable
    public static byte[] decode(String hexStr) {
        if (TextUtils.isEmpty(hexStr)) {
            return null;
        }

        if (hexStr.length() % 2 != 0) {
            hexStr = "0" + hexStr;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (int i = 0; i < hexStr.length(); i += 2) {
            int b = Integer.parseInt(hexStr.substring(i, i + 2), 16);
            outputStream.write(b);
        }
        return outputStream.toByteArray();
    }
}
