package com.htc.wallet.skrsdk.action;

import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.PIN_CODE_ERROR;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_NOTIFY_UI_UPDATE_AFTER_ERROR_PINCODE;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.htc.wallet.skrsdk.crypto.ChecksumUtil;
import com.htc.wallet.skrsdk.crypto.util.VerificationUtil;
import com.htc.wallet.skrsdk.messaging.MessageConstants;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupSourceUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;
import com.htc.wallet.skrsdk.util.RetryUtil;

import java.util.Map;

public class BackupErrorAction extends Action {
    private static final String TAG = "BackupErrorAction";

    @Override
    int getMessageTypeId() {
        return MessageConstants.TYPE_BACKUP_ERROR;
    }

    // Amy
    @Override
    public void sendInternal(
            @NonNull final Context context,
            @Nullable final String receiverFcmToken,
            @Nullable final String receiverWhisperPub,
            @Nullable final String receiverPushyToken,
            @NonNull final Map<String, String> messages) {

        final String retryTimesStr = messages.get(KEY_RETRY_TIMES);
        if (TextUtils.isEmpty(retryTimesStr)) {
            LogUtil.logError(
                    TAG,
                    "retryTimesStr is null or empty",
                    new IllegalStateException("retryTimesStr is null or empty"));
            return;
        }
        final String publicKey = messages.get(KEY_PUBLIC_KEY);
        if (TextUtils.isEmpty(publicKey)) {
            LogUtil.logError(
                    TAG,
                    "publicKey is null or empty",
                    new IllegalStateException("publicKey is null or empty"));
            return;
        }

        VerificationUtil verificationUtil = new VerificationUtil(false);
        final String myEmailHash = PhoneUtil.getSKREmailHash(context);
        if (TextUtils.isEmpty(myEmailHash)) {
            LogUtil.logError(
                    TAG,
                    "myEmailHash is null or empty",
                    new IllegalStateException("myEmailHash is null or empty"));
            return;
        }
        final String myUUID = PhoneUtil.getSKRID(context);
        if (TextUtils.isEmpty(myUUID)) {
            LogUtil.logError(
                    TAG,
                    "myUUID is null or empty",
                    new IllegalStateException("myUUID is null or empty"));
            return;
        }
        final String encUUID = verificationUtil.encryptMessage(myUUID, publicKey);
        if (TextUtils.isEmpty(encUUID)) {
            LogUtil.logError(
                    TAG,
                    "encUUID is null or empty",
                    new IllegalStateException("encUUID is null or empty"));
            return;
        }

        Map<String, String> map = new ArrayMap<>();
        map.put(KEY_ERROR, MSG_ERROR);
        map.put(KEY_EMAIL_HASH, myEmailHash);
        map.put(KEY_ENCRYPTED_UUID, encUUID);
        map.put(KEY_RETRY_TIMES, retryTimesStr);
        sendMessage(context, receiverFcmToken, receiverWhisperPub, receiverPushyToken, map);
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

        final String msg = messages.get(KEY_ERROR);
        if (msg == null || !msg.equals(MSG_ERROR)) {
            LogUtil.logError(TAG, "Error message not match");
            return;
        }
        final String emailHash = messages.get(KEY_EMAIL_HASH);
        if (TextUtils.isEmpty(emailHash)) {
            LogUtil.logError(TAG, "emailHash is null or empty");
            return;
        }
        VerificationUtil verificationUtil = new VerificationUtil(true);
        String uuid = messages.get(KEY_ENCRYPTED_UUID);
        if (TextUtils.isEmpty(uuid)) {
            LogUtil.logError(
                    TAG,
                    "Encrypted UUID is null or empty",
                    new IllegalStateException("Encrypted UUID is null or empty"));
            return;
        } else {
            uuid = verificationUtil.decryptMessage(uuid);
            if (TextUtils.isEmpty(uuid)) {
                LogUtil.logError(
                        TAG,
                        "UUID is null or empty",
                        new IllegalStateException("UUID is null or empty"));
                return;
            }
        }
        final String uuidHash = ChecksumUtil.generateChecksum(uuid);
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(
                    TAG,
                    "UUIDHash is null or empty",
                    new IllegalStateException("UUIDHash is null or empty"));
            return;
        }
        final String retryTimesStr = messages.get(KEY_RETRY_TIMES);
        if (TextUtils.isEmpty(retryTimesStr)) {
            LogUtil.logError(
                    TAG,
                    "retryTimesStr is null or empty",
                    new IllegalStateException("retryTimesStr is null or empty"));
            return;
        }
        final int retryTimes = Integer.parseInt(retryTimesStr);
        if (retryTimes < 0) {
            LogUtil.logError(
                    TAG, "retryTimes incorrect", new IllegalStateException("retryTimes incorrect"));
            return;
        }

        BackupSourceUtil.getWithUUIDHash(
                context,
                emailHash,
                uuidHash,
                new LoadDataListener() {
                    @Override
                    public void onLoadFinished(
                            final BackupSourceEntity backupSourceEntity,
                            BackupTargetEntity backupTargetEntity,
                            RestoreSourceEntity restoreSourceEntity,
                            RestoreTargetEntity restoreTargetEntity) {
                        if (backupSourceEntity == null
                                || !backupSourceEntity.compareStatus(
                                BACKUP_SOURCE_STATUS_REQUEST)) {
                            LogUtil.logDebug(TAG, "backupSource is null or incorrect status");
                            return;
                        }
                        String name = backupSourceEntity.getName();
                        LogUtil.logDebug(
                                TAG, "Receive message, verify " + name + "'s pin code error");
                        if (backupSourceEntity.getRetryTimes() != retryTimes) {
                            if (retryTimes % RetryUtil.MAXIMUM_TRY_NUMBER == 0) {
                                backupSourceEntity.setRetryWaitStartTime(
                                        System.currentTimeMillis());
                            }
                        }
                        backupSourceEntity.setRetryTimes(retryTimes);
                        backupSourceEntity.setIsPinCodeError(PIN_CODE_ERROR);
                        BackupSourceUtil.update(
                                context,
                                backupSourceEntity,
                                new DatabaseCompleteListener() {
                                    @Override
                                    public void onComplete() {
                                        Intent intent =
                                                new Intent(
                                                        ACTION_NOTIFY_UI_UPDATE_AFTER_ERROR_PINCODE);
                                        intent.putExtra(KEY_EMAIL_HASH, emailHash);
                                        intent.putExtra(KEY_UUID_HASH, uuidHash);
                                        intent.putExtra(KEY_RETRY_TIMES, retryTimes);
                                        intent.putExtra(
                                                KEY_RETRY_WAIT_START_TIME,
                                                backupSourceEntity.getRetryWaitStartTime());
                                        LocalBroadcastManager.getInstance(context)
                                                .sendBroadcast(intent);
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
