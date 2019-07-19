package com.htc.wallet.skrsdk.drives.onedrive;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.support.annotation.NonNull;

import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.core.DefaultClientConfig;
import com.microsoft.graph.core.IClientConfig;
import com.microsoft.graph.extensions.GraphServiceClient;
import com.microsoft.graph.extensions.IGraphServiceClient;
import com.microsoft.graph.http.IHttpRequest;

import java.io.IOException;

public class GraphServiceClientManager implements IAuthenticationProvider {
    private static final String TAG = "GraphServiceClientManager";
    private static GraphServiceClientManager sInstance;
    private IGraphServiceClient mGraphServiceClient;

    private GraphServiceClientManager() {
    }

    public static GraphServiceClientManager getInstance() {
        if (sInstance == null) {
            synchronized (GraphServiceClientManager.class) {
                if (sInstance == null) {
                    sInstance = new GraphServiceClientManager();
                }
            }
        }
        return sInstance;
    }

    @Override
    public void authenticateRequest(@NonNull final IHttpRequest request) {
        if (request == null) {
            LogUtil.logError(TAG, "authenticateRequest(), request is null");
            return;
        }
        try {
            Context context = ZionSkrSdkManager.getInstance().getAppContext();
            if (context == null) {
                LogUtil.logError(TAG, "authenticateRequest(), context is null");
                return;
            }
            request.addHeader(
                    "Authorization",
                    "Bearer " + AuthenticationManager.getInstance().getAccessToken());

            LogUtil.logDebug(TAG, "authenticateRequest(), request: " + request.toString());
        } catch (AuthenticatorException
                | NullPointerException
                | OperationCanceledException
                | IOException e) {
            LogUtil.logError(TAG, "authenticateRequest(), error: " + e);
        }
    }

    public synchronized IGraphServiceClient getGraphServiceClient() {
        return getGraphServiceClient(this);
    }

    public synchronized IGraphServiceClient getGraphServiceClient(
            IAuthenticationProvider authenticationProvider) {
        if (mGraphServiceClient == null) {
            IClientConfig clientConfig =
                    DefaultClientConfig.createWithAuthenticationProvider(authenticationProvider);
            mGraphServiceClient =
                    new GraphServiceClient.Builder().fromConfig(clientConfig).buildClient();
        }

        return mGraphServiceClient;
    }
}
