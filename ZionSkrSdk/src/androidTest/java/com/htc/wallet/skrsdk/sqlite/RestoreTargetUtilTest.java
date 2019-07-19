package com.htc.wallet.skrsdk.sqlite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.htc.wallet.skrsdk.crypto.ChecksumUtil;
import com.htc.wallet.skrsdk.sqlite.SocialKmDatabase;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.util.RestoreTargetUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;


@RunWith(AndroidJUnit4.class)
public class RestoreTargetUtilTest {
    private final Context CONTEXT = InstrumentationRegistry.getTargetContext();
    private SocialKmDatabase mSocialKmDatabase;

    // Test data 1
    private static final String EMAIL_HASH = ChecksumUtil.generateChecksum("test1@htc.com");
    private static final String UUID_HASH = ChecksumUtil.generateChecksum("13951cf6-5680-44d9-9f2d-bd6c8d669a2d");
    private static final String UUID_BACKUP = "23951cf6-5680-44d9-9f2d-bd6c8d669a3e";
    private static final String BACKUP_UUID_HASH = ChecksumUtil.generateChecksum(UUID_BACKUP);
    private static final String TZ_ID_HASH = "tz-id-hash-1234";
    private static final boolean IS_TEST = true;
    private static final String FCM_TOKEN = "fvJTMGlJaeU:APA91bE_C13HmjnuMODiHMkFSdaMKwg3pmykijTcMrqbvBVxSCItWLR1N6l78T81o"
            + "Diei0KLOP7wgjCXfDSUZjnqwV0MdDjQsWIToHYYeVbS7_cqUeLDuyVU6WEsZLqs7dysu8LFYqE8";
    private static final String PUBLIC_KEY = "qwsderftghynjmkdsadas";
    private static final String NAME = "Amy";
    private static final long TIME_STAMP = 1234567890L;
    private static final int RETRY_TIMES = 0;
    private static final String PIN_CODE = "666666";
    private static final String PHONE_MODEL = "HTC U12+";

    // Test data 2
    private static final String EMAIL_HASH_TEST2 = ChecksumUtil.generateChecksum("test2@htc.com");
    private static final String UUID_HASH_TEST2 = ChecksumUtil.generateChecksum("13951cf6-5680-44d9-9f2d-bd6c8d669a2e");
    private static final String UUID_BACKUP_TEST2 = "23951cf6-5680-44d9-9f2d-bd6c8d669a3f";
    private static final String BACKUP_UUID_HASH_TEST2 = ChecksumUtil.generateChecksum(UUID_BACKUP_TEST2);
    private static final String TZ_ID_HASH_TEST2 = "tz-id-hash-2222";
    private static final boolean IS_TEST_TEST2 = true;
    private static final String FCM_TOKEN_TEST2 = "fvJTMGlJaeU:APA91bE_C13HmjnuMODiHMkFSdaMKwg3pmykijTcMrqbvBVxSCItWLR1N6l78T81p"
            + "Diei0KLOP7wgjCXfDSUZjnqwV0MdDjQsWIToHYYeVbS7_cqUeLDuyVU6WEsZLqs7dysu8LFYqE8";
    private static final String PUBLIC_KEY_TEST2 = "qwsderftghynjmkdsadat";
    private static final String NAME_TEST2 = "Amy2";
    private static final long TIME_STAMP_TEST2 = 1234567891L;
    private static final int RETRY_TIMES_TEST2 = 1;
    private static final String PIN_CODE_TEST2 = "222222";
    private static final String PHONE_MODEL_TEST2 = "HTC U11+";

    @Before
    public void createDb() {
        mSocialKmDatabase = Room.inMemoryDatabaseBuilder(CONTEXT, SocialKmDatabase.class).build();
    }

    @After
    public void closeDb() {
        mSocialKmDatabase.close();
    }

    @Before
    public void put() throws InterruptedException {
        final RestoreTargetEntity restoreTargetEntity = new RestoreTargetEntity();
        restoreTargetEntity.setEmailHash(EMAIL_HASH);
        restoreTargetEntity.setUUIDHash(UUID_HASH);
        restoreTargetEntity.setBackupUUIDHash(BACKUP_UUID_HASH);
        restoreTargetEntity.setTzIdHash(TZ_ID_HASH);
        restoreTargetEntity.setIsTest(IS_TEST);
        restoreTargetEntity.setFcmToken(FCM_TOKEN);
        restoreTargetEntity.setPublicKey(PUBLIC_KEY);
        restoreTargetEntity.setName(NAME);
        restoreTargetEntity.setTimeStamp(TIME_STAMP);
        restoreTargetEntity.setRetryTimes(RETRY_TIMES);
        restoreTargetEntity.setPinCode(PIN_CODE);
        restoreTargetEntity.setPhoneModel(PHONE_MODEL);

        final CountDownLatch latch = new CountDownLatch(1);
        RestoreTargetUtil.put(CONTEXT, restoreTargetEntity, new DatabaseCompleteListener() {
            @Override
            public void onComplete() {
                latch.countDown();
            }

            @Override
            public void onError(Exception exception) {
                latch.countDown();
                fail(exception.getLocalizedMessage());
            }
        });
        latch.await();

        final RestoreTargetEntity restoreTargetEntity2 = new RestoreTargetEntity();
        restoreTargetEntity2.setEmailHash(EMAIL_HASH_TEST2);
        restoreTargetEntity2.setUUIDHash(UUID_HASH_TEST2);
        restoreTargetEntity2.setBackupUUIDHash(BACKUP_UUID_HASH_TEST2);
        restoreTargetEntity2.setTzIdHash(TZ_ID_HASH_TEST2);
        restoreTargetEntity2.setIsTest(IS_TEST_TEST2);
        restoreTargetEntity2.setFcmToken(FCM_TOKEN_TEST2);
        restoreTargetEntity2.setPublicKey(PUBLIC_KEY_TEST2);
        restoreTargetEntity2.setName(NAME_TEST2);
        restoreTargetEntity2.setTimeStamp(TIME_STAMP_TEST2);
        restoreTargetEntity2.setRetryTimes(RETRY_TIMES_TEST2);
        restoreTargetEntity2.setPinCode(PIN_CODE_TEST2);
        restoreTargetEntity2.setPhoneModel(PHONE_MODEL_TEST2);

        final CountDownLatch latch2= new CountDownLatch(1);
        RestoreTargetUtil.put(CONTEXT, restoreTargetEntity2, new DatabaseCompleteListener() {
            @Override
            public void onComplete() {
                latch2.countDown();
            }

            @Override
            public void onError(Exception exception) {
                latch2.countDown();
                fail(exception.getLocalizedMessage());
            }
        });
        latch2.await();
    }

    @Test
    public void getTest() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        RestoreTargetUtil.get(CONTEXT, EMAIL_HASH, UUID_HASH, new LoadDataListener() {
            @Override
            public void onLoadFinished(
                    BackupSourceEntity backupSourceEntity, BackupTargetEntity backupTargetEntity, RestoreSourceEntity restoreSourceEntity, RestoreTargetEntity restoreTargetEntity) {
                latch.countDown();
                assertEquals(EMAIL_HASH, restoreTargetEntity.getEmailHash());
                assertEquals(UUID_HASH, restoreTargetEntity.getUUIDHash());
                assertEquals(BACKUP_UUID_HASH, restoreTargetEntity.getBackupUUIDHash());
                assertEquals(TZ_ID_HASH, restoreTargetEntity.getTzIdHash());
                assertEquals(IS_TEST, restoreTargetEntity.getIsTest());
                assertEquals(FCM_TOKEN, restoreTargetEntity.getFcmToken());
                assertEquals(PUBLIC_KEY, restoreTargetEntity.getPublicKey());
                assertEquals(NAME, restoreTargetEntity.getName());
                assertEquals(TIME_STAMP, restoreTargetEntity.getTimeStamp());
                assertEquals(RETRY_TIMES, restoreTargetEntity.getRetryTimes());
                assertEquals(PIN_CODE, restoreTargetEntity.getPinCode());
                assertEquals(PHONE_MODEL, restoreTargetEntity.getPhoneModel());
            }
        });
        latch.await();
    }

    private void updateData() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        RestoreTargetUtil.get(CONTEXT, EMAIL_HASH_TEST2, UUID_HASH_TEST2, new LoadDataListener() {
            @Override
            public void onLoadFinished(
                    BackupSourceEntity backupSourceEntity, BackupTargetEntity backupTargetEntity, RestoreSourceEntity restoreSourceEntity, RestoreTargetEntity restoreTargetEntity) {
                restoreTargetEntity.setRetryTimes(RETRY_TIMES);
                restoreTargetEntity.setPhoneModel(PHONE_MODEL);
                RestoreTargetUtil.update(CONTEXT, restoreTargetEntity, new DatabaseCompleteListener() {
                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Exception exception) {
                        latch.countDown();
                        fail(exception.getLocalizedMessage());
                    }
                });
            }
        });
        latch.await();
    }

    @Test
    public void updateTest() throws InterruptedException {
        updateData();

        final CountDownLatch latch = new CountDownLatch(1);
        RestoreTargetUtil.get(CONTEXT, EMAIL_HASH_TEST2, UUID_HASH_TEST2, new LoadDataListener() {
            @Override
            public void onLoadFinished(
                    BackupSourceEntity backupSourceEntity, BackupTargetEntity backupTargetEntity, RestoreSourceEntity restoreSourceEntity, RestoreTargetEntity restoreTargetEntity) {
                latch.countDown();
                assertEquals(EMAIL_HASH_TEST2, restoreTargetEntity.getEmailHash());
                assertEquals(UUID_HASH_TEST2, restoreTargetEntity.getUUIDHash());
                assertEquals(BACKUP_UUID_HASH_TEST2, restoreTargetEntity.getBackupUUIDHash());
                assertEquals(TZ_ID_HASH_TEST2, restoreTargetEntity.getTzIdHash());
                assertEquals(IS_TEST_TEST2, restoreTargetEntity.getIsTest());
                assertEquals(FCM_TOKEN_TEST2, restoreTargetEntity.getFcmToken());
                assertEquals(PUBLIC_KEY_TEST2, restoreTargetEntity.getPublicKey());
                assertEquals(NAME_TEST2, restoreTargetEntity.getName());
                assertEquals(TIME_STAMP_TEST2, restoreTargetEntity.getTimeStamp());
                assertEquals(RETRY_TIMES, restoreTargetEntity.getRetryTimes());
                assertEquals(PIN_CODE_TEST2, restoreTargetEntity.getPinCode());
                assertEquals(PHONE_MODEL, restoreTargetEntity.getPhoneModel());
            }
        });
        latch.await();
    }

    @Test
    public void removeTest() throws InterruptedException {
        RestoreTargetUtil.remove(CONTEXT, EMAIL_HASH, UUID_HASH);

        // Sleep 1 sec, prevent read before remove
        Thread.sleep(1000);

        final CountDownLatch latch = new CountDownLatch(1);
        RestoreTargetUtil.get(CONTEXT, EMAIL_HASH, UUID_HASH, new LoadDataListener() {
            @Override
            public void onLoadFinished(
                    BackupSourceEntity backupSourceEntity, BackupTargetEntity backupTargetEntity, RestoreSourceEntity restoreSourceEntity, RestoreTargetEntity restoreTargetEntity) {
                latch.countDown();
                assertNull(backupSourceEntity);
            }
        });
        latch.await();
    }
}
