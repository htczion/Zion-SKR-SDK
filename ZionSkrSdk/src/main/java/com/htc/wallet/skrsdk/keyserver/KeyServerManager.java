package com.htc.wallet.skrsdk.keyserver;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.adapter.ApiKeyAdapter;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class KeyServerManager {
    public static final int TOKEN_INVALID_OR_EXPIRED_MAX_RETRY_TIMES = 1;

    private static final String TAG = "KeyServerManager";
    // Stage Server, only used in dashboard or the ROM with version x.xx.9999.x
    private static final String HOST_NAME_FOR_TESTING = "https://";
    private static final String HOST_NAME_FOR_PRODUCTION = "https://auth.htcexodus.com/";
    private static volatile KeyServerManager sInstance = null;
    private static volatile boolean sIsTest = false;
    private final KeyServerApiService mKeyServerApiService;

    private KeyServerManager(boolean isTest) {
        final OkHttpClient okHttpClient =
                new OkHttpClient.Builder()
                        .readTimeout(60, TimeUnit.SECONDS)
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .build();

        String hostName;
        hostName = HOST_NAME_FOR_PRODUCTION;

        sIsTest = isTest;

        if (sIsTest) {
            LogUtil.logDebug(TAG, "The current flow is for testing, use the testing URL");
            hostName = HOST_NAME_FOR_TESTING;
        }

        final Retrofit retrofit =
                new Retrofit.Builder()
                        .baseUrl(hostName)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(okHttpClient)
                        .build();
        mKeyServerApiService = retrofit.create(KeyServerApiService.class);
    }

    public static KeyServerManager getInstance(boolean isTest) {
        synchronized (KeyServerManager.class) {
            if (sIsTest != isTest) {
                sInstance = null;
            }
            if (sInstance == null) {
                sInstance = new KeyServerManager(isTest);
            }
        }
        return sInstance;
    }

    public String getTzApiKey(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");

        String tzApiKey;

        ApiKeyAdapter apiKeyAdapter = ZionSkrSdkManager.getInstance().getApiKeyAdapter();
        if (apiKeyAdapter == null) {
            throw new IllegalStateException("apiKeyAdapter is null");
        }

        tzApiKey = apiKeyAdapter.getTZApiKeyProduction();

        if (sIsTest) {
            LogUtil.logInfo(TAG, "The current flow is for testing, use the stage TZ api key");
            tzApiKey = apiKeyAdapter.getTZApiKeyStage();
        }

        if (TextUtils.isEmpty(tzApiKey)) {
            LogUtil.logError(
                    TAG,
                    "tzApiKey is null or empty",
                    new IllegalStateException("tzApiKey is null or empty"));
        }

        return tzApiKey;
    }

    public KeyServerApiService getKeyServerApiService() {
        return mKeyServerApiService;
    }
}
