package com.htc.wallet.skrsdk.deprecated.sharedprefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.htc.wallet.skrsdk.crypto.ChecksumUtil;
import com.htc.wallet.skrsdk.deprecated.sharedprefs.RestoreSource;
import com.htc.wallet.skrsdk.deprecated.sharedprefs.StorageBase;

import org.junit.Test;

public class RestoreSourceTest {

    // General, Request
    private static final String UUID_STR = "23951cf6-5680-44d9-9f2d-bd6c8d669a2d";
    static final String UUID_HASH = ChecksumUtil.generateChecksum(UUID_STR);
    private static final String FCM_TOKEN = "fvJTMGlJaeU:APA91bE_C13HmjnuMODiHMkFSdaMKwg3pmykijTcMrqbvBVxSCItWLR1N6l78T81o"
            + "Diei0KLOP7wgjCXfDSUZjnqwV0MdDjQsWIToHYYeVbS7_cqUeLDuyVU6WEsZLqs7dysu8LFYqE8";
    private static final String PUBLIC_KEY = "qwsderftghynjmkdsadas";
    private static final long TIME_STAMP = 1234567890L;

    // OK
    private static final String SEED = "Q154W53RG45GIN3H5J4D8T15YVKIGF48FSFFD5FDSGI8P";

    static RestoreSource newRequest() {
        return new RestoreSource.Builder(RestoreSource.RESTORE_SOURCE_STATUS_REQUEST)
                .setUUIDHash(UUID_HASH)
                .setFcmToken(FCM_TOKEN)
                .setPublicKey(PUBLIC_KEY)
                .setTimeStamp(TIME_STAMP)
                .build();
    }

    static RestoreSource newOK() {
        return new RestoreSource.Builder(RestoreSource.RESTORE_SOURCE_STATUS_OK)
                .setUUIDHash(UUID_HASH)
                .setFcmToken(FCM_TOKEN)
                .setPublicKey(PUBLIC_KEY)
                .setTimeStamp(TIME_STAMP)
                .setSeed(SEED)
                .build();
    }

    static void assertRequest(RestoreSource source) {
        assertTrue(source.compareStatus(RestoreSource.RESTORE_SOURCE_STATUS_REQUEST));
        assertTrue(source.compareUUIDHash(UUID_HASH));
        assertTrue(source.compareUUID(UUID_STR));
        assertEquals(StorageBase.STORAGE_VERSION, source.getVersion());
        assertEquals(RestoreSource.RESTORE_SOURCE_STATUS_REQUEST, source.getStatus());
        assertEquals(UUID_HASH, source.getUUIDHash());
        assertEquals(FCM_TOKEN, source.getFcmToken());
        assertEquals(PUBLIC_KEY, source.getPublicKey());
        assertEquals(TIME_STAMP, source.getTimeStamp());
    }

    static void assertOK(RestoreSource source) {
        assertTrue(source.compareStatus(RestoreSource.RESTORE_SOURCE_STATUS_OK));
        assertTrue(source.compareUUIDHash(UUID_HASH));
        assertTrue(source.compareUUID(UUID_STR));
        assertEquals(StorageBase.STORAGE_VERSION, source.getVersion());
        assertEquals(RestoreSource.RESTORE_SOURCE_STATUS_OK, source.getStatus());
        assertEquals(UUID_HASH, source.getUUIDHash());
        assertEquals(FCM_TOKEN, source.getFcmToken());
        assertEquals(PUBLIC_KEY, source.getPublicKey());
        assertEquals(TIME_STAMP, source.getTimeStamp());
        assertEquals(SEED, source.getSeed());
    }


    @Test
    public void buildRequestTest() {
        RestoreSource target = newRequest();

        assertFalse(target.isEncrypted());
        assertRequest(target);

        // encrypt
        target.encrypt();
        assertTrue(target.isEncrypted());
        assertRequest(target);

        // decrypt
        target.decrypt();
        assertFalse(target.isEncrypted());
        assertRequest(target);
    }

    @Test
    public void buildOKTest() {
        RestoreSource target = newOK();

        assertFalse(target.isEncrypted());
        assertOK(target);

        // encrypt
        target.encrypt();
        assertTrue(target.isEncrypted());
        assertOK(target);

        // decrypt
        target.decrypt();
        assertFalse(target.isEncrypted());
        assertOK(target);
    }
}
