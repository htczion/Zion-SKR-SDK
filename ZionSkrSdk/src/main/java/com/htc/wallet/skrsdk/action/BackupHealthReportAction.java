package com.htc.wallet.skrsdk.action;

import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_OK;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_TRIGGER_BROADCAST;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.htc.wallet.skrsdk.backup.SocialBackupIntroductionActivity;
import com.htc.wallet.skrsdk.crypto.ChecksumUtil;
import com.htc.wallet.skrsdk.crypto.util.VerificationUtil;
import com.htc.wallet.skrsdk.legacy.v1.LegacyBackupDataV1;
import com.htc.wallet.skrsdk.legacy.v1.LegacyV1Util;
import com.htc.wallet.skrsdk.messaging.MessageConstants;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupDataConstants;
import com.htc.wallet.skrsdk.sqlite.util.BackupSourceUtil;
import com.htc.wallet.skrsdk.sqlite.util.BackupTargetUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.NotificationUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;
import com.htc.wallet.skrsdk.util.WalletSdkUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackupHealthReportAction extends Action {
    private static final String TAG = "BackupHealthReportAction";

    private static final String MSG_RESEND_NAME = "msg_resend_empty";
    private static final int VERSION_DEFAULT = -1;

    private volatile SendCompleteListener mSendCompleteListener;

    private static int parseInt(String num) {
        try {
            return Integer.parseInt(num);
        } catch (NumberFormatException var1) {
            return VERSION_DEFAULT;
        }
    }

    @Override
    int getMessageTypeId() {
        return MessageConstants.TYPE_REPORT_BACKUP_STATUS;
    }

    // Bob
    @Override
    public void sendInternal(
            @NonNull final Context context,
            @Nullable final String receiverFcmToken,
            @Nullable final String receiverWhisperPub,
            @Nullable final String receiverPushyToken,
            @NonNull final Map<String, String> messages) {

        final String myDeviceId = PhoneUtil.getDeviceId(context);
        if (TextUtils.isEmpty(myDeviceId)) {
            LogUtil.logError(
                    TAG,
                    "myDeviceId is null or empty",
                    new IllegalStateException("myDeviceId is null or empty"));
            return;
        }

        final VerificationUtil verificationUtil = new VerificationUtil(true);
        String publicKey = messages.get(KEY_PUBLIC_KEY);
        String checksum = messages.get(KEY_CHECKSUM);
        if (TextUtils.isEmpty(checksum) && TextUtils.isEmpty(publicKey)) {
            // Take initiative in reporting backup status to Amy
            BackupSourceUtil.getAllOK(
                    context,
                    new LoadListListener() {
                        @Override
                        public void onLoadFinished(
                                List<BackupSourceEntity> backupSourceEntityList,
                                List<BackupTargetEntity> backupTargetEntityList,
                                List<RestoreSourceEntity> restoreSourceEntityList,
                                List<RestoreTargetEntity> restoreTargetEntityList) {
                            if (backupSourceEntityList == null) {
                                LogUtil.logError(TAG, "backupSources is null");
                                return;
                            }
                            for (BackupSourceEntity backupSource : backupSourceEntityList) {
                                final String encDeviceId =
                                        verificationUtil.encryptMessage(
                                                myDeviceId, backupSource.getPublicKey());
                                if (TextUtils.isEmpty(encDeviceId)) {
                                    LogUtil.logError(TAG, "encDeviceId is null or empty");
                                    continue;
                                }
                                Map<String, String> map =
                                        verifyChecksumAndReportStatus(
                                                backupSource.getSeed(),
                                                backupSource.getCheckSum(),
                                                encDeviceId,
                                                backupSource.getMyName());
                                if (map == null) {
                                    LogUtil.logError(TAG, "map is null");
                                    continue;
                                }
                                sendCheckStatus(
                                        context,
                                        verificationUtil,
                                        backupSource.getPublicKey(),
                                        backupSource.getFcmToken(),
                                        backupSource.getWhisperPub(),
                                        backupSource.getPushyToken(),
                                        map);
                            }
                            notifySendComplete();
                        }
                    });
        } else if (!TextUtils.isEmpty(checksum) && !TextUtils.isEmpty(publicKey)) {
            final String encDeviceId = verificationUtil.encryptMessage(myDeviceId, publicKey);
            if (TextUtils.isEmpty(encDeviceId)) {
                LogUtil.logError(
                        TAG,
                        "encDeviceId is null or empty",
                        new IllegalStateException("encDeviceId is null or empty"));
                return;
            }
            final String encSeed = messages.get(KEY_ENCRYPTED_SEED);
            if (TextUtils.isEmpty(encSeed)) {
                LogUtil.logError(
                        TAG,
                        "encSeed is null or empty",
                        new IllegalStateException("encSeed is null or empty"));
                return;
            }
            final String myName = messages.get(KEY_NAME);
            Map<String, String> map =
                    verifyChecksumAndReportStatus(encSeed, checksum, encDeviceId, myName);
            // TODO: Move this to stand alone new action
            // Only for check delete if needed
            String backupVersion = messages.get(KEY_BACKUP_VERSION);
            if (parseInt(backupVersion) != VERSION_DEFAULT && map != null) {
                LogUtil.logInfo(TAG, "with backupVersion = " + backupVersion);
                map.put(KEY_BACKUP_VERSION, backupVersion);
            }
            if (map == null) {
                LogUtil.logError(TAG, "map is null", new IllegalStateException("map is null"));
                return;
            }
            sendCheckStatus(
                    context,
                    verificationUtil,
                    publicKey,
                    receiverFcmToken,
                    receiverWhisperPub,
                    receiverPushyToken,
                    map);
            notifySendComplete();
        } else {
            LogUtil.logError(TAG, "Something wrong on getting checksum or publicKey");
            notifySendComplete();
        }
    }

    // Amy
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

        boolean isSeedExists = WalletSdkUtil.isSeedExists(context);
        if (!isSeedExists) {
            LogUtil.logWarning(TAG, "Receive health report without wallet, ignore it");
            return;
        }

        final VerificationUtil verificationUtil = new VerificationUtil(false);

        final String encUUID = messages.get(KEY_ENCRYPTED_UUID);
        if (TextUtils.isEmpty(encUUID)) {
            LogUtil.logError(
                    TAG,
                    "encUUID is null or empty",
                    new IllegalStateException("encUUID is null or empty"));
            return;
        }

        final String uuid = verificationUtil.decryptMessage(encUUID);
        if (TextUtils.isEmpty(uuid)) {
            LogUtil.logError(
                    TAG,
                    "uuid is null or empty",
                    new IllegalStateException("uuid is null or empty"));
            return;
        }

        final String uuidHash = ChecksumUtil.generateChecksum(uuid);
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(
                    TAG,
                    "uuidHash is null or empty",
                    new IllegalStateException("uuidHash is null or empty"));
            return;
        }

        final AtomicBoolean shouldResendName = new AtomicBoolean(false);
        if (messages.containsKey(KEY_NAME) && MSG_RESEND_NAME.equals(messages.get(KEY_NAME))) {
            shouldResendName.set(true);
        }

        final int backupVersion = parseInt(messages.get(KEY_BACKUP_VERSION));

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
                            LogUtil.logDebug(
                                    TAG, "Receive a health report without previous status");

                            String encSenderPub = messages.get(KEY_ENCRYPTED_PUBLIC_KEY);
                            // Bob may loss the delete message, so Amy need to send another delete
                            // message to delete Bob's backup.
                            if (!TextUtils.isEmpty(encSenderPub)) {
                                String senderPub = verificationUtil.decryptMessage(encSenderPub);
                                if (TextUtils.isEmpty(senderPub)) {
                                    LogUtil.logError(
                                            TAG,
                                            "senderPub is empty",
                                            new IllegalStateException("senderPub is empty"));
                                    return;
                                }
                                LogUtil.logInfo(TAG, "Sending delete message...");
                                sendDeleteToClearData(
                                        context,
                                        senderFcmToken,
                                        senderWhisperPub,
                                        senderPushyToken,
                                        uuidHash,
                                        senderPub);
                            }

                            if (backupVersion != VERSION_DEFAULT) {
                                sendDeleteToClearLegacyV1IfNeeded(
                                        context,
                                        senderFcmToken,
                                        senderWhisperPub,
                                        senderPushyToken,
                                        uuidHash,
                                        backupVersion,
                                        null);
                            }
                            return;
                        }
                        // BACKUP_TARGET_STATUS_REQUEST_WAIT_OK has seedIndex
                        if (backupTargetEntity.compareStatus(BACKUP_TARGET_STATUS_REQUEST)
                                && !backupTargetEntity.isSeeded()) {
                            LogUtil.logWarning(
                                    TAG, "Receive a health report with unseeded request");
                            return;
                        }

                        final String name = backupTargetEntity.getName();
                        LogUtil.logDebug(TAG, "Receive message, health report from " + name);

                        String whisperPub = senderWhisperPub;
                        String pushyToken = senderPushyToken;
                        // To ensure that BackupHealthReportAction can try whisper first.
                        if (TextUtils.isEmpty(whisperPub) || TextUtils.isEmpty(pushyToken)) {
                            whisperPub = backupTargetEntity.getWhisperPub();
                            pushyToken = backupTargetEntity.getPushyToken();
                        }

                        // To ensure that Amy can save Bob's whisper pub and pushy token if she
                        // doesn't save yet.
                        if (!TextUtils.isEmpty(whisperPub) && !TextUtils.isEmpty(pushyToken)) {
                            backupTargetEntity.setWhisperPub(whisperPub);
                            backupTargetEntity.setPushyToken(pushyToken);
                        }

                        if (shouldResendName.get()) {
                            Map<String, String> map = new ArrayMap<>();
                            map.put(KEY_PUBLIC_KEY, backupTargetEntity.getPublicKey());
                            map.put(KEY_NAME, name);
                            new ResendNameAction()
                                    .sendInternal(
                                            context, senderFcmToken, whisperPub, pushyToken, map);
                        }

                        if (backupVersion != VERSION_DEFAULT) {
                            sendDeleteToClearLegacyV1IfNeeded(
                                    context,
                                    senderFcmToken,
                                    whisperPub,
                                    pushyToken,
                                    uuidHash,
                                    backupVersion,
                                    backupTargetEntity);
                        }

                        final Intent intentToNotifyUpdateUI = new Intent(ACTION_TRIGGER_BROADCAST);
                        final String reportString = messages.get(KEY_REPORT_BACKUP_HEALTH);
                        switch (reportString) {
                            case MSG_REPORT_BACKUP_HEALTH_OK:
                                final String checkSum = messages.get(KEY_CHECKSUM);
                                if (TextUtils.isEmpty(checkSum)) {
                                    LogUtil.logError(
                                            TAG,
                                            "checkSum is null or empty",
                                            new IllegalStateException("checkSum is null or empty"));
                                    return;
                                }
                                String backupTargetChecksum = backupTargetEntity.getCheckSum();
                                if (TextUtils.isEmpty(backupTargetChecksum)) {
                                    LogUtil.logWarning(
                                            TAG,
                                            "backupTarget checksum is null or empty, maybe is bad"
                                                    + " ");
                                    return;
                                }

                                if (checkSum.equals(backupTargetChecksum)) {
                                    LogUtil.logDebug(
                                            TAG, backupTargetEntity.getName() + " pass the check");
                                    // Issue Fix, 2019/01/17, Fixed no response status can't change
                                    // back to ok status,
                                    // after receive BackupHealthReport's
                                    // MSG_REPORT_BACKUP_HEALTH_OK

                                    // CheckSum match, set backupTarget to OK status
                                    backupTargetEntity.setStatus(BACKUP_TARGET_STATUS_OK);
                                    backupTargetEntity.updateLastCheckedTime();
                                    BackupTargetUtil.update(
                                            context,
                                            backupTargetEntity,
                                            new DatabaseCompleteListener() {
                                                @Override
                                                public void onComplete() {
                                                    LocalBroadcastManager.getInstance(context)
                                                            .sendBroadcast(intentToNotifyUpdateUI);
                                                }

                                                @Override
                                                public void onError(Exception exception) {
                                                    LogUtil.logError(
                                                            TAG, "update error, e= " + exception);
                                                }
                                            });
                                } else {
                                    // Update backupTarget to Bad status if checksum doesn't match
                                    backupTargetEntity.updateLastCheckedTime();
                                    backupTargetEntity.updateStatusToBad();
                                    BackupTargetUtil.update(
                                            context,
                                            backupTargetEntity,
                                            new DatabaseCompleteListener() {
                                                @Override
                                                public void onComplete() {
                                                    LocalBroadcastManager.getInstance(context)
                                                            .sendBroadcast(intentToNotifyUpdateUI);
                                                }

                                                @Override
                                                public void onError(Exception exception) {
                                                    LogUtil.logError(
                                                            TAG, "update error, e= " + exception);
                                                }
                                            });
                                }
                                break;
                            case MSG_REPORT_BACKUP_HEALTH_BAD:
                                LogUtil.logDebug(
                                        TAG,
                                        backupTargetEntity.getName() + " didn't pass the check");

                                Intent intent = new Intent(context,
                                        SocialBackupIntroductionActivity.class);
                                NotificationUtil.showBackupHealthProblemNotification(context,
                                        intent);

                                backupTargetEntity.updateLastCheckedTime();
                                backupTargetEntity.updateStatusToBad();
                                BackupTargetUtil.update(
                                        context,
                                        backupTargetEntity,
                                        new DatabaseCompleteListener() {
                                            @Override
                                            public void onComplete() {
                                                LocalBroadcastManager.getInstance(context)
                                                        .sendBroadcast(intentToNotifyUpdateUI);
                                            }

                                            @Override
                                            public void onError(Exception exception) {
                                                LogUtil.logError(
                                                        TAG, "update error, e= " + exception);
                                            }
                                        });

                                break;
                            default:
                                LogUtil.logError(
                                        TAG,
                                        "No matching report status",
                                        new IllegalStateException("No matching report status"));
                                break;
                        }
                        // Notify Bob that Amy has received his report, also means that she still
                        // has the backupTarget of Bob.
                        new BackupReportExistedAction()
                                .sendInternal(
                                        context,
                                        senderFcmToken,
                                        senderWhisperPub,
                                        senderPushyToken,
                                        new HashMap<String, String>());
                    }
                });
    }

    private void sendDeleteToClearData(
            @NonNull Context context,
            @Nullable String senderFcmToken,
            @Nullable String senderWhisperPub,
            @Nullable String senderPushyToken,
            @NonNull String uuidHash,
            @NonNull String senderPub) {
        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(
                    TAG, "uuidHash is empty", new IllegalArgumentException("uuidHash is empty"));
            return;
        }
        if (TextUtils.isEmpty(senderPub)) {
            LogUtil.logError(
                    TAG, "senderPub is empty", new IllegalArgumentException("senderPub is empty"));
            return;
        }

        Map<String, String> map = new ArrayMap<>();
        map.put(KEY_UUID_HASH, uuidHash);
        map.put(KEY_PUBLIC_KEY, senderPub);
        new BackupDeleteAction()
                .sendInternal(context, senderFcmToken, senderWhisperPub, senderPushyToken, map);
    }

    private void sendDeleteToClearLegacyV1IfNeeded(
            @NonNull Context context,
            @Nullable String receiverToken,
            @Nullable String receiverWhisperPub,
            @Nullable String receiverPushyToken,
            @NonNull String uuidHash,
            final int backupVersion,
            @Nullable final BackupTargetEntity backupTargetEntity) {
        LogUtil.logDebug(TAG, "with backupVersion = " + backupVersion);
        Map<String, LegacyBackupDataV1> legacySkrV1Map = LegacyV1Util.getLegacyV1Map(context);
        if (backupVersion >= BackupDataConstants.BACKUP_DATA_CLEAR_LEGACY_V1_VERSION) {
            LogUtil.logDebug(TAG, "version " + backupVersion + " is not legacy V1");
            return;
        }
        if (backupTargetEntity != null
                && !backupTargetEntity.compareStatus(BackupTargetEntity.BACKUP_TARGET_STATUS_BAD)) {
            LogUtil.logDebug(TAG, "backupTargetEntity is not in Bad Status");
            return;
        }
        if (!SkrSharedPrefs.getUserAgreeDeleteLegacySkrBackupDataV1(context)) {
            LogUtil.logDebug(TAG, "User hasn't agreed");
            return;
        }
        if (!legacySkrV1Map.containsKey(uuidHash)) {
            LogUtil.logDebug(TAG, "Not in legacy V1 List");
            return;
        }

        // Send delete message back
        LogUtil.logDebug(
                TAG,
                "User agree and there is Legacy Backup Data V1 in Bob's device, Send delete "
                        + "message to "
                        + uuidHash);
        Map<String, String> map = new ArrayMap<>();
        map.put(KEY_UUID_HASH, uuidHash);
        map.put(KEY_IS_RESEND, MSG_IS_RESEND);
        // Put publicKey prevent backupTarget has been delete
        LegacyBackupDataV1 legacyBackupDataV1 = legacySkrV1Map.get(uuidHash);
        if (legacyBackupDataV1 != null && !TextUtils.isEmpty(legacyBackupDataV1.getPublicKey())) {
            map.put(KEY_PUBLIC_KEY, legacyBackupDataV1.getPublicKey());
        }
        // Let Bob delete when version less than or equal to BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION
        // (legacy V1, 1)
        map.put(
                Action.KEY_BACKUP_VERSION,
                String.valueOf(BackupDataConstants.BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION));
        new BackupDeleteAction()
                .sendInternal(context, receiverToken, receiverWhisperPub, receiverPushyToken, map);
    }

    @Nullable
    private Map<String, String> verifyChecksumAndReportStatus(
            String encSeed, String checksum, String encDeviceId, String myName) {
        if (TextUtils.isEmpty(encSeed)) {
            LogUtil.logError(
                    TAG,
                    "encSeed is null or empty",
                    new IllegalArgumentException("encSeed is null or empty"));
            return null;
        }
        if (TextUtils.isEmpty(checksum)) {
            LogUtil.logError(
                    TAG,
                    "checksum is null or empty",
                    new IllegalArgumentException("checksum is null or empty"));
            return null;
        }
        if (TextUtils.isEmpty(encDeviceId)) {
            LogUtil.logError(
                    TAG,
                    "encDeviceId is null or empty",
                    new IllegalArgumentException("encDeviceId is null or empty"));
            return null;
        }
        Map<String, String> messages = new ArrayMap<>();
        messages.put(KEY_ENCRYPTED_UUID, encDeviceId);
        if (TextUtils.isEmpty(myName)) {
            LogUtil.logInfo(TAG, "without myName, ask for help");
            messages.put(KEY_NAME, MSG_RESEND_NAME);
        }
        if (ChecksumUtil.verifyMessageWithChecksum(encSeed, checksum)) {
            messages.put(KEY_CHECKSUM, checksum);
            messages.put(KEY_REPORT_BACKUP_HEALTH, MSG_REPORT_BACKUP_HEALTH_OK);
        } else {
            messages.put(KEY_REPORT_BACKUP_HEALTH, MSG_REPORT_BACKUP_HEALTH_BAD);
        }
        return messages;
    }

    private void sendCheckStatus(
            @NonNull Context context,
            @NonNull VerificationUtil verificationUtil,
            String backupSourcePubKey,
            String receiverToken,
            String receiverWhisperPub,
            String receiverPushyToken,
            Map<String, String> map) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(verificationUtil, "verificationUtil is null");
        if (map.isEmpty()) {
            LogUtil.logWarning(TAG, "map is null or empty");
            return;
        }

        if (TextUtils.isEmpty(backupSourcePubKey)) {
            LogUtil.logError(
                    TAG,
                    "backupSourcePubKey is empty",
                    new IllegalArgumentException("backupSourcePubKey is empty"));
            return;
        }

        final String pubKey = verificationUtil.getPublicKeyString();
        if (TextUtils.isEmpty(pubKey)) {
            LogUtil.logError(TAG, "pubKey is empty", new IllegalStateException("pubKey is empty"));
            return;
        }

        final String encPubKey = verificationUtil.encryptMessage(pubKey, backupSourcePubKey);
        if (TextUtils.isEmpty(pubKey)) {
            LogUtil.logError(
                    TAG, "encPubKey is empty", new IllegalStateException("encPubKey is empty"));
            return;
        }

        map.put(KEY_ENCRYPTED_PUBLIC_KEY, encPubKey);

        sendMessage(context, receiverToken, receiverWhisperPub, receiverPushyToken, map);
    }

    private void notifySendComplete() {
        if (mSendCompleteListener != null) {
            mSendCompleteListener.onSendComplete();
        }
    }

    public void setSendCompleteListener(SendCompleteListener sendCompleteListener) {
        LogUtil.logDebug(TAG, "set SendCompleteListener");

        if (sendCompleteListener == null) {
            LogUtil.logWarning(TAG, "sendCompleteListener is null");
        }
        mSendCompleteListener = sendCompleteListener;
    }

    // Check if all actions need this callback ?
    public interface SendCompleteListener {
        void onSendComplete();
    }
}
