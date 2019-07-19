package com.htc.wallet.skrsdk.drives.onedrive;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;

import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.MsalException;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.User;

import java.io.IOException;

public class AuthenticationManager {
    private static final String TAG = "AuthenticationManager";
    private static AuthenticationManager sInstance;
    private static PublicClientApplication sPublicClientApplication;
    private AuthenticationResult mAuthResult;
    private MSALAuthenticationCallback mActivityCallback;

    private AuthenticationManager() {
    }

    // Context must be applicationContext
    // {@link PublicClientApplication(@NonNull final Context context)}
    public static AuthenticationManager getInstance() {
        if (sInstance == null) {
            synchronized (AuthenticationManager.class) {
                sInstance = new AuthenticationManager();
                if (sPublicClientApplication == null) {
                    Context context = ZionSkrSdkManager.getInstance().getAppContext();
                    if (context == null) {
                        throw new IllegalStateException("context is null");
                    }
                    sPublicClientApplication = new PublicClientApplication(context);
                }
            }
        }
        return sInstance;
    }

    private static synchronized void resetInstance() {
        sInstance = null;
    }

    public String getAccessToken()
            throws AuthenticatorException, IOException, OperationCanceledException {
        if (mAuthResult != null) {
            return mAuthResult.getAccessToken();
        }
        return "";
    }

    public PublicClientApplication getPublicClient() {
        return sPublicClientApplication;
    }

    public void disconnect() {
        if (mAuthResult != null) {
            sPublicClientApplication.remove(mAuthResult.getUser());
        }
        AuthenticationManager.resetInstance();
    }

    public void callAcquireToken(
            final Activity activity, final MSALAuthenticationCallback authenticationCallback) {
        if (activity == null) {
            LogUtil.logError(TAG, "callAcquireToken(), activity is null");
            return;
        }
        if (authenticationCallback == null) {
            LogUtil.logError(TAG, "callAcquireToken(), authenticationCallback is null");
            return;
        }
        mActivityCallback = authenticationCallback;
        sPublicClientApplication.acquireToken(activity, Constants.SCOPES, getAuthCallback());
    }

    public void callAcquireTokenSilent(
            final User user,
            boolean forceRefresh,
            @NonNull final MSALAuthenticationCallback msalAuthenticationCallback) {
        if (user == null) {
            LogUtil.logError(TAG, "callAcquireTokenSilent(), user is null");
            return;
        }
        if (msalAuthenticationCallback == null) {
            LogUtil.logError(TAG, "callAcquireTokenSilent(), msalAuthenticationCallback is null");
            return;
        }
        mActivityCallback = msalAuthenticationCallback;
        sPublicClientApplication.acquireTokenSilentAsync(
                Constants.SCOPES, user, null, forceRefresh, getAuthCallback());
    }

    private AuthenticationCallback getAuthCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                LogUtil.logInfo(TAG, "Successfully authenticated");
                LogUtil.logDebug(TAG, "ID Token: " + authenticationResult.getIdToken());

                // Store the authResult
                mAuthResult = authenticationResult;
                if (mActivityCallback != null) {
                    mActivityCallback.onSuccess(mAuthResult);
                }
            }

            @Override
            public void onError(MsalException e) {
                LogUtil.logError(TAG, "Authentication failed: " + e);
                if (mActivityCallback != null) {
                    mActivityCallback.onError(e);
                }
            }

            @Override
            public void onCancel() {
                LogUtil.logInfo(TAG, "User cancelled login.");
                if (mActivityCallback != null) {
                    mActivityCallback.onCancel();
                }
            }
        };
    }
}
