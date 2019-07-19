package com.htc.wallet.skrsdk.messaging.message;

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.annotations.SerializedName;
import com.htc.wallet.skrsdk.messaging.MessageConstants;
import com.htc.wallet.skrsdk.util.LogUtil;

public class Message {
    private static final String TAG = "Message";

    @SerializedName("sender")
    private final String sender;

    @SerializedName("receiver")
    private final String receiver;
    // messageType is used for sending different type message. e.g. verify, request, ok...
    @SerializedName("messageType")
    private final int messageType;

    @SerializedName("message")
    private final String message;

    @SerializedName("key")
    private String key;

    @SerializedName("notificationTitle")
    private String notificationTitle;

    // FCM
    public Message(
            String fcmSender, String fcmReceiver, @MessageType int messageType, String message) {
        this.sender = fcmSender;
        this.receiver = fcmReceiver;
        this.messageType = messageType;
        this.message = message;
    }

    public static boolean isValid(Message message) {
        return message != null
                && message.getMessageType() != 0
                && !TextUtils.isEmpty(message.getSender())
                && !TextUtils.isEmpty(message.getReceiver())
                && !TextUtils.isEmpty(message.getMessage());
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public int getMessageType() {
        return messageType;
    }

    public String getMessage() {
        return message;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setNotificationTitle(String notificationTitle) {
        this.notificationTitle = notificationTitle;
    }

    public String getNotificationTitle() {
        return notificationTitle;
    }

    @IntDef({
            MessageConstants.TYPE_BACKUP_REQUEST,
            MessageConstants.TYPE_BACKUP_VERIFY,
            MessageConstants.TYPE_BACKUP_SEED,
            MessageConstants.TYPE_BACKUP_OK,
            MessageConstants.TYPE_BACKUP_ERROR,
            MessageConstants.TYPE_BACKUP_DELETE,
            MessageConstants.TYPE_BACKUP_FULL,
            MessageConstants.TYPE_RESTORE_VERIFY,
            MessageConstants.TYPE_RESTORE_TARGET_VERIFY,
            MessageConstants.TYPE_RESTORE_SEED,
            MessageConstants.TYPE_RESTORE_OK,
            MessageConstants.TYPE_RESTORE_ERROR,
            MessageConstants.TYPE_CHECK_BACKUP_STATUS,
            MessageConstants.TYPE_REPORT_BACKUP_STATUS,
            MessageConstants.TYPE_RESTORE_UUID_CHECK,
            MessageConstants.TYPE_RESTORE_DELETE,
            MessageConstants.TYPE_RESEND_NAME,
            MessageConstants.TYPE_CHECK_BACKUP_VERSION,
            MessageConstants.TYPE_REPORT_BACKUP_EXISTED
    })
    public @interface MessageType {
    }

    @Nullable
    public static Message buildFromRemoteMessage(RemoteMessage remoteMessage) {
        if (remoteMessage == null) {
            LogUtil.logError(TAG, "RemoteMessage is null.");
            return null;
        }
        if (remoteMessage.getData() == null || remoteMessage.getData().isEmpty()) {
            LogUtil.logError(TAG, "payload is empty.");
            return null;
        }
        String message = remoteMessage.getData().get(MessageConstants.MESSAGE);
        int messageType =
                Integer.valueOf(remoteMessage.getData().get(MessageConstants.MESSAGE_TYPE));
        String sender = (remoteMessage.getData().get(MessageConstants.SENDER));
        String receiver = (remoteMessage.getData().get(MessageConstants.RECEIVER));

        return new Message(sender, receiver, messageType, message);
    }
}
