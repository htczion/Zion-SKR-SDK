package com.htc.wallet.skrsdk.sqlite.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.error.DatabaseSaveException;
import com.htc.wallet.skrsdk.sqlite.SocialKmDatabase;
import com.htc.wallet.skrsdk.sqlite.dao.RestoreSourceDao;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.List;
import java.util.Objects;

public class RestoreSourceUtil {
    private static final String TAG = "RestoreSourceUtil";

    private RestoreSourceUtil() {
        throw new AssertionError();
    }

    public static void getAll(
            @NonNull final Context context, @NonNull final LoadListListener loadListListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(loadListListener);

        DatabaseWorkManager.getInstance().enqueueRead(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                List<RestoreSourceEntity> restoreSources;
                restoreSources =
                        SocialKmDatabase.getInstance(context)
                                .restoreSourceDao().getAll();
                if (restoreSources != null) {
                    for (RestoreSourceEntity restoreSource : restoreSources) {
                        restoreSource.decrypt();
                    }
                } else {
                    LogUtil.logDebug(TAG, "Failed to getAll from restoreSourceDao");
                }
                loadListListener.onLoadFinished(null, null, restoreSources, null);
            }
        });
    }

    public static void getWithUUIDHash(
            @NonNull final Context context,
            @NonNull final String uuidHash,
            @NonNull final LoadDataListener loadDataListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(loadDataListener);
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(TAG, "uuidHash is null or empty");
            return;
        }

        DatabaseWorkManager.getInstance().enqueueRead(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                RestoreSourceEntity sourceEntity;
                sourceEntity = SocialKmDatabase.getInstance(context)
                        .restoreSourceDao().getWithUUIDHash(uuidHash);
                if (sourceEntity != null) {
                    sourceEntity.decrypt();
                } else {
                    LogUtil.logDebug(TAG, "Failed to getWithUUIDHash from restoreSourceDao");
                }
                loadDataListener.onLoadFinished(null, null, sourceEntity, null);
            }
        });
    }

    public static void put(
            @NonNull final Context context,
            @NonNull final RestoreSourceEntity restoreSource,
            @NonNull final DatabaseCompleteListener databaseCompleteListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(restoreSource, "restoreSource is null");
        Objects.requireNonNull(databaseCompleteListener);

        final RestoreSourceEntity cloneRestoreSourceEntity = new RestoreSourceEntity(restoreSource);
        DatabaseWorkManager.getInstance().enqueueWrite(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                RestoreSourceDao restoreSourceDao =
                        SocialKmDatabase.getInstance(context).restoreSourceDao();
                String uuidHash = cloneRestoreSourceEntity.getUUIDHash();
                // Remove origin if existed
                RestoreSourceEntity removing =
                        restoreSourceDao.getWithUUIDHash(uuidHash);
                if (removing != null) {
                    restoreSourceDao.remove(removing);
                    LogUtil.logDebug(TAG, "Remove existing RestoreSourceEntity, uuidHash="
                            + removing.getUUIDHash());
                }
                // Encrypt and add to List
                cloneRestoreSourceEntity.encrypt();
                long result = restoreSourceDao.insert(cloneRestoreSourceEntity);
                if (result > 0) {
                    databaseCompleteListener.onComplete();
                } else {
                    databaseCompleteListener.onError(new DatabaseSaveException("Insert failed"));
                }
            }
        });
    }

    public static void clearData(@NonNull final Context context) {
        Objects.requireNonNull(context);

        DatabaseWorkManager.getInstance().enqueueWrite(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                LogUtil.logInfo(TAG, "Start to clear restoreSource data");
                SocialKmDatabase.getInstance(context).restoreSourceDao().clearData();
                LogUtil.logInfo(TAG, "End of clear restoreSource data");
            }
        });
    }

    public static void removeAll(@NonNull final Context context) {
        Objects.requireNonNull(context);

        DatabaseWorkManager.getInstance().enqueueWrite(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                RestoreSourceDao restoreSourceDao =
                        SocialKmDatabase.getInstance(context).restoreSourceDao();
                List<RestoreSourceEntity> restoreSourceEntityList = restoreSourceDao.getAll();
                if (restoreSourceEntityList != null) {
                    int removeLine = restoreSourceDao.removeAll(restoreSourceEntityList);
                    if (removeLine <= 0) {
                        LogUtil.logWarning(TAG, "No data removed");
                    }
                } else {
                    LogUtil.logDebug(TAG, "restoreSourceEntityList is null"
                            + ", nothing to be removed");
                }
            }
        });
    }

    public static void removeWithUUIDHash(
            @NonNull final Context context, @NonNull final String uuidHash) {
        Objects.requireNonNull(context);
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(TAG, "uuidHash is null or empty");
            return;
        }

        DatabaseWorkManager.getInstance().enqueueWrite(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                RestoreSourceDao restoreSourceDao =
                        SocialKmDatabase.getInstance(context).restoreSourceDao();
                // Find by UUID Hash
                RestoreSourceEntity choice = restoreSourceDao.getWithUUIDHash(uuidHash);
                if (choice != null) {
                    LogUtil.logDebug(TAG, "Remove " + choice.getName() + "'s restoreSource");
                    int removeLine = restoreSourceDao.remove(choice);
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
            @NonNull final RestoreSourceEntity restoreSourceEntity,
            @NonNull final DatabaseCompleteListener databaseCompleteListener) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(restoreSourceEntity);
        Objects.requireNonNull(databaseCompleteListener);

        final RestoreSourceEntity cloneRestoreSourceEntity =
                new RestoreSourceEntity(restoreSourceEntity);
        DatabaseWorkManager.getInstance().enqueueWrite(new DatabaseWorkManager.DatabaseWorkItem() {
            @Override
            public void run() {
                RestoreSourceDao restoreSourceDao =
                        SocialKmDatabase.getInstance(context).restoreSourceDao();
                cloneRestoreSourceEntity.encrypt();
                int updateRowId = restoreSourceDao.update(cloneRestoreSourceEntity);
                if (updateRowId <= 0) {
                    LogUtil.logWarning(TAG, "No data updated");
                }
                databaseCompleteListener.onComplete();
            }
        });
    }
}
