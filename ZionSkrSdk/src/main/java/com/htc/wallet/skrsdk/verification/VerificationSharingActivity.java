package com.htc.wallet.skrsdk.verification;

import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_CLOSE_SHARING_ACTIVITY;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_NOTIFY_PINCODE_UPDATE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.SocialBaseActivity;
import com.htc.wallet.skrsdk.action.Action;
import com.htc.wallet.skrsdk.action.RestoreDeleteAction;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.util.RestoreTargetUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PinCodeUtil;

import java.util.Map;
import java.util.Objects;

public class VerificationSharingActivity extends SocialBaseActivity {
    private static final String TAG = "VerificationSharingActivity";

    private static final String KEY_VER_SHARING_EMAIL_HASH = "key_ver_sharing_email_hash";
    private static final String KEY_VER_SHARING_UUID_HASH = "key_ver_sharing_uuid_hash";
    private static final String KEY_VER_SHARING_FCM_TOKEN = "key_ver_sharing_fcm_token";
    private static final String KEY_VER_SHARING_WHISPER_PUB = "key_ver_sharing_whisper_pub";
    private static final String KEY_VER_SHARING_PUSHY_TOKEN = "key_ver_sharing_pushy_token";
    private static final String KEY_VER_SHARING_NAME = "key_ver_sharing_name";
    private static final String KEY_VER_SHARING_PIN_CODE = "key_ver_sharing_pin_code";
    private static final String KEY_VER_SHARING_PHONE_MODEL = "key_ver_sharing_phone_model";
    private static final int[] PINCODE_DIGIT_ID =
            new int[]{
                    R.id.tv_pin_digit1, R.id.tv_pin_digit2, R.id.tv_pin_digit3,
                    R.id.tv_pin_digit4, R.id.tv_pin_digit5, R.id.tv_pin_digit6,
            };
    private String mEmailHash;
    private String mUUIDHash;
    private String mFcmToken;
    private String mWhisperPub;
    private String mPushyToken;
    private String mName;
    private String mPINCodeString;
    private String mPhoneModel;
    // on toolbar
    private ImageButton mIbCancel;
    private TextView mTvShareQRTitle;
    private TextView[] mTvPinCode = new TextView[PINCODE_DIGIT_ID.length];
    private TextView mTvRemindName;
    private TextView mTvRemindRoute;
    private TextView mTvReject;
    private boolean mNeedRefresh = false;

    private LocalBroadcastManager mActionBroadcast;
    private BroadcastReceiver mActionReceiver;
    private View.OnClickListener mOnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int i = v.getId();
                    if (i == R.id.tv_reject) {
                        Map<String, String> map = new ArrayMap<>();
                        map.put(Action.KEY_EMAIL_HASH, mEmailHash);
                        map.put(Action.KEY_UUID_HASH, mUUIDHash);
                        new RestoreDeleteAction().send(VerificationSharingActivity.this, mFcmToken,
                                mWhisperPub, mPushyToken, map);
                        // To prevent double clicked
                        mTvReject.setClickable(false);
                        finish();

                    } else if (i == R.id.ib_close) {
                        onBackPressed();
                    }
                }
            };

    @NonNull
    public static Intent generateIntent(
            @NonNull Context context,
            @Nullable String emailHash,
            @NonNull String uuidHash,
            @NonNull String fcmToken,
            @NonNull String whisperPub,
            @NonNull String pushyToken,
            @NonNull String name,
            @NonNull String pinCode,
            @Nullable String phoneModel) {
        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(name)) {
            LogUtil.logError(TAG, "name is empty", new IllegalArgumentException("name is empty"));
            return new Intent();
        }
        Intent intent = new Intent(context, VerificationSharingActivity.class);
        intent.putExtra(KEY_VER_SHARING_EMAIL_HASH, emailHash);
        intent.putExtra(KEY_VER_SHARING_UUID_HASH, uuidHash);
        intent.putExtra(KEY_VER_SHARING_FCM_TOKEN, fcmToken);
        intent.putExtra(KEY_VER_SHARING_WHISPER_PUB, whisperPub);
        intent.putExtra(KEY_VER_SHARING_PUSHY_TOKEN, pushyToken);
        intent.putExtra(KEY_VER_SHARING_NAME, name);
        intent.putExtra(KEY_VER_SHARING_PIN_CODE, pinCode);
        intent.putExtra(KEY_VER_SHARING_PHONE_MODEL, phoneModel);
        return intent;
    }

    private void initVariable(@NonNull Intent intent) {
        if (intent == null) {
            LogUtil.logError(TAG, "intent is null");
            finish();
        }

        mActionBroadcast = LocalBroadcastManager.getInstance(this);

        // init Variable
        mEmailHash = intent.getStringExtra(KEY_VER_SHARING_EMAIL_HASH);
        if (TextUtils.isEmpty(mEmailHash)) {
            LogUtil.logError(
                    TAG, "Email hash is empty!", new IllegalStateException("Email hash is empty!"));
            return;
        }
        mUUIDHash = intent.getStringExtra(KEY_VER_SHARING_UUID_HASH);
        if (TextUtils.isEmpty(mUUIDHash)) {
            LogUtil.logError(
                    TAG, "UUID hash is empty!", new IllegalStateException("UUID hash is empty!"));
            return;
        }
        mFcmToken = intent.getStringExtra(KEY_VER_SHARING_FCM_TOKEN);
        mWhisperPub = intent.getStringExtra(KEY_VER_SHARING_WHISPER_PUB);
        mPushyToken = intent.getStringExtra(KEY_VER_SHARING_PUSHY_TOKEN);

        mName = intent.getStringExtra(KEY_VER_SHARING_NAME);
        mPhoneModel = intent.getStringExtra(KEY_VER_SHARING_PHONE_MODEL);
        mPINCodeString = intent.getStringExtra(KEY_VER_SHARING_PIN_CODE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.verification_sharing_activity);

        initVariable(getIntent());
        initUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNeedRefresh) {
            mNeedRefresh = false;
            checkRestoreTargetPinCode();
        }
        createAndRegisterOkActionReceiver();
    }

    private void checkRestoreTargetPinCode() {
        RestoreTargetUtil.get(
                this,
                mEmailHash,
                mUUIDHash,
                new LoadDataListener() {
                    @Override
                    public void onLoadFinished(
                            BackupSourceEntity backupSourceEntity,
                            BackupTargetEntity backupTargetEntity,
                            RestoreSourceEntity restoreSourceEntity,
                            final RestoreTargetEntity restoreTargetEntity) {
                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        if (restoreTargetEntity == null) {
                                            onBackPressed();
                                        } else {
                                            updatePinCode(restoreTargetEntity.getPinCode(), false);
                                        }
                                    }
                                });
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mActionReceiver == null) {
            LogUtil.logError(TAG, "mActionReceiver is null");
            return;
        }

        if (mActionBroadcast == null) {
            LogUtil.logError(TAG, "mActionBroadcast is null");
            return;
        }
        mActionBroadcast.unregisterReceiver(mActionReceiver);
        mNeedRefresh = true;
    }

    private void createAndRegisterOkActionReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_CLOSE_SHARING_ACTIVITY);
        intentFilter.addAction(ACTION_NOTIFY_PINCODE_UPDATE);

        mActionReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent == null) {
                            LogUtil.logError(TAG, "intent is null");
                            return;
                        }

                        // Check UUID Hash
                        String uuidHash = intent.getStringExtra(Action.KEY_UUID_HASH);
                        if (TextUtils.isEmpty(uuidHash)) {
                            LogUtil.logError(TAG, "uuidHash is null or empty");
                            return;
                        }
                        if (mUUIDHash == null || !mUUIDHash.equals(uuidHash)) {
                            LogUtil.logDebug(TAG, "uuidHash not match, ignore it");
                            return;
                        }

                        // Check Email Hash
                        String emailHash = intent.getStringExtra(Action.KEY_EMAIL_HASH);
                        if (TextUtils.isEmpty(emailHash)) {
                            LogUtil.logError(TAG, "emailHash is null or empty");
                            return;
                        }
                        if (mEmailHash == null || !mEmailHash.equals(emailHash)) {
                            LogUtil.logDebug(TAG, "emailHash not match, ignore it");
                            return;
                        }

                        String actionStr = intent.getAction();
                        // Not need to check empty
                        if (actionStr == null) {
                            LogUtil.logError(TAG, "actionStr is null");
                            return;
                        }
                        switch (actionStr) {
                            case ACTION_CLOSE_SHARING_ACTIVITY:
                                finish();
                                break;
                            case ACTION_NOTIFY_PINCODE_UPDATE:
                                String pinCode = intent.getStringExtra(Action.KEY_PIN_CODE);
                                updatePinCode(pinCode, false);
                                break;
                            default:
                                LogUtil.logError(TAG, "unknown action");
                        }
                    }
                };
        if (mActionBroadcast == null) {
            LogUtil.logError(TAG, "mActionBroadcast is null");
            return;
        }
        mActionBroadcast.registerReceiver(mActionReceiver, intentFilter);
    }

    private void initUI() {

        Resources resources = getResources();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mIbCancel = findViewById(R.id.ib_close);
        mIbCancel.setOnClickListener(mOnClickListener);

        mTvShareQRTitle = findViewById(R.id.tv_share_qr_title);
        String shareQrTitle = String.format(resources.getString(R.string.ver_sharing_title), mName);
        mTvShareQRTitle.setText(shareQrTitle);

        for (int idx = 0; idx < PINCODE_DIGIT_ID.length; idx++) {
            mTvPinCode[idx] = findViewById(PINCODE_DIGIT_ID[idx]);
        }
        updatePinCode(mPINCodeString, true);

        mTvRemindName = findViewById(R.id.tv_remind_name);
        mTvRemindName.setText(mName);

        String remindRoute =
                String.format(
                        resources.getString(R.string.ver_sharing_remind_route),
                        getResources().getString(R.string.ver_unknown_phone_number),
                        TextUtils.isEmpty(mPhoneModel)
                                ? getResources().getString(R.string.ver_unknown_phone_number)
                                : mPhoneModel);
        mTvRemindRoute = findViewById(R.id.tv_remind_route);
        mTvRemindRoute.setText(remindRoute);

        mTvReject = findViewById(R.id.tv_reject);
        mTvReject.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        mTvReject.setOnClickListener(mOnClickListener);
    }

    private void updatePinCode(String pinCode, boolean isInit) {
        if (!PinCodeUtil.isValidPinCode(pinCode)) {
            Log.e(TAG, "pinCode is incorrect or empty");
            pinCode = "";
        }

        if (!isInit && mPINCodeString.equals(pinCode)) {
            return;
        }
        mPINCodeString = pinCode;
        for (int idx = 0; idx < PINCODE_DIGIT_ID.length; idx++) {
            mTvPinCode[idx].setText(pinCode.substring(idx, idx + 1));
        }
    }
}
