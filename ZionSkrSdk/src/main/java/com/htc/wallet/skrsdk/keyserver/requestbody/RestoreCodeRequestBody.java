package com.htc.wallet.skrsdk.keyserver.requestbody;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.Objects;

public class RestoreCodeRequestBody {
    private static final String TAG = "RestoreCodeRequestBody";

    @SerializedName("params")
    private final Params mParams = new Params();

    @SerializedName("tzApiKey")
    private String mTzApiKey;

    public RestoreCodeRequestBody(
            @NonNull Context context,
            @NonNull String tzApiKey,
            @NonNull String encCode,
            @NonNull String encCodeSigned,
            @NonNull String secretIdHash) {
        Objects.requireNonNull(context, "context is null");

        if (TextUtils.isEmpty(tzApiKey)) {
            LogUtil.logError(
                    TAG,
                    "mTzApiKey is null or empty",
                    new IllegalStateException("mTzApiKey is null or empty"));
            return;
        }
        if (TextUtils.isEmpty(encCode)) {
            LogUtil.logError(
                    TAG,
                    "encCode is null or empty",
                    new IllegalArgumentException("encCode is null or empty"));
            return;
        }
        if (TextUtils.isEmpty(encCodeSigned)) {
            LogUtil.logError(
                    TAG,
                    "encCodeSigned is null or empty",
                    new IllegalArgumentException("encCodeSigned is null or empty"));
            return;
        }
        if (TextUtils.isEmpty(secretIdHash)) {
            LogUtil.logError(
                    TAG,
                    "secretIdHash is null or empty",
                    new IllegalArgumentException("secretIdHash is null or empty"));
            return;
        }

        mTzApiKey = tzApiKey;
        mParams.setEncCode(encCode);
        mParams.setEncCodeSigned(encCodeSigned);
        mParams.setSecretIdHash(secretIdHash);
    }

    private static class Params {
        @SerializedName("encCode")
        private String mEncCode;

        @SerializedName("encCodeSigned")
        private String mEncCodeSigned;

        @SerializedName("secretIdHash")
        private String mSecretIdHash;

        public void setEncCode(String encCode) {
            mEncCode = encCode;
        }

        public void setEncCodeSigned(String encCodeSigned) {
            mEncCodeSigned = encCodeSigned;
        }

        public void setSecretIdHash(String secretIdHash) {
            mSecretIdHash = secretIdHash;
        }
    }
}
