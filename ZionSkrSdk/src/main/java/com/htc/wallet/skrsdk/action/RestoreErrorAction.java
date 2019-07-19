package com.htc.wallet.skrsdk.action;

import static com.htc.wallet.skrsdk.restore.RestoreVerificationCodeView.ET_PIN_SIZE;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_RESTORE_NOTIFY_UI_UPDATE_AFTER_ERROR_PINCODE;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.htc.wallet.skrsdk.messaging.MessageConstants;
import com.htc.wallet.skrsdk.restore.util.SkrRestoreEditTextStatus;
import com.htc.wallet.skrsdk.restore.util.SkrRestoreEditTextStatusUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.ParseUtil;
import com.htc.wallet.skrsdk.util.WalletSdkUtil;

import java.util.HashMap;
import java.util.Map;

public class RestoreErrorAction extends Action {
    private static final String TAG = "RestoreErrorAction";
    private static final String[] KEY_ERROR_OTHER_MESSAGE =
            new String[]{KEY_RESTORE_SOURCE_PIN_CODE_POSITION};
    // TODO: Add KEY_VERIFY_TIMESTAMP to KEY_ERROR_OTHER_MESSAGE

    @Override
    int getMessageTypeId() {
        return MessageConstants.TYPE_RESTORE_ERROR;
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

        if (!isKeysFormatOKInMessage(messages, null, KEY_ERROR_OTHER_MESSAGE)) {
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
            LogUtil.logWarning(TAG, "Receive restore error with wallet, ignore it");
            return;
        }


        if (!isKeysFormatOKInMessage(messages, null, KEY_ERROR_OTHER_MESSAGE)) {
            LogUtil.logError(TAG, "KeysFormatInMessage is error");
            return;
        }
        LogUtil.logInfo(
                TAG,
                "Receive message, restore error position = "
                        + messages.get(KEY_RESTORE_SOURCE_PIN_CODE_POSITION));

        // Check position
        String positionStr = messages.get(KEY_RESTORE_SOURCE_PIN_CODE_POSITION);
        int position = ParseUtil.tryParseInt(positionStr, -1);
        if (position < 0 || position >= ET_PIN_SIZE) {
            LogUtil.logError(TAG, "Incorrect position=" + position);
            return;
        }

        SkrRestoreEditTextStatus skrRestoreEditTextStatus =
                SkrRestoreEditTextStatusUtil.getStatus(context, position);

        if (messages.containsKey(KEY_VERIFY_TIMESTAMP)) {
            // Check timestamp
            String timestampStr = messages.get(KEY_VERIFY_TIMESTAMP);
            long timestamp = ParseUtil.tryParseLong(timestampStr, 0);
            if ((skrRestoreEditTextStatus.getSentTimeMs() != timestamp) ||
                    skrRestoreEditTextStatus.isTimeout()) {
                LogUtil.logDebug(TAG, "Receive restore error timeout"
                        + ", position=" + position
                        + ", timestamp=" + timestamp);
                return;
            }
        } else {
            LogUtil.logWarning(TAG, "Without verify timestamp");
            // TODO: Let it pass, until next version
        }

        // Add receive number to sharedPrefs
        skrRestoreEditTextStatus.increaseReceivedPinCount();
        SkrRestoreEditTextStatusUtil.putStatus(context, position, skrRestoreEditTextStatus);

        Intent intent =
                setupIntent(
                        messages,
                        null,
                        KEY_ERROR_OTHER_MESSAGE,
                        ACTION_RESTORE_NOTIFY_UI_UPDATE_AFTER_ERROR_PINCODE);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    Map<String, String> createErrorMessage(
            final String restoreTargetPinCodePosition,
            final String restoreTargetVerifyTimestamp) {
        if (restoreTargetPinCodePosition == null) {
            LogUtil.logDebug(TAG, "createErrorMessage, restoreTargetPinCodePosition is null");
            return null;
        }
        final Map<String, String> messages = new HashMap<>();
        messages.put(KEY_RESTORE_SOURCE_PIN_CODE_POSITION, restoreTargetPinCodePosition);
        messages.put(KEY_VERIFY_TIMESTAMP, restoreTargetVerifyTimestamp);
        return messages;
    }
}
