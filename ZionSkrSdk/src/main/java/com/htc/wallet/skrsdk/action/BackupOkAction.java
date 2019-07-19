package com.htc.wallet.skrsdk.action;

import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_BAD;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_OK;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_CLOSE_SHARING_ACTIVITY;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_TRIGGER_BROADCAST;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.SOCIAL_KEY_RECOVERY_ACTIVE_THRESHOLD;

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
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupTargetUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;
import com.htc.wallet.skrsdk.util.WalletSdkUtil;

import java.util.List;
import java.util.Map;

public class BackupOkAction extends Action {
    private static final String TAG = "BackupOkAction";

    @Override
    int getMessageTypeId() {
        return MessageConstants.TYPE_BACKUP_OK;
    }

    // Bob
    @Override
    public void sendInternal(
            @NonNull final Context context,
            @Nullable final String receiverFcmToken,
            @Nullable final String receiverWhisperPub,
            @Nullable final String receiverPushyToken,
            @NonNull final Map<String, String> messages) {

        // Form Map
        final String msg = messages.get(KEY_OK);
        if (TextUtils.isEmpty(msg)) {
            LogUtil.logError(
                    TAG, "msg is null or empty", new IllegalStateException("msg is null or empty"));
            return;
        }
        final String checkSum = messages.get(KEY_CHECKSUM);
        if (TextUtils.isEmpty(checkSum)) {
            LogUtil.logError(
                    TAG,
                    "checkSum is null or empty",
                    new IllegalStateException("checkSum is null or empty"));
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

        final VerificationUtil verificationUtil = new VerificationUtil(true);
        final String myDeviceId = PhoneUtil.getDeviceId(context);
        if (TextUtils.isEmpty(myDeviceId)) {
            LogUtil.logError(
                    TAG,
                    "myDeviceId is null or empty",
                    new IllegalStateException("myDeviceId is null or empty"));
            return;
        }
        final String encDeviceId = verificationUtil.encryptMessage(myDeviceId, publicKey);
        if (TextUtils.isEmpty(encDeviceId)) {
            LogUtil.logError(
                    TAG,
                    "encDeviceId is null or empty",
                    new IllegalStateException("encDeviceId is null or empty"));
            return;
        }

        Map<String, String> map = new ArrayMap<>();
        map.put(KEY_OK, msg);
        map.put(KEY_ENCRYPTED_UUID, encDeviceId);
        map.put(KEY_CHECKSUM, checkSum);
        sendMessage(context, receiverFcmToken, receiverWhisperPub, receiverPushyToken, map);
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
            LogUtil.logWarning(TAG, "Receive backup ok without wallet, ignore it");
            return;
        }

        final String ok = messages.get(KEY_OK);
        if (TextUtils.isEmpty(ok)) {
            LogUtil.logError(
                    TAG, "ok is null or empty", new IllegalStateException("ok is null or empty"));
            return;
        }

        // Decrypt UUID
        final VerificationUtil verificationUtil = new VerificationUtil(false);
        final String encUUID = messages.get(KEY_ENCRYPTED_UUID);
        if (TextUtils.isEmpty(encUUID)) {
            LogUtil.logError(
                    TAG,
                    "encUUID is null or empty",
                    new IllegalStateException("encUUID is null or empty"));
            return;
        }
        final String senderUUID = verificationUtil.decryptMessage(encUUID);
        if (TextUtils.isEmpty(senderUUID)) {
            LogUtil.logError(
                    TAG,
                    "senderUUID is null or empty",
                    new IllegalStateException("senderUUID is null or empty"));
            return;
        }
        final String senderUUIDHash = ChecksumUtil.generateChecksum(senderUUID);
        if (TextUtils.isEmpty(senderUUIDHash)) {
            LogUtil.logError(
                    TAG,
                    "senderUUIDHash is null or empty",
                    new IllegalStateException("senderUUIDHash is null or empty"));
            return;
        }
        final String checkSum = messages.get(KEY_CHECKSUM);
        if (TextUtils.isEmpty(checkSum)) {
            LogUtil.logError(
                    TAG,
                    "checkSum is null or empty",
                    new IllegalStateException("checkSum is null or empty"));
            return;
        }
        BackupTargetUtil.get(
                context,
                senderUUIDHash,
                new LoadDataListener() {
                    @Override
                    public void onLoadFinished(
                            BackupSourceEntity backupSourceEntity,
                            BackupTargetEntity backupTargetOld,
                            RestoreSourceEntity restoreSourceEntity,
                            RestoreTargetEntity restoreTargetEntity) {
                        if (backupTargetOld == null) {
                            LogUtil.logInfo(TAG, "Receive a backup ok without previous status");
                            return;
                        }

                        final String fcmToken = backupTargetOld.getFcmToken();
                        final String whisperPub = backupTargetOld.getWhisperPub();
                        final String pushyToken = backupTargetOld.getPushyToken();
                        final String name = backupTargetOld.getName();
                        final String publicKey = backupTargetOld.getPublicKey();
                        final String uuidHash = backupTargetOld.getUUIDHash();
                        final int seedIndex = backupTargetOld.getSeedIndex();
                        final int id = backupTargetOld.getId();

                        LogUtil.logDebug(TAG, "Receive message, backup ok from " + name);

                        if (!backupTargetOld.isSeeded()) {
                            LogUtil.logError(
                                    TAG,
                                    "backupTargetOld is not seeded",
                                    new IllegalStateException("backupTargetOld is not seeded"));
                            return;
                        }

                        // After use keyserver encrypt partial seed, may not have checkSum although
                        // seedIndex had been set
                        final String targetCheckSum = backupTargetOld.getCheckSum();
                        if (TextUtils.isEmpty(targetCheckSum)) {
                            LogUtil.logError(TAG, "targetCheckSum is null or empty");
                            return;
                        }

                        String backupResult = ok;
                        if (ok.equals(MSG_OK) && !targetCheckSum.equals(checkSum)) {
                            LogUtil.logError(TAG, "Message OK, but checkSum not match");
                            backupResult = MSG_FAIL;
                        }

                        switch (backupResult) {
                            case MSG_OK:
                                final BackupTargetEntity backupTargetNewOK =
                                        new BackupTargetEntity();
                                backupTargetNewOK.setId(id);
                                backupTargetNewOK.setStatus(BACKUP_TARGET_STATUS_OK);
                                backupTargetNewOK.setFcmToken(fcmToken);
                                backupTargetNewOK.setWhisperPub(whisperPub);
                                backupTargetNewOK.setPushyToken(pushyToken);
                                backupTargetNewOK.setName(name);
                                backupTargetNewOK.setPublicKey(publicKey);
                                backupTargetNewOK.setUUIDHash(uuidHash);
                                backupTargetNewOK.setLastCheckedTime(System.currentTimeMillis());
                                backupTargetNewOK.setSeedIndex(seedIndex);
                                backupTargetNewOK.setCheckSum(checkSum);
                                BackupTargetUtil.update(
                                        context,
                                        backupTargetNewOK,
                                        new DatabaseCompleteListener() {
                                            @Override
                                            public void onComplete() {
                                                BackupTargetUtil.getAllOK(
                                                        context,
                                                        new LoadListListener() {
                                                            @Override
                                                            public void onLoadFinished(
                                                                    List<BackupSourceEntity>
                                                                            backupSourceEntityList,
                                                                    List<BackupTargetEntity>
                                                                            backupTargetEntityList,
                                                                    List<RestoreSourceEntity>
                                                                            restoreSourceEntityList,
                                                                    List<RestoreTargetEntity>
                                                                            restoreTargetEntityList) {
                                                                if (backupTargetEntityList.size()
                                                                        == SOCIAL_KEY_RECOVERY_ACTIVE_THRESHOLD) {
                                                                    SkrSharedPrefs
                                                                            .putShouldShowNotActiveNotification(
                                                                                    context, true);
                                                                    SkrSharedPrefs
                                                                            .putShouldCheckSocialKeyRecoveryActive(
                                                                                    context, true);
                                                                    SkrSharedPrefs
                                                                            .putCheckSocialKeyRecoveryActiveCount(
                                                                                    context, 0);
                                                                    SkrSharedPrefs
                                                                            .putCheckSocialKeyRecoveryActiveTime(
                                                                                    context, 0L);
                                                                }
                                                            }
                                                        });

                                                // Notify UI to update
                                                final LocalBroadcastManager localBroadcastManager =
                                                        LocalBroadcastManager.getInstance(context);

                                                Intent intentToNotifyUpdateUI =
                                                        new Intent(ACTION_TRIGGER_BROADCAST);
                                                localBroadcastManager.sendBroadcast(
                                                        intentToNotifyUpdateUI);

                                                Intent intentToCloseVerificationSharing =
                                                        new Intent(ACTION_CLOSE_SHARING_ACTIVITY);
                                                intentToCloseVerificationSharing.putExtra(
                                                        KEY_UUID_HASH, uuidHash);
                                                localBroadcastManager.sendBroadcast(
                                                        intentToCloseVerificationSharing);
                                            }

                                            @Override
                                            public void onError(Exception exception) {
                                                LogUtil.logError(
                                                        TAG, "update error, e= " + exception);
                                            }
                                        });
                                break;
                            case MSG_FAIL:
                                final BackupTargetEntity backupTargetNewBad =
                                        new BackupTargetEntity();
                                backupTargetNewBad.setId(id);
                                backupTargetNewBad.setStatus(BACKUP_TARGET_STATUS_BAD);
                                backupTargetNewBad.setFcmToken(fcmToken);
                                backupTargetNewBad.setWhisperPub(whisperPub);
                                backupTargetNewBad.setPushyToken(pushyToken);
                                backupTargetNewBad.setName(name);
                                backupTargetNewBad.setPublicKey(publicKey);
                                backupTargetNewBad.setUUIDHash(uuidHash);
                                backupTargetNewBad.setLastCheckedTime(System.currentTimeMillis());
                                backupTargetNewBad.setSeedIndex(seedIndex);
                                backupTargetNewBad.setCheckSum(checkSum);
                                BackupTargetUtil.update(
                                        context,
                                        backupTargetNewBad,
                                        new DatabaseCompleteListener() {
                                            @Override
                                            public void onComplete() {
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
                                        "No matched ok message",
                                        new IllegalStateException("No matched ok message"));
                        }
                    }
                });
    }
}
