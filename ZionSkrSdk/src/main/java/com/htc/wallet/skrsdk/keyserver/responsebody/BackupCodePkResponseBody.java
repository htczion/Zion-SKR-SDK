package com.htc.wallet.skrsdk.keyserver.responsebody;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;
import com.htc.wallet.skrsdk.util.LogUtil;

public class BackupCodePkResponseBody {
    private static final String TAG = "BackupCodePkResponseBody";

    @SerializedName("result")
    private Result mResult;

    @Nullable
    public String getEncCodePK() {
        if (mResult == null) {
            LogUtil.logError(TAG, "mResult is null", new IllegalStateException("mResult is null"));
            return null;
        }
        return mResult.mEncCodePK;
    }

    @Nullable
    public String getEncCodePKSigned() {
        if (mResult == null) {
            LogUtil.logError(TAG, "mResult is null", new IllegalStateException("mResult is null"));
            return null;
        }
        return mResult.mEncCodePKSigned;
    }

    @Nullable
    public String getEncAesKey() {
        if (mResult == null) {
            LogUtil.logError(TAG, "mResult is null", new IllegalStateException("mResult is null"));
            return null;
        }
        return mResult.mEncAesKey;
    }

    @Nullable
    public String getEncAesKeySigned() {
        if (mResult == null) {
            LogUtil.logError(TAG, "mResult is null", new IllegalStateException("mResult is null"));
            return null;
        }
        return mResult.mEncAesKeySigned;
    }

    private static class Result {
        @SerializedName("encCodePK")
        private String mEncCodePK;

        @SerializedName("encCodePKSigned")
        private String mEncCodePKSigned;

        @SerializedName("encAesKey")
        private String mEncAesKey;

        @SerializedName("encAesKeySigned")
        private String mEncAesKeySigned;
    }
}
