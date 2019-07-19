package com.htc.wallet.skrsdk.verification;

import static com.htc.wallet.skrsdk.action.Action.KEY_CHECKSUM;
import static com.htc.wallet.skrsdk.action.Action.KEY_EMAIL_HASH;
import static com.htc.wallet.skrsdk.action.Action.KEY_ENCRYPTED_SEED;
import static com.htc.wallet.skrsdk.action.Action.KEY_IS_RESEND;
import static com.htc.wallet.skrsdk.action.Action.KEY_IS_TEST;
import static com.htc.wallet.skrsdk.action.Action.KEY_NAME;
import static com.htc.wallet.skrsdk.action.Action.KEY_PHONE_NUMBER;
import static com.htc.wallet.skrsdk.action.Action.KEY_PUBLIC_KEY;
import static com.htc.wallet.skrsdk.action.Action.KEY_SOURCE_NAME;
import static com.htc.wallet.skrsdk.action.Action.KEY_TARGET_NAME;
import static com.htc.wallet.skrsdk.action.Action.KEY_TARGET_UUID_HASH;
import static com.htc.wallet.skrsdk.action.Action.KEY_TZID_HASH;
import static com.htc.wallet.skrsdk.action.Action.KEY_UUID_HASH;
import static com.htc.wallet.skrsdk.action.Action.MSG_IS_RESEND;
import static com.htc.wallet.skrsdk.applink.AppLinkConstant.KEY_TOKEN;
import static com.htc.wallet.skrsdk.applink.AppLinkConstant.KEY_WHISPER_PUB;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_DONE;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_OK;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_REQUEST_AUTO_BACKUP;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.INIT_LAST_VERIFY_TIME;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.INIT_PIN_CODE;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.INIT_RETRY_TIMES;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.INIT_RETRY_WAIT_START_TIME;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.PIN_CODE_NO_ERROR;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_RECEIVE_DELETE;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_RECEIVE_REPORT_OK;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.EMPTY_STRING;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.SocialBaseActivity;
import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.action.BackupHealthReportAction;
import com.htc.wallet.skrsdk.action.BackupRequestAction;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupSourceUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class VerificationRequestActivity extends SocialBaseActivity {
    private static final String TAG = "VerificationRequestActivity";

    private static final int PERMISSION_REQUIREMENT = 1;
    private static final int MAX_NAME_LENGTH = 80;
    // Used to check if Bob missed the delete message from Amy.
    private static final long HEALTH_CHECK_TIMEOUT = 60 * DateUtils.SECOND_IN_MILLIS;

    private static final String KEY_VER_REQUEST_EMAIL_HASH = "key_ver_request_email_hash";
    private static final String KEY_VER_REQUEST_UUID_HASH = "key_ver_request_uuid_hash";
    private static final String KEY_VER_REQUEST_NAME = "key_ver_request_name";
    private static final String KEY_VER_MY_NAME_FOR_RESEND = "key_ver_my_name_for_resend";
    private static final String KEY_VER_UUID_HASH_FOR_RESEND = "key_ver_uuid_hash_for_resend";
    private static final String KEY_VER_REQUEST_FCM_TOKEN = "key_ver_request_fcm_token";
    private static final String KEY_VER_REQUEST_WHISPER_PUB = "key_ver_request_whisper_pub";
    private static final String KEY_VER_REQUEST_PUSHY_TOKEN = "key_ver_request_pushy_token";
    private static final String KEY_VER_REQUEST_PUBLIC_KEY = "key_ver_request_public_key";
    private static final String KEY_VER_REQUEST_TZID_HASH = "key_ver_request_tzid_hash";
    // Bob need to know the rom type from Amy so that he can POST key server by using proper url.
    private static final String KEY_VER_REQUEST_IS_TEST = "key_ver_request_is_test";
    private final AtomicBoolean mHasReceivedOk = new AtomicBoolean(false);
    // On toolbar
    private ImageButton mIbPrevious;
    private TextView mTvRequestTitle;
    private TextView mTvStep1;
    private Button mBtnSendRequest;
    private EditText mEtName;
    // Me, Bob
    private String mMyName;
    private String mMyNameForResend = null;
    private String mUUIDHashForResend = null;
    private String mMyPhoneNumber = EMPTY_STRING;
    private volatile boolean mIsResend = false;
    // Amy
    private String mSenderEmailHash;
    private String mSenderToken;
    private String mSenderWhisperPub;
    private String mSenderPushyToken;
    private String mSenderPublicKey;
    private String mSenderName;
    private String mSenderUUIDHash;
    private String mSenderTZIDHash;
    private boolean mIsTest;
    private AlertDialog mBackupDoneDialog;
    private CountDownTimer mCountDownTimer;
    private BroadcastReceiver mBroadcastReceiver;
    private View.OnClickListener mClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int i = v.getId();
                    if (i == R.id.ib_previous) {
                        onBackPressed();

                    } else if (i == R.id.btn_send_request) {
                        request();

                    } else {
                        LogUtil.logError(TAG, "incorrect click id");
                    }
                }
            };

    @NonNull
    public static Intent generateIntent(
            @NonNull Context context,
            String emailHash,
            String uuidHash,
            String name,
            String fcmToken,
            String whisperPub,
            String pushyToken,
            String publicKey,
            String tzIdHash,
            boolean isTest) {
        return generateIntentForResend(
                context,
                emailHash,
                uuidHash,
                name,
                null,
                null,
                fcmToken,
                whisperPub,
                pushyToken,
                publicKey,
                tzIdHash,
                isTest);
    }

    @NonNull
    public static Intent generateIntentForResend(
            @NonNull Context context,
            String emailHash,
            String uuidHash,
            String name,
            String myName,
            String targetUUIDHash,
            String fcmToken,
            String whisperPub,
            String pushyToken,
            String publicKey,
            String tzIdHash,
            boolean isTest) {
        Objects.requireNonNull(context, "context is null");
        Intent intent = new Intent(context, VerificationRequestActivity.class);
        intent.putExtra(KEY_VER_REQUEST_EMAIL_HASH, emailHash);
        intent.putExtra(KEY_VER_REQUEST_UUID_HASH, uuidHash);
        intent.putExtra(KEY_VER_REQUEST_NAME, name);
        if (!TextUtils.isEmpty(myName)) {
            intent.putExtra(KEY_VER_MY_NAME_FOR_RESEND, myName);
        }
        if (!TextUtils.isEmpty(targetUUIDHash)) {
            intent.putExtra(KEY_VER_UUID_HASH_FOR_RESEND, targetUUIDHash);
        }
        intent.putExtra(KEY_VER_REQUEST_FCM_TOKEN, fcmToken);
        intent.putExtra(KEY_VER_REQUEST_WHISPER_PUB, whisperPub);
        intent.putExtra(KEY_VER_REQUEST_PUSHY_TOKEN, pushyToken);
        intent.putExtra(KEY_VER_REQUEST_PUBLIC_KEY, publicKey);
        intent.putExtra(KEY_VER_REQUEST_TZID_HASH, tzIdHash);
        intent.putExtra(KEY_VER_REQUEST_IS_TEST, isTest);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.verification_request_activity);

        android.support.v7.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        linkUIComponents();

        // The intent is from app link
        if (!initVariable(getIntent())) {
            LogUtil.logError(
                    TAG, "initVariable error", new IllegalStateException("initVariable error"));
            finish();
            return;
        }
        if (checkIfValidBackupSource()) {
            permissionCheck();
        }
        initViewContain();
        checkPreviousStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpReceiver();
        if (isLoadingViewShowing() && !mHasReceivedOk.get()) {
            checkPreviousStatus();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeReceiver();
    }

    @Override
    protected void onDestroy() {
        cancelCountDownTimer();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    //    @Override
    //    protected void onNewIntent(Intent intent) {
    //        super.onNewIntent(intent);
    //        setIntent(intent);
    //        final Bundle bundleFromAppLink = intent.getExtras();
    //        if (bundleFromAppLink == null) {
    //            LogUtil.logError(TAG, "bundleFromAppLink is null");
    //            return;
    //        }
    //
    //        permissionCheck();
    //        if (!initVariable(bundleFromAppLink)) {
    //            LogUtil.logError(TAG, "initVariable error");
    //            finish();
    //            return;
    //        }
    //        initViewContain();
    //        clearBackupDoneDialog();
    //        checkPreviousStatus(bundleFromAppLink);
    //    }

    private void permissionCheck() {
        if (ZionSkrSdkManager.getInstance().getIsDebug()) {
            // Permission check for getting phone number and external storage
            if (checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS)
                    != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{
                                Manifest.permission.READ_PHONE_NUMBERS,
                                Manifest.permission.READ_PHONE_STATE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        PERMISSION_REQUIREMENT);
            } else {
                mMyPhoneNumber = PhoneUtil.getNumber(this);
            }
            return;
        }
        // Permission check for getting phone number
        if (checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS)
                != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.READ_PHONE_NUMBERS,
                            Manifest.permission.READ_PHONE_STATE
                    },
                    PERMISSION_REQUIREMENT);
        } else {
            mMyPhoneNumber = PhoneUtil.getNumber(this);
        }
    }

    private boolean initVariable(@NonNull Intent intent) {
        if (intent == null) {
            LogUtil.logError(TAG, "intent is null", new IllegalArgumentException("intent is null"));
            return false;
        }

        mSenderToken = intent.getStringExtra(KEY_VER_REQUEST_FCM_TOKEN);
        mSenderWhisperPub = intent.getStringExtra(KEY_VER_REQUEST_WHISPER_PUB);
        mSenderPushyToken = intent.getStringExtra(KEY_VER_REQUEST_PUSHY_TOKEN);

        mSenderPublicKey = intent.getStringExtra(KEY_VER_REQUEST_PUBLIC_KEY);
        if (TextUtils.isEmpty(mSenderPublicKey)) {
            LogUtil.logError(TAG, "mSenderPublicKey is empty or null");
            return false;
        }

        mSenderEmailHash = intent.getStringExtra(KEY_VER_REQUEST_EMAIL_HASH);
        if (TextUtils.isEmpty(mSenderEmailHash)) {
            LogUtil.logError(TAG, "mSenderEmailHash is empty or null");
            return false;
        }

        mSenderName = intent.getStringExtra(KEY_VER_REQUEST_NAME);
        if (TextUtils.isEmpty(mSenderName)) {
            LogUtil.logError(TAG, "mSenderName is empty or null");
            return false;
        }

        mSenderUUIDHash = intent.getStringExtra(KEY_VER_REQUEST_UUID_HASH);
        if (TextUtils.isEmpty(mSenderUUIDHash)) {
            LogUtil.logError(TAG, "mSenderUUIDHash is empty or null");
            return false;
        }

        // For resending link
        mMyNameForResend = intent.getStringExtra(KEY_VER_MY_NAME_FOR_RESEND);
        if (!TextUtils.isEmpty(mMyNameForResend)) {
            LogUtil.logDebug(TAG, "Resend flow, mMyNameForResend is not null");
            mEtName.setText(mMyNameForResend);
            mIsResend = true;
        }

        // For resending link, real Bad
        mUUIDHashForResend = intent.getStringExtra(KEY_VER_UUID_HASH_FOR_RESEND);
        if (!TextUtils.isEmpty(mUUIDHashForResend)) {
            LogUtil.logDebug(TAG, "mUUIDHashForResend is not null");
        }

        // Check empty, move to checkPreviousStatus
        mSenderTZIDHash = intent.getStringExtra(KEY_VER_REQUEST_TZID_HASH);

        mIsTest = intent.getBooleanExtra(KEY_VER_REQUEST_IS_TEST, false);
        if (mIsTest) {
            LogUtil.logDebug(TAG, "mIsTest is true.");
        }

        return true;
    }

    private void checkPreviousStatus() {
        // Check if backup finished
        BackupSourceUtil.getWithUUIDHash(
                this,
                mSenderEmailHash,
                mSenderUUIDHash,
                new LoadDataListener() {
                    @Override
                    public void onLoadFinished(
                            final BackupSourceEntity backupSourceEntity,
                            BackupTargetEntity backupTargetEntity,
                            RestoreSourceEntity restoreSourceEntity,
                            RestoreTargetEntity restoreTargetEntity) {
                        if (backupSourceEntity != null) {
                            switch (backupSourceEntity.getStatus()) {
                                case BACKUP_SOURCE_STATUS_REQUEST:
                                case BACKUP_SOURCE_STATUS_OK:
                                    LogUtil.logError(
                                            TAG,
                                            "Incorrect status",
                                            new IllegalStateException("Incorrect status"));
                                    runOnUiThread(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    finish();
                                                }
                                            });
                                    break;
                                case BACKUP_SOURCE_STATUS_DONE:
                                    LogUtil.logDebug(TAG, "backupSource already exists.");

                                    showLoadingView(backupSourceEntity.getName());

                                    Map<String, String> msgToSend = new ArrayMap<>();
                                    msgToSend.put(KEY_CHECKSUM, backupSourceEntity.getCheckSum());
                                    msgToSend.put(KEY_ENCRYPTED_SEED, backupSourceEntity.getSeed());
                                    msgToSend.put(
                                            KEY_PUBLIC_KEY, backupSourceEntity.getPublicKey());
                                    msgToSend.put(KEY_NAME, backupSourceEntity.getMyName());
                                    // Used for notifying Amy to check if the backupTarget of Bob
                                    // has been deleted.
                                    new BackupHealthReportAction()
                                            .send(
                                                    VerificationRequestActivity.this,
                                                    mSenderToken,
                                                    mSenderWhisperPub,
                                                    mSenderPushyToken,
                                                    msgToSend);
                                    break;
                                case BACKUP_SOURCE_STATUS_REQUEST_AUTO_BACKUP:
                                    // In this case, we also show Bob already backup
                                    // If need to backup again, Amy need delete Bob manually
                                    // Otherwise, wait CheckAutoBackupJobIntentService remove it and
                                    // notify Amy after 1 hour
                                    LogUtil.logDebug(
                                            TAG,
                                            "In status "
                                                    + BACKUP_SOURCE_STATUS_REQUEST_AUTO_BACKUP
                                                    + ", emailHash = "
                                                    + mSenderEmailHash.substring(0, 5).hashCode()
                                                    + ", uuidHash = "
                                                    + mSenderUUIDHash.substring(0, 5).hashCode());
                                    runOnUiThread(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    showBackupDoneDialog(
                                                            backupSourceEntity.getName());
                                                }
                                            });
                                    break;
                                default:
                                    LogUtil.logError(
                                            TAG,
                                            "incorrect previous status",
                                            new IllegalStateException("incorrect previous status"));
                                    runOnUiThread(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    finish();
                                                }
                                            });
                                    break;
                            }
                        } else {
                            setLoadingViewVisibility(false);
                            setKeyboardVisibility(true);

                            if (TextUtils.isEmpty(mSenderTZIDHash)) {
                                LogUtil.logError(
                                        TAG,
                                        "mSenderTZIDHash is empty or null",
                                        new IllegalStateException());
                                runOnUiThread(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                finish();
                                            }
                                        });
                            }
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUIREMENT) {
            mMyPhoneNumber = PhoneUtil.getNumber(this);
        }
    }

    private void linkUIComponents() {
        mTvRequestTitle = findViewById(R.id.tv_request_title);
        mTvStep1 = findViewById(R.id.tv_step1);
        mIbPrevious = findViewById(R.id.ib_previous);
        mIbPrevious.setOnClickListener(mClickListener);
        mBtnSendRequest = findViewById(R.id.btn_send_request);
        mBtnSendRequest.setOnClickListener(mClickListener);
        mEtName = findViewById(R.id.et_enter_name);
        setNameInputFilter();
    }

    private void initViewContain() {
        String requestTitle =
                String.format(getResources().getString(R.string.ver_content_title), mSenderName);
        mTvRequestTitle.setText(requestTitle);

        String requestStep1 =
                String.format(getResources().getString(R.string.ver_step1), mSenderName);
        mTvStep1.setText(requestStep1);

        if (TextUtils.isEmpty(mMyNameForResend)) {
            mEtName.setText(EMPTY_STRING);
        }
    }

    private void clearBackupDoneDialog() {
        synchronized (this) {
            if (mBackupDoneDialog != null) {
                if (mBackupDoneDialog.isShowing()) {
                    mBackupDoneDialog.dismiss();
                }
                mBackupDoneDialog = null;
            }
        }
    }

    private void showBackupDoneDialog(@NonNull String name) {
        if (TextUtils.isEmpty(name)) {
            LogUtil.logError(TAG, "name is empty or null");
            name = getResources().getString(R.string.ver_unknown_name);
        }

        final String title =
                String.format(
                        getResources().getString(R.string.ver_dialog_backup_done_title), name);

        clearBackupDoneDialog();
        synchronized (this) {
            if (mBackupDoneDialog == null) {
                mBackupDoneDialog =
                        new AlertDialog.Builder(this)
                                .setTitle(title)
                                .setMessage(
                                        String.format(
                                                getResources()
                                                        .getString(
                                                                R.string
                                                                        .ver_dialog_backup_done_msg),
                                                name))
                                .setCancelable(false)
                                .setPositiveButton(
                                        R.string.ver_dialog_btn_ok,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                onBackPressed();
                                                dialog.dismiss();
                                            }
                                        })
                                .create();
                mBackupDoneDialog.setCanceledOnTouchOutside(false);
                mBackupDoneDialog.show();
            }
        }
    }

    private void setUpReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_RECEIVE_REPORT_OK);
        intentFilter.addAction(ACTION_RECEIVE_DELETE);
        // Receiving the report ok message from Amy means Amy does still have the backupTargetEntity
        // of Bob.
        mBroadcastReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent == null) {
                            LogUtil.logDebug(TAG, "intent is null");
                            return;
                        }

                        String action = intent.getAction();
                        if (TextUtils.isEmpty(action)) {
                            LogUtil.logDebug(TAG, "action is empty");
                            return;
                        }

                        switch (action) {
                            case ACTION_RECEIVE_REPORT_OK:
                                String fcmToken = intent.getStringExtra(KEY_TOKEN);
                                String whisperPub = intent.getStringExtra(KEY_WHISPER_PUB);
                                // To cover all the MESSAGING_SERVICE_TYPE
                                if ((!TextUtils.isEmpty(fcmToken) && fcmToken.equals(mSenderToken))
                                        || (!TextUtils.isEmpty(whisperPub)
                                        && whisperPub.equals(mSenderWhisperPub))) {
                                    if (mHasReceivedOk.get()) {
                                        LogUtil.logDebug(
                                                TAG, "Already received report ok, ignore it.");
                                        break;
                                    }
                                    setLoadingViewVisibility(false);
                                    cancelCountDownTimer();
                                    mHasReceivedOk.set(true);
                                    showBackupDoneDialog(mSenderName);
                                }
                                break;
                            case ACTION_RECEIVE_DELETE:
                                String uuidHash = intent.getStringExtra(KEY_UUID_HASH);
                                // Need to ensure that the Action is sent from the current link's
                                // sender by checking the uuid hash.
                                if (!TextUtils.isEmpty(uuidHash)
                                        && uuidHash.equals(mSenderUUIDHash)) {
                                    setLoadingViewVisibility(false);
                                    setKeyboardVisibility(true);
                                    cancelCountDownTimer();
                                }
                                break;
                            default:
                                LogUtil.logError(
                                        TAG, "No matching action, received action=" + action);
                                break;
                        }
                    }
                };
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void removeReceiver() {
        if (mBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        }
    }

    private void showLoadingView(@NonNull final String name) {
        if (TextUtils.isEmpty(name)) {
            LogUtil.logError(TAG, "name is empty", new IllegalStateException("name is empty"));
            return;
        }
        synchronized (this) {
            if (isLoadingViewShowing()) {
                return;
            }
            runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            setKeyboardVisibility(false);
                            setLoadingViewVisibility(true);

                            cancelCountDownTimer();
                            mCountDownTimer =
                                    new CountDownTimer(
                                            HEALTH_CHECK_TIMEOUT, DateUtils.SECOND_IN_MILLIS) {
                                        @Override
                                        public void onTick(long millisUntilFinished) {
                                        }

                                        @Override
                                        public void onFinish() {
                                            setLoadingViewVisibility(false);
                                            // TODO What should be really showing up if timeout?
                                            showBackupDoneDialog(name);
                                        }
                                    }.start();
                        }
                    });
        }
    }

    private void cancelCountDownTimer() {
        synchronized (this) {
            if (mCountDownTimer != null) {
                mCountDownTimer.cancel();
            }
        }
    }

    // Prevent Amy from backing up data for herself
    private boolean checkIfValidBackupSource() {
        final String myUUIDHash = PhoneUtil.getSKRIDHash(this);
        if (mSenderUUIDHash.equals(myUUIDHash)) {
            new AlertDialog.Builder(this)
                    .setMessage(getResources().getString(R.string.backup_backup_on_same_device))
                    .setCancelable(false)
                    .setPositiveButton(
                            R.string.ver_dialog_btn_ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                    dialog.dismiss();
                                }
                            })
                    .show();
            return false;
        } else {
            return true;
        }
    }

    private void setNameInputFilter() {
        mEtName.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        if (editable.toString().equals(EMPTY_STRING)) {
                            mBtnSendRequest.setEnabled(false);
                        }
                    }
                });

        mEtName.setFilters(
                new InputFilter[]{
                        new InputFilter() {
                            @Override
                            public CharSequence filter(
                                    CharSequence source,
                                    int start,
                                    int end,
                                    Spanned dest,
                                    int dstart,
                                    int dend) {
                                boolean keepOriginal = true;
                                StringBuffer stringBuffer = new StringBuffer();
                                for (int i = start; i < end; i++) {
                                    char c = source.charAt(i);
                                    if (isLegalChar(c)) {
                                        mBtnSendRequest.setEnabled(true);
                                        stringBuffer.append(c);
                                    } else {
                                        keepOriginal = false;
                                    }
                                }

                                if (keepOriginal) {
                                    return null;
                                } else {
                                    return stringBuffer;
                                }
                            }
                        },
                        new InputFilter.LengthFilter(MAX_NAME_LENGTH)
                });
    }

    private void request() {
        mMyName = mEtName.getText().toString();

        Map<String, String> map = new ArrayMap<>();
        // Source (Amy)
        map.put(KEY_EMAIL_HASH, mSenderEmailHash);
        map.put(KEY_SOURCE_NAME, mSenderName);
        map.put(KEY_UUID_HASH, mSenderUUIDHash);
        map.put(KEY_PUBLIC_KEY, mSenderPublicKey);
        map.put(KEY_TZID_HASH, mSenderTZIDHash);
        map.put(KEY_IS_TEST, String.valueOf(mIsTest));
        // Target (Bob)
        map.put(KEY_NAME, mMyName);
        map.put(KEY_TARGET_NAME, mMyNameForResend);
        map.put(KEY_TARGET_UUID_HASH, mUUIDHashForResend);
        map.put(KEY_PHONE_NUMBER, mMyPhoneNumber);
        if (mIsResend) {
            map.put(KEY_IS_RESEND, MSG_IS_RESEND);
        }
        new BackupRequestAction()
                .send(this, mSenderToken, mSenderWhisperPub, mSenderPushyToken, map);

        Intent intent =
                VerificationCodeActivity.generateIntent(
                        this,
                        mMyName,
                        mMyNameForResend,
                        mUUIDHashForResend,
                        mSenderEmailHash,
                        mSenderUUIDHash,
                        mSenderName,
                        mSenderToken,
                        mSenderWhisperPub,
                        mSenderPushyToken,
                        mSenderPublicKey,
                        mSenderTZIDHash,
                        System.currentTimeMillis(),
                        INIT_PIN_CODE,
                        PIN_CODE_NO_ERROR,
                        INIT_RETRY_TIMES,
                        INIT_RETRY_WAIT_START_TIME,
                        INIT_LAST_VERIFY_TIME,
                        false,
                        mIsTest);
        startVerificationCodeActivity(intent);
    }

    private void startVerificationCodeActivity(Intent intent) {
        if (intent == null) {
            LogUtil.logError(TAG, "intent is null", new IllegalStateException("intent is null"));
            return;
        }
        startActivity(intent);
        finish();
    }

    private boolean isLegalChar(@NonNull Character character) {
        if (character == null) {
            LogUtil.logError(TAG, "character is null");
            return false;
        }
        Pattern regex = Pattern.compile("[$&+,:;=\\\\?@#|/'<>.^*()%!\"-]");
        return !regex.matcher(character.toString()).find();
    }
}
