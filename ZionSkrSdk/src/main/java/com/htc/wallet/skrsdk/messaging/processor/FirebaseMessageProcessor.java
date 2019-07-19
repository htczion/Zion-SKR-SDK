package com.htc.wallet.skrsdk.messaging.processor;

import static com.htc.wallet.skrsdk.messaging.MessageConstants.TYPE_BACKUP_DELETE;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.SettableFuture;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.applink.NetworkUtil;
import com.htc.wallet.skrsdk.crypto.util.GenericCipherUtil;
import com.htc.wallet.skrsdk.messaging.MessageConstants;
import com.htc.wallet.skrsdk.messaging.MessagingResult;
import com.htc.wallet.skrsdk.messaging.UserTokenListener;
import com.htc.wallet.skrsdk.messaging.message.Message;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;
import com.htc.wallet.skrsdk.util.ThreadUtil;

import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

public class FirebaseMessageProcessor implements UpstreamMessageProcessor {
    private static final String TAG = "FirebaseMessageProcessor";
    private static final int MAX_POOL_SIZE = 2;
    private static volatile ThreadPoolExecutor sThreadPoolExecutor =
            ThreadUtil.newFixedThreadPool(MAX_POOL_SIZE, "firebase-msg-proc");
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;

    FirebaseMessageProcessor() {
        mFirebaseDatabase =
                Objects.requireNonNull(FirebaseDatabase.getInstance(), "mFirebaseDatabase is null");
        mDatabaseReference =
                Objects.requireNonNull(
                        mFirebaseDatabase.getReference(MessageConstants.MESSAGE),
                        "mDatabaseReference is null");
    }

    @WorkerThread
    @Override
    public int sendMessage(@NonNull Context context, @NonNull final Message message) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(context, "message is null");

        if (!NetworkUtil.isNetworkConnected(context.getApplicationContext())) {
            LogUtil.logWarning(
                    TAG, "Message has been queued and it will be send after connect to network.");
        }

        if (!Message.isValid(message)) {
            LogUtil.logError(TAG, "sendMessage information can't be null");
            return MessagingResult.E_INVALID_MESSAGE;
        }

        final String key = mDatabaseReference.push().getKey();
        if (TextUtils.isEmpty(key)) {
            LogUtil.logError(TAG, "can't get database key");
            return MessagingResult.E_ILLEGAL_STATE;
        }
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
        message.setKey(key);
        final SettableFuture<Boolean> future = SettableFuture.create();

        mDatabaseReference
                .child(key)
                .setValue(
                        message,
                        new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(
                                    @Nullable final DatabaseError databaseError,
                                    @NonNull DatabaseReference databaseReference) {
                                if (databaseError == null) {
                                    future.set(true);
                                } else {
                                    LogUtil.logError(
                                            TAG,
                                            "Failed to setValue, error message: "
                                                    + databaseError.getMessage());
                                    future.set(false);
                                }
                            }
                        });

        try {
            if (future.get()) {
                LogUtil.logDebug(
                        TAG,
                        "Message sent: type= " + message.getMessageType()
                                + ", message= " + message.getMessage()
                                + ", key= " + message.getKey());
                return MessagingResult.SUCCESS;
            } else {
                return MessagingResult.E_FAILED_TO_SEND_MESSAGE;
            }
        } catch (Exception e) {
            LogUtil.logError(TAG, "sendMessage with exception: " + e);
            return MessagingResult.E_ILLEGAL_STATE;
        }
    }

    @Override
    public void getUserToken(
            @NonNull final Context context, @NonNull final UserTokenListener userTokenListener) {

        sThreadPoolExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        String token = getFcmTokenFromSp(context);
                        if (!TextUtils.isEmpty(token)) {
                            LogUtil.logDebug(TAG, "FCM token: " + token);
                            userTokenListener.onUserTokenReceived(token);
                            return;
                        }

                        FirebaseInstanceId.getInstance()
                                .getInstanceId()
                                .addOnSuccessListener(
                                        new OnSuccessListener<InstanceIdResult>() {
                                            @Override
                                            public void onSuccess(
                                                    InstanceIdResult instanceIdResult) {
                                                if (instanceIdResult == null) {
                                                    LogUtil.logError(
                                                            TAG, "InstanceIdResult is null");
                                                    return;
                                                }
                                                final String token = instanceIdResult.getToken();
                                                if (TextUtils.isEmpty(token)) {
                                                    LogUtil.logError(TAG, "token value is null");
                                                    return;
                                                }
                                                userTokenListener.onUserTokenReceived(token);
                                                sThreadPoolExecutor.execute(
                                                        new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                final GenericCipherUtil
                                                                        genericCipherUtil =
                                                                        new GenericCipherUtil();
                                                                String encryptedToken =
                                                                        genericCipherUtil
                                                                                .encryptData(token);
                                                                SkrSharedPrefs.putUserToken(
                                                                        context
                                                                                .getApplicationContext(),
                                                                        encryptedToken);
                                                                LogUtil.logDebug(
                                                                        TAG,
                                                                        "Token encrypted: "
                                                                                + encryptedToken);
                                                            }
                                                        });
                                                LogUtil.logDebug(TAG, "Token: " + token);
                                            }
                                        })
                                .addOnFailureListener(
                                        new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                userTokenListener.onUserTokenError(e);
                                            }
                                        })
                                .addOnCanceledListener(
                                        new OnCanceledListener() {
                                            @Override
                                            public void onCanceled() {
                                                userTokenListener.onUserTokenError(
                                                        new RuntimeException("get token cancel"));
                                            }
                                        });
                    }
                });
    }

    @WorkerThread
    public String getFcmTokenFromSp(@NonNull Context context) {
        Objects.requireNonNull(context);
        String token = SkrSharedPrefs.getUserToken(context.getApplicationContext());
        String decryptedToken = null;
        if (!TextUtils.isEmpty(token)) {
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
            decryptedToken = genericCipherUtil.decryptData(token);
        }
        LogUtil.logDebug(TAG, "FCM token get from SkrSharedPrefs:" + decryptedToken);
        return decryptedToken;
    }
}
