package com.htc.wallet.skrsdk.action;

import static com.htc.wallet.skrsdk.messaging.processor.MessageProcessorFactory.PUSHY_MESSAGE_PROCESSOR;
import static com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity.RESTORE_SOURCE_STATUS_OK;
import static com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity.RESTORE_SOURCE_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_RESTORE_TARGET_VERITY_ACTION;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.crypto.ChecksumUtil;
import com.htc.wallet.skrsdk.crypto.util.VerificationUtil;
import com.htc.wallet.skrsdk.messaging.MessageConstants;
import com.htc.wallet.skrsdk.messaging.MultiTokenListener;
import com.htc.wallet.skrsdk.messaging.processor.MessageProcessorFactory;
import com.htc.wallet.skrsdk.messaging.processor.MessageProcessorWrapper;
import com.htc.wallet.skrsdk.messaging.processor.PushyMessageProcessor;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.util.RestoreSourceUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;
import com.htc.wallet.skrsdk.util.WalletSdkUtil;
import com.htc.wallet.skrsdk.whisper.WhisperKeyPair;
import com.htc.wallet.skrsdk.whisper.WhisperUtils;

import java.util.HashMap;
import java.util.Map;

public class RestoreTargetVerifyAction extends Action {
    private static final String TAG = "RestoreTargetVerifyAction";
    private static final String[][] KEY_PAIR_TARGET_VERIFY_ENCRYPTION_MESSAGE =
            new String[][]{
                    {KEY_RESTORE_SOURCE_ENCRYPTED_UUID, KEY_RESTORE_SOURCE_UUID},
                    {KEY_RESTORE_SOURCE_ENCRYPTED_PUBLIC_KEY, KEY_RESTORE_SOURCE_PUBLIC_KEY},
                    {KEY_RESTORE_SOURCE_ENCRYPTED_TOKEN, KEY_RESTORE_SOURCE_TOKEN},
                    {KEY_RESTORE_SOURCE_ENCRYPTED_WHISPER_PUB, KEY_RESTORE_SOURCE_WHISPER_PUB},
                    {KEY_RESTORE_SOURCE_ENCRYPTED_PUSHY_TOKEN, KEY_RESTORE_SOURCE_PUSHY_TOKEN}
            };

    private static final String[] KEY_TARGET_VERIFY_ENCRYPTION_MESSAGE =
            new String[]{
                    KEY_RESTORE_SOURCE_ENCRYPTED_UUID,
                    KEY_RESTORE_SOURCE_ENCRYPTED_PUBLIC_KEY,
                    KEY_RESTORE_SOURCE_ENCRYPTED_TOKEN,
                    KEY_RESTORE_SOURCE_ENCRYPTED_WHISPER_PUB,
                    KEY_RESTORE_SOURCE_ENCRYPTED_PUSHY_TOKEN
            };

    private static final String[] KEY_TARGET_VERIFY_OTHER_MESSAGE =
            new String[]{
                    KEY_RESTORE_TARGET_UUID_HASH,
                    KEY_RESTORE_TARGET_BACKUP_UUID_HASH,
                    KEY_RESTORE_TARGET_NAME
            };

    @Override
    int getMessageTypeId() {
        return MessageConstants.TYPE_RESTORE_TARGET_VERIFY;
    }

    // Bob
    @Override
    public void sendInternal(
            @NonNull final Context context,
            @Nullable final String receiverFcmToken,
            @Nullable final String receiverWhisperPub,
            @Nullable final String receiverPushyToken,
            @NonNull final Map<String, String> messages) {
        if (messages == null || messages.isEmpty()) {
            LogUtil.logError(TAG, "sendInternal, messages is null or empty!");
            return;
        }

        final String publicKey = messages.get(KEY_PUBLIC_KEY);
        if (TextUtils.isEmpty(publicKey)) {
            LogUtil.logError(TAG, "sendInternal, publicKey is null or empty!",
                    new IllegalStateException("publicKey is null or empty"));
            return;
        }

        final VerificationUtil verificationUtil = new VerificationUtil(true);
        final String myDeviceId = PhoneUtil.getDeviceId(context);
        final String encMyDeviceId = verificationUtil.encryptMessage(myDeviceId, publicKey);
        final String myPublicKey = verificationUtil.getPublicKeyString();
        final String encMyPublicKey = verificationUtil.encryptMessage(myPublicKey, publicKey);
        final String uuidHash = messages.get(
                KEY_LINK_UUID_HASH); // RestoreTarget's uuidHash (link uuidHash)
        final String backupSourceUuidHash = messages.get(
                KEY_RESTORE_TARGET_UUID_HASH); // backupSourceUuidHash

        if (TextUtils.isEmpty(encMyDeviceId)) {
            LogUtil.logError(TAG, "encMyDeviceId is empty",
                    new IllegalStateException("encMyDeviceId is empty"));
            return;
        }
        if (TextUtils.isEmpty(encMyPublicKey)) {
            LogUtil.logError(TAG, "encMyPublicKey is empty",
                    new IllegalStateException("encMyPublicKey is empty"));
            return;
        }
        if (TextUtils.isEmpty(backupSourceUuidHash)) {
            LogUtil.logError(TAG, "backupSourceUuidHash is empty",
                    new IllegalStateException("backupSourceUuidHash is empty"));
            return;
        }

        MessageProcessorWrapper.getMultiToken(context, new MultiTokenListener() {
            @Override
            public void onUserTokenReceived(@Nullable String pushyToken,
                    @Nullable String fcmToken) {
                if (!TextUtils.isEmpty(receiverWhisperPub) && !TextUtils.isEmpty(
                        receiverPushyToken)) {
                    final WhisperKeyPair whisperKeyPair = WhisperUtils.getKeyPair(context);
                    String myWhisperPub = null;
                    if (whisperKeyPair != null) {
                        myWhisperPub = whisperKeyPair.getPublicKey();
                    } else {
                        LogUtil.logWarning(TAG, "whisperKeyPair is null");
                    }

                    final PushyMessageProcessor processor =
                            (PushyMessageProcessor) MessageProcessorFactory.getInstance().getMessageProcessor(
                                    PUSHY_MESSAGE_PROCESSOR);
                    if (processor == null) {
                        LogUtil.logError(TAG, "processor is null",
                                new IllegalStateException("processor is null"));
                        return;
                    }
                    String encMyWhisperPub = null;
                    String encMyPushyToken = null;
                    String encMyFcmToken = null;

                    if (!TextUtils.isEmpty(myWhisperPub) && !TextUtils.isEmpty(pushyToken)) {
                        encMyWhisperPub = verificationUtil.encryptMessage(myWhisperPub, publicKey);
                        encMyPushyToken = verificationUtil.encryptMessage(pushyToken, publicKey);
                        if (TextUtils.isEmpty(encMyWhisperPub)) {
                            LogUtil.logError(TAG, "encMyWhisperPub is empty",
                                    new IllegalStateException("encMyWhisperPub is empty"));
                            return;
                        }
                        if (TextUtils.isEmpty(encMyPushyToken)) {
                            LogUtil.logError(TAG, "encMyPushyToken is empty",
                                    new IllegalStateException("encMyPushyToken is empty"));
                            return;
                        }
                    }

                    // For MESSAGING_TYPE == MULTI_MESSAGING
                    if (!TextUtils.isEmpty(receiverFcmToken) && !TextUtils.isEmpty(fcmToken)) {
                        encMyFcmToken = verificationUtil.encryptMessage(fcmToken, publicKey);
                        if (TextUtils.isEmpty(encMyFcmToken)) {
                            LogUtil.logError(TAG, "encMyFcmToken is empty",
                                    new IllegalStateException("encMyFcmToken is empty"));
                            return;
                        }
                    }

                    if ((TextUtils.isEmpty(encMyWhisperPub) || TextUtils.isEmpty(encMyPushyToken))
                            && TextUtils.isEmpty(encMyFcmToken)) {
                        LogUtil.logError(TAG, "All the tokens are empty",
                                new IllegalStateException("All the tokens are empty"));
                        return;
                    }

                    Map<String, String> map = new HashMap<>();
                    map.put(KEY_RESTORE_SOURCE_ENCRYPTED_UUID, encMyDeviceId);
                    map.put(KEY_RESTORE_SOURCE_ENCRYPTED_PUBLIC_KEY, encMyPublicKey);
                    map.put(KEY_RESTORE_SOURCE_ENCRYPTED_TOKEN, encMyFcmToken);
                    map.put(KEY_RESTORE_SOURCE_ENCRYPTED_WHISPER_PUB, encMyWhisperPub);
                    map.put(KEY_RESTORE_SOURCE_ENCRYPTED_PUSHY_TOKEN, encMyPushyToken);
                    map.put(KEY_RESTORE_TARGET_UUID_HASH,
                            backupSourceUuidHash); // uuid hash from Shared Link (Amy store in
                    // google drive)
                    map.put(KEY_RESTORE_TARGET_BACKUP_UUID_HASH,
                            backupSourceUuidHash); // uuid hash from Bob's DB
                    map.put(KEY_LINK_UUID_HASH, uuidHash);
                    // TODO: Remove duplicated value ?

                    if (!isKeysFormatOKInMessage(map, KEY_TARGET_VERIFY_ENCRYPTION_MESSAGE, null)) {
                        LogUtil.logError(TAG, "sendInternal, KeysFormatInMessage is incorrect!");
                        return;
                    }
                    sendMessage(context, receiverFcmToken, receiverWhisperPub, receiverPushyToken,
                            map);
                } else if (!TextUtils.isEmpty(receiverFcmToken)) {
                    if (TextUtils.isEmpty(fcmToken)) {
                        LogUtil.logError(TAG, "fcmToken is empty",
                                new IllegalStateException("token is empty"));
                        return;
                    }

                    String encMyFCMToken = verificationUtil.encryptMessage(fcmToken, publicKey);
                    if (TextUtils.isEmpty(encMyFCMToken)) {
                        LogUtil.logError(TAG, "encMyFCMToken is empty",
                                new IllegalStateException("encMyFCMToken is empty"));
                        return;
                    }
                    Map<String, String> map = new HashMap<>();
                    map.put(KEY_RESTORE_SOURCE_ENCRYPTED_UUID, encMyDeviceId);
                    map.put(KEY_RESTORE_SOURCE_ENCRYPTED_PUBLIC_KEY, encMyPublicKey);
                    map.put(KEY_RESTORE_SOURCE_ENCRYPTED_TOKEN, encMyFCMToken);
                    map.put(KEY_RESTORE_SOURCE_ENCRYPTED_WHISPER_PUB, null);
                    map.put(KEY_RESTORE_SOURCE_ENCRYPTED_PUSHY_TOKEN, null);
                    map.put(KEY_RESTORE_TARGET_UUID_HASH,
                            backupSourceUuidHash); // uuid hash from Shared Link (Amy store in
                    // google drive)
                    map.put(KEY_RESTORE_TARGET_BACKUP_UUID_HASH,
                            backupSourceUuidHash); // uuid hash from Bob's DB
                    map.put(KEY_LINK_UUID_HASH, uuidHash);

                    if (!isKeysFormatOKInMessage(map, KEY_TARGET_VERIFY_ENCRYPTION_MESSAGE, null)) {
                        LogUtil.logError(TAG, "sendInternal, KeysFormatInMessage is incorrect!");
                        return;
                    }
                    sendMessage(context, receiverFcmToken, receiverWhisperPub, receiverPushyToken,
                            map);
                } else {
                    LogUtil.logError(TAG, "No valid receiver",
                            new IllegalStateException("No valid receiver"));
                }
            }

            @Override
            public void onUserTokenError(Exception exception) {
                LogUtil.logError(TAG, "getUserToken with exception = " + exception);
            }
        });
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
        if (isSeedExists) {
            LogUtil.logWarning(TAG, "Receive restore target verify with wallet, ignore it");
            return;
        }

        final String linkUUIDHash = messages.get(KEY_LINK_UUID_HASH);
        if (TextUtils.isEmpty(linkUUIDHash)) {
            LogUtil.logError(TAG, "linkUUIDHash is null or empty",
                    new IllegalStateException("linkUUIDHash is null or empty"));
            return;
        }

        if (!linkUUIDHash.equals(PhoneUtil.getSKRIDHash(context))) {
            LogUtil.logError(TAG, "The sender clicked the old shared link, ignore the request.");
            return;
        }

        if (!isKeysFormatOKInMessage(messages, KEY_TARGET_VERIFY_ENCRYPTION_MESSAGE, null)) {
            LogUtil.logError(TAG, "onReceiveInternal, KeysFormatInMessage is error");
            return;
        }

        final Intent intent = setupIntent(messages, KEY_PAIR_TARGET_VERIFY_ENCRYPTION_MESSAGE, null,
                ACTION_RESTORE_TARGET_VERITY_ACTION);
        if (intent == null) {
            LogUtil.logError(TAG, "onReceiveInternal, intent is null");
            return;
        }

        // Amy's backup uuidHash, from Amy's shared link (Bob press)
        String backupUUIDHash = messages.get(KEY_RESTORE_TARGET_UUID_HASH);
        if (TextUtils.isEmpty(backupUUIDHash)) {
            LogUtil.logError(TAG, "backupUUIDHash is null or empty",
                    new IllegalStateException("backupUUIDHash is null or empty"));
            return;
        }
        // Amy's backup uuidHash, from Bob's DB
        String backupSourceUUIDHash = messages.get(KEY_RESTORE_TARGET_BACKUP_UUID_HASH);
        if (TextUtils.isEmpty(backupSourceUUIDHash)) {
            LogUtil.logError(TAG, "backupSourceUUIDHash is null or empty",
                    new IllegalStateException("backupSourceUUIDHash is null or empty"));
            return;
        }
        // Check match (Amy's shared link and Bob's DB) , can not happen
        if (!backupUUIDHash.equals(backupSourceUUIDHash)) {
            LogUtil.logError(TAG, "backupSourceUUIDHash is not match", new IllegalStateException());
            return;
        }

        // Amy's backup uuidHash, from Amy's shared preferences
        String uuidHashFromSp = SkrSharedPrefs.getRestoreUUIDHash(context);
        if (TextUtils.isEmpty(uuidHashFromSp)) {
            LogUtil.logError(TAG, "uuidHashFromSp is null or empty",
                    new IllegalStateException("uuidHashFromSp is null or empty"));
            return;
        }
        // Check match (Amy's shared preferences and Amy's shared link)
        if (!uuidHashFromSp.equals(backupUUIDHash)) {
            LogUtil.logInfo(TAG, "backupUUIDHash is not match, ignore it");
            return;
        }

        final String uuid = intent.getStringExtra(KEY_RESTORE_SOURCE_UUID);
        final String uuidHash = ChecksumUtil.generateChecksum(uuid);
        final String restoreSourceToken = intent.getStringExtra(KEY_RESTORE_SOURCE_TOKEN);
        final String restoreSourceWhisperPub = intent.getStringExtra(
                KEY_RESTORE_SOURCE_WHISPER_PUB);
        final String restoreSourcePushyToken = intent.getStringExtra(
                KEY_RESTORE_SOURCE_PUSHY_TOKEN);
        final String restoreSourcePublicKey = intent.getStringExtra(KEY_RESTORE_SOURCE_PUBLIC_KEY);
        LogUtil.logDebug(TAG, "Receive message, target verify from " + uuidHash);
        RestoreSourceUtil.getWithUUIDHash(context, uuidHash, new LoadDataListener() {
            @Override
            public void onLoadFinished(
                    BackupSourceEntity backupSourceEntity,
                    BackupTargetEntity backupTargetEntity,
                    RestoreSourceEntity restoreSourceEntity,
                    RestoreTargetEntity restoreTargetEntity) {
                if (restoreSourceEntity != null && restoreSourceEntity.compareStatus(
                        RESTORE_SOURCE_STATUS_OK)) {
                    LogUtil.logInfo(TAG, "Restore source is now in OK status, ignore it.");
                    return;
                }

                final RestoreSourceEntity saveRestoreSourceEntity = new RestoreSourceEntity();
                saveRestoreSourceEntity.setStatus(RESTORE_SOURCE_STATUS_REQUEST);
                saveRestoreSourceEntity.setUUIDHash(uuidHash);
                saveRestoreSourceEntity.setFcmToken(restoreSourceToken);
                saveRestoreSourceEntity.setWhisperPub(restoreSourceWhisperPub);
                saveRestoreSourceEntity.setPushyToken(restoreSourcePushyToken);
                saveRestoreSourceEntity.setPublicKey(restoreSourcePublicKey);
                saveRestoreSourceEntity.setTimeStamp(System.currentTimeMillis());
                saveRestoreSourceEntity.setPinCodePosition(0);

                RestoreSourceUtil.put(context, saveRestoreSourceEntity,
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
    }
}
