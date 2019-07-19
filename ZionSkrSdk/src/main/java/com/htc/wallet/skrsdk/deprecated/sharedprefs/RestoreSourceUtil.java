package com.htc.wallet.skrsdk.deprecated.sharedprefs;

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

public class RestoreSourceUtil {

    private static final String TAG = "RestoreSourceUtil";
    private static final Object LOCK = new Object();

    private RestoreSourceUtil() {
    }

    @WorkerThread
    public static List<RestoreSource> getAll(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        synchronized (LOCK) {
            List<RestoreSource> restoreSources = getAllInternal(context);
            if (restoreSources == null) {
                LogUtil.logError(
                        TAG,
                        "restoreSources is null",
                        new IllegalStateException("restoreSources is null"));
                return null;
            }
            for (RestoreSource restoreSource : restoreSources) {
                restoreSource.decrypt();
            }
            return restoreSources;
        }
    }

    @NonNull
    @WorkerThread
    private static ArrayList<RestoreSource> getAllInternal(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        String json = SkrSharedPrefs.getRestoreSources(context);
        if (TextUtils.isEmpty(json)) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        Type restoreSourceType = new TypeToken<ArrayList<RestoreSource>>() {
        }.getType();
        ArrayList<RestoreSource> restoreSources = gson.fromJson(json, restoreSourceType);
        if (restoreSources == null) {
            LogUtil.logError(
                    TAG,
                    "getAllInternal is null",
                    new IllegalStateException("getAllInternal is null"));
            return new ArrayList<>();
        }
        // Check upgrade
        boolean isUpgrade = false;
        for (RestoreSource source : restoreSources) {
            if (source.upgrade()) {
                isUpgrade = true;
            }
        }
        if (isUpgrade) {
            saveToSharedPrefs(context, restoreSources);
        }
        return restoreSources;
    }

    @Nullable
    @WorkerThread
    public static RestoreSource getWithUUIDHash(
            @NonNull Context context, @NonNull String uuidHash) {
        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(uuidHash)) {
            throw new IllegalArgumentException("uuidHash is null or empty");
        }
        synchronized (LOCK) {
            List<RestoreSource> restoreSources = getAllInternal(context);
            if (restoreSources == null) {
                LogUtil.logError(
                        TAG,
                        "restoreSources is null",
                        new IllegalStateException("restoreSources is null"));
                return null;
            }

            RestoreSource target = getWithUUIDHashInternal(context, restoreSources, uuidHash);
            if (target != null) {
                target.decrypt();
            }

            return target;
        }
    }

    @Nullable
    private static RestoreSource getWithUUIDHashInternal(
            @NonNull Context context,
            @NonNull final List<RestoreSource> restoreSources,
            @NonNull String uuidHash) {
        Objects.requireNonNull(context, "context is null");
        if (restoreSources == null) {
            LogUtil.logError(
                    TAG,
                    "restoreSources is null",
                    new IllegalStateException("restoreSources is null"));
            return null;
        }
        if (TextUtils.isEmpty(uuidHash)) {
            throw new IllegalArgumentException("uuidHash is null or empty");
        }

        for (RestoreSource restoreSource : restoreSources) {
            if (restoreSource.compareUUIDHash(uuidHash)) {
                return restoreSource;
            }
        }
        return null;
    }

    @WorkerThread
    public static boolean put(@NonNull Context context, @NonNull RestoreSource restoreSource) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(restoreSource, "restoreSource is null");
        synchronized (LOCK) {
            List<RestoreSource> restoreSources = getAllInternal(context);
            if (restoreSources == null) {
                LogUtil.logError(
                        TAG,
                        "restoreSources is null",
                        new IllegalStateException("restoreSources is null"));
                return false;
            }

            RestoreSource cloneSource = new RestoreSource(restoreSource);
            String uuidHash = cloneSource.getUUIDHash();
            // Remove origin if existed
            RestoreSource removing = getWithUUIDHashInternal(context, restoreSources, uuidHash);
            if (removing != null) {
                restoreSources.remove(removing);
            }

            // Encrypt and add to List
            cloneSource.encrypt();
            restoreSources.add(cloneSource);

            // Save to SharedPrefs
            return saveToSharedPrefs(context, restoreSources);
        }
    }

    @WorkerThread
    public static boolean removeWithUUIDHash(@NonNull Context context, @NonNull String uuidHash) {
        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(uuidHash)) {
            throw new IllegalArgumentException("uuidHash is null or empty");
        }
        synchronized (LOCK) {
            // Get all
            List<RestoreSource> restoreSources = getAllInternal(context);
            if (restoreSources == null) {
                LogUtil.logError(
                        TAG,
                        "restoreSources is null",
                        new IllegalStateException("restoreSources is null"));
                return false;
            }

            // Find by UUID Hash
            RestoreSource choice = getWithUUIDHashInternal(context, restoreSources, uuidHash);
            if (choice == null) {
                return false;
            } else {
                // Remove from list
                restoreSources.remove(choice);
                // Save to SharedPrefs
                return saveToSharedPrefs(context, restoreSources);
            }
        }
    }

    @WorkerThread
    public static boolean removeAll(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        synchronized (LOCK) {
            return saveToSharedPrefs(context, new ArrayList<RestoreSource>());
        }
    }

    @WorkerThread
    private static boolean saveToSharedPrefs(
            @NonNull Context context, @NonNull List<RestoreSource> restoreSources) {
        Objects.requireNonNull(context, "context is null");
        if (restoreSources == null) {
            LogUtil.logError(
                    TAG,
                    "restoreSources is null",
                    new IllegalStateException("restoreSources is null"));
            return false;
        }
        Gson gson = new Gson();
        String json = gson.toJson(restoreSources);
        SkrSharedPrefs.putRestoreSources(context, json);
        return true;
    }
}
