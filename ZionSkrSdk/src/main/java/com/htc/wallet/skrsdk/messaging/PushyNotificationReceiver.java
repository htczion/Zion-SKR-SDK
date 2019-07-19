package com.htc.wallet.skrsdk.messaging;

import static com.htc.wallet.skrsdk.messaging.MessageConstants.MESSAGE;
import static com.htc.wallet.skrsdk.messaging.MessageConstants.TIMESTAMP;
import static com.htc.wallet.skrsdk.messaging.MessageConstants.TOPIC;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.adapter.ApiKeyAdapter;
import com.htc.wallet.skrsdk.messaging.message.MultiSourceMessage;
import com.htc.wallet.skrsdk.messaging.message.WhisperMessage;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.ThreadUtil;
import com.htc.wallet.skrsdk.whisper.StringConverter;
import com.htc.wallet.skrsdk.whisper.WhisperKeyPair;
import com.htc.wallet.skrsdk.whisper.WhisperUtils;
import com.htc.wallet.skrsdk.whisper.retrofit.jsonbody.ShhMessageBody;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public class PushyNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "PushyNotificationReceiver";

    private static final String MAIL_SERVER_PEER =
            "enode://";

    private static final int MAX_POOL_SIZE = 5;
    private static volatile ThreadPoolExecutor sThreadPoolExecutor =
            ThreadUtil.newFixedThreadPool(MAX_POOL_SIZE, "pushy-notif");

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent == null) {
            LogUtil.logError(TAG, "Intent is null");
            return;
        }

        String message = intent.getStringExtra(MESSAGE);
        final int timestamp = intent.getIntExtra(TIMESTAMP, 0);
        if (TextUtils.isEmpty(message) || timestamp == 0) {
            LogUtil.logError(TAG, "message or timestamp is invalid");
            return;
        }
        final String topic = intent.getStringExtra(TOPIC);
        if (TextUtils.isEmpty(topic)) {
            LogUtil.logError(TAG, "topic is empty");
            return;
        }

        LogUtil.logDebug(
                TAG, "Message: " + message + ", timestamp: " + timestamp + ", topic: " + topic);

        sThreadPoolExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            WhisperKeyPair whisperKeyPair = WhisperUtils.getKeyPair(context);
                            if (whisperKeyPair == null) {
                                LogUtil.logError(
                                        TAG,
                                        "whisperKeyPair is null",
                                        new IllegalStateException("whisperKeyPair is null"));
                                return;
                            }
                            String keyPairId = whisperKeyPair.getKeyPairId();
                            if (TextUtils.isEmpty(keyPairId)) {
                                LogUtil.logError(
                                        TAG,
                                        "keyPairId is empty",
                                        new IllegalStateException("keyPairId is empty"));
                                return;
                            }

                            String msgFilterId = WhisperUtils.shhNewMessageFilter(keyPairId, topic);
                            if (TextUtils.isEmpty(msgFilterId)) {
                                LogUtil.logError(
                                        TAG,
                                        "msgFilterId is empty",
                                        new IllegalStateException("msgFilterId is empty"));
                                return;
                            }
                            ApiKeyAdapter apiKeyAdapter =
                                    ZionSkrSdkManager.getInstance().getApiKeyAdapter();
                            if (apiKeyAdapter == null) {
                                LogUtil.logError(TAG, "apiKeyAdapter is null",
                                        new IllegalStateException("apiKeyAdapter is null"));
                                return;
                            }
                            // TODO Need to distinguish stage from production server
                            String mailServerPwd = apiKeyAdapter.getMailServerPwdStage();
                            if (TextUtils.isEmpty(mailServerPwd)) {
                                LogUtil.logError(
                                        TAG,
                                        "mailServerPwd is empty",
                                        new IllegalStateException("mailServerPwd is empty"));
                                return;
                            }

                            String symKeyID =
                                    WhisperUtils.shhGenerateSymKeyFromPassword(mailServerPwd);
                            if (TextUtils.isEmpty(symKeyID)) {
                                LogUtil.logError(TAG, "symKeyID is empty");
                                return;
                            }

                            String requestResult =
                                    WhisperUtils.shhRequestMessage(
                                            MAIL_SERVER_PEER, symKeyID, topic, timestamp, 0);
                            if (TextUtils.isEmpty(requestResult)) {
                                LogUtil.logWarning(TAG, "requestResult is empty");
                                return;
                            }

                            List<ShhMessageBody.ResultObj> messages =
                                    WhisperUtils.shhGetFilterMessages(msgFilterId);
                            if (messages == null) {
                                LogUtil.logWarning(TAG, "messages is null");
                                return;
                            }
                            for (ShhMessageBody.ResultObj messageBody : messages) {
                                if (messageBody.getTimeStamp() != timestamp) {
                                    LogUtil.logDebug(
                                            TAG, "Not the current message, ignore it.");
                                    continue;
                                }

                                String receivedHexMsg = messageBody.getPayload();
                                String receivedMsg = StringConverter.decodeFromHex(receivedHexMsg);
                                LogUtil.logDebug(TAG, "receivedMsg=" + receivedMsg);

                                int messageType = 0;
                                String dataMessage = null;
                                String senderWhisperPub = null;
                                String receiverWhisperPub = null;
                                String senderPushyToken;
                                String receiverPushyToken;
                                String senderFcmToken = null;
                                String receiverFcmToken = null;

                                @MessageServiceType
                                int messageServiceType =
                                        ZionSkrSdkManager.getInstance().getMessageServiceType();
                                switch (messageServiceType) {
                                    case MessageServiceType.FIREBASE:
                                        LogUtil.logWarning(
                                                TAG,
                                                "Error messaging type=" + messageServiceType);
                                        break;
                                    case MessageServiceType.WHISPER:
                                        WhisperMessage whisperMsg =
                                                WhisperUtils.fromJson(
                                                        receivedMsg, WhisperMessage.class);
                                        if (whisperMsg == null) {
                                            LogUtil.logError(TAG, "whisperMsg is null.");
                                            return;
                                        }
                                        messageType = whisperMsg.getMessageType();
                                        dataMessage = whisperMsg.getMessage();
                                        senderWhisperPub = whisperMsg.getWhisperSender();
                                        receiverWhisperPub = whisperMsg.getWhisperReceiver();
                                        senderPushyToken = whisperMsg.getPushySender();
                                        receiverPushyToken = whisperMsg.getPushyReceiver();
                                        MessageTypeWrapper.toAction(
                                                context,
                                                null,
                                                null,
                                                senderWhisperPub,
                                                receiverWhisperPub,
                                                senderPushyToken,
                                                receiverPushyToken,
                                                dataMessage,
                                                messageType);
                                        break;
                                    case MessageServiceType.MULTI:
                                        MultiSourceMessage multiSourceMessage =
                                                WhisperUtils.fromJson(
                                                        receivedMsg, MultiSourceMessage.class);
                                        if (multiSourceMessage == null) {
                                            LogUtil.logDebug(TAG, "multiSourceMessage is null.");
                                            return;
                                        }
                                        messageType = multiSourceMessage.getMessageType();
                                        dataMessage = multiSourceMessage.getMessage();
                                        senderWhisperPub = multiSourceMessage.getWhisperSender();
                                        receiverWhisperPub =
                                                multiSourceMessage.getWhisperReceiver();
                                        senderPushyToken = multiSourceMessage.getPushySender();
                                        receiverPushyToken = multiSourceMessage.getPushyReceiver();
                                        // senderFcmToken and receiverFcmToken may be empty
                                        senderFcmToken = multiSourceMessage.getSender();
                                        receiverFcmToken = multiSourceMessage.getReceiver();
                                        MessageTypeWrapper.toAction(
                                                context,
                                                senderFcmToken,
                                                receiverFcmToken,
                                                senderWhisperPub,
                                                receiverWhisperPub,
                                                senderPushyToken,
                                                receiverPushyToken,
                                                dataMessage,
                                                messageType);
                                        break;
                                    default:
                                        LogUtil.logError(TAG,
                                                "Invalid messaging type=" + messageServiceType);
                                        break;
                                }
                                LogUtil.logDebug(
                                        TAG,
                                        "Message data payload: "
                                                + messageType
                                                + ", "
                                                + dataMessage
                                                + ", senderWhisperPub="
                                                + senderWhisperPub
                                                + ", receiverWhisperPub="
                                                + receiverWhisperPub
                                                + ", senderFcmToken="
                                                + senderFcmToken
                                                + ", receiverFcmToken="
                                                + receiverFcmToken);
                            }

                            if (WhisperUtils.shhDeleteMessageFilter(msgFilterId)) {
                                LogUtil.logDebug(TAG,
                                        "Message filter deleted, msgFilterId=" + msgFilterId);
                            } else {
                                LogUtil.logWarning(TAG, "Failed to delete whisper message filter");
                            }

                            if (WhisperUtils.shhDeleteSymKey(symKeyID)) {
                                LogUtil.logDebug(TAG,
                                        "Mail server symmetric key id deleted, symKeyID="
                                                + symKeyID);
                            } else {
                                LogUtil.logWarning(TAG,
                                        "Failed to delete mail server symmetric key id");
                            }
                        } catch (Exception e) {
                            LogUtil.logError(TAG, "onReceive error, e=" + e);
                        }
                    }
                });
    }
}
