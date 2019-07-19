package com.htc.wallet.skrsdk.keyserver.requestbody;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.Objects;

public class AttestationsRequestBody {
    private static final String TAG = "AttestationsRequestBody";

    @SerializedName("params")
    private final Params mParams = new Params();

    @SerializedName("tzApiKey")
    private String mTzApiKey;

    public AttestationsRequestBody(
            @NonNull Context context, @NonNull String tzApiKey, @NonNull String signedAttestation) {
        Objects.requireNonNull(context, "context is null");

        if (TextUtils.isEmpty(tzApiKey)) {
            LogUtil.logError(
                    TAG,
                    "mTzApiKey is null or empty",
                    new IllegalStateException("mTzApiKey is null or empty"));
            return;
        }

        if (TextUtils.isEmpty(signedAttestation)) {
            LogUtil.logError(
                    TAG,
                    "signedAttestation is null or empty",
                    new IllegalStateException("signedAttestation is null or empty"));
            return;
        }

        mTzApiKey = tzApiKey;
        mParams.setSignedAttestation(signedAttestation);
    }

    private static class Params {
        @SerializedName("signedAttestation")
        private String mSignedAttestation;

        public void setSignedAttestation(String signedAttestation) {
            mSignedAttestation = signedAttestation;
        }
    }
}
