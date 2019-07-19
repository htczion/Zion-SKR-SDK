package com.htc.wallet.skrsdk.keyserver.responsebody;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;
import com.htc.wallet.skrsdk.util.LogUtil;

public class AttestationsResponseBody {

    private static final String TAG = "AttestationsResponseBody";

    @SerializedName("result")
    private Result mResult;

    @Nullable
    public String getToken() {
        if (mResult == null) {
            LogUtil.logError(TAG, "mResult is null", new IllegalStateException("mResult is null"));
            return null;
        }
        return mResult.mToken;
    }

    private static class Result {
        @SerializedName("token")
        private String mToken;
    }
}
