package com.htc.wallet.skrsdk.util;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.security.SecureRandom;

public class PinCodeUtil {
    private static final String TAG = "PinCodeUtil";

    public static final int PIN_CODE_LENGTH = 6;

    private PinCodeUtil() {
    }

    @NonNull
    public static String newPinCode() {
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder pin = new StringBuilder();
        for (int i = 0; i < PIN_CODE_LENGTH; i++) {
            pin.append(secureRandom.nextInt(10)); // 0~9
        }
        return pin.toString();
    }

    public static boolean isValidPinCode(String pinCode) {
        return !TextUtils.isEmpty(pinCode)
                && pinCode.length() == PIN_CODE_LENGTH
                && isNumeric(pinCode);
    }

    // com.google.android.gms.common.util.NumberUtils.isNumeric
    private static boolean isNumeric(String num) {
        try {
            Long.parseLong(num);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
