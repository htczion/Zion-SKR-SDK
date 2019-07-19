package com.htc.wallet.skrsdk.whisper.retrofit.jsonbody;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ShhRequestBody {
    private static final String TAG = "ShhRequestBody";

    private static final String JSON_RPC_VER = "2.0";
    private static final int ID = 1;
    private static final String EMPTY_STRING = "";

    @SerializedName("jsonrpc")
    private String mJsonRpc;

    @SerializedName("id")
    private int mId;

    @SerializedName("method")
    private String mMethod;

    @SerializedName("params")
    private final List<Object> mParams = new ArrayList<>();

    public ShhRequestBody(@NonNull String method, @NonNull Object param) {
        Objects.requireNonNull(param);
        if (TextUtils.isEmpty(method)) {
            LogUtil.logError(
                    TAG, "method is empty", new IllegalArgumentException("method is empty"));
            return;
        }
        mJsonRpc = JSON_RPC_VER;
        mId = ID;
        mMethod = method;
        // For the json format: "params": [...]
        mParams.add(param);
    }

    public ShhRequestBody(@NonNull String method) {
        if (TextUtils.isEmpty(method)) {
            LogUtil.logError(
                    TAG, "method is empty", new IllegalArgumentException("method is empty"));
            return;
        }
        mJsonRpc = JSON_RPC_VER;
        mId = ID;
        mMethod = method;
        mParams.add(EMPTY_STRING);
    }
}
