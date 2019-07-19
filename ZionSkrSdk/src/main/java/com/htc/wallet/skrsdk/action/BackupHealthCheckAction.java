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
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupSourceUtil;
import com.htc.wallet.skrsdk.sqlite.util.BackupTargetUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;

import java.util.List;
import java.util.Map;

public class BackupHealthCheckAction extends Action {
    private static final String TAG = "BackupHealthCheckAction";

    @Override
    int getMessageTypeId() {
        return MessageConstants.TYPE_CHECK_BACKUP_STATUS;
    }

    // Amy
    @Override
    public void sendInternal(
            @NonNull final Context context,
            @Nullable final String receiverFcmToken,
            @Nullable final String receiverWhisperPub,
            @Nullable final String receiverPushyToken,
            @NonNull final Map<String, String> messages) {
        BackupTargetUtil.getAllOKAndNoResponse(
                context,
                new LoadListListener() {
                    @Override
                    public void onLoadFinished(
                            List<BackupSourceEntity> backupSourceEntityList,
                            List<BackupTargetEntity> backupTargetEntityList,
                            List<RestoreSourceEntity> restoreSourceEntityList,
                            List<RestoreTargetEntity> restoreTargetEntityList) {
                        if (backupTargetEntityList == null || backupTargetEntityList.isEmpty()) {
                            LogUtil.logWarning(TAG, "backupTargets is null");
                            return;
                        }
                        sendAll(context, backupTargetEntityList);
                    }
                });
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
        String backupCheck = messages.get(KEY_CHECK_BACKUP_HEALTH);

        if (!backupCheck.equals(MSG_CHECK_BACKUP_STATUS)) {
            LogUtil.logError(
                    TAG,
                    "backupCheck string doesn't match",
                    new IllegalStateException("backupCheck string doesn't match"));
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
                    "UUID hash is null or empty",
                    new IllegalStateException("UUID hash is null or empty"));
            return;
        }
        // TODO need to check if Bob has valid seed (checksum) report to Amy
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
                            // Not error, Bob may remove backupSource while auto backup timeout, Amy
                            // still in no response status
                            LogUtil.logWarning(TAG, "backupSource is null");
                            return;
                        }

                        String name = backupSourceEntity.getName();
                        LogUtil.logDebug(TAG, "Receive message, health check by " + name);

                        Map<String, String> msgToSend = new ArrayMap<>();
                        // Verify the checksum in BackupHealthReportAction().sendInternal() because
                        // Bob can also report backup status without
                        // Amy's ask
                        msgToSend.put(KEY_CHECKSUM, backupSourceEntity.getCheckSum());
                        msgToSend.put(KEY_ENCRYPTED_SEED, backupSourceEntity.getSeed());
                        msgToSend.put(KEY_PUBLIC_KEY, backupSourceEntity.getPublicKey());
                        msgToSend.put(KEY_NAME, backupSourceEntity.getMyName());

                        String whisperPub = senderWhisperPub;
                        String pushyToken = senderPushyToken;
                        // To ensure that BackupHealthReportAction can try whisper first.
                        if (TextUtils.isEmpty(whisperPub) || TextUtils.isEmpty(pushyToken)) {
                            whisperPub = backupSourceEntity.getWhisperPub();
                            pushyToken = backupSourceEntity.getPushyToken();
                        }

                        new BackupHealthReportAction()
                                .sendInternal(
                                        context, senderFcmToken, whisperPub, pushyToken, msgToSend);
                    }
                });
    }

    private void sendAll(
            @NonNull Context context, @NonNull List<BackupTargetEntity> backupTargets) {

        for (BackupTargetEntity target : backupTargets) {
            final String receiverWhisperPub = target.getWhisperPub();
            final String receiverPushyToken = target.getPushyToken();
            final String receiverFcmToken = target.getFcmToken();

            final String publicKey = target.getPublicKey();
            if (TextUtils.isEmpty(publicKey)) {
                LogUtil.logError(
                        TAG,
                        "publicKey is null or empty",
                        new IllegalStateException("publicKey is null ot empty"));
                continue;
            }
            final String myEmailHash = PhoneUtil.getSKREmailHash(context);
            if (TextUtils.isEmpty(myEmailHash)) {
                LogUtil.logError(
                        TAG,
                        "myEmailHash is null or empty",
                        new IllegalStateException("myEmailHash is null ot empty"));
                continue;
            }
            final String myUUID = PhoneUtil.getSKRID(context);
            if (TextUtils.isEmpty(myUUID)) {
                LogUtil.logError(
                        TAG,
                        "myUUID is null or empty",
                        new IllegalStateException("myUUID is null ot empty"));
                continue;
            }
            VerificationUtil verificationUtil = new VerificationUtil(false);
            final String encUUID = verificationUtil.encryptMessage(myUUID, publicKey);
            if (TextUtils.isEmpty(encUUID)) {
                LogUtil.logError(
                        TAG,
                        "encUUID is null or empty",
                        new IllegalStateException("encUUID is null ot empty"));
                continue;
            }

            Map<String, String> msgToSend = new ArrayMap<>();
            msgToSend.put(KEY_CHECK_BACKUP_HEALTH, MSG_CHECK_BACKUP_STATUS);
            msgToSend.put(KEY_EMAIL_HASH, myEmailHash);
            msgToSend.put(KEY_ENCRYPTED_UUID, encUUID);
            sendMessage(
                    context, receiverFcmToken, receiverWhisperPub, receiverPushyToken, msgToSend);
        }
    }
}
