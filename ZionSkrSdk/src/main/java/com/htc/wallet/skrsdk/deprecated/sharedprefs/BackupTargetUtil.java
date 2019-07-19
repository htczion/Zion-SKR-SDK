package com.htc.wallet.skrsdk.deprecated.sharedprefs;

import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupTarget.BACKUP_TARGET_STATUS_OK;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupTarget.BACKUP_TARGET_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.secretsharing.SeedUtil.INDEX_MAX;
import static com.htc.wallet.skrsdk.secretsharing.SeedUtil.INDEX_MIN;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.SOCIAL_KEY_RECOVERY_ACTIVE_THRESHOLD;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BackupTargetUtil {

    private static final String TAG = "BackupTargetUtil";
    private static final Object LOCK = new Object();

    private BackupTargetUtil() {
    }

    @WorkerThread
    public static List<BackupTarget> getAll(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        synchronized (LOCK) {
            List<BackupTarget> backupTargets = getAllInternal(context);
            if (backupTargets == null) {
                LogUtil.logError(
                        TAG,
                        "backupTargets is null",
                        new IllegalStateException("backupTargets is null"));
                return null;
            }
            for (BackupTarget backupTarget : backupTargets) {
                backupTarget.decrypt();
            }
            return backupTargets;
        }
    }

    @WorkerThread
    public static List<BackupTarget> getAllPending(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        synchronized (LOCK) {
            List<BackupTarget> backupTargets = getAllInternal(context);
            if (backupTargets == null) {
                LogUtil.logError(
                        TAG,
                        "backupTargets is null",
                        new IllegalStateException("backupTargets is null"));
                return null;
            }
            List<BackupTarget> pendingList = new ArrayList<>();
            for (BackupTarget backupTarget : backupTargets) {
                if (backupTarget.compareStatus(BACKUP_TARGET_STATUS_REQUEST)) {
                    backupTarget.decrypt();
                    pendingList.add(backupTarget);
                }
            }
            return pendingList;
        }
    }

    @NonNull
    @WorkerThread
    public static List<BackupTarget> getAllOK(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        synchronized (LOCK) {
            List<BackupTarget> backupTargets = getAllInternal(context);
            if (backupTargets == null) {
                LogUtil.logError(
                        TAG,
                        "backupTargets is null",
                        new IllegalStateException("backupTargets is null"));
                return new ArrayList<>();
            }
            List<BackupTarget> okList = new ArrayList<>();
            for (BackupTarget backupTarget : backupTargets) {
                if (backupTarget.compareStatus(BACKUP_TARGET_STATUS_OK)) {
                    backupTarget.decrypt();
                    okList.add(backupTarget);
                }
            }
            return okList;
        }
    }

    @NonNull
    @WorkerThread
    public static List<BackupTarget> getAllOKAndNoResponse(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        synchronized (LOCK) {
            List<BackupTarget> backupTargets = getAllInternal(context);
            if (backupTargets == null) {
                LogUtil.logError(
                        TAG,
                        "backupTargets is null",
                        new IllegalStateException("backupTargets is null"));
                return new ArrayList<>();
            }
            List<BackupTarget> okList = new ArrayList<>();
            for (BackupTarget backupTarget : backupTargets) {
                if (backupTarget.compareStatus(BACKUP_TARGET_STATUS_OK)
                        || backupTarget.compareStatus(
                        BackupTarget.BACKUP_TARGET_STATUS_NO_RESPONSE)) {
                    backupTarget.decrypt();
                    okList.add(backupTarget);
                }
            }
            return okList;
        }
    }

    @NonNull
    @WorkerThread
    private static ArrayList<BackupTarget> getAllInternal(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        String json = SkrSharedPrefs.getBackupTargets(context);
        if (TextUtils.isEmpty(json)) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        Type backupTargetType = new TypeToken<ArrayList<BackupTarget>>() {
        }.getType();
        ArrayList<BackupTarget> backupTargets = gson.fromJson(json, backupTargetType);
        if (backupTargets == null) {
            LogUtil.logError(
                    TAG,
                    "getAllInternal is null",
                    new IllegalStateException("getAllInternal is null"));
            return new ArrayList<>();
        }
        // Check upgrade
        boolean isUpgrade = false;
        for (BackupTarget target : backupTargets) {
            if (target.upgrade()) {
                isUpgrade = true;
            }
        }
        if (isUpgrade) {
            saveToSharedPrefs(context, backupTargets);
        }
        return backupTargets;
    }

    @Nullable
    @WorkerThread
    public static BackupTarget get(@NonNull Context context, @NonNull String uuidHash) {
        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(uuidHash)) {
            throw new IllegalArgumentException("uuidHash is null or empty");
        }
        synchronized (LOCK) {
            List<BackupTarget> backupTargets = getAllInternal(context);
            if (backupTargets == null) {
                LogUtil.logError(
                        TAG,
                        "backupTargets is null",
                        new IllegalStateException("backupTargets is null"));
                return null;
            }

            BackupTarget target = getInternal(context, backupTargets, uuidHash);
            if (target != null) {
                target.decrypt();
            }

            return target;
        }
    }

    @Nullable
    private static BackupTarget getInternal(
            @NonNull Context context,
            @NonNull final List<BackupTarget> backupTargets,
            @NonNull String uuidHash) {
        Objects.requireNonNull(context, "context is null");
        if (backupTargets == null) {
            LogUtil.logError(
                    TAG,
                    "backupTargets is null",
                    new IllegalStateException("backupTargets is null"));
            return null;
        }
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(TAG, "uuidHash is null or empty");
            return null;
        }

        for (BackupTarget backupTarget : backupTargets) {
            if (backupTarget.compareUUIDHash(uuidHash)) {
                return backupTarget;
            }
        }
        return null;
    }

    @WorkerThread
    public static boolean put(@NonNull Context context, @NonNull BackupTarget backupTarget) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(backupTarget, "backupTarget is null");
        synchronized (LOCK) {
            List<BackupTarget> backupTargets = getAllInternal(context);
            if (backupTargets == null) {
                LogUtil.logError(
                        TAG,
                        "backupTargets is null",
                        new IllegalStateException("backupTargets is null"));
                return false;
            }

            BackupTarget cloneTarget = new BackupTarget(backupTarget);
            String uuidHash = cloneTarget.getUUIDHash();
            // Remove origin if existed
            BackupTarget removing = getInternal(context, backupTargets, uuidHash);
            if (removing != null) {
                backupTargets.remove(removing);
            }

            // Encrypt and add to List
            cloneTarget.encrypt();
            backupTargets.add(cloneTarget);

            // Save to SharedPrefs
            return saveToSharedPrefs(context, backupTargets);
        }
    }

    @WorkerThread
    public static boolean remove(@NonNull Context context, @NonNull String uuidHash) {
        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(uuidHash)) {
            throw new IllegalArgumentException("uuidHash is null or empty");
        }
        synchronized (LOCK) {
            // Get all
            List<BackupTarget> backupTargets = getAllInternal(context);
            if (backupTargets == null) {
                LogUtil.logError(
                        TAG,
                        "backupTargets is null",
                        new IllegalStateException("backupTargets is null"));
                return false;
            }

            // Find by uuidHash
            BackupTarget choice = getInternal(context, backupTargets, uuidHash);
            if (choice == null) {
                return false;
            } else {
                // Remove from list
                backupTargets.remove(choice);
                // Save to SharedPrefs
                return saveToSharedPrefs(context, backupTargets);
            }
        }
    }

    @WorkerThread
    public static boolean removeAll(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        synchronized (LOCK) {
            return saveToSharedPrefs(context, new ArrayList<BackupTarget>());
        }
    }

    @WorkerThread
    private static boolean saveToSharedPrefs(
            @NonNull Context context, @NonNull List<BackupTarget> backupTargets) {
        Objects.requireNonNull(context, "context is null");
        if (backupTargets == null) {
            LogUtil.logError(
                    TAG,
                    "backupTargets is null",
                    new IllegalStateException("backupTargets is null"));
            return false;
        }
        Gson gson = new Gson();
        String json = gson.toJson(backupTargets);
        SkrSharedPrefs.putBackupTargets(context, json);
        return true;
    }

    @NonNull
    public static List<Integer> getFreeSeedIndex(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        synchronized (LOCK) {
            List<Integer> freeSeedIndexes = new ArrayList<>();
            for (int i = INDEX_MIN; i <= INDEX_MAX; i++) {
                freeSeedIndexes.add(i);
            }

            List<BackupTarget> targets = BackupTargetUtil.getAllInternal(context);
            if (targets == null) {
                LogUtil.logError(
                        TAG,
                        "backupTargets is null",
                        new IllegalStateException("backupTargets is null"));
                return Collections.emptyList();
            }

            for (BackupTarget target : targets) {
                int seedIndex = target.getSeedIndex();
                if (seedIndex != BackupTarget.UNDEFINED_SEED_INDEX
                        && freeSeedIndexes.contains(seedIndex)) {
                    freeSeedIndexes.remove((Integer) seedIndex);
                }
            }

            return freeSeedIndexes;
        }
    }

    public static boolean isActive(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        List<BackupTarget> targets = BackupTargetUtil.getAllInternal(context);
        if (targets == null) {
            LogUtil.logError(
                    TAG,
                    "backupTargets is null",
                    new IllegalStateException("backupTargets is null"));
            return false;
        }

        int activeNumber = 0;
        for (BackupTarget target : targets) {
            if (target.compareStatus(BACKUP_TARGET_STATUS_OK)) {
                activeNumber++;
            }
        }

        return (activeNumber >= SOCIAL_KEY_RECOVERY_ACTIVE_THRESHOLD);
    }
}
