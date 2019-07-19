package com.htc.wallet.skrsdk.sqlite.util;

import static com.htc.wallet.skrsdk.secretsharing.SeedUtil.INDEX_MAX;
import static com.htc.wallet.skrsdk.secretsharing.SeedUtil.INDEX_MIN;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_BAD;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.UNDEFINED_SEED_INDEX;
import static com.htc.wallet.skrsdk.util.NotificationUtil.NOTIFICATION_ID_VERIFICATION_SHARING;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupTarget;
import com.htc.wallet.skrsdk.drives.DriveJobIntentService;
import com.htc.wallet.skrsdk.error.DatabaseSaveException;
import com.htc.wallet.skrsdk.jobs.JobIdManager;
import com.htc.wallet.skrsdk.sqlite.SocialKmDatabase;
import com.htc.wallet.skrsdk.sqlite.dao.BackupTargetDao;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.NotificationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BackupTargetUtil {
    private static final String TAG = "BackupTargetUtil";

    private BackupTargetUtil() {
        throw new AssertionError();
    }

    public static void getAll(
            @NonNull final Context context,
            @NonNull final LoadListListener loadListListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(loadListListener);

        DatabaseWorkManager.getInstance().enqueueRead(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                List<BackupTargetEntity> backupTargets;
                backupTargets = SocialKmDatabase.getInstance(context).backupTargetDao().getAll();
                if (backupTargets != null) {
                    for (BackupTargetEntity backupTarget : backupTargets) {
                        backupTarget.decrypt();
                    }
                } else {
                    LogUtil.logDebug(TAG, "Failed to getAll from backupTargetDao");
                }
                loadListListener.onLoadFinished(null, backupTargets, null, null);
            }
        });
    }

    public static void getAllOK(
            @NonNull final Context context,
            @NonNull final LoadListListener loadListListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(loadListListener);

        DatabaseWorkManager.getInstance().enqueueRead(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                List<BackupTargetEntity> backupTargets;
                backupTargets = SocialKmDatabase.getInstance(context).backupTargetDao().getAllOK();
                if (backupTargets != null) {
                    for (BackupTargetEntity backupTarget : backupTargets) {
                        backupTarget.decrypt();
                    }
                } else {
                    LogUtil.logDebug(TAG, "Failed to getAllOK from backupTargetDao");
                }
                loadListListener.onLoadFinished(null, backupTargets, null, null);
            }
        });
    }

    public static void getAllOKAndNoResponse(
            @NonNull final Context context,
            @NonNull final LoadListListener loadListListener) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(loadListListener);

        DatabaseWorkManager.getInstance().enqueueRead(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                List<BackupTargetEntity> backupTargets;
                backupTargets = SocialKmDatabase.getInstance(context)
                        .backupTargetDao().getAllOKAndNoResponse();
                if (backupTargets != null) {
                    for (BackupTargetEntity backupTarget : backupTargets) {
                        backupTarget.decrypt();
                    }
                } else {
                    LogUtil.logDebug(TAG, "Failed to getAllOKAndNoResponse from backupTargetDao");
                }
                loadListListener.onLoadFinished(null, backupTargets, null, null);
            }
        });
    }

    // TODO: Refactor to use DatabaseWorkManager!
    @WorkerThread
    public static int getBadCount(@NonNull final Context context) {
        Objects.requireNonNull(context);

        List<BackupTargetEntity> backupTargets;
        backupTargets = SocialKmDatabase.getInstance(context).backupTargetDao().getAllBad();
        return backupTargets == null ? 0 : backupTargets.size();
    }

    public static void get(
            @NonNull final Context context,
            @NonNull final String uuidHash,
            @NonNull final LoadDataListener loadDataListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(loadDataListener);
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(TAG, "uuidHash is null or empty");
        }

        DatabaseWorkManager.getInstance().enqueueRead(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                BackupTargetEntity backupTargetEntity;
                backupTargetEntity = SocialKmDatabase.getInstance(context)
                        .backupTargetDao().get(uuidHash);
                if (backupTargetEntity != null) {
                    backupTargetEntity.decrypt();
                } else {
                    LogUtil.logDebug(TAG, "Failed to get from backupTargetDao");
                }
                loadDataListener.onLoadFinished(null, backupTargetEntity, null, null);
            }
        });
    }

    // TODO: Refactor to use DatabaseWorkManager!
    @WorkerThread
    private static BackupTargetEntity getWithOnlyName(
            @NonNull final Context context,
            @NonNull final String name) {
        Objects.requireNonNull(context);
        if (TextUtils.isEmpty(name)) {
            LogUtil.logError(TAG, "name is null or empty");
            return null;
        }

        List<BackupTargetEntity> backupTargets;
        backupTargets = SocialKmDatabase.getInstance(context).backupTargetDao().getOnlyNameList();
        if (backupTargets != null) {
            for (BackupTargetEntity backupTarget : backupTargets) {
                backupTarget.decrypt();
                if (name.equals(backupTarget.getName())) {
                    return backupTarget;
                }
            }
        } else {
            LogUtil.logDebug(TAG, "backupTargets is null, nothing to be get with only name");
        }

        return null;
    }

    public static void put(
            @NonNull final Context context,
            @NonNull final BackupTargetEntity backupTarget,
            @NonNull final DatabaseCompleteListener databaseCompleteListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(backupTarget, "backupTarget is null");
        Objects.requireNonNull(databaseCompleteListener);

        final BackupTargetEntity cloneBackupTargetEntity = new BackupTargetEntity(backupTarget);
        final String uuidHash = cloneBackupTargetEntity.getUUIDHash();

        DatabaseWorkManager.getInstance().enqueueWrite(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                long result;
                BackupTargetDao backupTargetDao = SocialKmDatabase.getInstance(context)
                        .backupTargetDao();
                // Remove origin if existed
                if (!uuidHash.equals("")) {
                    BackupTargetEntity removing = backupTargetDao.get(uuidHash);
                    if (removing != null) {
                        backupTargetDao.remove(removing);
                        LogUtil.logDebug(TAG, "Remove existing BackupTargetEntity, uuidHash="
                                + removing.getUUIDHash());
                    }
                }
                cloneBackupTargetEntity.encrypt();
                result = backupTargetDao.insert(cloneBackupTargetEntity);

                if (result > 0) {
                    databaseCompleteListener.onComplete();

                    // enqueue upload trust contacts
                    DriveJobIntentService.enqueueUpload(context,
                            JobIdManager.JOB_ID_SKR_UPLOAD_TRUST_CONTACTS);

                } else {
                    databaseCompleteListener.onError(new DatabaseSaveException("Insert failed"));
                }
            }
        });
    }

    public static void putList(
            @NonNull final Context context,
            @NonNull final List<BackupTargetEntity> backupTargetEntityList,
            @NonNull final DatabaseCompleteListener databaseCompleteListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(databaseCompleteListener);

        DatabaseWorkManager.getInstance().enqueueWrite(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                boolean isSuccess = false;
                BackupTargetDao backupTargetDao = SocialKmDatabase.getInstance(context)
                        .backupTargetDao();
                List<BackupTargetEntity> cloneBackupTargetList = new ArrayList<>();
                for (BackupTargetEntity backupTargetEntity :
                        backupTargetEntityList) {
                    BackupTargetEntity cloneBackupTargetEntity =
                            new BackupTargetEntity(backupTargetEntity);
                    String uuidHash = cloneBackupTargetEntity.getUUIDHash();
                    // Remove origin if existed
                    if (!uuidHash.equals("")) {
                        BackupTargetEntity removing = backupTargetDao.get(uuidHash);
                        if (removing != null) {
                            backupTargetDao.remove(removing);
                            LogUtil.logDebug(TAG, "Remove existing BackupTargetEntity, uuidHash="
                                    + removing.getUUIDHash());
                        }
                    } else {
                        BackupTargetEntity removing =
                                getWithOnlyName(context, cloneBackupTargetEntity.getName());
                        if (removing != null) {
                            backupTargetDao.remove(removing);
                            LogUtil.logDebug(TAG, "Remove existing BackupTargetEntity, uuidHash="
                                    + removing.getUUIDHash());
                        }
                    }
                    cloneBackupTargetEntity.encrypt();
                    cloneBackupTargetList.add(cloneBackupTargetEntity);
                }

                long[] result = backupTargetDao.insertAll(cloneBackupTargetList);
                if (result.length == cloneBackupTargetList.size()) {
                    isSuccess = true;
                } else {
                    LogUtil.logError(TAG, "insertAll failed");
                }

                if (isSuccess) {
                    databaseCompleteListener.onComplete();

                    // enqueue upload trust contacts
                    DriveJobIntentService.enqueueUpload(context,
                            JobIdManager.JOB_ID_SKR_UPLOAD_TRUST_CONTACTS);

                } else {
                    databaseCompleteListener.onError(new DatabaseSaveException("InsertAll failed"));
                }
            }
        });
    }

    public static void remove(
            @NonNull final Context context,
            @NonNull final String uuidHash,
            @NonNull final DatabaseCompleteListener databaseCompleteListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(databaseCompleteListener);
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(TAG, "uuidHash is null or empty");
            return;
        }

        DatabaseWorkManager.getInstance().enqueueWrite(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {

                // Clear removed notification
                NotificationUtil.cancelNotification(context, uuidHash,
                        NOTIFICATION_ID_VERIFICATION_SHARING);
                LogUtil.logDebug(TAG, "Remove " + uuidHash + "'s notification");

                BackupTargetDao backupTargetDao =
                        SocialKmDatabase.getInstance(context).backupTargetDao();
                BackupTargetEntity removeEntity = backupTargetDao.get(uuidHash);
                if (removeEntity != null) {
                    LogUtil.logDebug(TAG, "Remove " + removeEntity.getName() + "'s backupTarget");
                    int removeLine = backupTargetDao.remove(removeEntity);
                    if (removeLine <= 0) {
                        LogUtil.logWarning(TAG, "No data removed");
                    }
                } else {
                    LogUtil.logDebug(TAG, "removeEntity is null, nothing to be removed");
                }
                databaseCompleteListener.onComplete();
            }
        });
    }

    public static void removeWithOnlyName(
            @NonNull final Context context,
            @NonNull final String name,
            @NonNull final DatabaseCompleteListener databaseCompleteListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(databaseCompleteListener);
        if (TextUtils.isEmpty(name)) {
            LogUtil.logError(TAG, "name is null or empty");
            return;
        }

        DatabaseWorkManager.getInstance().enqueueWrite(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {

                // Not need to clear notification here
                // (only name) BackupTarget without notification

                BackupTargetDao backupTargetDao =
                        SocialKmDatabase.getInstance(context).backupTargetDao();
                BackupTargetEntity backupTargetEntity = getWithOnlyName(context, name);
                if (backupTargetEntity != null) {
                    LogUtil.logDebug(TAG, "Remove " + backupTargetEntity.getName()
                            + "'s backupTarget (with only name)");
                    int removeLine = backupTargetDao.remove(backupTargetEntity);
                    if (removeLine <= 0) {
                        LogUtil.logWarning(TAG, "No data removed");
                    }
                } else {
                    LogUtil.logDebug(TAG,
                            "removeEntity is null, nothing to be removed (with only name)");
                }

                databaseCompleteListener.onComplete();
            }
        });
    }

    @WorkerThread
    @NonNull
    public static List<Integer> getFreeSeedIndex(@NonNull final Context context) {
        Objects.requireNonNull(context);

        final List<Integer> freeSeedIndexes = new ArrayList<>();
        for (int i = INDEX_MIN; i <= INDEX_MAX; i++) {
            freeSeedIndexes.add(i);
        }
        List<Integer> seedIndexList =
                SocialKmDatabase.getInstance(context).backupTargetDao().getSeedIndex();
        for (Integer seedIndex : seedIndexList) {
            if (seedIndex != UNDEFINED_SEED_INDEX) {
                freeSeedIndexes.remove(seedIndex);
            }
        }
        return freeSeedIndexes;
    }

    public static void clearData(@NonNull final Context context) {

        getAll(context, new LoadListListener() {
            @Override
            public void onLoadFinished(
                    List<BackupSourceEntity> backupSourceEntityList,
                    List<BackupTargetEntity> backupTargetEntityList,
                    List<RestoreSourceEntity> restoreSourceEntityList,
                    List<RestoreTargetEntity> restoreTargetEntityList) {
                if (backupTargetEntityList == null) {
                    LogUtil.logError(TAG, "backupTargetEntityList is null");
                } else {
                    // Remove all verification code notification
                    LogUtil.logDebug(TAG, "Remove all verification code notification");
                    for (BackupTargetEntity backupTargetEntity : backupTargetEntityList) {
                        String uuidHash = backupTargetEntity.getUUIDHash();
                        NotificationUtil.cancelNotification(context, uuidHash,
                                NOTIFICATION_ID_VERIFICATION_SHARING);
                        LogUtil.logDebug(TAG, "Remove " + uuidHash + "'s notification");
                    }
                }

                DatabaseWorkManager.getInstance().enqueueWrite(
                        new DatabaseWorkManager.DatabaseWorkItem() {
                            @Override
                            public void run() {
                                LogUtil.logInfo(TAG, "Start to clear backupTarget data");
                                SocialKmDatabase.getInstance(context).backupTargetDao().clearData();
                                LogUtil.logInfo(TAG, "End of clear backupTarget data");

                                // this function only for logout
                                // no need to upload trust contacts
                            }
                        });
            }
        });
    }

    public static void update(
            @NonNull final Context context,
            @NonNull final BackupTargetEntity backupTargetEntity,
            @NonNull final DatabaseCompleteListener databaseCompleteListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(backupTargetEntity);
        Objects.requireNonNull(databaseCompleteListener);

        final BackupTargetEntity cloneBackupTargetEntity =
                new BackupTargetEntity(backupTargetEntity);
        DatabaseWorkManager.getInstance().enqueueWrite(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                BackupTargetDao backupTargetDao =
                        SocialKmDatabase.getInstance(context).backupTargetDao();
                if (TextUtils.isEmpty(cloneBackupTargetEntity.getName())) {
                    LogUtil.logError(TAG, "name is null or empty");
                    return;
                }
                cloneBackupTargetEntity.encrypt();
                int updateRowId = backupTargetDao.update(cloneBackupTargetEntity);
                if (updateRowId <= 0) {
                    LogUtil.logWarning(TAG, "No data updated");
                }

                databaseCompleteListener.onComplete();

                // enqueue upload trust contacts
                DriveJobIntentService.enqueueUpload(context,
                        JobIdManager.JOB_ID_SKR_UPLOAD_TRUST_CONTACTS);
            }
        });
    }

    public static BackupTargetEntity backupTargetToBackupTargetEntity(
            @NonNull BackupTarget backupTarget) {
        BackupTargetEntity backupTargetEntity;
        backupTargetEntity = new BackupTargetEntity();

        if (backupTarget.getStatus() == BACKUP_TARGET_STATUS_REQUEST) {
            backupTargetEntity.setStatus(BACKUP_TARGET_STATUS_BAD);
            backupTargetEntity.setFcmToken("");
            backupTargetEntity.setPublicKey("");
            backupTargetEntity.setUUIDHash("");
            backupTargetEntity.setName(backupTarget.getName());
            backupTargetEntity.setLastCheckedTime(backupTarget.getLastCheckedTime());
            backupTargetEntity.setSeedIndex(backupTarget.getSeedIndex());
            backupTargetEntity.setCheckSum("");
            backupTargetEntity.setPinCode("");
            backupTargetEntity.setSeed("");
        } else {
            backupTargetEntity.setStatus(backupTarget.getStatus());
            backupTargetEntity.setFcmToken(backupTarget.getFcmToken());
            backupTargetEntity.setPublicKey(backupTarget.getPublicKey());
            backupTargetEntity.setUUIDHash(backupTarget.getUUIDHash());
            backupTargetEntity.setName(backupTarget.getName());
            backupTargetEntity.setLastCheckedTime(backupTarget.getLastCheckedTime());
            backupTargetEntity.setSeedIndex(backupTarget.getSeedIndex());
            backupTargetEntity.setCheckSum(backupTarget.getCheckSum());
        }
        return backupTargetEntity;
    }

    public static boolean checkIfSaveCorrect(@NonNull Context context) {
        List<BackupTarget> backupTargets =
                com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupTargetUtil.getAll(context);
        List<BackupTargetEntity> backupTargetEntities =
                SocialKmDatabase.getInstance(context).backupTargetDao().getAll();
        if (backupTargets == null || backupTargetEntities == null) {
            LogUtil.logError(TAG, "backupTargets or backupTargetEntities is null or empty");
            return false;
        }

        for (BackupTargetEntity backupTargetEntity : backupTargetEntities) {
            backupTargetEntity.decrypt();
        }

        for (int i = 0; i < backupTargetEntities.size(); i++) {
            if (backupTargets.get(i).getStatus() != BACKUP_TARGET_STATUS_REQUEST) {
                if (backupTargetEntities.get(i).getStatus() != backupTargets.get(i).getStatus()) {
                    LogUtil.logError(
                            TAG, "backupTargetEntities getStatus is not equal to backupTargets");
                    return false;
                }
                if (!backupTargetEntities.get(i).getFcmToken()
                        .equals(backupTargets.get(i).getFcmToken())) {
                    LogUtil.logError(
                            TAG, "backupTargetEntities getFcmToken is not equal to backupTargets");
                    return false;
                }
                if (!backupTargetEntities.get(i).getPublicKey()
                        .equals(backupTargets.get(i).getPublicKey())) {
                    LogUtil.logDebug(
                            TAG, "backupTargetEntities getPublicKey is not equal to backupTargets");
                    return false;
                }
                if (!backupTargetEntities.get(i).getUUIDHash()
                        .equals(backupTargets.get(i).getUUIDHash())) {
                    LogUtil.logError(
                            TAG, "backupTargetEntities getUUIDHash is not equal to backupTargets");
                    return false;
                }
                if (!backupTargetEntities.get(i).getName().equals(backupTargets.get(i).getName())) {
                    LogUtil.logError(
                            TAG, "backupTargetEntities getName is not equal to backupTargets");
                    return false;
                }
                if (backupTargetEntities.get(i).getLastCheckedTime()
                        != backupTargets.get(i).getLastCheckedTime()) {
                    LogUtil.logError(TAG, "restoreSourceEntities getLastCheckedTime is "
                            + "not equal to backupTargets");
                    return false;
                }
                if (backupTargetEntities.get(i).getSeedIndex()
                        != backupTargets.get(i).getSeedIndex()) {
                    LogUtil.logError(TAG, "backupTargetEntities getSeedIndex is "
                            + "not equal to backupTargets");
                    return false;
                }
                if (backupTargetEntities.get(i).getCheckSum() != null) {
                    if (!backupTargetEntities.get(i).getCheckSum()
                            .equals(backupTargets.get(i).getCheckSum())) {
                        LogUtil.logError(TAG, "backupTargetEntities getCheckSum is "
                                + "not equal to backupTargets");
                        return false;
                    }
                }
            } else {
                if (backupTargetEntities.get(i).getStatus() != BACKUP_TARGET_STATUS_BAD) {
                    LogUtil.logError(TAG, "backupTargetEntities getStatus is "
                            + "not equal to BACKUP_TARGET_STATUS_NO_RESPONSE");
                    return false;
                }
                if (!TextUtils.isEmpty(backupTargetEntities.get(i).getFcmToken())) {
                    LogUtil.logError(TAG, "backupTargetEntities getFcmToken is "
                            + "not equal to default value");
                    return false;
                }
                if (!TextUtils.isEmpty(backupTargetEntities.get(i).getPublicKey())) {
                    LogUtil.logError(TAG, "backupTargetEntities getPublicKey is "
                            + "not equal to default value");
                    return false;
                }
            }
        }
        return true;
    }
}
