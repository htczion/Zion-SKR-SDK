package com.htc.wallet.skrsdk.action;

import static com.htc.wallet.skrsdk.keyserver.KeyServerManager.TOKEN_INVALID_OR_EXPIRED_MAX_RETRY_TIMES;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_REQUEST_AUTO_BACKUP;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_NOTIFY_PINCODE_UPDATE;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.EMPTY_STRING;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Base64;

import com.htc.wallet.skrsdk.crypto.Base64Util;
import com.htc.wallet.skrsdk.crypto.ChecksumUtil;
import com.htc.wallet.skrsdk.crypto.util.VerificationUtil;
import com.htc.wallet.skrsdk.keyserver.KeyServerApiService;
import com.htc.wallet.skrsdk.keyserver.KeyServerCallbackWithRetry;
import com.htc.wallet.skrsdk.keyserver.KeyServerManager;
import com.htc.wallet.skrsdk.keyserver.StatusCode;
import com.htc.wallet.skrsdk.keyserver.WebApiResponseUtils;
import com.htc.wallet.skrsdk.keyserver.requestbody.BackupCodePkRequestBody;
import com.htc.wallet.skrsdk.keyserver.requestbody.RestoreCodeRequestBody;
import com.htc.wallet.skrsdk.keyserver.requestbody.RestoreSeedRequestBody;
import com.htc.wallet.skrsdk.keyserver.responsebody.BackupCodePkResponseBody;
import com.htc.wallet.skrsdk.keyserver.responsebody.RestoreCodeResponseBody;
import com.htc.wallet.skrsdk.keyserver.responsebody.RestoreSeedResponseBody;
import com.htc.wallet.skrsdk.messaging.MessageConstants;
import com.htc.wallet.skrsdk.monitor.CheckAutoBackupJobService;
import com.htc.wallet.skrsdk.secretsharing.SeedUtil;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupSourceUtil;
import com.htc.wallet.skrsdk.sqlite.util.RestoreTargetUtil;
import com.htc.wallet.skrsdk.tools.security.attestation.SafetyNetWrapper;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;
import com.htc.wallet.skrsdk.util.PinCodeUtil;
import com.htc.wallet.skrsdk.util.ThreadUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Response;

public class RestoreVerifyAction extends Action {
    private static final String TAG = "RestoreVerifyAction";
    private static final String[] KEY_VERIFY_ENCRYPTION_MESSAGE =
            new String[]{
                    KEY_RESTORE_TARGET_ENC_CODE,
                    KEY_RESTORE_TARGET_ENC_CODE_SIGN,
                    KEY_RESTORE_TARGET_ENCRYPTED_UUID,
                    KEY_RESTORE_TARGET_ENCRYPTED_TOKEN,
                    KEY_RESTORE_TARGET_ENC_WHISPER_PUB,
                    KEY_RESTORE_TARGET_ENC_PUSHY_TOKEN
            };

    private static final String[] KEY_VERIFY_OTHER_MESSAGE =
            new String[]{KEY_RESTORE_SOURCE_PIN_CODE_POSITION, KEY_RESTORE_TARGET_EMAIL_HASH};
    // TODO: Add KEY_VERIFY_TIMESTAMP to KEY_VERIFY_OTHER_MESSAGE

    private static final int MAX_POOL_SIZE = 1;
    private static final ThreadPoolExecutor sThreadPoolExecutor =
            ThreadUtil.newFixedThreadPool(MAX_POOL_SIZE, "skr-restore-action");

    private static final ExecutorService sErrorHandleExecutor = Executors.newSingleThreadExecutor();

    private static final int TIMEOUT = 5; // seconds

    @Override
    int getMessageTypeId() {
        return MessageConstants.TYPE_RESTORE_VERIFY;
    }

    // Amy
    @Override
    public void sendInternal(
            @NonNull final Context context,
            @Nullable final String receiverFcmToken,
            @Nullable final String receiverWhisperPub,
            @Nullable final String receiverPushyToken,
            @NonNull final Map<String, String> messages) {
        if (messages == null) {
            LogUtil.logDebug(TAG, "sendInternal, messages is null");
        }

        if (!isKeysFormatOKInMessage(
                messages, KEY_VERIFY_ENCRYPTION_MESSAGE, KEY_VERIFY_OTHER_MESSAGE)) {
            LogUtil.logDebug(TAG, "sendInternal, KeysFormatInMessage is error");
            return;
        }
        sendMessage(context, receiverFcmToken, receiverWhisperPub, receiverPushyToken, messages);
    }

    // Bob
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
        final VerificationUtil verificationUtil = new VerificationUtil(true);
        if (!isKeysFormatOKInMessage(
                messages, KEY_VERIFY_ENCRYPTION_MESSAGE, KEY_VERIFY_OTHER_MESSAGE)) {
            LogUtil.logDebug(TAG, "onReceiveInternal, KeysFormatInMessage is error");
            return;
        }

        final String restoreTargetEncUUID = messages.get(KEY_RESTORE_TARGET_ENCRYPTED_UUID);
        final String restoreTargetEncToken = messages.get(KEY_RESTORE_TARGET_ENCRYPTED_TOKEN);
        final String restoreTargetEncWhisperPub = messages.get(KEY_RESTORE_TARGET_ENC_WHISPER_PUB);
        final String restoreTargetEncPushyToken = messages.get(KEY_RESTORE_TARGET_ENC_PUSHY_TOKEN);
        final String restoreTargetEncCode = messages.get(KEY_RESTORE_TARGET_ENC_CODE);
        final String restoreTargetEncCodeSign = messages.get(KEY_RESTORE_TARGET_ENC_CODE_SIGN);
        final String restoreTargetEmailHash = messages.get(KEY_RESTORE_TARGET_EMAIL_HASH);
        final String restoreTargetPinCodePosition =
                messages.get(KEY_RESTORE_SOURCE_PIN_CODE_POSITION);

        final String restoreTargetVerifyTimestamp = messages.get(KEY_VERIFY_TIMESTAMP);

        if (TextUtils.isEmpty(restoreTargetEncUUID)) {
            LogUtil.logError(TAG, "restoreTargetEncUUID is null or empty");
            return;
        }
        if (TextUtils.isEmpty(restoreTargetEmailHash)) {
            LogUtil.logError(TAG, "restoreTargetEmailHash is null or empty");
            return;
        }
        if (TextUtils.isEmpty(restoreTargetPinCodePosition)) {
            LogUtil.logError(TAG, "restoreTargetPinCodePosition is null or empty");
            return;
        }
        if (TextUtils.isEmpty(restoreTargetVerifyTimestamp)) {
            LogUtil.logError(TAG, "restoreTargetVerifyTimestamp is null or empty");
            // TODO: Let it pass, until next version
        }

        final String restoreTargetUUID = verificationUtil.decryptMessage(restoreTargetEncUUID);
        final String restoreTargetUUIDHash = ChecksumUtil.generateChecksum(restoreTargetUUID);

        String restoreTargetToken = null;
        String restoreTargetWhisperPub = null;
        String restoreTargetPushyToken = null;
        if (!TextUtils.isEmpty(restoreTargetEncToken)) {
            restoreTargetToken = verificationUtil.decryptMessage(restoreTargetEncToken);
        }
        if (!TextUtils.isEmpty(restoreTargetEncWhisperPub)
                && !TextUtils.isEmpty(restoreTargetEncPushyToken)) {
            restoreTargetWhisperPub = verificationUtil.decryptMessage(restoreTargetEncWhisperPub);
            restoreTargetPushyToken = verificationUtil.decryptMessage(restoreTargetEncPushyToken);
        }

        final String finalRestoreTargetWhisperPub = restoreTargetWhisperPub;
        final String finalRestoreTargetToken = restoreTargetToken;
        final String finalRestoreTargetPushyToken = restoreTargetPushyToken;
        RestoreTargetUtil.get(
                context,
                restoreTargetEmailHash,
                restoreTargetUUIDHash,
                new LoadDataListener() {
                    @Override
                    public void onLoadFinished(
                            BackupSourceEntity backupSourceEntity,
                            BackupTargetEntity backupTargetEntity,
                            RestoreSourceEntity restoreSourceEntity,
                            RestoreTargetEntity restoreTargetEntity) {
                        if (restoreTargetEntity == null) {
                            LogUtil.logWarning(
                                    TAG, "onReceiveInternal, restoreTarget doesn't exist");
                            return;
                        }

                        LogUtil.logDebug(
                                TAG,
                                "Receive message, restore verify from "
                                        + restoreTargetEntity.getName());

                        final String tzIdHash = restoreTargetEntity.getTzIdHash();
                        if (TextUtils.isEmpty(tzIdHash)) {
                            LogUtil.logError(
                                    TAG,
                                    "tzIdHash is null or empty",
                                    new IllegalStateException("tzIdHash is null or empty"));
                            return;
                        }
                        final boolean isTest = restoreTargetEntity.getIsTest();
                        if (isTest) {
                            LogUtil.logInfo(TAG, "isTest is true for testing");
                        }

                        // Check fcm token or whisper pub
                        if (!TextUtils.isEmpty(finalRestoreTargetToken)
                                && !finalRestoreTargetToken.equals(
                                restoreTargetEntity.getFcmToken())) {
                            LogUtil.logError(
                                    TAG,
                                    "restoreTargetToken not match",
                                    new IllegalStateException());
                        }
                        if (!TextUtils.isEmpty(finalRestoreTargetWhisperPub)
                                && !finalRestoreTargetWhisperPub.equals(
                                restoreTargetEntity.getWhisperPub())
                                && !TextUtils.isEmpty(finalRestoreTargetPushyToken)
                                && finalRestoreTargetPushyToken.equals(
                                restoreTargetEntity.getPushyToken())) {
                            LogUtil.logError(
                                    TAG,
                                    "restoreTargetWhisperPub or restoreTargetPushyToken not match",
                                    new IllegalStateException());
                            return;
                        }

                        // Retry when Attest token invalid or expired (401)
                        final AtomicInteger retryDecryptVerifyCodeTimes = new AtomicInteger(0);

                        // decrypt verify code via key server
                        final KeyServerManager keyServerManager =
                                KeyServerManager.getInstance(isTest);
                        final RestoreCodeRequestBody requestBody =
                                new RestoreCodeRequestBody(
                                        context,
                                        keyServerManager.getTzApiKey(context),
                                        restoreTargetEncCode,
                                        restoreTargetEncCodeSign,
                                        tzIdHash);
                        final KeyServerApiService keyServer =
                                keyServerManager.getKeyServerApiService();

                        // device attest token
                        final String attestToken =
                                SafetyNetWrapper.getInstance()
                                        .getDeviceAttestToken(context, isTest, false);
                        if (TextUtils.isEmpty(attestToken)) {
                            LogUtil.logError(TAG, "attestToken is null or empty");
                            return;
                        }

                        Call<RestoreCodeResponseBody> call =
                                keyServer.postVerifyCodeV2(attestToken, requestBody);
                        call.enqueue(
                                new KeyServerCallbackWithRetry<RestoreCodeResponseBody>(call) {
                                    @Override
                                    public void onResponse(
                                            @NonNull Call<RestoreCodeResponseBody> call,
                                            @NonNull Response<RestoreCodeResponseBody> response) {
                                        super.onResponse(call, response);
                                        if (response.isSuccessful()) {
                                            RestoreCodeResponseBody responseBody = response.body();
                                            if (responseBody == null) {
                                                LogUtil.logError(TAG, "responseBody is null");
                                                return;
                                            }
                                            String verifyCode = responseBody.getCode();
                                            if (TextUtils.isEmpty(verifyCode)) {
                                                LogUtil.logError(
                                                        TAG, "verifyCode is null or empty");
                                                return;
                                            }
                                            // Due to verifyCode encrypted by TZ, the last char is
                                            // '\0', we need to remove it
                                            if (verifyCode.length() > 6) {
                                                verifyCode = verifyCode.substring(0, 6);
                                            }
                                            final String finalVerifyCode = verifyCode;

                                            // Use single thread pool to verify
                                            sThreadPoolExecutor.execute(
                                                    new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            // Verify RestoreTarget and send seed or
                                                            // error back
                                                            verifyRestoreTarget(
                                                                    context,
                                                                    restoreTargetEmailHash,
                                                                    restoreTargetUUIDHash,
                                                                    finalRestoreTargetToken,
                                                                    finalRestoreTargetWhisperPub,
                                                                    finalRestoreTargetPushyToken,
                                                                    finalVerifyCode,
                                                                    restoreTargetPinCodePosition,
                                                                    restoreTargetVerifyTimestamp);
                                                        }
                                                    });
                                        } else {
                                            final int responseCode = response.code();
                                            LogUtil.logError(
                                                    TAG,
                                                    "postVerifyCodeV2 failed, e="
                                                            + WebApiResponseUtils.getErrorMsg(
                                                            response));
                                            // Use single work to handle error status
                                            sErrorHandleExecutor.execute(
                                                    new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            // handle error status code 401 (token
                                                            // invalid or expired)
                                                            if (responseCode
                                                                    == StatusCode
                                                                    .TOKEN_INVALID_OR_EXPIRED) {
                                                                if (retryDecryptVerifyCodeTimes
                                                                        .getAndIncrement()
                                                                        >= TOKEN_INVALID_OR_EXPIRED_MAX_RETRY_TIMES) {
                                                                    LogUtil.logDebug(
                                                                            TAG,
                                                                            "postVerifyCodeV2, "
                                                                                    + "401 max "
                                                                                    + "retry "
                                                                                    + "times, "
                                                                                    + "return");
                                                                    return;
                                                                }
                                                                // force acquire a new device attest
                                                                // token
                                                                final String newAttestToken =
                                                                        SafetyNetWrapper
                                                                                .getInstance()
                                                                                .getDeviceAttestToken(
                                                                                        context,
                                                                                        isTest,
                                                                                        true);
                                                                if (TextUtils.isEmpty(
                                                                        newAttestToken)) {
                                                                    LogUtil.logError(
                                                                            TAG,
                                                                            "new attestToken is "
                                                                                    + "null or "
                                                                                    + "empty");
                                                                    return;
                                                                }
                                                                LogUtil.logDebug(
                                                                        TAG,
                                                                        "update postVerifyCodeV2 "
                                                                                + "call with new "
                                                                                + "attestToken");
                                                                Call<RestoreCodeResponseBody>
                                                                        newCall =
                                                                        keyServer
                                                                                .postVerifyCodeV2(
                                                                                        newAttestToken,
                                                                                        requestBody);
                                                                newCall.enqueue(getSelf());
                                                            } else if (responseCode
                                                                    == StatusCode.NOT_FOUND) {
                                                                LogUtil.logDebug(
                                                                        TAG,
                                                                        "api key or device not "
                                                                                + "found, TZIDHash="
                                                                                + tzIdHash);
                                                            }
                                                        }
                                                    });
                                        }
                                    }

                                    @Override
                                    public void onFailure(
                                            @NonNull Call<RestoreCodeResponseBody> call,
                                            @NonNull Throwable t) {
                                        super.onFailure(call, t);
                                        LogUtil.logError(
                                                TAG, "postVerifyCode failed: " + t.getMessage());
                                    }
                                });
                    }
                });
    }

    @WorkerThread
    private void verifyRestoreTarget(
            final Context context,
            final String emailHash,
            final String uuidHash,
            final String token,
            final String restoreTargetWhisperPub,
            final String restoreTargetPushyToken,
            final String verifyCode,
            final String position,
            final String timestamp) {
        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(emailHash)) {
            LogUtil.logError(TAG, "emailHash is null or empty", new IllegalArgumentException());
            return;
        }

        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(TAG, "uuidHash is null or empty", new IllegalArgumentException());
            return;
        }

        if (!PinCodeUtil.isValidPinCode(verifyCode)) {
            LogUtil.logError(TAG, "verifyCode invalid", new IllegalArgumentException());
            return;
        }

        if (TextUtils.isEmpty(position)) {
            LogUtil.logError(TAG, "position is null or empty", new IllegalArgumentException());
            return;
        }

        final CountDownLatch latch = new CountDownLatch(1);

        RestoreTargetUtil.get(
                context,
                emailHash,
                uuidHash,
                new LoadDataListener() {
                    @Override
                    public void onLoadFinished(
                            BackupSourceEntity backupSourceEntity,
                            BackupTargetEntity backupTargetEntity,
                            RestoreSourceEntity restoreSourceEntity,
                            RestoreTargetEntity restoreTargetEntity) {
                        if (restoreTargetEntity == null) {
                            LogUtil.logWarning(
                                    TAG, "verifyRestoreTarget, restoreTarget doesn't exist");
                            // Send error back, prevent Amy wait to timeout
                            sendErrorAction(
                                    context,
                                    token,
                                    restoreTargetWhisperPub,
                                    restoreTargetPushyToken,
                                    position,
                                    timestamp);
                            latch.countDown();
                        } else if (verifyCode.equals(restoreTargetEntity.getPinCode())) {
                            LogUtil.logDebug(TAG, restoreTargetEntity.getName() + "'s verify pass");
                            // pin code verified pass, then send seed to Amy
                            sendSeedToRestoreTarget(
                                    context,
                                    restoreTargetEntity.getFcmToken(),
                                    restoreTargetEntity.getWhisperPub(),
                                    restoreTargetEntity.getPushyToken(),
                                    restoreTargetEntity.getTzIdHash(),
                                    emailHash,
                                    restoreTargetEntity.getBackupUUIDHash(),
                                    uuidHash,
                                    restoreTargetEntity.getPublicKey(),
                                    verifyCode,
                                    position,
                                    timestamp,
                                    restoreTargetEntity.getIsTest());
                            // Update pin code to prevent Amy verify same pin code from different
                            // position's editText
                            final String newPinCode = PinCodeUtil.newPinCode();
                            restoreTargetEntity.setPinCode(newPinCode);
                            RestoreTargetUtil.update(
                                    context,
                                    restoreTargetEntity,
                                    new DatabaseCompleteListener() {
                                        @Override
                                        public void onComplete() {
                                            Intent intent =
                                                    new Intent(ACTION_NOTIFY_PINCODE_UPDATE);
                                            intent.putExtra(Action.KEY_EMAIL_HASH, emailHash);
                                            intent.putExtra(Action.KEY_UUID_HASH, uuidHash);
                                            intent.putExtra(Action.KEY_PIN_CODE, newPinCode);
                                            LocalBroadcastManager.getInstance(context)
                                                    .sendBroadcast(intent);
                                            latch.countDown();
                                        }

                                        @Override
                                        public void onError(Exception exception) {
                                            LogUtil.logError(TAG, "update error, e= " + exception);
                                            latch.countDown();
                                        }
                                    });

                        } else {
                            LogUtil.logDebug(
                                    TAG,
                                    restoreTargetEntity.getName() + "'s verify failed, send error");
                            if (restoreTargetEntity.getRetryTimes() == 0) {
                                String pinCode = PinCodeUtil.newPinCode();
                                Intent intent =
                                        setupLocalBroadcastIntent(emailHash, uuidHash, pinCode);
                                if (intent == null) {
                                    LogUtil.logError(
                                            TAG, "setupLocalBroadcastIntent, intent is null");
                                } else {
                                    // No more retry times, change new pin code
                                    updateRestoreTargetPinCodeAndRetryTimes(
                                            context,
                                            restoreTargetEntity,
                                            pinCode,
                                            RESTORE_RETRY_TIMES);
                                    LocalBroadcastManager.getInstance(context)
                                            .sendBroadcast(intent);
                                }
                            } else {
                                // Just reduce retry times
                                int retryTimesUpdate = restoreTargetEntity.getRetryTimes() - 1;
                                updateRestoreTargetPinCodeAndRetryTimes(
                                        context, restoreTargetEntity, null, retryTimesUpdate);
                            }
                            sendErrorAction(
                                    context,
                                    restoreTargetEntity.getFcmToken(),
                                    restoreTargetEntity.getWhisperPub(),
                                    restoreTargetEntity.getPushyToken(),
                                    position,
                                    timestamp);
                            latch.countDown();
                        }
                    }
                });

        try {
            latch.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LogUtil.logError(TAG, "verifyRestoreTarget, InterruptedException e=" + e);
        }
    }

    private void sendErrorAction(
            @NonNull final Context context,
            final String restoreTargetToken,
            final String restoreTargetWhisperPub,
            final String restoreTargetPushyToken,
            final String restoreTargetPinCodePosition,
            final String restoreTargetVerifyTimestamp) {
        if (context == null || TextUtils.isEmpty(restoreTargetPinCodePosition)) {
            LogUtil.logDebug(TAG, "sendErrorAction, context, restoreTargetWhisperPub, "
                    + "restoreTargetPushyToken or restoreTargetPinCodePosition is null");
            return;
        }
        RestoreErrorAction errorAction = new RestoreErrorAction();
        final Map<String, String> messages = errorAction.createErrorMessage(
                restoreTargetPinCodePosition, restoreTargetVerifyTimestamp);
        if (messages == null) {
            LogUtil.logDebug(TAG, "onReceiveInternal, messages is null");
            return;
        }
        errorAction.sendInternal(
                context,
                restoreTargetToken,
                restoreTargetWhisperPub,
                restoreTargetPushyToken,
                messages);
    }

    private void sendSeedToRestoreTarget(
            @NonNull final Context context,
            final String restoreTargetToken,
            final String restoreTargetWhisperPub,
            final String restoreTargetPushyToken,
            final String tzIdHash,
            final String restoreTargetEmailHash,
            final String backupSourceUUIDHash,
            final String restoreTargetUUIDHash,
            final String restoreTargetPubKeyInRestoreSource,
            final String restoreTargetPinCode,
            final String restoreTargetPinCodePosition,
            final String restoreTargetVerifyTimestamp,
            final boolean isTest) {
        Objects.requireNonNull(context, "context is null");
        final VerificationUtil verificationUtil = new VerificationUtil(true);
        if (TextUtils.isEmpty(tzIdHash)) {
            LogUtil.logError(
                    TAG,
                    "tzIdHash is null or empty",
                    new IllegalArgumentException("tzIdHash is null or empty"));
            return;
        }
        if (TextUtils.isEmpty(restoreTargetEmailHash)) {
            LogUtil.logError(
                    TAG,
                    "restoreTargetEmailHash is null or empty",
                    new IllegalArgumentException("restoreTargetEmailHash is null or empty"));
            return;
        }
        if (TextUtils.isEmpty(backupSourceUUIDHash)) {
            LogUtil.logError(
                    TAG,
                    "backupSourceUUIDHash is null or empty",
                    new IllegalArgumentException("backupSourceUUIDHash is null or empty"));
            return;
        }
        if (TextUtils.isEmpty(restoreTargetPubKeyInRestoreSource)) {
            LogUtil.logError(
                    TAG,
                    "restoreTargetPubKeyInRestoreSource is null or empty",
                    new IllegalArgumentException(
                            "restoreTargetPubKeyInRestoreSource is null or empty"));
            return;
        }
        if (TextUtils.isEmpty(restoreTargetPinCode)) {
            LogUtil.logError(
                    TAG,
                    "restoreTargetPinCode is null or empty",
                    new IllegalArgumentException("restoreTargetPinCode is null or empty"));
            return;
        }
        if (TextUtils.isEmpty(restoreTargetPinCodePosition)) {
            LogUtil.logError(
                    TAG,
                    "restoreTargetPinCodePosition is null or empty",
                    new IllegalArgumentException("restoreTargetPinCodePosition is null or empty"));
            return;
        }

        BackupSourceUtil.getOK(
                context,
                restoreTargetEmailHash,
                backupSourceUUIDHash,
                new LoadDataListener() {
                    @Override
                    public void onLoadFinished(
                            final BackupSourceEntity backupSourceEntity,
                            final BackupTargetEntity backupTargetEntity,
                            RestoreSourceEntity restoreSourceEntity,
                            final RestoreTargetEntity restoreTargetEntity) {
                        if (backupSourceEntity == null) {
                            LogUtil.logError(
                                    TAG, "backupSourceEntity is null", new IllegalStateException());
                        } else {
                            final String myName = backupSourceEntity.getMyName();
                            final String seed = backupSourceEntity.getSeed();
                            final byte[] encSeedByte = Base64Util.decode(seed);
                            final byte[] decSeedByte = verificationUtil.decryptMessage(encSeedByte);
                            final String decSeed =
                                    Base64.encodeToString(decSeedByte, Base64.DEFAULT);
                            final String myDeviceId = PhoneUtil.getDeviceId(context);
                            final String restoreSourceEncryptedDeviceId =
                                    verificationUtil.encryptMessage(
                                            myDeviceId, restoreTargetPubKeyInRestoreSource);
                            final KeyServerManager keyServerManager =
                                    KeyServerManager.getInstance(isTest);
                            final KeyServerApiService keyServerApiService =
                                    keyServerManager.getKeyServerApiService();

                            final RestoreSeedRequestBody requestBody =
                                    new RestoreSeedRequestBody(
                                            context,
                                            keyServerManager.getTzApiKey(context),
                                            tzIdHash,
                                            decSeed);

                            // device attest token
                            final String attestToken =
                                    SafetyNetWrapper.getInstance()
                                            .getDeviceAttestToken(context, isTest, false);
                            if (TextUtils.isEmpty(attestToken)) {
                                LogUtil.logError(TAG, "attestToken is null or empty");
                                return;
                            }

                            // Retry when Attest token invalid or expired (401)
                            final AtomicInteger retryEncryptSeedTimes = new AtomicInteger(0);

                            Call<RestoreSeedResponseBody> call =
                                    keyServerApiService.postEncryptSeedV2(attestToken, requestBody);
                            call.enqueue(
                                    new KeyServerCallbackWithRetry<RestoreSeedResponseBody>(call) {
                                        @Override
                                        public void onResponse(
                                                @NonNull Call<RestoreSeedResponseBody> call,
                                                @NonNull
                                                        Response<RestoreSeedResponseBody>
                                                        response) {
                                            super.onResponse(call, response);
                                            if (response.isSuccessful()) {
                                                final RestoreSeedResponseBody responseBody =
                                                        response.body();
                                                if (responseBody == null) {
                                                    LogUtil.logError(
                                                            TAG,
                                                            "responseBody is null",
                                                            new IllegalStateException(
                                                                    "responseBody is null"));
                                                    return;
                                                }
                                                final String encSeed = responseBody.getEncSeed();
                                                final String encSeedSigned =
                                                        responseBody.getEncSeedSigned();

                                                if (TextUtils.isEmpty(encSeed)) {
                                                    LogUtil.logError(
                                                            TAG,
                                                            "encSeed is null",
                                                            new IllegalStateException(
                                                                    "encSeed is null"));
                                                }
                                                if (TextUtils.isEmpty(encSeedSigned)) {
                                                    LogUtil.logError(
                                                            TAG,
                                                            "encSeedSigned is null",
                                                            new IllegalStateException(
                                                                    "encSeedSigned is null"));
                                                }
                                                if (TextUtils.isEmpty(
                                                        restoreSourceEncryptedDeviceId)) {
                                                    LogUtil.logError(
                                                            TAG,
                                                            "restoreSourceEncryptedDeviceId is "
                                                                    + "null",
                                                            new IllegalStateException(
                                                                    "restoreSourceEncryptedDeviceId is null"));
                                                }

                                                final String myPublicKey =
                                                        verificationUtil.getPublicKeyString();
                                                if (TextUtils.isEmpty(myPublicKey)) {
                                                    LogUtil.logError(
                                                            TAG,
                                                            "myPublicKey is null or empty",
                                                            new IllegalStateException(
                                                                    "myPublicKey is null or "
                                                                            + "empty"));
                                                    return;
                                                }
                                                String seedPublicKey =
                                                        Base64.encodeToString(
                                                                Base64Util.decode(myPublicKey),
                                                                Base64.DEFAULT);

                                                // Retry when Attest token invalid or expired (401)
                                                final AtomicInteger retryEncryptCodePKTimes =
                                                        new AtomicInteger(0);

                                                final BackupCodePkRequestBody codePkRequestBody =
                                                        new BackupCodePkRequestBody(
                                                                context,
                                                                keyServerManager.getTzApiKey(
                                                                        context),
                                                                tzIdHash,
                                                                restoreTargetPinCode,
                                                                seedPublicKey);
                                                Call<BackupCodePkResponseBody> callEncCodePk =
                                                        keyServerApiService.postEncryptCodePKV2(
                                                                attestToken, codePkRequestBody);
                                                callEncCodePk.enqueue(
                                                        new KeyServerCallbackWithRetry<
                                                                BackupCodePkResponseBody>(
                                                                callEncCodePk) {
                                                            @Override
                                                            public void onResponse(
                                                                    @NonNull
                                                                            Call<
                                                                                    BackupCodePkResponseBody>
                                                                            call,
                                                                    @NonNull
                                                                            Response<
                                                                                    BackupCodePkResponseBody>
                                                                            response) {
                                                                super.onResponse(call, response);
                                                                if (response.isSuccessful()) {
                                                                    BackupCodePkResponseBody
                                                                            responseBody =
                                                                            response.body();
                                                                    if (responseBody != null) {
                                                                        // Check encCodePK
                                                                        final String encCodePK =
                                                                                responseBody
                                                                                        .getEncCodePK();
                                                                        if (TextUtils.isEmpty(
                                                                                encCodePK)) {
                                                                            LogUtil.logError(
                                                                                    TAG,
                                                                                    "encCodePK is null or empty",
                                                                                    new IllegalStateException());
                                                                            return;
                                                                        }
                                                                        if (encCodePK.length()
                                                                                != SeedUtil
                                                                                .ENC_CODE_PK_LENGTH) {
                                                                            LogUtil.logDebug(
                                                                                    TAG,
                                                                                    "incorrect encCodePK length "
                                                                                            + encCodePK
                                                                                            .length());
                                                                        }

                                                                        // Check encCodePKSigned
                                                                        final String
                                                                                encCodePKSigned =
                                                                                responseBody
                                                                                        .getEncCodePKSigned();
                                                                        if (TextUtils.isEmpty(
                                                                                encCodePKSigned)) {
                                                                            LogUtil.logError(
                                                                                    TAG,
                                                                                    "encCodePKSigned is null or empty",
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
                                                                                responseBody
                                                                                        .getEncAesKey();
                                                                        if (TextUtils.isEmpty(
                                                                                encAesKey)) {
                                                                            LogUtil.logError(
                                                                                    TAG,
                                                                                    "encAesKey is null or empty",
                                                                                    new IllegalStateException());
                                                                            return;
                                                                        }
                                                                        if (encAesKey.length()
                                                                                != SeedUtil
                                                                                .ENC_AES_KEY_LENGTH) {
                                                                            LogUtil.logDebug(
                                                                                    TAG,
                                                                                    "incorrect encAesKey length "
                                                                                            + encAesKey
                                                                                            .length());
                                                                        }

                                                                        // Check encAesKeySigned
                                                                        final String
                                                                                encAesKeySigned =
                                                                                responseBody
                                                                                        .getEncAesKeySigned();
                                                                        if (TextUtils.isEmpty(
                                                                                encAesKeySigned)) {
                                                                            LogUtil.logError(
                                                                                    TAG,
                                                                                    "encAesKeySigned is null or empty",
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
                                                                                message =
                                                                                new HashMap<>();
                                                                        message.put(
                                                                                KEY_RESTORE_SOURCE_SEED,
                                                                                encSeed);
                                                                        message.put(
                                                                                KEY_RESTORE_ENC_SEED_SIGN,
                                                                                encSeedSigned);
                                                                        message.put(
                                                                                KEY_RESTORE_SOURCE_PIN_CODE_POSITION,
                                                                                restoreTargetPinCodePosition);
                                                                        message.put(
                                                                                KEY_RESTORE_SOURCE_ENCRYPTED_UUID,
                                                                                restoreSourceEncryptedDeviceId);
                                                                        message.put(
                                                                                KEY_RESTORE_SOURCE_NAME,
                                                                                myName);
                                                                        message.put(
                                                                                KEY_ENCRYPTED_CODE_PK,
                                                                                encCodePK);
                                                                        message.put(
                                                                                KEY_ENCRYPTED_CODE_PK_SIGNED,
                                                                                encCodePKSigned);
                                                                        message.put(
                                                                                KEY_ENCRYPTED_AES_KEY,
                                                                                encAesKey);
                                                                        message.put(
                                                                                KEY_ENCRYPTED_AES_KEY_SIGNED,
                                                                                encAesKeySigned);
                                                                        message.put(
                                                                                KEY_VERIFY_TIMESTAMP,
                                                                                restoreTargetVerifyTimestamp);

                                                                        // V1 no save myName, will
                                                                        // make Amy rebuild backup
                                                                        // failed, don't save
                                                                        if (!TextUtils.isEmpty(
                                                                                myName)) {
                                                                            backupSourceEntity
                                                                                    .setStatus(
                                                                                            BACKUP_SOURCE_STATUS_REQUEST_AUTO_BACKUP);
                                                                            // Fixed issue, After
                                                                            // restore fcm token
                                                                            // will change, need to
                                                                            // set again.
                                                                            backupSourceEntity
                                                                                    .setFcmToken(
                                                                                            restoreTargetToken);
                                                                            backupSourceEntity
                                                                                    .setWhisperPub(
                                                                                            restoreTargetWhisperPub);
                                                                            backupSourceEntity
                                                                                    .setPushyToken(
                                                                                            restoreTargetPushyToken);
                                                                            backupSourceEntity
                                                                                    .setPublicKey(
                                                                                            restoreTargetPubKeyInRestoreSource);
                                                                            backupSourceEntity
                                                                                    .setUUIDHash(
                                                                                            restoreTargetUUIDHash);
                                                                            backupSourceEntity
                                                                                    .setSeed(
                                                                                            EMPTY_STRING);
                                                                            backupSourceEntity
                                                                                    .setCheckSum(
                                                                                            EMPTY_STRING);
                                                                            backupSourceEntity
                                                                                    .setIsTest(
                                                                                            isTest);
                                                                            backupSourceEntity
                                                                                    .setTimeStamp(
                                                                                            System
                                                                                                    .currentTimeMillis());
                                                                            LogUtil.logInfo(
                                                                                    TAG,
                                                                                    "Save new backupSource");
                                                                            BackupSourceUtil.put(
                                                                                    context,
                                                                                    backupSourceEntity,
                                                                                    new DatabaseCompleteListener() {
                                                                                        @Override
                                                                                        public void
                                                                                        onComplete() {
                                                                                            LogUtil
                                                                                                    .logInfo(
                                                                                                            TAG,
                                                                                                            "Send restore seed");
                                                                                            new RestoreSeedAction()
                                                                                                    .sendInternal(
                                                                                                            context,
                                                                                                            restoreTargetToken,
                                                                                                            restoreTargetWhisperPub,
                                                                                                            restoreTargetPushyToken,
                                                                                                            message);

                                                                                            // Schedule check auto backup
                                                                                            CheckAutoBackupJobService
                                                                                                    .enqueue(
                                                                                                            context,
                                                                                                            restoreTargetEmailHash,
                                                                                                            restoreTargetUUIDHash);
                                                                                        }

                                                                                        @Override
                                                                                        public void
                                                                                        onError(
                                                                                                Exception
                                                                                                        exception) {
                                                                                            LogUtil
                                                                                                    .logError(
                                                                                                            TAG,
                                                                                                            "put error, e= "
                                                                                                                    + exception);
                                                                                        }
                                                                                    });
                                                                        } else {
                                                                            LogUtil.logInfo(
                                                                                    TAG,
                                                                                    "myName is empty, just send restore seed");
                                                                            new RestoreSeedAction()
                                                                                    .sendInternal(
                                                                                            context,
                                                                                            restoreTargetToken,
                                                                                            restoreTargetWhisperPub,
                                                                                            restoreTargetPushyToken,
                                                                                            message);
                                                                        }
                                                                    } else {
                                                                        LogUtil.logError(
                                                                                TAG,
                                                                                "responseBody is "
                                                                                        + "null",
                                                                                new IllegalStateException());
                                                                    }
                                                                } else {
                                                                    final int responseCode =
                                                                            response.code();
                                                                    LogUtil.logError(
                                                                            TAG,
                                                                            "postEncryptCodePKV2 "
                                                                                    + "failed, e="
                                                                                    + WebApiResponseUtils
                                                                                    .getErrorMsg(
                                                                                            response));
                                                                    // Use single work to handle
                                                                    // error status
                                                                    sErrorHandleExecutor.execute(
                                                                            new Runnable() {
                                                                                @Override
                                                                                public void run() {
                                                                                    // handle error
                                                                                    // status code
                                                                                    // 401 (token
                                                                                    // invalid or
                                                                                    // expired)
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
                                                                                            return;
                                                                                        }
                                                                                        // force
                                                                                        // acquire a
                                                                                        // new
                                                                                        // device
                                                                                        // attest
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
                                                                                        LogUtil
                                                                                                .logDebug(
                                                                                                        TAG,
                                                                                                        "update postEncryptCodePKV2 call with new attestToken");
                                                                                        Call<
                                                                                                BackupCodePkResponseBody>
                                                                                                newCall =
                                                                                                keyServerApiService
                                                                                                        .postEncryptCodePKV2(
                                                                                                                newAttestToken,
                                                                                                                codePkRequestBody);
                                                                                        newCall
                                                                                                .enqueue(
                                                                                                        getSelf());
                                                                                    } else if (
                                                                                            responseCode
                                                                                                    == StatusCode
                                                                                                    .NOT_FOUND) {

                                                                                        LogUtil
                                                                                                .logDebug(
                                                                                                        TAG,
                                                                                                        "api key or device not found, TZIDHash="
                                                                                                                + tzIdHash);
                                                                                    }
                                                                                }
                                                                            });
                                                                }
                                                            }

                                                            @Override
                                                            public void onFailure(
                                                                    @NonNull
                                                                            Call<
                                                                                    BackupCodePkResponseBody>
                                                                            call,
                                                                    @NonNull Throwable t) {
                                                                super.onFailure(call, t);
                                                                LogUtil.logError(
                                                                        TAG,
                                                                        "postEncryptCodePK failed");
                                                            }
                                                        });
                                            } else {
                                                final int responseCode = response.code();
                                                LogUtil.logError(
                                                        TAG,
                                                        "postEncryptSeedV2 failed, e="
                                                                + WebApiResponseUtils.getErrorMsg(
                                                                response));
                                                // Use single work to handle error status
                                                sErrorHandleExecutor.execute(
                                                        new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                // handle error status code 401
                                                                // (token invalid or expired)
                                                                if (responseCode
                                                                        == StatusCode
                                                                        .TOKEN_INVALID_OR_EXPIRED) {
                                                                    if (retryEncryptSeedTimes
                                                                            .getAndIncrement()
                                                                            >= TOKEN_INVALID_OR_EXPIRED_MAX_RETRY_TIMES) {
                                                                        LogUtil.logDebug(
                                                                                TAG,
                                                                                "postEncryptSeedV2, 401 max retry times, return");
                                                                        return;
                                                                    }
                                                                    // force acquire a new device
                                                                    // attest token
                                                                    final String newAttestToken =
                                                                            SafetyNetWrapper
                                                                                    .getInstance()
                                                                                    .getDeviceAttestToken(
                                                                                            context,
                                                                                            isTest,
                                                                                            true);
                                                                    if (TextUtils.isEmpty(
                                                                            newAttestToken)) {
                                                                        LogUtil.logError(
                                                                                TAG,
                                                                                "new attestToken "
                                                                                        + "is "
                                                                                        + "null "
                                                                                        + "or "
                                                                                        + "empty");
                                                                        return;
                                                                    }
                                                                    LogUtil.logDebug(
                                                                            TAG,
                                                                            "update postEncryptSeedV2 call with new attestToken");
                                                                    Call<RestoreSeedResponseBody>
                                                                            newCall =
                                                                            keyServerApiService
                                                                                    .postEncryptSeedV2(
                                                                                            newAttestToken,
                                                                                            requestBody);
                                                                    newCall.enqueue(getSelf());
                                                                } else if (responseCode
                                                                        == StatusCode.NOT_FOUND) {
                                                                    LogUtil.logDebug(
                                                                            TAG,
                                                                            "api key or device "
                                                                                    + "not found,"
                                                                                    + " TZIDHash="
                                                                                    + tzIdHash);
                                                                }
                                                            }
                                                        });
                                            }
                                        }

                                        @Override
                                        public void onFailure(
                                                @NonNull Call<RestoreSeedResponseBody> call,
                                                @NonNull Throwable t) {
                                            super.onFailure(call, t);
                                        }
                                    });
                        }
                    }
                });
    }

    private Intent setupLocalBroadcastIntent(
            @NonNull String emailHash, @NonNull String uuidHash, final String pinCode) {
        if (TextUtils.isEmpty(emailHash)) {
            LogUtil.logError(
                    TAG,
                    "setupLocalBroadcastIntent, emailHash is null",
                    new IllegalStateException());
            return null;
        }
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(
                    TAG,
                    "setupLocalBroadcastIntent, uuidHash is null",
                    new IllegalStateException());
            return null;
        }
        Intent intent = new Intent(ACTION_NOTIFY_PINCODE_UPDATE);
        intent.putExtra(KEY_PIN_CODE, pinCode);
        intent.putExtra(KEY_EMAIL_HASH, emailHash);
        intent.putExtra(KEY_UUID_HASH, uuidHash);
        return intent;
    }

    @WorkerThread
    private void updateRestoreTargetPinCodeAndRetryTimes(
            @NonNull final Context context,
            @NonNull final RestoreTargetEntity restoreTarget,
            @Nullable final String pinCode,
            int retryTimes) {
        Objects.requireNonNull(context, "context is null");
        if (restoreTarget == null) {
            LogUtil.logError(TAG, "updateRestoreTargetPinCodeAndRetryTimes, restoreTarget is null");
            return;
        }

        // We need to lock this thread, prevent new verify comes before update finish
        final CountDownLatch latch = new CountDownLatch(1);

        restoreTarget.setRetryTimes(retryTimes);
        if (!TextUtils.isEmpty(pinCode)) {
            restoreTarget.setPinCode(pinCode);
        }

        RestoreTargetUtil.update(
                context,
                restoreTarget,
                new DatabaseCompleteListener() {
                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Exception exception) {
                        LogUtil.logError(TAG, "update error, e= " + exception);
                        latch.countDown();
                    }
                });

        try {
            latch.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LogUtil.logError(
                    TAG, "updateRestoreTargetPinCodeAndRetryTimes, InterruptedException e=" + e);
        }
    }
}
