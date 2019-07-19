package com.htc.wallet.skrsdk.crypto;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;

public class Base64Util {
    private static final int BASE_ENCODE_DECODE_FLAG = Base64.URL_SAFE;

    private Base64Util() {
        throw new AssertionError();
    }

    public static String encodeToString(@NonNull final byte[] input) {
        if (input == null || input.length == 0) {
            throw new IllegalArgumentException("encode, input is empty");
        }
        return Base64.encodeToString(input, BASE_ENCODE_DECODE_FLAG);
    }

    public static byte[] decode(final String input) {
        if (TextUtils.isEmpty(input)) {
            throw new IllegalArgumentException("decode, input is empty");
        }

        return Base64.decode(input, BASE_ENCODE_DECODE_FLAG);
    }

    // Default
    public static String encodeToStringDefault(@NonNull final byte[] input) {
        if (input == null || input.length == 0) {
            throw new IllegalArgumentException("encode, input is empty");
        }
        return Base64.encodeToString(input, Base64.DEFAULT);
    }

    public static byte[] decodeDefault(final String input) {
        if (TextUtils.isEmpty(input)) {
            throw new IllegalArgumentException("decode, input is empty");
        }

        return Base64.decode(input, Base64.DEFAULT);
    }
}
