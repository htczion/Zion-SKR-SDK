package com.htc.wallet.skrsdk.legacy.v1;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.ArrayMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LegacyV1Util {

    private static final String TAG = "LegacyV1Util";

    private static final Object LOCK = new Object();

    @WorkerThread
    public static Map<String, LegacyBackupDataV1> getLegacyV1Map(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");

        // Move LegacySKRListV1 to LegacySkrV1 (only for user trial rom)
        List<String> legacyUuidHashV1List = SkrSharedPrefs.getLegacySKRListV1(context);
        if (legacyUuidHashV1List.size() > 0) {
            synchronized (LOCK) {
                legacyUuidHashV1List = SkrSharedPrefs.getLegacySKRListV1(context);
                if (legacyUuidHashV1List.size() > 0) {
                    LogUtil.logDebug(TAG, "Move LegacySKRListV1 to LegacySkrV1");
                    // clear deprecated sharedPrefs
                    SkrSharedPrefs.clearLegacySKRListV1(context);
                    // move to new sharedPrefs
                    Map<String, LegacyBackupDataV1> map = new ArrayMap<>();
                    for (String uuidHash : legacyUuidHashV1List) {
                        LegacyBackupDataV1 legacyBackupDataV1 = new LegacyBackupDataV1();
                        legacyBackupDataV1.setUuidHash(uuidHash);
                        // In this case without publicKey
                        map.put(uuidHash, legacyBackupDataV1);
                    }
                    putLegacyV1Map(context, map);
                }
            }
        }

        String json = SkrSharedPrefs.getLegacySkrV1(context);
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, LegacyBackupDataV1>>() {
        }.getType();
        Map<String, LegacyBackupDataV1> legacyV1Map = gson.fromJson(json, type);

        if (legacyV1Map == null) {
            LogUtil.logDebug(TAG, "legacyV1Map is null");
            return new ArrayMap<>();
        }

        for (String uuidHash : legacyV1Map.keySet()) {
            legacyV1Map.get(uuidHash).decrypt();
        }

        return legacyV1Map;
    }

    @WorkerThread
    public static void putLegacyV1Map(
            @NonNull Context context, @NonNull final Map<String, LegacyBackupDataV1> legacyV1Map) {
        Objects.requireNonNull(context, "context is null");
        if (legacyV1Map == null) {
            LogUtil.logError(TAG, "legacyV1Map is null", new IllegalArgumentException());
            return;
        }

        ArrayMap<String, LegacyBackupDataV1> cloneMap = new ArrayMap<>();

        for (String uuidHash : legacyV1Map.keySet()) {
            LegacyBackupDataV1 legacyBackupDataV1 = legacyV1Map.get(uuidHash);
            if (legacyBackupDataV1 != null) {
                LegacyBackupDataV1 cloneLegacyBackupDataV1 =
                        new LegacyBackupDataV1(legacyBackupDataV1);
                cloneLegacyBackupDataV1.encrypt();
                cloneMap.put(uuidHash, cloneLegacyBackupDataV1);
            }
        }

        Gson gson = new Gson();
        String json = gson.toJson(cloneMap);

        SkrSharedPrefs.putLegacySkrV1(context, json);
    }
}
