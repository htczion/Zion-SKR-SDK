package com.htc.wallet.skrsdk.keyserver.responsebody;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;
import com.htc.wallet.skrsdk.util.LogUtil;

public class RestoreSeedResponseBody {
    private static final String TAG = "RestoreSeedResponseBody";

    @SerializedName("result")
    private Result mResult;

    @Nullable
    public String getEncSeed() {
        if (mResult == null) {
            LogUtil.logError(TAG, "mResult is null", new IllegalStateException("mResult is null"));
            return null;
        }
        return mResult.mEncSeed;
    }

    public String getEncSeedSigned() {
        if (mResult == null) {
            LogUtil.logError(TAG, "mResult is null", new IllegalStateException("mResult is null"));
            return null;
        }
        return mResult.mEncSeedSigned;
    }

    private static class Result {
        @SerializedName("encSeed")
        private String mEncSeed;

        @SerializedName("encSeedSigned")
        private String mEncSeedSigned;
    }
}
