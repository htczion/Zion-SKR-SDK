package com.htc.wallet.skrsdk;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.common.base.Preconditions;
import com.htc.wallet.skrsdk.adapter.ActivityStateAdapter;
import com.htc.wallet.skrsdk.adapter.ApiKeyAdapter;
import com.htc.wallet.skrsdk.adapter.EntryActivityAdapter;
import com.htc.wallet.skrsdk.adapter.HomeAuthenticatorAdapter;
import com.htc.wallet.skrsdk.adapter.IsUsingProductionServerAdapter;
import com.htc.wallet.skrsdk.adapter.WalletAdapter;
import com.htc.wallet.skrsdk.backup.SocialBackupIntroductionActivity;
import com.htc.wallet.skrsdk.backup.SocialKeyRecoveryChooseDriveAccountActivity;
import com.htc.wallet.skrsdk.backup.constants.RestoreChooserConstants;
import com.htc.wallet.skrsdk.messaging.MessageServiceType;
import com.htc.wallet.skrsdk.monitor.BackupHealthCheckJobService;
import com.htc.wallet.skrsdk.monitor.BackupHealthReportJobService;
import com.htc.wallet.skrsdk.monitor.SocialKeyRecoveryChecker;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupDataStatusUtil;
import com.htc.wallet.skrsdk.sqlite.util.BackupTargetUtil;
import com.htc.wallet.skrsdk.tools.security.attestation.SafetyNetWrapper;
import com.htc.wallet.skrsdk.util.Callback;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.ThreadUtil;
import com.htc.wallet.skrsdk.util.WalletSdkUtil;
import com.htc.wallet.skrsdk.verification.SocialKeyRecoveryRequestActivity;
import com.htc.wallet.skrsdk.verification.VerificationConstants;

import java.util.List;

import io.branch.referral.Branch;

public final class ZionSkrSdkManager {
    public static final String ACTION_SKR_REQUESTS_DATA_CHANGED =
            VerificationConstants.ACTION_TRIGGER_BROADCAST;
    private static final String TAG = "ZionSkrSdkManager";

    private static volatile ZionSkrSdkManager sInstance;

    /**
     * Must provide Android application context.
     */
    private final Context mContext;
    private final ApiKeyAdapter mApiKeyAdapter;
    private final WalletAdapter mWalletAdapter;
    private final ActivityStateAdapter mActivityStateAdapter;
    private final EntryActivityAdapter mEntryActivityAdapter;
    private final HomeAuthenticatorAdapter mHomeAuthenticatorAdapter;
    private final IsUsingProductionServerAdapter mIsUsingProductionServerAdapter;
    private final Callback<Void> mRestoreCompleteCallback;
    private final boolean mIsDebug;

    private ZionSkrSdkManager(Params params) {
        checkParams(params);

        mContext = params.mContext.getApplicationContext();
        mApiKeyAdapter = params.mApiKeyAdapter;
        mWalletAdapter = params.mWalletAdapter;
        mActivityStateAdapter = params.mActivityStateAdapter;
        mEntryActivityAdapter = params.mEntryActivityAdapter;
        mHomeAuthenticatorAdapter = params.mHomeAuthenticatorAdapter;
        mIsUsingProductionServerAdapter = params.mIsUsingProductionServerAdapter;
        mRestoreCompleteCallback = params.mRestoreCompleteCallback;
        mIsDebug = params.mIsDebug;

        // Set LogUtil debuggable
        LogUtil.setIsDebug(mIsDebug);

        ThreadUtil.getBackgroundExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (WalletSdkUtil.isExodus(mContext)) {
                    // Schedule SKR monitor backup health check if needed
                    BackupHealthCheckJobService.schedule(mContext);
                }

                // Schedule SKR monitor backup health report
                BackupHealthReportJobService.schedule(mContext);

                // Check SKR
                SocialKeyRecoveryChecker.check(mContext);

                // SafetyNetWrapper, update token background
                SafetyNetWrapper.getInstance().update(mContext);

                // Init branch if needed
                String branchKey = mApiKeyAdapter.getBranchKey();
                if (!TextUtils.isEmpty(branchKey)) {
                    Branch.getAutoInstance(mContext, branchKey);
                }
            }
        });
    }

    private void checkParams(Params params) {
        Preconditions.checkNotNull(params);
        Preconditions.checkNotNull(params.mContext);
        Preconditions.checkNotNull(params.mApiKeyAdapter);
        Preconditions.checkNotNull(params.mWalletAdapter);
    }

    public static void init(@NonNull Params params) {
        synchronized (ZionSkrSdkManager.class) {
            if (sInstance != null) {
                throw new IllegalStateException("init already called!");
            }
            sInstance = new ZionSkrSdkManager(params);
        }
    }

    public static ZionSkrSdkManager getInstance() {
        synchronized (ZionSkrSdkManager.class) {
            if (sInstance == null) {
                throw new IllegalStateException("Call init first!");
            }
            return sInstance;
        }
    }

    @NonNull
    public Context getAppContext() {
        return mContext;
    }

    @NonNull
    public ApiKeyAdapter getApiKeyAdapter() {
        return mApiKeyAdapter;
    }

    @NonNull
    public WalletAdapter getWalletAdapter() {
        return mWalletAdapter;
    }

    @Nullable
    public ActivityStateAdapter getActivityStateAdapter() {
        return mActivityStateAdapter;
    }

    @Nullable
    public EntryActivityAdapter getEntryActivityAdapter() {
        return mEntryActivityAdapter;
    }

    public HomeAuthenticatorAdapter getHomeAuthenticatorAdapter() {
        return mHomeAuthenticatorAdapter;
    }

    @MessageServiceType
    public int getMessageServiceType() {
        return MessageServiceType.FIREBASE;
    }

    public boolean getIsDebug() {
        return mIsDebug;
    }

    public boolean getIsUsingProductionServer() {
        if (mIsUsingProductionServerAdapter == null) {
            return true;
        }
        return mIsUsingProductionServerAdapter.getIsUsingProductionServer();
    }

    @Nullable
    public Callback<Void> getRestoreCompleteCallback() {
        return mRestoreCompleteCallback;
    }

    /**
     * Start Social Key Recovery backup activity.
     *
     * @param context Activity Context
     */
    public void startSkrBackup(@NonNull Context context) {
        Preconditions.checkNotNull(context);
        Intent intent = new Intent(context, SocialBackupIntroductionActivity.class);
        context.startActivity(intent);
    }

    /**
     * Start Social Key Recovery restore activity.
     *
     * @param context Activity Context
     */
    public void startSkrRestore(@NonNull Context context) {
        Preconditions.checkNotNull(context);
        Intent intent = new Intent(context, SocialKeyRecoveryChooseDriveAccountActivity.class);
        intent.putExtra(RestoreChooserConstants.KEY_FROM_ACTIVITY,
                RestoreChooserConstants.VALUE_FROM_ENTRY_ACTIVITY);
        context.startActivity(intent);
    }

    /**
     * Launch Social Key Recovery requests activity.
     *
     * @param context Activity Context
     */
    public void launchSkrRequestsActivity(@NonNull Context context) {
        Preconditions.checkNotNull(context);
        Intent intent = new Intent(context, SocialKeyRecoveryRequestActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    /**
     * Get if Social Key Recovery is active (active count is greater than 3).
     *
     * @param context  Context
     * @param callback Response is skr active or not
     */
    public void getIsSkrActive(@NonNull Context context,
            @NonNull final Callback<Boolean> callback) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(callback);

        BackupTargetUtil.getAllOK(context, new LoadListListener() {
            @Override
            public void onLoadFinished(
                    List<BackupSourceEntity> backupSourceEntityList,
                    List<BackupTargetEntity> backupTargetEntityList,
                    List<RestoreSourceEntity> restoreSourceEntityList,
                    List<RestoreTargetEntity> restoreTargetEntityList) {
                int size = 0;
                if (backupTargetEntityList == null) {
                    LogUtil.logError(TAG, "backupTargetEntityList is null");
                } else {
                    size = backupTargetEntityList.size();
                    LogUtil.logDebug(TAG, "Active backup target size=" + size);
                }

                final boolean isActive =
                        (size >= VerificationConstants.SOCIAL_KEY_RECOVERY_ACTIVE_THRESHOLD);

                // Run callback on main thread
                ThreadUtil.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResponse(isActive);
                    }
                });
            }
        });
    }

    /**
     * Get Social Key Recovery pending request quantity.
     *
     * @param context  Context
     * @param callback Response skr pending request quantity
     */
    public void getSkrRequestsCount(@NonNull Context context,
            @NonNull final Callback<Integer> callback) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(callback);

        BackupDataStatusUtil.getRequestsCount(context, new Callback<Integer>() {
            @Override
            public void onResponse(final Integer count) {
                // Run callback on main thread
                ThreadUtil.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResponse(count);
                    }
                });
            }
        });
    }

    public static class Params {
        private final Context mContext;
        private ApiKeyAdapter mApiKeyAdapter;
        private WalletAdapter mWalletAdapter;
        private ActivityStateAdapter mActivityStateAdapter;
        private EntryActivityAdapter mEntryActivityAdapter;
        private HomeAuthenticatorAdapter mHomeAuthenticatorAdapter;
        private IsUsingProductionServerAdapter mIsUsingProductionServerAdapter;
        private Callback<Void> mRestoreCompleteCallback;
        private boolean mIsDebug = false;

        public Params(@NonNull Context context) {
            Preconditions.checkNotNull(context);
            mContext = context;
        }

        public Params setApiKeyAdapter(@NonNull ApiKeyAdapter apiKeyAdapter) {
            Preconditions.checkNotNull(apiKeyAdapter);
            mApiKeyAdapter = apiKeyAdapter;
            return this;
        }

        public Params setWalletAdapter(@NonNull WalletAdapter walletAdapter) {
            Preconditions.checkNotNull(walletAdapter);
            mWalletAdapter = walletAdapter;
            return this;
        }

        public Params setActivityStateAdapter(ActivityStateAdapter activityStateAdapter) {
            mActivityStateAdapter = activityStateAdapter;
            return this;
        }

        public Params setEntryActivityAdapter(EntryActivityAdapter entryActivityAdapter) {
            mEntryActivityAdapter = entryActivityAdapter;
            return this;
        }

        public Params setHomeAuthenticatorAdapter(
                HomeAuthenticatorAdapter homeAuthenticatorAdapter) {
            mHomeAuthenticatorAdapter = homeAuthenticatorAdapter;
            return this;
        }

        public Params setIsUsingProductionServerAdapter(
                IsUsingProductionServerAdapter isUsingProductionServerAdapter) {
            mIsUsingProductionServerAdapter = isUsingProductionServerAdapter;
            return this;
        }

        public Params setRestoreCompleteCallback(Callback<Void> callback) {
            mRestoreCompleteCallback = callback;
            return this;
        }

        public Params setIsDebug(boolean isDebug) {
            mIsDebug = isDebug;
            return this;
        }
    }
}
