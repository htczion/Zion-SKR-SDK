package com.htc.wallet.skrsdk.util;

import android.support.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public abstract class StringUtil {

    public static boolean isAscii(@NonNull String text) {
        Objects.requireNonNull(text, "text is null");
        return StandardCharsets.US_ASCII.newEncoder().canEncode(text);
    }
}
