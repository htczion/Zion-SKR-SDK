package com.htc.wallet.skrsdk.keyserver;

import android.support.annotation.NonNull;

import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public abstract class KeyServerCallbackWithRetry<T> implements Callback<T> {
    private static final String TAG = "KeyServerCallbackWithRetry";

    public static final int TOTAL_RETRIES = 3;

    private final Call<T> mCall;
    private final boolean mShouldRetry;
    private int mRetryCount = 0;

    public KeyServerCallbackWithRetry(Call<T> call) {
        Objects.requireNonNull(call, "call is null!");
        this.mCall = call;
        this.mShouldRetry = true;
    }

    public KeyServerCallbackWithRetry(Call<T> call, boolean shouldRetry) {
        Objects.requireNonNull(call, "call is null!");
        this.mCall = call;
        this.mShouldRetry = shouldRetry;
    }

    @Override
    public void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
        if (!response.isSuccessful()) {
            LogUtil.logWarning(TAG, "Response is failed, response code=" + response.code());
        }
    }

    @Override
    public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
        LogUtil.logWarning(TAG, t.getLocalizedMessage());
        retry();
    }

    public KeyServerCallbackWithRetry<T> getSelf() {
        return this;
    }

    private void retry() {
        if (mCall == null) {
            LogUtil.logError(
                    TAG,
                    "mCall is null, failed to retry!",
                    new IllegalStateException("mCall is null, failed to retry!"));
            return;
        }

        if (mShouldRetry && mRetryCount++ < TOTAL_RETRIES) {
            LogUtil.logVerbose(
                    TAG, "Retrying... (" + mRetryCount + " out of " + TOTAL_RETRIES + ")");
            mCall.clone().enqueue(this);
        }
    }
}
