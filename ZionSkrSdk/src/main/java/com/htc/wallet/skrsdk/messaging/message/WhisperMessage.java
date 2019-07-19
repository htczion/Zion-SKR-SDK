package com.htc.wallet.skrsdk.messaging.message;

import com.google.gson.annotations.SerializedName;

public class WhisperMessage extends Message {
    @SerializedName("whisperSender")
    private final String whisperSender;

    @SerializedName("whisperReceiver")
    private final String whisperReceiver;

    @SerializedName("pushySender")
    private final String pushySender;

    @SerializedName("pushyReceiver")
    private final String pushyReceiver;

    public WhisperMessage(
            String whisperSender,
            String whisperReceiver,
            String pushySender,
            String pushyReceiver,
            @MessageType int messageType,
            String message) {
        super(null, null, messageType, message);

        this.whisperSender = whisperSender;
        this.whisperReceiver = whisperReceiver;
        this.pushySender = pushySender;
        this.pushyReceiver = pushyReceiver;
    }

    public String getWhisperSender() {
        return whisperSender;
    }

    public String getWhisperReceiver() {
        return whisperReceiver;
    }

    public String getPushySender() {
        return pushySender;
    }

    public String getPushyReceiver() {
        return pushyReceiver;
    }
}
