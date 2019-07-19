package com.htc.wallet.skrsdk.action;

import static com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity.RESTORE_SOURCE_STATUS_OK;
import static com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity.RESTORE_SOURCE_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_RESTORE_NOTIFY_UI_UPDATE_AFTER_OK_PINCODE;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.crypto.ChecksumUtil;
import com.htc.wallet.skrsdk.crypto.util.VerificationUtil;
import com.htc.wallet.skrsdk.messaging.MessageConstants;
import com.htc.wallet.skrsdk.restore.util.SkrRestoreEditTextStatus;
import com.htc.wallet.skrsdk.restore.util.SkrRestoreEditTextStatusUtil;
import com.htc.wallet.skrsdk.restore.util.SocialRestoreUtil;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.util.RestoreSourceUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.ParseUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;
import com.htc.wallet.skrsdk.util.WalletSdkUtil;

import java.util.Map;

public class RestoreSeedAction extends Action {
    private static final String TAG = "RestoreSeedAction";
    private static final String[][] KEY_PAIR_SEED_ENCRYPTION_MESSAGE =
            new String[][]{
                    {KEY_RESTORE_SOURCE_ENCRYPTED_UUID, KEY_RESTORE_SOURCE_UUID},
            };

    private static final String[] KEY_SEED_ENCRYPTION_MESSAGE =
            new String[]{KEY_RESTORE_SOURCE_ENCRYPTED_UUID};

    private static final String[] KEY_SEED_OTHER_MESSAGE =
            new String[]{
                    KEY_RESTORE_SOURCE_PIN_CODE_POSITION,
                    KEY_RESTORE_SOURCE_SEED,
                    KEY_RESTORE_ENC_SEED_SIGN,
                    KEY_ENCRYPTED_CODE_PK,
                    KEY_ENCRYPTED_CODE_PK_SIGNED,
                    KEY_ENCRYPTED_AES_KEY,
                    KEY_ENCRYPTED_AES_KEY_SIGNED
            };
    // TODO: Add KEY_VERIFY_TIMESTAMP to KEY_SEED_OTHER_MESSAGE

    private static final String[] KEY_SEED_FOR_RESTORE_BACKUP_LIST_MESSAGE =
            new String[]{KEY_RESTORE_SOURCE_NAME};

    private static final String SECRET_PIN_CODE = "* * * * * *";


    @Override
    int getMessageTypeId() {
        return MessageConstants.TYPE_RESTORE_SEED;
    }

    // Bob
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
                messages, KEY_SEED_ENCRYPTION_MESSAGE, KEY_SEED_OTHER_MESSAGE)) {
            LogUtil.logDebug(TAG, "KeysFormatInMessage is error");
            return;
        }
        sendMessage(context, receiverFcmToken, receiverWhisperPub, receiverPushyToken, messages);
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
            LogUtil.logWarning(TAG, "Receive restore seed with wallet, ignore it");
            return;
        }

        if (!isKeysFormatOKInMessage(
                messages, KEY_SEED_ENCRYPTION_MESSAGE, KEY_SEED_OTHER_MESSAGE)) {
            LogUtil.logWarning(TAG, "KeysFormatInMessage is incorrect!");
            return;
        }

        Intent intent =
                setupIntent(
                        messages,
                        KEY_PAIR_SEED_ENCRYPTION_MESSAGE,
                        KEY_SEED_OTHER_MESSAGE,
                        ACTION_RESTORE_NOTIFY_UI_UPDATE_AFTER_OK_PINCODE);
        intent =
                appendIntentWithRestoreBackupListMessage(
                        intent,
                        messages,
                        KEY_SEED_FOR_RESTORE_BACKUP_LIST_MESSAGE,
                        ACTION_RESTORE_NOTIFY_UI_UPDATE_AFTER_OK_PINCODE);
        if (intent == null) {
            LogUtil.logWarning(TAG, "onReceiveInternal, intent is null!");
            return;
        }

        final String strOkCodePosition =
                intent.getStringExtra(KEY_RESTORE_SOURCE_PIN_CODE_POSITION);
        if (TextUtils.isEmpty(strOkCodePosition)) {
            LogUtil.logError(
                    TAG,
                    "strOkCodePosition is null or empty",
                    new IllegalStateException("strOkCodePosition is null or empty"));
            return;
        }
        final int intOkCodePosition = Integer.parseInt(strOkCodePosition);

        SkrRestoreEditTextStatus skrRestoreEditTextStatus =
                SkrRestoreEditTextStatusUtil.getStatus(context, intOkCodePosition);

        if (messages.containsKey(KEY_VERIFY_TIMESTAMP)) {
            // Check timestamp
            String timestampStr = messages.get(KEY_VERIFY_TIMESTAMP);
            long timestamp = ParseUtil.tryParseLong(timestampStr, 0);
            if ((skrRestoreEditTextStatus.getSentTimeMs() != timestamp) ||
                    skrRestoreEditTextStatus.isTimeout()) {
                LogUtil.logDebug(TAG, "Receive restore seed timeout"
                        + ", position=" + intOkCodePosition
                        + ", timestamp=" + timestamp);
                return;
            }
        } else {
            LogUtil.logWarning(TAG, "Without verify timestamp");
            // TODO: Let it pass, until next version
        }

        // Check is this position already restore ok
        if (SocialRestoreUtil.isEditTextPositionHasBeenRestored(context, intOkCodePosition)) {
            LogUtil.logInfo(TAG, "It's restored, position=" + intOkCodePosition);
            return;
        }

        // Add receive number to sharedPrefs
        skrRestoreEditTextStatus.increaseReceivedPinCount();
        SkrRestoreEditTextStatusUtil.putStatus(context, intOkCodePosition,
                skrRestoreEditTextStatus);

        final String restoreSourceUUID = intent.getStringExtra(KEY_RESTORE_SOURCE_UUID);
        final String restoreSourceUUIDHash = ChecksumUtil.generateChecksum(restoreSourceUUID);
        final String restoreSourceName = intent.getStringExtra(KEY_RESTORE_SOURCE_NAME);

        final String encSeed = intent.getStringExtra(KEY_RESTORE_SOURCE_SEED);
        final String encSeedSign = intent.getStringExtra(KEY_RESTORE_ENC_SEED_SIGN);
        final String encCodePk = intent.getStringExtra(KEY_ENCRYPTED_CODE_PK);
        final String encCodePkSign = intent.getStringExtra(KEY_ENCRYPTED_CODE_PK_SIGNED);
        final String encAesKey = intent.getStringExtra(KEY_ENCRYPTED_AES_KEY);
        final String encAesKeySign = intent.getStringExtra(KEY_ENCRYPTED_AES_KEY_SIGNED);
        RestoreSourceUtil.getWithUUIDHash(
                context,
                restoreSourceUUIDHash,
                new LoadDataListener() {
                    @Override
                    public void onLoadFinished(
                            BackupSourceEntity backupSourceEntity,
                            BackupTargetEntity backupTargetEntity,
                            final RestoreSourceEntity restoreSourceEntity,
                            RestoreTargetEntity restoreTargetEntity) {
                        if (restoreSourceEntity == null) {
                            LogUtil.logError(
                                    TAG,
                                    "restoreSourceEntity is null",
                                    new IllegalStateException("restoreSourceEntity is null"));
                            return;
                        }

                        if (restoreSourceEntity.compareStatus(RESTORE_SOURCE_STATUS_REQUEST)) {
                            LogUtil.logInfo(TAG, "saveRestoreSourceAndSendOK");

                            final RestoreSourceEntity saveRestoreSourceEntity =
                                    new RestoreSourceEntity();
                            saveRestoreSourceEntity.setUUIDHash(restoreSourceUUIDHash);
                            saveRestoreSourceEntity.setFcmToken(restoreSourceEntity.getFcmToken());
                            saveRestoreSourceEntity.setWhisperPub(
                                    restoreSourceEntity.getWhisperPub());
                            saveRestoreSourceEntity.setPushyToken(
                                    restoreSourceEntity.getPushyToken());
                            saveRestoreSourceEntity.setPublicKey(
                                    restoreSourceEntity.getPublicKey());
                            saveRestoreSourceEntity.setTimeStamp(
                                    restoreSourceEntity.getTimeStamp());
                            saveRestoreSourceEntity.setPinCode(SECRET_PIN_CODE);
                            saveRestoreSourceEntity.setPinCodePosition(intOkCodePosition);
                            saveRestoreSourceEntity.setSeed(encSeed);
                            saveRestoreSourceEntity.setEncSeedSigned(encSeedSign);
                            saveRestoreSourceEntity.setStatus(RESTORE_SOURCE_STATUS_OK);
                            saveRestoreSourceEntity.setName(restoreSourceName);
                            saveRestoreSourceEntity.setEncCodePk(encCodePk);
                            saveRestoreSourceEntity.setEncCodePkSign(encCodePkSign);
                            saveRestoreSourceEntity.setEncAseKey(encAesKey);
                            saveRestoreSourceEntity.setEncAseKeySign(encAesKeySign);
                            RestoreSourceUtil.update(
                                    context,
                                    saveRestoreSourceEntity,
                                    new DatabaseCompleteListener() {
                                        @Override
                                        public void onComplete() {
                                            LogUtil.logDebug(
                                                    TAG,
                                                    "saveRestoreSourceAndSendOKIfNeeded "
                                                            + "completely, sendRestoreOKAction");
                                            final String email =
                                                    SkrSharedPrefs.getCurrentRestoreEmail(context);
                                            if (TextUtils.isEmpty(email)) {
                                                LogUtil.logError(
                                                        TAG, "email is null or empty",
                                                        new IllegalStateException());
                                                return;
                                            }
                                            final String emailHash = ChecksumUtil.generateChecksum(
                                                    email);
                                            if (TextUtils.isEmpty(emailHash)) {
                                                LogUtil.logError(
                                                        TAG, "emailHash is null or empty",
                                                        new IllegalStateException());
                                                return;
                                            }
                                            final String uuid = PhoneUtil.getSKRID(context);
                                            final VerificationUtil verificationUtil =
                                                    new VerificationUtil(false);
                                            final String encryptedUUID =
                                                    verificationUtil.encryptMessage(uuid,
                                                            restoreSourceEntity.getPublicKey());
                                            final RestoreOKAction restoreOKAction =
                                                    new RestoreOKAction();

                                            final Map<String, String> messagesToRestoreSources =
                                                    restoreOKAction.createOKMessage(encryptedUUID,
                                                            emailHash);
                                            restoreOKAction.send(
                                                    context,
                                                    restoreSourceEntity.getFcmToken(),
                                                    restoreSourceEntity.getWhisperPub(),
                                                    restoreSourceEntity.getPushyToken(),
                                                    messagesToRestoreSources);
                                        }

                                        @Override
                                        public void onError(Exception exception) {
                                            LogUtil.logError(TAG, "update error, e= " + exception);
                                        }
                                    });

                            Intent broadcastIntent =
                                    new Intent(ACTION_RESTORE_NOTIFY_UI_UPDATE_AFTER_OK_PINCODE);
                            broadcastIntent.putExtra(
                                    KEY_RESTORE_SOURCE_PIN_CODE_POSITION, strOkCodePosition);
                            LocalBroadcastManager.getInstance(context)
                                    .sendBroadcast(broadcastIntent);
                        } else {
                            LogUtil.logInfo(TAG, "restoreSourceEntity is OK status, ignore it");
                        }
                    }
                });
    }
}
