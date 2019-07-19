package com.htc.wallet.skrsdk.messaging.processor;


import static com.htc.wallet.skrsdk.messaging.MessageConstants.TYPE_BACKUP_DELETE;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.google.gson.GsonBuilder;
import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.crypto.util.GenericCipherUtil;
import com.htc.wallet.skrsdk.messaging.MessageServiceType;
import com.htc.wallet.skrsdk.messaging.MessagingResult;
import com.htc.wallet.skrsdk.messaging.UserTokenListener;
import com.htc.wallet.skrsdk.messaging.message.Message;
import com.htc.wallet.skrsdk.messaging.message.MultiSourceMessage;
import com.htc.wallet.skrsdk.messaging.message.WhisperMessage;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;
import com.htc.wallet.skrsdk.util.ThreadUtil;
import com.htc.wallet.skrsdk.whisper.WhisperKeyPair;
import com.htc.wallet.skrsdk.whisper.WhisperUtils;

import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

import me.pushy.sdk.Pushy;
import me.pushy.sdk.util.PushyAuthentication;
import me.pushy.sdk.util.exceptions.PushyRegistrationException;

public class PushyMessageProcessor implements UpstreamMessageProcessor {
    private static final String TAG = "PushyMessageProcessor";
    private static final Object sLock = new Object();

    private static final GenericCipherUtil sGenericCipherUtil = new GenericCipherUtil();

    private static final int HEX_STRING_PREFIX_LENGTH = 2;
    private static final int TOPIC_BYTE_STRING_LENGTH = 8;
    // For parsing the first 4 bytes of whisper public key with prefix "0x" as the topic. e.g.
    // 0xAABBCCDD...
    private static final int TOPIC_LENGTH = HEX_STRING_PREFIX_LENGTH + TOPIC_BYTE_STRING_LENGTH;

    private static final int MAX_POOL_SIZE = 5;
    private static volatile ThreadPoolExecutor sThreadPoolExecutor =
            ThreadUtil.newFixedThreadPool(MAX_POOL_SIZE, "pushy-msg-proc");

    @WorkerThread
    @Override
    public int sendMessage(@NonNull final Context context, @NonNull final Message message) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(context, "message is null");

        String targetWhisperPub = null;
        String targetPushyToken = null;

        @MessageServiceType
        int messageServiceType = ZionSkrSdkManager.getInstance().getMessageServiceType();
        if (messageServiceType == MessageServiceType.WHISPER) {
            final WhisperMessage whisperMessage = (WhisperMessage) message;
            targetWhisperPub = whisperMessage.getWhisperReceiver();
            targetPushyToken = whisperMessage.getPushyReceiver();
        } else if (messageServiceType == MessageServiceType.MULTI) {
            final MultiSourceMessage multiSourceMessage = (MultiSourceMessage) message;
            targetWhisperPub = multiSourceMessage.getWhisperReceiver();
            targetPushyToken = multiSourceMessage.getPushyReceiver();
        } else {
            LogUtil.logError(TAG, "Error messaging type=" + messageServiceType);
            return MessagingResult.E_INVALID_MESSAGING_TYPE;
        }

        if (TextUtils.isEmpty(targetWhisperPub)) {
            LogUtil.logError(TAG, "targetWhisperPub is empty");
            return MessagingResult.E_ILLEGAL_STATE;
        }
        if (TextUtils.isEmpty(targetPushyToken)) {
            LogUtil.logError(TAG, "targetPushyToken is empty");
            return MessagingResult.E_ILLEGAL_STATE;
        }

        WhisperKeyPair whisperKeyPair = WhisperUtils.getKeyPair(context);
        if (whisperKeyPair == null) {
            LogUtil.logError(TAG, "whisperKeyPair is null");
            return MessagingResult.E_ILLEGAL_STATE;
        }
        String keyPairId = whisperKeyPair.getKeyPairId();
        if (TextUtils.isEmpty(keyPairId)) {
            LogUtil.logError(TAG, "keyPairId is empty");
            return MessagingResult.E_ILLEGAL_STATE;
        }

        // For iOS device
        String senderName = SkrSharedPrefs.getSocialKMUserName(context);
        if (!TextUtils.isEmpty(senderName)) {
            String notificationTitle;
            if (message.getMessageType() == TYPE_BACKUP_DELETE) {
                notificationTitle =
                        String.format(
                                context.getString(
                                        R.string.ios_security_protection_notification_bar),
                                senderName);
            } else {
                notificationTitle =
                        String.format(
                                context.getString(R.string.ver_notification_request_title),
                                senderName);
            }
            message.setNotificationTitle(notificationTitle);
        }

        String whisperPub = whisperKeyPair.getPublicKey();
        if (TextUtils.isEmpty(whisperPub)) {
            LogUtil.logError(TAG, "whisperPub is empty");
            return MessagingResult.E_ILLEGAL_STATE;
        }

        String topic = whisperPub.substring(0, TOPIC_LENGTH);

        String postResult =
                WhisperUtils.shhPost(
                        targetWhisperPub,
                        targetPushyToken,
                        topic,
                        keyPairId,
                        new GsonBuilder().serializeNulls().create().toJson(message));
        if (TextUtils.isEmpty(postResult)) {
            LogUtil.logError(TAG, "Failed to post message!");
            return MessagingResult.E_FAILED_TO_SEND_MESSAGE;
        }
        LogUtil.logDebug(
                TAG,
                "Message sent: type= "
                        + message.getMessageType()
                        + ", message= "
                        + message.getMessage());

        return MessagingResult.SUCCESS;
    }

    @Override
    public void getUserToken(
            @NonNull final Context context, @NonNull final UserTokenListener userTokenListener) {

        sThreadPoolExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        String pushyToken = getPushyTokenFromSp(context);

                        if (!TextUtils.isEmpty(pushyToken)) {
                            Pushy.listen(context);
                        } else {
                            synchronized (sLock) {
                                try {
                                    pushyToken = getPushyTokenFromSp(context);
                                    if (TextUtils.isEmpty(pushyToken)) {
                                        if (!Pushy.isRegistered(context)) {
                                            pushyToken = Pushy.register(context);
                                        } else {
                                            Pushy.listen(context);
                                            // If the device has registered but not saved in
                                            // sharedPrefs, get pushy token from external storage
                                            pushyToken =
                                                    PushyAuthentication.getDeviceCredentials(
                                                            context)
                                                            .token;
                                        }

                                        if (TextUtils.isEmpty(pushyToken)) {
                                            userTokenListener.onUserTokenError(
                                                    new PushyRegistrationException(
                                                            "Pushy pushyToken is null"));
                                            return;
                                        }
                                        LogUtil.logDebug(TAG, "Pushy token: " + pushyToken);
                                        savePushyToken(context, pushyToken);
                                    }
                                } catch (Exception exception) {
                                    userTokenListener.onUserTokenError(exception);
                                    return;
                                }
                            }
                            // To check if the pushyToken saved successfully
                            if (TextUtils.isEmpty(getPushyTokenFromSp(context))) {
                                userTokenListener.onUserTokenError(
                                        new RuntimeException(
                                                "Pushy pushyToken not save to SkrSharedPrefs"));
                                return;
                            }
                        }
                        userTokenListener.onUserTokenReceived(pushyToken);
                    }
                });
    }

    @WorkerThread
    public String getPushyTokenFromSp(@NonNull Context context) {
        Objects.requireNonNull(context);
        String token = SkrSharedPrefs.getPushyToken(context);
        String decryptedToken = null;
        if (!TextUtils.isEmpty(token)) {
            synchronized (sLock) {
                decryptedToken = sGenericCipherUtil.decryptData(token);
            }
        }
        LogUtil.logDebug(TAG, "Pushy token get from SkrSharedPrefs: " + decryptedToken);
        return decryptedToken;
    }

    @WorkerThread
    private void savePushyToken(@NonNull Context context, @NonNull String deviceToken) {
        Objects.requireNonNull(context);
        if (TextUtils.isEmpty(deviceToken)) {
            throw new IllegalArgumentException("deviceToken is empty");
        }
        synchronized (sLock) {
            String encryptedToken = sGenericCipherUtil.encryptData(deviceToken);
            if (TextUtils.isEmpty(encryptedToken)) {
                LogUtil.logError(
                        TAG,
                        "Pushy token encryption failed",
                        new IllegalStateException("Pushy token encryption failed"));
                return;
            }
            SkrSharedPrefs.putPushyToken(context, encryptedToken);
        }
    }
}
