package com.htc.wallet.skrsdk.sqlite.util;

import static com.htc.wallet.skrsdk.applink.AppLinkConstant.APP_LINK_VALIDITY_TIME;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_BAD;
import static com.htc.wallet.skrsdk.sqlite.util.BackupDataConstants.BACKUP_DATA_CLEAR_LEGACY_V1_VERSION;
import static com.htc.wallet.skrsdk.sqlite.util.BackupDataConstants.BACKUP_DATA_DB_INIT_VERSION;
import static com.htc.wallet.skrsdk.sqlite.util.BackupDataConstants.BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION;
import static com.htc.wallet.skrsdk.sqlite.util.BackupSourceUtil.backupSourceToBackupSourceEntity;
import static com.htc.wallet.skrsdk.sqlite.util.BackupTargetUtil.backupTargetToBackupTargetEntity;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupSource;
import com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupTarget;
import com.htc.wallet.skrsdk.error.DatabaseSaveException;
import com.htc.wallet.skrsdk.legacy.v1.LegacyBackupDataV1;
import com.htc.wallet.skrsdk.legacy.v1.LegacyV1Util;
import com.htc.wallet.skrsdk.monitor.CheckBackupVersionJobService;
import com.htc.wallet.skrsdk.sqlite.SocialKmDatabase;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.util.Callback;
import com.htc.wallet.skrsdk.util.IntentUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.NotificationUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BackupDataStatusUtil {
    private static final String TAG = "BackupDataStatusUtil";
    private static final long TIMEOUT = 60L; // 60 seconds

    private BackupDataStatusUtil() {
        throw new AssertionError();
    }

    public static void getRequestsCount(
            @NonNull final Context context,
            @NonNull final Callback<Integer> callback) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(callback, "callback is null");

        DatabaseWorkManager.getInstance().enqueueRead(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                clearExpiredPending(context);
                int backupSourceSize = SocialKmDatabase.getInstance(context).backupSourceDao()
                        .getAllPending().size();
                int backupTargetSize = SocialKmDatabase.getInstance(context).backupTargetDao()
                        .getAllPending().size();
                int restoreTargetSize = SocialKmDatabase.getInstance(context).restoreTargetDao()
                        .getAll().size();
                final int totalSize = backupSourceSize + backupTargetSize + restoreTargetSize;

                callback.onResponse(totalSize);
            }
        });
    }

    public static void getAllPending(
            @NonNull final Context context,
            @NonNull final LoadListListener loadListListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(loadListListener);

        DatabaseWorkManager.getInstance().enqueueRead(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                clearExpiredPending(context);
                List<BackupSourceEntity> backupSourceList = SocialKmDatabase.getInstance(context)
                        .backupSourceDao().getAllPending();
                if (backupSourceList != null) {
                    for (BackupSourceEntity source : backupSourceList) {
                        source.decrypt();
                    }
                } else {
                    LogUtil.logDebug(TAG, "Failed to getAllPending from backupSourceDao");
                }
                List<BackupTargetEntity> backupTargetList = SocialKmDatabase.getInstance(context)
                        .backupTargetDao().getAllPending();
                if (backupTargetList != null) {
                    for (BackupTargetEntity source : backupTargetList) {
                        source.decrypt();
                    }
                } else {
                    LogUtil.logDebug(TAG, "Failed to getAllPending from backupTargetDao");
                }
                List<RestoreTargetEntity> restoreTargetList = SocialKmDatabase.getInstance(context)
                        .restoreTargetDao().getAll();
                if (restoreTargetList != null) {
                    for (RestoreTargetEntity restoreTarget : restoreTargetList) {
                        restoreTarget.decrypt();
                    }
                } else {
                    LogUtil.logDebug(TAG, "Failed to getAll from restoreTargetDao");
                }

                loadListListener.onLoadFinished(
                        backupSourceList,
                        backupTargetList,
                        null,
                        restoreTargetList);
            }
        });
    }

    public static void clearAllData(@NonNull final Context context) {
        DatabaseWorkManager.getInstance().enqueueWrite(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                LogUtil.logInfo(TAG, "Start to clear all data");
                SocialKmDatabase.clearAllTablesData(context);
                LogUtil.logInfo(TAG, "End of clear all data");
            }
        });
    }

    private static boolean isDbExist(@NonNull final Context context, @NonNull final String name) {
        // Check arguments
        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(name)) {
            LogUtil.logError(TAG, "name is null or empty", new IllegalArgumentException());
            return false;
        }

        // Get database path
        // context.getDatabasePath(), Returns the absolute path on the filesystem
        // https://developer.android.com/reference/android/content/Context.html#getDatabasePath
        // (java.lang.String)
        File file = context.getApplicationContext().getDatabasePath(name);
        String path = file.getPath();

        // Try to open database
        SQLiteDatabase database = null;
        try {
            database = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
        } catch (SQLiteException e) {
            // database doesn't exist yet.
        } finally {
            IOUtils.closeQuietly(database);
        }
        return database != null;
    }

    // sharedPrefs to db
    public static void saveSharePrefsToDb(
            @NonNull final Context context,
            @NonNull final DatabaseCompleteListener databaseCompleteListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(databaseCompleteListener);

        DatabaseWorkManager.getInstance().enqueueWrite(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                // Already check at sharedPrefs
                // getCheckSocialKeyRecoverySaveToDb(context)
                // Do the check prevent update to db more than one times
                if (isDbExist(context, SocialKmDatabase.DATABASE_NAME)
                        || SocialKmDatabase.getInstance(context)
                        .backupSourceDao().getAll().size() > 0
                        || SocialKmDatabase.getInstance(context)
                        .backupTargetDao().getAll().size() > 0) {
                    LogUtil.logDebug(TAG, "You have move sp to db");
                    databaseCompleteListener.onComplete();
                    return;
                }

                // backupSource
                List<BackupSource> backupSources = com.htc.wallet.skrsdk.deprecated.sharedprefs
                        .BackupSourceUtil.getAllOK(context);
                List<BackupSourceEntity> backupSourceEntities = new ArrayList<>();
                for (BackupSource backupSource : backupSources) {
                    BackupSourceEntity backupSourceEntity =
                            backupSourceToBackupSourceEntity(backupSource);
                    // Set version to BACKUP_DATA_DB_INIT_VERSION, default is the
                    // last version (BACKUP_DATA_CURRENT_VERSION)
                    backupSourceEntity.setVersion(BACKUP_DATA_DB_INIT_VERSION);
                    backupSourceEntity.encrypt();
                    backupSourceEntities.add(backupSourceEntity);
                }

                // backupTarget
                List<BackupTarget> backupTargetList = com.htc.wallet.skrsdk.deprecated.sharedprefs.
                        BackupTargetUtil.getAll(context);
                List<BackupTargetEntity> backupTargetEntities = new ArrayList<>();
                for (BackupTarget backupTarget : backupTargetList) {
                    BackupTargetEntity backupTargetEntity =
                            backupTargetToBackupTargetEntity(backupTarget);
                    // Set version to BACKUP_DATA_DB_INIT_VERSION, default is the
                    // last version (BACKUP_DATA_CURRENT_VERSION)
                    backupTargetEntity.setVersion(BACKUP_DATA_DB_INIT_VERSION);
                    backupTargetEntity.encrypt();
                    backupTargetEntities.add(backupTargetEntity);
                }

                long[] backupSourceResult = SocialKmDatabase.getInstance(context)
                        .backupSourceDao().insertAll(backupSourceEntities);
                long[] backupTargetResult = SocialKmDatabase.getInstance(context)
                        .backupTargetDao().insertAll(backupTargetEntities);

                if (backupSourceResult.length == backupSources.size()
                        && backupTargetResult.length
                        == backupTargetEntities.size()) {
                    if (!BackupSourceUtil.checkIfSaveCorrect(context)) {
                        databaseCompleteListener.onError(new DatabaseSaveException(
                                "Save BackupSource from sharedPrefs to "
                                        + "database have some data wrong"));
                        return;
                    }
                    if (!BackupTargetUtil.checkIfSaveCorrect(context)) {
                        databaseCompleteListener.onError(new DatabaseSaveException(
                                "Save BackupTarget from sharedPrefs to "
                                        + "database have some data wrong"));
                        return;
                    }
                    databaseCompleteListener.onComplete();
                } else {
                    databaseCompleteListener.onError(
                            new DatabaseSaveException("Save data not complete"));
                }
            }
        });
    }

    @WorkerThread
    private static void clearExpiredPending(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        final long currentTime = System.currentTimeMillis();

        final List<BackupSourceEntity> backupSourceList =
                SocialKmDatabase.getInstance(context).backupSourceDao().getAllPending();
        if (backupSourceList == null) {
            LogUtil.logError(TAG, "backupSourceList is null", new IllegalStateException());
        } else {
            for (BackupSourceEntity backupSourceEntity : backupSourceList) {
                if (backupSourceEntity.compareStatus(
                        BackupSourceEntity.BACKUP_SOURCE_STATUS_REQUEST)
                        && (currentTime - backupSourceEntity.getTimeStamp())
                        > APP_LINK_VALIDITY_TIME) {
                    LogUtil.logDebug(TAG, "Clear expired BackupSource,"
                            + " name=" + backupSourceEntity.getName());
                    BackupSourceUtil.remove(
                            context,
                            backupSourceEntity.getEmailHash(),
                            backupSourceEntity.getUUIDHash());
                }
            }
        }

        final List<BackupTargetEntity> backupTargetList =
                SocialKmDatabase.getInstance(context).backupTargetDao().getAllPending();
        if (backupTargetList == null) {
            LogUtil.logError(TAG, "backupTargetList is null", new IllegalStateException());
        } else {
            for (BackupTargetEntity backupTargetEntity : backupTargetList) {
                if (backupTargetEntity.isStatusPending()
                        && (currentTime - backupTargetEntity.getLastCheckedTime())
                        > APP_LINK_VALIDITY_TIME) {
                    LogUtil.logDebug(TAG, "Clear expired BackupTarget, "
                            + "name=" + backupTargetEntity.getName());

                    BackupTargetUtil.remove(context, backupTargetEntity.getUUIDHash(),
                            new DatabaseCompleteListener() {
                                @Override
                                public void onComplete() {
                                    LogUtil.logInfo(TAG, "Clear expired backupTarget complete");
                                }

                                @Override
                                public void onError(Exception exception) {
                                    LogUtil.logError(TAG, "Clear expired backupTarget failed");
                                }
                            });
                }
            }
        }

        final List<RestoreTargetEntity> restoreTargetList =
                SocialKmDatabase.getInstance(context).restoreTargetDao().getAll();
        if (restoreTargetList == null) {
            LogUtil.logError(TAG, "restoreTargetList is null", new IllegalStateException());
        } else {
            for (RestoreTargetEntity restoreTargetEntity : restoreTargetList) {
                if ((currentTime - restoreTargetEntity.getTimeStamp()) > APP_LINK_VALIDITY_TIME) {
                    LogUtil.logDebug(TAG, "Clear expired RestoreTarget, "
                            + "name=" + restoreTargetEntity.getName());
                    RestoreTargetUtil.remove(
                            context,
                            restoreTargetEntity.getEmailHash(),
                            restoreTargetEntity.getUUIDHash());
                }
            }
        }
    }

    // Backup data version 0 --> 1
    public static void updateDataWithEncrypted(
            final Context context, final DatabaseCompleteListener databaseCompleteListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(databaseCompleteListener);

        DatabaseWorkManager.getInstance().enqueueWrite(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                // We need to ensure all things have been finished, thus lock this
                // work thread
                final CountDownLatch latch = new CountDownLatch(4);

                BackupSourceUtil.getAll(context, new LoadListListener() {
                    @Override
                    public void onLoadFinished(
                            List<BackupSourceEntity> backupSourceEntityList,
                            List<BackupTargetEntity> backupTargetEntityList,
                            List<RestoreSourceEntity>
                                    restoreSourceEntityList,
                            List<RestoreTargetEntity>
                                    restoreTargetEntityList) {
                        if (backupSourceEntityList == null) {
                            LogUtil.logError(TAG, "backupSourceEntityList is null");
                            latch.countDown();
                            return;
                        }
                        final AtomicInteger backupSourceCount =
                                new AtomicInteger(backupSourceEntityList.size());
                        if (backupSourceCount.get() == 0) {
                            latch.countDown();
                        }
                        for (BackupSourceEntity backupSourceEntity :
                                backupSourceEntityList) {
                            if (!backupSourceEntity.isSensitiveDataEncrypted()) {
                                // Set version to
                                // BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION,
                                // default is the last version
                                // (BACKUP_DATA_CURRENT_VERSION)
                                backupSourceEntity.setVersion(
                                        BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION);
                                BackupSourceUtil.update(context, backupSourceEntity,
                                        new DatabaseCompleteListener() {
                                            @Override
                                            public void onComplete() {
                                                LogUtil.logDebug(TAG, "update backupSource "
                                                        + "data with AES encryption");
                                                if (backupSourceCount.decrementAndGet() == 0) {
                                                    latch.countDown();
                                                }
                                            }

                                            @Override
                                            public void onError(
                                                    Exception exception) {
                                                LogUtil.logError(TAG, "update error, "
                                                        + "e=" + exception);
                                                if (backupSourceCount.decrementAndGet() == 0) {
                                                    latch.countDown();
                                                }
                                            }
                                        });
                            } else {
                                if (backupSourceCount.decrementAndGet() == 0) {
                                    latch.countDown();
                                }
                            }
                        }
                    }
                });

                BackupTargetUtil.getAll(context, new LoadListListener() {
                    @Override
                    public void onLoadFinished(
                            List<BackupSourceEntity> backupSourceEntityList,
                            List<BackupTargetEntity> backupTargetEntityList,
                            List<RestoreSourceEntity>
                                    restoreSourceEntityList,
                            List<RestoreTargetEntity>
                                    restoreTargetEntityList) {
                        if (backupTargetEntityList == null) {
                            LogUtil.logError(TAG, "backupTargetEntityList is null");
                            latch.countDown();
                            return;
                        }
                        final AtomicInteger backupTargetCount =
                                new AtomicInteger(backupTargetEntityList.size());
                        if (backupTargetCount.get() == 0) {
                            latch.countDown();
                        }
                        for (BackupTargetEntity backupTargetEntity : backupTargetEntityList) {
                            if (!backupTargetEntity.isSensitiveDataEncrypted()) {
                                // Set version to
                                // BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION,
                                // default is the last version
                                // (BACKUP_DATA_CURRENT_VERSION)
                                backupTargetEntity.setVersion(
                                        BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION);
                                BackupTargetUtil.update(context, backupTargetEntity,
                                        new DatabaseCompleteListener() {
                                            @Override
                                            public void onComplete() {
                                                LogUtil.logDebug(TAG, "update backupTarget "
                                                        + "data with AES encryption");
                                                if (backupTargetCount.decrementAndGet() == 0) {
                                                    latch.countDown();
                                                }
                                            }

                                            @Override
                                            public void onError(Exception exception) {
                                                LogUtil.logError(TAG, "update error, "
                                                        + "e= " + exception);
                                                if (backupTargetCount.decrementAndGet() == 0) {
                                                    latch.countDown();
                                                }
                                            }
                                        });
                            } else {
                                if (backupTargetCount.decrementAndGet() == 0) {
                                    latch.countDown();
                                }
                            }
                        }
                    }
                });

                RestoreSourceUtil.getAll(context, new LoadListListener() {
                    @Override
                    public void onLoadFinished(
                            List<BackupSourceEntity> backupSourceEntityList,
                            List<BackupTargetEntity> backupTargetEntityList,
                            List<RestoreSourceEntity>
                                    restoreSourceEntityList,
                            List<RestoreTargetEntity>
                                    restoreTargetEntityList) {
                        if (restoreSourceEntityList == null) {
                            LogUtil.logError(TAG, "restoreSourceEntityList is null");
                            latch.countDown();
                            return;
                        }
                        final AtomicInteger restoreSourceCount =
                                new AtomicInteger(restoreSourceEntityList.size());
                        if (restoreSourceCount.get() == 0) {
                            latch.countDown();
                        }
                        for (RestoreSourceEntity restoreSourceEntity : restoreSourceEntityList) {
                            if (!restoreSourceEntity.isSensitiveDataEncrypted()) {
                                // Set version to
                                // BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION,
                                // default is the last version
                                // (BACKUP_DATA_CURRENT_VERSION)
                                restoreSourceEntity.setVersion(
                                        BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION);
                                RestoreSourceUtil.update(context, restoreSourceEntity,
                                        new DatabaseCompleteListener() {
                                            @Override
                                            public void onComplete() {
                                                LogUtil.logDebug(TAG, "update restoreSource "
                                                        + "data with AES encryption");
                                                if (restoreSourceCount.decrementAndGet() == 0) {
                                                    latch.countDown();
                                                }
                                            }

                                            @Override
                                            public void onError(Exception exception) {
                                                LogUtil.logError(TAG, "update error, "
                                                        + "e= " + exception);
                                                if (restoreSourceCount.decrementAndGet() == 0) {
                                                    latch.countDown();
                                                }
                                            }
                                        });
                            } else {
                                if (restoreSourceCount.decrementAndGet() == 0) {
                                    latch.countDown();
                                }
                            }
                        }
                    }
                });

                RestoreTargetUtil.getAll(context, new LoadListListener() {
                    @Override
                    public void onLoadFinished(
                            List<BackupSourceEntity> backupSourceEntityList,
                            List<BackupTargetEntity> backupTargetEntityList,
                            List<RestoreSourceEntity>
                                    restoreSourceEntityList,
                            List<RestoreTargetEntity>
                                    restoreTargetEntityList) {
                        if (restoreTargetEntityList == null) {
                            LogUtil.logError(TAG, "restoreTargetEntityList is null");
                            latch.countDown();
                            return;
                        }
                        final AtomicInteger restoreTargetCount =
                                new AtomicInteger(restoreTargetEntityList.size());
                        if (restoreTargetCount.get() == 0) {
                            latch.countDown();
                        }
                        for (RestoreTargetEntity restoreTargetEntity : restoreTargetEntityList) {
                            if (!restoreTargetEntity.isSensitiveDataEncrypted()) {
                                // Set version to
                                // BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION,
                                // default is the last version
                                // (BACKUP_DATA_CURRENT_VERSION)
                                restoreTargetEntity.setVersion(
                                        BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION);
                                RestoreTargetUtil.update(context, restoreTargetEntity,
                                        new DatabaseCompleteListener() {
                                            @Override
                                            public void onComplete() {
                                                LogUtil.logDebug(TAG,
                                                        "update restoreTarget "
                                                                + "data with AES encryption");
                                                if (restoreTargetCount.decrementAndGet() == 0) {
                                                    latch.countDown();
                                                }
                                            }

                                            @Override
                                            public void onError(Exception exception) {
                                                LogUtil.logError(TAG, "update error, "
                                                        + "e= " + exception);
                                                if (restoreTargetCount.decrementAndGet() == 0) {
                                                    latch.countDown();
                                                }
                                            }
                                        });
                            } else {
                                if (restoreTargetCount.decrementAndGet() == 0) {
                                    latch.countDown();
                                }
                            }
                        }
                    }
                });

                String googleAccountEmail = SkrSharedPrefs.getSocialKMBackupEmail(context);
                if (!TextUtils.isEmpty(googleAccountEmail)) {
                    SkrSharedPrefs.putSocialKMBackupEmail(context, googleAccountEmail);
                }

                String googleAccountUserName = SkrSharedPrefs.getSocialKMUserName(context);
                if (!TextUtils.isEmpty(googleAccountUserName)) {
                    SkrSharedPrefs.putSocialKMUserName(context, googleAccountUserName);
                }

                String currentRestoreEmail = SkrSharedPrefs.getCurrentRestoreEmail(context);
                if (!TextUtils.isEmpty(currentRestoreEmail)) {
                    SkrSharedPrefs.putCurrentRestoreEmail(context, currentRestoreEmail);
                }

                String restoreTrustNames = SkrSharedPrefs.getRestoreTrustNames(context);
                if (!TextUtils.isEmpty(restoreTrustNames)) {
                    SkrSharedPrefs.putRestoreTrustNames(context, restoreTrustNames);
                }

                try {
                    latch.await(TIMEOUT, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    LogUtil.logError(TAG, "InterruptedException, e=" + e);
                }

                databaseCompleteListener.onComplete();
            }
        });
    }

    // Backup data version 1 --> 2
    public static void updateLegacyBackupDataV1(
            final Context context, final DatabaseCompleteListener databaseCompleteListener) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(databaseCompleteListener, "databaseCompleteListener is null");

        DatabaseWorkManager.getInstance().enqueueWrite(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                // We need to ensure all things have been updated finish, thus lock
                // this work thread
                final CountDownLatch latch = new CountDownLatch(4);

                // Legacy Backup Data V1 List
                final Map<String, LegacyBackupDataV1> legacyV1Map =
                        Collections.synchronizedMap(new ArrayMap<String, LegacyBackupDataV1>());

                // Update ok and no-response backupTarget to bad, remove all pending
                // backupTarget if need
                BackupTargetUtil.getAll(context, new LoadListListener() {
                    @Override
                    public void onLoadFinished(
                            List<BackupSourceEntity> backupSourceEntityList,
                            List<BackupTargetEntity> backupTargetEntityList,
                            List<RestoreSourceEntity> restoreSourceEntityList,
                            List<RestoreTargetEntity> restoreTargetEntityList) {
                        if (backupTargetEntityList == null) {
                            LogUtil.logError(TAG, "backupTargetEntityList is null");
                            latch.countDown();
                            return;
                        }
                        final AtomicInteger backupTargetCount =
                                new AtomicInteger(backupTargetEntityList.size());
                        if (backupTargetCount.get() == 0) {
                            latch.countDown();
                        }
                        for (BackupTargetEntity backupTargetEntity : backupTargetEntityList) {
                            final String uuidHash = backupTargetEntity.getUUIDHash();
                            final String name = backupTargetEntity.getName();
                            final String publicKey = backupTargetEntity.getPublicKey();
                            if (backupTargetEntity.isLegacyDataUpdatedV1()) {
                                if (backupTargetCount.decrementAndGet() == 0) {
                                    latch.countDown();
                                }
                                continue;
                            }

                            // Should cope with Legacy Backup Data V1

                            // Remove unset seedIndex and transfer to Bad
                            if (!backupTargetEntity.isSeeded()) {
                                // Remove pending
                                BackupTargetUtil.remove(context, uuidHash,
                                        new DatabaseCompleteListener() {
                                            @Override
                                            public void onComplete() {
                                                LogUtil.logDebug(TAG, "Remove " + name
                                                        + "'s pending complete");
                                                if (backupTargetCount.decrementAndGet() == 0) {
                                                    latch.countDown();
                                                }
                                            }

                                            @Override
                                            public void onError(Exception e) {
                                                LogUtil.logError(TAG, "remove() failed, "
                                                        + "e=" + e);
                                                if (backupTargetCount.decrementAndGet() == 0) {
                                                    latch.countDown();
                                                }
                                            }
                                        });

                            } else {
                                // Transfer to Bad

                                // (auto backup after restore) "Bad"
                                // backupTarget may has only name, thus it
                                // has empty uuidHash
                                if (!TextUtils.isEmpty(uuidHash) && !TextUtils.isEmpty(publicKey)) {
                                    LegacyBackupDataV1 legacyBackupDataV1 =
                                            new LegacyBackupDataV1();
                                    legacyBackupDataV1.setUuidHash(uuidHash);
                                    legacyBackupDataV1.setPublicKey(publicKey);
                                    legacyV1Map.put(uuidHash, legacyBackupDataV1);
                                }

                                // BACKUP_TARGET_STATUS_BAD not need to
                                // update to bad again
                                if (!backupTargetEntity.compareStatus(BACKUP_TARGET_STATUS_BAD)) {
                                    // Set checksum to null, prevent belated
                                    // message (OK or Health Report)
                                    backupTargetEntity.setCheckSum(null);
                                    backupTargetEntity.updateLastCheckedTime();
                                    backupTargetEntity.updateStatusToBad();
                                }
                                // Set version to
                                // BACKUP_DATA_CLEAR_LEGACY_V1_VERSION,
                                // default is the last version
                                // (BACKUP_DATA_CURRENT_VERSION)
                                backupTargetEntity.setVersion(BACKUP_DATA_CLEAR_LEGACY_V1_VERSION);
                                BackupTargetUtil.update(context, backupTargetEntity,
                                        new DatabaseCompleteListener() {
                                            @Override
                                            public void onComplete() {
                                                LogUtil.logDebug(TAG, "Update " + name
                                                        + "'s backup to bad and version "
                                                        + BACKUP_DATA_CLEAR_LEGACY_V1_VERSION);
                                                if (backupTargetCount.decrementAndGet() == 0) {
                                                    latch.countDown();
                                                }
                                            }

                                            @Override
                                            public void onError(Exception exception) {
                                                LogUtil.logError(TAG, "update() failed, "
                                                        + "e=" + exception);
                                                if (backupTargetCount.decrementAndGet() == 0) {
                                                    latch.countDown();
                                                }
                                            }
                                        });
                            }
                        }
                    }
                });

                final AtomicBoolean shouldCheckDaily = new AtomicBoolean(false);

                // keep backupSource's original version, ask for help to Amy by
                // daily jobService
                BackupSourceUtil.getAll(context, new LoadListListener() {
                    @Override
                    public void onLoadFinished(
                            List<BackupSourceEntity> backupSourceEntityList,
                            List<BackupTargetEntity> backupTargetEntityList,
                            List<RestoreSourceEntity>
                                    restoreSourceEntityList,
                            List<RestoreTargetEntity>
                                    restoreTargetEntityList) {
                        if (backupSourceEntityList == null) {
                            LogUtil.logError(TAG, "backupSourceEntityList is null");
                            latch.countDown();
                            return;
                        }
                        final AtomicInteger backupSourceCount =
                                new AtomicInteger(backupSourceEntityList.size());
                        if (backupSourceCount.get() == 0) {
                            latch.countDown();
                        }
                        for (final BackupSourceEntity backupSourceEntity : backupSourceEntityList) {
                            if (!backupSourceEntity.isLegacyDataUpdatedV1()) {
                                // If any backupSource is Legacy Backup Data
                                // V1, check daily
                                shouldCheckDaily.set(true);
                            }

                            if (backupSourceCount.decrementAndGet() == 0) {
                                latch.countDown();
                            }
                        }
                    }
                });

                // restoreSource
                RestoreSourceUtil.getAll(context, new LoadListListener() {
                    @Override
                    public void onLoadFinished(
                            List<BackupSourceEntity> backupSourceEntityList,
                            List<BackupTargetEntity> backupTargetEntityList,
                            List<RestoreSourceEntity>
                                    restoreSourceEntityList,
                            List<RestoreTargetEntity>
                                    restoreTargetEntityList) {
                        if (restoreSourceEntityList == null) {
                            LogUtil.logError(TAG, "restoreSourceEntityList is null");
                            latch.countDown();
                            return;
                        }
                        final AtomicInteger restoreSourceCount =
                                new AtomicInteger(restoreSourceEntityList.size());
                        if (restoreSourceCount.get() == 0) {
                            latch.countDown();
                        }
                        for (final RestoreSourceEntity restoreSourceEntity :
                                restoreSourceEntityList) {
                            if (restoreSourceEntity.isLegacyDataUpdatedV1()) {
                                if (restoreSourceCount.decrementAndGet() == 0) {
                                    latch.countDown();
                                }
                                continue;
                            }
                            // Just update backup data version
                            restoreSourceEntity.setVersion(
                                    BACKUP_DATA_CLEAR_LEGACY_V1_VERSION);
                            RestoreSourceUtil.update(context, restoreSourceEntity,
                                    new DatabaseCompleteListener() {
                                        @Override
                                        public void onComplete() {
                                            LogUtil.logDebug(TAG, "Update "
                                                    + restoreSourceEntity.getName()
                                                    + "'s version (restoreSource)");
                                            if (restoreSourceCount.decrementAndGet() == 0) {
                                                latch.countDown();
                                            }
                                        }

                                        @Override
                                        public void onError(Exception exception) {
                                            LogUtil.logError(TAG, "update() failed, "
                                                    + "e=" + exception);
                                            if (restoreSourceCount.decrementAndGet() == 0) {
                                                latch.countDown();
                                            }
                                        }
                                    });
                        }
                    }
                });

                // restoreTarget
                RestoreTargetUtil.getAll(context, new LoadListListener() {
                    @Override
                    public void onLoadFinished(
                            List<BackupSourceEntity> backupSourceEntityList,
                            List<BackupTargetEntity> backupTargetEntityList,
                            List<RestoreSourceEntity>
                                    restoreSourceEntityList,
                            List<RestoreTargetEntity>
                                    restoreTargetEntityList) {
                        if (restoreTargetEntityList == null) {
                            LogUtil.logError(TAG, "restoreTargetEntityList is null");
                            latch.countDown();
                            return;
                        }
                        final AtomicInteger restoreTargetCount =
                                new AtomicInteger(restoreTargetEntityList.size());
                        if (restoreTargetCount.get() == 0) {
                            latch.countDown();
                        }
                        for (final RestoreTargetEntity restoreTargetEntity :
                                restoreTargetEntityList) {
                            if (restoreTargetEntity
                                    .isLegacyDataUpdatedV1()) {
                                if (restoreTargetCount.decrementAndGet() == 0) {
                                    latch.countDown();
                                }
                                continue;
                            }
                            // Just update backup data version
                            restoreTargetEntity.setVersion(
                                    BACKUP_DATA_CLEAR_LEGACY_V1_VERSION);
                            RestoreTargetUtil.update(context, restoreTargetEntity,
                                    new DatabaseCompleteListener() {
                                        @Override
                                        public void onComplete() {
                                            LogUtil.logDebug(TAG, "Update "
                                                    + restoreTargetEntity.getName()
                                                    + "'s version (restoreTarget)");
                                            if (restoreTargetCount.decrementAndGet() == 0) {
                                                latch.countDown();
                                            }
                                        }

                                        @Override
                                        public void onError(Exception exception) {
                                            LogUtil.logError(TAG, "update() failed, "
                                                    + "e=" + exception);
                                            if (restoreTargetCount.decrementAndGet() == 0) {
                                                latch.countDown();
                                            }
                                        }
                                    });
                        }
                    }
                });

                try {
                    latch.await(TIMEOUT, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    LogUtil.logError(TAG, "InterruptedException, e=" + e);
                }

                databaseCompleteListener.onComplete();

                // Should notify Amy
                if (legacyV1Map.size() > 0) {
                    SkrSharedPrefs.putShouldShowSkrSecurityUpdateDialogV1(
                            context, true);
                    SkrSharedPrefs.putShouldShowSkrSecurityUpdateHeaderV1(
                            context, true);
                    LegacyV1Util.putLegacyV1Map(context, legacyV1Map);

                    // Get EntryActivity intent
                    Intent intent = IntentUtil.generateEntryActivityIntent(context);

                    // Show notification
                    NotificationUtil.showUpdateLegacyBackupDataV1Notification(
                            context, intent);
                }

                // Should check daily (Bob side)
                if (shouldCheckDaily.get()) {
                    // Schedule jobService
                    CheckBackupVersionJobService.schedule(context);
                }
            }
        });
    }
}
