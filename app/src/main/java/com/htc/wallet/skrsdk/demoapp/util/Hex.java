package com.htc.wallet.skrsdk.demoapp.util;

public abstract class Hex {
    private final static char[] hexUpperArray = "0123456789ABCDEF".toCharArray();
    private final static char[] hexLowerArray = "0123456789abcdef".toCharArray();

    public static String encodeToUppercase(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexUpperArray[v >>> 4];
            hexChars[j * 2 + 1] = hexUpperArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String encodeToLowercase(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexLowerArray[v >>> 4];
            hexChars[j * 2 + 1] = hexLowerArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}