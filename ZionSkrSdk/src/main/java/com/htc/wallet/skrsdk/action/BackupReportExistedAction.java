package com.htc.wallet.skrsdk.action;

import static com.htc.wallet.skrsdk.applink.AppLinkConstant.KEY_TOKEN;
import static com.htc.wallet.skrsdk.applink.AppLinkConstant.KEY_WHISPER_PUB;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_RECEIVE_REPORT_OK;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.ArrayMap;

import com.htc.wallet.skrsdk.messaging.MessageConstants;

import java.util.Map;

public class BackupReportExistedAction extends Action {
    @Override
    int getMessageTypeId() {
        return MessageConstants.TYPE_REPORT_BACKUP_EXISTED;
    }

    // Amy
    @Override
    void sendInternal(
            @NonNull Context context,
            @Nullable String receiverFcmToken,
            @Nullable String receiverWhisperPub,
            @Nullable String receiverPushyToken,
            @NonNull Map<String, String> messages) {
        Map<String, String> map = new ArrayMap<>();
        map.put(KEY_OK, MSG_OK);
        sendMessage(context, receiverFcmToken, receiverWhisperPub, receiverPushyToken, map);
    }

    // Bob
    @Override
    void onReceiveInternal(
            @NonNull Context context,
            @Nullable String senderFcmToken,
            @Nullable String myFcmToken,
            @Nullable String senderWhisperPub,
            @Nullable String myWhisperPub,
            @Nullable String senderPushyToken,
            @Nullable String myPushyToken,
            @NonNull Map<String, String> messages) {
        Intent intent = new Intent(ACTION_RECEIVE_REPORT_OK);
        // To cover all the MESSAGING_SERVICE_TYPE
        intent.putExtra(KEY_TOKEN, senderFcmToken);
        intent.putExtra(KEY_WHISPER_PUB, senderWhisperPub);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
