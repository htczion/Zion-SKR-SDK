package com.htc.wallet.skrsdk.sqlite.util;

import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_DONE;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_OK;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_REQUEST_AUTO_BACKUP;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupSource;
import com.htc.wallet.skrsdk.error.DatabaseSaveException;
import com.htc.wallet.skrsdk.sqlite.SocialKmDatabase;
import com.htc.wallet.skrsdk.sqlite.dao.BackupSourceDao;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.List;
import java.util.Objects;

public class BackupSourceUtil {
    private static final String TAG = "BackupSourceUtil";

    private BackupSourceUtil() {
        throw new AssertionError();
    }

    // TODO: change to package-private
    public static void getAll(
            @NonNull final Context context, @NonNull final LoadListListener loadListListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(loadListListener);

        DatabaseWorkManager.getInstance().enqueueRead(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                List<BackupSourceEntity> backupSourceList;
                backupSourceList = SocialKmDatabase.getInstance(context).backupSourceDao().getAll();
                if (backupSourceList != null) {
                    for (BackupSourceEntity source : backupSourceList) {
                        source.decrypt();
                    }
                } else {
                    LogUtil.logDebug(TAG, "Failed to getAll from backupSourceDao");
                }
                loadListListener.onLoadFinished(backupSourceList, null, null, null);
            }
        });
    }

    public static void getAllOK(
            @NonNull final Context context, @NonNull final LoadListListener loadListListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(loadListListener);

        DatabaseWorkManager.getInstance().enqueueRead(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                List<BackupSourceEntity> backupSourceList;
                backupSourceList = SocialKmDatabase.getInstance(context)
                        .backupSourceDao().getAllOK();
                if (backupSourceList != null) {
                    for (BackupSourceEntity source : backupSourceList) {
                        source.decrypt();
                    }
                } else {
                    LogUtil.logDebug(TAG, "Failed to getAllOK from backupSourceDao");
                }
                loadListListener.onLoadFinished(backupSourceList, null, null, null);
            }
        });
    }

    public static void getAllRequestAutoBackup(
            @NonNull final Context context, @NonNull final LoadListListener loadListListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(loadListListener);

        DatabaseWorkManager.getInstance().enqueueRead(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                List<BackupSourceEntity> backupSourceList;
                backupSourceList = SocialKmDatabase.getInstance(context)
                        .backupSourceDao().getRequestAutoBackup();
                if (backupSourceList != null) {
                    for (BackupSourceEntity source : backupSourceList) {
                        source.decrypt();
                    }
                } else {
                    LogUtil.logDebug(TAG, "Failed to getAllRequestAutoBackup from "
                            + "backupSourceDao");
                }
                loadListListener.onLoadFinished(backupSourceList, null, null, null);
            }
        });
    }

    // Only recover flow
    public static void getOK(
            @NonNull final Context context,
            @NonNull final String emailHash,
            @NonNull final String uuidHash,
            @NonNull final LoadDataListener loadDataListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(loadDataListener);
        if (TextUtils.isEmpty(emailHash)) {
            LogUtil.logError(TAG, "emailHash is null or empty");
        }
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(TAG, "uuidHash is null or empty");
        }

        DatabaseWorkManager.getInstance().enqueueRead(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                BackupSourceEntity backupSource = null;
                if (!TextUtils.isEmpty(emailHash) && !TextUtils.isEmpty(uuidHash)) {
                    backupSource = SocialKmDatabase.getInstance(context)
                            .backupSourceDao().getOK(emailHash, uuidHash);
                }

                if (backupSource != null) {
                    backupSource.decrypt();
                } else {
                    LogUtil.logDebug(TAG, "Failed to getOK from backupSourceDao");
                }
                loadDataListener.onLoadFinished(backupSource, null, null, null);
            }
        });
    }

    public static void getWithUUIDHash(
            @NonNull final Context context,
            @NonNull final String emailHash,
            @NonNull final String uuidHash,
            @NonNull final LoadDataListener loadDataListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(loadDataListener);
        if (TextUtils.isEmpty(emailHash)) {
            LogUtil.logError(TAG, "emailHash is null or empty");
        }
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(TAG, "uuidHash is null or empty");
        }

        DatabaseWorkManager.getInstance().enqueueRead(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                BackupSourceEntity backupSource;
                backupSource = SocialKmDatabase.getInstance(context)
                        .backupSourceDao().getWithUUIDHash(emailHash, uuidHash);

                if (backupSource != null) {
                    backupSource.decrypt();
                } else {
                    LogUtil.logDebug(TAG, "Failed to getWithUUIDHash from backupSourceDao");
                }
                loadDataListener.onLoadFinished(backupSource, null, null, null);
            }
        });
    }

    public static void remove(
            @NonNull final Context context,
            @NonNull final String emailHash,
            @NonNull final String uuidHash) {
        Objects.requireNonNull(context);
        if (TextUtils.isEmpty(emailHash)) {
            LogUtil.logError(TAG, "email hash is null or empty");
        }
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(TAG, "uuidHash is null or empty");
        }

        DatabaseWorkManager.getInstance().enqueueWrite(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                BackupSourceDao backupSourceDao =
                        SocialKmDatabase.getInstance(context).backupSourceDao();
                BackupSourceEntity backupSourceEntity =
                        backupSourceDao.getWithUUIDHash(emailHash, uuidHash);
                if (backupSourceEntity != null) {
                    LogUtil.logDebug(TAG, "Remove " + backupSourceEntity.getName()
                            + "'s backupSource");
                    int removeLine = backupSourceDao.remove(backupSourceEntity);
                    if (removeLine <= 0) {
                        LogUtil.logWarning(TAG, "No data removed");
                    }
                } else {
                    LogUtil.logDebug(TAG, "backupSourceEntity is null, nothing to be removed");
                }
            }
        });
    }

    // TODO: Merge similar function: removeAllByEmailHash, removeAllByEmailHashWithVersion and
    // removeAllByEmailHashExceptUUIDHash

    public static void removeAllByEmailHash(
            @NonNull final Context context,
            @NonNull final String emailHash) {
        Objects.requireNonNull(context);
        if (TextUtils.isEmpty(emailHash)) {
            LogUtil.logError(TAG, "emailHash is null or empty");
            return;
        }
        LogUtil.logDebug(TAG, "Remove all by emailHash=" + emailHash);

        getAll(context, new LoadListListener() {
            @Override
            public void onLoadFinished(
                    List<BackupSourceEntity> backupSourceEntityList,
                    List<BackupTargetEntity> backupTargetEntityList,
                    List<RestoreSourceEntity> restoreSourceEntityList,
                    List<RestoreTargetEntity> restoreTargetEntityList) {
                if (backupSourceEntityList == null) {
                    LogUtil.logError(TAG, "backupSourceEntityList is null");
                    return;
                }

                for (BackupSourceEntity backupSourceEntity : backupSourceEntityList) {
                    if (emailHash.equals(backupSourceEntity.getEmailHash())) {
                        final String uuidHash = backupSourceEntity.getUUIDHash();
                        LogUtil.logDebug(TAG, "Remove all by emailHash, uuidHash=" + uuidHash);
                        if (TextUtils.isEmpty(uuidHash)) {
                            LogUtil.logError(TAG, "uuidHash is null or empty");
                        } else {
                            remove(context, emailHash, uuidHash);
                        }
                    }
                }
            }
        });
    }

    // This method is only use for delete legacy V1 backup data automatically while user agreed
    // (with version)
    // If value of version is 1, only version 0 and 1 will deleted (Less than or equal to version)
    public static void removeAllByEmailHashWithVersion(
            @NonNull final Context context, @NonNull final String emailHash, final int version) {
        Objects.requireNonNull(context);
        if (TextUtils.isEmpty(emailHash)) {
            LogUtil.logError(TAG, "emailHash is null or empty");
            return;
        }
        LogUtil.logDebug(TAG, "remove all by emailHash " + emailHash + ", with version " + version);

        getAll(context, new LoadListListener() {
            @Override
            public void onLoadFinished(
                    List<BackupSourceEntity> backupSourceEntityList,
                    List<BackupTargetEntity> backupTargetEntityList,
                    List<RestoreSourceEntity> restoreSourceEntityList,
                    List<RestoreTargetEntity> restoreTargetEntityList) {
                if (backupSourceEntityList == null) {
                    LogUtil.logError(TAG, "backupSourceEntityList is null");
                    return;
                }
                for (BackupSourceEntity backupSourceEntity : backupSourceEntityList) {
                    if (emailHash.equals(backupSourceEntity.getEmailHash())
                            && backupSourceEntity.getVersion() <= version) {
                        final String uuidHash = backupSourceEntity.getUUIDHash();
                        LogUtil.logDebug(TAG, "Remove all by emailHash"
                                + ", uuidHash=" + uuidHash
                                + ", version=" + backupSourceEntity.getVersion());
                        if (TextUtils.isEmpty(uuidHash)) {
                            LogUtil.logError(TAG, "uuidHash is null or empty");
                        } else {
                            remove(context, emailHash, uuidHash);
                        }
                    }
                }
            }
        });
    }

    public static void removeAllByEmailHashExceptUUIDHash(
            @NonNull final Context context,
            @NonNull final String emailHash,
            @NonNull final String uuidHash) {
        Objects.requireNonNull(context);
        if (TextUtils.isEmpty(emailHash)) {
            LogUtil.logError(TAG, "emailHash is null or empty");
            return;
        }
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(TAG, "uuidHash is null or empty");
            return;
        }
        LogUtil.logDebug(TAG,
                "Remove all by emailHash=" + emailHash + ", except uuidHash=" + uuidHash);

        getAll(context, new LoadListListener() {
            @Override
            public void onLoadFinished(
                    List<BackupSourceEntity> backupSourceEntityList,
                    List<BackupTargetEntity> backupTargetEntityList,
                    List<RestoreSourceEntity> restoreSourceEntityList,
                    List<RestoreTargetEntity> restoreTargetEntityList) {
                if (backupSourceEntityList == null) {
                    LogUtil.logError(TAG, "backupSourceEntityList is null");
                    return;
                }

                for (BackupSourceEntity backupSourceEntity : backupSourceEntityList) {
                    final String deletedUuidHash = backupSourceEntity.getUUIDHash();
                    // Remove all by emailHash, except this uuidHash
                    if (emailHash.equals(backupSourceEntity.getEmailHash())
                            && !uuidHash.equals(deletedUuidHash)) {
                        if (TextUtils.isEmpty(deletedUuidHash)) {
                            LogUtil.logError(TAG, "deleted uuidHash is null or empty");
                        } else {
                            LogUtil.logDebug(TAG, "deleted uuidHash is " + deletedUuidHash);
                            remove(context, emailHash, deletedUuidHash);
                        }
                    }
                }
            }
        });
    }

    public static void put(
            @NonNull final Context context,
            @NonNull final BackupSourceEntity backupSourceEntity,
            @NonNull final DatabaseCompleteListener databaseCompleteListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(backupSourceEntity);
        Objects.requireNonNull(databaseCompleteListener);

        final BackupSourceEntity cloneBackupSourceEntity =
                new BackupSourceEntity(backupSourceEntity);
        final String emailHash = cloneBackupSourceEntity.getEmailHash();
        final String uuidHash = cloneBackupSourceEntity.getUUIDHash();

        DatabaseWorkManager.getInstance().enqueueWrite(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                BackupSourceDao backupSourceDao =
                        SocialKmDatabase.getInstance(context).backupSourceDao();
                // Same emailHash and uuidHash, previous backupSource
                BackupSourceEntity previousSource =
                        backupSourceDao.getWithUUIDHash(emailHash, uuidHash);
                LogUtil.logDebug(TAG, "Put new backupSource with emailHash=" + emailHash
                        + ", uuidHash=" + uuidHash);

                final int status = cloneBackupSourceEntity.getStatus();
                switch (status) {
                    case BACKUP_SOURCE_STATUS_REQUEST:
                    case BACKUP_SOURCE_STATUS_REQUEST_AUTO_BACKUP:
                        // Remove previous backupSource
                        if (previousSource != null) {
                            backupSourceDao.remove(previousSource);
                            LogUtil.logDebug(TAG, "Remove previous backupSource "
                                    + "with new status " + status);
                        }
                        break;
                    case BACKUP_SOURCE_STATUS_OK:
                    case BACKUP_SOURCE_STATUS_DONE:
                        // Remove previous backupSource
                        if (previousSource != null) {
                            backupSourceDao.remove(previousSource);
                            LogUtil.logDebug(TAG, "Remove previous backupSource"
                                    + " with new status " + status);
                        }
                        // Remove all has same emailHash backupSource with request
                        // state
                        List<BackupSourceEntity> chooses =
                                backupSourceDao.getAllByEmailHash(emailHash);
                        for (BackupSourceEntity choose : chooses) {
                            if (choose.compareStatus(
                                    BACKUP_SOURCE_STATUS_REQUEST)) {
                                backupSourceDao.remove(choose);
                                LogUtil.logDebug(TAG, "Remove backupSource"
                                        + ", emailHash=" + emailHash
                                        + ", status=" + status);
                            }
                        }
                        break;
                    default:
                        LogUtil.logError(TAG, "Incorrect status " + status);
                        break;
                }

                cloneBackupSourceEntity.encrypt();
                long result = backupSourceDao.insert(cloneBackupSourceEntity);
                if (result > 0) {
                    databaseCompleteListener.onComplete();
                } else {
                    databaseCompleteListener.onError(new DatabaseSaveException("Insert failed"));
                }
            }
        });
    }

    // TODO: change to package-private
    public static BackupSourceEntity backupSourceToBackupSourceEntity(
            @NonNull final BackupSource backupSource) {
        BackupSourceEntity backupSourceEntity = new BackupSourceEntity();
        backupSourceEntity.setStatus(backupSource.getStatus());
        backupSourceEntity.setEmailHash(backupSource.getEmailHash());
        backupSourceEntity.setFcmToken(backupSource.getFcmToken());
        backupSourceEntity.setPublicKey(backupSource.getPublicKey());
        backupSourceEntity.setUUIDHash(backupSource.getUUIDHash());
        backupSourceEntity.setTimeStamp(backupSource.getTimeStamp());
        backupSourceEntity.setName(backupSource.getName());
        backupSourceEntity.setMyName(backupSource.getMyName());
        backupSourceEntity.setSeed(backupSource.getSeed());
        backupSourceEntity.setCheckSum(backupSource.getCheckSum());
        return backupSourceEntity;
    }

    // TODO: change to package-private
    public static boolean checkIfSaveCorrect(@NonNull Context context) {
        List<BackupSource> backupSources =
                com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupSourceUtil.getAllOK(context);
        List<BackupSourceEntity> backupSourceEntities =
                SocialKmDatabase.getInstance(context).backupSourceDao().getAll();
        if (backupSourceEntities == null || backupSources == null) {
            LogUtil.logError(TAG, "backupSourceEntities or backupSources is null or empty");
            return false;
        }

        for (BackupSourceEntity source : backupSourceEntities) {
            source.decrypt();
        }

        for (int i = 0; i < backupSourceEntities.size(); i++) {
            if (backupSourceEntities.get(i).getStatus() != backupSources.get(i).getStatus()) {
                LogUtil.logError(
                        TAG, "backupSourceEntities getStatus is not equal to backupSources");
                return false;
            }
            if (!backupSourceEntities.get(i).getEmailHash()
                    .equals(backupSources.get(i).getEmailHash())) {
                LogUtil.logError(
                        TAG, "backupSourceEntities getEmailHash is not equal to backupSources");
                return false;
            }
            if (!backupSourceEntities.get(i).getFcmToken()
                    .equals(backupSources.get(i).getFcmToken())) {
                LogUtil.logError(
                        TAG, "backupSourceEntities getFcmToken is not equal to backupSources");
                return false;
            }
            if (!backupSourceEntities.get(i).getPublicKey()
                    .equals(backupSources.get(i).getPublicKey())) {
                LogUtil.logError(
                        TAG, "backupSourceEntities getPublicKey is not equal to backupSources");
                return false;
            }
            if (!backupSourceEntities.get(i).getUUIDHash()
                    .equals(backupSources.get(i).getUUIDHash())) {
                LogUtil.logError(
                        TAG, "backupSourceEntities getUUIDHash is not equal to backupSources");
                return false;
            }
            if (backupSourceEntities.get(i).getTimeStamp() != backupSources.get(i).getTimeStamp()) {
                LogUtil.logError(
                        TAG, "backupSourceEntities getTimeStamp is not equal to backupSources");
                return false;
            }
            if (!backupSourceEntities.get(i).getName().equals(backupSources.get(i).getName())) {
                LogUtil.logError(TAG, "backupSourceEntities getName is not equal to backupSources");
                return false;
            }
            if (!backupSourceEntities.get(i).getSeed().equals(backupSources.get(i).getSeed())) {
                LogUtil.logError(TAG, "backupSourceEntities getSeed is not equal to backupSources");
                return false;
            }
            if (!backupSourceEntities.get(i).getCheckSum()
                    .equals(backupSources.get(i).getCheckSum())) {
                LogUtil.logError(
                        TAG, "backupSourceEntities getCheckSum is not equal to backupSources");
                return false;
            }
        }
        return true;
    }

    public static void update(
            @NonNull final Context context,
            @NonNull final BackupSourceEntity backupSourceEntity,
            @NonNull final DatabaseCompleteListener databaseCompleteListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(backupSourceEntity);
        Objects.requireNonNull(databaseCompleteListener);

        final BackupSourceEntity cloneBackupSourceEntity =
                new BackupSourceEntity(backupSourceEntity);
        DatabaseWorkManager.getInstance().enqueueWrite(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                BackupSourceDao backupSourceDao =
                        SocialKmDatabase.getInstance(context).backupSourceDao();
                cloneBackupSourceEntity.encrypt();
                int updateRowId = backupSourceDao.update(cloneBackupSourceEntity);
                if (updateRowId <= 0) {
                    LogUtil.logWarning(TAG, "No data updated");
                }
                databaseCompleteListener.onComplete();
            }
        });
    }
}
