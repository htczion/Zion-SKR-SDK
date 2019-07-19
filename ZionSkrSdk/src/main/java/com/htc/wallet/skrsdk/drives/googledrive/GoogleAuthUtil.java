package com.htc.wallet.skrsdk.drives.googledrive;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.adapter.ApiKeyAdapter;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

public class GoogleAuthUtil {
    private static final String TAG = "GoogleAuthUtil";
    private static final String AUTH_TOKEN_TYPE =
            "oauth2:https://www.googleapis.com/auth/userinfo.profile "
                    + "https://www.googleapis.com/auth/userinfo.email "
                    + "https://www.googleapis.com/auth/drive "
                    + "https://www.googleapis.com/auth/drive.appdata";
    private final AppCompatActivity mActivity;
    private final String mApiKey;
    private final String mClientId;
    private final String mClientSecret;
    private OnTokenAcquiredListener mListener;

    public GoogleAuthUtil(@NonNull final AppCompatActivity activity) {
        mActivity = Objects.requireNonNull(activity);

        ApiKeyAdapter apiKeyAdapter = Objects.requireNonNull(
                ZionSkrSdkManager.getInstance().getApiKeyAdapter(), "apiKeyAdapter is null");

        mApiKey = apiKeyAdapter.getGoogleApiKey();
        mClientId = apiKeyAdapter.getGoogleClientId();
        mClientSecret = apiKeyAdapter.getGoogleClientSecret();
    }

    public void executeAccountAuth(Account account) {
        Bundle options = new Bundle();
        AccountManager.get(mActivity).getAuthToken(account, AUTH_TOKEN_TYPE, options, mActivity,
                new OnTokenAcquired(), null);
    }

    public void setOnTokenAcquiredListener(@NonNull final OnTokenAcquiredListener listener) {
        mListener = listener;
    }

    private class OnTokenAcquired implements AccountManagerCallback<Bundle> {
        @Override
        public void run(final AccountManagerFuture<Bundle> result) {
            if (anyEmpty(mApiKey, mClientId, mClientSecret)) {
                LogUtil.logError(TAG, "mApiKey, mClientId or mClientSecret is empty");
                if (mListener != null) {
                    mListener.OnTokenAcquiredFinished(false);
                }
                return;
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final Bundle bundle = result.getResult();
                        String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                        URL url = new URL("https://www.googleapis.com/tasks/v1/users/@me/lists?key="
                                + mApiKey);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.addRequestProperty("client_id", mClientId);
                        conn.addRequestProperty("client_secret", mClientSecret);
                        conn.setRequestProperty("Authorization", "OAuth " + token);
                        conn.connect();
                        Intent launch = (Intent) result.getResult().get(AccountManager.KEY_INTENT);
                        if (launch != null) {
                            LogUtil.logDebug(TAG, "OnTokenAcquired run, launch isn't null");
                            mActivity.startActivityForResult(launch, 0);
                            return;
                        }
                        if (mListener != null) {
                            mListener.OnTokenAcquiredFinished(true);
                        }
                    } catch (OperationCanceledException
                            | AuthenticatorException
                            | IOException e) {
                        LogUtil.logError(TAG, "OnTokenAcquired run, error = " + e);
                        if (mListener != null) {
                            mListener.OnTokenAcquiredFinished(false);
                        }
                    }
                }
            }).start();
        }
    }

    private boolean isSuccessCode(int code) {
        return 200 <= code && code <= 299;
    }

    private static boolean anyEmpty(String... keys) {
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            if (TextUtils.isEmpty(key)) {
                LogUtil.logError(TAG, "id=" + i + " is null or empty");
                return true;
            }
        }
        return false;
    }
}
