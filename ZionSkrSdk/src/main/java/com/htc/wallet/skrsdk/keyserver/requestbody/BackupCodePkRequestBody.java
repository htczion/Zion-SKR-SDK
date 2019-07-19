package com.htc.wallet.skrsdk.keyserver.requestbody;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.Objects;

public class BackupCodePkRequestBody {
    private static final String TAG = "BackupCodePkRequestBody";

    @SerializedName("params")
    private final Params mParams = new Params();

    @SerializedName("tzApiKey")
    private String mTzApiKey;

    public BackupCodePkRequestBody(
            @NonNull Context context,
            @NonNull String tzApiKey,
            @NonNull String secretIdHash,
            @NonNull String verifyCode,
            @NonNull String pubKey) {
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
                    new IllegalStateException("secretIdHash is null or empty"));
            return;
        }
        if (TextUtils.isEmpty(verifyCode)) {
            LogUtil.logError(
                    TAG,
                    "verifyCode is null or empty",
                    new IllegalStateException("verifyCode is null or empty"));
            return;
        }
        if (TextUtils.isEmpty(pubKey)) {
            LogUtil.logError(
                    TAG,
                    "tzApiKey is null or empty",
                    new IllegalStateException("pubKey is null or empty"));
            return;
        }

        mTzApiKey = tzApiKey;
        mParams.setSecretIdHash(secretIdHash);
        mParams.setCodePubKey(verifyCode + ":" + pubKey);
    }

    private static class Params {
        @SerializedName("secretIdHash")
        private String mSecretIdHash;

        @SerializedName("codePubKey")
        private String mCodePubKey;

        public void setSecretIdHash(String secretIdHash) {
            mSecretIdHash = secretIdHash;
        }

        public void setCodePubKey(String codePubKey) {
            mCodePubKey = codePubKey;
        }
    }
}
