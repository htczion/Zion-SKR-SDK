package com.htc.wallet.skrsdk.action;

import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_NOTIFY_BACKUP_FULL;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_TRIGGER_BROADCAST;

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

import java.util.Map;

public class BackupFullAction extends Action {
    private static final String TAG = "BackupFullAction";

    @Override
    int getMessageTypeId() {
        return MessageConstants.TYPE_BACKUP_FULL;
    }

    // Amy need to notify Bob that her backupTargets are full
    @Override
    public void sendInternal(
            @NonNull final Context context,
            @Nullable final String receiverFcmToken,
            @Nullable final String receiverWhisperPub,
            @Nullable final String receiverPushyToken,
            @NonNull final Map<String, String> messages) {

        final String myEmailHash = PhoneUtil.getSKREmailHash(context);
        final String myUUID = PhoneUtil.getSKRID(context);
        final String publicKey = messages.get(KEY_PUBLIC_KEY);
        if (TextUtils.isEmpty(publicKey)) {
            LogUtil.logError(
                    TAG,
                    "publicKey is null or empty",
                    new IllegalStateException("publicKey is null or empty"));
            return;
        }
        VerificationUtil verificationUtil = new VerificationUtil(false);

        final String encUUID = verificationUtil.encryptMessage(myUUID, publicKey);
        if (TextUtils.isEmpty(encUUID)) {
            LogUtil.logError(
                    TAG,
                    "encUUID is null or empty",
                    new IllegalStateException("encUUID is null or empty"));
            return;
        }
        Map<String, String> msgToSend = new ArrayMap<>();
        msgToSend.put(KEY_EMAIL_HASH, myEmailHash);
        msgToSend.put(KEY_ENCRYPTED_UUID, encUUID);
        msgToSend.put(KEY_BACKUP_FULL, MSG_BACKUP_FULL);
        sendMessage(context, receiverFcmToken, receiverWhisperPub, receiverPushyToken, msgToSend);

        Intent intentToNotifyUpdateUI = new Intent(ACTION_TRIGGER_BROADCAST);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intentToNotifyUpdateUI);
    }

    // Bob will receive a notification (a dialog show up) about Amy's full info
    @Override
    void onReceiveInternal(
            @NonNull final Context context,
            @Nullable final String senderFcmToken,
            @Nullable final String myFcmToken,
            @Nullable final String senderWhisperPub,
            @Nullable final String myWhisperPub,
            @Nullable final String senderPushyToken,
            @Nullable final String myPushyToken,
            @NonNull final Map<String, String> messages) {
        String backupFull = messages.get(KEY_BACKUP_FULL);
        if (!backupFull.equals(MSG_BACKUP_FULL)) {
            LogUtil.logError(TAG, "backupFull message doesn't match");
            return;
        }

        final String emailHash = messages.get(KEY_EMAIL_HASH);
        if (TextUtils.isEmpty(emailHash)) {
            LogUtil.logError(
                    TAG,
                    "emailHash is null or empty",
                    new IllegalStateException("emailHash is null or empty"));
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

        BackupSourceUtil.getWithUUIDHash(
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
                        if (backupSourceEntity == null) {
                            LogUtil.logError(
                                    TAG,
                                    "backupSource is null",
                                    new IllegalStateException("backupSource is null"));
                            return;
                        }

                        LogUtil.logDebug(
                                TAG,
                                "Update the status of "
                                        + backupSourceEntity.getName()
                                        + "'s backupSource to BACKUP_SOURCE_STATUS_FULL");
                        backupSourceEntity.updateStatusToFull();
                        BackupSourceUtil.update(
                                context,
                                backupSourceEntity,
                                new DatabaseCompleteListener() {
                                    @Override
                                    public void onComplete() {
                                        Intent intentNotifyFull =
                                                new Intent(ACTION_NOTIFY_BACKUP_FULL);
                                        intentNotifyFull.putExtra(KEY_EMAIL_HASH, emailHash);
                                        intentNotifyFull.putExtra(KEY_UUID_HASH, uuidHash);
                                        LocalBroadcastManager.getInstance(context)
                                                .sendBroadcast(intentNotifyFull);
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
