package com.htc.wallet.skrsdk.sqlite;

import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_BAD;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_NO_RESPONSE;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_OK;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_REQUEST_WAIT_OK;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.htc.wallet.skrsdk.crypto.ChecksumUtil;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupTargetUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4.class)
public class BackupTargetUtilTest {
    private final Context CONTEXT = InstrumentationRegistry.getTargetContext();
    private SocialKmDatabase mSocialKmDatabase;

    // Test data 1
    private static final int STATUS = BACKUP_TARGET_STATUS_REQUEST;
    private static final String UUID_HASH = ChecksumUtil.generateChecksum("13951cf6-5680-44d9-9f2d-bd6c8d669a2d");
    private static final String FCM_TOKEN = "fvJTMGlJaeU:APA91bE_C13HmjnuMODiHMkFSdaMKwg3pmykijTcMrqbvBVxSCItWLR1N6l78T81o";
    private static final String NAME = "Bob==...,,,  1212!@#$%^&*()_+===.,.,.,";
    private static final String PUBLIC_KEY = "qwsderftghynjmkdsadas";
    private static final int RETRY_TIMES = 0;
    private static final long RETRY_START_WAIT_TIME = 0;
    private static final String PHONE_NUMBER = "0987-654-111";
    private static final String PHONE_MODEL = "HTC U12+";
    private static final String PIN_CODE = "666666";
    private static final String SEED = "Q154W53RG45GIN3H5J4D8T15YVKIGF48FSFFD5FDSGI8P";
    private static final long LAST_CHECKED_TIME = 1234567890L;
    private static final int SEED_INDEX = 3;
    private static final String CHECK_SUM = ChecksumUtil.generateChecksum(SEED);

    // Test data 2
    private static final int STATUS_TEST2 = BACKUP_TARGET_STATUS_OK;
    private static final String UUID_HASH_TEST2 = ChecksumUtil.generateChecksum("23951cf6-5680-44d9-9f2d-bd6c8d669a2d");
    private static final String FCM_TOKEN_TEST2 = "gvJTMGlJaeU:APA91bE_C13HmjnuMODiHMkFSdaMKwg3pmykijTcMrqbvBVxSCItWLR1N6l78T81o";
    private static final String NAME_TEST2 = "Bob2==...,,,  1212!@#$%^&*()_+===.,.,.,";
    private static final String PUBLIC_KEY_TEST2 = "qwsderftghynjmkdsadaa";
    private static final int RETRY_TIMES_TEST2 = 0;
    private static final long RETRY_START_WAIT_TIME_TEST2 = 1;
    private static final String PHONE_NUMBER_TEST2 = "0987-654-222";
    private static final String PHONE_MODEL_TEST2 = "HTC U11+";
    private static final String PIN_CODE_TEST2 = "777777";
    private static final String SEED_TEST2 = "Q154W53RG45GIN3H5J4D8T15YVKIGF48FSFFD5FDSGI8Q";
    private static final long LAST_CHECKED_TIME_TEST2 = 1234567891L;
    private static final int SEED_INDEX_TEST2 = 3;
    private static final String CHECK_SUM_TEST2 = ChecksumUtil.generateChecksum(SEED_TEST2);

    // Test data 3
    private static final int STATUS_TEST3 = BACKUP_TARGET_STATUS_BAD;
    private static final String UUID_HASH_TEST3 = ChecksumUtil.generateChecksum("33951cf6-5680-44d9-9f2d-bd6c8d669a2d");
    private static final String FCM_TOKEN_TEST3 = "hvJTMGlJaeU:APA91bE_C13HmjnuMODiHMkFSdaMKwg3pmykijTcMrqbvBVxSCItWLR1N6l78T81o";
    private static final String NAME_TEST3 = "Bob3==...,,,  1212!@#$%^&*()_+===.,.,.,";
    private static final String PUBLIC_KEY_TEST3 = "qwsderftghynjmkdsadab";
    private static final int RETRY_TIMES_TEST3 = 0;
    private static final long RETRY_START_WAIT_TIME_TEST3 = 2;
    private static final String PHONE_NUMBER_TEST3 = "0987-654-333";
    private static final String PHONE_MODEL_TEST3 = "HTC U11";
    private static final String PIN_CODE_TEST3 = "888888";
    private static final String SEED_TEST3 = "Q154W53RG45GIN3H5J4D8T15YVKIGF48FSFFD5FDSGI8R";
    private static final long LAST_CHECKED_TIME_TEST3 = 1234567892L;
    private static final int SEED_INDEX_TEST3 = 3;
    private static final String CHECK_SUM_TEST3 = ChecksumUtil.generateChecksum(SEED_TEST3);

    // Test 4
    private static final int STATUS_TEST4 = BACKUP_TARGET_STATUS_NO_RESPONSE;
    private static final String UUID_HASH_TEST4 = ChecksumUtil.generateChecksum("43951cf6-5680-44d9-9f2d-bd6c8d669a2d");
    private static final String FCM_TOKEN_TEST4 = "ivJTMGlJaeU:APA91bE_C13HmjnuMODiHMkFSdaMKwg3pmykijTcMrqbvBVxSCItWLR1N6l78T81o";
    private static final String NAME_TEST4 = "Bob4==...,,,  1212!@#$%^&*()_+===.,.,.,";
    private static final String PUBLIC_KEY_TEST4 = "qwsderftghynjmkdsadac";
    private static final int RETRY_TIMES_TEST4 = 0;
    private static final long RETRY_START_WAIT_TIME_TEST4 = 2;
    private static final String PHONE_NUMBER_TEST4 = "0987-654-444";
    private static final String PHONE_MODEL_TEST4 = "HTC 10";
    private static final String PIN_CODE_TEST4 = "888888";
    private static final String SEED_TEST4 = "Q154W53RG45GIN3H5J4D8T15YVKIGF48FSFFD5FDSGI8S";
    private static final long LAST_CHECKED_TIME_TEST4 = 1234567893L;
    private static final int SEED_INDEX_TEST4 = 3;
    private static final String CHECK_SUM_TEST4 = ChecksumUtil.generateChecksum(SEED_TEST4);

    // TEST 5
    private static final int STATUS_TEST5 = BACKUP_TARGET_STATUS_REQUEST_WAIT_OK;
    private static final String UUID_HASH_TEST5 = ChecksumUtil.generateChecksum("53951cf6-5680-44d9-9f2d-bd6c8d669a2d");
    private static final String FCM_TOKEN_TEST5 = "jvJTMGlJaeU:APA91bE_C13HmjnuMODiHMkFSdaMKwg3pmykijTcMrqbvBVxSCItWLR1N6l78T81o";
    private static final String NAME_TEST5 = "Bob5==...,,,  1212!@#$%^&*()_+===.,.,.,";
    private static final String PUBLIC_KEY_TEST5 = "qwsderftghynjmkdsadad";
    private static final int RETRY_TIMES_TEST5 = 0;
    private static final long RETRY_START_WAIT_TIME_TEST5 = 2;
    private static final String PHONE_NUMBER_TEST5 = "0987-654-555";
    private static final String PHONE_MODEL_TEST5 = "HTC U12 life";
    private static final String PIN_CODE_TEST5 = "999999";
    private static final String SEED_TEST5 = "Q154W53RG45GIN3H5J4D8T15YVKIGF48FSFFD5FDSGI8T";
    private static final long LAST_CHECKED_TIME_TEST5 = 1234567894L;
    private static final int SEED_INDEX_TEST5 = 3;
    private static final String CHECK_SUM_TEST5 = ChecksumUtil.generateChecksum(SEED_TEST5);


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
        final BackupTargetEntity backupTargetEntity = new BackupTargetEntity();
        backupTargetEntity.setStatus(STATUS);
        backupTargetEntity.setFcmToken(FCM_TOKEN);
        backupTargetEntity.setName(NAME);
        backupTargetEntity.setPublicKey(PUBLIC_KEY);
        backupTargetEntity.setUUIDHash(UUID_HASH);
        backupTargetEntity.setLastCheckedTime(LAST_CHECKED_TIME);
        backupTargetEntity.setSeedIndex(SEED_INDEX);
        backupTargetEntity.setCheckSum(CHECK_SUM);
        backupTargetEntity.setRetryTimes(RETRY_TIMES);
        backupTargetEntity.setRetryWaitStartTime(RETRY_START_WAIT_TIME);
        backupTargetEntity.setPhoneNumber(PHONE_NUMBER);
        backupTargetEntity.setPhoneModel(PHONE_MODEL);
        backupTargetEntity.setPinCode(PIN_CODE);

        final BackupTargetEntity backupTargetEntity2 = new BackupTargetEntity();
        backupTargetEntity2.setStatus(STATUS_TEST2);
        backupTargetEntity2.setFcmToken(FCM_TOKEN_TEST2);
        backupTargetEntity2.setName(NAME_TEST2);
        backupTargetEntity2.setPublicKey(PUBLIC_KEY_TEST2);
        backupTargetEntity2.setUUIDHash(UUID_HASH_TEST2);
        backupTargetEntity2.setLastCheckedTime(LAST_CHECKED_TIME_TEST2);
        backupTargetEntity2.setSeedIndex(SEED_INDEX_TEST2);
        backupTargetEntity2.setCheckSum(CHECK_SUM_TEST2);
        backupTargetEntity2.setRetryTimes(RETRY_TIMES_TEST2);
        backupTargetEntity2.setRetryWaitStartTime(RETRY_START_WAIT_TIME_TEST2);
        backupTargetEntity2.setPhoneNumber(PHONE_NUMBER_TEST2);
        backupTargetEntity2.setPhoneModel(PHONE_MODEL_TEST2);
        backupTargetEntity2.setPinCode(PIN_CODE_TEST2);

        final BackupTargetEntity backupTargetEntity3 = new BackupTargetEntity();
        backupTargetEntity3.setStatus(STATUS_TEST3);
        backupTargetEntity3.setFcmToken(FCM_TOKEN_TEST3);
        backupTargetEntity3.setName(NAME_TEST3);
        backupTargetEntity3.setPublicKey(PUBLIC_KEY_TEST3);
        backupTargetEntity3.setUUIDHash(UUID_HASH_TEST3);
        backupTargetEntity3.setLastCheckedTime(LAST_CHECKED_TIME_TEST3);
        backupTargetEntity3.setSeedIndex(SEED_INDEX_TEST3);
        backupTargetEntity3.setCheckSum(CHECK_SUM_TEST3);
        backupTargetEntity3.setRetryTimes(RETRY_TIMES_TEST3);
        backupTargetEntity3.setRetryWaitStartTime(RETRY_START_WAIT_TIME_TEST3);
        backupTargetEntity3.setPhoneNumber(PHONE_NUMBER_TEST3);
        backupTargetEntity3.setPhoneModel(PHONE_MODEL_TEST3);
        backupTargetEntity3.setPinCode(PIN_CODE_TEST3);

        final BackupTargetEntity backupTargetEntity4 = new BackupTargetEntity();
        backupTargetEntity4.setStatus(STATUS_TEST4);
        backupTargetEntity4.setFcmToken(FCM_TOKEN_TEST4);
        backupTargetEntity4.setName(NAME_TEST4);
        backupTargetEntity4.setPublicKey(PUBLIC_KEY_TEST4);
        backupTargetEntity4.setUUIDHash(UUID_HASH_TEST4);
        backupTargetEntity4.setLastCheckedTime(LAST_CHECKED_TIME_TEST4);
        backupTargetEntity4.setSeedIndex(SEED_INDEX_TEST4);
        backupTargetEntity4.setCheckSum(CHECK_SUM_TEST4);
        backupTargetEntity4.setRetryTimes(RETRY_TIMES_TEST4);
        backupTargetEntity4.setRetryWaitStartTime(RETRY_START_WAIT_TIME_TEST4);
        backupTargetEntity4.setPhoneNumber(PHONE_NUMBER_TEST4);
        backupTargetEntity4.setPhoneModel(PHONE_MODEL_TEST4);
        backupTargetEntity4.setPinCode(PIN_CODE_TEST4);

        final BackupTargetEntity backupTargetEntity5 = new BackupTargetEntity();
        backupTargetEntity5.setStatus(STATUS_TEST5);
        backupTargetEntity5.setFcmToken(FCM_TOKEN_TEST5);
        backupTargetEntity5.setName(NAME_TEST5);
        backupTargetEntity5.setPublicKey(PUBLIC_KEY_TEST5);
        backupTargetEntity5.setUUIDHash(UUID_HASH_TEST5);
        backupTargetEntity5.setLastCheckedTime(LAST_CHECKED_TIME_TEST5);
        backupTargetEntity5.setSeedIndex(SEED_INDEX_TEST5);
        backupTargetEntity5.setCheckSum(CHECK_SUM_TEST5);
        backupTargetEntity5.setRetryTimes(RETRY_TIMES_TEST5);
        backupTargetEntity5.setRetryWaitStartTime(RETRY_START_WAIT_TIME_TEST5);
        backupTargetEntity5.setPhoneNumber(PHONE_NUMBER_TEST5);
        backupTargetEntity5.setPhoneModel(PHONE_MODEL_TEST5);
        backupTargetEntity5.setPinCode(PIN_CODE_TEST5);

        final List<BackupTargetEntity> backupTargetList = new ArrayList<>();
        backupTargetList.add(backupTargetEntity);
        backupTargetList.add(backupTargetEntity2);
        backupTargetList.add(backupTargetEntity3);
        backupTargetList.add(backupTargetEntity4);
        backupTargetList.add(backupTargetEntity5);
        final CountDownLatch latch = new CountDownLatch(1);
        BackupTargetUtil.putList(CONTEXT, backupTargetList, new DatabaseCompleteListener() {
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
    }

    @Test
    public void getAllTest() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        BackupTargetUtil.getAll(CONTEXT, new LoadListListener() {
            @Override
            public void onLoadFinished(List<BackupSourceEntity> backupSourceEntityList, List<BackupTargetEntity> backupTargetEntityList, List<RestoreSourceEntity> restoreSourceEntityList, List<RestoreTargetEntity> restoreTargetEntityList) {
                latch.countDown();
                assertEquals(backupTargetEntityList.size(), 5);
            }
        });
        latch.await();
    }

    @Test
    public void getAllOKTest() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        BackupTargetUtil.getAllOK(CONTEXT, new LoadListListener() {
            @Override
            public void onLoadFinished(List<BackupSourceEntity> backupSourceEntityList, List<BackupTargetEntity> backupTargetEntityList, List<RestoreSourceEntity> restoreSourceEntityList, List<RestoreTargetEntity> restoreTargetEntityList) {
                latch.countDown();
                assertEquals(backupTargetEntityList.size(), 1);
            }
        });
        latch.await();
    }

    @Test
    public void getAllOKAndNoResponseTest() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        BackupTargetUtil.getAllOKAndNoResponse(CONTEXT, new LoadListListener() {
            @Override
            public void onLoadFinished(List<BackupSourceEntity> backupSourceEntityList, List<BackupTargetEntity> backupTargetEntityList, List<RestoreSourceEntity> restoreSourceEntityList, List<RestoreTargetEntity> restoreTargetEntityList) {
                latch.countDown();
                assertEquals(backupTargetEntityList.size(), 2);
            }
        });
        latch.await();
    }

    @Test
    public void getBadTest() {
        int badStatusCount = BackupTargetUtil.getBadCount(CONTEXT);
        assertEquals(badStatusCount, 1);
    }

    @Test
    public void getTest() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        BackupTargetUtil.get(
                InstrumentationRegistry.getTargetContext(), UUID_HASH, new LoadDataListener() {
            @Override
            public void onLoadFinished(
                    BackupSourceEntity backupSourceEntity, BackupTargetEntity backupTargetEntity, RestoreSourceEntity restoreSourceEntity, RestoreTargetEntity restoreTargetEntity) {
                assertEquals(STATUS, backupTargetEntity.getStatus());
                assertEquals(FCM_TOKEN, backupTargetEntity.getFcmToken());
                assertEquals(NAME, backupTargetEntity.getName());
                assertEquals(PUBLIC_KEY, backupTargetEntity.getPublicKey());
                assertEquals(UUID_HASH, backupTargetEntity.getUUIDHash());
                assertEquals(LAST_CHECKED_TIME, backupTargetEntity.getLastCheckedTime());
                assertEquals(SEED_INDEX, backupTargetEntity.getSeedIndex());
                assertEquals(CHECK_SUM, backupTargetEntity.getCheckSum());
                assertEquals(RETRY_TIMES, backupTargetEntity.getRetryTimes());
                assertEquals(RETRY_START_WAIT_TIME, backupTargetEntity.getRetryWaitStartTime());
                assertEquals(PHONE_NUMBER, backupTargetEntity.getPhoneNumber());
                assertEquals(PHONE_MODEL, backupTargetEntity.getPhoneModel());
                assertEquals(PIN_CODE, backupTargetEntity.getPinCode());
                latch.countDown();
            }
        });
        latch.await();
    }

    private void updateData() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        BackupTargetUtil.get(CONTEXT, UUID_HASH, new LoadDataListener() {
            @Override
            public void onLoadFinished(
                    BackupSourceEntity backupSourceEntity, BackupTargetEntity backupTargetEntity, RestoreSourceEntity restoreSourceEntity, RestoreTargetEntity restoreTargetEntity) {

                backupTargetEntity.setStatus(STATUS_TEST2);
                BackupTargetUtil.update(CONTEXT, backupTargetEntity, new DatabaseCompleteListener() {
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
        BackupTargetUtil.get(CONTEXT, UUID_HASH, new LoadDataListener() {
            @Override
            public void onLoadFinished(
                    BackupSourceEntity backupSourceEntity, BackupTargetEntity backupTargetEntity, RestoreSourceEntity restoreSourceEntity, RestoreTargetEntity restoreTargetEntity) {
                latch.countDown();
                assertEquals(STATUS_TEST2, backupTargetEntity.getStatus());
                assertEquals(FCM_TOKEN, backupTargetEntity.getFcmToken());
                assertEquals(NAME, backupTargetEntity.getName());
                assertEquals(PUBLIC_KEY, backupTargetEntity.getPublicKey());
                assertEquals(UUID_HASH, backupTargetEntity.getUUIDHash());
                assertEquals(LAST_CHECKED_TIME, backupTargetEntity.getLastCheckedTime());
                assertEquals(SEED_INDEX, backupTargetEntity.getSeedIndex());
                assertEquals(CHECK_SUM, backupTargetEntity.getCheckSum());
                assertEquals(RETRY_TIMES, backupTargetEntity.getRetryTimes());
                assertEquals(RETRY_START_WAIT_TIME, backupTargetEntity.getRetryWaitStartTime());
                assertEquals(PHONE_MODEL, backupTargetEntity.getPhoneModel());
            }
        });
        latch.await();
    }

    @Test
    public void removeTest() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        BackupTargetUtil.remove(CONTEXT, UUID_HASH_TEST2, new DatabaseCompleteListener() {
            @Override
            public void onComplete() {
                BackupTargetUtil.get(CONTEXT, UUID_HASH_TEST2, new LoadDataListener() {
                    @Override
                    public void onLoadFinished(
                            BackupSourceEntity backupSourceEntity,
                            BackupTargetEntity backupTargetEntity,
                            RestoreSourceEntity restoreSourceEntity,
                            RestoreTargetEntity restoreTargetEntity) {
                        assertNull(backupTargetEntity);
                        latch.countDown();
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                fail(exception.toString());
            }
        });
        latch.await();
    }
}
