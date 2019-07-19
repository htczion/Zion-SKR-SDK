package com.htc.wallet.skrsdk.action;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

// Resend name to Bob, because V1 haven't store "myName" in OK status.
public class ResendNameAction extends Action {
    private static final String TAG = "ResendNameAction";

    @Override
    int getMessageTypeId() {
        return MessageConstants.TYPE_RESEND_NAME;
    }

    // Amy
    @Override
    public void sendInternal(
            @NonNull final Context context,
            @Nullable final String receiverFcmToken,
            @Nullable final String receiverWhisperPub,
            @Nullable final String receiverPushyToken,
            @NonNull final Map<String, String> messages) {
        final String publicKey = messages.get(KEY_PUBLIC_KEY);
        if (TextUtils.isEmpty(publicKey)) {
            LogUtil.logError(TAG, "publicKey is null or empty", new IllegalStateException());
            return;
        }
        final String name = messages.get(KEY_NAME);
        if (TextUtils.isEmpty(name)) {
            LogUtil.logError(TAG, "name is null or empty", new IllegalStateException());
            return;
        }
        final String myEmailHash = PhoneUtil.getSKREmailHash(context);
        if (TextUtils.isEmpty(myEmailHash)) {
            LogUtil.logError(TAG, "myEmailHash is null or empty", new IllegalStateException());
            return;
        }
        final String myUUID = PhoneUtil.getSKRID(context);
        if (TextUtils.isEmpty(myUUID)) {
            LogUtil.logError(TAG, "myUUID is null or empty", new IllegalStateException());
            return;
        }
        VerificationUtil verificationUtil = new VerificationUtil(false);
        final String encUUID = verificationUtil.encryptMessage(myUUID, publicKey);
        if (TextUtils.isEmpty(encUUID)) {
            LogUtil.logError(TAG, "encUUID is null or empty", new IllegalStateException());
            return;
        }

        LogUtil.logDebug(TAG, "Resend name to " + name);

        Map<String, String> msgToSend = new ArrayMap<>();
        msgToSend.put(KEY_EMAIL_HASH, myEmailHash);
        msgToSend.put(KEY_ENCRYPTED_UUID, encUUID);
        msgToSend.put(KEY_NAME, name);
        sendMessage(context, receiverFcmToken, receiverWhisperPub, receiverPushyToken, msgToSend);
    }

    // Bob
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
        final String emailHash = messages.get(KEY_EMAIL_HASH);
        if (TextUtils.isEmpty(emailHash)) {
            LogUtil.logError(TAG, "emailHash is null or empty");
            return;
        }
        VerificationUtil verificationUtil = new VerificationUtil(true);
        String uuid = messages.get(KEY_ENCRYPTED_UUID);
        if (TextUtils.isEmpty(uuid)) {
            LogUtil.logError(TAG, "Encrypted UUID is null or empty", new IllegalStateException());
            return;
        } else {
            uuid = verificationUtil.decryptMessage(uuid);
            if (TextUtils.isEmpty(uuid)) {
                LogUtil.logError(TAG, "UUID is null or empty", new IllegalStateException());
                return;
            }
        }
        final String uuidHash = ChecksumUtil.generateChecksum(uuid);
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(TAG, "UUID hash is null or empty", new IllegalStateException());
            return;
        }
        final String name = messages.get(KEY_NAME);
        if (TextUtils.isEmpty(name)) {
            LogUtil.logError(TAG, "name is null or empty", new IllegalStateException());
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
                            LogUtil.logDebug(
                                    TAG, "Receive a ResendNameAction without backupSourceEntity");
                            return;
                        }
                        LogUtil.logDebug(
                                TAG,
                                "Receive message, resend name from "
                                        + backupSourceEntity.getName());
                        if (!TextUtils.isEmpty(backupSourceEntity.getMyName())) {
                            LogUtil.logDebug(
                                    TAG,
                                    "Receive a ResendNameAction, backupSourceEntity has myName "
                                            + "already");
                        } else {
                            backupSourceEntity.setMyName(name);
                            BackupSourceUtil.update(
                                    context,
                                    backupSourceEntity,
                                    new DatabaseCompleteListener() {
                                        @Override
                                        public void onComplete() {
                                            LogUtil.logInfo(TAG, "reassigned myName success");
                                        }

                                        @Override
                                        public void onError(Exception exception) {
                                            LogUtil.logError(TAG, "update error, e= " + exception);
                                        }
                                    });
                        }
                    }
                });
    }
}
