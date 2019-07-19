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

public class RestoreTargetUtil {

    private static final String TAG = "RestoreTargetUtil";
    private static final Object LOCK = new Object();

    private RestoreTargetUtil() {
    }

    @WorkerThread
    public static List<RestoreTarget> getAll(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        synchronized (LOCK) {
            List<RestoreTarget> restoreTargets = getAllInternal(context);
            if (restoreTargets == null) {
                LogUtil.logError(
                        TAG,
                        "restoreTargets is null",
                        new IllegalStateException("restoreTargets is null"));
                return null;
            }
            for (RestoreTarget restoreTarget : restoreTargets) {
                restoreTarget.decrypt();
            }
            return restoreTargets;
        }
    }

    @NonNull
    @WorkerThread
    private static ArrayList<RestoreTarget> getAllInternal(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        String json = SkrSharedPrefs.getRestoreTargets(context);
        if (TextUtils.isEmpty(json)) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        Type restoreTargetType = new TypeToken<ArrayList<RestoreTarget>>() {
        }.getType();
        ArrayList<RestoreTarget> restoreTargets = gson.fromJson(json, restoreTargetType);
        if (restoreTargets == null) {
            LogUtil.logError(
                    TAG,
                    "getAllInternal is null",
                    new IllegalStateException("getAllInternal is null"));
            return new ArrayList<>();
        }
        // Check upgrade
        boolean isUpgrade = false;
        for (RestoreTarget target : restoreTargets) {
            if (target.upgrade()) {
                isUpgrade = true;
            }
        }
        if (isUpgrade) {
            saveToSharedPrefs(context, restoreTargets);
        }
        return restoreTargets;
    }

    @Nullable
    @WorkerThread
    public static RestoreTarget get(
            @NonNull Context context, @NonNull String emailHash, @NonNull String uuidHash) {
        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(emailHash)) {
            throw new IllegalArgumentException("email hash is null or empty");
        }
        if (TextUtils.isEmpty(uuidHash)) {
            throw new IllegalArgumentException("uuidHash is null or empty");
        }
        synchronized (LOCK) {
            List<RestoreTarget> restoreTargets = getAllInternal(context);
            if (restoreTargets == null) {
                LogUtil.logError(TAG, "restoreTargets is null", new IllegalStateException());
                return null;
            }

            if (restoreTargets.isEmpty()) {
                return null;
            }

            RestoreTarget source = getInternal(context, restoreTargets, emailHash, uuidHash);
            if (source != null) {
                source.decrypt();
            }

            return source;
        }
    }

    @Nullable
    private static RestoreTarget getInternal(
            @NonNull Context context,
            @NonNull final List<RestoreTarget> restoreTargets,
            @NonNull String emailHash,
            @NonNull String uuidHash) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(restoreTargets, "restoreTargets is null");
        if (TextUtils.isEmpty(emailHash)) {
            throw new IllegalArgumentException("email hash is null or empty");
        }

        if (TextUtils.isEmpty(uuidHash)) {
            throw new IllegalArgumentException("uuidHash is null or empty");
        }

        for (RestoreTarget restoreTarget : restoreTargets) {
            if (restoreTarget.compareEmailHash(emailHash)
                    && restoreTarget.compareUUIDHash(uuidHash)) {
                return restoreTarget;
            }
        }
        return null;
    }

    @NonNull
    private static List<RestoreTarget> getAllByEmailHash(
            @NonNull Context context,
            @NonNull final List<RestoreTarget> restoreTargets,
            @NonNull String emailHash) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(context, "restoreTargets is null");
        if (TextUtils.isEmpty(emailHash)) {
            throw new IllegalArgumentException("email hash is null or empty");
        }

        List<RestoreTarget> sourceList = new ArrayList<>();
        for (RestoreTarget restoreTarget : restoreTargets) {
            if (restoreTarget.compareEmailHash(emailHash)) {
                sourceList.add(restoreTarget);
            }
        }
        return sourceList;
    }

    @WorkerThread
    public static boolean put(@NonNull Context context, @NonNull RestoreTarget restoreTarget) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(restoreTarget, "restoreTarget is null");
        synchronized (LOCK) {
            List<RestoreTarget> restoreTargets = getAllInternal(context);
            if (restoreTargets == null) {
                LogUtil.logError(TAG, "restoreTargets is null", new IllegalStateException());
                return false;
            }

            RestoreTarget cloneSource = new RestoreTarget(restoreTarget);
            String emailHash = cloneSource.getEmailHash();
            String uuidHash = cloneSource.getUUIDHash();
            RestoreTarget removing = getInternal(context, restoreTargets, emailHash, uuidHash);
            if (removing != null) {
                restoreTargets.remove(removing);
            }

            // Add to List
            cloneSource.encrypt();
            restoreTargets.add(cloneSource);

            // Save to SharedPrefs
            return saveToSharedPrefs(context, restoreTargets);
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
            List<RestoreTarget> restoreTargets = getAllInternal(context);
            if (restoreTargets == null) {
                LogUtil.logError(TAG, "restoreTargets is null", new IllegalStateException());
                return false;
            }

            // Find by emailHash and uuidHash
            RestoreTarget choice = getInternal(context, restoreTargets, emailHash, uuidHash);
            if (choice == null) {
                return false;
            } else {
                // Remove from list
                restoreTargets.remove(choice);
                // Save to SharedPrefs
                return saveToSharedPrefs(context, restoreTargets);
            }
        }
    }

    @WorkerThread
    public static boolean removeAllByEmailHash(
            @NonNull Context context, @NonNull String emailHash) {
        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(emailHash)) {
            throw new IllegalArgumentException("emailHash is null or empty");
        }
        synchronized (LOCK) {
            // Get all
            List<RestoreTarget> restoreTargets = getAllInternal(context);
            if (restoreTargets == null) {
                LogUtil.logError(TAG, "restoreTargets is null", new IllegalStateException());
                return false;
            }

            // Find by emailHash and uuid
            List<RestoreTarget> chooses = getAllByEmailHash(context, restoreTargets, emailHash);
            if (chooses == null || chooses.isEmpty()) {
                return false;
            } else {
                // Remove all from list
                for (RestoreTarget choose : chooses) {
                    restoreTargets.remove(choose);
                }
                // Save to SharedPrefs
                return saveToSharedPrefs(context, restoreTargets);
            }
        }
    }

    private static boolean saveToSharedPrefs(
            @NonNull Context context, @NonNull List<RestoreTarget> restoreTargets) {
        Objects.requireNonNull(context, "context is null");
        if (restoreTargets == null) {
            LogUtil.logError(TAG, "restoreTargets is null");
            return false;
        }

        Gson gson = new Gson();
        String json = gson.toJson(restoreTargets);
        SkrSharedPrefs.putRestoreTargets(context, json);
        return true;
    }
}
