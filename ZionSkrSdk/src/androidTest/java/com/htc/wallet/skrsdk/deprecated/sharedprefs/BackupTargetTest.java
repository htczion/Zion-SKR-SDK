package com.htc.wallet.skrsdk.deprecated.sharedprefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.htc.wallet.skrsdk.crypto.ChecksumUtil;
import com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupTarget;
import com.htc.wallet.skrsdk.deprecated.sharedprefs.StorageBase;

import org.junit.Test;

public class BackupTargetTest {

    // Request
    private static final int RETRY_TIMES = 0;
    private static final long RETRY_START_WAIT_TIME = 0;
    private static final String PHONE_NUMBER = "0987-654-321";
    private static final String PHONE_MODEL = "HTC U12+";
    private static final String PIN_CODE = "666666";
    private static final String SEED = "Q154W53RG45GIN3H5J4D8T15YVKIGF48FSFFD5FDSGI8P";

    // General
    private static final String FCM_TOKEN = "fvJTMGlJaeU:APA91bE_C13HmjnuMODiHMkFSdaMKwg3pmykijTcMrqbvBVxSCItWLR1N6l78T81o"
            + "Diei0KLOP7wgjCXfDSUZjnqwV0MdDjQsWIToHYYeVbS7_cqUeLDuyVU6WEsZLqs7dysu8LFYqE8";
    private static final String NAME = "Bob==...,,,  1212!@#$%^&*()_+===.,.,.,";
    private static final String PUBLIC_KEY = "qwsderftghynjmkdsadas";
    static final String UUID_HASH = ChecksumUtil.generateChecksum("23951cf6-5680-44d9-9f2d-bd6c8d669a2d");
    private static final long LAST_CHECKED_TIME = 1234567890L;
    private static final int SEED_INDEX = 3; // 1 ~ 5
    private static final String CHECK_SUM = ChecksumUtil.generateChecksum(SEED);

    private static final long ALLOW_GAP = 10L;

    static BackupTarget newUnSeededRequest() {
        return new BackupTarget.Builder(BackupTarget.BACKUP_TARGET_STATUS_REQUEST)
                .setFcmToken(FCM_TOKEN)
                .setName(NAME)
                .setPublicKey(PUBLIC_KEY)
                .setUUIDHash(UUID_HASH)
                .setLastCheckedTime(LAST_CHECKED_TIME)
                .setPhoneNumber(PHONE_NUMBER)
                .setPhoneModel(PHONE_MODEL)
                .build();
    }

    static BackupTarget newSeededRequest() {
        return new BackupTarget.Builder(BackupTarget.BACKUP_TARGET_STATUS_REQUEST)
                .setFcmToken(FCM_TOKEN)
                .setName(NAME)
                .setPublicKey(PUBLIC_KEY)
                .setUUIDHash(UUID_HASH)
                .setLastCheckedTime(LAST_CHECKED_TIME)
                .setSeedIndex(SEED_INDEX)
                .setCheckSum(CHECK_SUM)
                .setPhoneNumber(PHONE_NUMBER)
                .setPhoneModel(PHONE_MODEL)
                .setPinCode(PIN_CODE)
                .setSeed(SEED)
                .build();
    }

    static BackupTarget newOK() {
        return new BackupTarget.Builder(BackupTarget.BACKUP_TARGET_STATUS_OK)
                .setFcmToken(FCM_TOKEN)
                .setName(NAME)
                .setPublicKey(PUBLIC_KEY)
                .setUUIDHash(UUID_HASH)
                .setLastCheckedTime(LAST_CHECKED_TIME)
                .setSeedIndex(SEED_INDEX)
                .setCheckSum(CHECK_SUM)
                .build();
    }

    static BackupTarget newBad() {
        return new BackupTarget.Builder(BackupTarget.BACKUP_TARGET_STATUS_BAD)
                .setFcmToken(FCM_TOKEN)
                .setName(NAME)
                .setPublicKey(PUBLIC_KEY)
                .setUUIDHash(UUID_HASH)
                .setLastCheckedTime(LAST_CHECKED_TIME)
                .setSeedIndex(SEED_INDEX)
                .setCheckSum(CHECK_SUM)
                .build();
    }

    static void assertUnSeededRequest(BackupTarget target) {
        assertFalse(target.isSeeded());
        assertTrue(target.compareStatus(BackupTarget.BACKUP_TARGET_STATUS_REQUEST));
        assertTrue(target.compareFcmToken(FCM_TOKEN));
        assertTrue(target.compareUUIDHash(UUID_HASH));
        assertFalse(target.comparePinCode(PIN_CODE));
        assertEquals(StorageBase.STORAGE_VERSION, target.getVersion());
        assertEquals(BackupTarget.BACKUP_TARGET_STATUS_REQUEST, target.getStatus());
        assertEquals(FCM_TOKEN, target.getFcmToken());
        assertEquals(NAME, target.getName());
        assertEquals(PUBLIC_KEY, target.getPublicKey());
        assertEquals(UUID_HASH, target.getUUIDHash());
        assertEquals(LAST_CHECKED_TIME, target.getLastCheckedTime());
        assertEquals(BackupTarget.UNDEFINED_SEED_INDEX, target.getSeedIndex());
        assertNull(target.getCheckSum());
        assertEquals(RETRY_TIMES, target.getRetryTimes());
        assertEquals(RETRY_START_WAIT_TIME, target.getRetryWaitStartTime());
        assertEquals(PHONE_NUMBER, target.getPhoneNumber());
        assertEquals(PHONE_MODEL, target.getPhoneModel());
        assertNull(target.getPinCode());
        assertNull(target.getSeed());
    }

    static void assertSeededRequest(BackupTarget target) {
        assertTrue(target.isSeeded());
        assertTrue(target.compareStatus(BackupTarget.BACKUP_TARGET_STATUS_REQUEST));
        assertTrue(target.compareFcmToken(FCM_TOKEN));
        assertTrue(target.compareUUIDHash(UUID_HASH));
        assertTrue(target.comparePinCode(PIN_CODE));
        assertEquals(StorageBase.STORAGE_VERSION, target.getVersion());
        assertEquals(BackupTarget.BACKUP_TARGET_STATUS_REQUEST, target.getStatus());
        assertEquals(FCM_TOKEN, target.getFcmToken());
        assertEquals(NAME, target.getName());
        assertEquals(PUBLIC_KEY, target.getPublicKey());
        assertEquals(UUID_HASH, target.getUUIDHash());
        assertEquals(LAST_CHECKED_TIME, target.getLastCheckedTime());
        assertEquals(SEED_INDEX, target.getSeedIndex());
        assertEquals(CHECK_SUM, target.getCheckSum());
        assertEquals(RETRY_TIMES, target.getRetryTimes());
        assertEquals(RETRY_START_WAIT_TIME, target.getRetryWaitStartTime());
        assertEquals(PHONE_NUMBER, target.getPhoneNumber());
        assertEquals(PHONE_MODEL, target.getPhoneModel());
        assertEquals(PIN_CODE, target.getPinCode());
        assertEquals(SEED, target.getSeed());
    }

    static void assertOK(BackupTarget target) {
        assertTrue(target.isSeeded());
        assertTrue(target.compareStatus(BackupTarget.BACKUP_TARGET_STATUS_OK));
        assertTrue(target.compareFcmToken(FCM_TOKEN));
        assertTrue(target.compareUUIDHash(UUID_HASH));
        assertFalse(target.comparePinCode(PIN_CODE)); // Request
        assertEquals(StorageBase.STORAGE_VERSION, target.getVersion());
        assertEquals(BackupTarget.BACKUP_TARGET_STATUS_OK, target.getStatus());
        assertEquals(FCM_TOKEN, target.getFcmToken());
        assertEquals(NAME, target.getName());
        assertEquals(PUBLIC_KEY, target.getPublicKey());
        assertEquals(UUID_HASH, target.getUUIDHash());
        assertEquals(LAST_CHECKED_TIME, target.getLastCheckedTime());
        assertEquals(SEED_INDEX, target.getSeedIndex());
        assertEquals(CHECK_SUM, target.getCheckSum());
    }

    static void assertBad(BackupTarget target) {
        assertTrue(target.isSeeded());
        assertTrue(target.compareStatus(BackupTarget.BACKUP_TARGET_STATUS_BAD));
        assertTrue(target.compareFcmToken(FCM_TOKEN));
        assertTrue(target.compareUUIDHash(UUID_HASH));
        assertFalse(target.comparePinCode(PIN_CODE)); // Request
        assertEquals(StorageBase.STORAGE_VERSION, target.getVersion());
        assertEquals(BackupTarget.BACKUP_TARGET_STATUS_BAD, target.getStatus());
        assertEquals(FCM_TOKEN, target.getFcmToken());
        assertEquals(NAME, target.getName());
        assertEquals(PUBLIC_KEY, target.getPublicKey());
        assertEquals(UUID_HASH, target.getUUIDHash());
        assertEquals(LAST_CHECKED_TIME, target.getLastCheckedTime());
        assertEquals(SEED_INDEX, target.getSeedIndex());
        assertEquals(CHECK_SUM, target.getCheckSum());
    }


    @Test
    public void buildUnSeededRequestTest() {
        BackupTarget target = newUnSeededRequest();

        assertFalse(target.isEncrypted());
        assertUnSeededRequest(target);

        // encrypt
        target.encrypt();
        assertTrue(target.isEncrypted());
        assertUnSeededRequest(target);

        // decrypt
        target.decrypt();
        assertFalse(target.isEncrypted());
        assertUnSeededRequest(target);
    }

    @Test
    public void buildSeededRequestTest() {
        BackupTarget target = newSeededRequest();

        assertFalse(target.isEncrypted());
        assertSeededRequest(target);

        // encrypt
        target.encrypt();
        assertTrue(target.isEncrypted());
        assertSeededRequest(target);

        // decrypt
        target.decrypt();
        assertFalse(target.isEncrypted());
        assertSeededRequest(target);
    }

    @Test
    public void buildOKTest() {
        BackupTarget target = newOK();

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

    @Test
    public void buildBadTest() {
        BackupTarget target = newBad();

        assertFalse(target.isEncrypted());
        assertBad(target);

        // encrypt
        target.encrypt();
        assertTrue(target.isEncrypted());
        assertBad(target);

        // decrypt
        target.decrypt();
        assertFalse(target.isEncrypted());
        assertBad(target);
    }

//    @Test
//    public void MethodUpdateStatusToBad() {
//        BackupTarget target;
//
//        target = newSeededRequest();
//        target.updateStatusToBad();
//        assertBad(target);
//
//        target = newOK();
//        target.updateStatusToBad();
//        assertBad(target);
//    }

    @Test
    public void MethodUpdateLastCheckedTimeTest() {
        BackupTarget target = newOK();
        target.updateLastCheckedTime();
        assertNotEquals(LAST_CHECKED_TIME, target.getLastCheckedTime());
        assertTrue(System.currentTimeMillis() - target.getLastCheckedTime() < ALLOW_GAP);
    }
}
