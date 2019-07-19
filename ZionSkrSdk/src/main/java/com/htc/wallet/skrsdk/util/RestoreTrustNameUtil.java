package com.htc.wallet.skrsdk.util;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Objects;

public class RestoreTrustNameUtil {
    private static final String TAG = "RestoreTrustNameUtil";
    private static final String EMPTY_STRING = "";

    private RestoreTrustNameUtil() {
    }

    public static void put(@NonNull Context context, @NonNull ArrayList<String> nameList) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(nameList, "nameList is null");
        Gson gson = new Gson();
        String json = gson.toJson(nameList);
        SkrSharedPrefs.putRestoreTrustNames(context, json);
    }

    @NonNull
    public static ArrayList<String> get(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<String>>() {
        }.getType();
        String json = SkrSharedPrefs.getRestoreTrustNames(context);
        ArrayList<String> nameList = gson.fromJson(json, listType);
        if (nameList == null) {
            nameList = new ArrayList<>();
        }
        return nameList;
    }

    public static void removeAll(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        SkrSharedPrefs.putRestoreTrustNames(context, EMPTY_STRING);
    }
}
