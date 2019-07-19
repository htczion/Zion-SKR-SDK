package com.htc.wallet.skrsdk.verification;

import static com.htc.wallet.skrsdk.action.Action.KEY_EMAIL_HASH;
import static com.htc.wallet.skrsdk.action.Action.KEY_IS_RESEND;
import static com.htc.wallet.skrsdk.action.Action.KEY_IS_TEST;
import static com.htc.wallet.skrsdk.action.Action.KEY_NAME;
import static com.htc.wallet.skrsdk.action.Action.KEY_PHONE_NUMBER;
import static com.htc.wallet.skrsdk.action.Action.KEY_PIN_CODE;
import static com.htc.wallet.skrsdk.action.Action.KEY_PUBLIC_KEY;
import static com.htc.wallet.skrsdk.action.Action.KEY_RETRY_TIMES;
import static com.htc.wallet.skrsdk.action.Action.KEY_RETRY_WAIT_START_TIME;
import static com.htc.wallet.skrsdk.action.Action.KEY_SOURCE_NAME;
import static com.htc.wallet.skrsdk.action.Action.KEY_TARGET_NAME;
import static com.htc.wallet.skrsdk.action.Action.KEY_TARGET_UUID_HASH;
import static com.htc.wallet.skrsdk.action.Action.KEY_TZID_HASH;
import static com.htc.wallet.skrsdk.action.Action.KEY_UUID_HASH;
import static com.htc.wallet.skrsdk.action.Action.MSG_IS_RESEND;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_FULL;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_OK;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.PIN_CODE_ERROR;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.PIN_CODE_NO_ERROR;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_NOTIFY_BACKUP_FULL;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_NOTIFY_UI_UPDATE_AFTER_ERROR_PINCODE;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_OK_SENT;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.EMPTY_STRING;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.MILLIS_IN_MINUTE;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.NO_RESPONSE_HINT_TIMER;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.VERIFY_ACTION_TIMEOUT;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.SocialBaseActivity;
import com.htc.wallet.skrsdk.action.BackupRequestAction;
import com.htc.wallet.skrsdk.action.BackupVerifyAction;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupSourceUtil;
import com.htc.wallet.skrsdk.util.IntentUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;
import com.htc.wallet.skrsdk.util.PinCodeUtil;
import com.htc.wallet.skrsdk.util.RetryUtil;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnegative;

public class VerificationCodeActivity extends SocialBaseActivity implements
        VerificationCodeView.PinCodeChangeListener {
    private static final String TAG = "VerificationCodeActivity";

    private static final String KEY_VER_CODE_MY_NAME = "key_ver_code_my_name";
    private static final String KEY_VER_CODE_EMAIL_HASH = "key_ver_code_email_hash";
    private static final String KEY_VER_CODE_UUID_HASH = "key_ver_code_uuid_hash";
    private static final String KEY_VER_CODE_NAME = "key_ver_code_name";
    private static final String KEY_VER_CODE_MY_NAME_FOR_RESEND = "key_ver_code_my_name_for_resend";
    private static final String KEY_VER_CODE_UUID_HASH_FOR_RESEND =
            "key_ver_code_uuid_hash_for_resend";
    private static final String KEY_VER_CODE_FCM_TOKEN = "key_ver_code_fcm_token";
    private static final String KEY_VER_CODE_WHISPER_PUB = "key_ver_code_whisper_pub";
    private static final String KEY_VER_CODE_PUSHY_TOKEN = "key_ver_code_pushy_token";
    private static final String KEY_VER_CODE_PUBLIC_KEY = "key_ver_code_public_key";
    private static final String KEY_VER_CODE_TZID_HASH = "key_ver_code_tzid_hash";
    private static final String KEY_VER_CODE_LAST_REQUEST_TIME = "key_ver_code_last_request_time";
    private static final String KEY_VER_CODE_PIN_CODE = "key_ver_code_pin_code";
    private static final String KEY_VER_CODE_IS_PIN_CODE_ERROR = "key_ver_code_is_pin_code_error";
    private static final String KEY_VER_CODE_RETRY_TIMES = "key_ver_code_retry_times";
    private static final String KEY_VER_CODE_RETRY_WAIT_START_TIME =
            "key_ver_code_retry_wait_start_time";
    private static final String KEY_VER_CODE_LAST_VERIFY_TIME = "key_ver_code_last_verify_time";
    private static final String KEY_VER_CODE_IS_BACKUP_SOURCE_FULL =
            "key_ver_code_is_backup_source_full";
    private static final String KEY_VER_CODE_IS_TEST = "key_ver_code_is_test";

    // On toolbar
    private ImageButton mIbPrevious;
    private VerificationCodeView mVerificationCodeView;

    // Bob
    private String mMyName;
    private String mMyPhoneNumber;

    // Sender, Amy
    private String mEmailHash;
    private String mUUIDHash;
    private String mName;
    private String mFCMToken;
    private String mWhisperPub;
    private String mPushyToken;
    private String mPublicKey;
    private String mTZIDHash;
    private int mRetryTimes;
    private long mRetryWaitStartTime;
    private String mPinCode;
    private long mLastVerifyTime;
    private boolean mIsBackupFull;
    private boolean mIsTest;
    private boolean mIsPinCodeError;
    private long mLastRequestTime;
    // Resend
    private String mTargetName;
    private String mTargetUUIDHash;
    private boolean mIsResend = false;

    private TextView mTvVerCodeTitle;
    private TextView mTvStep2;
    private TextView mTvNoResponse;
    private LocalBroadcastManager mErrorActionBroadcast;
    private BroadcastReceiver mActionReceiver;

    private AlertDialog mRetryWaitDialog;
    private AlertDialog mBackupFullDialog;
    private ProgressDialog mProgressDialog;
    private CountDownTimer mCountDownTimer;
    private Handler mProgressTimeoutHandler = new Handler();
    private Runnable mProgressRunnable =
            new Runnable() {
                @Override
                public void run() {
                    cancelProgressDialog();
                    mVerificationCodeView.setTimeoutStyle();
                }
            };

    private View.OnClickListener mClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int i = v.getId();
                    if (i == R.id.ib_previous) {
                        onBackPressed();
                    } else if (i == R.id.bt_verify) {
                        verify();
                    } else if (i == R.id.tv_no_response) {
                        requestAgain();
                    } else {
                        LogUtil.logError(TAG, "incorrect click id");
                    }
                }
            };

    @NonNull
    public static Intent generateIntent(
            @NonNull Context context,
            String myName,
            String targetName,
            String targetUUIDHash,
            String emailHash,
            String uuidHash,
            String name,
            String fcmToken,
            String whisperPub,
            String pushyToken,
            String publicKey,
            String tzIdHash,
            @Nonnegative long lastRequestTime,
            @Nullable String pinCode,
            int isPinCodeError,
            @Nonnegative int retryTimes,
            @Nonnegative long retryWaitStartTime,
            @Nonnegative long lastVerifyTime,
            boolean isFull,
            boolean isTest) {
        Objects.requireNonNull(context, "context is null");
        Intent intent = new Intent(context, VerificationCodeActivity.class);
        intent.putExtra(KEY_VER_CODE_MY_NAME, myName);
        intent.putExtra(KEY_VER_CODE_MY_NAME_FOR_RESEND, targetName);
        intent.putExtra(KEY_VER_CODE_UUID_HASH_FOR_RESEND, targetUUIDHash);
        intent.putExtra(KEY_VER_CODE_EMAIL_HASH, emailHash);
        intent.putExtra(KEY_VER_CODE_UUID_HASH, uuidHash);
        intent.putExtra(KEY_VER_CODE_NAME, name);
        intent.putExtra(KEY_VER_CODE_FCM_TOKEN, fcmToken);
        intent.putExtra(KEY_VER_CODE_WHISPER_PUB, whisperPub);
        intent.putExtra(KEY_VER_CODE_PUSHY_TOKEN, pushyToken);
        intent.putExtra(KEY_VER_CODE_PUBLIC_KEY, publicKey);
        intent.putExtra(KEY_VER_CODE_TZID_HASH, tzIdHash);
        intent.putExtra(KEY_VER_CODE_LAST_REQUEST_TIME, lastRequestTime);
        intent.putExtra(KEY_VER_CODE_PIN_CODE, pinCode);
        intent.putExtra(KEY_VER_CODE_IS_PIN_CODE_ERROR, isPinCodeError);
        intent.putExtra(KEY_VER_CODE_RETRY_TIMES, retryTimes);
        intent.putExtra(KEY_VER_CODE_RETRY_WAIT_START_TIME, retryWaitStartTime);
        intent.putExtra(KEY_VER_CODE_LAST_VERIFY_TIME, lastVerifyTime);
        intent.putExtra(KEY_VER_CODE_IS_BACKUP_SOURCE_FULL, isFull);
        intent.putExtra(KEY_VER_CODE_IS_TEST, isTest);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.verification_code_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        linkUIComponents();

        final Intent intent = getIntent();
        if (intent == null) {
            LogUtil.logError(TAG, "intentFromRequest is null");
            finish();
            return;
        }
        if (!initVariable(intent)) {
            LogUtil.logError(TAG, "initVariable error");
            finish();
            return;
        }
        initViewContain();
        initCountdownTimer();
    }

    //    @Override
    //    protected void onNewIntent(Intent intent) {
    //        super.onNewIntent(intent);
    //        setIntent(intent);
    //        if (!initVariable(intent)) {
    //            LogUtil.logError(TAG, "initVariable error");
    //            finish();
    //            return;
    //        }
    //        initViewContain();
    //        initCountdownTimer();
    //        cancelProgressDialog();
    //        clearRetryWaitDialog();
    //        mProgressTimeoutHandler.removeCallbacksAndMessages(null);
    //    }

    private boolean initVariable(@NonNull Intent intent) {
        if (intent == null) {
            LogUtil.logError(TAG, "intent is null", new IllegalArgumentException("intent is null"));
            return false;
        }

        // Amy
        mEmailHash = intent.getStringExtra(KEY_VER_CODE_EMAIL_HASH);
        if (TextUtils.isEmpty(mEmailHash)) {
            LogUtil.logError(TAG, "mEmailHash is null or empty");
            return false;
        }
        mUUIDHash = intent.getStringExtra(KEY_VER_CODE_UUID_HASH);
        if (TextUtils.isEmpty(mUUIDHash)) {
            LogUtil.logError(TAG, "mUUIDHash is null or empty");
            return false;
        }
        mName = intent.getStringExtra(KEY_VER_CODE_NAME);
        if (TextUtils.isEmpty(mName)) {
            LogUtil.logError(TAG, "mName is null or empty");
            return false;
        }
        mFCMToken = intent.getStringExtra(KEY_VER_CODE_FCM_TOKEN);
        mWhisperPub = intent.getStringExtra(KEY_VER_CODE_WHISPER_PUB);
        mPushyToken = intent.getStringExtra(KEY_VER_CODE_PUSHY_TOKEN);

        mPublicKey = intent.getStringExtra(KEY_VER_CODE_PUBLIC_KEY);
        if (TextUtils.isEmpty(mPublicKey)) {
            LogUtil.logError(TAG, "mPublicKey is null or empty");
            return false;
        }
        mTZIDHash = intent.getStringExtra(KEY_VER_CODE_TZID_HASH);
        if (TextUtils.isEmpty(mTZIDHash)) {
            LogUtil.logError(TAG, "mTZIDHash is null or empty");
            return false;
        }
        mLastRequestTime =
                intent.getLongExtra(KEY_VER_CODE_LAST_REQUEST_TIME, System.currentTimeMillis());
        mPinCode = intent.getStringExtra(KEY_VER_CODE_PIN_CODE);
        // PIN_CODE_NO_ERROR = 0 (default), PIN_CODE_ERROR = 1
        mIsPinCodeError = intent.getIntExtra(KEY_VER_CODE_IS_PIN_CODE_ERROR, PIN_CODE_NO_ERROR)
                == PIN_CODE_ERROR;
        mRetryTimes = intent.getIntExtra(KEY_VER_CODE_RETRY_TIMES, 0);
        mRetryWaitStartTime = intent.getLongExtra(KEY_VER_CODE_RETRY_WAIT_START_TIME, 0);
        mLastVerifyTime = intent.getLongExtra(KEY_VER_CODE_LAST_VERIFY_TIME, 0);
        mIsBackupFull = intent.getBooleanExtra(KEY_VER_CODE_IS_BACKUP_SOURCE_FULL, false);
        mIsTest = intent.getBooleanExtra(KEY_VER_CODE_IS_TEST, false);

        // Bob
        mMyName = intent.getStringExtra(KEY_VER_CODE_MY_NAME);
        if (TextUtils.isEmpty(mMyName)) {
            LogUtil.logError(TAG, "mMyName is null or empty");
            return false;
        }
        // Permissions have been request at VerificationRequestActivity, not need to request again,
        // although user denied
        mMyPhoneNumber = PhoneUtil.getNumber(this);

        // Resend
        mTargetName = intent.getStringExtra(KEY_VER_CODE_MY_NAME_FOR_RESEND);
        if (!TextUtils.isEmpty(mTargetName)) {
            mIsResend = true;
        }
        mTargetUUIDHash = intent.getStringExtra(KEY_VER_CODE_UUID_HASH_FOR_RESEND);

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkVerifyResult();
        createAndRegisterActionReceiver();
        showBackupFullDialogIfNeeded();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mErrorActionBroadcast == null) {
            LogUtil.logError(TAG, "mErrorActionBroadcast is null");
            return;
        }
        mErrorActionBroadcast.unregisterReceiver(mActionReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelProgressDialog();
        clearCountDownTimer();
    }

    @Override
    public void onBackPressed() {
        //        if (mVerificationCodeView.isKeyboardShowing()) {
        //            mVerificationCodeView.setNumberKeyboardVisibility(View.INVISIBLE);
        //            return;
        //        }
        super.onBackPressed();
        finish();
    }

    private void cancelProgressDialog() {
        synchronized (this) {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
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

    private void showRetryWaitDialog(@Nonnegative int minutes) {
        if (minutes < 0) {
            LogUtil.logError(TAG, "minutes is incorrect");
            minutes = 0;
        }

        final String title = getResources().getString(R.string.try_too_often_title);
        final String message =
                String.format(getResources().getString(R.string.try_too_often_message), minutes);

        clearRetryWaitDialog();
        synchronized (this) {
            if (mRetryWaitDialog == null) {
                mRetryWaitDialog =
                        new AlertDialog.Builder(this)
                                .setTitle(title)
                                .setMessage(message)
                                .setPositiveButton(
                                        R.string.ver_dialog_btn_ok,
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

    private void showBackupFullDialogIfNeeded() {
        BackupSourceUtil.getWithUUIDHash(
                this,
                mEmailHash,
                mUUIDHash,
                new LoadDataListener() {
                    @Override
                    public void onLoadFinished(
                            BackupSourceEntity backupSourceEntity,
                            BackupTargetEntity backupTargetEntity,
                            RestoreSourceEntity restoreSourceEntity,
                            RestoreTargetEntity restoreTargetEntity) {
                        if (mIsBackupFull
                                || (backupSourceEntity != null
                                && backupSourceEntity.compareStatus(
                                BACKUP_SOURCE_STATUS_FULL))) {
                            cancelProgressDialog();
                            mProgressTimeoutHandler.removeCallbacksAndMessages(null);
                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            showBackupFullDialog();
                                        }
                                    });
                        }
                    }
                });
    }

    private void showBackupFullDialog() {
        final String title =
                getResources().getString(R.string.ver_dialog_trusted_contact_full_title);
        final String message =
                String.format(
                        getResources().getString(R.string.ver_dialog_trusted_contact_full_content),
                        mName);

        clearBackupFullDialog();
        synchronized (this) {
            if (mBackupFullDialog == null) {
                mBackupFullDialog =
                        new AlertDialog.Builder(this)
                                .setTitle(title)
                                .setMessage(message)
                                .setCancelable(false)
                                .setPositiveButton(
                                        R.string.ver_dialog_btn_ok,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                LogUtil.logDebug(
                                                        TAG,
                                                        "User knows "
                                                                + mName
                                                                + "'s contacts is full, remove it");
                                                BackupSourceUtil.remove(
                                                        VerificationCodeActivity.this,
                                                        mEmailHash,
                                                        mUUIDHash);
                                                Intent intent =
                                                        IntentUtil.generateEntryActivityIntent(
                                                                VerificationCodeActivity.this);
                                                startActivity(intent);
                                                finish();
                                                dialog.dismiss();
                                            }
                                        })
                                .create();
                mBackupFullDialog.setCanceledOnTouchOutside(false);
                mBackupFullDialog.show();
            }
        }
    }

    private void clearBackupFullDialog() {
        synchronized (this) {
            if (mBackupFullDialog != null) {
                if (mBackupFullDialog.isShowing()) {
                    mBackupFullDialog.dismiss();
                }
                mBackupFullDialog = null;
            }
        }
    }

    private void verify() {
        final String verificationPinCode = mVerificationCodeView.getVerificationPinCode();
        if (!PinCodeUtil.isValidPinCode(verificationPinCode)) {
            LogUtil.logError(TAG, "Invalid pin code format");
            return;
        }

        long waitTime = RetryUtil.getWaitTimeMillis(mRetryTimes);
        long remainTime = mRetryWaitStartTime + waitTime - System.currentTimeMillis();
        if (remainTime > 0) {
            int remainTimeMinutes = (int) Math.ceil((double) remainTime / MILLIS_IN_MINUTE);
            showRetryWaitDialog(remainTimeMinutes);
            LogUtil.logDebug(TAG, "Wait for retry waitTime");
            return;
        }

        mPinCode = verificationPinCode;
        mLastVerifyTime = System.currentTimeMillis();
        mProgressDialog.show();
        // TODO: verify action timeout, consider it as pin error situation? dialog or toast msg?
        mProgressTimeoutHandler.postDelayed(mProgressRunnable, VERIFY_ACTION_TIMEOUT);
        final Map<String, String> messages = new ArrayMap<>();
        messages.put(KEY_EMAIL_HASH, mEmailHash);
        messages.put(KEY_UUID_HASH, mUUIDHash);
        messages.put(KEY_PIN_CODE, verificationPinCode);
        new BackupVerifyAction()
                .send(VerificationCodeActivity.this, mFCMToken, mWhisperPub, mPushyToken, messages);
    }

    private void requestAgain() {
        mLastRequestTime = System.currentTimeMillis();
        initCountdownTimer();

        Map<String, String> messageContent = new ArrayMap<>();
        // Source (Amy)
        messageContent.put(KEY_EMAIL_HASH, mEmailHash);
        messageContent.put(KEY_SOURCE_NAME, mName);
        messageContent.put(KEY_UUID_HASH, mUUIDHash);
        messageContent.put(KEY_PUBLIC_KEY, mPublicKey);
        messageContent.put(KEY_TZID_HASH, mTZIDHash);
        messageContent.put(KEY_IS_TEST, String.valueOf(mIsTest));
        // Target (Bob)
        messageContent.put(KEY_NAME, mMyName);
        messageContent.put(KEY_PHONE_NUMBER, mMyPhoneNumber);
        messageContent.put(KEY_TARGET_NAME, mTargetName);
        messageContent.put(KEY_TARGET_UUID_HASH, mTargetUUIDHash);
        if (mIsResend) {
            messageContent.put(KEY_IS_RESEND, MSG_IS_RESEND);
        }

        new BackupRequestAction()
                .send(
                        VerificationCodeActivity.this,
                        mFCMToken,
                        mWhisperPub,
                        mPushyToken,
                        messageContent);
    }

    private void setupVerifyingProgress() {
        synchronized (this) {
            if (mProgressDialog == null) {
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setTitle(R.string.ver_dialog_verifying_title);

                String message =
                        String.format(
                                getResources().getString(R.string.ver_dialog_verifying), mName);
                mProgressDialog.setMessage(message);
                mProgressDialog.setButton(
                        DialogInterface.BUTTON_POSITIVE,
                        getResources().getString(R.string.ver_dialog_btn_ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO return to invited list
                                Intent intent =
                                        new Intent(
                                                VerificationCodeActivity.this,
                                                SocialKeyRecoveryRequestActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
                            }
                        });
                mProgressDialog.setOnKeyListener(
                        new DialogInterface.OnKeyListener() {
                            @Override
                            public boolean onKey(
                                    DialogInterface dialog, int keyCode, KeyEvent event) {
                                return keyCode == KeyEvent.KEYCODE_BACK;
                            }
                        });
                mProgressDialog.setCanceledOnTouchOutside(false);
            }
        }
    }

    private void initCountdownTimer() {
        long remainRequestAgainTime =
                mLastRequestTime + NO_RESPONSE_HINT_TIMER - System.currentTimeMillis();
        if (remainRequestAgainTime <= 0) {
            mTvNoResponse.setClickable(true);
            clearCountDownTimer();
            timeUpdate(0);
            return;
        } else {
            mTvNoResponse.setClickable(false);
            clearCountDownTimer();
        }

        synchronized (this) {
            mCountDownTimer =
                    new CountDownTimer(remainRequestAgainTime, 1000) {

                        @Override
                        public void onFinish() {
                            // Enable text to clickable
                            mTvNoResponse.setClickable(true);
                            clearCountDownTimer();
                        }

                        @Override
                        public void onTick(long millisUntilFinished) {
                            timeUpdate(millisUntilFinished);
                        }
                    }.start();
        }
    }

    private void clearCountDownTimer() {
        synchronized (this) {
            if (mCountDownTimer != null) {
                mCountDownTimer.cancel();
                mCountDownTimer = null;
            }
        }
    }

    private void timeUpdate(long millisUntilFinished) {
        String time =
                String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)
                                % TimeUnit.HOURS.toMinutes(1),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)
                                % TimeUnit.MINUTES.toSeconds(1));
        String noResponseHintWithTimer =
                String.format(getResources().getString(R.string.ver_no_response_hint), time);
        mTvNoResponse.setText(noResponseHintWithTimer);
    }

    private void createAndRegisterActionReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NOTIFY_UI_UPDATE_AFTER_ERROR_PINCODE);
        filter.addAction(ACTION_OK_SENT);
        filter.addAction(ACTION_NOTIFY_BACKUP_FULL);

        mActionReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent == null) {
                            LogUtil.logError(TAG, "intent is null");
                            return;
                        }

                        String actionStr = intent.getAction();
                        // Not need to check empty
                        if (actionStr == null) {
                            LogUtil.logError(TAG, "actionStr is null");
                            return;
                        }

                        String emailHash = intent.getStringExtra(KEY_EMAIL_HASH);
                        String uuidHash = intent.getStringExtra(KEY_UUID_HASH);

                        if (!mEmailHash.equals(emailHash) || !mUUIDHash.equals(uuidHash)) {
                            LogUtil.logDebug(TAG, "emailHash and uuidHash not match");
                            return;
                        }

                        switch (actionStr) {
                            case ACTION_OK_SENT:
                                goOkCongratulation();
                                break;
                            case ACTION_NOTIFY_UI_UPDATE_AFTER_ERROR_PINCODE:
                                // RetryTimes
                                mRetryTimes = intent.getIntExtra(KEY_RETRY_TIMES, mRetryTimes);
                                mRetryWaitStartTime =
                                        intent.getLongExtra(
                                                KEY_RETRY_WAIT_START_TIME, mRetryWaitStartTime);
                                if (mLastVerifyTime + VERIFY_ACTION_TIMEOUT
                                        > System.currentTimeMillis()) {
                                    cancelProgressDialog();
                                    mProgressTimeoutHandler.removeCallbacksAndMessages(null);
                                    mVerificationCodeView.setPinErrorStyle();
                                }
                                break;
                            case ACTION_NOTIFY_BACKUP_FULL:
                                cancelProgressDialog();
                                mProgressTimeoutHandler.removeCallbacksAndMessages(null);
                                showBackupFullDialog();
                                break;
                            default:
                                LogUtil.logError(TAG, "No matching action");
                                break;
                        }
                    }
                };

        if (mErrorActionBroadcast == null) {
            LogUtil.logError(TAG, "mErrorActionBroadcast is null");
            return;
        }
        mErrorActionBroadcast.registerReceiver(mActionReceiver, filter);
    }

    private void goOkCongratulation() {
        Intent intentToOkCongratulation =
                VerificationOkCongratulationActivity.generateIntent(
                        VerificationCodeActivity.this, mEmailHash, mUUIDHash, mName);
        startActivity(intentToOkCongratulation);
        finish();
    }

    private void checkVerifyResult() {
        BackupSourceUtil.getWithUUIDHash(
                this,
                mEmailHash,
                mUUIDHash,
                new LoadDataListener() {
                    @Override
                    public void onLoadFinished(
                            BackupSourceEntity backupSourceEntity,
                            BackupTargetEntity backupTargetEntity,
                            RestoreSourceEntity restoreSourceEntity,
                            RestoreTargetEntity restoreTargetEntity) {
                        if (backupSourceEntity == null) {
                            LogUtil.logDebug(TAG, "backupSource is null");
                            return;
                        }

                        if (backupSourceEntity.compareStatus(BACKUP_SOURCE_STATUS_OK)) {
                            goOkCongratulation();
                            return;
                        }

                        if (backupSourceEntity.compareStatus(BACKUP_SOURCE_STATUS_REQUEST)
                                && backupSourceEntity.getIsPinCodeError() == 1) {
                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            cancelProgressDialog();
                                            mVerificationCodeView.setPinErrorStyle();
                                        }
                                    });
                        }
                    }
                });
    }

    private void linkUIComponents() {
        mErrorActionBroadcast = LocalBroadcastManager.getInstance(this);
        mVerificationCodeView = findViewById(R.id.vcv_verification_code);
        mVerificationCodeView.setPinCodeChangeListener(this);

        mTvVerCodeTitle = findViewById(R.id.tv_ver_code_title);

        mTvStep2 = findViewById(R.id.tv_step2);

        mTvNoResponse = findViewById(R.id.tv_no_response);
        mTvNoResponse.setPaintFlags(mTvNoResponse.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        mTvNoResponse.setOnClickListener(mClickListener);

        mVerificationCodeView.mBtnVerify.setOnClickListener(mClickListener);

        mIbPrevious = findViewById(R.id.ib_previous);
        mIbPrevious.setOnClickListener(mClickListener);
    }

    private void initViewContain() {
        String verCodeTitle =
                String.format(getResources().getString(R.string.ver_content_title), mName);
        mTvVerCodeTitle.setText(verCodeTitle);

        String verStep2 = String.format(getResources().getString(R.string.ver_step2), mName);
        mTvStep2.setText(verStep2);

        setupVerifyingProgress();

        mVerificationCodeView.setNumberKeyboardVisibility(View.VISIBLE);
        mVerificationCodeView.clearVerificationPinCode();

        if (!TextUtils.isEmpty(mPinCode)) {
            mVerificationCodeView.setVerificationPinCode(mPinCode);
            final long elapsedTime = System.currentTimeMillis() - mLastVerifyTime;
            if (mIsPinCodeError) {
                mVerificationCodeView.setPinErrorStyle();
            } else if (elapsedTime >= VERIFY_ACTION_TIMEOUT) {
                mVerificationCodeView.setTimeoutStyle();
            } else {
                mProgressDialog.show();
                mProgressTimeoutHandler.postDelayed(
                        mProgressRunnable, (VERIFY_ACTION_TIMEOUT - elapsedTime));
            }
        }
    }

    @Override
    public void onPinCodeChanged() {
        if (!TextUtils.isEmpty(mPinCode)) {
            mPinCode = EMPTY_STRING;
            BackupSourceUtil.getWithUUIDHash(
                    this,
                    mEmailHash,
                    mUUIDHash,
                    new LoadDataListener() {
                        @Override
                        public void onLoadFinished(
                                BackupSourceEntity backupSourceEntity,
                                BackupTargetEntity backupTargetEntity,
                                RestoreSourceEntity restoreSourceEntity,
                                RestoreTargetEntity restoreTargetEntity) {
                            if (backupSourceEntity == null) {
                                LogUtil.logDebug(TAG, "backupSource has been deleted, ignore it.");
                                return;
                            }
                            if (!backupSourceEntity.compareStatus(BACKUP_SOURCE_STATUS_REQUEST)) {
                                LogUtil.logError(
                                        TAG, "incorrect status", new IllegalStateException());
                                return;
                            }
                            backupSourceEntity.clearPinCode();
                            BackupSourceUtil.update(
                                    getBaseContext(),
                                    backupSourceEntity,
                                    new DatabaseCompleteListener() {
                                        @Override
                                        public void onComplete() {
                                        }

                                        @Override
                                        public void onError(Exception exception) {
                                            LogUtil.logError(TAG, "update error, e= " + exception);
                                        }
                                    });
                        }
                    });
        }
    }
}
