package com.htc.wallet.skrsdk.action;

import static com.htc.wallet.skrsdk.verification.VerificationConstants.EMPTY_STRING;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.crypto.util.VerificationUtil;
import com.htc.wallet.skrsdk.messaging.MessageConstants;
import com.htc.wallet.skrsdk.messaging.message.Message;
import com.htc.wallet.skrsdk.messaging.processor.MessageProcessorWrapper;
import com.htc.wallet.skrsdk.util.JsonUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.ThreadUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

public abstract class Action {
    public static final String KEY_NAME = "key_name";
    public static final String KEY_SOURCE_NAME = "key_source_name";
    public static final String KEY_UUID_HASH = "key_uuid_hash";
    public static final String KEY_LINK_UUID_HASH = "key_link_uuid_hash";
    public static final String KEY_PHONE_NUMBER = "key_phone_number";
    public static final String KEY_PHONE_MODEL = "key_phone_model";
    public static final String KEY_IS_RESEND = "key_is_resend";
    public static final String KEY_PUBLIC_KEY = "key_public_key";
    public static final String KEY_TZID_HASH = "key_tzid_hash";
    public static final String KEY_IS_TEST = "key_is_test";
    public static final String KEY_PIN_CODE = "key_pin_code";
    public static final String KEY_TARGET_NAME = "key_target_name";
    public static final String KEY_TARGET_UUID_HASH = "key_target_uuid_hash";
    public static final String KEY_RETRY_TIMES = "key_retry_times";
    public static final String KEY_RETRY_WAIT_START_TIME = "key_retry_wait_start_time";
    public static final String KEY_ENCRYPTED_UUID = "key_encrypted_uuid";
    public static final String KEY_ENCRYPTED_PUBLIC_KEY = "key_encrypted_public_key";
    // public static final String KEY_ENCRYPTED_PIN_AND_PUBLIC_KEY =
    // "key_encrypted_pin_and_public_key";
    public static final String KEY_ENCRYPTED_CODE_PK = "key_enc_code_pk";
    public static final String KEY_ENCRYPTED_CODE_PK_SIGNED = "key_enc_code_pk_sig";
    public static final String KEY_ENCRYPTED_AES_KEY = "key_enc_aes_key";
    public static final String KEY_ENCRYPTED_AES_KEY_SIGNED = "key_enc_aes_key_sig";
    public static final String KEY_ENCRYPTED_PHONE_NUMBER = "key_encrypted_phone_number";
    public static final String KEY_ENCRYPTED_TOKEN = "key_encrypted_token";
    public static final String KEY_ENCRYPTED_WHISPER_PUB = "key_encrypted_whisper_pub";
    public static final String KEY_ENCRYPTED_PUSHY_TOKEN = "key_encrypted_pushy_token";
    public static final String KEY_EMAIL_HASH = "email_hash";
    public static final String KEY_ENCRYPTED_SEED = "encrypted_seed";
    public static final String KEY_CHECKSUM = "checksum";
    public static final String KEY_ERROR = "key_error";
    public static final String KEY_OK = "key_ok";
    public static final String KEY_BACKUP_FULL = "key_backup_full";
    public static final String KEY_RESTORE_TARGET_UUID_HASH = "key_restore_target_uuid_hash";
    public static final String KEY_RESTORE_TARGET_BACKUP_UUID_HASH =
            "key_restore_target_backup_uuid_hash";
    // public static final String KEY_RESTORE_TARGET_PUBLIC_KEY_HASH =
    // "key_restore_target_public_key_hash";
    // public static final String KEY_RESTORE_TARGET_UUID_HASH_CHECK_STATUS =
    // "key_restore_target_public_key_hash_status";
    public static final String KEY_RESTORE_TARGET_ENCRYPTED_UUID =
            "key_restore_target_encrypted_uuid";
    public static final String KEY_RESTORE_TARGET_EMAIL_HASH =
            "key_restore_target_encrypted_email_hash";
    public static final String KEY_RESTORE_TARGET_ENC_CODE = "key_restore_target_enc_code";
    public static final String KEY_RESTORE_TARGET_ENC_CODE_SIGN =
            "key_restore_target_enc_code_sign";
    public static final String KEY_RESTORE_TARGET_ENCRYPTED_TOKEN =
            "key_restore_target_encrypted_token";
    public static final String KEY_RESTORE_TARGET_ENC_WHISPER_PUB =
            "key_restore_target_enc_whisper_pub";
    public static final String KEY_RESTORE_TARGET_ENC_PUSHY_TOKEN =
            "key_restore_target_enc_pushy_token";
    public static final String KEY_RESTORE_TARGET_NAME = "key_restore_target_name";
    public static final String KEY_RESTORE_SOURCE_PUBLIC_KEY = "key_restore_source_public_key";
    public static final String KEY_RESTORE_SOURCE_TOKEN = "key_restore_source_token";
    public static final String KEY_RESTORE_SOURCE_WHISPER_PUB = "key_restore_source_whisper_pub";
    public static final String KEY_RESTORE_SOURCE_PUSHY_TOKEN = "key_restore_source_pushy_token";
    public static final String KEY_RESTORE_SOURCE_UUID = "key_restore_source_uuid";
    public static final String KEY_RESTORE_SOURCE_SEED = "key_restore_source_seed";
    public static final String KEY_RESTORE_ENC_SEED_SIGN = "key_restore_enc_seed_sign";
    public static final String KEY_RESTORE_SOURCE_NAME = "key_restore_source_name";
    public static final String KEY_RESTORE_SOURCE_PIN_CODE_POSITION =
            "key_restore_source_pin_code_position";
    public static final String KEY_RESTORE_SOURCE_ENCRYPTED_UUID = "key_restore_source_uuid";
    public static final String KEY_RESTORE_SOURCE_ENCRYPTED_PUBLIC_KEY =
            "key_restore_source_encrypted_public_key";
    public static final String KEY_RESTORE_SOURCE_ENCRYPTED_TOKEN =
            "key_restore_source_encrypted_token";
    public static final String KEY_RESTORE_SOURCE_ENCRYPTED_WHISPER_PUB =
            "key_restore_source_encrypted_whisper_pub";
    public static final String KEY_RESTORE_SOURCE_ENCRYPTED_PUSHY_TOKEN =
            "key_restore_source_encrypted_pushy_token";
    public static final String KEY_CHECK_BACKUP_HEALTH = "key_check_backup_health";
    public static final String KEY_REPORT_BACKUP_HEALTH = "key_report_backup_health";
    public static final String KEY_BACKUP_VERSION = "key_backup_version";
    public static final String KEY_VERIFY_TIMESTAMP = "key_v_ts";
    public static final int RESTORE_RETRY_TIMES = 9;
    public static final String MSG_IS_RESEND = "msg_is_resend";
    public static final String MSG_ERROR = "msg_error";
    public static final String MSG_OK = "msg_ok";
    public static final String MSG_FAIL = "msg_fail";
    public static final String MSG_BACKUP_FULL = "msg_backup_full";
    public static final String MSG_CHECK_BACKUP_STATUS = "msg_check_backup_status";
    public static final String MSG_REPORT_BACKUP_HEALTH_OK = "msg_report_backup_health_ok";
    public static final String MSG_REPORT_BACKUP_HEALTH_BAD = "msg_report_backup_health_bad";
    // Use MULTI_TOKEN, MULTI_WHISPER_PUB and MULTI_PUSHY_TOKEN to be the argument of send() method
    // if we want to send the messages to
    // more than 1 user at a time.
    // e.g. BackupHealthReportAction will get all the receivers when calling send() for more
    // efficiency
    public static final String MULTI_TOKEN = "multi_token";
    public static final String MULTI_WHISPER_PUB = "multi_whisper_pub";
    public static final String MULTI_PUSHY_TOKEN = "multi_pushy_token";

    private static final String TAG = "Action";

    private static final String KEY_VERSION = "key_version";
    private static final String VERSION = "3";
    // The whitelist, can receive message from different version
    private static final List<Integer> VERSION_CHECK_IGNORE_WHITELIST =
            Arrays.asList(
                    MessageConstants.TYPE_CHECK_BACKUP_STATUS,
                    MessageConstants.TYPE_REPORT_BACKUP_STATUS);

    private static final int MAX_POOL_SIZE = 5;
    private static volatile ThreadPoolExecutor sThreadPoolExecutor =
            ThreadUtil.newFixedThreadPool(MAX_POOL_SIZE, "skr-action");

    @Message.MessageType
    abstract int getMessageTypeId();

    public final void send(
            @NonNull final Context context,
            @Nullable final String receiverFcmToken,
            @Nullable final String receiverWhisperPub,
            @Nullable final String receiverPushyToken,
            @NonNull final Map<String, String> messages) {
        // Check arguments
        Objects.requireNonNull(context, "context is null.");
        Objects.requireNonNull(messages, "messages is null");
        sThreadPoolExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        sendInternal(
                                context,
                                receiverFcmToken,
                                receiverWhisperPub,
                                receiverPushyToken,
                                messages);
                    }
                });
    }

    abstract void sendInternal(
            @NonNull final Context context,
            @Nullable final String receiverFcmToken,
            @Nullable final String receiverWhisperPub,
            @Nullable final String receiverPushyToken,
            @NonNull final Map<String, String> messages);

    void sendMessage(
            @NonNull final Context context,
            @Nullable final String receiverFcmToken,
            @Nullable final String receiverWhisperPub,
            @Nullable final String receiverPushyToken,
            @NonNull final Map<String, String> map) {
        Objects.requireNonNull(map, "map is null");

        map.put(KEY_VERSION, VERSION);

        MessageProcessorWrapper.sendMessage(
                context,
                receiverFcmToken,
                receiverWhisperPub,
                receiverPushyToken,
                getMessageTypeId(),
                map);
    }

    public final void onReceive(
            @NonNull final Context context,
            @Nullable final String senderFcmToken,
            @Nullable final String myFcmToken,
            @Nullable final String senderWhisperPub,
            @Nullable final String myWhisperPub,
            @Nullable final String senderPushyToken,
            @Nullable final String myPushyToken,
            @NonNull final String message) {
        // Check arguments
        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(message)) {
            throw new IllegalArgumentException("message is empty");
        }

        final Map<String, String> map = JsonUtil.jsonToMap(message);
        if (map == null) {
            LogUtil.logError(TAG, "map is null", new IllegalStateException("map is null"));
            return;
        }
        final String version = map.remove(KEY_VERSION);
        if (!VERSION.equals(version)) {
            // Let backup health monitor message can pass from different version
            if (VERSION_CHECK_IGNORE_WHITELIST.contains(getMessageTypeId())) {
                LogUtil.logInfo(
                        TAG,
                        "Receive incorrect version = "
                                + version
                                + ", but in whitelist = "
                                + getMessageTypeId()
                                + ", pass");
            } else {
                // TODO: Use Notification to notify Amy ?
                LogUtil.logWarning(TAG, "Receive incorrect version, ignore it.");
                return;
            }
        }
        sThreadPoolExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        onReceiveInternal(
                                context,
                                senderFcmToken,
                                myFcmToken,
                                senderWhisperPub,
                                myWhisperPub,
                                senderPushyToken,
                                myPushyToken,
                                map);
                    }
                });
    }

    abstract void onReceiveInternal(
            @NonNull final Context context,
            @Nullable final String senderFcmToken,
            @Nullable final String myFcmToken,
            @Nullable final String senderWhisperPub,
            @Nullable final String myWhisperPub,
            @Nullable final String senderPushyToken,
            @Nullable final String myPushyToken,
            @NonNull final Map<String, String> messages);

    Intent setupIntent(
            @NonNull final Map<String, String> messageContent,
            final String[][] keyPairsForEncryption,
            final String[] otherKeys,
            final String intentAction) {
        if (messageContent == null || TextUtils.isEmpty(intentAction)) {
            LogUtil.logDebug(TAG, "setupIntent, messageContent or intentAction is null");
            return null;
        }
        Intent intent = null;
        if (keyPairsForEncryption != null && keyPairsForEncryption.length >= 0) {
            intent =
                    setupIntentWithEncryptionMessage(
                            messageContent, keyPairsForEncryption, intentAction);
        }
        if (otherKeys != null && otherKeys.length >= 0) {
            intent = appendIntentWithOtherMessage(intent, messageContent, otherKeys, intentAction);
        }
        return intent;
    }

    private Intent setupIntentWithEncryptionMessage(
            @NonNull final Map<String, String> messageContent,
            final String[][] keysForEncryption,
            final String intentAction) {
        if (messageContent == null || TextUtils.isEmpty(intentAction)) {
            LogUtil.logDebug(
                    TAG,
                    "setupIntentWithEncryptionMessage, messageContent or intentAction is null");
            return null;
        }
        final VerificationUtil verificationUtil = new VerificationUtil(false);
        final Intent intent = new Intent(intentAction);
        for (String[] intentKeyPair : keysForEncryption) {
            final String data = messageContent.get(intentKeyPair[0]);
            if (TextUtils.isEmpty(data)) {
                intent.putExtra(intentKeyPair[1], EMPTY_STRING);
                continue;
            }
            final String decryptedData = verificationUtil.decryptMessage(data);
            intent.putExtra(intentKeyPair[1], decryptedData);
        }
        return intent;
    }

    private Intent appendIntentWithOtherMessage(
            @NonNull Intent intent,
            @NonNull final Map<String, String> messageContent,
            final String[] otherKeys,
            final String intentAction) {
        if (messageContent == null) {
            LogUtil.logDebug(TAG, "appendIntentWithOtherMessage, messageContent is null");
            return intent;
        }

        if (intent == null) {
            intent = new Intent(intentAction);
        }

        for (String intentKey : otherKeys) {
            final String data = messageContent.get(intentKey);
            intent.putExtra(intentKey, data);
        }
        return intent;
    }

    Intent appendIntentWithRestoreBackupListMessage(
            @NonNull Intent intent,
            @NonNull final Map<String, String> messageContent,
            final String[] keys,
            final String intentAction) {
        if (messageContent == null || TextUtils.isEmpty(intentAction)) {
            LogUtil.logDebug(
                    TAG,
                    "appendIntentWithRestoreBackupListMessage, messageContent or intentAction is "
                            + "null");
            return intent;
        }

        if (intent == null) {
            intent = new Intent(intentAction);
        }

        for (String intentKey : keys) {
            final String data = messageContent.get(intentKey);
            intent.putExtra(intentKey, data);
        }
        return intent;
    }

    boolean isKeysFormatOKInMessage(
            @NonNull final Map<String, String> messageContent,
            final String[] keysForEncryption,
            final String[] otherKeys) {
        if (messageContent == null) {
            LogUtil.logDebug(TAG, "isKeysFormatOKInMessage, messageContent is null");
            return false;
        }
        boolean isOK = false;
        if (keysForEncryption != null && keysForEncryption.length > 0) {
            isOK = anyEncryptionKeysInMessage(messageContent, keysForEncryption);
        }

        if (otherKeys != null && otherKeys.length > 0) {
            isOK = anyOtherKeysOKInMessage(messageContent, otherKeys);
        }
        return isOK;
    }

    private boolean anyEncryptionKeysInMessage(
            @NonNull final Map<String, String> messageContent, final String[] keysForEncryption) {
        if (messageContent == null) {
            LogUtil.logDebug(TAG, "anyEncryptionKeysInMessage, messageContent is null");
            return false;
        }
        for (String key : keysForEncryption) {
            if (!messageContent.containsKey(key)) {
                LogUtil.logDebug(TAG, "anyEncryptionKeysInMessage, key: " + key + " loss");
                return false;
            }
        }
        return true;
    }

    private boolean anyOtherKeysOKInMessage(
            @NonNull final Map<String, String> messageContent, final String[] otherKeys) {
        if (messageContent == null) {
            LogUtil.logDebug(TAG, "anyOtherKeysOKInMessage, messageContent is null");
            return false;
        }
        for (String key : otherKeys) {
            if (!messageContent.containsKey(key)) {
                LogUtil.logDebug(TAG, "anyOtherKeysOKInMessage, key: " + key + " loss");
                return false;
            }
        }
        return true;
    }
}
