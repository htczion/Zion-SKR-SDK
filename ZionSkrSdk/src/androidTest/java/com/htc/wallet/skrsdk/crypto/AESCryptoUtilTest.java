package com.htc.wallet.skrsdk.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.text.TextUtils;

import com.htc.wallet.skrsdk.crypto.util.AESCryptoUtil;

import org.junit.Test;

public class AESCryptoUtilTest {

    private static final String TEST_UUID = "0b414f3a-d7d9-4083-893b-e665040bb505";
    private static final String TEST_TOKEN =
            "ezAbQhYLk58:APA91bFYsaeMYDoPjryj-5Kfzm1vruvmsP79N04jaEbDxfuoFgRTQ1z02NbNSwFoci97nh"
                    + "7jWayjIiUII_FS2kJgYhdhLEwyqBP1kEJDK6i0lah4sdIFa5ay8k652QGKrSyUp2Fujidt";

    private static final String ENC_UUID =
            "P4Z_GhV1ejAZ7hdXzjSiNpOP-HHRkJ-rHqwe_Hxt5rjMdYXrx2BnEztVFcX0kb2q\n";
    private static final String ENC_TOKEN =
            "H2y3wZTS-qFDPPPuNUfAhvaq2EMGmZ5bcNaO_fJTfF3hdoYfv_-Ol_pruWuEwjdL6pbxcnNf1IIi\n"
                    + "SjlDn-O5Qg1XlbKlZl3UfSVFZlZYsoV9Owv0dIr"
                    + "horvELgFk1dMwX-eF234UVweePLe0k-vc_pbV\n"
                    + "P9e-U3pejur8nqyTgLhqNAPAF1skIEYPF9zMhFv"
                    + "GV5wkQpWH91HiXYyr6efK2g==\n";
    private static final String KEY_AND_IV =
            "ylCJphYkd1v2uweFwWQ6t2M7G3YzH8GYOMTt8aIsY88=:gHvRXHR5AQxrl7L8xJ-uKA==";

    @Test
    public void newInstanceTest() {
        AESCryptoUtil aesCryptoUtil = AESCryptoUtil.newInstance();

        // UUID
        String encUUID = aesCryptoUtil.encrypt(TEST_UUID);
        assertFalse(TextUtils.isEmpty(encUUID));

        String decUUID = aesCryptoUtil.decrypt(encUUID);
        assertFalse(TextUtils.isEmpty(decUUID));

        assertEquals(TEST_UUID, decUUID);

        // FCM Token
        String encToken = aesCryptoUtil.encrypt(TEST_TOKEN);
        assertFalse(TextUtils.isEmpty(encToken));

        String decToken = aesCryptoUtil.decrypt(encToken);
        assertFalse(TextUtils.isEmpty(decToken));

        assertEquals(TEST_TOKEN, decToken);
    }

    @Test
    public void getInstanceTest() {
        AESCryptoUtil aesCryptoUtil = AESCryptoUtil.getInstance(KEY_AND_IV);
        assertNotNull(aesCryptoUtil);

        String encUUID = aesCryptoUtil.encrypt(TEST_UUID);
        assertNotNull(encUUID);
        assertEquals(ENC_UUID, encUUID);

        String encToken = aesCryptoUtil.encrypt(TEST_TOKEN);
        assertNotNull(encToken);
        assertEquals(ENC_TOKEN, encToken);
    }

    @Test
    public void getInstanceFailedTest() {
        AESCryptoUtil aesCryptoUtil;

        aesCryptoUtil = AESCryptoUtil.getInstance("ABC:123");
        assertNull(aesCryptoUtil);

        aesCryptoUtil = AESCryptoUtil.getInstance("ABC:123:AAA");
        assertNull(aesCryptoUtil);
    }
}
