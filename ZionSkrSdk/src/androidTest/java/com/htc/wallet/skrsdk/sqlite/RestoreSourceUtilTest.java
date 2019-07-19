package com.htc.wallet.skrsdk.sqlite;

import static com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity.RESTORE_SOURCE_STATUS_OK;
import static com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity.RESTORE_SOURCE_STATUS_REQUEST;

import static junit.framework.Assert.assertNull;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.sqlite.util.RestoreSourceUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4.class)
public class RestoreSourceUtilTest {
    private final Context CONTEXT = InstrumentationRegistry.getTargetContext();
    private SocialKmDatabase mSocialKmDatabase;

    //Test data 1
    private static final int STATUS = RESTORE_SOURCE_STATUS_REQUEST;
    private static final String UUID_HASH = ChecksumUtil.generateChecksum("13951cf6-5680-44d9-9f2d-bd6c8d669a2d");
    private static final String FCM_TOKEN = "fvJTMGlJaeU:APA91bE_C13HmjnuMODiHMkFSdaMKwg3pmykijTcMrqbvBVxSCItWLR1N6l78T81o"
            + "Diei0KLOP7wgjCXfDSUZjnqwV0MdDjQsWIToHYYeVbS7_cqUeLDuyVU6WEsZLqs7dysu8LFYqE8";
    private static final String PUBLIC_KEY = "qwsderftghynjmkdsadas";
    private static final long TIME_STAMP = 1234567890L;
    private static final int PIN_CODE_POSITION = 0;
    private static final String PIN_CODE = "666666";
    private static final String NAME = "Amy";

    //Test data 2
    private static final int STATUS_TEST2 = RESTORE_SOURCE_STATUS_OK;
    private static final String UUID_HASH_TEST2 = ChecksumUtil.generateChecksum("13951cf6-5680-44d9-9f2d-bd6c8d669a2e");
    private static final String FCM_TOKEN_TEST2 = "fvJTMGlJaeU:APA91bE_C13HmjnuMODiHMkFSdaMKwg3pmykijTcMrqbvBVxSCItWLR1N6l78T81p"
            + "Diei0KLOP7wgjCXfDSUZjnqwV0MdDjQsWIToHYYeVbS7_cqUeLDuyVU6WEsZLqs7dysu8LFYqE9";
    private static final String PUBLIC_KEY_TEST2 = "qwsderftghynjmkdsadat";
    private static final long TIME_STAMP_TEST2 = 1234567891L;
    private static final int PIN_CODE_POSITION_TEST2 = 1;
    private static final String PIN_CODE_TEST2 = "222222";
    private static final String NAME_TEST2 = "Amy2";

    @Before
    public void createDb() {
        mSocialKmDatabase = Room.inMemoryDatabaseBuilder(CONTEXT, SocialKmDatabase.class).build();
    }

    @After
    public void closeDb() {
        mSocialKmDatabase.close();
    }

    @Before
    public void putAll() throws InterruptedException {
        final RestoreSourceEntity restoreSourceEntity = new RestoreSourceEntity();
        restoreSourceEntity.setStatus(STATUS);
        restoreSourceEntity.setUUIDHash(UUID_HASH);
        restoreSourceEntity.setFcmToken(FCM_TOKEN);
        restoreSourceEntity.setPublicKey(PUBLIC_KEY);
        restoreSourceEntity.setTimeStamp(TIME_STAMP);
        restoreSourceEntity.setPinCodePosition(PIN_CODE_POSITION);
        restoreSourceEntity.setPinCode(PIN_CODE);
        restoreSourceEntity.setName(NAME);
        final CountDownLatch latch = new CountDownLatch(1);
        RestoreSourceUtil.put(CONTEXT, restoreSourceEntity, new DatabaseCompleteListener() {
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

        final RestoreSourceEntity restoreSourceEntity2 = new RestoreSourceEntity();
        restoreSourceEntity2.setStatus(STATUS_TEST2);
        restoreSourceEntity2.setUUIDHash(UUID_HASH_TEST2);
        restoreSourceEntity2.setFcmToken(FCM_TOKEN_TEST2);
        restoreSourceEntity2.setPublicKey(PUBLIC_KEY_TEST2);
        restoreSourceEntity2.setTimeStamp(TIME_STAMP_TEST2);
        restoreSourceEntity2.setPinCodePosition(PIN_CODE_POSITION_TEST2);
        restoreSourceEntity2.setPinCode(PIN_CODE_TEST2);
        restoreSourceEntity2.setName(NAME_TEST2);
        final CountDownLatch latch2 = new CountDownLatch(1);
        RestoreSourceUtil.put(CONTEXT, restoreSourceEntity2, new DatabaseCompleteListener() {
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
    public void getAllTest() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        RestoreSourceUtil.getAll(CONTEXT, new LoadListListener() {
            @Override
            public void onLoadFinished(List<BackupSourceEntity> backupSourceEntityList, List<BackupTargetEntity> backupTargetEntityList, List<RestoreSourceEntity> restoreSourceEntityList, List<RestoreTargetEntity> restoreTargetEntityList) {
                latch.countDown();
                assertEquals(restoreSourceEntityList.size(), 2);
            }
        });
        latch.await();
    }

    @Test
    public void getWithUUIDHashTest() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        RestoreSourceUtil.getWithUUIDHash(CONTEXT, UUID_HASH, new LoadDataListener() {
            @Override
            public void onLoadFinished(
                    BackupSourceEntity backupSourceEntity, BackupTargetEntity backupTargetEntity, RestoreSourceEntity restoreSourceEntity, RestoreTargetEntity restoreTargetEntity) {
                latch.countDown();
                assertEquals(STATUS, restoreSourceEntity.getStatus());
                assertEquals(UUID_HASH, restoreSourceEntity.getUUIDHash());
                assertEquals(FCM_TOKEN, restoreSourceEntity.getFcmToken());
                assertEquals(PUBLIC_KEY, restoreSourceEntity.getPublicKey());
                assertEquals(TIME_STAMP, restoreSourceEntity.getTimeStamp());
                assertEquals(PIN_CODE_POSITION, restoreSourceEntity.getPinCodePosition());
                assertEquals(PIN_CODE, restoreSourceEntity.getPinCode());
                assertEquals(NAME, restoreSourceEntity.getName());
            }
        });
        latch.await();
    }

    private void updateData() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        RestoreSourceUtil.getWithUUIDHash(CONTEXT, UUID_HASH, new LoadDataListener() {
            @Override
            public void onLoadFinished(
                    BackupSourceEntity backupSourceEntity, BackupTargetEntity backupTargetEntity, RestoreSourceEntity restoreSourceEntity, RestoreTargetEntity restoreTargetEntity) {
                restoreSourceEntity.setStatus(STATUS_TEST2);
                RestoreSourceUtil.update(CONTEXT, restoreSourceEntity, new DatabaseCompleteListener() {
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
        RestoreSourceUtil.getWithUUIDHash(CONTEXT, UUID_HASH, new LoadDataListener() {
            @Override
            public void onLoadFinished(
                    BackupSourceEntity backupSourceEntity, BackupTargetEntity backupTargetEntity, RestoreSourceEntity restoreSourceEntity, RestoreTargetEntity restoreTargetEntity) {
                latch.countDown();
                assertEquals(STATUS_TEST2,restoreSourceEntity.getStatus());
                assertEquals(UUID_HASH,restoreSourceEntity.getUUIDHash());
                assertEquals(FCM_TOKEN, restoreSourceEntity.getFcmToken());
                assertEquals(PUBLIC_KEY, restoreSourceEntity.getPublicKey());
                assertEquals(TIME_STAMP, restoreSourceEntity.getTimeStamp());
                assertEquals(PIN_CODE_POSITION, restoreSourceEntity.getPinCodePosition());
                assertEquals(PIN_CODE, restoreSourceEntity.getPinCode());
                assertEquals(NAME, restoreSourceEntity.getName());
            }
        });
        latch.await();
    }

    @Test
    public void removeTest() throws InterruptedException {
        RestoreSourceUtil.removeWithUUIDHash(CONTEXT, UUID_HASH);

        // Sleep 1 sec, prevent read before remove
        Thread.sleep(1000);

        final CountDownLatch latch = new CountDownLatch(1);
        RestoreSourceUtil.getWithUUIDHash(CONTEXT, UUID_HASH, new LoadDataListener() {
            @Override
            public void onLoadFinished(
                    BackupSourceEntity backupSourceEntity, BackupTargetEntity backupTargetEntity, RestoreSourceEntity restoreSourceEntity, RestoreTargetEntity restoreTargetEntity) {
                latch.countDown();
                assertNull(restoreSourceEntity);
            }
        });
        latch.await();
    }

    @Test
    public void removeAllTest() throws  InterruptedException {
        RestoreSourceUtil.removeAll(CONTEXT);

        // Sleep 1 sec, prevent read before remove
        Thread.sleep(1000);

        final CountDownLatch latch = new CountDownLatch(1);
        RestoreSourceUtil.getAll(CONTEXT, new LoadListListener() {
            @Override
            public void onLoadFinished(List<BackupSourceEntity> backupSourceEntityList, List<BackupTargetEntity> backupTargetEntityList, List<RestoreSourceEntity> restoreSourceEntityList, List<RestoreTargetEntity> restoreTargetEntityList) {
                latch.countDown();
                assertTrue(restoreSourceEntityList.isEmpty());
            }
        });
        latch.await();
    }
}
