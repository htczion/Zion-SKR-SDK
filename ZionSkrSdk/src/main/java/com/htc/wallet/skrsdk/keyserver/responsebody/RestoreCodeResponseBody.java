package com.htc.wallet.skrsdk.keyserver.responsebody;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;
import com.htc.wallet.skrsdk.util.LogUtil;

public class RestoreCodeResponseBody {
    private static final String TAG = "RestoreCodeResponseBody";

    @SerializedName("result")
    private Result mResult;

    @Nullable
    public String getCode() {
        if (mResult == null) {
            LogUtil.logError(TAG, "mResult is null", new IllegalStateException("mResult is null"));
            return null;
        }
        return mResult.mCode;
    }

    private static class Result {
        @SerializedName("code")
        private String mCode;
    }
}
