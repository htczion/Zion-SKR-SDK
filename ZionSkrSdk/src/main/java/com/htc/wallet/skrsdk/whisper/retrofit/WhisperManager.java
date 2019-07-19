package com.htc.wallet.skrsdk.whisper.retrofit;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.adapter.ApiKeyAdapter;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class WhisperManager {
    private static final String TAG = "WhisperManager";

    private static final String HOST_FOR_TESTING = "https://";
    private static final long TIMEOUT = 60;

    private static volatile WhisperManager sInstance = null;
    private WhisperApiService mApiService = null;

    private WhisperManager() {
        ApiKeyAdapter apiKeyAdapter = ZionSkrSdkManager.getInstance().getApiKeyAdapter();
        if (apiKeyAdapter == null) {
            LogUtil.logError(TAG, "apiKeyAdapter is null",
                    new IllegalStateException("apiKeyAdapter is null"));
            return;
        }
        // TODO Need to distinguish stage from production server
        final String apiKey = apiKeyAdapter.getWhisperApiKeyStage();
        if (TextUtils.isEmpty(apiKey)) {
            LogUtil.logError(TAG, "apiKey is empty", new IllegalStateException("apiKey is empty"));
            return;
        }

        final OkHttpClient okHttpClient =
                new OkHttpClient.Builder()
                        .addInterceptor(
                                new Interceptor() {
                                    @NonNull
                                    @Override
                                    public Response intercept(@NonNull Chain chain)
                                            throws IOException {
                                        Request newRequest =
                                                chain.request()
                                                        .newBuilder()
                                                        .addHeader(
                                                                "Content-Type", "application/json")
                                                        .addHeader("x-api-key", apiKey)
                                                        .build();
                                        return chain.proceed(newRequest);
                                    }
                                })
                        .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                        .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                        .build();

        Retrofit retrofit =
                new Retrofit.Builder()
                        .baseUrl(HOST_FOR_TESTING)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(okHttpClient)
                        .build();

        mApiService = retrofit.create(WhisperApiService.class);
    }

    public static WhisperManager getInstance() {
        if (sInstance == null) {
            synchronized (WhisperManager.class) {
                if (sInstance == null) {
                    sInstance = new WhisperManager();
                }
            }
        }
        return sInstance;
    }

    public WhisperApiService getApiService() {
        return mApiService;
    }
}
