package com.htc.wallet.skrsdk.restore.reconnect;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.crypto.util.GenericCipherUtil;
import com.htc.wallet.skrsdk.util.JsonUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class ReconnectNameUtils {
    private static final String TAG = "ReconnectNameUtils";

    @WorkerThread
    public static List<String> getNameList(@NonNull final Context context) {
        Objects.requireNonNull(context, "context is null");

        String encNameJson = SkrSharedPrefs.getSkrReconnectNamesToJson(context);
        if (TextUtils.isEmpty(encNameJson)) {
            return Collections.emptyList();
        }

        List<String> encNameList = JsonUtil.jsonToList(encNameJson);
        if (encNameList == null) {
            LogUtil.logError(TAG, "encNameList is null");
            return Collections.emptyList();
        }

        final GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
        final List<String> nameList = new ArrayList<>();
        for (String encName : encNameList) {
            if (TextUtils.isEmpty(encName)) {
                LogUtil.logError(TAG, "encName is null or empty");
            } else {
                String name = genericCipherUtil.decryptData(encName);
                nameList.add(name);
            }
        }

        return nameList;
    }

    @WorkerThread
    public static void putNameList(
            @NonNull final Context context, @NonNull final List<String> nameList) {
        Objects.requireNonNull(context, "context is null");

        if (nameList == null) {
            LogUtil.logError(TAG, "nameList is null");
            clearNameList(context);
        } else if (nameList.isEmpty()) {
            LogUtil.logDebug(TAG, "nameList is empty");
            clearNameList(context);
        } else {
            final GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
            final List<String> encNameList = new ArrayList<>();
            for (String name : nameList) {
                if (TextUtils.isEmpty(name)) {
                    LogUtil.logError(TAG, "name is null or empty");
                } else {
                    String encName = genericCipherUtil.encryptData(name);
                    encNameList.add(encName);
                }
            }
            LogUtil.logDebug(TAG, "put name to sharedPrefs, size=" + encNameList.size());
            final String encNameJson = JsonUtil.toJson(encNameList);
            SkrSharedPrefs.putSkrReconnectNamesFromJson(context, encNameJson);
        }
    }

    public static void clearNameList(@NonNull final Context context) {
        Objects.requireNonNull(context, "context is null");
        LogUtil.logDebug(TAG, "clear name list");
        SkrSharedPrefs.clearSkrReconnectNames(context);
    }
}
