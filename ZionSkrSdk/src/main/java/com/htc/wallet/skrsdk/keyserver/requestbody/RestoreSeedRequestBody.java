package com.htc.wallet.skrsdk.keyserver.requestbody;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.Objects;

public class RestoreSeedRequestBody {
    private static final String TAG = "RestoreSeedRequestBody";

    @SerializedName("params")
    private final Params mParams = new Params();

    @SerializedName("tzApiKey")
    private String mTzApiKey;

    public RestoreSeedRequestBody(
            @NonNull Context context,
            @NonNull String tzApiKey,
            @NonNull String secretIdHash,
            @NonNull String seed) {
        Objects.requireNonNull(context, "context is null");

        if (TextUtils.isEmpty(tzApiKey)) {
            LogUtil.logError(
                    TAG,
                    "mTzApiKey is null or empty",
                    new IllegalStateException("mTzApiKey is null or empty"));
            return;
        }
        if (TextUtils.isEmpty(secretIdHash)) {
            LogUtil.logError(
                    TAG,
                    "secretIdHash is null or empty",
                    new IllegalArgumentException("secretIdHash is null or empty"));
            return;
        }
        if (TextUtils.isEmpty(seed)) {
            LogUtil.logError(
                    TAG,
                    "seed is null or empty",
                    new IllegalArgumentException("seed is null or empty"));
            return;
        }

        mTzApiKey = tzApiKey;
        mParams.setSecretIdHash(secretIdHash);
        mParams.setSeed(seed);
    }

    private static class Params {
        @SerializedName("secretIdHash")
        private String mSecretIdHash;

        @SerializedName("seed")
        private String mSeed;

        public void setSecretIdHash(String secretIdHash) {
            this.mSecretIdHash = secretIdHash;
        }

        public void setSeed(String seed) {
            this.mSeed = seed;
        }
    }
}
