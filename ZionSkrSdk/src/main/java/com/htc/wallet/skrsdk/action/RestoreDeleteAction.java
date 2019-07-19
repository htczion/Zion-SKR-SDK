package com.htc.wallet.skrsdk.action;

import static com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity.RESTORE_SOURCE_STATUS_OK;
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
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.util.RestoreSourceUtil;
import com.htc.wallet.skrsdk.sqlite.util.RestoreTargetUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;
import com.htc.wallet.skrsdk.util.WalletSdkUtil;

import java.util.Map;

public class RestoreDeleteAction extends Action {
    private static final String TAG = "RestoreDeleteAction";

    @Override
    int getMessageTypeId() {
        return MessageConstants.TYPE_RESTORE_DELETE;
    }

    // Bob
    @Override
    public void sendInternal(
            @NonNull final Context context,
            @Nullable final String receiverFcmToken,
            @Nullable final String receiverWhisperPub,
            @Nullable final String receiverPushyToken,
            @NonNull final Map<String, String> messages) {
        // Check map input, email Hash and UUID Hash
        final String emailHash = messages.get(Action.KEY_EMAIL_HASH);
        if (TextUtils.isEmpty(emailHash)) {
            LogUtil.logError(TAG, "emailHash is null or empty", new IllegalArgumentException());
            return;
        }
        final String uuidHash = messages.get(Action.KEY_UUID_HASH);
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(TAG, "uuidHash is null or empty", new IllegalArgumentException());
            return;
        }

        // Find RestoreTarget
        RestoreTargetUtil.get(
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
                        if (restoreTargetEntity == null) {
                            LogUtil.logDebug(
                                    TAG,
                                    "restoreTargetEntity is null, Amy's restore ok may be faster");
                            return;
                        }

                        // Get Amy's publicKey
                        final String publicKey = restoreTargetEntity.getPublicKey();
                        if (TextUtils.isEmpty(publicKey)) {
                            LogUtil.logError(
                                    TAG, "publicKey is null or empty", new IllegalStateException());
                            return;
                        }

                        // Remove RestoreTarget
                        RestoreTargetUtil.remove(context, emailHash, uuidHash);

                        // Encrypt my DeviceId
                        final VerificationUtil verificationUtil = new VerificationUtil(true);
                        final String myDeviceId = PhoneUtil.getDeviceId(context);
                        if (TextUtils.isEmpty(myDeviceId)) {
                            LogUtil.logError(
                                    TAG,
                                    "myDeviceId is null or empty",
                                    new IllegalStateException());
                            return;
                        }
                        final String encMyDeviceId =
                                verificationUtil.encryptMessage(myDeviceId, publicKey);
                        if (TextUtils.isEmpty(encMyDeviceId)) {
                            LogUtil.logError(
                                    TAG,
                                    "encMyDeviceId is null or empty",
                                    new IllegalStateException());
                            return;
                        }

                        // Send Restore Delete to Amy
                        final Map<String, String> map = new ArrayMap<>();
                        map.put(KEY_LINK_UUID_HASH, uuidHash);
                        map.put(KEY_ENCRYPTED_UUID, encMyDeviceId);
                        sendMessage(
                                context,
                                receiverFcmToken,
                                receiverWhisperPub,
                                receiverPushyToken,
                                map);

                        Intent intentToNotifyUpdateUI = new Intent(ACTION_TRIGGER_BROADCAST);
                        LocalBroadcastManager.getInstance(context)
                                .sendBroadcast(intentToNotifyUpdateUI);
                    }
                });
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
        if (isSeedExists) {
            LogUtil.logWarning(TAG, "Receive restore delete with wallet, ignore it");
            return;
        }

        if (messages == null) {
            LogUtil.logError(TAG, "messages is null", new IllegalArgumentException());
            return;
        }

        // Check link UUID Hash
        final String linkUUIDHash = messages.get(KEY_LINK_UUID_HASH);
        if (TextUtils.isEmpty(linkUUIDHash)) {
            LogUtil.logError(TAG, "linkUUIDHash is null or empty", new IllegalArgumentException());
            return;
        }
        if (!linkUUIDHash.equals(PhoneUtil.getSKRIDHash(context))) {
            LogUtil.logDebug(
                    TAG, "Receive restore delete action, link UUID Hash not equals my, ignore it.");
            return;
        }

        // Check encrypted Bob's UUID (DeviceId)
        final String encUUID = messages.get(KEY_ENCRYPTED_UUID);
        if (TextUtils.isEmpty(encUUID)) {
            LogUtil.logError(TAG, "encUUID is null or empty", new IllegalArgumentException());
            return;
        }

        // Decrypt UUID
        final VerificationUtil verificationUtil = new VerificationUtil(false);
        final String senderUUID = verificationUtil.decryptMessage(encUUID);
        if (TextUtils.isEmpty(senderUUID)) {
            LogUtil.logError(TAG, "senderUUID is null or empty", new IllegalStateException());
            return;
        }

        // Hashed sender UUID
        final String senderUUIDHash = ChecksumUtil.generateChecksum(senderUUID);
        if (TextUtils.isEmpty(senderUUIDHash)) {
            LogUtil.logError(TAG, "senderUUIDHash is null or empty", new IllegalStateException());
            return;
        }

        // Check status
        RestoreSourceUtil.getWithUUIDHash(
                context,
                senderUUIDHash,
                new LoadDataListener() {
                    @Override
                    public void onLoadFinished(
                            BackupSourceEntity backupSourceEntity,
                            BackupTargetEntity backupTargetEntity,
                            RestoreSourceEntity restoreSourceEntity,
                            RestoreTargetEntity restoreTargetEntity) {
                        if (restoreSourceEntity == null) {
                            LogUtil.logDebug(
                                    TAG,
                                    "Receive restore delete without RestoreSource, ignore it.");
                            return;
                        }
                        if (restoreSourceEntity.compareStatus(RESTORE_SOURCE_STATUS_OK)) {
                            LogUtil.logDebug(
                                    TAG,
                                    "Receive restore delete with OK RestoreSource, ignore it.");
                            return;
                        }
                        LogUtil.logDebug(
                                TAG, "Receive message, restore delete from " + senderUUIDHash);
                        // Remove RestoreDelete
                        RestoreSourceUtil.removeWithUUIDHash(context, senderUUIDHash);
                    }
                });
    }
}
