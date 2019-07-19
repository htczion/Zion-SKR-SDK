package com.htc.wallet.skrsdk.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class JsonUtil {
    public static final String TAG = "JsonUtil";

    private JsonUtil() {

    }

    public static String toJson(Object object) {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(object);
    }

    // TODO: Change to use toJson() ?
    public static String mapToJson(@NonNull Map<String, String> map) {
        Objects.requireNonNull(map, "map is null");

        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(map);
    }

    @Nullable
    public static Map<String, String> jsonToMap(String json) {
        if (TextUtils.isEmpty(json)) {
            LogUtil.logError(
                    TAG,
                    "json string is empty",
                    new IllegalArgumentException("json string is empty"));
            return null;
        }

        Gson gson = new GsonBuilder().serializeNulls().create();
        Type mapType = new TypeToken<Map<String, String>>() {
        }.getType();
        return gson.fromJson(json, mapType);
    }

    @Nullable
    public static List<String> jsonToList(String json) {
        if (TextUtils.isEmpty(json)) {
            LogUtil.logError(
                    TAG,
                    "json string is empty",
                    new IllegalArgumentException("json string is empty"));
            return null;
        }

        Gson gson = new GsonBuilder().serializeNulls().create();
        Type mapType = new TypeToken<List<String>>() {
        }.getType();
        return gson.fromJson(json, mapType);
    }

    @Nullable
    public static <T> T jsonToObject(String json, Class<T> tClass) {
        if (TextUtils.isEmpty(json)) {
            LogUtil.logError(TAG, "json string is empty",
                    new IllegalArgumentException("json string is empty"));
            return null;
        }

        Gson gson = new GsonBuilder().serializeNulls().create();

        try {
            return gson.fromJson(json, tClass);
        } catch (JsonSyntaxException e) {
            LogUtil.logError(TAG, "jsonToObject(), JsonSyntaxException e=" + e);
            return null;
        }
    }
}
