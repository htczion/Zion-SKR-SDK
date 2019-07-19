package com.htc.wallet.skrsdk.restore;

import static com.htc.wallet.skrsdk.action.Action.KEY_RESTORE_SOURCE_PIN_CODE_POSITION;
import static com.htc.wallet.skrsdk.action.Action.KEY_RESTORE_TARGET_EMAIL_HASH;
import static com.htc.wallet.skrsdk.action.Action.KEY_RESTORE_TARGET_ENCRYPTED_TOKEN;
import static com.htc.wallet.skrsdk.action.Action.KEY_RESTORE_TARGET_ENCRYPTED_UUID;
import static com.htc.wallet.skrsdk.action.Action.KEY_RESTORE_TARGET_ENC_CODE;
import static com.htc.wallet.skrsdk.action.Action.KEY_RESTORE_TARGET_ENC_CODE_SIGN;
import static com.htc.wallet.skrsdk.action.Action.KEY_RESTORE_TARGET_ENC_PUSHY_TOKEN;
import static com.htc.wallet.skrsdk.action.Action.KEY_RESTORE_TARGET_ENC_WHISPER_PUB;
import static com.htc.wallet.skrsdk.action.Action.KEY_VERIFY_TIMESTAMP;
import static com.htc.wallet.skrsdk.messaging.processor.MessageProcessorFactory.PUSHY_MESSAGE_PROCESSOR;
import static com.htc.wallet.skrsdk.restore.PinCodeStatusConstant.FAIL;
import static com.htc.wallet.skrsdk.restore.PinCodeStatusConstant.NOT_COMPLETE;
import static com.htc.wallet.skrsdk.restore.PinCodeStatusConstant.SUCCESS;
import static com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity.RESTORE_SOURCE_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_RESTORE_NOTIFY_UI_UPDATE_AFTER_ERROR_PINCODE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.action.RestoreVerifyAction;
import com.htc.wallet.skrsdk.crypto.util.VerificationUtil;
import com.htc.wallet.skrsdk.messaging.MultiTokenListener;
import com.htc.wallet.skrsdk.messaging.processor.MessageProcessorFactory;
import com.htc.wallet.skrsdk.messaging.processor.MessageProcessorWrapper;
import com.htc.wallet.skrsdk.messaging.processor.PushyMessageProcessor;
import com.htc.wallet.skrsdk.restore.util.SkrRestoreEditTextStatus;
import com.htc.wallet.skrsdk.restore.util.SkrRestoreEditTextStatusUtil;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.sqlite.util.RestoreSourceUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;
import com.htc.wallet.skrsdk.whisper.WhisperKeyPair;
import com.htc.wallet.skrsdk.whisper.WhisperUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RestoreVerificationCodeView extends RelativeLayout {
    private static final String TAG = "RestoreVerificationCodeView";

    public static final int VERIFY_TIME_OUT = 60 * 1000;
    private static final int VERIFY_TIME_OUT_INTERVAL = 1000;
    private static final int WAIT_TIMEOUT = 1; // second, for wait if nothing can be sent
    public static final int WAIT_TIMES = 20; // The number of wait and check times

    private static final int[] ET_PIN_IDS = new int[]{R.id.et_ver1, R.id.et_ver2, R.id.et_ver3};
    public static final int ET_PIN_SIZE = ET_PIN_IDS.length;

    private static final int[] IV_CHECK_IDS = new int[]{R.id.iv_ver1, R.id.iv_ver2, R.id.iv_ver3};
    private static final int[] PB_IDS = new int[]{R.id.pb1, R.id.pb2, R.id.pb3};

    private final Context mContext;

    EditText[] etPinShow;
    private ImageView[] imageView = new ImageView[IV_CHECK_IDS.length];
    private ProgressBar[] progressBars = new ProgressBar[PB_IDS.length];

    private TextView mTvValidError;
    private String mEmailHash;
    private CountDownTimer[] mCountDownTimers = new CountDownTimer[ET_PIN_SIZE];
    private int[] mCurrentPinCodePositionReceivedCount = new int[ET_PIN_SIZE];
    private int[] mCurrentPinCodePositionReceivedMaxCount = new int[ET_PIN_SIZE];
    private int[] mPinCodeStatuses = new int[ET_PIN_SIZE];
    private int mRetryTime = 0;

    public RestoreVerificationCodeView(Context context) {
        this(context, null);
    }

    public RestoreVerificationCodeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RestoreVerificationCodeView(Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        initComponents(context);
    }

    private void initComponents(final Context context) {
        inflate(context, R.layout.social_restore_verification_code_view, this);
        mTvValidError = findViewById(R.id.tv_restore_valid_error_text);
        etPinShow = new EditText[ET_PIN_SIZE];
        for (int i = 0; i < ET_PIN_SIZE; i++) {
            etPinShow[i] = this.findViewById(ET_PIN_IDS[i]);
        }

        for (int i = 0; i < IV_CHECK_IDS.length; i++) {
            imageView[i] = this.findViewById(IV_CHECK_IDS[i]);
        }
        for (int i = 0; i < PB_IDS.length; i++) {
            progressBars[i] = this.findViewById(PB_IDS[i]);
        }

        for (int i = 0; i < ET_PIN_SIZE; i++) {
            mCurrentPinCodePositionReceivedCount[i] = 0;
            mCurrentPinCodePositionReceivedMaxCount[i] = 0;
        }
    }

    void setValidErrorVisibility(int visibility) {
        mTvValidError.setVisibility(visibility);
    }

    void refreshValidErrorVisibility() {
        int nonErrCount = 0;
        for (int i = 0; i < getPinCodeEditTextSize(); i++) {
            int pinStatus = getPinCodeStatus(i);
            if (pinStatus == SUCCESS || pinStatus == NOT_COMPLETE) {
                nonErrCount++;
            }
        }
        if (nonErrCount == getPinCodeEditTextSize()) {
            setValidErrorVisibility(INVISIBLE);
        } else {
            setValidErrorVisibility(VISIBLE);
        }
    }

    private void setPinCorrectStyle(int position) {
        etPinShow[position].setBackgroundResource(
                R.drawable.shape_social_restore_verification_view);
        etPinShow[position].setTextColor(
                ContextCompat.getColor(mContext, R.color.dark_primaryfont_color));
        imageView[position].setVisibility(VISIBLE);
    }

    private void setPinNotCompletedStyle(int position) {
        etPinShow[position].setBackgroundResource(
                R.drawable.shape_social_restore_verification_view);
        etPinShow[position].setTextColor(
                ContextCompat.getColor(mContext, R.color.dark_primaryfont_color));
    }

    private void setPinErrorStyle(int position) {
        imageView[position].setVisibility(INVISIBLE);
        setValidErrorVisibility(VISIBLE);
        etPinShow[position].setBackgroundResource(
                R.drawable.shape_social_restore_verification_error_view);
        etPinShow[position].setTextColor(
                ContextCompat.getColor(mContext, R.color.social_restore_error_pincode_color));
    }

    public void setEmailHash(final String emailHash) {
        if (TextUtils.isEmpty(emailHash)) {
            LogUtil.logDebug(TAG, "emailHash is null");
            return;
        }
        mEmailHash = emailHash;
    }

    private Map<String, String> createMessagesToRestoreSources(
            final String encryptedUUID,
            final String encCode,
            final String encCodeSigned,
            final String encryptedRestoreTargetToken,
            final String encRestoreTargetWhisperPub,
            final String encRestoreTargetPushyToken,
            int position) {
        final Map<String, String> messagesToRestoreSources = new HashMap<>();
        if (TextUtils.isEmpty(encryptedUUID) || TextUtils.isEmpty(encCode)) {
            LogUtil.logDebug(TAG,
                    "createMessagesToRestoreSources, encryptedUUID, encryptedPinCode is null");
            return messagesToRestoreSources;
        }
        messagesToRestoreSources.put(KEY_RESTORE_TARGET_ENCRYPTED_UUID, encryptedUUID);
        messagesToRestoreSources.put(KEY_RESTORE_TARGET_EMAIL_HASH, mEmailHash);
        messagesToRestoreSources.put(KEY_RESTORE_TARGET_ENC_CODE, encCode);
        messagesToRestoreSources.put(KEY_RESTORE_TARGET_ENC_CODE_SIGN, encCodeSigned);
        messagesToRestoreSources.put(KEY_RESTORE_SOURCE_PIN_CODE_POSITION,
                String.valueOf(position));
        messagesToRestoreSources.put(KEY_RESTORE_TARGET_ENCRYPTED_TOKEN,
                encryptedRestoreTargetToken);
        messagesToRestoreSources.put(KEY_RESTORE_TARGET_ENC_WHISPER_PUB,
                encRestoreTargetWhisperPub);
        messagesToRestoreSources.put(KEY_RESTORE_TARGET_ENC_PUSHY_TOKEN,
                encRestoreTargetPushyToken);

        // Put verify timestamp to message from SkrRestoreEditTextStatus
        SkrRestoreEditTextStatus skrRestoreEditTextStatus =
                SkrRestoreEditTextStatusUtil.getStatus(mContext, position);
        String timestampStr = String.valueOf(skrRestoreEditTextStatus.getSentTimeMs());
        messagesToRestoreSources.put(KEY_VERIFY_TIMESTAMP, timestampStr);

        return messagesToRestoreSources;
    }

    private void setPinCodeStatus(int position, @PinCodeStatus int status) {
        if (position >= ET_PIN_SIZE) {
            LogUtil.logDebug(TAG, "setPinCodeStatus, position is large than editText size");
            return;
        }
        mPinCodeStatuses[position] = status;
    }

    int getPinCodeStatus(int position) {
        if (position >= ET_PIN_SIZE) {
            LogUtil.logDebug(TAG, "setPinCodeStatus, position is large than editText size");
            return FAIL;
        }
        return mPinCodeStatuses[position];
    }

    private void setPinCodeEditTextEnabled(final int position, boolean enabled) {
        if (position >= etPinShow.length) {
            LogUtil.logDebug(TAG, "position is large than mTvPin size");
            return;
        }
        EditText editTextShow = etPinShow[position];
        if (editTextShow == null) {
            LogUtil.logDebug(TAG, "setPinCodeEditTextEnabled, editTextShow is null");
            return;
        }
        editTextShow.setEnabled(enabled);
    }

    int getPinCodeEditTextSize() {
        return ET_PIN_SIZE;
    }

    private void sendErrorPinCodeLocalBroadcast(int position) {
        if (isPinCodePositionIllegal(position)) {
            LogUtil.logDebug(TAG, "getPinCodeEditTextSize, error position");
            return;
        }
        Intent intent = new Intent(ACTION_RESTORE_NOTIFY_UI_UPDATE_AFTER_ERROR_PINCODE);
        intent.putExtra(KEY_RESTORE_SOURCE_PIN_CODE_POSITION, String.valueOf(position));
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private boolean isPinCodePositionIllegal(int position) {
        return (position >= ET_PIN_SIZE && position < 0);
    }

    @WorkerThread
    void sendPinCodeToAllRestoreSources(
            final String encCode, final String encCodeSign, final int position) {
        if (TextUtils.isEmpty(encCode) || TextUtils.isEmpty(encCodeSign)) {
            LogUtil.logDebug(TAG, "encCode or encCodeSign is empty");
            return;
        }

        // Put new SkrRestoreEditTextStatus to sharedPrefs
        final SkrRestoreEditTextStatus skrRestoreEditTextStatus = new SkrRestoreEditTextStatus();
        skrRestoreEditTextStatus.setSentTimeMs(System.currentTimeMillis());
        SkrRestoreEditTextStatusUtil.putStatus(mContext, position, skrRestoreEditTextStatus);

        // Wait a while if nothing can be sent
        waitIfNothingCanBeSent();

        RestoreSourceUtil.getAll(
                mContext,
                new LoadListListener() {
                    @Override
                    public void onLoadFinished(
                            List<BackupSourceEntity> backupSourceEntityList,
                            List<BackupTargetEntity> backupTargetEntityList,
                            List<RestoreSourceEntity> restoreSourceEntityList,
                            List<RestoreTargetEntity> restoreTargetEntityList) {
                        if (restoreSourceEntityList == null) {
                            LogUtil.logError(TAG, "restoreSourceEntityList is null",
                                    new IllegalArgumentException(
                                            "restoreSourceEntityList is null"));
                            return;
                        }
                        mCurrentPinCodePositionReceivedMaxCount[position] =
                                getCanBeSentRestoreSourcesNumber(restoreSourceEntityList);
                        if (mCurrentPinCodePositionReceivedMaxCount[position] == 0) {
                            LogUtil.logDebug(TAG, "sendPinCodeToAllRestoreSources, "
                                    + "mCurrentPinCodePositionReceivedMaxNumber is 0");
                            ((Activity) mContext).runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            setPinCodeErrorStyle(position);
                                        }
                                    });
                            return;
                        }

                        // Put max receive number to sharedPrefs
                        skrRestoreEditTextStatus.setReceivedPinMaxCount(
                                mCurrentPinCodePositionReceivedMaxCount[position]);
                        SkrRestoreEditTextStatusUtil.putStatus(mContext, position,
                                skrRestoreEditTextStatus);

                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                startCountdownTimer(position, VERIFY_TIME_OUT);
                            }
                        });

                        sendRestoreVerifyAction(restoreSourceEntityList, encCode, encCodeSign,
                                position);
                    }
                });
    }

    // Wait a while if nothing can be sent
    @WorkerThread
    private void waitIfNothingCanBeSent() {
        final AtomicBoolean found = new AtomicBoolean(false);
        for (int i = 0; i < WAIT_TIMES; i++) {
            final int times = i;
            final CountDownLatch latch = new CountDownLatch(1);
            RestoreSourceUtil.getAll(mContext, new LoadListListener() {
                @Override
                public void onLoadFinished(
                        List<BackupSourceEntity> backupSourceEntityList,
                        List<BackupTargetEntity> backupTargetEntityList,
                        List<RestoreSourceEntity> restoreSourceEntityList,
                        List<RestoreTargetEntity> restoreTargetEntityList) {
                    if (restoreSourceEntityList == null) {
                        LogUtil.logError(TAG, "restoreSourceEntityList is null",
                                new IllegalStateException());
                        latch.countDown();
                        return;
                    }
                    int canBeSentNumber = getCanBeSentRestoreSourcesNumber(restoreSourceEntityList);
                    if (canBeSentNumber > 0) {
                        LogUtil.logInfo(TAG, "Can be sent = " + canBeSentNumber + ", " + times);
                        found.set(true);
                        latch.countDown();
                    } else {
                        LogUtil.logInfo(TAG, "Nothing can be sent, wait a while ! " + times);
                    }
                }
            });
            try {
                latch.await(WAIT_TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LogUtil.logError(TAG, "InterruptedException e = " + e);
            }
            if (found.get()) {
                break;
            }
        }
    }

    @WorkerThread
    private void sendRestoreVerifyAction(
            final List<RestoreSourceEntity> restoreSources,
            final String encCode,
            final String encCodeSign,
            final int position) {
        final String uuid = PhoneUtil.getSKRID(mContext);
        if (restoreSources == null || TextUtils.isEmpty(uuid) || TextUtils.isEmpty(encCode)
                || TextUtils.isEmpty(encCodeSign)) {
            LogUtil.logDebug(TAG,
                    "sendRestoreVerifyAction. restoreSources, uuid, encCode or encCodeSign may be"
                            + " null");
            return;
        }
        final VerificationUtil verificationUtil = new VerificationUtil(false);
        final PushyMessageProcessor processor =
                (PushyMessageProcessor) MessageProcessorFactory.getInstance()
                        .getMessageProcessor(PUSHY_MESSAGE_PROCESSOR);
        if (processor == null) {
            LogUtil.logError(TAG, "processor is null",
                    new IllegalStateException("processor is null"));
            return;
        }

        for (final RestoreSourceEntity source : restoreSources) {
            if (!source.compareStatus(RESTORE_SOURCE_STATUS_REQUEST)) {
                continue;
            }
            final String restoreSourcePublicKey = source.getPublicKey();
            final String encryptedUUID = verificationUtil.encryptMessage(uuid,
                    restoreSourcePublicKey);

            final String sourceFcmToken = source.getFcmToken();
            final String sourceWhisperPub = source.getWhisperPub();
            final String sourcePushyToken = source.getPushyToken();

            MessageProcessorWrapper.getMultiToken(mContext, new MultiTokenListener() {
                @Override
                public void onUserTokenReceived(@Nullable String pushyToken,
                        @Nullable String fcmToken) {
                    final Map<String, String> messagesToRestoreSources;

                    if (!TextUtils.isEmpty(sourceWhisperPub) && !TextUtils.isEmpty(
                            sourcePushyToken)) {
                        WhisperKeyPair whisperKeyPair = WhisperUtils.getKeyPair(mContext);
                        String whisperPub = null;
                        if (whisperKeyPair != null) {
                            whisperPub = whisperKeyPair.getPublicKey();
                        } else {
                            LogUtil.logWarning(TAG, "whisperKeyPair is null");
                        }

                        String encMyWhisperPub = null;
                        String encMyPushyToken = null;
                        String encMyFcmToken = null;

                        if (!TextUtils.isEmpty(whisperPub) && !TextUtils.isEmpty(pushyToken)) {
                            encMyWhisperPub = verificationUtil.encryptMessage(whisperPub,
                                    restoreSourcePublicKey);
                            encMyPushyToken = verificationUtil.encryptMessage(pushyToken,
                                    restoreSourcePublicKey);
                            if (TextUtils.isEmpty(encMyWhisperPub)) {
                                LogUtil.logError(TAG, "encMyWhisperPub is empty",
                                        new IllegalStateException("encMyWhisperPub is empty"));
                                return;
                            }
                            if (TextUtils.isEmpty(encMyPushyToken)) {
                                LogUtil.logError(TAG, "encMyPushyToken is empty",
                                        new IllegalStateException("encMyPushyToken is empty"));
                                return;
                            }
                        }

                        // For MESSAGING_TYPE == MULTI_MESSAGING
                        if (!TextUtils.isEmpty(sourceFcmToken) && !TextUtils.isEmpty(fcmToken)) {
                            encMyFcmToken = verificationUtil.encryptMessage(fcmToken,
                                    restoreSourcePublicKey);
                            if (TextUtils.isEmpty(encMyFcmToken)) {
                                LogUtil.logError(TAG, "encMyFcmToken is empty",
                                        new IllegalStateException("encMyFcmToken is empty"));
                                return;
                            }
                        }

                        if ((TextUtils.isEmpty(encMyWhisperPub) || TextUtils.isEmpty(
                                encMyPushyToken))
                                && TextUtils.isEmpty(encMyFcmToken)) {
                            LogUtil.logError(TAG, "All the tokens are empty",
                                    new IllegalStateException("All the tokens are empty"));
                            return;
                        }

                        messagesToRestoreSources = createMessagesToRestoreSources(encryptedUUID,
                                encCode, encCodeSign, encMyFcmToken,
                                encMyWhisperPub, encMyPushyToken, position);

                        new RestoreVerifyAction().send(mContext, sourceFcmToken, sourceWhisperPub,
                                sourcePushyToken,
                                messagesToRestoreSources);
                    } else if (!TextUtils.isEmpty(sourceFcmToken)) {
                        if (TextUtils.isEmpty(fcmToken)) {
                            LogUtil.logError(TAG, "fcmToken is empty",
                                    new IllegalStateException("fcmToken is empty"));
                            return;
                        }
                        String encMyFcmToken = verificationUtil.encryptMessage(fcmToken,
                                restoreSourcePublicKey);
                        if (TextUtils.isEmpty(encMyFcmToken)) {
                            LogUtil.logError(TAG, "encMyFcmToken is empty",
                                    new IllegalStateException("encMyFcmToken is empty"));
                            return;
                        }
                        messagesToRestoreSources =
                                createMessagesToRestoreSources(encryptedUUID, encCode, encCodeSign,
                                        encMyFcmToken, null, null, position);
                        new RestoreVerifyAction().send(mContext, sourceFcmToken, null, null,
                                messagesToRestoreSources);
                    } else {
                        LogUtil.logError(TAG, "No valid receiver",
                                new IllegalStateException("No valid receiver"));
                    }
                }

                @Override
                public void onUserTokenError(Exception exception) {
                    LogUtil.logError(TAG, "getMultiToken Exception, e=" + exception);
                }
            });
        }
    }

    private int getCanBeSentRestoreSourcesNumber(
            @NonNull final List<RestoreSourceEntity> restoreSources) {
        if (restoreSources == null) {
            LogUtil.logDebug(TAG, "anyRestoreSourceRequestStatus, restoreSources is null");
            return 0;
        }
        int canBeSentRestoreSourceNumber = 0;
        for (RestoreSourceEntity source : restoreSources) {
            if (source.compareStatus(RESTORE_SOURCE_STATUS_REQUEST)) {
                canBeSentRestoreSourceNumber++;
            }
        }
        return canBeSentRestoreSourceNumber;
    }

    @UiThread
    void cancelCountdownTimer(int position) {
        if (position >= 0 && position < ET_PIN_SIZE && mCountDownTimers[position] != null) {
            mCountDownTimers[position].cancel();
        }
    }

    @UiThread
    void startCountdownTimer(final int position, final long timeout) {
        if (position < 0 || position >= ET_PIN_SIZE) {
            adjustPinCodeStyleAndEnabled();
            return;
        }
        if (timeout <= 0) {
            LogUtil.logError(TAG, "startCountdownTimer() failed, incorrect timeout=" + timeout);
            return;
        }

        cancelCountdownTimer(position);
        mCountDownTimers[position] = new CountDownTimer(timeout, VERIFY_TIME_OUT_INTERVAL) {
            @Override
            public void onFinish() {
                if (mPinCodeStatuses[position] != SUCCESS) {
                    mCurrentPinCodePositionReceivedCount[position] =
                            mCurrentPinCodePositionReceivedMaxCount[position] - 1;
                    sendErrorPinCodeLocalBroadcast(position);
                }
                progressBars[position].setVisibility(INVISIBLE);
            }

            @Override
            public void onTick(long millisUntilFinished) {
            }
        }.start();
    }


    void addCurrentPinCodePositionReceivedCount(final int position) {
        if (position < 0 || position > ET_PIN_SIZE) {
            LogUtil.logError(TAG, "incorrect position = " + position,
                    new IllegalArgumentException());
            return;
        }
        mCurrentPinCodePositionReceivedCount[position]++;
    }

    void resetCurrentPinCodePositionReceivedCount(final int position) {
        if (position < 0 || position > ET_PIN_SIZE) {
            LogUtil.logError(TAG, "incorrect position = " + position,
                    new IllegalArgumentException());
            return;
        }
        mCurrentPinCodePositionReceivedCount[position] = 0;
    }

    int getCurrentPinCodePositionReceivedCount(final int position) {
        if (position < 0 || position > ET_PIN_SIZE) {
            LogUtil.logError(TAG, "incorrect position = " + position,
                    new IllegalArgumentException());
            return -1;
        }
        return mCurrentPinCodePositionReceivedCount[position];
    }

    void setCurrentPinCodePositionReceivedCount(int position, int count) {
        if (position < 0 || position > ET_PIN_SIZE) {
            LogUtil.logError(TAG, "incorrect position=" + position,
                    new IllegalArgumentException("incorrect position=" + position));
            return;
        }

        if (count < 0) {
            LogUtil.logError(TAG, "incorrect count=" + count,
                    new IllegalArgumentException("incorrect count=" + count));
            return;
        }

        mCurrentPinCodePositionReceivedCount[position] = count;
    }

    int getCurrentPinCodePositionReceivedMaxCount(final int position) {
        if (position < 0 || position > ET_PIN_SIZE) {
            LogUtil.logError(TAG, "incorrect position = " + position,
                    new IllegalArgumentException());
            return -1;
        }
        return mCurrentPinCodePositionReceivedMaxCount[position];
    }

    void setCurrentPinCodePositionReceivedMaxCount(int position, int count) {
        if (position < 0 || position > ET_PIN_SIZE) {
            LogUtil.logError(TAG, "incorrect position=" + position,
                    new IllegalArgumentException("incorrect position=" + position));
            return;
        }

        if (count < 0) {
            LogUtil.logError(TAG, "incorrect count=" + count,
                    new IllegalArgumentException("incorrect count=" + count));
            return;
        }

        mCurrentPinCodePositionReceivedMaxCount[position] = count;
    }

    private void adjustPinCodeStyleAndEnabled() {
        for (int i = 0; i < mPinCodeStatuses.length; i++) {
            if (mPinCodeStatuses[i] == SUCCESS) {
                setPinCodeSuccessStyle(i);
            } else {
                setPinCodeErrorStyle(i);
            }
        }
    }

    void setPinCodeSuccessStyle(int position) {
        if (position >= getPinCodeEditTextSize()) {
            return;
        }
        setPinCodeStatus(position, SUCCESS);
        setPinCodeEditTextEnabled(position, false);
        setPinCorrectStyle(position);
        progressBars[position].setVisibility(INVISIBLE);
    }

    void setPinCodeNotCompletedStyle(int position) {
        setPinNotCompletedStyle(position);
        setPinCodeEditTextEnabled(position, false);
        setPinCodeStatus(position, NOT_COMPLETE);
        progressBars[position].setVisibility(VISIBLE);
    }

    void setPinCodeErrorStyle(int position) {
        if (position >= getPinCodeEditTextSize()) {
            return;
        }
        setPinErrorStyle(position);
        setPinCodeStatus(position, PinCodeStatusConstant.FAIL);
        setPinCodeEditTextEnabled(position, true);
        progressBars[position].setVisibility(INVISIBLE);
    }

    void saveRetryTimes(int retryTimes) {
        mRetryTime = retryTimes;
        SkrSharedPrefs.putRestoreRetryTime(mContext, String.valueOf(mRetryTime));
    }

    int getRestoreRetryTime() {
        String retryTime = SkrSharedPrefs.getRestoreRetryTime(mContext);
        if (!TextUtils.isEmpty(retryTime)) {
            mRetryTime = Integer.valueOf(retryTime);
        }
        return mRetryTime;
    }

    @IntDef({NOT_COMPLETE, SUCCESS, FAIL})
    @interface PinCodeStatus {
    }
}
