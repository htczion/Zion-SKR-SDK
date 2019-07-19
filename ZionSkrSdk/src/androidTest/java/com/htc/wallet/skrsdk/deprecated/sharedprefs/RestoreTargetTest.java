package com.htc.wallet.skrsdk.deprecated.sharedprefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.htc.wallet.skrsdk.crypto.ChecksumUtil;
import com.htc.wallet.skrsdk.deprecated.sharedprefs.RestoreTarget;
import com.htc.wallet.skrsdk.deprecated.sharedprefs.StorageBase;

import org.junit.Test;

public class RestoreTargetTest {

    // General
    static final String EMAIL_HASH = ChecksumUtil.generateChecksum("abc@htc.com");
    private static final String UUID_STR = "23951cf6-5680-44d9-9f2d-bd6c8d669a2d";
    static final String UUID_HASH = ChecksumUtil.generateChecksum(UUID_STR);
    private static final String UUID_STR2 = "23951cf6-5680-44d9-9f2d-bd6c8d669a2e";
    static final String UUID_HASH2 = ChecksumUtil.generateChecksum(UUID_STR2);
    private static final String UUID_BACKUP = "23951cf6-5680-44d9-9f2d-bd6c8d669a3e";
    private static final String UUID_HASH_BACKUP = ChecksumUtil.generateChecksum(UUID_BACKUP);
    private static final String FCM_TOKEN = "fvJTMGlJaeU:APA91bE_C13HmjnuMODiHMkFSdaMKwg3pmykijTcMrqbvBVxSCItWLR1N6l78T81o"
            + "Diei0KLOP7wgjCXfDSUZjnqwV0MdDjQsWIToHYYeVbS7_cqUeLDuyVU6WEsZLqs7dysu8LFYqE8";
    private static final String PUBLIC_KEY = "qwsderftghynjmkdsadas";
    private static final String NAME = "Amy";
    private static final long TIME_STAMP = 1234567890L;
    private static final int RETRY_TIMES = 0;
    private static final String PIN_CODE = "123456";

    static RestoreTarget newRestoreTarget1() {
        return new RestoreTarget.Builder()
                .setEmailHash(EMAIL_HASH)
                .setUUIDHash(UUID_HASH)
                .setBackupUUIDHash(UUID_HASH_BACKUP)
                .setFcmToken(FCM_TOKEN)
                .setPublicKey(PUBLIC_KEY)
                .setName(NAME)
                .setTimeStamp(TIME_STAMP)
                .setRetryTimes(RETRY_TIMES)
                .setPinCode(PIN_CODE)
                .build();
    }

    static RestoreTarget newRestoreTarget2() {
        return new RestoreTarget.Builder()
                .setEmailHash(EMAIL_HASH)
                .setUUIDHash(UUID_HASH2)
                .setFcmToken(FCM_TOKEN)
                .setPublicKey(PUBLIC_KEY)
                .setName(NAME)
                .setTimeStamp(TIME_STAMP)
                .setRetryTimes(RETRY_TIMES)
                .setPinCode(PIN_CODE)
                .build();
    }

    static void assertRestoreTarget1(RestoreTarget target) {
        assertTrue(target.compareEmailHash(EMAIL_HASH));
        assertTrue(target.compareUUIDHash(UUID_HASH));
        assertTrue(target.comparePinCode(PIN_CODE));
        assertEquals(StorageBase.STORAGE_VERSION, target.getVersion());
        assertEquals(EMAIL_HASH, target.getEmailHash());
        assertEquals(UUID_HASH, target.getUUIDHash());
        assertEquals(UUID_HASH_BACKUP, target.getBackupUUIDHash());
        assertEquals(FCM_TOKEN, target.getFcmToken());
        assertEquals(PUBLIC_KEY, target.getPublicKey());
        assertEquals(NAME, target.getName());
        assertEquals(TIME_STAMP, target.getTimeStamp());
        assertEquals(RETRY_TIMES, target.getRetryTimes());
        assertEquals(PIN_CODE, target.getPinCode());
    }

    static void assertRestoreTarget2(RestoreTarget target) {
        assertTrue(target.compareEmailHash(EMAIL_HASH));
        assertTrue(target.compareUUIDHash(UUID_HASH2));
        assertTrue(target.comparePinCode(PIN_CODE));
        assertEquals(StorageBase.STORAGE_VERSION, target.getVersion());
        assertEquals(EMAIL_HASH, target.getEmailHash());
        assertEquals(UUID_HASH2, target.getUUIDHash());
        assertEquals(null, target.getBackupUUIDHash());
        assertEquals(FCM_TOKEN, target.getFcmToken());
        assertEquals(PUBLIC_KEY, target.getPublicKey());
        assertEquals(NAME, target.getName());
        assertEquals(TIME_STAMP, target.getTimeStamp());
        assertEquals(RETRY_TIMES, target.getRetryTimes());
        assertEquals(PIN_CODE, target.getPinCode());
    }

    @Test
    public void buildRestoreTargetTest() {
        RestoreTarget target = newRestoreTarget1();

        assertFalse(target.isEncrypted());
        assertRestoreTarget1(target);

        // encrypt
        target.encrypt();
        assertTrue(target.isEncrypted());
        assertRestoreTarget1(target);

        // decrypt
        target.decrypt();
        assertFalse(target.isEncrypted());
        assertRestoreTarget1(target);
    }
}
