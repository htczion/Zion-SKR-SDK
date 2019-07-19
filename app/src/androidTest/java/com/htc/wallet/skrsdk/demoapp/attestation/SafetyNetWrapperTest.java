package com.htc.wallet.skrsdk.demoapp.attestation;

import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.text.TextUtils;
import android.util.Log;

import com.htc.wallet.skrsdk.tools.security.attestation.SafetyNetWrapper;

import org.junit.Test;

public class SafetyNetWrapperTest {
    private static final String TAG = SafetyNetWrapperTest.class.getSimpleName();

    private static final Context CONTEXT = InstrumentationRegistry.getTargetContext();

    @Test
    public void getAttestTokenTest() {
        String attestToken = SafetyNetWrapper.getInstance().getDeviceAttestToken(CONTEXT, true);
        Log.d(TAG, "attestToken = " + attestToken);
        assertFalse(TextUtils.isEmpty(attestToken));
    }

    @Test
    public void stageTest() {
        String attestToken = SafetyNetWrapper.getInstance().getDeviceAttestToken(CONTEXT, true, true);
        Log.d(TAG, "stage attestToken = " + attestToken);
        assertFalse(TextUtils.isEmpty(attestToken));
    }

    @Test
    public void productionTest() {
        SafetyNetWrapper.getInstance().update(CONTEXT);
        String attestToken = SafetyNetWrapper.getInstance().getDeviceAttestToken(CONTEXT, false, true);
        Log.d(TAG, "production attestToken = " + attestToken);
        assertFalse(TextUtils.isEmpty(attestToken));
    }

    // Use for test running time
    //@Test
    public void stageTime() throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            long start = SystemClock.elapsedRealtime();
            String attestToken = SafetyNetWrapper.getInstance().getDeviceAttestToken(CONTEXT, true, true);
            long end = SystemClock.elapsedRealtime();
            Log.d(TAG, "stage attestToken = " + attestToken);
            Log.d(TAG, "time = " + (end - start) + " ms");
            assertFalse(TextUtils.isEmpty(attestToken));

            Thread.sleep(10_000);
        }
    }
}
