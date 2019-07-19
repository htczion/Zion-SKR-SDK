package com.htc.wallet.skrsdk.util;

public class RetryUtil {
    private static final String TAG = "RetryUtil";

    private static final long TEN_MINUTES = 600_000;

    public static final int MAXIMUM_TRY_NUMBER = 3;

    private RetryUtil() {
    }

    public static long getWaitTimeMillis(int retryTimes) {
        if (retryTimes < MAXIMUM_TRY_NUMBER) {
            // Not need to wait.
            return 0;
        } else {
            int power = (retryTimes / MAXIMUM_TRY_NUMBER) - 1;
            return TEN_MINUTES * (long) Math.pow(MAXIMUM_TRY_NUMBER, power);
        }
    }
}
