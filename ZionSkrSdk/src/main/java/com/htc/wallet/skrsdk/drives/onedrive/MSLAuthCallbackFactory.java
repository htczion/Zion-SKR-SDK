package com.htc.wallet.skrsdk.drives.onedrive;

import android.app.Activity;

import com.htc.wallet.skrsdk.util.LogUtil;
import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.MsalClientException;
import com.microsoft.identity.client.MsalException;
import com.microsoft.identity.client.MsalServiceException;
import com.microsoft.identity.client.MsalUiRequiredException;

public class MSLAuthCallbackFactory {
    private static final String TAG = "MSLAuthCallbackFactory";

    private MSLAuthCallbackFactory() {
        throw new AssertionError();
    }

    public static MSALAuthenticationCallback getMSALAuthenticationCallback(
            final AuthResultCallback callback,
            final MSLResultCallback resultCallback,
            final Activity activity) {
        return new MSALAuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                if (callback != null) {
                    callback.getAuthResult(authenticationResult, resultCallback);
                }
            }

            @Override
            public void onError(MsalException e) {
                if (e instanceof MsalClientException) {
                    // This means errors happened in the sdk itself, could be network, Json parse,
                    // etc. Check MsalError.java
                    // for detailed list of the errors.
                    LogUtil.logError(TAG, "OneDrive auth, MsalException, error = " + e);
                } else if (e instanceof MsalServiceException) {
                    // This means something is wrong when the sdk is communication to the service,
                    // mostly likely it's the client
                    // configuration.
                    LogUtil.logError(TAG, "OneDrive auth, MsalServiceException, error = " + e);
                } else if (e instanceof MsalUiRequiredException) {
                    // This explicitly indicates that developer needs to prompt the user, it could
                    // be refresh token is expired, revoked
                    // or user changes the password or it could be that no token was found in the
                    // token cache.
                    LogUtil.logError(TAG, "OneDrive auth, MsalUiRequiredException, error = " + e);
                    AuthenticationManager mgr = AuthenticationManager.getInstance();
                    if (activity == null) {
                        LogUtil.logError(TAG, "callAcquireToken(), activity is null");
                        return;
                    }
                    mgr.callAcquireToken(activity, this);
                }
            }

            @Override
            public void onError(Exception e) {
                LogUtil.logError(TAG, "OneDrive auth,, Other exception, error = " + e);
            }

            @Override
            public void onCancel() {
                LogUtil.logInfo(TAG, "User cancelled the flow.");
                if (callback != null) {
                    callback.onCancel();
                }
            }
        };
    }

    public interface AuthResultCallback {
        void getAuthResult(AuthenticationResult result, MSLResultCallback callback);

        void onCancel();
    }

    public interface MSLResultCallback {
        void onCall();
    }
}
