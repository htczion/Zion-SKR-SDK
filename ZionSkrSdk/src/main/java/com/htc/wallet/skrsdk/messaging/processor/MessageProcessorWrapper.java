package com.htc.wallet.skrsdk.messaging.processor;

import static com.htc.wallet.skrsdk.messaging.processor.MessageProcessorFactory.FIREBASE_MESSAGE_PROCESSOR;
import static com.htc.wallet.skrsdk.messaging.processor.MessageProcessorFactory.PUSHY_MESSAGE_PROCESSOR;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.action.Action;
import com.htc.wallet.skrsdk.messaging.FirebaseException;
import com.htc.wallet.skrsdk.messaging.MessageConstants;
import com.htc.wallet.skrsdk.messaging.MessageServiceType;
import com.htc.wallet.skrsdk.messaging.MessagingResult;
import com.htc.wallet.skrsdk.messaging.MultiTokenListener;
import com.htc.wallet.skrsdk.messaging.PushyException;
import com.htc.wallet.skrsdk.messaging.UserTokenListener;
import com.htc.wallet.skrsdk.messaging.message.Message;
import com.htc.wallet.skrsdk.messaging.message.MultiSourceMessage;
import com.htc.wallet.skrsdk.messaging.message.WhisperMessage;
import com.htc.wallet.skrsdk.util.JsonUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.ThreadUtil;
import com.htc.wallet.skrsdk.whisper.WhisperException;
import com.htc.wallet.skrsdk.whisper.WhisperKeyPair;
import com.htc.wallet.skrsdk.whisper.WhisperUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MessageProcessorWrapper {
    private static final String TAG = "MessageProcessorWrapper";
    private static final long TIMEOUT = 60L;
    private static final int LENGTH_OF_BYTE = 1024;
    // 4KB
    private static final int FCM_MESSAGE_SIZE_LIMIT = 4 * LENGTH_OF_BYTE;

    private static final int MAX_POOL_SIZE = 5;
    private static volatile ThreadPoolExecutor sThreadPoolExecutor =
            ThreadUtil.newFixedThreadPool(MAX_POOL_SIZE, "msg-proc");

    @WorkerThread
    public static void sendMessage(
            @NonNull final Context context,
            @Nullable final String receiverFcmToken,
            @Nullable final String receiverWhisperPub,
            @Nullable final String receiverPushyToken,
            final int messageType,
            @NonNull final Map<String, String> map) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(map, "map is null");

        final String json = JsonUtil.mapToJson(map);
        if (TextUtils.isEmpty(json)) {
            LogUtil.logError(TAG, "json is empty", new IllegalStateException("json is empty"));
            return;
        }

        final int sizeOfJson = json.getBytes().length;

        getMultiToken(context, new MultiTokenListener() {
            @Override
            public void onUserTokenReceived(@Nullable String pushyToken,
                    @Nullable String fcmToken) {
                WhisperKeyPair whisperKeyPair = null;
                String myWhisperPub = null;
                FirebaseMessageProcessor fcmProcessor = null;
                PushyMessageProcessor pushyProcessor = null;
                int sentResult;

                @MessageServiceType
                int messageServiceType = ZionSkrSdkManager.getInstance().getMessageServiceType();
                switch (messageServiceType) {
                    case MessageServiceType.FIREBASE:
                        if (TextUtils.isEmpty(fcmToken)) {
                            LogUtil.logError(TAG, "fcmToken is empty",
                                    new IllegalStateException("fcmToken is empty"));
                            return;
                        }

                        if (TextUtils.isEmpty(receiverFcmToken)) {
                            LogUtil.logError(TAG, "receiverFcmToken is empty",
                                    new IllegalStateException("receiverFcmToken is empty"));
                            return;
                        }

                        if (sizeOfJson > FCM_MESSAGE_SIZE_LIMIT) {
                            String errMsg = String.format(Locale.getDefault(),
                                    "Message size %.02fKB is larger than the limit of FCM (4KB).",
                                    (float) sizeOfJson / LENGTH_OF_BYTE);
                            LogUtil.logError(TAG, errMsg, new IllegalStateException(errMsg));
                            return;
                        }

                        Message fcmMsg = new Message(fcmToken, receiverFcmToken, messageType, json);

                        fcmProcessor =
                                (FirebaseMessageProcessor) MessageProcessorFactory.getInstance()
                                        .getMessageProcessor(FIREBASE_MESSAGE_PROCESSOR);
                        if (fcmProcessor == null) {
                            LogUtil.logError(TAG, "fcmProcessor is null",
                                    new IllegalStateException("fcmProcessor is null"));
                            return;
                        }
                        sentResult = fcmProcessor.sendMessage(context, fcmMsg);

                        if (sentResult != MessagingResult.SUCCESS) {
                            LogUtil.logError(TAG,
                                    "Failed to send message via Firebase, error code=" + sentResult,
                                    new FirebaseException(
                                            "Failed to send message via Firebase, error code="
                                                    + sentResult));
                        }
                        break;
                    case MessageServiceType.WHISPER:
                        if (TextUtils.isEmpty(pushyToken)) {
                            LogUtil.logError(TAG, "pushyToken is empty",
                                    new PushyException("pushyToken is empty"));
                            return;
                        }

                        if (TextUtils.isEmpty(receiverWhisperPub) || TextUtils.isEmpty(
                                receiverPushyToken)) {
                            LogUtil.logError(TAG,
                                    "receiverWhisperPub or receiverPushyToken is empty",
                                    new WhisperException(
                                            "receiverWhisperPub or receiverPushyToken is empty"));
                            return;
                        }
                        whisperKeyPair = WhisperUtils.getKeyPair(context);
                        if (whisperKeyPair == null) {
                            LogUtil.logError(TAG, "whisperKeyPair is null",
                                    new WhisperException("whisperKeyPair is null"));
                            return;
                        }
                        myWhisperPub = whisperKeyPair.getPublicKey();
                        if (TextUtils.isEmpty(myWhisperPub)) {
                            LogUtil.logError(TAG, "myWhisperPub is empty",
                                    new WhisperException("myWhisperPub is empty"));
                            return;
                        }

                        WhisperMessage whisperMsg =
                                new WhisperMessage(myWhisperPub, receiverWhisperPub, pushyToken,
                                        receiverPushyToken, messageType, json);

                        pushyProcessor =
                                (PushyMessageProcessor) MessageProcessorFactory.getInstance()
                                        .getMessageProcessor(PUSHY_MESSAGE_PROCESSOR);
                        if (pushyProcessor == null) {
                            LogUtil.logError(TAG, "processor is null",
                                    new PushyException("processor is null"));
                            return;
                        }
                        sentResult = pushyProcessor.sendMessage(context, whisperMsg);
                        if (sentResult != MessagingResult.SUCCESS) {
                            LogUtil.logError(TAG,
                                    "Failed to send message via Whisper, error code=" + sentResult,
                                    new PushyException(
                                            "Failed to send message via Whisper, error code="
                                                    + sentResult));
                        }
                        break;
                    case MessageServiceType.MULTI:
                        // Try whisper first. If failed, then try fcm
                        try {
                            if (TextUtils.isEmpty(pushyToken)) {
                                throw new PushyException("WhisperException, pushyToken is empty");
                            }

                            if (TextUtils.isEmpty(receiverWhisperPub) || TextUtils.isEmpty(
                                    receiverPushyToken)) {
                                throw new WhisperException(
                                        "WhisperException, receiverWhisperPub or "
                                                + "receiverPushyToken is null");
                            }
                            whisperKeyPair = WhisperUtils.getKeyPair(context);
                            if (whisperKeyPair == null) {
                                throw new WhisperException("whisperKeyPair is null");
                            }
                            myWhisperPub = whisperKeyPair.getPublicKey();
                            if (TextUtils.isEmpty(myWhisperPub)) {
                                throw new WhisperException("myWhisperPub is empty");
                            }

                            MultiSourceMessage multiSourceMessage = new MultiSourceMessage(
                                    myWhisperPub, receiverWhisperPub, pushyToken,
                                    receiverPushyToken, fcmToken, receiverFcmToken, messageType,
                                    json);

                            pushyProcessor =
                                    (PushyMessageProcessor) MessageProcessorFactory.getInstance()
                                            .getMessageProcessor(PUSHY_MESSAGE_PROCESSOR);
                            if (pushyProcessor == null) {
                                throw new PushyException("pushyProcessor is null");
                            }
                            sentResult = pushyProcessor.sendMessage(context, multiSourceMessage);
                            if (sentResult != MessagingResult.SUCCESS) {
                                throw new PushyException(
                                        "Failed to send message via Whisper, error code="
                                                + sentResult);
                            }
                        } catch (Exception e) {
                            LogUtil.logError(TAG, "Send message error, e=" + e);
                            LogUtil.logInfo(TAG, "Fallback to FCM...");
                            if (TextUtils.isEmpty(fcmToken)) {
                                LogUtil.logWarning(TAG, "fcmToken is empty, cannot send via FCM");
                                return;
                            }
                            if (TextUtils.isEmpty(receiverFcmToken)) {
                                LogUtil.logWarning(TAG,
                                        "receiverFcmToken is empty, cannot send via FCM");
                                return;
                            }
                            fcmProcessor =
                                    (FirebaseMessageProcessor) MessageProcessorFactory.getInstance()
                                            .getMessageProcessor(FIREBASE_MESSAGE_PROCESSOR);
                            if (fcmProcessor == null) {
                                LogUtil.logError(TAG, "fcmProcessor is null",
                                        new FirebaseException("fcmProcessor is null"));
                                return;
                            }

                            // Use the original Message object here because the risk may be
                            // too high if we modify FCM server.
                            Message message;
                            if (sizeOfJson > FCM_MESSAGE_SIZE_LIMIT) {
                                if (messageType == MessageConstants.TYPE_BACKUP_VERIFY) {
                                    LogUtil.logDebug(TAG,
                                            "Remove KEY_ENCRYPTED_WHISPER_PUB and "
                                                    + "KEY_ENCRYPTED_PUSHY_TOKEN from message to "
                                                    + "avoid "
                                                    + "exceeding the size limit of FCM");
                                    map.remove(Action.KEY_ENCRYPTED_WHISPER_PUB);
                                    map.remove(Action.KEY_ENCRYPTED_PUSHY_TOKEN);
                                    String newJson = JsonUtil.mapToJson(map);
                                    message = new Message(fcmToken, receiverFcmToken, messageType,
                                            newJson);
                                } else {
                                    String errMsg = String.format(Locale.getDefault(),
                                            "Message size %.02fKB is larger than the limit of FCM"
                                                    + " (4KB).",
                                            (float) sizeOfJson / LENGTH_OF_BYTE);
                                    LogUtil.logError(TAG, errMsg,
                                            new IllegalStateException(errMsg));
                                    return;
                                }
                            } else {
                                message = new Message(fcmToken, receiverFcmToken, messageType,
                                        json);
                            }

                            sentResult = fcmProcessor.sendMessage(context, message);
                            if (sentResult != MessagingResult.SUCCESS) {
                                LogUtil.logError(TAG,
                                        "Failed to send message via Firebase, error code="
                                                + sentResult);
                            }
                        }
                        break;
                    default:
                        LogUtil.logError(TAG, "Invalid messaging type=" + messageServiceType);
                        break;
                }
            }

            @Override
            public void onUserTokenError(Exception exception) {
                LogUtil.logError(TAG, "getMultiToken with exception: " + exception);
            }
        });
    }

    @WorkerThread
    public static void getMultiToken(@NonNull final Context context,
            @NonNull final MultiTokenListener multiTokenListener) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(multiTokenListener, "multiTokenListener is null");

        final CountDownLatch latch = new CountDownLatch(2);

        sThreadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final String[] myPushyToken = {null};
                final String[] myFcmToken = {null};

                @MessageServiceType
                int messageServiceType = ZionSkrSdkManager.getInstance().getMessageServiceType();
                if (messageServiceType == MessageServiceType.WHISPER
                        || messageServiceType == MessageServiceType.MULTI) {
                    final PushyMessageProcessor pushyProcessor =
                            (PushyMessageProcessor) MessageProcessorFactory.getInstance().getMessageProcessor(
                                    PUSHY_MESSAGE_PROCESSOR);
                    if (pushyProcessor == null) {
                        multiTokenListener.onUserTokenError(
                                new IllegalStateException(
                                        "PushyRegistrationException, pushyProcessor is null"));
                        return;
                    }

                    // Getting pushy token
                    pushyProcessor.getUserToken(context, new UserTokenListener() {
                        @Override
                        public void onUserTokenReceived(@NonNull final String pushyToken) {
                            if (TextUtils.isEmpty(pushyToken)) {
                                multiTokenListener.onUserTokenError(
                                        new IllegalStateException(
                                                "PushyRegistrationException, pushyToken is empty"));
                                latch.countDown();
                                return;
                            }
                            myPushyToken[0] = pushyToken;
                            latch.countDown();
                        }

                        @Override
                        public void onUserTokenError(Exception exception) {
                            multiTokenListener.onUserTokenError(exception);
                            latch.countDown();
                        }
                    });
                } else {
                    latch.countDown();
                }

                if (messageServiceType == MessageServiceType.FIREBASE
                        || messageServiceType == MessageServiceType.MULTI) {
                    final FirebaseMessageProcessor firebaseProcessor =
                            (FirebaseMessageProcessor) MessageProcessorFactory.getInstance().getMessageProcessor(
                                    FIREBASE_MESSAGE_PROCESSOR);
                    if (firebaseProcessor == null) {
                        multiTokenListener.onUserTokenError(
                                new IllegalStateException(
                                        "FirebaseAuthException, firebaseProcessor is null"));
                        latch.countDown();
                        return;
                    }

                    // Getting fcm token
                    firebaseProcessor.getUserToken(context, new UserTokenListener() {
                        @Override
                        public void onUserTokenReceived(@NonNull String token) {
                            if (TextUtils.isEmpty(token)) {
                                multiTokenListener.onUserTokenError(new IllegalStateException(
                                        "FirebaseAuthException, token is empty"));
                                latch.countDown();
                                return;
                            }
                            myFcmToken[0] = token;
                            latch.countDown();
                        }

                        @Override
                        public void onUserTokenError(Exception exception) {
                            multiTokenListener.onUserTokenError(exception);
                            latch.countDown();
                        }
                    });
                } else {
                    latch.countDown();
                }

                try {
                    latch.await(TIMEOUT, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    LogUtil.logError(TAG, "InterruptedException e=" + e);
                }
                multiTokenListener.onUserTokenReceived(myPushyToken[0], myFcmToken[0]);
            }
        });
    }
}
