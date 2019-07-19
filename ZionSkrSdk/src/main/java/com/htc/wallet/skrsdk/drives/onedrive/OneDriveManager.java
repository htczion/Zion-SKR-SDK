package com.htc.wallet.skrsdk.drives.onedrive;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.support.annotation.NonNull;

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

public class OneDriveManager {
    private static final String TAG = "OneDriveManager";
    private static final String HOST_MICROSOFT_GRAPH = "https://graph.microsoft.com/v1.0/me/drive/";
    private static final long TIMEOUT = 60;

    private static volatile OneDriveManager sInstance = null;
    private final OneDriveApiService mApiService;

    private OneDriveManager() {
        final OkHttpClient.Builder httpClient =
                new OkHttpClient.Builder()
                        .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                        .connectTimeout(TIMEOUT, TimeUnit.SECONDS);

        final Interceptor interceptor =
                new Interceptor() {
                    @NonNull
                    @Override
                    public Response intercept(@NonNull Chain chain) throws IOException {
                        Request original = chain.request();
                        Request request = null;
                        try {
                            request =
                                    original.newBuilder()
                                            .header(
                                                    "Authorization",
                                                    "Bearer "
                                                            + AuthenticationManager.getInstance()
                                                            .getAccessToken())
                                            .method(original.method(), original.body())
                                            .build();
                        } catch (AuthenticatorException | OperationCanceledException e) {
                            LogUtil.logError(TAG, "intercept(), error = " + e);
                        }
                        return chain.proceed(request);
                    }
                };

        httpClient.addInterceptor(interceptor);
        OkHttpClient client = httpClient.build();
        Retrofit retrofit =
                new Retrofit.Builder()
                        .baseUrl(HOST_MICROSOFT_GRAPH)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(client)
                        .build();

        mApiService = retrofit.create(OneDriveApiService.class);
    }

    public static OneDriveManager getInstance() {
        if (sInstance == null) {
            synchronized (OneDriveManager.class) {
                if (sInstance == null) {
                    sInstance = new OneDriveManager();
                }
            }
        }
        return sInstance;
    }

    public OneDriveApiService getApiService() {
        return mApiService;
    }
}
