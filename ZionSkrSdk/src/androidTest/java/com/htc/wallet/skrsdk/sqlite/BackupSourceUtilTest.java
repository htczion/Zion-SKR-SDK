package com.htc.wallet.skrsdk.sqlite;

import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_DONE;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_OK;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_REQUEST_AUTO_BACKUP;

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
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupSourceUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4.class)
public class BackupSourceUtilTest {
    private final Context CONTEXT = InstrumentationRegistry.getTargetContext();
    private SocialKmDatabase mSocialKmDatabase;

    //Test data 1
    private static final int STATUS = BACKUP_SOURCE_STATUS_REQUEST;
    private static final String EMAIL_HASH = ChecksumUtil.generateChecksum("test1@htc.com");
    private static final String UUID_HASH = ChecksumUtil.generateChecksum("13951cf6-5680-44d9-9f2d-bd6c8d669a2d");
    private static final String FCM_TOKEN = "fvJTMGlJaeU:APA91bE_C13HmjnuMODiHMkFSdaMKwg3pmykijTcMrqbvBVxSCItWLR1N6l78T81o"
            + "Diei0KLOP7wgjCXfDSUZjnqwV0MdDjQsWIToHYYeVbS7_cqUeLDuyVU6WEsZLqs7dysu8LFYqE8";
    private static final String NAME = "Amy";
    private static final String TZ_ID_HASH = "tz-id-hash-1234";
    private static final long TIME_STAMP = 1234567890L;
    private static final boolean IS_TEST = true;
    private static final String PUBLIC_KEY = "qwsderftghynjmkdsadas";
    private static final String MY_NAME = "Bob";
    private static final String SEED = "Q154W53RG45GIN3H5J4D8T15YVKIGF48FSFFD5FDSGI8PU";
    private static final String CHECKSUM = ChecksumUtil.generateChecksum(SEED);
    private static final int RETRY_TIMES = 0;
    private static final long RETRY_START_WAIT_TIME = 0;
    private static final String PIN_CODE = "666666";
    private static final long LAST_VERIFY_TIME = TIME_STAMP + 100L;
    private static final long LAST_REQUEST_TIME = 123123123L;
    private static final int IS_PIN_CODE_ERROR = 1;

    //Test data 2
    private static final int STATUS_TEST2 = BACKUP_SOURCE_STATUS_OK;
    private static final String EMAIL_HASH_TEST2 = ChecksumUtil.generateChecksum("test2@htc.com");
    private static final String UUID_HASH_TEST2 = ChecksumUtil.generateChecksum("13951cf6-5680-44d9-9f2d-bd6c8d669a2e");
    private static final String FCM_TOKEN_TEST2 = "fvJTMGlJaeU:APA91bE_C13HmjnuMODiHMkFSdaMKwg3pmykijTcMrqbvBVxSCItWLR1N6l78T81p"
            + "Diei0KLOP7wgjCXfDSUZjnqwV0MdDjQsWIToHYYeVbS7_cqUeLDuyVU6WEsZLqs7dysu8LFYqE8";
    private static final String NAME_TEST2 = "Amy2";
    private static final String TZ_ID_HASH_TEST2 = "tz-id-hash-2222";
    private static final long TIME_STAMP_TEST2 = 1234567892L;
    private static final boolean IS_TEST_TEST2 = true;
    private static final String PUBLIC_KEY_TEST2 = "qwsderftghynjmkdsadat";
    private static final String MY_NAME_TEST2 = "Bob2";
    private static final String SEED_TEST2 = "Q154W53RG45GIN3H5J4D8T15YVKIGF48FSFFD5FDSGI8PV";
    private static final String CHECKSUM_TEST2 = ChecksumUtil.generateChecksum(SEED_TEST2);
    private static final int RETRY_TIMES_TEST2 = 1;
    private static final long RETRY_START_WAIT_TIME_TEST2 = TIME_STAMP_TEST2;
    private static final String PIN_CODE_TEST2 = "222222";
    private static final long LAST_VERIFY_TIME_TEST2 = TIME_STAMP + 100L;
    private static final long LAST_REQUEST_TIME_TEST2 = 123123122L;
    private static final int IS_PIN_CODE_ERROR_TEST2 = 1;

    //Test data 3
    private static final int STATUS_TEST3 = BACKUP_SOURCE_STATUS_DONE;
    private static final String EMAIL_HASH_TEST3 = ChecksumUtil.generateChecksum("test3@htc.com");
    private static final String UUID_HASH_TEST3 = ChecksumUtil.generateChecksum("13951cf6-5680-44d9-9f2d-bd6c8d669a2f");
    private static final String FCM_TOKEN_TEST3 = "fvJTMGlJaeU:APA91bE_C13HmjnuMODiHMkFSdaMKwg3pmykijTcMrqbvBVxSCItWLR1N6l78T81q"
            + "Diei0KLOP7wgjCXfDSUZjnqwV0MdDjQsWIToHYYeVbS7_cqUeLDuyVU6WEsZLqs7dysu8LFYqE8";
    private static final String NAME_TEST3 = "Amy3";
    private static final String TZ_ID_HASH_TEST3 = "tz-id-hash-3333";
    private static final long TIME_STAMP_TEST3 = 1234567893L;
    private static final boolean IS_TEST_TEST3 = true;
    private static final String PUBLIC_KEY_TEST3 = "qwsderftghynjmkdsadau";
    private static final String MY_NAME_TEST3 = "Bob3";
    private static final String SEED_TEST3 = "Q154W53RG45GIN3H5J4D8T15YVKIGF48FSFFD5FDSGI8PW";
    private static final String CHECKSUM_TEST3 = ChecksumUtil.generateChecksum(SEED_TEST3);
    private static final int RETRY_TIMES_TEST3 = 2;
    private static final long RETRY_START_WAIT_TIME_TEST3 = TIME_STAMP_TEST3;
    private static final String PIN_CODE_TEST3 = "333333";
    private static final long LAST_VERIFY_TIME_TEST3 = TIME_STAMP + 100L;
    private static final long LAST_REQUEST_TIME_TEST3 = 123123333L;
    private static final int IS_PIN_CODE_ERROR_TEST3 = 1;

    //Test data 4
    private static final int STATUS_TEST4 = BACKUP_SOURCE_STATUS_REQUEST_AUTO_BACKUP;
    private static final String UUID_HASH_TEST4 = ChecksumUtil.generateChecksum("13951cf6-5680-44d9-9f2d-bd6c8d669a2g");
    private static final String EMAIL_HASH_TEST4 = ChecksumUtil.generateChecksum("test4@htc.com");
    private static final String FCM_TOKEN_TEST4 = "fvJTMGlJaeU:APA91bE_C13HmjnuMODiHMkFSdaMKwg3pmykijTcMrqbvBVxSCItWLR1N6l78T81s"
            + "Diei0KLOP7wgjCXfDSUZjnqwV0MdDjQsWIToHYYeVbS7_cqUeLDuyVU6WEsZLqs7dysu8LFYqE8";
    private static final String NAME_TEST4 = "Amy";
    private static final String TZ_ID_HASH_TEST4 = "tz-id-hash-4444";
    private static final long TIME_STAMP_TEST4 = 1234567894L;
    private static final boolean IS_TEST_TEST4 = true;
    private static final String PUBLIC_KEY_TEST4 = "qwsderftghynjmkdsadav";
    private static final String MY_NAME_TEST4 = "Bob4";
    private static final String SEED_TEST4 = "Q154W53RG45GIN3H5J4D8T15YVKIGF48FSFFD5FDSGI8PX";
    private static final String CHECKSUM_TEST4 = ChecksumUtil.generateChecksum(SEED_TEST4);
    private static final int RETRY_TIMES_TEST4 = 2;
    private static final long RETRY_START_WAIT_TIME_TEST4 = TIME_STAMP_TEST4;
    private static final String PIN_CODE_TEST4 = "4444";
    private static final long LAST_VERIFY_TIME_TEST4 = TIME_STAMP + 100L;
    private static final long LAST_REQUEST_TIME_TEST4 = 123123444L;
    private static final int IS_PIN_CODE_ERROR_TEST4 = 1;

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
        final BackupSourceEntity backupSourceEntity = new BackupSourceEntity();
        backupSourceEntity.setStatus(STATUS);
        backupSourceEntity.setEmailHash(EMAIL_HASH);
        backupSourceEntity.setUUIDHash(UUID_HASH);
        backupSourceEntity.setFcmToken(FCM_TOKEN);
        backupSourceEntity.setName(NAME);
        backupSourceEntity.setTzIdHash(TZ_ID_HASH);
        backupSourceEntity.setTimeStamp(TIME_STAMP);
        backupSourceEntity.setIsTest(IS_TEST);
        backupSourceEntity.setPublicKey(PUBLIC_KEY);
        backupSourceEntity.setMyName(MY_NAME);
        backupSourceEntity.setCheckSum(CHECKSUM);
        backupSourceEntity.setRetryTimes(RETRY_TIMES);
        backupSourceEntity.setRetryWaitStartTime(RETRY_START_WAIT_TIME);
        backupSourceEntity.setPinCode(PIN_CODE);
        backupSourceEntity.setLastVerifyTime(LAST_VERIFY_TIME);
        backupSourceEntity.setLastRequestTime(LAST_REQUEST_TIME);
        backupSourceEntity.setIsPinCodeError(IS_PIN_CODE_ERROR);

        final CountDownLatch latch = new CountDownLatch(1);
        BackupSourceUtil.put(CONTEXT, backupSourceEntity, new DatabaseCompleteListener() {
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

        final BackupSourceEntity backupSourceEntity2 = new BackupSourceEntity();
        backupSourceEntity2.setStatus(STATUS_TEST2);
        backupSourceEntity2.setEmailHash(EMAIL_HASH_TEST2);
        backupSourceEntity2.setUUIDHash(UUID_HASH_TEST2);
        backupSourceEntity2.setFcmToken(FCM_TOKEN_TEST2);
        backupSourceEntity2.setName(NAME_TEST2);
        backupSourceEntity2.setTzIdHash(TZ_ID_HASH_TEST2);
        backupSourceEntity2.setTimeStamp(TIME_STAMP_TEST2);
        backupSourceEntity2.setIsTest(IS_TEST_TEST2);
        backupSourceEntity2.setPublicKey(PUBLIC_KEY_TEST2);
        backupSourceEntity2.setMyName(MY_NAME_TEST2);
        backupSourceEntity2.setCheckSum(CHECKSUM_TEST2);
        backupSourceEntity2.setRetryTimes(RETRY_TIMES_TEST2);
        backupSourceEntity2.setRetryWaitStartTime(RETRY_START_WAIT_TIME_TEST2);
        backupSourceEntity2.setPinCode(PIN_CODE_TEST2);
        backupSourceEntity2.setLastVerifyTime(LAST_VERIFY_TIME_TEST2);
        backupSourceEntity2.setLastRequestTime(LAST_REQUEST_TIME_TEST2);
        backupSourceEntity2.setIsPinCodeError(IS_PIN_CODE_ERROR_TEST2);

        final CountDownLatch latch2 = new CountDownLatch(1);
        BackupSourceUtil.put(CONTEXT, backupSourceEntity2, new DatabaseCompleteListener() {
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

        final BackupSourceEntity backupSourceEntity3 = new BackupSourceEntity();
        backupSourceEntity3.setStatus(STATUS_TEST3);
        backupSourceEntity3.setEmailHash(EMAIL_HASH_TEST3);
        backupSourceEntity3.setUUIDHash(UUID_HASH_TEST3);
        backupSourceEntity3.setFcmToken(FCM_TOKEN_TEST3);
        backupSourceEntity3.setName(NAME_TEST3);
        backupSourceEntity3.setTzIdHash(TZ_ID_HASH_TEST3);
        backupSourceEntity3.setTimeStamp(TIME_STAMP_TEST3);
        backupSourceEntity3.setIsTest(IS_TEST_TEST3);
        backupSourceEntity3.setPublicKey(PUBLIC_KEY_TEST3);
        backupSourceEntity3.setMyName(MY_NAME_TEST3);
        backupSourceEntity3.setCheckSum(CHECKSUM_TEST3);
        backupSourceEntity3.setRetryTimes(RETRY_TIMES_TEST3);
        backupSourceEntity3.setRetryWaitStartTime(RETRY_START_WAIT_TIME_TEST3);
        backupSourceEntity3.setPinCode(PIN_CODE_TEST3);
        backupSourceEntity3.setLastVerifyTime(LAST_VERIFY_TIME_TEST3);
        backupSourceEntity3.setLastRequestTime(LAST_REQUEST_TIME_TEST3);
        backupSourceEntity3.setIsPinCodeError(IS_PIN_CODE_ERROR_TEST3);

        final CountDownLatch latch3 = new CountDownLatch(1);
        BackupSourceUtil.put(CONTEXT, backupSourceEntity3, new DatabaseCompleteListener() {
            @Override
            public void onComplete() {
                latch3.countDown();
            }

            @Override
            public void onError(Exception exception) {
                latch3.countDown();
                fail(exception.getLocalizedMessage());
            }
        });

        latch3.await();

        final BackupSourceEntity backupSourceEntity4 = new BackupSourceEntity();
        backupSourceEntity4.setStatus(STATUS_TEST4);
        backupSourceEntity4.setEmailHash(EMAIL_HASH_TEST4);
        backupSourceEntity4.setUUIDHash(UUID_HASH_TEST4);
        backupSourceEntity4.setFcmToken(FCM_TOKEN_TEST4);
        backupSourceEntity4.setName(NAME_TEST4);
        backupSourceEntity4.setTzIdHash(TZ_ID_HASH_TEST4);
        backupSourceEntity4.setTimeStamp(TIME_STAMP_TEST4);
        backupSourceEntity4.setIsTest(IS_TEST_TEST4);
        backupSourceEntity4.setPublicKey(PUBLIC_KEY_TEST4);
        backupSourceEntity4.setMyName(MY_NAME_TEST4);
        backupSourceEntity4.setCheckSum(CHECKSUM_TEST4);
        backupSourceEntity4.setRetryTimes(RETRY_TIMES_TEST4);
        backupSourceEntity4.setRetryWaitStartTime(RETRY_START_WAIT_TIME_TEST4);
        backupSourceEntity4.setPinCode(PIN_CODE_TEST4);
        backupSourceEntity4.setLastVerifyTime(LAST_VERIFY_TIME_TEST4);
        backupSourceEntity4.setLastRequestTime(LAST_REQUEST_TIME_TEST4);
        backupSourceEntity4.setIsPinCodeError(IS_PIN_CODE_ERROR_TEST4);

        final CountDownLatch latch4 = new CountDownLatch(1);
        BackupSourceUtil.put(CONTEXT, backupSourceEntity4, new DatabaseCompleteListener() {
            @Override
            public void onComplete() {
                latch4.countDown();
            }

            @Override
            public void onError(Exception exception) {
                latch4.countDown();
                fail(exception.getLocalizedMessage());
            }
        });

        latch4.await();
    }

    @Test
    public void getWithUUIDHashTest() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        BackupSourceUtil.getWithUUIDHash(CONTEXT, EMAIL_HASH, UUID_HASH, new LoadDataListener() {
            @Override
            public void onLoadFinished(
                    BackupSourceEntity backupSourceEntity, BackupTargetEntity backupTargetEntity, RestoreSourceEntity restoreSourceEntity, RestoreTargetEntity restoreTargetEntity) {
                latch.countDown();
                assertEquals(STATUS, backupSourceEntity.getStatus());
                assertEquals(EMAIL_HASH, backupSourceEntity.getEmailHash());
                assertEquals(UUID_HASH, backupSourceEntity.getUUIDHash());
                assertEquals(FCM_TOKEN, backupSourceEntity.getFcmToken());
                assertEquals(NAME, backupSourceEntity.getName());
                assertEquals(TZ_ID_HASH, backupSourceEntity.getTzIdHash());
                assertEquals(TIME_STAMP, backupSourceEntity.getTimeStamp());
                assertEquals(IS_TEST, backupSourceEntity.getIsTest());
                assertEquals(RETRY_TIMES, backupSourceEntity.getRetryTimes());
                assertEquals(RETRY_START_WAIT_TIME, backupSourceEntity.getRetryWaitStartTime());
                assertEquals(PIN_CODE, backupSourceEntity.getPinCode());
                assertEquals(LAST_VERIFY_TIME, backupSourceEntity.getLastVerifyTime());
                assertEquals(LAST_REQUEST_TIME, backupSourceEntity.getLastRequestTime());
                assertEquals(IS_PIN_CODE_ERROR, backupSourceEntity.getIsPinCodeError());
            }
        });
        latch.await();
    }

    @Test
    public void getAllOkTest() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        BackupSourceUtil.getAllOK(CONTEXT, new LoadListListener() {
            @Override
            public void onLoadFinished(List<BackupSourceEntity> backupSourceEntityList, List<BackupTargetEntity> backupTargetEntityList, List<RestoreSourceEntity> restoreSourceEntityList, List<RestoreTargetEntity> restoreTargetEntityList) {
                latch.countDown();
                assertEquals(backupSourceEntityList.size(), 2);
            }
        });
        latch.await();
    }

    @Test
    public void getOkTest() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        BackupSourceUtil.getOK(CONTEXT, EMAIL_HASH_TEST2, UUID_HASH_TEST2, new LoadDataListener() {
            @Override
            public void onLoadFinished(
                    BackupSourceEntity backupSourceEntity, BackupTargetEntity backupTargetEntity, RestoreSourceEntity restoreSourceEntity, RestoreTargetEntity restoreTargetEntity) {
                latch.countDown();
                assertEquals(STATUS_TEST2, backupSourceEntity.getStatus());
                assertEquals(EMAIL_HASH_TEST2, backupSourceEntity.getEmailHash());
                assertEquals(UUID_HASH_TEST2, backupSourceEntity.getUUIDHash());
                assertEquals(FCM_TOKEN_TEST2, backupSourceEntity.getFcmToken());
                assertEquals(NAME_TEST2, backupSourceEntity.getName());
                assertEquals(TZ_ID_HASH_TEST2, backupSourceEntity.getTzIdHash());
                assertEquals(TIME_STAMP_TEST2, backupSourceEntity.getTimeStamp());
                assertEquals(IS_TEST_TEST2, backupSourceEntity.getIsTest());
                assertEquals(RETRY_TIMES_TEST2, backupSourceEntity.getRetryTimes());
                assertEquals(RETRY_START_WAIT_TIME_TEST2, backupSourceEntity.getRetryWaitStartTime());
                assertEquals(PIN_CODE_TEST2, backupSourceEntity.getPinCode());
                assertEquals(LAST_VERIFY_TIME_TEST2, backupSourceEntity.getLastVerifyTime());
                assertEquals(LAST_REQUEST_TIME_TEST2, backupSourceEntity.getLastRequestTime());
                assertEquals(IS_PIN_CODE_ERROR_TEST2, backupSourceEntity.getIsPinCodeError());
            }
        });
        latch.await();
    }

    @Test
    public void getAllRequestAutoBackupTest() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        BackupSourceUtil.getAllRequestAutoBackup(CONTEXT, new LoadListListener() {
            @Override
            public void onLoadFinished(List<BackupSourceEntity> backupSourceEntityList, List<BackupTargetEntity> backupTargetEntityList, List<RestoreSourceEntity> restoreSourceEntityList, List<RestoreTargetEntity> restoreTargetEntityList) {
                latch.countDown();
                assertEquals(backupSourceEntityList.size(), 1);
            }
        });
        latch.await();
    }

    private void updateData() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        BackupSourceUtil.getWithUUIDHash(CONTEXT, EMAIL_HASH, UUID_HASH, new LoadDataListener() {
            @Override
            public void onLoadFinished(
                    BackupSourceEntity backupSourceEntity, BackupTargetEntity backupTargetEntity, RestoreSourceEntity restoreSourceEntity, RestoreTargetEntity restoreTargetEntity) {
               backupSourceEntity.setStatus(STATUS_TEST2);
               BackupSourceUtil.update(CONTEXT, backupSourceEntity, new DatabaseCompleteListener() {
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
        BackupSourceUtil.getWithUUIDHash(CONTEXT, EMAIL_HASH, UUID_HASH, new LoadDataListener() {
            @Override
            public void onLoadFinished(
                    BackupSourceEntity backupSourceEntity, BackupTargetEntity backupTargetEntity, RestoreSourceEntity restoreSourceEntity, RestoreTargetEntity restoreTargetEntity) {
                latch.countDown();
                assertEquals(STATUS_TEST2, backupSourceEntity.getStatus());
                assertEquals(EMAIL_HASH, backupSourceEntity.getEmailHash());
                assertEquals(UUID_HASH, backupSourceEntity.getUUIDHash());
                assertEquals(FCM_TOKEN, backupSourceEntity.getFcmToken());
                assertEquals(NAME, backupSourceEntity.getName());
                assertEquals(TZ_ID_HASH, backupSourceEntity.getTzIdHash());
                assertEquals(TIME_STAMP, backupSourceEntity.getTimeStamp());
                assertEquals(IS_TEST, backupSourceEntity.getIsTest());
                assertEquals(RETRY_TIMES, backupSourceEntity.getRetryTimes());
                assertEquals(RETRY_START_WAIT_TIME, backupSourceEntity.getRetryWaitStartTime());
                assertEquals(PIN_CODE, backupSourceEntity.getPinCode());
                assertEquals(LAST_VERIFY_TIME, backupSourceEntity.getLastVerifyTime());
                assertEquals(LAST_REQUEST_TIME, backupSourceEntity.getLastRequestTime());
                assertEquals(IS_PIN_CODE_ERROR, backupSourceEntity.getIsPinCodeError());
            }
        });
        latch.await();
    }

    @Test
    public void removeTest() throws InterruptedException {
        BackupSourceUtil.remove(CONTEXT, EMAIL_HASH_TEST4, UUID_HASH_TEST4);

        // Sleep 1 sec, prevent read before remove
        Thread.sleep(1000);

        final CountDownLatch latch = new CountDownLatch(1);
        BackupSourceUtil.getWithUUIDHash(CONTEXT, EMAIL_HASH_TEST4, UUID_HASH_TEST4, new LoadDataListener() {
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
