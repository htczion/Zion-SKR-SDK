package com.htc.wallet.skrsdk.action;

import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_RECEIVE_DELETE;
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
import com.htc.wallet.skrsdk.sqlite.util.BackupTargetUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;

import java.util.Map;
import java.util.Objects;

public class BackupDeleteAction extends Action {
    private static final String TAG = "BackupDeleteAction";

    @Override
    int getMessageTypeId() {
        return MessageConstants.TYPE_BACKUP_DELETE;
    }

    // Amy
    @Override
    public void sendInternal(
            @NonNull final Context context,
            @Nullable final String receiverFcmToken,
            @Nullable final String receiverWhisperPub,
            @Nullable final String receiverPushyToken,
            @NonNull final Map<String, String> messages) {
        Objects.requireNonNull(messages, "messages is null");

        final String uuidHash = messages.get(KEY_UUID_HASH);
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(
                    TAG,
                    "uuidHash is null or empty",
                    new IllegalStateException("uuidHash is null or empty"));
            return;
        }

        // This value is only use for delete legacy V1 backup data automatically (user agreed) and
        // SkrSecurityUpdateDialogV1's CONTINUE
        // This value will be null while user delete or click resend manually
        final String backupVersion = messages.get(KEY_BACKUP_VERSION);

        BackupTargetUtil.get(
                context,
                uuidHash,
                new LoadDataListener() {
                    @Override
                    public void onLoadFinished(
                            BackupSourceEntity backupSourceEntity,
                            BackupTargetEntity backupTargetEntity,
                            RestoreSourceEntity restoreSourceEntity,
                            RestoreTargetEntity restoreTargetEntity) {
                        if (backupTargetEntity == null) {
                            LogUtil.logWarning(TAG, "target is null");

                            // This value is used to delete the legacy V1 or the incomplete deleted
                            // action in case the backupTarget has been
                            // deleted by Amy.
                            final String publicKey = messages.get(KEY_PUBLIC_KEY);

                            if (!TextUtils.isEmpty(backupVersion)
                                    && !TextUtils.isEmpty(publicKey)) {
                                LogUtil.logInfo(
                                        TAG, "backupVersion and publicKey are not empty, send it");
                                sendDeleteMessage(
                                        context,
                                        receiverFcmToken,
                                        receiverWhisperPub,
                                        receiverPushyToken,
                                        publicKey,
                                        backupVersion);
                            } else if (!TextUtils.isEmpty(publicKey)) {
                                LogUtil.logInfo(
                                        TAG,
                                        "backupTargetEntity is null and publicKey is not empty, "
                                                + "send it");
                                sendDeleteMessage(
                                        context,
                                        receiverFcmToken,
                                        receiverWhisperPub,
                                        receiverPushyToken,
                                        publicKey,
                                        null);
                            } else {
                                LogUtil.logWarning(
                                        TAG, "backupVersion or publicKey is invalid, ignore it");
                            }
                            return;
                        }

                        final String resendMsg = messages.get(KEY_IS_RESEND);
                        // If that is resend flow, do not remove backup target
                        if (MSG_IS_RESEND.equals(resendMsg)) {
                            LogUtil.logDebug(
                                    TAG, "Resend flow. No need to delete the backup target");
                        } else {
                            BackupTargetUtil.remove(context, uuidHash,
                                    new DatabaseCompleteListener() {
                                        @Override
                                        public void onComplete() {
                                            // Only remove backupTarget need to update the view
                                            Intent intentToNotifyUpdateUI =
                                                    new Intent(ACTION_TRIGGER_BROADCAST);
                                            LocalBroadcastManager.getInstance(context)
                                                    .sendBroadcast(intentToNotifyUpdateUI);
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            LogUtil.logError(TAG, "remove() failed, e=" + e);
                                        }
                                    });

                        }
                        final String publicKey = backupTargetEntity.getPublicKey();
                        sendDeleteMessage(
                                context,
                                receiverFcmToken,
                                receiverWhisperPub,
                                receiverPushyToken,
                                publicKey,
                                backupVersion);
                    }
                });
    }

    private void sendDeleteMessage(
            @NonNull Context context,
            @Nullable String receiverToken,
            @Nullable String receiverWhisperPub,
            @Nullable String receiverPushyToken,
            @NonNull String targetPublicKey,
            @Nullable String deleteBackupVersion) {
        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(targetPublicKey)) {
            LogUtil.logError(TAG, "targetPublicKey is null or empty");
            return;
        }

        final String myEmailHash = PhoneUtil.getSKREmailHash(context);
        final String myUUID = PhoneUtil.getSKRID(context);
        VerificationUtil verificationUtil = new VerificationUtil(false);
        final String encUUID = verificationUtil.encryptMessage(myUUID, targetPublicKey);
        if (TextUtils.isEmpty(encUUID)) {
            LogUtil.logError(TAG, "encUUID is null or empty");
            return;
        }

        final Map<String, String> map = new ArrayMap<>();
        map.put(KEY_EMAIL_HASH, myEmailHash);
        map.put(KEY_ENCRYPTED_UUID, encUUID);
        if (!TextUtils.isEmpty(deleteBackupVersion)) {
            map.put(KEY_BACKUP_VERSION, deleteBackupVersion);
            LogUtil.logInfo(TAG, "Send delete message with version");
        }
        sendMessage(context, receiverToken, receiverWhisperPub, receiverPushyToken, map);
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

        final String emailHash = messages.get(KEY_EMAIL_HASH);
        if (TextUtils.isEmpty(emailHash)) {
            LogUtil.logError(
                    TAG,
                    "emailHash is null or empty",
                    new IllegalStateException("emailHash is null or empty"));
            return;
        }
        VerificationUtil verificationUtil = new VerificationUtil(true);
        String encUuid = messages.get(KEY_ENCRYPTED_UUID);
        String uuid;
        if (TextUtils.isEmpty(encUuid)) {
            LogUtil.logError(
                    TAG,
                    "encUuid is null or empty",
                    new IllegalStateException("encUuid is null or empty"));
            return;
        } else {
            uuid = verificationUtil.decryptMessage(encUuid);
            if (TextUtils.isEmpty(uuid) || encUuid.equals(uuid)) {
                LogUtil.logError(
                        TAG,
                        "UUID is empty or failed to decrypt",
                        new IllegalStateException("UUID is empty or failed to decrypt"));
                return;
            }
        }
        String uuidHash = ChecksumUtil.generateChecksum(uuid);
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(
                    TAG,
                    "UUIDHash is null or empty",
                    new IllegalStateException("UUIDHash is null or empty"));
            return;
        }

        LogUtil.logDebug(
                TAG,
                "Receive message, delete backupSource, emailHash = "
                        + emailHash
                        + ", uuidHash = "
                        + uuidHash);

        // With version, Only remove less than or equal to version's backupSource
        if (messages.containsKey(KEY_BACKUP_VERSION)) {
            String backupVersionStr = messages.get(KEY_BACKUP_VERSION);
            LogUtil.logDebug(TAG, "with version " + backupVersionStr);
            try {
                int backupVersion = Integer.parseInt(backupVersionStr);
                BackupSourceUtil.removeAllByEmailHashWithVersion(context, emailHash, backupVersion);
            } catch (NumberFormatException e) {
                LogUtil.logError(TAG, "Incorrect version, e = " + e);
            }
        } else {
            // Change delete message's behavior, 2019-02-27
            // Remove all have same emailHash
            BackupSourceUtil.removeAllByEmailHash(context, emailHash);
        }

        // Notify VerificationRequestActivity to update UI
        Intent intent = new Intent(ACTION_RECEIVE_DELETE);
        intent.putExtra(KEY_UUID_HASH, uuidHash);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
