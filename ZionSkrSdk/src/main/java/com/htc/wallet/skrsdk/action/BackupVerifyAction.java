package com.htc.wallet.skrsdk.action;

import static com.htc.wallet.skrsdk.keyserver.KeyServerManager.TOKEN_INVALID_OR_EXPIRED_MAX_RETRY_TIMES;
import static com.htc.wallet.skrsdk.messaging.processor.MessageProcessorFactory.FIREBASE_MESSAGE_PROCESSOR;
import static com.htc.wallet.skrsdk.messaging.processor.MessageProcessorFactory.PUSHY_MESSAGE_PROCESSOR;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.PIN_CODE_NO_ERROR;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_BAD;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_NO_RESPONSE;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_OK;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_REQUEST_WAIT_OK;
import static com.htc.wallet.skrsdk.tools.security.attestation.SafetyNetWrapper.ACTION_ATTEST_FAILED;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_NOTIFY_UI_UPDATE_AFTER_ERROR_PINCODE;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.Base64;

import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.crypto.Base64Util;
import com.htc.wallet.skrsdk.crypto.ChecksumUtil;
import com.htc.wallet.skrsdk.crypto.util.VerificationUtil;
import com.htc.wallet.skrsdk.keyserver.KeyServerApiService;
import com.htc.wallet.skrsdk.keyserver.KeyServerCallbackWithRetry;
import com.htc.wallet.skrsdk.keyserver.KeyServerManager;
import com.htc.wallet.skrsdk.keyserver.StatusCode;
import com.htc.wallet.skrsdk.keyserver.WebApiResponseUtils;
import com.htc.wallet.skrsdk.keyserver.requestbody.BackupCodePkRequestBody;
import com.htc.wallet.skrsdk.keyserver.responsebody.BackupCodePkResponseBody;
import com.htc.wallet.skrsdk.messaging.MessageConstants;
import com.htc.wallet.skrsdk.messaging.MessageServiceType;
import com.htc.wallet.skrsdk.messaging.processor.FirebaseMessageProcessor;
import com.htc.wallet.skrsdk.messaging.processor.MessageProcessorFactory;
import com.htc.wallet.skrsdk.messaging.processor.PushyMessageProcessor;
import com.htc.wallet.skrsdk.secretsharing.SeedUtil;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupSourceUtil;
import com.htc.wallet.skrsdk.sqlite.util.BackupTargetUtil;
import com.htc.wallet.skrsdk.tools.security.attestation.SafetyNetWrapper;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;
import com.htc.wallet.skrsdk.util.PinCodeUtil;
import com.htc.wallet.skrsdk.util.RetryUtil;
import com.htc.wallet.skrsdk.util.ThreadUtil;
import com.htc.wallet.skrsdk.util.WalletSdkUtil;
import com.htc.wallet.skrsdk.whisper.WhisperKeyPair;
import com.htc.wallet.skrsdk.whisper.WhisperUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnegative;

import retrofit2.Call;
import retrofit2.Response;

public class BackupVerifyAction extends Action {
    private static final String TAG = "BackupVerifyAction";

    private static final String KEY_MY_TOKEN = "key_my_token";
    private static final String KEY_SENDER_TOKEN = "key_sender_token";
    private static final String KEY_MY_WHISPER_PUB = "key_my_whisper_pub";
    private static final String KEY_SENDER_WHISPER_PUB = "key_sender_whisper_pub";
    private static final String KEY_MY_PUSHY_TOKEN = "key_my_pushy_token";
    private static final String KEY_SENDER_PUSHY_TOKEN = "key_sender_pushy_token";
    private static final int TIMEOUT = 10; // seconds

    private static final ExecutorService sErrorHandleExecutor = Executors.newSingleThreadExecutor();

    private static void sendError(
            @NonNull Context context,
            @Nullable final String receiverToken,
            @Nullable String receiverWhisperPub,
            @Nullable String receiverPushyToken,
            @Nonnegative int retryTimes,
            @NonNull String publicKey) {
        Map<String, String> errorMsg = new ArrayMap<>();
        errorMsg.put(KEY_RETRY_TIMES, String.valueOf(retryTimes));
        errorMsg.put(KEY_PUBLIC_KEY, publicKey);
        new BackupErrorAction()
                .sendInternal(
                        context, receiverToken, receiverWhisperPub, receiverPushyToken, errorMsg);
    }

    @Override
    int getMessageTypeId() {
        return MessageConstants.TYPE_BACKUP_VERIFY;
    }

    // Bob
    @Override
    public void sendInternal(
            @NonNull final Context context,
            @Nullable final String receiverFcmToken,
            @Nullable final String receiverWhisperPub,
            @Nullable final String receiverPushyToken,
            @NonNull final Map<String, String> messages) {

        final String sourceEmailHash = messages.get(KEY_EMAIL_HASH);
        final String sourceUUIDHash = messages.get(KEY_UUID_HASH);
        final String pinCode = messages.get(KEY_PIN_CODE);
        if (TextUtils.isEmpty(sourceEmailHash)) {
            throw new IllegalStateException("sourceEmailHash is null or empty");
        }
        if (TextUtils.isEmpty(sourceUUIDHash)) {
            throw new IllegalStateException("sourceUUIDHash is null or empty");
        }
        if (!PinCodeUtil.isValidPinCode(pinCode)) {
            throw new IllegalStateException("invalid Pin Code");
        }

        BackupSourceUtil.getWithUUIDHash(
                context,
                sourceEmailHash,
                sourceUUIDHash,
                new LoadDataListener() {
                    @Override
                    public void onLoadFinished(
                            BackupSourceEntity backupSourceEntity,
                            BackupTargetEntity backupTargetEntity,
                            RestoreSourceEntity restoreSourceEntity,
                            RestoreTargetEntity restoreTargetEntity) {
                        // Bob's request has been reject, just show error, and request again
                        if (backupSourceEntity == null) {
                            LogUtil.logInfo(
                                    TAG, "without backupSourceEntity, request has been rejected");
                            ThreadUtil.sleep(DateUtils.SECOND_IN_MILLIS);
                            Intent intent = new Intent(ACTION_NOTIFY_UI_UPDATE_AFTER_ERROR_PINCODE);
                            intent.putExtra(KEY_EMAIL_HASH, sourceEmailHash);
                            intent.putExtra(KEY_UUID_HASH, sourceUUIDHash);
                            intent.putExtra(KEY_RETRY_TIMES, 0);
                            intent.putExtra(KEY_RETRY_WAIT_START_TIME, System.currentTimeMillis());
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                            return;
                        }

                        if (!backupSourceEntity.compareStatus(BACKUP_SOURCE_STATUS_REQUEST)) {
                            LogUtil.logWarning(
                                    TAG, "incorrect status=" + backupSourceEntity.getStatus());
                            return;
                        }
                        // Get Amy's publicKey from storage
                        final String sourcePublicKey = backupSourceEntity.getPublicKey();
                        if (TextUtils.isEmpty(sourcePublicKey)) {
                            LogUtil.logError(
                                    TAG,
                                    "sourcePublicKey is null or empty",
                                    new IllegalStateException());
                            return;
                        }
                        // Get Amy's TZIDHash from storage
                        final String sourceTZIDHash = backupSourceEntity.getTzIdHash();
                        if (TextUtils.isEmpty(sourceTZIDHash)) {
                            LogUtil.logError(
                                    TAG,
                                    "sourceTZIDHash is null or empty",
                                    new IllegalStateException());
                            return;
                        }
                        // Getting Amy's isTest from storage
                        final boolean isTest = backupSourceEntity.getIsTest();
                        if (isTest) {
                            LogUtil.logInfo(TAG, "isTest is true for testing");
                        }
                        // device attest token
                        final String attestToken =
                                SafetyNetWrapper.getInstance()
                                        .getDeviceAttestToken(context, isTest, false);
                        if (TextUtils.isEmpty(attestToken)) {
                            LogUtil.logError(TAG, "attestToken is null or empty");
                            return;
                        }

                        // Set last type Pin Code and save
                        backupSourceEntity.setPinCode(pinCode);
                        backupSourceEntity.setLastVerifyTime(System.currentTimeMillis());
                        backupSourceEntity.setIsPinCodeError(PIN_CODE_NO_ERROR);
                        BackupSourceUtil.update(
                                context,
                                backupSourceEntity,
                                new DatabaseCompleteListener() {
                                    @Override
                                    public void onComplete() {
                                        final VerificationUtil verificationUtil =
                                                new VerificationUtil(true);
                                        final String myDeviceId = PhoneUtil.getDeviceId(context);
                                        if (TextUtils.isEmpty(myDeviceId)) {
                                            LogUtil.logError(
                                                    TAG,
                                                    "myDeviceId is null or empty",
                                                    new IllegalStateException(
                                                            "myDeviceId is null or empty"));
                                            return;
                                        }
                                        final String encDeviceId =
                                                verificationUtil.encryptMessage(
                                                        myDeviceId, sourcePublicKey);
                                        if (TextUtils.isEmpty(encDeviceId)) {
                                            LogUtil.logError(
                                                    TAG,
                                                    "encDeviceId is null or empty",
                                                    new IllegalStateException(
                                                            "encDeviceId is null or empty"));
                                            return;
                                        }
                                        final String myPublicKey =
                                                verificationUtil.getPublicKeyString();
                                        if (TextUtils.isEmpty(myPublicKey)) {
                                            LogUtil.logError(
                                                    TAG,
                                                    "myPublicKey is null or empty",
                                                    new IllegalStateException(
                                                            "myPublicKey is null or empty"));
                                            return;
                                        }

                                        String encToken = null;
                                        String encWhisperPub = null;
                                        String encPushyToken = null;

                                        final PushyMessageProcessor processor =
                                                (PushyMessageProcessor)
                                                        MessageProcessorFactory.getInstance()
                                                                .getMessageProcessor(
                                                                        PUSHY_MESSAGE_PROCESSOR);
                                        if (processor == null) {
                                            LogUtil.logError(
                                                    TAG,
                                                    "processor is null",
                                                    new IllegalStateException("processor is null"));
                                            return;
                                        }
                                        final FirebaseMessageProcessor firebaseMessageProcessor =
                                                (FirebaseMessageProcessor)
                                                        MessageProcessorFactory.getInstance()
                                                                .getMessageProcessor(
                                                                        FIREBASE_MESSAGE_PROCESSOR);
                                        if (firebaseMessageProcessor == null) {
                                            LogUtil.logError(
                                                    TAG,
                                                    "firebaseMessageProcessor is null",
                                                    new IllegalStateException(
                                                            "firebaseMessageProcessor is null"));
                                            return;
                                        }

                                        if (!TextUtils.isEmpty(receiverWhisperPub)
                                                && !TextUtils.isEmpty(receiverPushyToken) &&
                                                ZionSkrSdkManager.getInstance().getMessageServiceType()
                                                        != MessageServiceType.FIREBASE) {
                                            // For MESSAGING_TYPE == MULTI_MESSAGING
                                            if (!TextUtils.isEmpty(receiverFcmToken)) {
                                                final String myFcmToken =
                                                        firebaseMessageProcessor.getFcmTokenFromSp(
                                                                context);
                                                if (TextUtils.isEmpty(myFcmToken)) {
                                                    LogUtil.logError(
                                                            TAG,
                                                            "myFcmToken is empty",
                                                            new IllegalStateException(
                                                                    "myFcmToken is empty"));
                                                    return;
                                                }
                                                encToken =
                                                        verificationUtil.encryptMessage(
                                                                myFcmToken, sourcePublicKey);
                                                if (TextUtils.isEmpty(encToken)) {
                                                    LogUtil.logError(
                                                            TAG,
                                                            "encToken is null or empty",
                                                            new IllegalStateException(
                                                                    "encToken is null or empty"));
                                                    return;
                                                }
                                            }

                                            final WhisperKeyPair whisperKeyPair =
                                                    WhisperUtils.getKeyPair(context);
                                            if (whisperKeyPair != null) {
                                                final String myWhisperPub =
                                                        whisperKeyPair.getPublicKey();

                                                final String myPushyToken =
                                                        processor.getPushyTokenFromSp(context);
                                                if (!TextUtils.isEmpty(myWhisperPub)
                                                        && !TextUtils.isEmpty(myPushyToken)) {
                                                    encWhisperPub =
                                                            verificationUtil.encryptMessage(
                                                                    myWhisperPub, sourcePublicKey);
                                                    if (TextUtils.isEmpty(encWhisperPub)) {
                                                        LogUtil.logError(
                                                                TAG,
                                                                "encWhisperPub is null or empty",
                                                                new IllegalStateException(
                                                                        "encWhisperPub is null or"
                                                                                + " empty"));
                                                        return;
                                                    }

                                                    encPushyToken =
                                                            verificationUtil.encryptMessage(
                                                                    myPushyToken, sourcePublicKey);
                                                    if (TextUtils.isEmpty(encPushyToken)) {
                                                        LogUtil.logError(
                                                                TAG,
                                                                "encPushyToken is null or empty",
                                                                new IllegalStateException(
                                                                        "encPushyToken is null or"
                                                                                + " empty"));
                                                        return;
                                                    }
                                                }
                                            } else {
                                                LogUtil.logWarning(TAG, "whisperKeyPair is null");
                                            }
                                        } else if (!TextUtils.isEmpty(receiverFcmToken)) {
                                            final String myFcmToken =
                                                    firebaseMessageProcessor.getFcmTokenFromSp(
                                                            context);
                                            if (TextUtils.isEmpty(myFcmToken)) {
                                                LogUtil.logError(
                                                        TAG,
                                                        "myFcmToken is empty",
                                                        new IllegalStateException(
                                                                "myFcmToken is empty"));
                                                return;
                                            }
                                            encToken =
                                                    verificationUtil.encryptMessage(
                                                            myFcmToken, sourcePublicKey);
                                            if (TextUtils.isEmpty(encToken)) {
                                                LogUtil.logError(
                                                        TAG,
                                                        "encToken is null or empty",
                                                        new IllegalStateException(
                                                                "encToken is null or empty"));
                                                return;
                                            }
                                        } else {
                                            LogUtil.logError(
                                                    TAG,
                                                    "No valid receiver",
                                                    new IllegalStateException("No valid receiver"));
                                            return;
                                        }

                                        // Retry when Attest token invalid or expired (401)
                                        final AtomicInteger retryEncryptCodePKTimes =
                                                new AtomicInteger(0);

                                        // Public Key Base64 Default
                                        String seedPublicKey =
                                                Base64.encodeToString(
                                                        Base64Util.decode(myPublicKey),
                                                        Base64.DEFAULT);
                                        final KeyServerManager keyServerManager =
                                                KeyServerManager.getInstance(isTest);
                                        final BackupCodePkRequestBody requestBody =
                                                new BackupCodePkRequestBody(
                                                        context,
                                                        keyServerManager.getTzApiKey(context),
                                                        sourceTZIDHash,
                                                        pinCode,
                                                        seedPublicKey);
                                        final KeyServerApiService keyServer =
                                                keyServerManager.getKeyServerApiService();
                                        Call<BackupCodePkResponseBody> call =
                                                keyServer.postEncryptCodePKV2(
                                                        attestToken, requestBody);
                                        final String finalEncWhisperPub = encWhisperPub;
                                        final String finalEncPushyToken = encPushyToken;
                                        final String finalEncToken = encToken;
                                        call.enqueue(
                                                new KeyServerCallbackWithRetry<
                                                        BackupCodePkResponseBody>(call) {
                                                    @Override
                                                    public void onResponse(
                                                            @NonNull
                                                                    Call<BackupCodePkResponseBody>
                                                                    call,
                                                            @NonNull
                                                                    Response<
                                                                            BackupCodePkResponseBody>
                                                                    response) {
                                                        super.onResponse(call, response);
                                                        if (response.isSuccessful()) {
                                                            BackupCodePkResponseBody responseBody =
                                                                    response.body();
                                                            if (responseBody != null) {

                                                                // Check encCodePK
                                                                final String encCodePK =
                                                                        responseBody.getEncCodePK();
                                                                if (TextUtils.isEmpty(encCodePK)) {
                                                                    LogUtil.logError(
                                                                            TAG,
                                                                            "encCodePK is null or"
                                                                                    + " empty",
                                                                            new IllegalStateException());
                                                                    return;
                                                                }
                                                                if (encCodePK.length()
                                                                        != SeedUtil
                                                                        .ENC_CODE_PK_LENGTH) {
                                                                    LogUtil.logDebug(
                                                                            TAG,
                                                                            "incorrect encCodePK "
                                                                                    + "length "
                                                                                    + encCodePK
                                                                                    .length());
                                                                }

                                                                // Check encCodePKSigned
                                                                final String encCodePKSigned =
                                                                        responseBody
                                                                                .getEncCodePKSigned();
                                                                if (TextUtils.isEmpty(
                                                                        encCodePKSigned)) {
                                                                    LogUtil.logError(
                                                                            TAG,
                                                                            "encCodePKSigned is "
                                                                                    + "null or "
                                                                                    + "empty",
                                                                            new IllegalStateException());
                                                                    return;
                                                                }
                                                                if (encCodePKSigned.length()
                                                                        != SeedUtil
                                                                        .ENC_CODE_PK_SIG_LENGTH) {
                                                                    LogUtil.logDebug(
                                                                            TAG,
                                                                            "incorrect encCodePKSigned length "
                                                                                    + encCodePKSigned
                                                                                    .length());
                                                                }

                                                                // Check encAesKey
                                                                final String encAesKey =
                                                                        responseBody.getEncAesKey();
                                                                if (TextUtils.isEmpty(encAesKey)) {
                                                                    LogUtil.logError(
                                                                            TAG,
                                                                            "encAesKey is null or"
                                                                                    + " empty",
                                                                            new IllegalStateException());
                                                                    return;
                                                                }
                                                                if (encAesKey.length()
                                                                        != SeedUtil
                                                                        .ENC_AES_KEY_LENGTH) {
                                                                    LogUtil.logDebug(
                                                                            TAG,
                                                                            "incorrect encAesKey "
                                                                                    + "length "
                                                                                    + encAesKey
                                                                                    .length());
                                                                }

                                                                // Check encAesKeySigned
                                                                final String encAesKeySigned =
                                                                        responseBody
                                                                                .getEncAesKeySigned();
                                                                if (TextUtils.isEmpty(
                                                                        encAesKeySigned)) {
                                                                    LogUtil.logError(
                                                                            TAG,
                                                                            "encAesKeySigned is "
                                                                                    + "null or "
                                                                                    + "empty",
                                                                            new IllegalStateException());
                                                                    return;
                                                                }
                                                                if (encAesKeySigned.length()
                                                                        != SeedUtil
                                                                        .ENC_AES_KEY_SIG_LENGTH) {
                                                                    LogUtil.logDebug(
                                                                            TAG,
                                                                            "incorrect encAesKeySigned length "
                                                                                    + encAesKeySigned
                                                                                    .length());
                                                                }

                                                                final Map<String, String>
                                                                        encryptedMap =
                                                                        new ArrayMap<>();
                                                                encryptedMap.put(
                                                                        KEY_ENCRYPTED_UUID,
                                                                        encDeviceId);
                                                                encryptedMap.put(
                                                                        KEY_ENCRYPTED_CODE_PK,
                                                                        encCodePK);
                                                                encryptedMap.put(
                                                                        KEY_ENCRYPTED_CODE_PK_SIGNED,
                                                                        encCodePKSigned);
                                                                encryptedMap.put(
                                                                        KEY_ENCRYPTED_AES_KEY,
                                                                        encAesKey);
                                                                encryptedMap.put(
                                                                        KEY_ENCRYPTED_AES_KEY_SIGNED,
                                                                        encAesKeySigned);
                                                                encryptedMap.put(
                                                                        KEY_ENCRYPTED_TOKEN,
                                                                        finalEncToken);
                                                                encryptedMap.put(
                                                                        KEY_ENCRYPTED_WHISPER_PUB,
                                                                        finalEncWhisperPub);
                                                                encryptedMap.put(
                                                                        KEY_ENCRYPTED_PUSHY_TOKEN,
                                                                        finalEncPushyToken);
                                                                encryptedMap.put(
                                                                        KEY_LINK_UUID_HASH,
                                                                        sourceUUIDHash);
                                                                sendMessage(
                                                                        context,
                                                                        receiverFcmToken,
                                                                        receiverWhisperPub,
                                                                        receiverPushyToken,
                                                                        encryptedMap);
                                                            } else {
                                                                LogUtil.logError(
                                                                        TAG,
                                                                        "responseBody is null",
                                                                        new IllegalStateException());
                                                            }
                                                        } else {
                                                            final int responseCode =
                                                                    response.code();
                                                            LogUtil.logError(
                                                                    TAG,
                                                                    "postEncryptCodePKV2 failed, e="
                                                                            + WebApiResponseUtils
                                                                            .getErrorMsg(
                                                                                    response));
                                                            // Use single work to handle error
                                                            // status
                                                            sErrorHandleExecutor.execute(
                                                                    new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            // handle error status
                                                                            // code 401 (token
                                                                            // invalid or expired)
                                                                            if (responseCode
                                                                                    == StatusCode
                                                                                    .TOKEN_INVALID_OR_EXPIRED) {
                                                                                if (retryEncryptCodePKTimes
                                                                                        .getAndIncrement()
                                                                                        >= TOKEN_INVALID_OR_EXPIRED_MAX_RETRY_TIMES) {
                                                                                    LogUtil
                                                                                            .logDebug(
                                                                                                    TAG,
                                                                                                    "postEncryptCodePKV2, reach 401 max retry times, return");
                                                                                    // Broadcast to
                                                                                    // show attest
                                                                                    // failed
                                                                                    // dialog. It's
                                                                                    // may show on
                                                                                    // another
                                                                                    // activity.
                                                                                    LogUtil
                                                                                            .logDebug(
                                                                                                    TAG,
                                                                                                    "Send attest failed broadcast");
                                                                                    Intent intent =
                                                                                            new Intent(
                                                                                                    ACTION_ATTEST_FAILED);
                                                                                    LocalBroadcastManager
                                                                                            .getInstance(
                                                                                                    context)
                                                                                            .sendBroadcast(
                                                                                                    intent);
                                                                                    return;
                                                                                }
                                                                                // force acquire a
                                                                                // new device attest
                                                                                // token
                                                                                final String
                                                                                        newAttestToken =
                                                                                        SafetyNetWrapper
                                                                                                .getInstance()
                                                                                                .getDeviceAttestToken(
                                                                                                        context,
                                                                                                        isTest,
                                                                                                        true);
                                                                                if (TextUtils
                                                                                        .isEmpty(
                                                                                                newAttestToken)) {
                                                                                    LogUtil
                                                                                            .logError(
                                                                                                    TAG,
                                                                                                    "new attestToken is null or empty");
                                                                                    return;
                                                                                }
                                                                                LogUtil.logDebug(
                                                                                        TAG,
                                                                                        "enqueue postEncryptCodePKV2 call with new attestToken");
                                                                                Call<
                                                                                        BackupCodePkResponseBody>
                                                                                        newCall =
                                                                                        keyServer
                                                                                                .postEncryptCodePKV2(
                                                                                                        newAttestToken,
                                                                                                        requestBody);
                                                                                newCall.enqueue(
                                                                                        getSelf());
                                                                            } else if (responseCode
                                                                                    == StatusCode
                                                                                    .NOT_FOUND) {
                                                                                LogUtil.logDebug(
                                                                                        TAG,
                                                                                        "api key or device not found, TZIDHash="
                                                                                                + sourceTZIDHash);
                                                                            }
                                                                        }
                                                                    });
                                                        }
                                                    }

                                                    @Override
                                                    public void onFailure(
                                                            @NonNull
                                                                    Call<BackupCodePkResponseBody>
                                                                    call,
                                                            @NonNull Throwable t) {
                                                        super.onFailure(call, t);
                                                        LogUtil.logError(
                                                                TAG, "postEncryptCodePK failed");
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onError(Exception exception) {
                                        LogUtil.logError(TAG, "update error, e= " + exception);
                                    }
                                });
                    }
                });
    }

    // Amy
    @Override
    public void onReceiveInternal(
            @NonNull final Context context,
            @Nullable final String senderFcmToken,
            @Nullable final String myFcmToken,
            @Nullable final String senderWhisperPub,
            @Nullable final String myWhisperPub,
            @Nullable final String senderPushyToken,
            @Nullable final String myPushyToken,
            @NonNull final Map<String, String> messages) {

        boolean isSeedExists = WalletSdkUtil.isSeedExists(context);
        if (!isSeedExists) {
            LogUtil.logWarning(TAG, "Receive backup verify without wallet, ignore it");
            return;
        }

        // Convert Map<String, String> to Bundle
        Bundle bundle = new Bundle();
        bundle.putString(KEY_SENDER_TOKEN, senderFcmToken);
        bundle.putString(KEY_MY_TOKEN, myFcmToken);
        bundle.putString(KEY_SENDER_WHISPER_PUB, senderWhisperPub);
        bundle.putString(KEY_MY_WHISPER_PUB, myWhisperPub);
        bundle.putString(KEY_SENDER_PUSHY_TOKEN, senderPushyToken);
        bundle.putString(KEY_MY_PUSHY_TOKEN, myPushyToken);
        for (Map.Entry<String, String> entry : messages.entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }

        // Create Message
        Message message = Message.obtain();
        message.setData(bundle);

        // Use single thread to run in sequence
        ReceiverHandler.getInstance(context).sendMessage(message);
    }

    private static class ReceiverHandler extends Handler {
        private static final String THREAD_NAME = "BackupVerifyActionReceiver";
        private static final Object LOCK = new Object();
        private static volatile ReceiverHandler sReceiverHandler;

        private static volatile Context sContext;

        private ReceiverHandler(Looper looper, Context context) {
            super(looper);
            sContext = context.getApplicationContext();
        }

        private static ReceiverHandler getInstance(@NonNull Context context) {
            Objects.requireNonNull(context, "context is null");
            if (sReceiverHandler != null) {
                return sReceiverHandler;
            }
            synchronized (LOCK) {
                if (sReceiverHandler != null) {
                    return sReceiverHandler;
                }
                HandlerThread handlerThread = new HandlerThread(THREAD_NAME);
                handlerThread.start();
                sReceiverHandler = new ReceiverHandler(handlerThread.getLooper(), context);
                return sReceiverHandler;
            }
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            final Context context = sContext;
            if (context == null) {
                LogUtil.logError(TAG, "context is null");
                return;
            }

            final Bundle bundle = msg.getData();
            if (bundle == null) {
                LogUtil.logError(TAG, "bundle is null");
                return;
            }

            final String senderToken = bundle.getString(KEY_SENDER_TOKEN);
            final String senderWhisperPub = bundle.getString(KEY_SENDER_WHISPER_PUB);
            final String senderPushyToken = bundle.getString(KEY_SENDER_PUSHY_TOKEN);

            final String linkUUIDHash = bundle.getString(KEY_LINK_UUID_HASH);
            if (TextUtils.isEmpty(linkUUIDHash)) {
                LogUtil.logError(
                        TAG,
                        "linkUuidHash is null or empty",
                        new IllegalStateException("linkUuidHash is null or empty"));
                return;
            }

            if (!linkUUIDHash.equals(PhoneUtil.getSKRIDHash(context))) {
                LogUtil.logDebug(
                        TAG, "The sender clicked the old shared link, ignore the request.");
                return;
            }

            final VerificationUtil verificationUtil = new VerificationUtil(false);

            // Decrypt UUID
            final String encUUID = bundle.getString(KEY_ENCRYPTED_UUID);
            if (TextUtils.isEmpty(encUUID)) {
                LogUtil.logError(
                        TAG,
                        "encUUID is null or empty",
                        new IllegalStateException("encUUID is null or empty"));
                return;
            }
            final String senderUUID = verificationUtil.decryptMessage(encUUID);
            if (TextUtils.isEmpty(senderUUID)) {
                LogUtil.logError(
                        TAG,
                        "senderUUID is null or empty",
                        new IllegalStateException("senderUUID is null or empty"));
                return;
            }
            final String senderUUIDHash = ChecksumUtil.generateChecksum(senderUUID);
            if (TextUtils.isEmpty(senderUUIDHash)) {
                LogUtil.logError(
                        TAG,
                        "senderUUIDHash is null or empty",
                        new IllegalStateException("senderUUIDHash is null or empty"));
                return;
            }
            final String encCodePK = bundle.getString(KEY_ENCRYPTED_CODE_PK);
            if (TextUtils.isEmpty(encCodePK)) {
                LogUtil.logError(TAG, "encCodePK is null or empty", new IllegalStateException());
                return;
            }
            final String encCodePKSigned = bundle.getString(KEY_ENCRYPTED_CODE_PK_SIGNED);
            if (TextUtils.isEmpty(encCodePKSigned)) {
                LogUtil.logError(
                        TAG, "encCodePKSigned is null or empty", new IllegalStateException());
                return;
            }
            final String encAesKey = bundle.getString(KEY_ENCRYPTED_AES_KEY);
            if (TextUtils.isEmpty(encAesKey)) {
                LogUtil.logError(TAG, "encAesKey is null or empty", new IllegalStateException());
                return;
            }
            final String encAesKeySigned = bundle.getString(KEY_ENCRYPTED_AES_KEY_SIGNED);
            if (TextUtils.isEmpty(encAesKeySigned)) {
                LogUtil.logError(
                        TAG, "encAesKeySigned is null or empty", new IllegalStateException());
                return;
            }

            // Decrypt FCM Token
            String decFcmToken = null;
            final String encFcmToken = bundle.getString(KEY_ENCRYPTED_TOKEN);
            if (!TextUtils.isEmpty(encFcmToken)) {
                decFcmToken = verificationUtil.decryptMessage(encFcmToken);
                if (TextUtils.isEmpty(decFcmToken)) {
                    LogUtil.logError(
                            TAG,
                            "decFcmToken is null or empty",
                            new IllegalStateException("decFcmToken is null or empty"));
                    return;
                }
            }

            // Decrypt Whisper Pub and Pushy Token
            String decWhisperPub = null;
            final String encWhisperPub = bundle.getString(KEY_ENCRYPTED_WHISPER_PUB);
            if (!TextUtils.isEmpty(encWhisperPub)) {
                decWhisperPub = verificationUtil.decryptMessage(encWhisperPub);
                if (TextUtils.isEmpty(decWhisperPub)) {
                    LogUtil.logError(
                            TAG,
                            "decWhisperPub is null or empty",
                            new IllegalStateException("decWhisperPub is null or empty"));
                    return;
                }
            }

            String decPushyToken = null;
            final String encPushyToken = bundle.getString(KEY_ENCRYPTED_PUSHY_TOKEN);
            if (!TextUtils.isEmpty(encPushyToken)) {
                decPushyToken = verificationUtil.decryptMessage(encPushyToken);
                if (TextUtils.isEmpty(decPushyToken)) {
                    LogUtil.logError(
                            TAG,
                            "decPushyToken is null or empty",
                            new IllegalStateException("decPushyToken is null or empty"));
                    return;
                }
            }

            final CountDownLatch latch = new CountDownLatch(1);
            final String finalDecWhisperPub = decWhisperPub;
            final String finalDecPushyToken = decPushyToken;
            final String finalDecFcmToken = decFcmToken;
            BackupTargetUtil.get(
                    context,
                    senderUUIDHash,
                    new LoadDataListener() {
                        @Override
                        public void onLoadFinished(
                                BackupSourceEntity backupSourceEntity,
                                final BackupTargetEntity backupTargetEntity,
                                RestoreSourceEntity restoreSourceEntity,
                                RestoreTargetEntity restoreTargetEntity) {
                            if (backupTargetEntity == null) {
                                LogUtil.logInfo(TAG, "Receive a verify without previous status");
                                latch.countDown();
                                return;
                            }

                            String whisperPub = senderWhisperPub;
                            String pushyToken = senderPushyToken;
                            // To ensure that BackupHealthReportAction can try whisper first.
                            if (TextUtils.isEmpty(whisperPub) || TextUtils.isEmpty(pushyToken)) {
                                whisperPub = backupTargetEntity.getWhisperPub();
                                pushyToken = backupTargetEntity.getPushyToken();
                            }

                            final int status = backupTargetEntity.getStatus();
                            switch (status) {
                                case BACKUP_TARGET_STATUS_REQUEST:
                                    LogUtil.logDebug(
                                            TAG,
                                            "Receive message, verify from "
                                                    + backupTargetEntity.getName());
                                    if (!backupTargetEntity.isSeeded()) {
                                        // Do not increase wait time and retryTimes
                                        sendError(
                                                context,
                                                senderToken,
                                                whisperPub,
                                                pushyToken,
                                                0,
                                                backupTargetEntity.getPublicKey());
                                        latch.countDown();
                                        return;
                                    }
                                    break;
                                case BACKUP_TARGET_STATUS_OK:
                                    LogUtil.logError(
                                            TAG, "Receive a verify with previous status OK");
                                    latch.countDown();
                                    return;
                                case BACKUP_TARGET_STATUS_BAD:
                                    LogUtil.logError(
                                            TAG, "Receive a verify with previous status Bad");
                                    latch.countDown();
                                    return;
                                case BACKUP_TARGET_STATUS_NO_RESPONSE:
                                    LogUtil.logError(
                                            TAG,
                                            "Receive a verify with previous status No response");
                                    latch.countDown();
                                    return;
                                case BACKUP_TARGET_STATUS_REQUEST_WAIT_OK:
                                    LogUtil.logInfo(
                                            TAG,
                                            "Receive a verify with previous status WaitOK, send "
                                                    + "error back");
                                    sendError(
                                            context,
                                            senderToken,
                                            whisperPub,
                                            pushyToken,
                                            0,
                                            backupTargetEntity.getPublicKey());
                                    latch.countDown();
                                    return;
                                default:
                                    LogUtil.logError(TAG, "incorrect previous status " + status);
                                    latch.countDown();
                                    return;
                            }

                            // Check whisper pub and pushy token
                            if (!TextUtils.isEmpty(senderWhisperPub)
                                    && !TextUtils.isEmpty(senderPushyToken)) {
                                if (!senderWhisperPub.equals(finalDecWhisperPub)
                                        || !senderPushyToken.equals(finalDecPushyToken)) {
                                    LogUtil.logError(
                                            TAG,
                                            "Whisper Pub or Pushy Token not match",
                                            new IllegalStateException());
                                    sendError(
                                            context,
                                            senderToken,
                                            whisperPub,
                                            pushyToken,
                                            backupTargetEntity.getRetryTimes(),
                                            backupTargetEntity.getPublicKey());
                                    latch.countDown();
                                    return;
                                }
                            } else if (!TextUtils.isEmpty(senderToken)) {
                                if (!senderToken.equals(finalDecFcmToken)) {
                                    LogUtil.logError(
                                            TAG,
                                            "FCM token not match",
                                            new IllegalStateException());
                                    sendError(
                                            context,
                                            senderToken,
                                            whisperPub,
                                            pushyToken,
                                            backupTargetEntity.getRetryTimes(),
                                            backupTargetEntity.getPublicKey());
                                    latch.countDown();
                                    return;
                                }
                            }

                            // Check if waitTime finish
                            long waitTime =
                                    RetryUtil.getWaitTimeMillis(backupTargetEntity.getRetryTimes());
                            if (backupTargetEntity.getRetryWaitStartTime() + waitTime
                                    > System.currentTimeMillis()) {
                                LogUtil.logDebug(TAG, "waitTime not finish");
                                latch.countDown();
                                return;
                            }

                            // Bob's publicKey
                            final String publicKey = backupTargetEntity.getPublicKey();
                            if (TextUtils.isEmpty(publicKey)) {
                                LogUtil.logError(
                                        TAG,
                                        "publicKey is null or empty",
                                        new IllegalStateException());
                                latch.countDown();
                                return;
                            }

                            int seedIndex = backupTargetEntity.getSeedIndex();
                            final String encryptedPartialSeed =
                                    SeedUtil.getPartialSeedV2(
                                            context,
                                            seedIndex,
                                            encCodePK,
                                            encCodePKSigned,
                                            encAesKey,
                                            encAesKeySigned);

                            final String finalWhisperPub = whisperPub;
                            final String finalPushyToken = pushyToken;
                            // To ensure that Amy can save Bob's whisper pub and pushy token if she
                            // doesn't save yet.
                            if (!TextUtils.isEmpty(finalWhisperPub)
                                    && !TextUtils.isEmpty(finalPushyToken)) {
                                backupTargetEntity.setWhisperPub(finalWhisperPub);
                                backupTargetEntity.setPushyToken(finalPushyToken);
                            }

                            // Check if pin code correct
                            if (TextUtils.isEmpty(encryptedPartialSeed)) {
                                // Not Success
                                LogUtil.logDebug(TAG, "pin code doesn't match, notify sender");

                                // Save newRetryTimes to BackupTarget
                                backupTargetEntity.increaseRetryTimes();
                                BackupTargetUtil.update(
                                        context,
                                        backupTargetEntity,
                                        new DatabaseCompleteListener() {
                                            @Override
                                            public void onComplete() {
                                                // Send Error back
                                                sendError(
                                                        context,
                                                        senderToken,
                                                        finalWhisperPub,
                                                        finalPushyToken,
                                                        backupTargetEntity.getRetryTimes(),
                                                        publicKey);
                                                latch.countDown();
                                            }

                                            @Override
                                            public void onError(Exception exception) {
                                                LogUtil.logError(
                                                        TAG, "update error, e= " + exception);
                                            }
                                        });
                            } else {
                                // Success
                                final String checkSum =
                                        ChecksumUtil.generateChecksum(encryptedPartialSeed);
                                backupTargetEntity.setStatus(BACKUP_TARGET_STATUS_REQUEST_WAIT_OK);
                                backupTargetEntity.setCheckSum(checkSum);
                                BackupTargetUtil.update(
                                        context,
                                        backupTargetEntity,
                                        new DatabaseCompleteListener() {
                                            @Override
                                            public void onComplete() {
                                                Map<String, String> map = new ArrayMap<>();
                                                map.put(KEY_ENCRYPTED_SEED, encryptedPartialSeed);
                                                map.put(KEY_CHECKSUM, checkSum);
                                                map.put(KEY_PUBLIC_KEY, publicKey);
                                                new BackupSeedAction()
                                                        .sendInternal(
                                                                context,
                                                                senderToken,
                                                                finalWhisperPub,
                                                                finalPushyToken,
                                                                map);
                                                latch.countDown();
                                            }

                                            @Override
                                            public void onError(Exception exception) {
                                                LogUtil.logError(
                                                        TAG, "update error, e= " + exception);
                                            }
                                        });
                            }
                        }
                    });

            try {
                latch.await(TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LogUtil.logError(TAG, "InterruptedException e" + e);
            }
        }
    }
}
