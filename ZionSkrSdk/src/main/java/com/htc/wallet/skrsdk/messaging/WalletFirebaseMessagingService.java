package com.htc.wallet.skrsdk.messaging;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.htc.wallet.skrsdk.crypto.util.GenericCipherUtil;
import com.htc.wallet.skrsdk.messaging.message.Message;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;
import com.htc.wallet.skrsdk.util.ThreadUtil;

import java.util.concurrent.ThreadPoolExecutor;

public class WalletFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "WalletFirebaseMessagingService";
    private static final int MAX_POOL_SIZE = 2;
    private static volatile ThreadPoolExecutor sThreadPoolExecutor =
            ThreadUtil.newFixedThreadPool(MAX_POOL_SIZE, "firebase-msg-srv");

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Message message = Message.buildFromRemoteMessage(remoteMessage);
        if (message == null) {
            LogUtil.logDebug(TAG, "Message is empty.");
            return;
        }

        String dataMessage = message.getMessage();
        int dataMessageType = message.getMessageType();
        String dataSender = message.getSender();
        String dataReceiver = message.getReceiver();
        if (dataMessage == null || dataMessage.isEmpty()) {
            LogUtil.logError(TAG, "message is empty.");
            return;
        }
        LogUtil.logDebug(
                TAG, "Message data payload: " + dataMessageType + ", " + dataMessage + ", "
                        + dataSender + ", " + dataReceiver);


        MessageTypeWrapper.toAction(
                getApplicationContext(),
                dataSender,
                dataReceiver,
                null,
                null,
                null,
                null,
                dataMessage,
                dataMessageType);
    }

    @Override
    public void onNewToken(final String token) {
        super.onNewToken(token);
        sThreadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
                String encryptedToken = genericCipherUtil.encryptData(token);
                SkrSharedPrefs.putUserToken(
                        getApplicationContext(), encryptedToken);
                LogUtil.logDebug(
                        TAG, "Refreshed token:" + token + ", encrypted: " + encryptedToken);
            }
        });
    }
}
