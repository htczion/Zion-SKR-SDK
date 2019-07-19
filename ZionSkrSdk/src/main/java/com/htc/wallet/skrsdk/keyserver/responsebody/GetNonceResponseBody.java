package com.htc.wallet.skrsdk.keyserver.responsebody;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;
import com.htc.wallet.skrsdk.util.LogUtil;

public class GetNonceResponseBody {
    private static final String TAG = "GetNonceResponseBody";

    @SerializedName("result")
    private Result mResult;

    @Nullable
    public String getNonce() {
        if (mResult == null) {
            LogUtil.logError(TAG, "mResult is null", new IllegalStateException("mResult is null"));
            return null;
        }
        return mResult.mNonce;
    }

    private static class Result {
        @SerializedName("nonce")
        private String mNonce;
    }
}
