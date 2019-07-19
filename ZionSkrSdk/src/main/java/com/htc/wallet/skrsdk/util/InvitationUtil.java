package com.htc.wallet.skrsdk.util;

import static com.htc.wallet.skrsdk.messaging.processor.MessageProcessorFactory.PUSHY_MESSAGE_PROCESSOR;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.widget.Toast;

import com.htc.wallet.skrsdk.BuildConfig;
import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.applink.AppLink;
import com.htc.wallet.skrsdk.applink.AppLinkConstant;
import com.htc.wallet.skrsdk.applink.BranchAppLink;
import com.htc.wallet.skrsdk.applink.FirebaseAppLink;
import com.htc.wallet.skrsdk.applink.ShortLinkFailureListener;
import com.htc.wallet.skrsdk.applink.ShortLinkTaskListener;
import com.htc.wallet.skrsdk.applink.crypto.AppLinkParamsCryptoUtil;
import com.htc.wallet.skrsdk.crypto.util.GenericCipherUtil;
import com.htc.wallet.skrsdk.crypto.util.VerificationUtil;
import com.htc.wallet.skrsdk.messaging.MessageServiceType;
import com.htc.wallet.skrsdk.messaging.MultiTokenListener;
import com.htc.wallet.skrsdk.messaging.UserTokenListener;
import com.htc.wallet.skrsdk.messaging.processor.FirebaseMessageProcessor;
import com.htc.wallet.skrsdk.messaging.processor.MessageProcessorFactory;
import com.htc.wallet.skrsdk.messaging.processor.MessageProcessorWrapper;
import com.htc.wallet.skrsdk.messaging.processor.PushyMessageProcessor;
import com.htc.wallet.skrsdk.whisper.WhisperKeyPair;
import com.htc.wallet.skrsdk.whisper.WhisperUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

public class InvitationUtil {
    private static final String TAG = "InvitationUtil";
    private static final int APPLINK_TIME_OUT = 20000;
    private static final int APPLINK_TIME_OUT_INTERVAL = 1000;

    private static final String[] NOT_SUPPORT_DEEPLINK_APPS = {
            "skype",
            "com.android.nfc",
            "com.google.android.apps.docs",
            "com.android.bluetooth",
            "com.google.android.apps.plus",
            "com.google.android.keep",
            "com.instagram",
            "com.tencent",
            "gogolook.callgogolook2",
            "org.mozilla.focus",
            "org.mozilla.firefox",
            "com.duckduckgo.mobile.android",
            "com.snapchat.android",
            "com.sina.weibo",
            "com.weico.international",
            "com.facebook.katana",
            "com.qwant.liberty"
    };
    private static final String[] NOT_SUPPORT_DEEPLINK_ACTIVITES = {
            "com.linecorp.linekeep.ui.KeepSaveActivity", "com.twitter.composer.ComposerActivity"
    };
    private static final int MAX_POOL_SIZE = 1;
    private static final ThreadPoolExecutor sThreadPoolExecutor =
            ThreadUtil.newFixedThreadPool(MAX_POOL_SIZE, "invite-util");
    private final Uri mGooglePlayLink;
    private final String mUserName;
    private final String mAddress;
    private final Activity mActivity;
    private final AppLinkParamsCryptoUtil mParamsCryptoUtil;
    private boolean mRestoreMode = false;
    private ProgressDialogUtil mProgressDialogUtil;
    private ProgressDialog mProgressDialog;
    private CountDownTimer mCountDownTimer;
    private String mGoogleDriveDeviceId;
    // For resending link
    private String mBackupTargetName = null;
    // For resending link, real Bad
    private String mBackupTargetUUIDHash = null;

    public InvitationUtil(
            @NonNull final Activity activity,
            @NonNull final String userName,
            @NonNull final String address) {
        if (TextUtils.isEmpty(userName)) {
            throw new IllegalArgumentException("userName is null or empty");
        }
        if (TextUtils.isEmpty(address)) {
            throw new IllegalArgumentException("address is null or empty");
        }

        mActivity = Objects.requireNonNull(activity);
        mGooglePlayLink = Uri.parse(
                mActivity.getResources().getString(R.string.applink_google_play_link));
        mUserName = userName;
        mAddress = address;
        mParamsCryptoUtil = new AppLinkParamsCryptoUtil(activity);
        setupProgressDialog();
    }

    public void changeToRestoreMode() {
        mRestoreMode = true;
    }

    private void setupSharingInvitation(final String link) {
        if (TextUtils.isEmpty(link)) {
            throw new IllegalArgumentException("setupSharingInvitation, link is empty");
        }
        if (mActivity != null && !mActivity.isFinishing()) {
            mProgressDialogUtil.dismiss(mProgressDialog);
        }
        final String message = buildMessage(Uri.parse(link));
        Objects.requireNonNull(mActivity).startActivity(onShareClick(message));
    }

    private void setupProgressDialog() {
        String dialogTitle = mActivity.getResources().getString(
                R.string.backup_choose_applink_dialog_title);
        String dialogContent = mActivity.getResources().getString(
                R.string.backup_choose_applink_dialog_content);
        mProgressDialogUtil = new ProgressDialogUtil(mActivity, dialogTitle, dialogContent);
    }

    public void sharingInvitation() {
        mProgressDialog = mProgressDialogUtil.show();
        startCountdownTimer();

        switch (ZionSkrSdkManager.getInstance().getMessageServiceType()) {
            case MessageServiceType.FIREBASE:
                final FirebaseMessageProcessor firebaseMessageProcessor =
                        (FirebaseMessageProcessor) MessageProcessorFactory.getInstance()
                                .getMessageProcessor(
                                        MessageProcessorFactory.FIREBASE_MESSAGE_PROCESSOR);
                if (firebaseMessageProcessor == null) {
                    LogUtil.logError(TAG, "firebaseMessageProcessor is null",
                            new IllegalStateException("firebaseMessageProcessor is null"));
                    return;
                }
                firebaseMessageProcessor.getUserToken(mActivity, new UserTokenListener() {
                    @Override
                    public void onUserTokenReceived(@NonNull final String token) {
                        if (TextUtils.isEmpty(token)) {
                            LogUtil.logError(TAG, "pushyToken is empty",
                                    new IllegalStateException("pushyToken is empty"));
                            return;
                        }
                        sThreadPoolExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                setupLinkTask(null, null, token);
                            }
                        });
                    }

                    @Override
                    public void onUserTokenError(Exception exception) {
                        LogUtil.logError(TAG, "getUserToken with exception = " + exception);
                    }
                });
                break;
            case MessageServiceType.WHISPER:
                final PushyMessageProcessor processor =
                        (PushyMessageProcessor) MessageProcessorFactory.getInstance()
                                .getMessageProcessor(PUSHY_MESSAGE_PROCESSOR);
                if (processor == null) {
                    LogUtil.logError(TAG, "processor is null",
                            new IllegalStateException("processor is null"));
                    return;
                }
                processor.getUserToken(mActivity, new UserTokenListener() {
                    @Override
                    public void onUserTokenReceived(@NonNull final String pushyToken) {
                        if (TextUtils.isEmpty(pushyToken)) {
                            LogUtil.logError(TAG, "pushyToken is empty",
                                    new IllegalStateException("pushyToken is empty"));
                            return;
                        }
                        sThreadPoolExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                WhisperKeyPair whisperKeyPair = WhisperUtils.getKeyPair(
                                        mActivity.getBaseContext());
                                if (whisperKeyPair == null) {
                                    LogUtil.logError(TAG, "whisperKeyPair is null",
                                            new IllegalStateException("whisperKeyPair is null"));
                                    return;
                                }
                                final String whisperPub = whisperKeyPair.getPublicKey();
                                if (TextUtils.isEmpty(whisperPub)) {
                                    LogUtil.logError(TAG, "whisperPub is empty",
                                            new IllegalStateException("whisperPub is empty"));
                                    return;
                                }
                                setupLinkTask(whisperPub, pushyToken, null);
                            }
                        });
                    }

                    @Override
                    public void onUserTokenError(Exception exception) {
                        LogUtil.logError(TAG, "getUserToken with exception=" + exception);
                    }
                });
                break;
            case MessageServiceType.MULTI:
                MessageProcessorWrapper.getMultiToken(mActivity.getBaseContext(),
                        new MultiTokenListener() {
                            @Override
                            public void onUserTokenReceived(@Nullable final String pushyToken,
                                    @Nullable final String fcmToken) {
                                if (TextUtils.isEmpty(pushyToken)) {
                                    LogUtil.logError(TAG, "pushyToken is empty",
                                            new IllegalStateException("pushyToken is empty"));
                                    return;
                                }

                                if (TextUtils.isEmpty(fcmToken)) {
                                    LogUtil.logError(TAG, "fcmToken is empty",
                                            new IllegalStateException("fcmToken is empty"));
                                    return;
                                }

                                sThreadPoolExecutor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        String whisperPub = null;
                                        WhisperKeyPair whisperKeyPair = WhisperUtils.getKeyPair(
                                                mActivity.getBaseContext());
                                        if (whisperKeyPair != null) {
                                            whisperPub = whisperKeyPair.getPublicKey();
                                            if (TextUtils.isEmpty(whisperPub)) {
                                                LogUtil.logError(TAG, "whisperPub is empty");
                                            }
                                        } else {
                                            LogUtil.logWarning(TAG, "whisperKeyPair is null");
                                        }

                                        setupLinkTask(whisperPub, pushyToken, fcmToken);
                                    }
                                });
                            }

                            @Override
                            public void onUserTokenError(Exception exception) {
                                LogUtil.logError(TAG, "getMultiToken with exception=" + exception);
                            }
                        });
                break;
            default:
                LogUtil.logError(TAG, "Invalid messaging type");
                break;
        }
    }

    // For resending link
    public void setBackupTargetName(String backupTargetName) {
        mBackupTargetName = backupTargetName;
    }

    // For resending link, real Bad
    public void setBackupTargetUUIDHash(String backupTargetUUIDHash) {
        mBackupTargetUUIDHash = backupTargetUUIDHash;
    }

    public void clearBackupTargetName() {
        mBackupTargetName = null;
    }

    @WorkerThread
    private void setupLinkTask(@Nullable final String whisperPub, @Nullable final String pushyToken,
            @Nullable final String fcmToken) {
        if (TextUtils.isEmpty(whisperPub) && TextUtils.isEmpty(pushyToken) && TextUtils.isEmpty(
                fcmToken)) {
            throw new IllegalArgumentException(
                    "whisperPub, pushyToken and fcmToken are all empty!");
        }
        final VerificationUtil util = new VerificationUtil(false);
        final String publicKeyString = util.getPublicKeyString();

        // Shared Link with Hashed UUID
        final String deviceId = PhoneUtil.getSKRIDHash(mActivity);
        final String tzIdHash = WalletSdkUtil.getTZIDHash(mActivity);
        if (TextUtils.isEmpty(tzIdHash)) {
            cancelCountdownTimer();
            mProgressDialogUtil.dismiss(mProgressDialog);
            if (BuildConfig.DEBUG) {
                mActivity.runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mActivity, "No TZIDHash", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
            return;
        }
        final AppLink.ParamsBuilder builder = new AppLink.ParamsBuilder();
        builder.publicKey(mParamsCryptoUtil.encrypt(publicKeyString));

        if (!TextUtils.isEmpty(fcmToken)) {
            builder.token(mParamsCryptoUtil.encrypt(fcmToken));
        }
        if (!TextUtils.isEmpty(whisperPub) && !TextUtils.isEmpty(pushyToken)) {
            builder.whisperPub(mParamsCryptoUtil.encrypt(whisperPub));
            builder.pushyToken(mParamsCryptoUtil.encrypt(pushyToken));
        }
        String addressSignature = util.getChecksum(mAddress);
        builder.addressSignature(mParamsCryptoUtil.encrypt(addressSignature));
        builder.userName(mParamsCryptoUtil.encrypt(mUserName));
        builder.deviceId(mParamsCryptoUtil.encrypt(deviceId));
        builder.phoneModel(Build.MODEL);
        builder.tzIdHash(mParamsCryptoUtil.encrypt(tzIdHash));
        // For resending link
        if (!TextUtils.isEmpty(mBackupTargetName)) {
            LogUtil.logDebug(TAG, "Setup link for resending...");
            builder.backupTargetName(mParamsCryptoUtil.encrypt(mBackupTargetName));
        }
        // For resending link, real Bad
        if (!TextUtils.isEmpty(mBackupTargetUUIDHash)) {
            LogUtil.logDebug(TAG, "Encrypt backupTargetUUIDHash via keystore's AES");
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
            builder.backupTargetUUIDHash(genericCipherUtil.encryptData(mBackupTargetUUIDHash));
        }
        if (mRestoreMode) {
            builder.flowType(AppLinkConstant.FLOW_TYPE_RESTORE);
            builder.googleDriveDeviceId(mParamsCryptoUtil.encrypt(mGoogleDriveDeviceId));
        }

        boolean isProductionServer = ZionSkrSdkManager.getInstance().getIsUsingProductionServer();
        if (isProductionServer) {
            LogUtil.logDebug(TAG, "Building a production link...");
        } else {
            LogUtil.logDebug(TAG, "Building a stage link...");
            builder.isTest(AppLinkConstant.IS_TESTING);
        }

        final Map<String, String> paramsMap = builder.setupParamsMap();
        AppLink appLink;
        if (AppLinkConstant.APP_LINK_VERSION >= AppLinkConstant.APP_LINK_BRANCH_START_VERSION) {
            appLink = new BranchAppLink(paramsMap);
        } else {
            Uri uri = builder.setupLink(mGooglePlayLink, paramsMap);
            appLink = new FirebaseAppLink(uri);
        }

        appLink.setShortLinkTaskListener(new ShortLinkTaskListener() {
            @Override
            public void onShortLinkTaskFinished(
                    @NonNull Uri shortLink) {
                cancelCountdownTimer();
                setupSharingInvitation(shortLink.toString());
            }
        });
        appLink.setFailureListener(new ShortLinkFailureListener() {
            @Override
            public void onFailure(Exception e) {
                LogUtil.logError(TAG, "AppLink, onFailure: " + e);
                cancelCountdownTimer();
                mProgressDialogUtil.dismiss(mProgressDialog);
            }
        });
        appLink.createLinkTask(mActivity);
    }

    private String buildMessage(@NonNull final Uri link) {
        if (link == null) {
            LogUtil.logError(TAG, "buildMessage, link is null");
            return "";
        }
        final String messageFirstPart;
        final String messageSecondPart;
        final String messageThirdPart;
        final String messageFourthPart;
        if (mRestoreMode) {
            messageFirstPart = mActivity.getResources().getString(
                    R.string.restore_invitation_message_first_part);
            messageSecondPart = mActivity.getResources().getString(
                    R.string.restore_invitation_message_second_part);
            messageThirdPart = mActivity.getResources().getString(
                    R.string.restore_invitation_message_third_part);
            messageFourthPart = mActivity.getResources().getString(
                    R.string.restore_invitation_message_fourth_part);
        } else {
            messageFirstPart = mActivity.getResources().getString(
                    R.string.backup_invitation_message_first_part);
            messageSecondPart = mActivity.getResources().getString(
                    R.string.backup_invitation_message_second_part);
            messageThirdPart = mActivity.getResources().getString(
                    R.string.backup_invitation_message_third_part);
            messageFourthPart = mActivity.getResources().getString(
                    R.string.backup_invitation_message_fourth_part);
        }

        StringBuilder builder = new StringBuilder();
        builder.append(messageFirstPart);
        builder.append(messageSecondPart);
        builder.append(link.toString());
        builder.append("\n\n");
        builder.append(messageThirdPart);
        builder.append(messageFourthPart);
        return builder.toString();
    }

    private void startCountdownTimer() {
        synchronized (this) {
            if (mCountDownTimer != null) {
                mCountDownTimer.cancel();
            }
            mCountDownTimer = new CountDownTimer(APPLINK_TIME_OUT, APPLINK_TIME_OUT_INTERVAL) {
                @Override
                public void onFinish() {
                    mProgressDialogUtil.dismiss(mProgressDialog);
                    showTimeoutDialog();
                }

                @Override
                public void onTick(long millisUntilFinished) {
                }
            }.start();
        }
    }

    private void showTimeoutDialog() {
        final String title = mActivity.getResources().getString(
                R.string.backup_restore_applink_timeout_dialog_title);
        final String content = mActivity.getResources().getString(
                R.string.backup_restore_applink_timeout_dialog_content);
        TimeoutDialogUtil.setupTimeOutDialog(mActivity, title, content);
    }

    public void cancelCountdownTimer() {
        synchronized (this) {
            if (mCountDownTimer != null) {
                mCountDownTimer.cancel();
            }
        }
    }

    public void setGoogleDriveDeviceId(String googleDriveDeviceId) {
        mGoogleDriveDeviceId = googleDriveDeviceId;
    }

    private boolean isAppSupportDeepLink(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }

        for (String app : NOT_SUPPORT_DEEPLINK_APPS) {
            if (packageName.contains(app)) {
                return false;
            }
        }
        return true;
    }

    private boolean isActivitySupportDeepLink(String activityName) {
        if (TextUtils.isEmpty(activityName)) {
            return false;
        }

        for (String activity : NOT_SUPPORT_DEEPLINK_ACTIVITES) {
            if (activityName.equals(activity)) {
                return false;
            }
        }
        return true;
    }

    private Intent onShareClick(String message) {
        PackageManager pm = mActivity.getPackageManager();

        final Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, message);
        sendIntent.setType("text/plain");
        String subjectTitle;
        String subjectWithName;
        if (mRestoreMode) {
            subjectTitle = mActivity.getResources().getString(
                    R.string.social_restore_backup_recovery_support_request_from);
        } else {
            subjectTitle = mActivity.getResources().getString(
                    R.string.social_restore_backup_requests);
        }

        subjectWithName = String.format(subjectTitle, mUserName);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, subjectWithName);

        List<ResolveInfo> resInfo = pm.queryIntentActivities(sendIntent,
                PackageManager.MATCH_DEFAULT_ONLY);
        List<Intent> intentList = new ArrayList<>();
        for (int i = 0; i < resInfo.size(); i++) {
            ResolveInfo ri = resInfo.get(i);
            if (ri == null || ri.activityInfo == null) {
                LogUtil.logDebug(TAG, "ResolveInfo is null!");
                continue;
            }

            final String packageName = ri.activityInfo.packageName;
            final String activityName = ri.activityInfo.name;
            if (!isAppSupportDeepLink(packageName)) {
                LogUtil.logDebug(TAG, "Ignoring package: " + packageName);
            } else if (!isActivitySupportDeepLink(activityName)) {
                LogUtil.logDebug(TAG, "Ignoring activity: " + activityName);
            } else {
                sendIntent.setComponent(new ComponentName(packageName, activityName));
                final LabeledIntent labeledIntent = new LabeledIntent(sendIntent, packageName,
                        ri.loadLabel(pm), ri.icon);
                intentList.remove(labeledIntent);
                intentList.add(labeledIntent);
            }
        }

        final Intent openInChooser =
                Intent.createChooser(new Intent(),
                        mActivity.getResources().getString(R.string.backup_invitation_sharing));
        final LabeledIntent[] extraIntents = intentList.toArray(
                new LabeledIntent[intentList.size()]);
        openInChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);

        return openInChooser;
    }
}
