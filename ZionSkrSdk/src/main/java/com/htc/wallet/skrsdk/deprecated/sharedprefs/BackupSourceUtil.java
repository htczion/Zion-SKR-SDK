package com.htc.wallet.skrsdk.deprecated.sharedprefs;

import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupSource.BACKUP_SOURCE_STATUS_DONE;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupSource.BACKUP_SOURCE_STATUS_FULL;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupSource.BACKUP_SOURCE_STATUS_OK;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupSource.BACKUP_SOURCE_STATUS_REQUEST;

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
import java.util.List;
import java.util.Objects;

public class BackupSourceUtil {

    private static final String TAG = "BackupSourceUtil";
    private static final Object LOCK = new Object();

    private BackupSourceUtil() {
    }

    @NonNull
    @WorkerThread
    public static List<BackupSource> getAll(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        synchronized (LOCK) {
            List<BackupSource> backupSources = getAllInternal(context);
            if (backupSources == null) {
                LogUtil.logError(TAG, "backupSources is null", new IllegalStateException());
                return null;
            }

            List<BackupSource> backupSourceList = new ArrayList<>();
            for (BackupSource source : backupSources) {
                source.decrypt();
                backupSourceList.add(source);
            }
            return backupSourceList;
        }
    }

    @NonNull
    @WorkerThread
    public static List<BackupSource> getAllPending(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        synchronized (LOCK) {
            List<BackupSource> backupSources = getAllInternal(context);
            if (backupSources == null) {
                LogUtil.logError(TAG, "backupSources is null", new IllegalStateException());
                return null;
            }

            List<BackupSource> pendingList = new ArrayList<>();
            for (BackupSource source : backupSources) {
                if (!source.compareStatus(BACKUP_SOURCE_STATUS_DONE)) {
                    source.decrypt();
                    pendingList.add(source);
                }
            }
            return pendingList;
        }
    }

    @WorkerThread
    public static List<BackupSource> getAllOK(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        synchronized (LOCK) {
            List<BackupSource> backupSources = getAllInternal(context);
            if (backupSources == null) {
                LogUtil.logError(TAG, "backupSources is null", new IllegalStateException());
                return null;
            }
            List<BackupSource> okList = new ArrayList<>();
            for (BackupSource source : backupSources) {
                if (source.compareStatus(BACKUP_SOURCE_STATUS_OK)
                        || source.compareStatus(BACKUP_SOURCE_STATUS_DONE)) {
                    source.decrypt();
                    okList.add(source);
                }
            }
            return okList;
        }
    }

    @NonNull
    @WorkerThread
    private static ArrayList<BackupSource> getAllInternal(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");

        String json = SkrSharedPrefs.getBackupSources(context);
        if (TextUtils.isEmpty(json)) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        Type backupSourceType = new TypeToken<ArrayList<BackupSource>>() {
        }.getType();
        ArrayList<BackupSource> backupSources = gson.fromJson(json, backupSourceType);
        if (backupSources == null) {
            LogUtil.logError(
                    TAG,
                    "getAllInternal is null",
                    new IllegalStateException("getAllInternal is null"));
            return new ArrayList<>();
        }
        // Check upgrade
        boolean isUpgrade = false;
        for (BackupSource source : backupSources) {
            if (source.upgrade()) {
                isUpgrade = true;
            }
        }
        if (isUpgrade) {
            saveToSharedPrefs(context, backupSources);
        }

        return backupSources;
    }

    @Nullable
    @WorkerThread
    public static BackupSource getWithUUIDHash(
            @NonNull Context context, @NonNull String emailHash, @NonNull String uuidHash) {
        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(emailHash)) {
            throw new IllegalArgumentException("email hash is null or empty");
        }
        if (TextUtils.isEmpty(uuidHash)) {
            throw new IllegalArgumentException("uuidHash is null or empty");
        }

        synchronized (LOCK) {
            List<BackupSource> backupSources = getAllInternal(context);
            if (backupSources == null) {
                LogUtil.logError(TAG, "backupSources is null", new IllegalStateException());
                return null;
            }

            if (backupSources.isEmpty()) {
                return null;
            }

            BackupSource source =
                    getWithUUIDHashInternal(context, backupSources, emailHash, uuidHash);
            if (source != null) {
                source.decrypt();
            }

            return source;
        }
    }

    @Nullable
    private static BackupSource getWithUUIDHashInternal(
            @NonNull Context context,
            @NonNull final List<BackupSource> backupSources,
            @NonNull String emailHash,
            @NonNull String uuidHash) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(backupSources, "backupSources is null");
        if (TextUtils.isEmpty(emailHash)) {
            throw new IllegalArgumentException("email hash is null or empty");
        }

        if (TextUtils.isEmpty(uuidHash)) {
            throw new IllegalArgumentException("uuidHash is null or empty");
        }

        for (BackupSource backupSource : backupSources) {
            if (backupSource.compareEmailHash(emailHash)
                    && backupSource.compareUUIDHash(uuidHash)) {
                return backupSource;
            }
        }
        return null;
    }

    // Only recover flow
    @Nullable
    @WorkerThread
    public static BackupSource getOK(
            @NonNull Context context, @NonNull String emailHash, @Nullable String uuidHash) {
        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(emailHash)) {
            throw new IllegalArgumentException("email hash is null or empty");
        }

        synchronized (LOCK) {
            List<BackupSource> backupSources = getAllInternal(context);
            Objects.requireNonNull(backupSources, "backupSources is null");

            if (backupSources.isEmpty()) {
                return null;
            }

            List<BackupSource> sourceList = getOKInternal(context, backupSources, emailHash);
            if (sourceList.isEmpty()) {
                return null;
            }

            BackupSource source = null;
            if (TextUtils.isEmpty(uuidHash)) {
                // Without UUID Hash, Find the latest one.
                LogUtil.logError(TAG, "Without UUID Hash, Find the latest one.");
                source = sourceList.get(0);
                if (sourceList.size() > 1) {
                    for (int i = 1; i < sourceList.size(); i++) {
                        BackupSource comparison = sourceList.get(i);
                        if (comparison.getTimeStamp() > source.getTimeStamp()) {
                            source = comparison;
                        }
                    }
                }
                source.decrypt();
            } else {
                // With UUID Hash
                for (int i = 0; i < sourceList.size(); i++) {
                    BackupSource comparison = sourceList.get(i);
                    if (comparison.compareUUIDHash(uuidHash)) {
                        source = comparison;
                        source.decrypt();
                        break;
                    }
                }
            }
            return source;
        }
    }

    @NonNull
    private static List<BackupSource> getOKInternal(
            @NonNull Context context,
            @NonNull final List<BackupSource> backupSources,
            @NonNull String emailHash) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(context, "backupSources is null");
        if (TextUtils.isEmpty(emailHash)) {
            throw new IllegalArgumentException("email hash is null or empty");
        }

        List<BackupSource> sourceList = getAllByEmailHash(context, backupSources, emailHash);

        for (BackupSource source : sourceList) {
            // Only return BACKUP_SOURCE_STATUS_OK or BACKUP_SOURCE_STATUS_DONE
            if (source.compareStatus(BACKUP_SOURCE_STATUS_OK)
                    || source.compareStatus(BACKUP_SOURCE_STATUS_DONE)) {
                continue;
            }
            sourceList.remove(source);
        }

        return sourceList;
    }

    @NonNull
    private static List<BackupSource> getAllByEmailHash(
            @NonNull Context context,
            @NonNull final List<BackupSource> backupSources,
            @NonNull String emailHash) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(context, "backupSources is null");
        if (TextUtils.isEmpty(emailHash)) {
            throw new IllegalArgumentException("email hash is null or empty");
        }

        List<BackupSource> sourceList = new ArrayList<>();
        for (BackupSource backupSource : backupSources) {
            if (backupSource.compareEmailHash(emailHash)) {
                sourceList.add(backupSource);
            }
        }
        return sourceList;
    }

    @WorkerThread
    public static boolean put(@NonNull Context context, @NonNull BackupSource backupSource) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(backupSource, "backupSource is null");
        synchronized (LOCK) {
            List<BackupSource> backupSources = getAllInternal(context);
            if (backupSources == null) {
                LogUtil.logError(TAG, "backupSources is null", new IllegalStateException());
                return false;
            }

            BackupSource cloneSource = new BackupSource(backupSource);
            String emailHash = cloneSource.getEmailHash();
            String uuidHash = cloneSource.getUUIDHash();
            // Same emailHash and uuidHash, previous backupSource
            BackupSource previousSource =
                    getWithUUIDHashInternal(context, backupSources, emailHash, uuidHash);

            switch (cloneSource.getStatus()) {
                case BACKUP_SOURCE_STATUS_REQUEST:
                    // Remove previous backupSource
                    if (previousSource != null) {
                        backupSources.remove(previousSource);
                        LogUtil.logDebug(
                                TAG,
                                "Remove the previous "
                                        + previousSource.getName()
                                        + "'s backupSource before saving new one");
                    }
                    break;
                case BACKUP_SOURCE_STATUS_OK:
                case BACKUP_SOURCE_STATUS_DONE:
                    // Remove previous backupSource
                    if (previousSource != null) {
                        backupSources.remove(previousSource);
                        LogUtil.logDebug(
                                TAG,
                                "Remove the previous "
                                        + previousSource.getName()
                                        + "'s backupSource before saving new one");
                    }
                    // Remove all has same emailHash backupSource with request state
                    List<BackupSource> chooses =
                            getAllByEmailHash(context, backupSources, emailHash);
                    LogUtil.logDebug(
                            TAG,
                            "Remove all the request state backupSources that have the same "
                                    + "emailHash");
                    for (BackupSource choose : chooses) {
                        if (choose.compareStatus(BACKUP_SOURCE_STATUS_REQUEST)) {
                            backupSources.remove(choose);
                        }
                    }
                    break;
                case BACKUP_SOURCE_STATUS_FULL:
                    // Remove previous backupSource
                    if (previousSource != null) {
                        backupSources.remove(previousSource);
                        LogUtil.logDebug(
                                TAG,
                                "Remove the previous "
                                        + previousSource.getName()
                                        + "'s backupSource before saving new one");
                    }
                    break;
                default:
                    throw new IllegalStateException("incorrect status");
            }

            // Add to List
            cloneSource.encrypt();
            backupSources.add(cloneSource);

            // Save to SharedPrefs
            return saveToSharedPrefs(context, backupSources);
        }
    }

    @WorkerThread
    public static boolean remove(
            @NonNull Context context, @NonNull String emailHash, @NonNull String uuidHash) {
        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(emailHash)) {
            throw new IllegalArgumentException("emailHash is null or empty");
        }
        if (TextUtils.isEmpty(uuidHash)) {
            throw new IllegalArgumentException("uuidHash is null or empty");
        }
        synchronized (LOCK) {
            // Get all
            List<BackupSource> backupSources = getAllInternal(context);
            if (backupSources == null) {
                LogUtil.logError(TAG, "backupSources is null", new IllegalStateException());
                return false;
            }

            // Find by emailHash and uuidHash
            BackupSource choice =
                    getWithUUIDHashInternal(context, backupSources, emailHash, uuidHash);
            if (choice == null) {
                return false;
            } else {
                // Remove from list
                backupSources.remove(choice);
                // Save to SharedPrefs
                return saveToSharedPrefs(context, backupSources);
            }
        }
    }

    private static boolean saveToSharedPrefs(
            @NonNull Context context, @NonNull List<BackupSource> backupSources) {
        Objects.requireNonNull(context, "context is null");
        if (backupSources == null) {
            LogUtil.logError(TAG, "backupSources is null");
            return false;
        }

        Gson gson = new Gson();
        String json = gson.toJson(backupSources);
        SkrSharedPrefs.putBackupSources(context, json);
        return true;
    }
}
