package com.htc.wallet.skrsdk.action;

import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_CLOSE_SHARING_ACTIVITY;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_TRIGGER_BROADCAST;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.crypto.ChecksumUtil;
import com.htc.wallet.skrsdk.crypto.util.VerificationUtil;
import com.htc.wallet.skrsdk.messaging.MessageConstants;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.util.RestoreTargetUtil;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.HashMap;
import java.util.Map;

public class RestoreOKAction extends Action {
    private static final String TAG = "RestoreOKAction";

    private static final String[] KEY_OK_ENCRYPTION_MESSAGE =
            new String[]{KEY_RESTORE_TARGET_ENCRYPTED_UUID};

    private static final String[] KEY_ERROR_OTHER_MESSAGE =
            new String[]{KEY_RESTORE_TARGET_EMAIL_HASH};

    @Override
    int getMessageTypeId() {
        return MessageConstants.TYPE_RESTORE_OK;
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
                messages, KEY_OK_ENCRYPTION_MESSAGE, KEY_ERROR_OTHER_MESSAGE)) {
            LogUtil.logDebug(TAG, "KeysFormatInMessage is error");
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
        if (!isKeysFormatOKInMessage(messages, null, KEY_ERROR_OTHER_MESSAGE)) {
            LogUtil.logDebug(TAG, "KeysFormatInMessage is error");
            return;
        }

        final VerificationUtil verificationUtil = new VerificationUtil(true);
        final String restoreTargetEncryptedUUID = messages.get(KEY_RESTORE_TARGET_ENCRYPTED_UUID);
        final String restoreTargetUUID =
                verificationUtil.decryptMessage(restoreTargetEncryptedUUID);
        final String restoreTargetUUIDHash = ChecksumUtil.generateChecksum(restoreTargetUUID);
        final String restoreTargetEmailHash = messages.get(KEY_RESTORE_TARGET_EMAIL_HASH);

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
                            LogUtil.logDebug(TAG, "Receive restore ok without previous status");
                            return;
                        } else {
                            LogUtil.logDebug(
                                    TAG,
                                    "Receive message, restore ok from "
                                            + restoreTargetEntity.getName());
                            RestoreTargetUtil.remove(
                                    context, restoreTargetEmailHash, restoreTargetUUIDHash);
                        }

                        final LocalBroadcastManager localBroadcastManager =
                                LocalBroadcastManager.getInstance(context);
                        // Notify SocialKeyRecoveryRequestActivity and EntryActivity to update UI
                        Intent intentToNotifyUpdateUI = new Intent(ACTION_TRIGGER_BROADCAST);
                        localBroadcastManager.sendBroadcast(intentToNotifyUpdateUI);

                        // Receive OK, close VerificationSharingActivity
                        Intent intent = new Intent(ACTION_CLOSE_SHARING_ACTIVITY);
                        intent.putExtra(KEY_EMAIL_HASH, restoreTargetEmailHash);
                        intent.putExtra(KEY_UUID_HASH, restoreTargetUUIDHash);
                        localBroadcastManager.sendBroadcast(intent);
                    }
                });
    }

    public Map<String, String> createOKMessage(final String encryptedUUID, final String emailHash) {
        if (TextUtils.isEmpty(encryptedUUID) || TextUtils.isEmpty(emailHash)) {
            LogUtil.logDebug(TAG, "createOKMessage, encryptedUUID or emailHash is null");
            return null;
        }
        final Map<String, String> messages = new HashMap<>();
        messages.put(KEY_RESTORE_TARGET_ENCRYPTED_UUID, encryptedUUID);
        messages.put(KEY_RESTORE_TARGET_EMAIL_HASH, emailHash);
        return messages;
    }
}
