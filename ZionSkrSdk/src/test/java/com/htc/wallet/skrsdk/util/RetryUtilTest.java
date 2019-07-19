package com.htc.wallet.skrsdk.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RetryUtilTest {
    private static final long TEN_MINUTES = 600_000;
    private static final int MAXIMUM_NUMBER = 3;

    @Test
    public void getWaitTimeMillisTest() {
        long waitTime;
        int power;
        for (int i = 0; i < 100; i++) {
            if (i < 3) {
                waitTime = 0;
            } else {
                power = (i / MAXIMUM_NUMBER) - 1;
                waitTime = TEN_MINUTES * (long) Math.pow(MAXIMUM_NUMBER, power);
            }
            assertEquals(waitTime, RetryUtil.getWaitTimeMillis(i));
        }
    }
}