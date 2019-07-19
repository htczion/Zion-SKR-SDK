package com.htc.wallet.skrsdk.sqlite.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.error.DatabaseSaveException;
import com.htc.wallet.skrsdk.sqlite.SocialKmDatabase;
import com.htc.wallet.skrsdk.sqlite.dao.RestoreTargetDao;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.List;
import java.util.Objects;

public class RestoreTargetUtil {
    private static final String TAG = "RestoreTargetUtil";

    private RestoreTargetUtil() {
        throw new AssertionError();
    }

    // TODO: change to package-private
    public static void getAll(
            @NonNull final Context context,
            @NonNull final LoadListListener loadListListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(loadListListener);

        DatabaseWorkManager.getInstance().enqueueRead(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                List<RestoreTargetEntity> restoreTargetList;
                restoreTargetList = SocialKmDatabase.getInstance(context)
                        .restoreTargetDao().getAll();
                if (restoreTargetList != null) {
                    for (RestoreTargetEntity restoreTargetEntity : restoreTargetList) {
                        restoreTargetEntity.decrypt();
                    }
                } else {
                    LogUtil.logDebug(TAG, "Failed to getAll from restoreTargetDao");
                }
                loadListListener.onLoadFinished(null, null, null, restoreTargetList);
            }
        });
    }

    public static void get(
            @NonNull final Context context,
            @NonNull final String emailHash,
            @NonNull final String uuidHash,
            @NonNull final LoadDataListener loadDataListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(loadDataListener);
        if (TextUtils.isEmpty(emailHash)) {
            LogUtil.logError(TAG, "emailHash is null or empty");
            return;
        }
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(TAG, "uuidHash is null or empty");
            return;
        }

        DatabaseWorkManager.getInstance().enqueueRead(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                RestoreTargetEntity restoreTargetEntity;
                restoreTargetEntity = SocialKmDatabase.getInstance(context)
                        .restoreTargetDao().get(emailHash, uuidHash);
                if (restoreTargetEntity != null) {
                    restoreTargetEntity.decrypt();
                } else {
                    LogUtil.logDebug(TAG, "Failed to get from restoreTargetDao"
                            + ", emailHash=" + emailHash
                            + ", uuidHash=" + uuidHash);
                }
                loadDataListener.onLoadFinished(null, null, null, restoreTargetEntity);
            }
        });
    }

    public static void put(
            @NonNull final Context context,
            @NonNull final RestoreTargetEntity restoreTargetEntity,
            @NonNull final DatabaseCompleteListener databaseCompleteListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(restoreTargetEntity, "restoreTargetEntity is null");
        Objects.requireNonNull(databaseCompleteListener);

        final RestoreTargetEntity cloneRestoreTargetEntity =
                new RestoreTargetEntity(restoreTargetEntity);
        DatabaseWorkManager.getInstance().enqueueWrite(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                RestoreTargetDao restoreTargetDao = SocialKmDatabase.getInstance(context)
                        .restoreTargetDao();
                String uuidHash = cloneRestoreTargetEntity.getUUIDHash();
                // Remove origin if existed
                RestoreTargetEntity removing =
                        restoreTargetDao.getWithUUIDHash(uuidHash);
                if (removing != null) {
                    restoreTargetDao.remove(removing);
                    LogUtil.logDebug(TAG, "Remove existing RestoreTargetEntity"
                            + ", uuidHash=" + removing.getUUIDHash());
                }
                cloneRestoreTargetEntity.encrypt();
                long result = restoreTargetDao.insert(cloneRestoreTargetEntity);
                if (result > 0) {
                    databaseCompleteListener.onComplete();
                } else {
                    databaseCompleteListener.onError(
                            new DatabaseSaveException("Insert failed"));
                }
            }
        });
    }

    public static void remove(
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

        DatabaseWorkManager.getInstance().enqueueWrite(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                RestoreTargetDao restoreTargetDao = SocialKmDatabase.getInstance(context)
                        .restoreTargetDao();
                RestoreTargetEntity choice =
                        restoreTargetDao.get(emailHash, uuidHash);
                if (choice != null) {
                    LogUtil.logDebug(
                            TAG, "Remove " + choice.getName() + "'s restoreTarget");
                    int removeLine = restoreTargetDao.remove(choice);
                    if (removeLine <= 0) {
                        LogUtil.logWarning(TAG, "No data removed");
                    }
                } else {
                    LogUtil.logDebug(TAG, "choice is null, nothing to be removed");
                }
            }
        });
    }

    public static void update(
            @NonNull final Context context,
            @NonNull final RestoreTargetEntity restoreTargetEntity,
            @NonNull final DatabaseCompleteListener databaseCompleteListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(restoreTargetEntity);
        Objects.requireNonNull(databaseCompleteListener);

        final RestoreTargetEntity cloneRestoreTargetEntity =
                new RestoreTargetEntity(restoreTargetEntity);
        DatabaseWorkManager.getInstance().enqueueWrite(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                RestoreTargetDao restoreTargetDao =
                        SocialKmDatabase.getInstance(context).restoreTargetDao();
                cloneRestoreTargetEntity.encrypt();
                int updateRowId = restoreTargetDao.update(cloneRestoreTargetEntity);
                if (updateRowId <= 0) {
                    LogUtil.logWarning(TAG, "No data updated");
                }
                databaseCompleteListener.onComplete();
            }
        });
    }
}
