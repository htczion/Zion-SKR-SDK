package com.htc.wallet.skrsdk.keyserver.requestbody;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.Objects;

public class GetNonceRequestBody {
    private static final String TAG = "GetNonceRequestBody";

    @SerializedName("tzApiKey")
    private String mTzApiKey;

    public GetNonceRequestBody(@NonNull Context context, @NonNull String tzApiKey) {
        Objects.requireNonNull(context, "context is null");

        if (TextUtils.isEmpty(tzApiKey)) {
            LogUtil.logError(
                    TAG,
                    "mTzApiKey is null or empty",
                    new IllegalStateException("mTzApiKey is null or empty"));
            return;
        }

        mTzApiKey = tzApiKey;
    }
}
