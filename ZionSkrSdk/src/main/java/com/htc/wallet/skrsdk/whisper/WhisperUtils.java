package com.htc.wallet.skrsdk.whisper;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.ArrayMap;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.htc.wallet.skrsdk.crypto.util.GenericCipherUtil;
import com.htc.wallet.skrsdk.messaging.message.Message;
import com.htc.wallet.skrsdk.messaging.message.MultiSourceMessage;
import com.htc.wallet.skrsdk.messaging.message.WhisperMessage;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;
import com.htc.wallet.skrsdk.util.ThreadUtil;
import com.htc.wallet.skrsdk.whisper.retrofit.WhisperManager;
import com.htc.wallet.skrsdk.whisper.retrofit.jsonbody.ShhMessageBody;
import com.htc.wallet.skrsdk.whisper.retrofit.jsonbody.ShhRequestBody;
import com.htc.wallet.skrsdk.whisper.retrofit.jsonbody.ShhResponseBody;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Response;

public final class WhisperUtils {
    private static final String TAG = "WhisperUtils";

    private static final int TOTAL_RETRIES = 3;
    private static final int PUSHY_TOKEN_LENGTH = 22;

    private static final long TIME_BEFORE_RETRY = 5 * DateUtils.SECOND_IN_MILLIS;

    private static final Object sLock = new Object();

    private static final int MAX_POOL_SIZE = 5;
    private static final ThreadPoolExecutor sThreadPoolExecutor =
            ThreadUtil.newFixedThreadPool(MAX_POOL_SIZE, "whisper-util");

    private static final GenericCipherUtil sGenericCipherUtil = new GenericCipherUtil();
    private static final LinkedHashMap<String, MsgPoller> sRunningPollers =
            new LinkedHashMap<String, MsgPoller>() {
                @Override
                protected boolean removeEldestEntry(final Entry eldest) {
                    return size() > MAX_POOL_SIZE;
                }
            };

    private WhisperUtils() {
    }

    @WorkerThread
    public static String shhVersion() {
        return (String) doShhRequestWithRetry("shh_version");
    }

    @WorkerThread
    public static Object shhInfo() {
        return doShhRequestWithRetry("shh_info");
    }

    @WorkerThread
    public static String shhNewKeyPair() {
        return (String) doShhRequestWithRetry("shh_newKeyPair");
    }

    @WorkerThread
    public static String shhGetPublicKey(String keyPairId) {
        if (TextUtils.isEmpty(keyPairId)) {
            throw new IllegalArgumentException("keyPairId is empty");
        }
        return (String) doShhRequestWithRetry("shh_getPublicKey", keyPairId);
    }

    @WorkerThread
    public static String shhGetPrivateKey(String keyPairId) {
        if (TextUtils.isEmpty(keyPairId)) {
            throw new IllegalArgumentException("keyPairId is empty");
        }
        return (String) doShhRequestWithRetry("shh_getPrivateKey", keyPairId);
    }

    @WorkerThread
    public static String shhAddPrivateKey(String prvKey) {
        if (TextUtils.isEmpty(prvKey)) {
            throw new IllegalArgumentException("prvKey is empty");
        }
        return (String) doShhRequestWithRetry("shh_addPrivateKey", prvKey);
    }

    @WorkerThread
    public static boolean shhDeleteKeyPair(String keyPairId) {
        if (TextUtils.isEmpty(keyPairId)) {
            throw new IllegalArgumentException("keyPairId is empty");
        }
        return (boolean) doShhRequestWithRetry("shh_deleteKeyPair", keyPairId);
    }

    @WorkerThread
    public static boolean shhHasKeyPair(String keyPairId) {
        if (TextUtils.isEmpty(keyPairId)) {
            throw new IllegalArgumentException("keyPairId is empty");
        }
        return (boolean) doShhRequestWithRetry("shh_hasKeyPair", keyPairId);
    }

    @WorkerThread
    public static String shhGenerateSymKeyFromPassword(String pass) {
        if (TextUtils.isEmpty(pass)) {
            throw new IllegalArgumentException("pass is empty");
        }
        return (String) doShhRequestWithRetry("shh_generateSymKeyFromPassword", pass);
    }

    @WorkerThread
    public static boolean shhDeleteSymKey(String symKeyId) {
        if (TextUtils.isEmpty(symKeyId)) {
            throw new IllegalArgumentException("symKeyId is empty");
        }
        return (boolean) doShhRequestWithRetry("shh_deleteSymKey", symKeyId);
    }

    @WorkerThread
    public static String shhNewMessageFilter(String keyPairId, @Nullable String topic) {
        if (TextUtils.isEmpty(keyPairId)) {
            throw new IllegalArgumentException("keyPairId is empty");
        }

        Map<String, Object> filterParams = new ArrayMap<>();
        filterParams.put("privateKeyID", keyPairId);
        if (!TextUtils.isEmpty(topic)) {
            List<String> topics = new ArrayList<>();
            topics.add(topic);
            filterParams.put("topics", topics);
        }
        filterParams.put("allowP2P", true);

        return (String) doShhRequestWithRetry("shh_newMessageFilter", filterParams);
    }

    @WorkerThread
    public static boolean shhDeleteMessageFilter(String msgFilterId) {
        if (TextUtils.isEmpty(msgFilterId)) {
            throw new IllegalArgumentException("msgFilterId is empty");
        }
        return (boolean) doShhRequestWithRetry("shh_deleteMessageFilter", msgFilterId);
    }

    @WorkerThread
    // one time polling
    public static List<ShhMessageBody.ResultObj> shhGetFilterMessages(final String msgFilterId) {
        if (TextUtils.isEmpty(msgFilterId)) {
            throw new IllegalArgumentException("msgFilterId is empty");
        }
        return doShhMsgRequestWithRetry(msgFilterId);
    }

    /**
     * Continuous polling. The {@link #unRegisterFilterMessages(String)} must be called before
     * leaving the current context.
     */
    public static void registerFilterMessages(
            final String msgFilterId, final WhisperListener whisperListener) {
        Objects.requireNonNull(whisperListener);
        if (TextUtils.isEmpty(msgFilterId)) {
            throw new IllegalArgumentException("msgFilterId is empty");
        }

        synchronized (sLock) {
            MsgPoller oldMsgPoller = sRunningPollers.get(msgFilterId);
            if (oldMsgPoller != null) {
                oldMsgPoller.cancel();
                sRunningPollers.remove(msgFilterId);
            }

            MsgPoller msgPoller = new MsgPoller(msgFilterId, whisperListener);
            sRunningPollers.put(msgFilterId, msgPoller);
            sThreadPoolExecutor.execute(msgPoller);
        }
    }

    public static void unRegisterFilterMessages(final String msgFilterId) {
        if (TextUtils.isEmpty(msgFilterId)) {
            throw new IllegalArgumentException("msgFilterId is empty");
        }

        synchronized (sLock) {
            MsgPoller msgPoller = sRunningPollers.get(msgFilterId);
            if (msgPoller != null) {
                msgPoller.cancel();
            }
        }
    }

    /**
     * @param mailServerPeer The mail server peer (e.g. "enode://...@127.0.0.1:30303").
     * @param symKeyID       Mail server's symmetric key id.
     */
    @WorkerThread
    public static String shhRequestMessage(
            String mailServerPeer, String symKeyID, @Nullable String topic, long from, long to) {
        if (TextUtils.isEmpty(mailServerPeer)) {
            throw new IllegalArgumentException("mailServerPeer is empty");
        }
        if (TextUtils.isEmpty(symKeyID)) {
            throw new IllegalArgumentException("symKeyID is empty");
        }
        Map<String, Object> requestHisParams = new ArrayMap<>();
        requestHisParams.put("mailServerPeer", mailServerPeer);
        requestHisParams.put("symKeyID", symKeyID);
        requestHisParams.put("force", true);
        if (!TextUtils.isEmpty(topic)) {
            List<String> topics = new ArrayList<>();
            topics.add(topic);
            requestHisParams.put("topics", topics);
        }
        if (from >= 0 && to >= 0) {
            requestHisParams.put("from", from);
            requestHisParams.put("to", to);
        }

        return (String) doShhRequestWithRetry("shhext_requestMessages", requestHisParams);
    }

    public static String shhRequestMessage(String mailServerPeer, String symKeyID) {
        return shhRequestMessage(mailServerPeer, symKeyID, null, -1, -1);
    }

    /** @param sig ID of the signing key, sender's keypair id */
    @WorkerThread
    public static String shhPost(
            String targetPublicKey,
            @Nullable String targetPushyToken,
            @Nullable String topic,
            String sig,
            String message) {
        if (TextUtils.isEmpty(targetPublicKey)) {
            throw new IllegalArgumentException("targetPublicKey is empty");
        }
        if (TextUtils.isEmpty(sig)) {
            throw new IllegalArgumentException("sig is empty");
        }
        if (TextUtils.isEmpty(message)) {
            throw new IllegalArgumentException("message is empty");
        }
        if (!TextUtils.isEmpty(targetPushyToken)
                && targetPushyToken.length() == PUSHY_TOKEN_LENGTH) {
            message = targetPushyToken + "," + message;
        } else {
            LogUtil.logWarning(TAG, "targetPushyToken may be empty or invalid");
        }

        String msgHex = StringConverter.encodeToHex(message);

        Map<String, Object> paramsPost = new ArrayMap<>();
        paramsPost.put("pubKey", targetPublicKey);
        paramsPost.put("sig", sig);
        // ttl: Time-to-live in seconds.
        paramsPost.put("ttl", 60);
        if (!TextUtils.isEmpty(topic)) {
            paramsPost.put("topic", topic);
        }
        // powTarget: Minimal PoW target required for this message.
        paramsPost.put("powTarget", 2.01);
        // powTarget: Maximal time in seconds to be spent on proof of work.
        paramsPost.put("powTime", 20);
        paramsPost.put("payload", msgHex);

        return (String) doShhRequestWithRetry("shh_post", paramsPost);
    }

    @WorkerThread
    public static String shhPost(String targetPublicKey, String sig, String message) {
        return shhPost(targetPublicKey, null, null, sig, message);
    }

    private static Object doShhRequestWithRetry(String method, @Nullable Object param) {
        if (TextUtils.isEmpty(method)) {
            throw new IllegalArgumentException("method is empty");
        }

        int retryCount = 0;
        Response<ShhResponseBody> response;
        Object err;
        Object result = new Object();

        do {
            try {
                if (param == null) {
                    response =
                            WhisperManager.getInstance()
                                    .getApiService()
                                    .shhMethod(new ShhRequestBody(method))
                                    .execute();
                } else {
                    response =
                            WhisperManager.getInstance()
                                    .getApiService()
                                    .shhMethod(new ShhRequestBody(method, param))
                                    .execute();
                }

                if (!response.isSuccessful()) {
                    throw new IllegalStateException("Failed response, code=" + response.code());
                }

                ShhResponseBody responseBody = response.body();
                if (responseBody == null) {
                    throw new IllegalStateException("responseBody is null");
                }

                err = responseBody.getError();
                if (err != null) {
                    throw new IllegalStateException(err.toString());
                }

                result = responseBody.getResult();
                break;
            } catch (Exception e) {
                LogUtil.logWarning(TAG, method + " Exception: " + e);
                LogUtil.logVerbose(
                        TAG, "Retrying... (" + ++retryCount + " out of " + TOTAL_RETRIES + ")");
                if (retryCount == TOTAL_RETRIES) {
                    // For fallback design, we don't throw an exception if exceeding the retry
                    // times.
                    return null;
                }
                ThreadUtil.sleep(TIME_BEFORE_RETRY);
            }
        } while (retryCount < TOTAL_RETRIES);

        return result;
    }

    private static Object doShhRequestWithRetry(String method) {
        return doShhRequestWithRetry(method, null);
    }

    private static List<ShhMessageBody.ResultObj> doShhMsgRequestWithRetry(Object param) {
        Objects.requireNonNull(param, "param is null");

        int retryCount = 0;
        Response<ShhMessageBody> msgResponse;
        Object err;
        List<ShhMessageBody.ResultObj> result = new ArrayList<>();

        do {
            try {
                msgResponse =
                        WhisperManager.getInstance()
                                .getApiService()
                                .shhGetFilterMessages(
                                        new ShhRequestBody("shh_getFilterMessages", param))
                                .execute();

                if (!msgResponse.isSuccessful()) {
                    throw new IllegalStateException("Failed response, code=" + msgResponse.code());
                }

                ShhMessageBody responseBody = msgResponse.body();
                if (responseBody == null) {
                    throw new IllegalStateException("responseBody is null");
                }

                err = responseBody.getError();
                if (err != null) {
                    throw new IllegalStateException(err.toString());
                }

                result = responseBody.getResults();
                break;
            } catch (Exception e) {
                LogUtil.logWarning(TAG, "shh_getFilterMessages" + " Exception: " + e);
                LogUtil.logVerbose(
                        TAG, "Retrying... (" + ++retryCount + " out of " + TOTAL_RETRIES + ")");
                if (retryCount == TOTAL_RETRIES) {
                    // For fallback design, we don't throw an exception if exceeding the retry
                    // times.
                    return null;
                }
                ThreadUtil.sleep(TIME_BEFORE_RETRY);
            }
        } while (retryCount < TOTAL_RETRIES);

        return result;
    }

    @Nullable
    public static <T extends Message> T fromJson(
            @NonNull String json, @NonNull final Class<T> cls) {
        Type type = null;

        if (cls == WhisperMessage.class) {
            type = new TypeToken<WhisperMessage>() {
            }.getType();
        } else if (cls == MultiSourceMessage.class) {
            type = new TypeToken<MultiSourceMessage>() {
            }.getType();
        }

        if (type == null) {
            LogUtil.logError(TAG, "type is null");
            return null;
        }

        return new GsonBuilder().serializeNulls().create().fromJson(json, type);
    }

    @WorkerThread
    @Nullable
    public static WhisperKeyPair getKeyPair(@NonNull Context context) {
        String whisperKeyPairId;
        String whisperPrvKey = getWhisperPrvKeyFromSharedPrefs(context);
        String whisperPubKey;

        try {
            if (TextUtils.isEmpty(whisperPrvKey)) {
                whisperKeyPairId = shhNewKeyPair();
                whisperPrvKey = shhGetPrivateKey(whisperKeyPairId);
                if (TextUtils.isEmpty(whisperPrvKey)) {
                    LogUtil.logError(TAG, "whisperPrvKey is empty");
                    return null;
                }
                saveWhisperPrvKeyFromSharedPrefs(context, whisperPrvKey);
                whisperPubKey = shhGetPublicKey(whisperKeyPairId);
            } else {
                whisperKeyPairId = shhAddPrivateKey(whisperPrvKey);
                whisperPubKey = shhGetPublicKey(whisperKeyPairId);
            }

            if (TextUtils.isEmpty(whisperKeyPairId)) {
                LogUtil.logError(TAG, "whisperKeyPairId is empty");
                return null;
            }
            if (TextUtils.isEmpty(whisperPubKey)) {
                LogUtil.logError(TAG, "whisperPubKey is empty");
                return null;
            }
        } catch (Exception e) {
            LogUtil.logWarning(TAG, "getKeyPair Exception e=" + e);
            return null;
        }

        LogUtil.logDebug(
                TAG,
                "whisperKeyPairId="
                        + whisperKeyPairId
                        + ", whisperPrvKey="
                        + whisperPrvKey
                        + ", whisperPubKey="
                        + whisperPubKey);

        return new WhisperKeyPair(whisperKeyPairId, whisperPrvKey, whisperPubKey);
    }

    private static String getWhisperPrvKeyFromSharedPrefs(@NonNull Context context) {
        Objects.requireNonNull(context);
        String prvKey = SkrSharedPrefs.getWhisperPrvKey(context);
        String decryptedPrvKey = null;
        if (!TextUtils.isEmpty(prvKey)) {
            synchronized (sLock) {
                decryptedPrvKey = sGenericCipherUtil.decryptData(prvKey);
            }
        }
        LogUtil.logDebug(TAG, "Whisper prv key get from SkrSharedPrefs: " + decryptedPrvKey);
        return decryptedPrvKey;
    }

    private static void saveWhisperPrvKeyFromSharedPrefs(
            @NonNull Context context, @NonNull String whisperPrvKey) {
        Objects.requireNonNull(context);
        if (TextUtils.isEmpty(whisperPrvKey)) {
            throw new IllegalArgumentException("whisperPrvKey is empty");
        }
        synchronized (sLock) {
            String encryptedPrvKey = sGenericCipherUtil.encryptData(whisperPrvKey);
            if (TextUtils.isEmpty(encryptedPrvKey)) {
                LogUtil.logError(TAG, "Whisper prv key encryption failed");
                return;
            }
            SkrSharedPrefs.putWhisperPrvKey(context, encryptedPrvKey);
        }
    }

    public interface WhisperListener {
        void onMessageReceived(String message, long timeStamp);
    }

    private static class MsgPoller implements Runnable {
        private final AtomicBoolean mCancel = new AtomicBoolean(false);
        private final String mMsgFilterId;
        private final WhisperListener mWhisperListener;

        MsgPoller(@NonNull String msgFilterId, @NonNull WhisperListener whisperListener) {
            mMsgFilterId = msgFilterId;
            mWhisperListener = whisperListener;
        }

        @Override
        public void run() {
            List<ShhMessageBody.ResultObj> msgList;
            while (!mCancel.get()) {
                msgList = shhGetFilterMessages(mMsgFilterId);

                for (int i = 0; i < msgList.size(); i++) {
                    final ShhMessageBody.ResultObj messageBody = msgList.get(i);
                    String receivedHexMsg = messageBody.getPayload();
                    String receivedMsg = StringConverter.decodeFromHex(receivedHexMsg);
                    long timeStamp = messageBody.getTimeStamp();
                    mWhisperListener.onMessageReceived(receivedMsg, timeStamp);
                }

                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    LogUtil.logWarning(TAG, "registerFilterMessages Exception: " + e);
                }
            }
            LogUtil.logDebug(TAG, "FilterMessages unregistered.");
        }

        public void cancel() {
            mCancel.set(true);
            sRunningPollers.remove(mMsgFilterId);
        }
    }
}
