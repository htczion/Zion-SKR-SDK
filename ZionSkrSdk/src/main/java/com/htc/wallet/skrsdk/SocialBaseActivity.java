package com.htc.wallet.skrsdk;

import static com.htc.wallet.skrsdk.tools.security.attestation.SafetyNetWrapper.KEY_ERROR_CODE_GOOGLE_PLAY_SERVICE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.internal.ConnectionErrorMessages;
import com.htc.wallet.skrsdk.adapter.ActivityStateAdapter;
import com.htc.wallet.skrsdk.adapter.HomeAuthenticatorAdapter;
import com.htc.wallet.skrsdk.applink.NetworkUtil;
import com.htc.wallet.skrsdk.tools.security.attestation.SafetyNetWrapper;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.WalletSdkUtil;

import java.util.Objects;

public class SocialBaseActivity extends AppCompatActivity {
    private static final String TAG = "SocialBaseActivity";

    private static int DEFAULT_VALUE = -1;
    private BroadcastReceiver mBroadcastReceiver;
    private View mLoadingView;
    private volatile AlertDialog mConnectivityDialog;
    private volatile AlertDialog mGooglePlayServicesNotAvailableDialog;
    private volatile AlertDialog mAttestFailedAlertDialog;
    private boolean mShouldAuth = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!ZionSkrSdkManager.getInstance().getIsDebug()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (Settings.canDrawOverlays(this)) {
                    getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                } else {
                    LogUtil.logDebug(TAG, "Cannot set application overlay!");
                }
            }
        }
        checkNetworkConnectivity(this);
        setStatusBarTransparent();
    }

    @Override
    protected void onResume() {
        super.onResume();

        authenticate();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(SafetyNetWrapper.ACTION_ATTEST_FAILED);
        intentFilter.addAction(SafetyNetWrapper.ACTION_GOOGLE_PLAY_SERVICE_NOT_AVAILABLE);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (context == null) {
                    LogUtil.logError(TAG, "context is null");
                    return;
                }
                if (intent == null) {
                    LogUtil.logError(TAG, "intent is null");
                    return;
                }
                String action = intent.getAction();
                if (TextUtils.isEmpty(action)) {
                    LogUtil.logError(TAG, "action is empty");
                    return;
                }

                switch (action) {
                    case ConnectivityManager.CONNECTIVITY_ACTION:
                        LogUtil.logDebug(TAG, "Network connectivity changed");
                        checkNetworkConnectivity(context);
                        break;
                    case SafetyNetWrapper.ACTION_ATTEST_FAILED:
                        LogUtil.logError(TAG, "Attest failed");
                        showAttestFailedAlertDialog(true);
                        break;
                    case SafetyNetWrapper.ACTION_GOOGLE_PLAY_SERVICE_NOT_AVAILABLE:
                        LogUtil.logError(TAG, "Google play service not available");
                        int errorCode = intent.getIntExtra(KEY_ERROR_CODE_GOOGLE_PLAY_SERVICE,
                                DEFAULT_VALUE);
                        if (errorCode != DEFAULT_VALUE) {
                            showGooglePlayServiceNotAvailableDialog(true, errorCode);
                        } else {
                            LogUtil.logError(TAG, "incorrect errorCode");
                        }
                        break;
                    default:
                        LogUtil.logError(TAG, "Unknown action=" + action);
                        break;
                }
            }
        };
        registerReceiver(mBroadcastReceiver, intentFilter);

        // Ensure Devices Have the Google Play services APK
        // https://developers.google.com/android/guides/setup
        int errorCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (errorCode != ConnectionResult.SUCCESS) {
            LogUtil.logWarning(TAG, "Google play services not available, errorCode=" + errorCode);
            showGooglePlayServiceNotAvailableDialog(true, errorCode);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (mAttestFailedAlertDialog != null && mAttestFailedAlertDialog.isShowing()) {
            mAttestFailedAlertDialog.dismiss();
        }

        if (mGooglePlayServicesNotAvailableDialog != null
                && mGooglePlayServicesNotAvailableDialog.isShowing()) {
            mGooglePlayServicesNotAvailableDialog.dismiss();
        }

        if (mConnectivityDialog != null && mConnectivityDialog.isShowing()) {
            mConnectivityDialog.dismiss();
        }

        super.onDestroy();
    }

    public void onAuthSuccess() {

    }

    public void authenticate() {
        ActivityStateAdapter activityStateAdapter =
                ZionSkrSdkManager.getInstance().getActivityStateAdapter();

        if (mShouldAuth
                && activityStateAdapter != null && !activityStateAdapter.getIsAuthPassed()
                && WalletSdkUtil.isSeedExists(this)) {

            Intent homeAuth = createHomeAuthIntent();
            if (homeAuth == null) {
                LogUtil.logDebug(TAG, "homeAuth is null");
                onAuthSuccess();
            } else {
                startActivity(homeAuth);
            }
        } else {
            onAuthSuccess();
        }
    }

    private void showAttestFailedAlertDialog(final boolean finishSelf) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mAttestFailedAlertDialog == null) {
                    mAttestFailedAlertDialog = new AlertDialog.Builder(SocialBaseActivity.this)
                            .setTitle(R.string.exchange_safety_net_null_token_title)
                            .setMessage(R.string.exchange_safety_net_null_token)
                            .setPositiveButton(R.string.db_btn_confirm,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            if (finishSelf) {
                                                finish();
                                            }
                                        }
                                    }).create();
                }
                if (!isFinishing() && !mAttestFailedAlertDialog.isShowing()) {
                    mAttestFailedAlertDialog.setCanceledOnTouchOutside(false);
                    mAttestFailedAlertDialog.show();
                }
            }
        });
    }

    private void setStatusBarTransparent() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    @Nullable
    private Intent createHomeAuthIntent() {
        HomeAuthenticatorAdapter homeAuthenticatorAdapter =
                ZionSkrSdkManager.getInstance().getHomeAuthenticatorAdapter();
        if (homeAuthenticatorAdapter == null) {
            LogUtil.logInfo(TAG, "HomeAuthenticator not exist!");
            return null;
        }
        Intent intent = new Intent(this, homeAuthenticatorAdapter.getHomeAuthenticatorClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return intent;
    }

    private void checkNetworkConnectivity(@NonNull final Context context) {
        Objects.requireNonNull(context);

        if (!NetworkUtil.isNetworkConnected(context)) {
            if (mConnectivityDialog == null) {
                synchronized (SocialBaseActivity.class) {
                    if (mConnectivityDialog == null) {
                        mConnectivityDialog = new AlertDialog.Builder(context)
                                .setTitle(R.string.socialkm_network_connection_title)
                                .setMessage(R.string.socialkm_network_connection_message)
                                .setPositiveButton(R.string.Settings,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface,
                                                    int i) {
                                                dialogInterface.dismiss();

                                                Intent intent = new Intent(
                                                        Settings.ACTION_WIRELESS_SETTINGS);
                                                context.startActivity(intent);
                                            }
                                        })
                                .setNegativeButton(R.string.cancel,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface,
                                                    int i) {
                                                dialogInterface.dismiss();
                                                finish();
                                            }
                                        })
                                .setCancelable(false)
                                .create();
                    }
                }
            }
            mConnectivityDialog.show();
        } else {
            if (mConnectivityDialog != null && mConnectivityDialog.isShowing()) {
                mConnectivityDialog.dismiss();
            }
        }
    }

    public void showGooglePlayServiceNotAvailableDialog(final boolean finishSelf,
            final int errorCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mGooglePlayServicesNotAvailableDialog == null) {
                    final Intent intent =
                            GoogleApiAvailability.getInstance().getErrorResolutionIntent(
                                    SocialBaseActivity.this, errorCode, null);
                    String title = ConnectionErrorMessages.getErrorTitle(SocialBaseActivity.this,
                            errorCode);
                    String message = ConnectionErrorMessages.getErrorMessage(
                            SocialBaseActivity.this, errorCode);
                    String positiveButtonMessage =
                            ConnectionErrorMessages.getErrorDialogButtonMessage(
                                    SocialBaseActivity.this, errorCode);

                    if (TextUtils.isEmpty(message) || TextUtils.isEmpty(positiveButtonMessage)) {
                        // It's almost never happen
                        LogUtil.logError(TAG, "message or positiveButtonMessage is empty");
                        return;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(SocialBaseActivity.this);
                    if (!TextUtils.isEmpty(title)) {
                        builder.setTitle(title);
                    }
                    builder.setMessage(message);
                    builder.setPositiveButton(positiveButtonMessage,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    if (intent == null) {
                                        // Only SERVICE_MISSING, SERVICE_VERSION_UPDATE_REQUIRED
                                        // and SERVICE_DISABLED has error intent
                                        if (finishSelf) {
                                            finish();
                                        }
                                    } else {
                                        startActivity(intent);
                                        finishAffinity();
                                    }
                                }
                            });
                    builder.setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    mGooglePlayServicesNotAvailableDialog = builder.create();
                }

                if (!isFinishing() && !mGooglePlayServicesNotAvailableDialog.isShowing()) {
                    mGooglePlayServicesNotAvailableDialog.setCanceledOnTouchOutside(false);
                    mGooglePlayServicesNotAvailableDialog.show();
                }
            }
        });
    }

    public void setLoadingViewVisibility(final boolean visible) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (visible) {
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    mLoadingView = getLayoutInflater().inflate(R.layout.activity_fake_layout, null);
                    ViewGroup.LayoutParams params =
                            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT);
                    addContentView(mLoadingView, params);
                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    if (mLoadingView != null) {
                        View parent = (View) mLoadingView.getParent();
                        if (parent != null) {
                            ((ViewGroup) parent).removeView(mLoadingView);
                        }
                        mLoadingView = null;
                    }
                }
            }
        });
    }

    protected boolean isLoadingViewShowing() {
        return mLoadingView != null;
    }

    @UiThread
    public void setKeyboardVisibility(final boolean visible) {
        if (visible) {
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        } else {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }

    protected void setShouldAuth(boolean shouldAuth) {
        LogUtil.logDebug(TAG, "Set should auth=" + shouldAuth);
        mShouldAuth = shouldAuth;
    }
}
