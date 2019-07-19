package com.htc.wallet.skrsdk.verification;

import static com.htc.wallet.skrsdk.secretsharing.SeedUtil.INDEX_MAX;
import static com.htc.wallet.skrsdk.secretsharing.SeedUtil.INDEX_MIN;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_REQUEST_WAIT_OK;
import static com.htc.wallet.skrsdk.util.NotificationUtil.NOTIFICATION_ID_VERIFICATION_SHARING;
import static com.htc.wallet.skrsdk.util.NotificationUtil.cancelNotification;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.EMPTY_STRING;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.view.View;
import android.view.WindowManager;

import com.htc.htcwalletsdk.Export.HtcWalletSdkManager;
import com.htc.htcwalletsdk.Export.RESULT;
import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.action.Action;
import com.htc.wallet.skrsdk.action.BackupDeleteAction;
import com.htc.wallet.skrsdk.adapter.ActivityStateAdapter;
import com.htc.wallet.skrsdk.adapter.HomeAuthenticatorAdapter;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupTargetUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.StringUtil;
import com.htc.wallet.skrsdk.util.ThreadUtil;
import com.htc.wallet.skrsdk.util.WalletSdkUtil;

import java.lang.ref.SoftReference;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BackupShowVerificationCodeActivity extends AppCompatActivity {

    private static final String TAG = "BackupShowVerificationCodeActivity";
    private static final String KEY_UUID_HASH = "key_bsvc_uuid_hash";
    private static final String HOME_AUTHENTICATOR_FINISH = "home_authenticator_finish";
    private static final int MSG_FINISH = 200;
    private static final int MSG_FINISH_AND_AUTH_PASSED = 201;
    private static final int TIMEOUT = 10; // seconds
    private final ThreadPoolExecutor mSingleThreadExecutor = ThreadUtil.newFixedThreadPool(1, "backup-show-vfcode");
    private String mUUIDHash;
    private UiHandler mUiHandler;

    public static Intent generateIntent(Context context, String uuidHash) {
        Intent intent = new Intent(context, BackupShowVerificationCodeActivity.class);
        intent.putExtra(KEY_UUID_HASH, uuidHash);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake_layout);
        setStatusBarTransparent();
        initVariable(getIntent());
        checkSeedIndexAndContinue(this);
    }

    private void initVariable(@NonNull Intent intent) {
        if (intent == null) {
            LogUtil.logError(TAG, "intent is null", new IllegalArgumentException());
            finish();
            return;
        }
        mUUIDHash = intent.getStringExtra(KEY_UUID_HASH);
        if (TextUtils.isEmpty(mUUIDHash)) {
            LogUtil.logError(TAG, "UUIDHash is null or empty", new IllegalArgumentException());
            finish();
            return;
        }
        mUiHandler = new UiHandler(this);

        // Remove used Notification
        cancelNotification(this, mUUIDHash, NOTIFICATION_ID_VERIFICATION_SHARING);
    }

    // Step 1, Get seed Index or choose new one
    private void checkSeedIndexAndContinue(@NonNull final Context context) {
        BackupTargetUtil.get(context, mUUIDHash, new LoadDataListener() {
            @Override
            public void onLoadFinished(
                    final BackupSourceEntity backupSourceEntity,
                    final BackupTargetEntity backupTargetEntity,
                    RestoreSourceEntity restoreSourceEntity,
                    RestoreTargetEntity restoreTargetEntity) {
                // Check status
                if (backupTargetEntity == null) {
                    LogUtil.logError(TAG, "backupTargetEntity is null");
                    finish();
                    return;
                } else if (!backupTargetEntity.isStatusPending()) {
                    LogUtil.logError(TAG, "incorrect status, " + backupTargetEntity.getStatus());
                    finish();
                    return;
                }

                mSingleThreadExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        final String fcmToken = backupTargetEntity.getFcmToken();
                        final String whisperPub = backupTargetEntity.getWhisperPub();
                        final String pushyToken = backupTargetEntity.getPushyToken();
                        final String name = backupTargetEntity.getName();
                        final String phoneNumber =
                                TextUtils.isEmpty(backupTargetEntity.getPhoneNumber())
                                        ? EMPTY_STRING : backupTargetEntity.getPhoneNumber();
                        String uncheckedPhoneModel = backupTargetEntity.getPhoneModel();
                        if (uncheckedPhoneModel == null) {
                            LogUtil.logInfo(TAG, "phone model is null");
                            uncheckedPhoneModel = EMPTY_STRING;
                        } else if (!StringUtil.isAscii(uncheckedPhoneModel)) {
                            LogUtil.logInfo(TAG,
                                    "phone model=" + uncheckedPhoneModel + " is not Ascii String");
                            uncheckedPhoneModel = EMPTY_STRING;
                        }
                        final String phoneModel = uncheckedPhoneModel;

                        int seedIndex;
                        if (backupTargetEntity.isSeeded()) {
                            seedIndex = backupTargetEntity.getSeedIndex();
                        } else {
                            List<Integer> freeSeedIndexes = BackupTargetUtil.getFreeSeedIndex(
                                    context);
                            if (freeSeedIndexes.isEmpty()) {
                                LogUtil.logInfo(TAG, "All partial Seeds have been allocated.");
                                mUiHandler.sendEmptyMessage(MSG_FINISH);
                                return;
                            }
                            seedIndex = freeSeedIndexes.get(0);
                        }

                        showVerificationCode(context, seedIndex, name, phoneNumber, phoneModel,
                                fcmToken, whisperPub, pushyToken);
                    }
                });
            }
        });
    }

    //  Step 2, show verification code, trigger update backupTarget if success
    @WorkerThread
    private void showVerificationCode(final Context context, final int seedIndex, final String name,
            final String phoneNumber,
            final String phoneModel, final String fcmToken, final String whisperPub,
            final String pushyToken) {

        Objects.requireNonNull(context, "context is null");
        if (seedIndex < INDEX_MIN || seedIndex > INDEX_MAX) {
            LogUtil.logError(TAG, "incorrect seed index", new IllegalStateException());
            mUiHandler.sendEmptyMessage(MSG_FINISH);
            return;
        }
        if (TextUtils.isEmpty(name)) {
            LogUtil.logError(TAG, "name is null or empty", new IllegalStateException());
            mUiHandler.sendEmptyMessage(MSG_FINISH);
            return;
        }
        if (phoneNumber == null) {
            LogUtil.logError(TAG, "phoneNumber is null", new IllegalStateException());
            mUiHandler.sendEmptyMessage(MSG_FINISH);
            return;
        }
        if (phoneModel == null) {
            LogUtil.logError(TAG, "phoneModel is null", new IllegalStateException());
            mUiHandler.sendEmptyMessage(MSG_FINISH);
            return;
        }

        // Use WalletSdkUtil's thread to execute TZ function
        WalletSdkUtil.enqueue(new WalletSdkUtil.TzWorkItem() {
            @Override
            public void run() {
                HtcWalletSdkManager htcWalletSdkManager = HtcWalletSdkManager.getInstance();
                int initRet = htcWalletSdkManager.init(context);
                if (initRet != RESULT.SUCCESS) {
                    LogUtil.logError(TAG, "init failed, ret = " + initRet,
                            new IllegalStateException());
                    mUiHandler.sendEmptyMessage(MSG_FINISH);
                } else {
                    long uniqueId = ZionSkrSdkManager.getInstance().getWalletAdapter().getUniqueId(
                            BackupShowVerificationCodeActivity.this);
                    if (uniqueId == 0) {
                        LogUtil.logError(TAG, "showVerificationCode, uid=0");
                        return;
                    } else {
                        LogUtil.logDebug(TAG, "showVerificationCode, uid=" + LogUtil.pii(uniqueId));
                    }
                    final int showRet = htcWalletSdkManager.showVerificationCode(uniqueId,
                            seedIndex, name, phoneNumber, phoneModel);

                    // Use own single thread to execute
                    mSingleThreadExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            switch (showRet) {
                                case RESULT.E_TEEKM_VC_CANCEL: // -105
                                    LogUtil.logInfo(TAG, "User cancel");
                                    updateBackupTarget(context, mUUIDHash, seedIndex);
                                    mUiHandler.sendEmptyMessage(MSG_FINISH_AND_AUTH_PASSED);
                                    break;
                                case RESULT.E_TEEKM_VC_TIMEOUT: // -806
                                    LogUtil.logInfo(TAG, "Timeout, auto dismiss");
                                    updateBackupTarget(context, mUUIDHash, seedIndex);
                                    mUiHandler.sendEmptyMessage(MSG_FINISH_AND_AUTH_PASSED);
                                    break;
                                case RESULT.E_TEEKM_UI_REJECT: // -104
                                    LogUtil.logInfo(TAG, "User reject, remove it");
                                    Map<String, String> map = new ArrayMap<>();
                                    map.put(Action.KEY_UUID_HASH, mUUIDHash);
                                    new BackupDeleteAction().send(context, fcmToken, whisperPub,
                                            pushyToken, map);
                                    mUiHandler.sendEmptyMessage(MSG_FINISH_AND_AUTH_PASSED);
                                    break;
                                case RESULT.E_TEEKM_UI_CANCEL: // -101
                                    // User press back when enter the pin code
                                case RESULT.E_TEEKM_TIME_TIMEOUT: // -805
                                    // Timeout when enter the pin code
                                case RESULT.E_TEEKM_RETRY_15M: // -202
                                case RESULT.E_TEEKM_RETRY_30M: // -203
                                case RESULT.E_TEEKM_RETRY_45M: // -204
                                    // User try wrong pin code too many times, wait 15/30/45 minutes
                                case RESULT.E_TEEKM_RETRY_RESET: // -205
                                    // User try wrong pin code too many times, TZ reset wallet
                                default:
                                    LogUtil.logWarning(TAG,
                                            "Show verification code failed, result=" + showRet);
                                    mUiHandler.sendEmptyMessage(MSG_FINISH);
                            }
                        }
                    });
                }
            }
        });
    }

    // Check and update BackupTargetEntry
    @WorkerThread
    private void updateBackupTarget(
            @NonNull final Context context, @NonNull final String uuidHash, final int seedIndex) {
        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(TAG, "uuidHash is null or empty", new IllegalArgumentException());
            return;
        }
        if (seedIndex < INDEX_MIN || seedIndex > INDEX_MAX) {
            LogUtil.logError(TAG, "Incorrect seed index = " + seedIndex,
                    new IllegalStateException());
            return;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        BackupTargetUtil.get(context, uuidHash, new LoadDataListener() {
            @Override
            public void onLoadFinished(
                    BackupSourceEntity backupSourceEntity,
                    final BackupTargetEntity backupTargetEntity,
                    RestoreSourceEntity restoreSourceEntity,
                    RestoreTargetEntity restoreTargetEntity) {
                try {
                    if (backupTargetEntity == null) {
                        LogUtil.logError(TAG, "backupTargetEntity is null",
                                new IllegalStateException());
                        return;
                    }

                    boolean shouldUpdate = false;
                    if (backupTargetEntity.compareStatus(BACKUP_TARGET_STATUS_REQUEST_WAIT_OK)) {
                        LogUtil.logDebug(TAG, "Update " + backupTargetEntity.getName()
                                + "'s request wait ok to request");
                        backupTargetEntity.setStatus(BACKUP_TARGET_STATUS_REQUEST);
                        shouldUpdate = true;
                    }
                    if (!backupTargetEntity.isSeeded()) {
                        LogUtil.logDebug(TAG,
                                "Assign " + backupTargetEntity.getName() + "'s seedIndex");
                        backupTargetEntity.setSeedIndex(seedIndex);
                        shouldUpdate = true;
                    }
                    if (shouldUpdate) {
                        BackupTargetUtil.update(context, backupTargetEntity,
                                new DatabaseCompleteListener() {
                                    @Override
                                    public void onComplete() {
                                        LogUtil.logDebug(TAG,
                                                "Update " + backupTargetEntity.getName()
                                                        + "'s request success");
                                    }

                                    @Override
                                    public void onError(Exception exception) {
                                        LogUtil.logError(TAG, "update error, e= " + exception);
                                    }
                                });
                    }
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LogUtil.logError(TAG, "InterruptedException e = " + e);
        }
    }

    private void setStatusBarTransparent() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    private static class UiHandler extends Handler {
        private SoftReference<Activity> mSoftReference;

        UiHandler(Activity activity) {
            mSoftReference = new SoftReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Activity activity = mSoftReference.get();
            if (activity == null) {
                LogUtil.logError(TAG, "activity is null");
                return;
            }
            switch (msg.what) {
                case MSG_FINISH:
                    activity.finish();
                    break;
                case MSG_FINISH_AND_AUTH_PASSED:
                    // Set auth pass
                    ActivityStateAdapter activityStateAdapter =
                            ZionSkrSdkManager.getInstance().getActivityStateAdapter();
                    if (activityStateAdapter != null) {
                        activityStateAdapter.setIsAuthPassed(true);
                    }

                    // Start HomeAuthenticator to finish current HomeAuthenticator
                    HomeAuthenticatorAdapter homeAuthenticatorAdapter =
                            ZionSkrSdkManager.getInstance().getHomeAuthenticatorAdapter();
                    if (homeAuthenticatorAdapter != null) {
                        Intent intent = new Intent(activity,
                                homeAuthenticatorAdapter.getHomeAuthenticatorClass());
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        // put this extra to finish HomeAuthenticator
                        intent.putExtra(HOME_AUTHENTICATOR_FINISH, true);
                        activity.startActivity(intent);
                    }

                    // finish itself, finish() must be called after startActivity(), or
                    // IsAuthPass will go false
                    activity.finish();
                    break;
                default:
                    LogUtil.logError(TAG, "unknown message=" + msg.what);
                    activity.finish();
            }
        }
    }
}
