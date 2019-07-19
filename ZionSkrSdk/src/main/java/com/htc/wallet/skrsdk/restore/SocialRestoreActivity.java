package com.htc.wallet.skrsdk.restore;

import static android.view.View.INVISIBLE;

import static com.htc.wallet.skrsdk.action.Action.KEY_CHECKSUM;
import static com.htc.wallet.skrsdk.action.Action.KEY_ENCRYPTED_SEED;
import static com.htc.wallet.skrsdk.action.Action.KEY_PUBLIC_KEY;
import static com.htc.wallet.skrsdk.action.Action.KEY_RESTORE_SOURCE_PIN_CODE_POSITION;
import static com.htc.wallet.skrsdk.restore.PinCodeStatusConstant.SUCCESS;
import static com.htc.wallet.skrsdk.restore.RestoreVerificationCodeView.ET_PIN_SIZE;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_BAD;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_NO_RESPONSE;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.UNDEFINED_LAST_CHECKED_TIME;
import static com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity.RESTORE_SOURCE_STATUS_OK;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_RESTORE_NOTIFY_UI_UPDATE_AFTER_ERROR_PINCODE;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_RESTORE_NOTIFY_UI_UPDATE_AFTER_OK_PINCODE;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.htc.htcwalletsdk.Export.HtcWalletSdkManager;
import com.htc.htcwalletsdk.Export.RESULT;
import com.htc.htcwalletsdk.Native.Type.ByteArrayHolder;
import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.SocialBaseActivity;
import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.action.BackupSeedAction;
import com.htc.wallet.skrsdk.adapter.ActivityStateAdapter;
import com.htc.wallet.skrsdk.backup.constants.BackupSourceConstants;
import com.htc.wallet.skrsdk.crypto.ChecksumUtil;
import com.htc.wallet.skrsdk.drives.BackupTargetForDrive;
import com.htc.wallet.skrsdk.drives.DriveCallback;
import com.htc.wallet.skrsdk.drives.DriveUtil;
import com.htc.wallet.skrsdk.drives.DriveUtilFactory;
import com.htc.wallet.skrsdk.messaging.MessageServiceType;
import com.htc.wallet.skrsdk.restore.reconnect.ReconnectNameUtils;
import com.htc.wallet.skrsdk.restore.util.SkrRestoreEditTextStatus;
import com.htc.wallet.skrsdk.restore.util.SkrRestoreEditTextStatusUtil;
import com.htc.wallet.skrsdk.secretsharing.SeedUtil;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupTargetUtil;
import com.htc.wallet.skrsdk.sqlite.util.RestoreSourceUtil;
import com.htc.wallet.skrsdk.util.Callback;
import com.htc.wallet.skrsdk.util.IntentUtil;
import com.htc.wallet.skrsdk.util.InvitationUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;
import com.htc.wallet.skrsdk.util.RestoreTrustNameUtil;
import com.htc.wallet.skrsdk.util.RetryUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;
import com.htc.wallet.skrsdk.util.ThreadUtil;
import com.htc.wallet.skrsdk.util.WalletSdkUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.Nonnegative;

import me.pushy.sdk.Pushy;

public class SocialRestoreActivity extends SocialBaseActivity implements
        View.OnClickListener {
    public static final String SECRET_PIN_CODE = "* * * * * *";

    private static final String TAG = "SocialRestoreActivity";
    private static final int RESTORE_OK_NUMBER = 3;
    private static final int MAX_OTHER_FRIENDS = 2;
    private static final int MILLIS_IN_MINUTE = 60 * 1000;
    private static final long CLICK_TIME_INTERVAL = 300;
    private static final int PERMISSION_REQUEST = 0;

    private static final ThreadPoolExecutor sThreadPoolExecutor =
            ThreadUtil.newFixedThreadPoolNoQueue(3, "skr-restore-acty");

    private RestoreVerificationCodeView mVerifyCodeView;
    private Intent mIntent;
    private LocalBroadcastManager mActionBroadcast;
    private BroadcastReceiver mActionReceiver;
    private Button mBtnCancel;
    private Button mBtnNext;
    private InvitationUtil mInvitationUtil;
    private Button mBtnShareLink;
    private TextView mTrustContactName;

    private String mAddress;
    private String mEmailHash;
    private String mRestoreName;
    private String mGoogleDriveDeviceId;

    private volatile AlertDialog mRestoreFailedDialog = null;
    private volatile AlertDialog mRetryWaitDialog = null;
    private CountDownTimer mCountDownTimer;
    private volatile boolean mIsRetryTimesTimeout = true;
    private volatile boolean mCanShowEnterCode = true;
    // click verification code's editText time
    private long mEnterCodeLastClickTime;
    // click next button time
    private long mNextButtonLastClickTime;
    private int mRemainTimeMinutes = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_social_restore);
        mIntent = getIntent();
        initViews();
        setupComponents();
        setShouldAuth(false);

        int msgServiceType = ZionSkrSdkManager.getInstance().getMessageServiceType();
        boolean isUsedPushy = (msgServiceType == MessageServiceType.WHISPER)
                || (msgServiceType == MessageServiceType.MULTI);
        if (ZionSkrSdkManager.getInstance().getIsDebug() && isUsedPushy
                && !Pushy.isRegistered(this)) {
            checkExternalStoragePermission();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mActionBroadcast = LocalBroadcastManager.getInstance(this);
        setupReceiver();
        checkRemainWaitTime();
        refreshVerifyCodeView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeReceiver();
        storeRemainTime();
    }

    @Override
    protected void onStop() {
        super.onStop();
        cancelCountdownTimer();
    }

    @Override
    public void onClick(View v) {
        // Constant expression required
        // Resource IDs cannot be used in switch statement in Android library
        int id = v.getId();
        if (id == R.id.btn_restore_share_invitation) {
            mInvitationUtil.setGoogleDriveDeviceId(mGoogleDriveDeviceId);
            mInvitationUtil.changeToRestoreMode();
            mInvitationUtil.sharingInvitation();
        } else if (id == R.id.social_restore_btn_cancel) {
            onBackPressed();
        } else if (id == R.id.social_restore_btn_next) {
            final long currentTime = SystemClock.elapsedRealtime();
            if (currentTime >= mNextButtonLastClickTime + CLICK_TIME_INTERVAL) {
                checkIsSeedOKNumberEnough();
            }
            mNextButtonLastClickTime = currentTime;
        } else {
            LogUtil.logError(TAG, "Unknown id=" + id);
        }
    }

    private void initViews() {
        setupToolbar();
        mVerifyCodeView = findViewById(R.id.restore_verification_code);
        mBtnShareLink = findViewById(R.id.btn_restore_share_invitation);
        mBtnCancel = findViewById(R.id.social_restore_btn_cancel);
        mBtnNext = findViewById(R.id.social_restore_btn_next);
        mTrustContactName = findViewById(R.id.tv_restore_trust_contacts);
    }

    private void setupComponents() {
        mRestoreName = mIntent.getStringExtra(BackupSourceConstants.NAME);
        if (TextUtils.isEmpty(mRestoreName)) {
            LogUtil.logError(TAG, "mRestoreName is null or empty",
                    new IllegalStateException("mRestoreName is null or empty"));
            onBackPressed();
            return;
        }
        mAddress = mIntent.getStringExtra(BackupSourceConstants.ADDRESS);
        if (TextUtils.isEmpty(mAddress)) {
            LogUtil.logError(TAG, "Email is null or empty", new IllegalStateException());
            onBackPressed();
            return;
        }
        mEmailHash = ChecksumUtil.generateChecksum(mAddress);
        if (TextUtils.isEmpty(mEmailHash)) {
            LogUtil.logError(TAG, "mEmailHash is null or empty",
                    new IllegalStateException("mEmailHash is null or empty"));
            onBackPressed();
            return;
        }
        mGoogleDriveDeviceId = mIntent.getStringExtra(BackupSourceConstants.UUID_HASH);
        if (TextUtils.isEmpty(mGoogleDriveDeviceId)) {
            LogUtil.logError(TAG, "GoogleDriveDeviceId is null or empty",
                    new IllegalStateException());
            onBackPressed();
            return;
        }

        // Remove restore temporary data because restore's UUIDHash has changed
        if (!SkrSharedPrefs.getRestoreUUIDHash(this).equals(mGoogleDriveDeviceId)) {
            LogUtil.logInfo(TAG,
                    "Remove restore temporary data because restore's UUIDHash has changed");
            RestoreSourceUtil.removeAll(this);
            // Reset restore status
            SkrSharedPrefs.resetSkrRestoreStatus(this);
            // Put GoogleDriveDeviceId value to sharedPreference.
            SkrSharedPrefs.putRestoreUUIDHash(this, mGoogleDriveDeviceId);
        }

        mInvitationUtil = new InvitationUtil(SocialRestoreActivity.this, mRestoreName, mAddress);
        mBtnShareLink.setOnClickListener(this);
        mBtnCancel.setOnClickListener(this);
        mBtnNext.setOnClickListener(this);

        setupVerifyCodeView();
        setupTrustContacts();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.tb_restore_toolbar);
        setSupportActionBar(toolbar);
        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
    }

    private void setupTrustContacts() {
        final String trustContactData = mIntent.getStringExtra(BackupSourceConstants.BACKUPTARGETS);
        if (TextUtils.isEmpty(mAddress)) {
            LogUtil.logDebug(TAG, "setupTrustContacts address is null");
            return;
        }

        final String currentRestoreEmail = SkrSharedPrefs.getCurrentRestoreEmail(this);
        ArrayList<String> nameList = new ArrayList<>();
        if (TextUtils.isEmpty(currentRestoreEmail) || !mAddress.equals(currentRestoreEmail)) {
            // Restore email is changed (not init), clear previous data and socialkmId
            if (!TextUtils.isEmpty(currentRestoreEmail)) {
                // TODO: Check if duplicate, restore's uuidHash has checked before
                LogUtil.logInfo(TAG, "Use different email, clear previous pending restore data.");
                RestoreSourceUtil.removeAll(this);

                // Reset restore status
                SkrSharedPrefs.resetSkrRestoreStatus(this);
            }
            List<BackupTargetForDrive> trustDataList = DriveUtil.stringToList(trustContactData);
            if (trustDataList != null) {
                for (BackupTargetForDrive backupTargetForDrive : trustDataList) {
                    nameList.add(backupTargetForDrive.getName());
                }
            }
            SkrSharedPrefs.putCurrentRestoreEmail(this, mAddress);
            RestoreTrustNameUtil.put(this, nameList);
        } else {
            nameList = RestoreTrustNameUtil.get(this);
        }
        mTrustContactName.setText(String.join(", ", nameList));
    }

    private void refreshVerifyCodeView() {
        RestoreSourceUtil.getAll(this, new LoadListListener() {
            @Override
            public void onLoadFinished(
                    List<BackupSourceEntity> backupSourceEntityList,
                    List<BackupTargetEntity> backupTargetEntityList,
                    final List<RestoreSourceEntity> restoreSourceEntityList,
                    List<RestoreTargetEntity> restoreTargetEntityList) {
                if (restoreSourceEntityList == null) {
                    LogUtil.logError(TAG, "restoreSources is null",
                            new IllegalStateException("restoreSources is null"));
                    return;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Find OK
                        for (RestoreSourceEntity restoreSource : restoreSourceEntityList) {
                            if (restoreSource.compareStatus(RESTORE_SOURCE_STATUS_OK)) {

                                final int pinCodePosition = restoreSource.getPinCodePosition();
                                final String pinCode = restoreSource.getPinCode();
                                mVerifyCodeView.etPinShow[pinCodePosition].setText(pinCode);
                                mVerifyCodeView.setPinCodeSuccessStyle(pinCodePosition);
                                // Reset restore editText status
                                SkrSharedPrefs.resetSkrRestoreEditTextStatus(
                                        getBaseContext(), pinCodePosition);
                            }
                        }

                        // Check others status
                        for (int i = 0; i < ET_PIN_SIZE; i++) {
                            if (mVerifyCodeView.getPinCodeStatus(i) == SUCCESS) {
                                // this position is success
                                continue;
                            }

                            SkrRestoreEditTextStatus skrRestoreEditTextStatus =
                                    SkrRestoreEditTextStatusUtil.getStatus(getBaseContext(), i);
                            if (!skrRestoreEditTextStatus.hasSent()) {
                                continue;
                            }

                            long remainTimeoutTime =
                                    skrRestoreEditTextStatus.getRemainingTimeBeforeTimeout();
                            int receiveCount = skrRestoreEditTextStatus.getReceivedPinCount();
                            int receiveMaxCount = skrRestoreEditTextStatus.getReceivedPinMaxCount();
                            mVerifyCodeView.setCurrentPinCodePositionReceivedCount(i, receiveCount);
                            mVerifyCodeView.setCurrentPinCodePositionReceivedMaxCount(i,
                                    receiveMaxCount);
                            mVerifyCodeView.etPinShow[i].setText(SECRET_PIN_CODE);
                            if (remainTimeoutTime <= 0 || (receiveCount >= receiveMaxCount
                                    && receiveMaxCount != 0)) {
                                mVerifyCodeView.setPinCodeErrorStyle(i);
                                mVerifyCodeView.cancelCountdownTimer(i);
                            } else {
                                mVerifyCodeView.setPinCodeNotCompletedStyle(i);
                                mVerifyCodeView.startCountdownTimer(i, remainTimeoutTime);
                            }
                        }
                    }
                });
            }
        });
    }

    private void setupVerifyCodeView() {
        mVerifyCodeView.setEmailHash(mEmailHash);
        for (int i = 0; i < mVerifyCodeView.getPinCodeEditTextSize(); i++) {
            final int position = i;
            final EditText editTextShow = mVerifyCodeView.etPinShow[position];
            View.OnTouchListener touchListener =
                    new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent event) {
                            switch (event.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                    editTextShow.requestFocus();
                                    Drawable overlay = ContextCompat.getDrawable(
                                            SocialRestoreActivity.this,
                                            R.drawable.shape_social_backup_item_rectangle_overlay);
                                    editTextShow.setForeground(overlay);
                                    mCanShowEnterCode = true;
                                    checkRetryTime();
                                    final long now = SystemClock.elapsedRealtime();
                                    if (now - mEnterCodeLastClickTime < CLICK_TIME_INTERVAL) {
                                        mCanShowEnterCode = false;
                                    }
                                    mEnterCodeLastClickTime = now;
                                    return true;
                                case MotionEvent.ACTION_UP:
                                    if (mCanShowEnterCode) {
                                        enterVerificationCode(position);
                                    }
                                case MotionEvent.ACTION_CANCEL:
                                    Drawable transparent = new ColorDrawable(Color.TRANSPARENT);
                                    editTextShow.setForeground(transparent);
                                    return true;
                            }
                            return false;
                        }
                    };
            editTextShow.setOnTouchListener(touchListener);
        }
    }

    private void setupReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_RESTORE_NOTIFY_UI_UPDATE_AFTER_ERROR_PINCODE);
        filter.addAction(ACTION_RESTORE_NOTIFY_UI_UPDATE_AFTER_OK_PINCODE);

        mActionReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(final Context context, Intent intent) {
                        switch (Objects.requireNonNull(intent.getAction(), "intent is null")) {
                            case ACTION_RESTORE_NOTIFY_UI_UPDATE_AFTER_ERROR_PINCODE:
                                LogUtil.logDebug(TAG,
                                        "Receive ACTION_RESTORE_NOTIFY_UI_UPDATE_AFTER_ERROR_PINCODE");
                                if (mVerifyCodeView == null) {
                                    LogUtil.logError(TAG, "verificationCodeView is null",
                                            new IllegalStateException(
                                                    "verificationCodeView is null"));
                                    return;
                                }
                                final String strErrCodePosition = intent.getStringExtra(
                                        KEY_RESTORE_SOURCE_PIN_CODE_POSITION);
                                if (TextUtils.isEmpty(strErrCodePosition)) {
                                    LogUtil.logError(TAG, "strErrCodePosition is null or empty",
                                            new IllegalStateException(
                                                    "strErrCodePosition is null or empty"));
                                    return;
                                }
                                final int errCodePosition = Integer.valueOf(strErrCodePosition);

                                mVerifyCodeView.addCurrentPinCodePositionReceivedCount(
                                        errCodePosition);
                                if (mVerifyCodeView.getCurrentPinCodePositionReceivedMaxCount(
                                        errCodePosition) != 0
                                        && mVerifyCodeView.getCurrentPinCodePositionReceivedCount(
                                        errCodePosition)
                                        != mVerifyCodeView
                                        .getCurrentPinCodePositionReceivedMaxCount(
                                                errCodePosition)) {
                                    LogUtil.logDebug(TAG,
                                            "Current pin code positionNot reaching max received "
                                                    + "number");
                                    return;
                                }
                                mVerifyCodeView.resetCurrentPinCodePositionReceivedCount(
                                        errCodePosition);
                                if (mVerifyCodeView.getPinCodeStatus(errCodePosition) != SUCCESS) {
                                    int retryTime = mVerifyCodeView.getRestoreRetryTime() + 1;
                                    mVerifyCodeView.saveRetryTimes(retryTime);
                                    mVerifyCodeView.setPinCodeErrorStyle(errCodePosition);
                                }
                                break;
                            case ACTION_RESTORE_NOTIFY_UI_UPDATE_AFTER_OK_PINCODE:
                                LogUtil.logDebug(TAG,
                                        "Receive ACTION_RESTORE_NOTIFY_UI_UPDATE_AFTER_OK_PINCODE");
                                final String strOkCodePosition = intent.getStringExtra(
                                        KEY_RESTORE_SOURCE_PIN_CODE_POSITION);
                                if (TextUtils.isEmpty(strOkCodePosition)) {
                                    LogUtil.logError(TAG, "strOkCodePosition is null or empty",
                                            new IllegalStateException(
                                                    "strOkCodePosition is null or empty"));
                                    return;
                                }
                                final int okCodePosition = Integer.parseInt(strOkCodePosition);
                                mVerifyCodeView.addCurrentPinCodePositionReceivedCount(
                                        okCodePosition);
                                mVerifyCodeView.setPinCodeSuccessStyle(okCodePosition);
                                mVerifyCodeView.refreshValidErrorVisibility();
                                // Reset restore editText status
                                SkrSharedPrefs.resetSkrRestoreEditTextStatus(getBaseContext(),
                                        okCodePosition);
                                break;
                            default:
                                LogUtil.logError(TAG, "No matching action");
                                break;
                        }
                    }
                };

        if (mActionBroadcast == null) {
            LogUtil.logError(TAG, "mActionBroadcast is null",
                    new IllegalStateException("mActionBroadcast is null"));
            return;
        }
        mActionBroadcast.registerReceiver(mActionReceiver, filter);
    }

    private void removeReceiver() {
        if (mActionBroadcast == null) {
            LogUtil.logError(TAG, "mActionBroadcast is null",
                    new IllegalStateException("mActionBroadcast is null"));
            return;
        }
        mActionBroadcast.unregisterReceiver(mActionReceiver);
    }

    private void showRestoreFailedDialog() {
        final String title = getResources().getString(
                R.string.social_restore_dialog_failed_to_restore_title);
        final String content = getResources().getString(
                R.string.social_restore_dialog_failed_to_restore_content);

        clearRestoreFailedDialog();
        synchronized (this) {
            if (mRestoreFailedDialog == null) {
                mRestoreFailedDialog =
                        new AlertDialog.Builder(this)
                                .setTitle(title)
                                .setMessage(content)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ver_dialog_btn_ok,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                SkrSharedPrefs.clearAllSocialKeyRecoveryData(
                                                        getBaseContext());
                                                RestoreSourceUtil.clearData(getBaseContext());
                                                Intent intent =
                                                        IntentUtil.generateEntryActivityIntent(
                                                                getBaseContext());
                                                startActivity(intent);
                                                finish();
                                                dialog.dismiss();
                                            }
                                        })
                                .create();
                mRestoreFailedDialog.setCanceledOnTouchOutside(false);
                mRestoreFailedDialog.show();
            }
        }
    }

    private void checkIsSeedOKNumberEnough() {
        RestoreSourceUtil.getAll(getBaseContext(), new LoadListListener() {
            @Override
            public void onLoadFinished(
                    List<BackupSourceEntity> backupSourceEntityList,
                    List<BackupTargetEntity> backupTargetEntityList,
                    final List<RestoreSourceEntity> restoreSources,
                    List<RestoreTargetEntity> restoreTargetEntityList) {
                if (restoreSources == null) {
                    LogUtil.logError(TAG, "restoreSources is null");
                    return;
                }

                int OKNumber = 0;
                List<String> seeds = new ArrayList<>();
                List<String> encSeedSigns = new ArrayList<>();
                for (RestoreSourceEntity restoreSource : restoreSources) {
                    if (restoreSource.compareStatus(RESTORE_SOURCE_STATUS_OK)) {
                        seeds.add(restoreSource.getSeed());
                        encSeedSigns.add(restoreSource.getEncSeedSigned());
                        OKNumber++;
                    }
                }
                if (RESTORE_OK_NUMBER != OKNumber) {
                    LogUtil.logWarning(TAG, "OKNumber is not equal to RESTORE_OK_NUMBER(3)");
                    return;
                }
                mVerifyCodeView.setValidErrorVisibility(INVISIBLE);
                setLoadingViewVisibility(true);
                final int combineSeedResult = SeedUtil.combineSeedV2(
                        SocialRestoreActivity.this,
                        seeds.get(0),
                        encSeedSigns.get(0),
                        seeds.get(1),
                        encSeedSigns.get(1),
                        seeds.get(2),
                        encSeedSigns.get(2));
                if (combineSeedResult == RESULT.SUCCESS) {
                    saveBackupSourceInfo();
                    transformRestoreSourceToBackupTarget(restoreSources);
                    RestoreSourceUtil.clearData(getApplicationContext());
                    SkrSharedPrefs.putShouldShowTrySocialKeyRecoveryDialog(
                            getApplicationContext(), false);
                    startAllSetActivity();
                } else {
                    setLoadingViewVisibility(false);
                    switch (combineSeedResult) {
                        case RESULT.E_TEEKM_UI_CANCEL: // -101
                            LogUtil.logInfo(TAG, "Timeout, auto cancel");
                            break;
                        case RESULT.E_TEEKM_UI_RELATED: // -200
                            LogUtil.logInfo(TAG, "User cancel");
                            break;
                        case RESULT.E_TEEKM_SEED_EXISTS: // -501
                            LogUtil.logWarning(TAG, "Seed exist");
                            break;
                        default:
                            LogUtil.logWarning(TAG, "Unknown failed, ret = " + combineSeedResult);
                            SkrSharedPrefs.clearAllSocialKeyRecoveryData(
                                    SocialRestoreActivity.this);
                            BackupTargetUtil.clearData(SocialRestoreActivity.this);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showRestoreFailedDialog();
                                }
                            });
                    }
                }
            }
        });
    }

    private void transformRestoreSourceToBackupTarget(
            final List<RestoreSourceEntity> restoreSources) {
        if (restoreSources == null) {
            LogUtil.logDebug(TAG, "transformRestoreSourceToBackupTarget, restoreSources is null");
            return;
        }

        int seedIndexCount = 0;
        final List<BackupTargetEntity> backupTargetList = new ArrayList<>();
        for (RestoreSourceEntity restoreSource : restoreSources) {
            if (restoreSource.compareStatus(RESTORE_SOURCE_STATUS_OK) && !TextUtils.isEmpty(
                    restoreSource.getName())) {
                BackupTargetEntity backupTargetEntity = new BackupTargetEntity();
                // Auto backup, set init status no response (gray)
                backupTargetEntity.setStatus(BACKUP_TARGET_STATUS_NO_RESPONSE);
                backupTargetEntity.setName(restoreSource.getName());
                backupTargetEntity.setFcmToken(restoreSource.getFcmToken());
                backupTargetEntity.setWhisperPub(restoreSource.getWhisperPub());
                backupTargetEntity.setPushyToken(restoreSource.getPushyToken());
                // Use UNDEFINED_LAST_CHECKED_TIME(-1) to represent the status until receiving the
                // BackupOKAction from Bob
                backupTargetEntity.setLastCheckedTime(UNDEFINED_LAST_CHECKED_TIME);
                backupTargetEntity.setPublicKey(restoreSource.getPublicKey());
                backupTargetEntity.setPinCode(restoreSource.getPinCode());
                backupTargetEntity.setSeedIndex(restoreSource.getPinCodePosition());
                backupTargetEntity.setUUIDHash(restoreSource.getUUIDHash());
                backupTargetEntity.setEncCodePk(restoreSource.getEncCodePk());
                backupTargetEntity.setEncCodePkSign(restoreSource.getEncCodePkSign());
                backupTargetEntity.setEncAseKey(restoreSource.getEncAseKey());
                backupTargetEntity.setEncAseKeySign(restoreSource.getEncAseKeySign());
                // Add backupTarget to backupTargetList
                backupTargetList.add(backupTargetEntity);
                seedIndexCount++;
            }
        }
        // Check recover friends number
        if (seedIndexCount != RESTORE_OK_NUMBER) {
            LogUtil.logError(TAG,
                    "The amount of OK RestoreSource must be 3, now is = " + seedIndexCount);
        }

        // Find only name friends
        final ArrayList<String> nameList = RestoreTrustNameUtil.get(this);
        for (BackupTargetEntity backupTargetEntity : backupTargetList) {
            String name = backupTargetEntity.getName();
            boolean removeRet = nameList.remove(name);
            if (!removeRet) {
                LogUtil.logWarning(TAG, "remove failed");
            }
        }
        // Check only name friends number
        if (nameList.size() > MAX_OTHER_FRIENDS) {
            LogUtil.logWarning(TAG, "incorrect other friend number, count = " + nameList.size());
        }

        final List<String> reconnectNameList = new ArrayList<>();

        // Put only name friends
        for (String name : nameList) {
            if (seedIndexCount > SeedUtil.INDEX_MAX) {
                LogUtil.logError(TAG, "incorrect seed index, " + seedIndexCount + ", skip");
                break;
            }
            BackupTargetEntity backupTargetEntity = new BackupTargetEntity();
            backupTargetEntity.setStatus(BACKUP_TARGET_STATUS_BAD);
            backupTargetEntity.setName(name);
            backupTargetEntity.setFcmToken("");
            backupTargetEntity.setWhisperPub("");
            backupTargetEntity.setPushyToken("");
            backupTargetEntity.setLastCheckedTime(System.currentTimeMillis());
            backupTargetEntity.setPublicKey("");
            backupTargetEntity.setPinCode("");
            backupTargetEntity.setSeedIndex(seedIndexCount++);
            backupTargetEntity.setCheckSum("");
            backupTargetEntity.setUUIDHash("");

            // Add only name backupTarget to backupTargetList
            backupTargetList.add(backupTargetEntity);
            // Add name to reconnectNameList
            reconnectNameList.add(name);
        }

        // Put BackupTargets
        BackupTargetUtil.putList(this, backupTargetList, new DatabaseCompleteListener() {
            @Override
            public void onComplete() {
                generateAndSendBackup();
            }

            @Override
            public void onError(Exception exception) {
                LogUtil.logError(TAG, "put list error, e= " + exception);
            }
        });

        // Put reconnectNameList to sharedPreferences
        if (reconnectNameList.isEmpty()) {
            LogUtil.logDebug(TAG, "No reconnect name");
        } else {
            ReconnectNameUtils.putNameList(getBaseContext(), reconnectNameList);
        }
    }

    private void saveBackupSourceInfo() {
        final String currentUUIDHash = PhoneUtil.getSKRIDHash(this);
        SkrSharedPrefs.putSocialKMBackupEmail(this, mAddress);
        SkrSharedPrefs.putSocialKMUserName(this, mRestoreName);

        final DriveUtil util = DriveUtilFactory.getDriveUtil(this, mAddress);

        util.saveUUIDHash(currentUUIDHash, new DriveCallback<Void>() {
            @Override
            public void onComplete(Void aVoid) {
                LogUtil.logInfo(TAG, "saveUUIDHash(), success");
            }

            @Override
            public void onFailure(Exception e) {
                LogUtil.logInfo(TAG, "saveUUIDHash(), error = " + e);
            }
        });
    }

    private void checkRetryTime() {
        synchronized (this) {
            showRetryWaitDialog(mRemainTimeMinutes);
            if (mIsRetryTimesTimeout) {
                int retryTime = mVerifyCodeView.getRestoreRetryTime();
                int waitTimeCount = (int) Math.floor((double) retryTime / 9.0);
                String waitTimeCountFormSp = SkrSharedPrefs.getRestoreWaitTime(this);
                int waitTimeDivide = (int) Math.floor((double) retryTime / 3.0);
                if (TextUtils.isEmpty(waitTimeCountFormSp)) {
                    SkrSharedPrefs.putRestoreWaitTime(this, String.valueOf(waitTimeCount));
                    waitTimeCountFormSp = String.valueOf(waitTimeCount);
                }
                if (waitTimeCount != Integer.valueOf(waitTimeCountFormSp)) {
                    SkrSharedPrefs.putRestoreWaitTime(this, String.valueOf(waitTimeCount));
                    long waitTime = RetryUtil.getWaitTimeMillis(waitTimeDivide);
                    mRemainTimeMinutes = (int) waitTime;
                    startCountdownTimer(waitTime, true);
                }
            }
        }
    }

    private void checkRemainWaitTime() {
        String remainTime = SkrSharedPrefs.getRestoreRemainTime(this);
        if (!TextUtils.isEmpty(remainTime)) {
            long waitRemainTime = Long.valueOf(remainTime) - System.currentTimeMillis();
            mRemainTimeMinutes = (int) waitRemainTime;
            if (waitRemainTime > 0) {
                startCountdownTimer(waitRemainTime, false);
            }
        }
    }

    private void storeRemainTime() {
        synchronized (this) {
            if (mRemainTimeMinutes > 0) {
                SkrSharedPrefs.putRestoreRemainTime(this,
                        String.valueOf(System.currentTimeMillis() + mRemainTimeMinutes));
            }
        }
    }

    private void showRetryWaitDialog(@Nonnegative int seconds) {
        if (seconds < 0) {
            return;
        }
        mCanShowEnterCode = false;
        final String title = getResources().getString(R.string.try_too_often_title);
        final String message =
                String.format(getResources().getString(R.string.try_too_often_message),
                        (int) Math.ceil((double) seconds / (double) MILLIS_IN_MINUTE));

        clearRetryWaitDialog();

        if (mRetryWaitDialog == null) {
            synchronized (this) {
                mRetryWaitDialog = new AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(R.string.ver_dialog_btn_ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                        .create();
                mRetryWaitDialog.setCanceledOnTouchOutside(false);
                mRetryWaitDialog.show();
            }
        }
    }

    private void clearRetryWaitDialog() {
        synchronized (this) {
            if (mRetryWaitDialog != null) {
                if (mRetryWaitDialog.isShowing()) {
                    mRetryWaitDialog.dismiss();
                }
                mRetryWaitDialog = null;
            }
        }
    }

    private void clearRestoreFailedDialog() {
        synchronized (this) {
            if (mRestoreFailedDialog != null) {
                if (mRestoreFailedDialog.isShowing()) {
                    mRestoreFailedDialog.dismiss();
                }
                mRestoreFailedDialog = null;
            }
        }
    }

    private void cancelCountdownTimer() {
        synchronized (this) {
            if (mCountDownTimer != null) {
                mCountDownTimer.cancel();
            }
        }
    }

    private void startCountdownTimer(final long waitTime, final boolean nextBtnClick) {
        synchronized (this) {
            if (nextBtnClick) {
                showRetryWaitDialog(mRemainTimeMinutes);
            }
            cancelCountdownTimer();
            mIsRetryTimesTimeout = false;
            mCountDownTimer =
                    new CountDownTimer(waitTime, MILLIS_IN_MINUTE) {
                        @Override
                        public void onFinish() {
                            mIsRetryTimesTimeout = true;
                            mRemainTimeMinutes = -1;
                        }

                        @Override
                        public void onTick(long millisUntilFinished) {
                            mRemainTimeMinutes = (int) millisUntilFinished;
                        }
                    }.start();
        }
    }

    private void generateAndSendBackup() {
        BackupTargetUtil.getAll(
                getBaseContext(),
                new LoadListListener() {
                    @Override
                    public void onLoadFinished(
                            List<BackupSourceEntity> backupSourceEntityList,
                            final List<BackupTargetEntity> backupTargetEntityList,
                            List<RestoreSourceEntity> restoreSourceEntityList,
                            List<RestoreTargetEntity> restoreTargetEntityList) {
                        if (backupTargetEntityList == null) {
                            LogUtil.logError(TAG,
                                    "Failed to load backup targets, backupTargetEntityList is "
                                            + "null.",
                                    new IllegalStateException(
                                            "Failed to load backup targets, "
                                                    + "backupTargetEntityList is null."));
                            return;
                        }
                        final List<String> newSeed = new ArrayList<>();
                        final List<BackupTargetEntity> newBackupTargetEntityList =
                                new ArrayList<>();
                        for (BackupTargetEntity target : backupTargetEntityList) {
                            if (!TextUtils.isEmpty(target.getPublicKey())) {
                                final String partialSeed = SeedUtil.getPartialSeedV2(
                                        SocialRestoreActivity.this,
                                        target.getSeedIndex(),
                                        target.getEncCodePk(),
                                        target.getEncCodePkSign(),
                                        target.getEncAseKey(),
                                        target.getEncAseKeySign());
                                if (TextUtils.isEmpty(partialSeed)) {
                                    LogUtil.logError(TAG, "partialSeed is null or empty");
                                    LogUtil.logDebug(TAG,
                                            "Update " + target.getName() + "'s status to Bad.");
                                    target.setLastCheckedTime(System.currentTimeMillis());
                                    target.updateStatusToBad();
                                } else {
                                    newSeed.add(partialSeed);
                                    target.setCheckSum(ChecksumUtil.generateChecksum(partialSeed));
                                    newBackupTargetEntityList.add(target);
                                }
                            }
                        }
                        BackupTargetUtil.putList(SocialRestoreActivity.this, backupTargetEntityList,
                                new DatabaseCompleteListener() {
                                    @Override
                                    public void onComplete() {
                                        for (int index = 0;
                                                index < newBackupTargetEntityList.size(); index++) {
                                            executeBackupFlow(newBackupTargetEntityList.get(index),
                                                    newSeed.get(index));
                                        }
                                    }

                                    @Override
                                    public void onError(Exception exception) {
                                        LogUtil.logError(TAG, "put list error, e= " + exception);
                                    }
                                });
                    }
                });
    }

    private void executeBackupFlow(final BackupTargetEntity target, final String seed) {
        if (target == null) {
            LogUtil.logError(TAG, "executeBackupFlow, target is null",
                    new IllegalStateException("target is null"));
            return;
        }
        if (TextUtils.isEmpty(seed)) {
            LogUtil.logError(TAG, "executeBackupFlow, seed is null or empty",
                    new IllegalStateException("seed is null or empty"));
            return;
        }

        final Map<String, String> map = new ArrayMap<>();
        map.put(KEY_PUBLIC_KEY, target.getPublicKey());
        map.put(KEY_ENCRYPTED_SEED, seed);
        map.put(KEY_CHECKSUM, target.getCheckSum());
        new BackupSeedAction().send(this, target.getFcmToken(), target.getWhisperPub(),
                target.getPushyToken(), map);
    }

    private void startAllSetActivity() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // No need to show HomeAuthenticator after restore success.
                ActivityStateAdapter activityStateAdapter =
                        ZionSkrSdkManager.getInstance().getActivityStateAdapter();
                if (activityStateAdapter != null) {
                    activityStateAdapter.setIsAuthPassed(true);
                    LogUtil.logDebug(TAG, "Set auth pass");
                }

                Callback<Void> restoreCompleteCallback =
                        ZionSkrSdkManager.getInstance().getRestoreCompleteCallback();
                if (restoreCompleteCallback != null) {
                    restoreCompleteCallback.onResponse(null);
                }
                finish();
            }
        });
    }

    private void enterVerificationCode(final int position) {
        // show loading view
        setLoadingViewVisibility(true);

        sThreadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                int initRet = HtcWalletSdkManager.getInstance().init(getBaseContext());
                if (initRet != RESULT.SUCCESS) {
                    LogUtil.logError(TAG, "init failed, initRet = " + initRet);
                    return;
                }

                final ByteArrayHolder encCodeWithSign = new ByteArrayHolder();

                long uniqueId = WalletSdkUtil.getUniqueId(getBaseContext());
                // Use the pin code field position to be the seed index while restoring
                final int enterRet = HtcWalletSdkManager.getInstance()
                        .enterVerificationCode(uniqueId, position, encCodeWithSign);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        // hide loading view
                        setLoadingViewVisibility(false);

                        refreshVerifyCodeView();
                        if (enterRet != RESULT.SUCCESS) {
                            switch (enterRet) {
                                case RESULT.E_TEEKM_SEED_EXISTS:
                                    LogUtil.logError(TAG, "enterVerificationCode() failed, "
                                            + "seed existed");
                                    break;
                                case RESULT.E_TEEKM_UI_CANCEL:
                                    LogUtil.logInfo(TAG, "enterVerificationCode(), "
                                            + "canceled by user");
                                    break;
                                case RESULT.E_TEEKM_TIME_TIMEOUT:
                                    LogUtil.logInfo(TAG, "enterVerificationCode(), "
                                            + "time out and auto dismiss");
                                    break;
                                default:
                                    LogUtil.logError(TAG, "enterVerificationCode() failed, "
                                            + "ret=" + enterRet);
                                    break;
                            }
                            return;
                        }

                        byte[] encCodeBytes = Arrays.copyOf(encCodeWithSign.byteArray, 256);
                        byte[] encCodeSignedBytes =
                                Arrays.copyOfRange(encCodeWithSign.byteArray, 256, 512);

                        final String encCode = Base64.encodeToString(encCodeBytes, Base64.DEFAULT);
                        final String encCodeSigned =
                                Base64.encodeToString(encCodeSignedBytes, Base64.DEFAULT);

                        mVerifyCodeView.etPinShow[position].setText(SECRET_PIN_CODE);
                        mVerifyCodeView.setPinCodeNotCompletedStyle(position);
                        mVerifyCodeView.refreshValidErrorVisibility();

                        sThreadPoolExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                mVerifyCodeView.sendPinCodeToAllRestoreSources(
                                        encCode,
                                        encCodeSigned,
                                        position);
                            }
                        });
                    }
                });
            }
        });
    }

    private void checkExternalStoragePermission() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {

            requestPermissions(
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                LogUtil.logDebug(TAG, "WRITE_EXTERNAL_STORAGE permission is granted");
            } else {
                LogUtil.logDebug(TAG, "WRITE_EXTERNAL_STORAGE permission is denied");
            }
        }
    }
}
