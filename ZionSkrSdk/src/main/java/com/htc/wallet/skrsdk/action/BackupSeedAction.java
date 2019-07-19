package com.htc.wallet.skrsdk.action;

import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_DONE;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_OK;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_REQUEST_AUTO_BACKUP;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_OK_SENT;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.VERIFY_ACTION_TIMEOUT;

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

public class BackupSeedAction extends Action {
    private static final String TAG = "BackupSeedAction";

    @Override
    int getMessageTypeId() {
        return MessageConstants.TYPE_BACKUP_SEED;
    }

    // Amy
    @Override
    public void sendInternal(
            @NonNull final Context context,
            @Nullable final String receiverFcmToken,
            @Nullable final String receiverWhisperPub,
            @Nullable final String receiverPushyToken,
            @NonNull final Map<String, String> messages) {

        // From Map
        final String publicKey = messages.get(KEY_PUBLIC_KEY);
        if (TextUtils.isEmpty(publicKey)) {
            LogUtil.logError(
                    TAG,
                    "publicKey is null or empty",
                    new IllegalStateException("publicKey is null ot empty"));
            return;
        }
        final String encSeed = messages.get(KEY_ENCRYPTED_SEED);
        if (TextUtils.isEmpty(encSeed)) {
            LogUtil.logError(
                    TAG,
                    "encSeed is null or empty",
                    new IllegalStateException("encSeed is null ot empty"));
            return;
        }
        final String checkSum = messages.get(KEY_CHECKSUM);
        if (TextUtils.isEmpty(checkSum)) {
            LogUtil.logError(
                    TAG,
                    "checkSum is null or empty",
                    new IllegalStateException("checkSum is null ot empty"));
            return;
        }

        final String myEmailHash = PhoneUtil.getSKREmailHash(context);
        if (TextUtils.isEmpty(myEmailHash)) {
            LogUtil.logError(
                    TAG,
                    "myEmailHash is null or empty",
                    new IllegalStateException("myEmailHash is null ot empty"));
            return;
        }
        final String myUUID = PhoneUtil.getSKRID(context);
        if (TextUtils.isEmpty(myUUID)) {
            LogUtil.logError(
                    TAG,
                    "myUUID is null or empty",
                    new IllegalStateException("myUUID is null ot empty"));
            return;
        }
        VerificationUtil verificationUtil = new VerificationUtil(false);
        LogUtil.logDebug(TAG, "publicKey=" + publicKey);
        final String encUUID = verificationUtil.encryptMessage(myUUID, publicKey);
        if (TextUtils.isEmpty(encUUID)) {
            LogUtil.logError(
                    TAG,
                    "encUUID is null or empty",
                    new IllegalStateException("encUUID is null ot empty"));
            return;
        }

        Map<String, String> map = new ArrayMap<>();
        map.put(KEY_EMAIL_HASH, myEmailHash);
        map.put(KEY_ENCRYPTED_UUID, encUUID);
        map.put(KEY_ENCRYPTED_SEED, encSeed);
        map.put(KEY_CHECKSUM, checkSum);
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
        final String encSeed = messages.get(KEY_ENCRYPTED_SEED);
        if (TextUtils.isEmpty(encSeed)) {
            LogUtil.logError(
                    TAG,
                    "encSeed is null or empty",
                    new IllegalStateException("encSeed is null or empty"));
            return;
        }
        final String checksum = messages.get(KEY_CHECKSUM);
        if (TextUtils.isEmpty(checksum)) {
            LogUtil.logError(
                    TAG,
                    "checksum is null or empty",
                    new IllegalStateException("checksum is null or empty"));
            return;
        }

        BackupSourceUtil.getWithUUIDHash(
                context,
                emailHash,
                uuidHash,
                new LoadDataListener() {
                    @Override
                    public void onLoadFinished(
                            BackupSourceEntity originBackupSource,
                            BackupTargetEntity backupTargetEntity,
                            RestoreSourceEntity restoreSourceEntity,
                            RestoreTargetEntity restoreTargetEntity) {
                        if (originBackupSource == null) {
                            LogUtil.logInfo(TAG, "Receive seed without backupSource, ignore it");
                            return;
                        }

                        LogUtil.logDebug(
                                TAG,
                                "Receive message, partial seed from "
                                        + originBackupSource.getName());

                        if (originBackupSource.compareStatus(BACKUP_SOURCE_STATUS_REQUEST)
                                && (System.currentTimeMillis()
                                > originBackupSource.getLastVerifyTime()
                                + VERIFY_ACTION_TIMEOUT)) {
                            LogUtil.logWarning(TAG, "verification time out");
                            return;
                        }

                        if (!originBackupSource.getUUIDHash().equals(uuidHash)) {
                            LogUtil.logError(
                                    TAG,
                                    "uuidHash doesn't match",
                                    new IllegalStateException("uuidHash doesn't match"));
                            return;
                        }

                        String name = originBackupSource.getName();
                        if (TextUtils.isEmpty(name)) {
                            LogUtil.logError(
                                    TAG,
                                    "name is null or empty",
                                    new IllegalStateException("name is null or empty"));
                            return;
                        }

                        String myName = originBackupSource.getMyName();
                        if (TextUtils.isEmpty(myName)) {
                            LogUtil.logError(
                                    TAG,
                                    "myName is null or empty",
                                    new IllegalStateException("myName is null or empty"));
                            return;
                        }

                        final String publicKey = originBackupSource.getPublicKey();
                        if (TextUtils.isEmpty(publicKey)) {
                            LogUtil.logError(
                                    TAG,
                                    "publicKey is null or empty",
                                    new IllegalStateException("publicKey is null or empty"));
                            return;
                        }

                        String whisperPub = senderWhisperPub;
                        String pushyToken = senderPushyToken;
                        // To ensure that BackupHealthReportAction can try whisper first.
                        if (TextUtils.isEmpty(whisperPub) || TextUtils.isEmpty(pushyToken)) {
                            whisperPub = originBackupSource.getWhisperPub();
                            pushyToken = originBackupSource.getPushyToken();
                        }

                        final BackupSourceEntity backupSource;
                        if (ChecksumUtil.verifyMessageWithChecksum(encSeed, checksum)) {
                            int oldStatus = originBackupSource.getStatus();
                            int newStatus;
                            switch (oldStatus) {
                                case BACKUP_SOURCE_STATUS_REQUEST:
                                    newStatus = BACKUP_SOURCE_STATUS_OK;
                                    break;
                                case BACKUP_SOURCE_STATUS_REQUEST_AUTO_BACKUP:
                                    newStatus = BACKUP_SOURCE_STATUS_DONE;
                                    break;
                                default:
                                    newStatus = oldStatus;
                            }
                            LogUtil.logInfo(
                                    TAG,
                                    "Checksum verified, status from "
                                            + oldStatus
                                            + " to "
                                            + newStatus);
                            backupSource = new BackupSourceEntity();
                            backupSource.setStatus(newStatus);
                            backupSource.setEmailHash(emailHash);
                            backupSource.setFcmToken(senderFcmToken);
                            backupSource.setWhisperPub(whisperPub);
                            backupSource.setPushyToken(pushyToken);
                            backupSource.setName(originBackupSource.getName());
                            backupSource.setMyName(myName);
                            backupSource.setUUIDHash(uuidHash);
                            backupSource.setIsTest(originBackupSource.getIsTest());
                            backupSource.setTimeStamp(System.currentTimeMillis());
                            backupSource.setPublicKey(publicKey);
                            backupSource.setSeed(encSeed);
                            backupSource.setCheckSum(checksum);

                            final String finalWhisperPub = whisperPub;
                            final String finalPushyToken = pushyToken;
                            BackupSourceUtil.update(
                                    context,
                                    backupSource,
                                    new DatabaseCompleteListener() {
                                        @Override
                                        public void onComplete() {
                                            // Send BackupOkAction to notify Amy change status
                                            final Map<String, String> messageToSend =
                                                    new ArrayMap<>();
                                            messageToSend.put(KEY_OK, MSG_OK);
                                            messageToSend.put(KEY_CHECKSUM, checksum);
                                            messageToSend.put(KEY_PUBLIC_KEY, publicKey);
                                            new BackupOkAction()
                                                    .sendInternal(
                                                            context,
                                                            senderFcmToken,
                                                            finalWhisperPub,
                                                            finalPushyToken,
                                                            messageToSend);

                                            // Remove all legacy backupSource by emailHash, except
                                            // this uuidHash
                                            BackupSourceUtil.removeAllByEmailHashExceptUUIDHash(
                                                    context, emailHash, uuidHash);
                                        }

                                        @Override
                                        public void onError(Exception exception) {
                                            LogUtil.logError(TAG, "update error, e= " + exception);
                                        }
                                    });
                        } else {
                            LogUtil.logError(TAG, "checksum error");

                            backupSource = new BackupSourceEntity();
                            backupSource.setStatus(BACKUP_SOURCE_STATUS_OK);
                            backupSource.setEmailHash(emailHash);
                            backupSource.setFcmToken(senderFcmToken);
                            backupSource.setWhisperPub(whisperPub);
                            backupSource.setPushyToken(pushyToken);
                            backupSource.setName(originBackupSource.getName());
                            backupSource.setMyName(myName);
                            backupSource.setUUIDHash(uuidHash);
                            backupSource.setIsTest(originBackupSource.getIsTest());
                            backupSource.setTimeStamp(System.currentTimeMillis());
                            backupSource.setPublicKey(publicKey);
                            backupSource.setSeed(encSeed);
                            backupSource.setCheckSum(checksum);

                            final String finalWhisperPub = whisperPub;
                            final String finalPushyToken = pushyToken;
                            BackupSourceUtil.update(
                                    context,
                                    backupSource,
                                    new DatabaseCompleteListener() {
                                        @Override
                                        public void onComplete() {
                                            final Map<String, String> messageToSend =
                                                    new ArrayMap<>();
                                            messageToSend.put(KEY_OK, MSG_FAIL);
                                            messageToSend.put(KEY_CHECKSUM, checksum);
                                            messageToSend.put(KEY_PUBLIC_KEY, publicKey);
                                            new BackupOkAction()
                                                    .sendInternal(
                                                            context,
                                                            senderFcmToken,
                                                            finalWhisperPub,
                                                            finalPushyToken,
                                                            messageToSend);
                                        }

                                        @Override
                                        public void onError(Exception exception) {
                                            LogUtil.logError(TAG, "update error, e= " + exception);
                                        }
                                    });
                        }

                        // Send LocalBroadcast to show VerificationOkCongratulationActivity
                        Intent intent = new Intent(ACTION_OK_SENT);
                        intent.putExtra(KEY_EMAIL_HASH, emailHash);
                        intent.putExtra(KEY_UUID_HASH, uuidHash);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    }
                });
    }
}
