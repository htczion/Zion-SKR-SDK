package com.htc.wallet.skrsdk.action;

import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_BAD;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_NO_RESPONSE;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_OK;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_REQUEST_WAIT_OK;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_TRIGGER_BROADCAST;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.htc.wallet.skrsdk.crypto.ChecksumUtil;
import com.htc.wallet.skrsdk.crypto.util.GenericCipherUtil;
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
import com.htc.wallet.skrsdk.util.IntentUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.NotificationUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;
import com.htc.wallet.skrsdk.util.WalletSdkUtil;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackupRequestAction extends Action {
    private static final String TAG = "BackupRequestAction";

    private static final int TIMEOUT = 5; // seconds

    @Override
    int getMessageTypeId() {
        return MessageConstants.TYPE_BACKUP_REQUEST;
    }

    // Bob
    @Override
    public void sendInternal(
            @NonNull final Context context,
            @Nullable final String receiverFcmToken,
            @Nullable final String receiverWhisperPub,
            @Nullable final String receiverPushyToken,
            @NonNull final Map<String, String> messages) {

        // Store Amy's info to BackupSource
        final String sourceEmailHash = messages.get(KEY_EMAIL_HASH);
        final String sourceName = messages.get(KEY_SOURCE_NAME);
        final String sourceUUIDHash = messages.get(KEY_UUID_HASH);
        final String sourcePublicKey = messages.get(KEY_PUBLIC_KEY);
        final String sourceTZIDHash = messages.get(KEY_TZID_HASH);
        final String strIsTest = messages.get(KEY_IS_TEST);

        if (TextUtils.isEmpty(sourceEmailHash)) {
            LogUtil.logError(
                    TAG,
                    "EmailHash is empty or null",
                    new IllegalStateException("EmailHash is empty or null"));
            return;
        }
        if (TextUtils.isEmpty(sourceName)) {
            LogUtil.logError(
                    TAG,
                    "Name is empty or null",
                    new IllegalStateException("Name is empty or null"));
            return;
        }
        if (TextUtils.isEmpty(sourceUUIDHash)) {
            LogUtil.logError(
                    TAG,
                    "UUIDHash is empty or null",
                    new IllegalStateException("UUIDHash is empty or null"));
            return;
        }
        if (TextUtils.isEmpty(sourcePublicKey)) {
            LogUtil.logError(
                    TAG,
                    "PublicKey is empty or null",
                    new IllegalStateException("PublicKey is empty or null"));
            return;
        }
        if (TextUtils.isEmpty(sourceTZIDHash)) {
            LogUtil.logError(
                    TAG,
                    "TZIDHash is empty or null",
                    new IllegalStateException("TZIDHash is empty or null"));
            return;
        }

        final boolean isTest = Boolean.parseBoolean(strIsTest);
        if (isTest) {
            LogUtil.logDebug(TAG, "isTest is true for testing");
        }

        // Send my info to Amy
        VerificationUtil verificationUtil = new VerificationUtil(true);
        final String myName = messages.get(KEY_NAME);
        if (TextUtils.isEmpty(myName)) {
            LogUtil.logError(
                    TAG,
                    "myName is null or empty",
                    new IllegalArgumentException("myName is null or empty"));
            return;
        }
        final String myPhoneNumber = messages.get(KEY_PHONE_NUMBER);
        final String myPhoneModel = PhoneUtil.getModel();
        final String targetName = messages.get(KEY_TARGET_NAME);
        final String targetUUIDHash = messages.get(KEY_TARGET_UUID_HASH);
        final String myPublicKey = verificationUtil.getPublicKeyString();
        if (TextUtils.isEmpty(myPublicKey)) {
            LogUtil.logError(
                    TAG,
                    "myPublicKey is null or empty",
                    new IllegalArgumentException("myPublicKey is null or empty"));
            return;
        }
        final String myDeviceId = PhoneUtil.getDeviceId(context);
        if (TextUtils.isEmpty(myDeviceId)) {
            LogUtil.logError(
                    TAG,
                    "myDeviceId is null or empty",
                    new IllegalArgumentException("myDeviceId is null or empty"));
            return;
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
                        if (backupSourceEntity == null) {
                            backupSourceEntity = new BackupSourceEntity();
                            backupSourceEntity.setStatus(BACKUP_SOURCE_STATUS_REQUEST);
                            backupSourceEntity.setEmailHash(sourceEmailHash);
                            backupSourceEntity.setFcmToken(receiverFcmToken);
                            backupSourceEntity.setWhisperPub(receiverWhisperPub);
                            backupSourceEntity.setPushyToken(receiverPushyToken);
                            backupSourceEntity.setName(sourceName);
                            backupSourceEntity.setUUIDHash(sourceUUIDHash);
                            backupSourceEntity.setTimeStamp(System.currentTimeMillis());
                            backupSourceEntity.setTzIdHash(sourceTZIDHash);
                            backupSourceEntity.setIsTest(isTest);
                            backupSourceEntity.setPublicKey(sourcePublicKey);
                            backupSourceEntity.setMyName(myName);
                        } else {
                            if (!backupSourceEntity.compareStatus(BACKUP_SOURCE_STATUS_REQUEST)) {
                                LogUtil.logError(TAG, "incorrect status");
                                return;
                            }
                        }
                        backupSourceEntity.setLastRequestTime(System.currentTimeMillis());
                        BackupSourceUtil.put(
                                context,
                                backupSourceEntity,
                                new DatabaseCompleteListener() {
                                    @Override
                                    public void onComplete() {
                                    }

                                    @Override
                                    public void onError(Exception exception) {
                                        LogUtil.logError(TAG, "put error, e= " + exception);
                                    }
                                });
                    }
                });

        String encMyPhoneNumber = null;
        if (!TextUtils.isEmpty(myPhoneNumber)) {
            encMyPhoneNumber = verificationUtil.encryptMessage(myPhoneNumber, sourcePublicKey);
            if (TextUtils.isEmpty(encMyPhoneNumber)) {
                LogUtil.logError(TAG, "encrypt myPhoneNumber fail");
            }
        }
        String encMyPublicKey = verificationUtil.encryptMessage(myPublicKey, sourcePublicKey);
        if (TextUtils.isEmpty(encMyPublicKey)) {
            LogUtil.logError(
                    TAG,
                    "encMyPublicKey is null or empty",
                    new IllegalArgumentException("encMyPublicKey is null or empty"));
            return;
        }
        String encMyDeviceId = verificationUtil.encryptMessage(myDeviceId, sourcePublicKey);
        if (TextUtils.isEmpty(encMyDeviceId)) {
            LogUtil.logError(
                    TAG,
                    "encMyDeviceId is null or empty",
                    new IllegalArgumentException("encMyDeviceId is null or empty"));
            return;
        }

        Map<String, String> mapToSend = new ArrayMap<>();
        mapToSend.put(KEY_NAME, myName);
        if (!TextUtils.isEmpty(encMyPhoneNumber)) {
            mapToSend.put(KEY_ENCRYPTED_PHONE_NUMBER, encMyPhoneNumber);
        }
        if (!TextUtils.isEmpty(myPhoneModel)) {
            mapToSend.put(KEY_PHONE_MODEL, myPhoneModel);
        }
        mapToSend.put(KEY_TARGET_NAME, targetName);
        mapToSend.put(KEY_TARGET_UUID_HASH, targetUUIDHash);
        mapToSend.put(KEY_ENCRYPTED_PUBLIC_KEY, encMyPublicKey);
        mapToSend.put(KEY_ENCRYPTED_UUID, encMyDeviceId);
        mapToSend.put(KEY_LINK_UUID_HASH, sourceUUIDHash);
        mapToSend.put(KEY_IS_RESEND, messages.get(KEY_IS_RESEND));
        sendMessage(context, receiverFcmToken, receiverWhisperPub, receiverPushyToken, mapToSend);
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
            LogUtil.logWarning(TAG, "Receive backup request without wallet, ignore it");
            return;
        }

        // Get name, phoneNumber and phoneModel from message
        if (messages == null) {
            LogUtil.logError(TAG, "messages is null");
            return;
        }

        // Decrypted Bob's UUID, Phone Number and Public Key
        VerificationUtil verificationUtil = new VerificationUtil(false);
        // UUID
        String uuid = messages.get(KEY_ENCRYPTED_UUID);
        if (TextUtils.isEmpty(uuid)) {
            LogUtil.logError(
                    TAG,
                    "Encrypted UUID is empty or null",
                    new IllegalStateException("Encrypted UUID is empty or null"));
            return;
        } else {
            uuid = verificationUtil.decryptMessage(uuid);
            if (TextUtils.isEmpty(uuid)) {
                LogUtil.logError(
                        TAG,
                        "UUID is empty or null",
                        new IllegalStateException("UUID is empty or null"));
                return;
            }
        }

        String linkUUIDHash = messages.get(KEY_LINK_UUID_HASH);
        if (TextUtils.isEmpty(linkUUIDHash)) {
            LogUtil.logError(
                    TAG,
                    "linkUuidHash is null or empty",
                    new IllegalStateException("linkUuidHash is null or empty"));
            return;
        }

        if (!linkUUIDHash.equals(PhoneUtil.getSKRIDHash(context))) {
            LogUtil.logDebug(TAG, "The sender clicked the old shared link, ignore the request.");
            return;
        }

        // Phone Number
        String phoneNumber = messages.get(KEY_ENCRYPTED_PHONE_NUMBER);
        if (!TextUtils.isEmpty(phoneNumber)) {
            phoneNumber = verificationUtil.decryptMessage(phoneNumber);
        }
        // Public Key
        String publicKey = messages.get(KEY_ENCRYPTED_PUBLIC_KEY);
        if (TextUtils.isEmpty(publicKey)) {
            LogUtil.logError(
                    TAG,
                    "Encrypted publicKey is empty or null",
                    new IllegalStateException("Encrypted publicKey is empty or null"));
            return;
        } else {
            publicKey = verificationUtil.decryptMessage(publicKey);
            if (TextUtils.isEmpty(publicKey)) {
                LogUtil.logError(
                        TAG,
                        "publicKey is empty or null",
                        new IllegalStateException("publicKey is empty or null"));
                return;
            }
        }

        final String senderName = messages.get(KEY_NAME);
        if (TextUtils.isEmpty(senderName)) {
            LogUtil.logError(
                    TAG,
                    "name is empty or null",
                    new IllegalStateException("name is empty or null"));
            return;
        }

        LogUtil.logDebug(TAG, "Receive message, backup request from " + senderName);

        @Nullable final String senderPhoneNumber = phoneNumber;
        @Nullable final String senderPhoneModel = messages.get(KEY_PHONE_MODEL);
        // Resend name
        @Nullable final String targetName = messages.get(KEY_TARGET_NAME);
        // Resend, real Bad
        @Nullable String encTargetUUIDHash = messages.get(KEY_TARGET_UUID_HASH);
        if (!TextUtils.isEmpty(encTargetUUIDHash)) {
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
            encTargetUUIDHash = genericCipherUtil.decryptData(encTargetUUIDHash);
        }
        @Nullable final String targetUUIDHash = encTargetUUIDHash;

        final String senderPublicKey = publicKey;
        final String senderUUIDHash = ChecksumUtil.generateChecksum(uuid);
        if (TextUtils.isEmpty(senderUUIDHash)) {
            LogUtil.logError(
                    TAG,
                    "UUIDHash is empty or null",
                    new IllegalStateException("UUIDHash is empty or null"));
            return;
        }

        final AtomicBoolean isResend = new AtomicBoolean(false);
        final String resendMsg = messages.get(KEY_IS_RESEND);
        if (MSG_IS_RESEND.equals(resendMsg)) {
            LogUtil.logInfo(TAG, "Resend flow");
            isResend.set(true);
        }

        // Resend flow, real Bad, remove status Bad by targetUUIDHash
        if (isResend.get() && !TextUtils.isEmpty(targetUUIDHash)) {
            final AtomicBoolean isThisRequestActionFinish = new AtomicBoolean(false);
            final CountDownLatch latch = new CountDownLatch(1);
            BackupTargetUtil.get(context, targetUUIDHash, new LoadDataListener() {
                @Override
                public void onLoadFinished(
                        BackupSourceEntity backupSourceEntity,
                        BackupTargetEntity backupTargetEntity,
                        RestoreSourceEntity restoreSourceEntity,
                        RestoreTargetEntity restoreTargetEntity) {
                    // Only remove status Bad
                    if (backupTargetEntity != null
                            && backupTargetEntity.compareStatus(BACKUP_TARGET_STATUS_BAD)) {
                        LogUtil.logDebug(TAG, "remove by resend link, "
                                + "targetUUIDHash " + targetUUIDHash);
                        BackupTargetUtil.remove(context, targetUUIDHash,
                                new DatabaseCompleteListener() {
                                    @Override
                                    public void onComplete() {
                                        saveBackupTargetToDB(
                                                context,
                                                senderFcmToken,
                                                senderWhisperPub,
                                                senderPushyToken,
                                                senderUUIDHash,
                                                senderName,
                                                senderPublicKey,
                                                senderPhoneNumber,
                                                senderPhoneModel);
                                        isThisRequestActionFinish.set(true);
                                        latch.countDown();
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        LogUtil.logError(TAG, "remove() failed, e=" + e);
                                        latch.countDown();
                                    }
                                });
                    } else {
                        latch.countDown();
                    }
                }
            });

            try {
                latch.await(TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LogUtil.logError(TAG, "InterruptedException e = " + e);
            }

            if (isThisRequestActionFinish.get()) {
                return;
            }
        }

        // Check if backup finished
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
                        if (backupTargetEntity != null) {
                            // Found previous status
                            final int status = backupTargetEntity.getStatus();
                            switch (status) {
                                case BACKUP_TARGET_STATUS_OK:
                                    LogUtil.logDebug(
                                            TAG,
                                            "Receive "
                                                    + senderName
                                                    + "'s request, but already exists and finish "
                                                    + "backup");
                                    break;
                                case BACKUP_TARGET_STATUS_REQUEST:
                                case BACKUP_TARGET_STATUS_REQUEST_WAIT_OK:
                                    LogUtil.logDebug(
                                            TAG,
                                            "In request status, ignore "
                                                    + senderName
                                                    + "'s duplicate request ");
                                    // Send notification again
                                    Intent intent = IntentUtil.generateBackupTargetIntent(context,
                                            backupTargetEntity);
                                    String name = backupTargetEntity.getName();
                                    String uuidHash = backupTargetEntity.getUUIDHash();
                                    NotificationUtil.showVerificationSharingNotification(
                                            context, intent, name, uuidHash);
                                    break;
                                case BACKUP_TARGET_STATUS_BAD:
                                    LogUtil.logDebug(
                                            TAG,
                                            "Receive "
                                                    + senderName
                                                    + "'s request, previous status is real bad, "
                                                    + "transfer Bad to Request");
                                    // Transfer Bad to Request
                                    // Use BACKUP_TARGET_STATUS_REQUEST_WAIT_OK to prevent
                                    // getPartialSeedV2 before show verification code
                                    backupTargetEntity.setStatus(
                                            BACKUP_TARGET_STATUS_REQUEST_WAIT_OK);
                                    backupTargetEntity.setFcmToken(senderFcmToken);
                                    backupTargetEntity.setWhisperPub(senderWhisperPub);
                                    backupTargetEntity.setPushyToken(senderPushyToken);
                                    backupTargetEntity.setName(senderName);
                                    backupTargetEntity.setPublicKey(senderPublicKey);
                                    backupTargetEntity.setLastCheckedTime(
                                            System.currentTimeMillis());
                                    backupTargetEntity.setPhoneNumber(senderPhoneNumber);
                                    backupTargetEntity.setPhoneModel(senderPhoneModel);
                                    BackupTargetUtil.update(
                                            context,
                                            backupTargetEntity,
                                            new DatabaseCompleteListener() {
                                                @Override
                                                public void onComplete() {
                                                    // Send notification
//                                                    Intent intent =
//                                                            IntentUtil.generateBackupTargetIntent(
//                                                                    context,
//                                                                    backupTargetEntity);
//                                                    String name = backupTargetEntity.getName();
//                                                    String uuidHash =
//                                                            backupTargetEntity.getUUIDHash();
//                                                    showVerificationSharingNotification(
//                                                            context, intent, name, uuidHash);
//
//                                                    // Notify UI to update
//                                                    Intent intentToNotifyUpdateUI =
//                                                            new Intent(ACTION_TRIGGER_BROADCAST);
//                                                    LocalBroadcastManager.getInstance(context)
//                                                            .sendBroadcast
//                                                            (intentToNotifyUpdateUI);
                                                }

                                                @Override
                                                public void onError(Exception exception) {
                                                    LogUtil.logError(
                                                            TAG, "update error, e= " + exception);
                                                }
                                            });
                                    break;
                                case BACKUP_TARGET_STATUS_NO_RESPONSE:
                                    LogUtil.logDebug(
                                            TAG,
                                            "Receive "
                                                    + senderName
                                                    + "'s request, in status No response");
                                    break;
                                default:
                                    LogUtil.logError(
                                            TAG, "Receive a request, unknown status = " + status);
                            }
                        } else {
                            // Can't found previous status

                            // Resend, and can't found previous status by UUIDHash
                            // It's auto backup after recover, we only have name, remove bad by only
                            // name first and continue
                            if (isResend.get() && !TextUtils.isEmpty(targetName)) {
                                LogUtil.logDebug(
                                        TAG, "Try to remove only name first, " + targetName);
                                BackupTargetUtil.removeWithOnlyName(
                                        context,
                                        targetName,
                                        new DatabaseCompleteListener() {
                                            @Override
                                            public void onComplete() {
                                                saveBackupTargetToDB(
                                                        context,
                                                        senderFcmToken,
                                                        senderWhisperPub,
                                                        senderPushyToken,
                                                        senderUUIDHash,
                                                        senderName,
                                                        senderPublicKey,
                                                        senderPhoneNumber,
                                                        senderPhoneModel);
                                            }

                                            @Override
                                            public void onError(Exception exception) {
                                                LogUtil.logError(
                                                        TAG,
                                                        "remove with name error, e= " + exception);
                                            }
                                        });

                            } else {
                                saveBackupTargetToDB(
                                        context,
                                        senderFcmToken,
                                        senderWhisperPub,
                                        senderPushyToken,
                                        senderUUIDHash,
                                        senderName,
                                        senderPublicKey,
                                        senderPhoneNumber,
                                        senderPhoneModel);
                            }
                        }
                    }
                });
    }

    private void saveBackupTargetToDB(
            @NonNull final Context context,
            @Nullable final String senderToken,
            @Nullable final String senderWhisperPub,
            @Nullable final String senderPushyToken,
            @NonNull final String senderUUIDHash,
            @NonNull final String senderName,
            @NonNull final String senderPublicKey,
            @Nullable final String senderPhoneNumber,
            @Nullable final String senderPhoneModel) {

        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(senderUUIDHash)) {
            LogUtil.logError(TAG, "senderUUIDHash is empty", new IllegalArgumentException());
            return;
        }
        if (TextUtils.isEmpty(senderName)) {
            LogUtil.logError(TAG, "senderName is empty", new IllegalArgumentException());
            return;
        }
        if (TextUtils.isEmpty(senderPublicKey)) {
            LogUtil.logError(TAG, "senderPublicKey is empty", new IllegalArgumentException());
            return;
        }

        // Notify target(Bob) if backup full
        if (BackupTargetUtil.getFreeSeedIndex(context).isEmpty()) {
            Map<String, String> messageToSend = new ArrayMap<>();
            messageToSend.put(KEY_PUBLIC_KEY, senderPublicKey);
            new BackupFullAction()
                    .sendInternal(
                            context,
                            senderToken,
                            senderWhisperPub,
                            senderPushyToken,
                            messageToSend);
            LogUtil.logDebug(TAG, "backup full, notify " + senderName);
            return;
        }

        // Store unseeded new request BackupTarget to Database
        final BackupTargetEntity backupTarget = new BackupTargetEntity();
        backupTarget.setStatus(BACKUP_TARGET_STATUS_REQUEST);
        backupTarget.setFcmToken(senderToken);
        backupTarget.setWhisperPub(senderWhisperPub);
        backupTarget.setPushyToken(senderPushyToken);
        backupTarget.setName(senderName);
        backupTarget.setPublicKey(senderPublicKey);
        backupTarget.setUUIDHash(senderUUIDHash);
        backupTarget.setLastCheckedTime(System.currentTimeMillis());
        backupTarget.setPhoneNumber(senderPhoneNumber);
        backupTarget.setPhoneModel(senderPhoneModel);
        BackupTargetUtil.put(
                context,
                backupTarget,
                new DatabaseCompleteListener() {
                    @Override
                    public void onComplete() {
                        LogUtil.logDebug(TAG, "Store " + senderName + "'s unseeded request to DB");

                        // Send notification
                        Intent intent = IntentUtil.generateBackupTargetIntent(context,
                                backupTarget);
                        String name = backupTarget.getName();
                        String uuidHash = backupTarget.getUUIDHash();
                        NotificationUtil.showVerificationSharingNotification(
                                context, intent, name, uuidHash);

                        // Notify UI to update
                        Intent intentToNotifyUpdateUI = new Intent(ACTION_TRIGGER_BROADCAST);
                        LocalBroadcastManager.getInstance(context)
                                .sendBroadcast(intentToNotifyUpdateUI);
                    }

                    @Override
                    public void onError(Exception exception) {
                        LogUtil.logError(TAG, "put error, e= " + exception);
                    }
                });
    }
}
