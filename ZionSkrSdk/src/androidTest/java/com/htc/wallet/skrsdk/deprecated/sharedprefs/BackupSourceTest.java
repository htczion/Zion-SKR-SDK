package com.htc.wallet.skrsdk.deprecated.sharedprefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.htc.wallet.skrsdk.crypto.ChecksumUtil;
import com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupSource;
import com.htc.wallet.skrsdk.deprecated.sharedprefs.StorageBase;
import com.htc.wallet.skrsdk.util.RetryUtil;

import org.junit.Test;

public class BackupSourceTest {

    // General
    static final String EMAIL_HASH = ChecksumUtil.generateChecksum("abc@htc.com");
    private static final String FCM_TOKEN = "fvJTMGlJaeU:APA91bE_C13HmjnuMODiHMkFSdaMKwg3pmykijTcMrqbvBVxSCItWLR1N6l78T81o"
            + "Diei0KLOP7wgjCXfDSUZjnqwV0MdDjQsWIToHYYeVbS7_cqUeLDuyVU6WEsZLqs7dysu8LFYqE8";
    private static final String NAME = "Amy";
    private static final String MY_NAME = "Bob";
    private static final long TIME_STAMP = 1234567890L;
    static final String UUID_HASH = ChecksumUtil.generateChecksum("23951cf6-5680-44d9-9f2d-bd6c8d669a2d");
    static final String UUID_HASH2 = ChecksumUtil.generateChecksum("23951cf6-5680-44d9-9f2d-bd6c8d669a2e");

    // Request
    private static final String PUBLIC_KEY = "qwsderftghynjmkdsadas";
    private static final long LAST_VERIFY_TIME = 0;
    // Request Method Variable
    private static final long LAST_REQUEST_TIME = 123123123L;
    private static final String PIN_CODE = "123654";

    // OK
    private static final String SEED_ENCRYPTED = "Q154W53RG45GIN3H5J4D8T15YVKIGF48FSFFD5FDSGI8PU";
    private static final String CHECKSUM = ChecksumUtil.generateChecksum(SEED_ENCRYPTED);
    private static final long ALLOW_GAP = 10L;

    static BackupSource newBackupSourceRequest() {
        return new BackupSource.Builder(BackupSource.BACKUP_SOURCE_STATUS_REQUEST)
                .setEmailHash(EMAIL_HASH)
                .setFcmToken(FCM_TOKEN)
                .setName(NAME)
                .setUUIDHash(UUID_HASH)
                .setTimeStamp(TIME_STAMP)
                .setPublicKey(PUBLIC_KEY)
                .setMyName(MY_NAME)
                .build();
    }

    static BackupSource newBackupSourceRequest2() {
        return new BackupSource.Builder(BackupSource.BACKUP_SOURCE_STATUS_REQUEST)
                .setEmailHash(EMAIL_HASH)
                .setFcmToken(FCM_TOKEN)
                .setName(NAME)
                .setUUIDHash(UUID_HASH2)
                .setTimeStamp(TIME_STAMP)
                .setPublicKey(PUBLIC_KEY)
                .setMyName(MY_NAME)
                .build();
    }

    static BackupSource newBackupSourceOK() {
        return new BackupSource.Builder(BackupSource.BACKUP_SOURCE_STATUS_OK)
                .setEmailHash(EMAIL_HASH)
                .setFcmToken(FCM_TOKEN)
                .setName(NAME)
                .setUUIDHash(UUID_HASH)
                .setTimeStamp(TIME_STAMP)
                .setPublicKey(PUBLIC_KEY)
                .setSeed(SEED_ENCRYPTED)
                .setCheckSum(CHECKSUM)
                .build();
    }

    static BackupSource newBackupSourceOK2() {
        return new BackupSource.Builder(BackupSource.BACKUP_SOURCE_STATUS_OK)
                .setEmailHash(EMAIL_HASH)
                .setFcmToken(FCM_TOKEN)
                .setName(NAME)
                .setUUIDHash(UUID_HASH2)
                .setTimeStamp(TIME_STAMP)
                .setPublicKey(PUBLIC_KEY)
                .setSeed(SEED_ENCRYPTED)
                .setCheckSum(CHECKSUM)
                .build();
    }

    static void assertBackupSourceRequest(BackupSource source) {
        assertTrue(source.compareStatus(BackupSource.BACKUP_SOURCE_STATUS_REQUEST));
        assertTrue(source.compareEmailHash(EMAIL_HASH));
        assertTrue(source.compareUUIDHash(UUID_HASH));
        assertEquals(BackupSource.BACKUP_SOURCE_STATUS_REQUEST, source.getStatus());
        assertEquals(EMAIL_HASH, source.getEmailHash());
        assertEquals(FCM_TOKEN, source.getFcmToken());
        assertEquals(NAME, source.getName());
        assertEquals(TIME_STAMP, source.getTimeStamp());
        assertEquals(UUID_HASH, source.getUUIDHash());
        assertEquals(PUBLIC_KEY, source.getPublicKey());
        assertEquals(MY_NAME, source.getMyName());
        assertEquals(BackupSource.INIT_RETRY_TIMES, source.getRetryTimes());
        assertEquals(LAST_VERIFY_TIME, source.getLastVerifyTime());
    }

    static void assertBackupSourceRequest2(BackupSource source) {
        assertTrue(source.compareStatus(BackupSource.BACKUP_SOURCE_STATUS_REQUEST));
        assertTrue(source.compareEmailHash(EMAIL_HASH));
        assertTrue(source.compareUUIDHash(UUID_HASH2));
        assertEquals(StorageBase.STORAGE_VERSION, source.getVersion());
        assertEquals(BackupSource.BACKUP_SOURCE_STATUS_REQUEST, source.getStatus());
        assertEquals(EMAIL_HASH, source.getEmailHash());
        assertEquals(FCM_TOKEN, source.getFcmToken());
        assertEquals(NAME, source.getName());
        assertEquals(TIME_STAMP, source.getTimeStamp());
        assertEquals(UUID_HASH2, source.getUUIDHash());
        assertEquals(PUBLIC_KEY, source.getPublicKey());
        assertEquals(MY_NAME, source.getMyName());
        assertEquals(BackupSource.INIT_RETRY_TIMES, source.getRetryTimes());
        assertEquals(LAST_VERIFY_TIME, source.getLastVerifyTime());
    }

    static void assertBackupSourceOK(BackupSource source) {
        assertTrue(source.compareStatus(BackupSource.BACKUP_SOURCE_STATUS_OK));
        assertTrue(source.compareEmailHash(EMAIL_HASH));
        assertTrue(source.compareUUIDHash(UUID_HASH));
        assertEquals(StorageBase.STORAGE_VERSION, source.getVersion());
        assertEquals(BackupSource.BACKUP_SOURCE_STATUS_OK, source.getStatus());
        assertEquals(EMAIL_HASH, source.getEmailHash());
        assertEquals(FCM_TOKEN, source.getFcmToken());
        assertEquals(NAME, source.getName());
        assertEquals(TIME_STAMP, source.getTimeStamp());
        assertEquals(UUID_HASH, source.getUUIDHash());
        assertEquals(PUBLIC_KEY, source.getPublicKey());
        assertEquals(SEED_ENCRYPTED, source.getSeed());
        assertEquals(CHECKSUM, source.getCheckSum());
    }

    static void assertBackupSourceOK2(BackupSource source) {
        assertTrue(source.compareStatus(BackupSource.BACKUP_SOURCE_STATUS_OK));
        assertTrue(source.compareEmailHash(EMAIL_HASH));
        assertTrue(source.compareUUIDHash(UUID_HASH2));
        assertEquals(StorageBase.STORAGE_VERSION, source.getVersion());
        assertEquals(BackupSource.BACKUP_SOURCE_STATUS_OK, source.getStatus());
        assertEquals(EMAIL_HASH, source.getEmailHash());
        assertEquals(FCM_TOKEN, source.getFcmToken());
        assertEquals(NAME, source.getName());
        assertEquals(TIME_STAMP, source.getTimeStamp());
        assertEquals(UUID_HASH2, source.getUUIDHash());
        assertEquals(PUBLIC_KEY, source.getPublicKey());
        assertEquals(SEED_ENCRYPTED, source.getSeed());
        assertEquals(CHECKSUM, source.getCheckSum());
    }

    @Test
    public void buildRequestTest() {
        BackupSource source = newBackupSourceRequest();

        assertFalse(source.isEncrypted());
        assertBackupSourceRequest(source);

        // encrypt
        source.encrypt();
        assertTrue(source.isEncrypted());
        assertBackupSourceRequest(source);

        // decrypt
        source.decrypt();
        assertFalse(source.isEncrypted());
        assertBackupSourceRequest(source);
    }

    @Test
    public void requestMethodLastRequestTimeTest() {
        BackupSource source = newBackupSourceRequest();
        assertTrue(System.currentTimeMillis() - source.getLastRequestTime() < ALLOW_GAP);
        source.setLastRequestTime(LAST_REQUEST_TIME);
        assertEquals(LAST_REQUEST_TIME, source.getLastRequestTime());
    }

    @Test
    public void requestMethodRetryTimesTest() throws InterruptedException {
        long lastRetryWaitStartTime;
        BackupSource source = newBackupSourceRequest();
        // Before
        assertFalse(source.getIsPinCodeError());
        assertEquals(BackupSource.INIT_RETRY_TIMES, source.getRetryTimes());
        lastRetryWaitStartTime = source.getRetryWaitStartTime();
        assertEquals(BackupSource.INIT_RETRY_WAIT_START_TIME, source.getRetryWaitStartTime());

        // Set
        for (int retryTimes = 1; retryTimes <= 10; retryTimes++) {
            Thread.sleep(100);
            source.setRetryTimes(retryTimes);
            assertEquals(retryTimes, source.getRetryTimes());
            assertTrue(source.getIsPinCodeError());
            if (retryTimes % RetryUtil.MAXIMUM_TRY_NUMBER == 0) {
                assertNotEquals(lastRetryWaitStartTime, source.getRetryWaitStartTime());
                assertTrue(System.currentTimeMillis() - source.getRetryWaitStartTime() < ALLOW_GAP);
                lastRetryWaitStartTime = source.getRetryWaitStartTime();
            } else {
                assertEquals(lastRetryWaitStartTime, source.getRetryWaitStartTime());
            }
        }
    }

    @Test
    public void requestMethodSetPinCodeTest() {
        BackupSource source = newBackupSourceRequest();
        source.setRetryTimes(1);
        assertTrue(source.getIsPinCodeError());
        source.setPinCode(PIN_CODE);
        assertFalse(source.getIsPinCodeError());
        assertTrue(System.currentTimeMillis() - source.getLastVerifyTime() < ALLOW_GAP);
        assertEquals(PIN_CODE, source.getPinCode());
    }

    @Test
    public void requestMethodClearPinCodeTest() {
        BackupSource source = newBackupSourceRequest();
        source.setRetryTimes(1);
        assertTrue(source.getIsPinCodeError());
        source.clearPinCode();
        assertFalse(source.getIsPinCodeError());
        assertEquals(BackupSource.INIT_LAST_VERIFY_TIME, source.getLastVerifyTime());
        assertEquals(BackupSource.INIT_PIN_CODE, source.getPinCode());
    }

    @Test
    public void buildOKTest() {
        BackupSource source = newBackupSourceOK();

        assertFalse(source.isEncrypted());
        assertBackupSourceOK(source);

        // encrypt
        source.encrypt();
        assertTrue(source.isEncrypted());
        assertBackupSourceOK(source);

        // decrypt
        source.decrypt();
        assertFalse(source.isEncrypted());
        assertBackupSourceOK(source);
    }

    @Test
    public void OKMethodTest() {
        BackupSource source = newBackupSourceOK();
        assertTrue(source.compareStatus(BackupSource.BACKUP_SOURCE_STATUS_OK));
        source.setDone();
        assertTrue(source.compareStatus(BackupSource.BACKUP_SOURCE_STATUS_DONE));
    }
}
