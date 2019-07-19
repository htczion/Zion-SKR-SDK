package com.htc.wallet.skrsdk.util;

import static com.htc.wallet.skrsdk.applink.AppLinkConstant.APP_LINK_VALIDITY_TIME;
import static com.htc.wallet.skrsdk.applink.AppLinkConstant.IS_TESTING;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_DONE;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_FULL;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_OK;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_REQUEST_AUTO_BACKUP;
import static com.htc.wallet.skrsdk.verification.DialogActivity.KEY_DIALOG_MESSAGE;
import static com.htc.wallet.skrsdk.verification.DialogActivity.KEY_DIALOG_TITLE;
import static com.htc.wallet.skrsdk.verification.DialogActivity.KEY_DIALOG_TYPE;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.ArrayMap;

import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.action.Action;
import com.htc.wallet.skrsdk.action.RestoreTargetVerifyAction;
import com.htc.wallet.skrsdk.adapter.EntryActivityAdapter;
import com.htc.wallet.skrsdk.applink.AppLinkConstant;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupSourceUtil;
import com.htc.wallet.skrsdk.sqlite.util.RestoreTargetUtil;
import com.htc.wallet.skrsdk.verification.BackupShowVerificationCodeActivity;
import com.htc.wallet.skrsdk.verification.DialogActivity;
import com.htc.wallet.skrsdk.verification.VerificationCodeActivity;
import com.htc.wallet.skrsdk.verification.VerificationOkCongratulationActivity;
import com.htc.wallet.skrsdk.verification.VerificationRequestActivity;
import com.htc.wallet.skrsdk.verification.VerificationSharingActivity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class IntentUtil {
    private static final String TAG = "IntentUtil";

    private static final long MAX_DEVICE_TIME_TOLERANCE =
            5 * DateUtils.MINUTE_IN_MILLIS; // 5 minutes tolerance, for devices not synced

    private static final int LINK_STATE_PASS = 0;
    private static final int LINK_STATE_UPDATE = 1;
    private static final int LINK_STATE_EXPIRE = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LINK_STATE_PASS, LINK_STATE_UPDATE, LINK_STATE_EXPIRE})
    private @interface LinkStateType {
    }

    private IntentUtil() {
    }

    // AppLinkReceiverActivity
    @NonNull
    public static Intent generateBackupIntent(
            @NonNull final Context context,
            @NonNull final Bundle bundle) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(bundle, "bundle is null");
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Intent> intent = new AtomicReference<>();
        final String name = bundle.getString(AppLinkConstant.KEY_USER_NAME);
        if (TextUtils.isEmpty(name)) {
            LogUtil.logError(TAG, "name is null or empty.",
                    new IllegalArgumentException("name is null or empty."));
            return generateEntryActivityIntent(context);
        }

        @LinkStateType int linkState = checkLinkState(bundle);
        switch (linkState) {
            case LINK_STATE_PASS:
                // Both of link version and timestamp is fine, keep going.
                break;
            case LINK_STATE_UPDATE:
                // App should update, show dialog
                return generateDialogActivityIntent(
                        context, DialogActivity.DIALOG_TYPE_APP_UPDATE, null, null);
            case LINK_STATE_EXPIRE:
                // link expired, show dialog
                final String message = String.format(
                        context.getResources().getString(R.string.ver_expired_link), name);
                return generateDialogActivityIntent(
                        context, DialogActivity.DIALOG_TYPE_NORMAL, null, message);
            default:
                LogUtil.logError(TAG, "unknown link state", new IllegalStateException());
                return generateEntryActivityIntent(context);
        }

        // TODO: Refactor Check
        final String emailHash = bundle.getString(AppLinkConstant.KEY_ADDRESS_SIGNATURE);
        if (TextUtils.isEmpty(emailHash)) {
            LogUtil.logError(TAG, "emailHash is null or empty.",
                    new IllegalArgumentException("emailHash is null or empty."));
            return generateEntryActivityIntent(context);
        }
        final String uuidHash = bundle.getString(AppLinkConstant.KEY_DEVICE_ID);
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(TAG, "uuid hash is null or empty.",
                    new IllegalArgumentException("uuid hash is null or empty."));
            return generateEntryActivityIntent(context);
        }
        final String tzIdHash = bundle.getString(AppLinkConstant.KEY_TZ_ID_HASH);
        if (TextUtils.isEmpty(tzIdHash)) {
            LogUtil.logError(TAG, "tzIdHash is null or empty.",
                    new IllegalArgumentException("tzIdHash is null or empty."));
            return generateEntryActivityIntent(context);
        }
        final String fcmToken = bundle.getString(AppLinkConstant.KEY_TOKEN);
        final String whisperPub = bundle.getString(AppLinkConstant.KEY_WHISPER_PUB);
        final String pushyToken = bundle.getString(AppLinkConstant.KEY_PUSHY_TOKEN);
        final String publicKey = bundle.getString(AppLinkConstant.KEY_PUBLIC);

        if (TextUtils.isEmpty(emailHash)) {
            LogUtil.logError(TAG, "publicKey is null or empty.",
                    new IllegalArgumentException("publicKey is null or empty."));
            return generateEntryActivityIntent(context);
        }
        final String strIsTest = bundle.getString(AppLinkConstant.KEY_TEST);
        boolean isTest = false;
        if (IS_TESTING.equals(strIsTest)) {
            LogUtil.logInfo(TAG, "generateBackupIntent. Clicked the test link.");
            isTest = true;
        }
        final boolean finalIsTest = isTest;
        BackupSourceUtil.getWithUUIDHash(context, emailHash, uuidHash, new LoadDataListener() {
            final String backupTargetName =
                    bundle.getString(AppLinkConstant.KEY_BACKUP_TARGET_NAME);
            final String backupTargetUUIDHash =
                    bundle.getString(AppLinkConstant.KEY_BACKUP_TARGET_UUID_HASH);

            @Override
            public void onLoadFinished(
                    BackupSourceEntity backupSourceEntity,
                    BackupTargetEntity backupTargetEntity,
                    RestoreSourceEntity restoreSourceEntity,
                    RestoreTargetEntity restoreTargetEntity) {
                if (backupSourceEntity == null) {
                    if (TextUtils.isEmpty(backupTargetName)) {
                        intent.set(VerificationRequestActivity.generateIntent(
                                context,
                                emailHash,
                                uuidHash,
                                name,
                                fcmToken,
                                whisperPub,
                                pushyToken,
                                publicKey,
                                tzIdHash,
                                finalIsTest));
                        latch.countDown();
                    } else {
                        LogUtil.logDebug(TAG, "Generating the intent for resend...");
                        intent.set(VerificationRequestActivity.generateIntentForResend(
                                context,
                                emailHash,
                                uuidHash,
                                name,
                                backupTargetName,
                                backupTargetUUIDHash,
                                fcmToken,
                                whisperPub,
                                pushyToken,
                                publicKey,
                                tzIdHash,
                                finalIsTest));
                        latch.countDown();
                    }
                } else {
                    // We don't save the tzIdHash after completing the backup, so ensuring
                    // that Bob can get tzIdHash when entering
                    // VerificationRequestActivity to prevent tzIdHash exception.
                    backupSourceEntity.setTzIdHash(tzIdHash);
                    intent.set(generateBackupSourceIntent(
                            context,
                            backupSourceEntity,
                            backupTargetName,
                            backupTargetUUIDHash));
                    latch.countDown();
                }
            }
        });
        if (intent.get() == null) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                LogUtil.logError(TAG, "thread interrupted");
            }
        }
        return intent.get();
    }

    // AppLinkReceiverActivity
    @NonNull
    public static Intent generateRestoreIntent(
            @NonNull final Context context,
            @NonNull Bundle bundle) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(bundle, "bundle is null");
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Intent> intent = new AtomicReference<>();
        final String name = bundle.getString(AppLinkConstant.KEY_USER_NAME);
        if (TextUtils.isEmpty(name)) {
            LogUtil.logError(TAG, "name is null or empty.",
                    new IllegalArgumentException("name is null or empty."));
            return generateEntryActivityIntent(context);
        }

        @LinkStateType int linkState = checkLinkState(bundle);
        switch (linkState) {
            case LINK_STATE_PASS:
                // Both of link version and timestamp is fine, keep going.
                break;
            case LINK_STATE_UPDATE:
                // App should update, show dialog
                return generateDialogActivityIntent(
                        context, DialogActivity.DIALOG_TYPE_APP_UPDATE, null, null);
            case LINK_STATE_EXPIRE:
                // link expired, show dialog
                final String message = String.format(
                        context.getResources().getString(R.string.ver_expired_link), name);
                return generateDialogActivityIntent(
                        context, DialogActivity.DIALOG_TYPE_NORMAL, null, message);
            default:
                LogUtil.logError(TAG, "unknown link state", new IllegalStateException());
                return generateEntryActivityIntent(context);
        }

        // TODO: Refactor Check
        final String emailHash = bundle.getString(AppLinkConstant.KEY_ADDRESS_SIGNATURE);
        if (TextUtils.isEmpty(emailHash)) {
            LogUtil.logError(TAG, "emailHash is null or empty.",
                    new IllegalArgumentException("emailHash is null or empty."));
            return generateEntryActivityIntent(context);
        }
        final String uuidHash = bundle.getString(AppLinkConstant.KEY_DEVICE_ID);
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(TAG, "uuid hash is null or empty.",
                    new IllegalArgumentException("uuid hash is null or empty."));
            return generateEntryActivityIntent(context);
        }
        final String backupUUIDHash = bundle.getString(AppLinkConstant.KEY_GOOGLE_DRIVE_DEVICE_ID);
        if (TextUtils.isEmpty(backupUUIDHash)) {
            LogUtil.logError(TAG, "backupUUIDHash is null or empty.");
            return generateEntryActivityIntent(context);
        }
        final String tzIdHash = bundle.getString(AppLinkConstant.KEY_TZ_ID_HASH);
        if (TextUtils.isEmpty(tzIdHash)) {
            LogUtil.logError(TAG, "tzIdHash is null or empty.",
                    new IllegalArgumentException("tzIdHash is null or empty."));
            return generateEntryActivityIntent(context);
        }
        String strIsTest = bundle.getString(AppLinkConstant.KEY_TEST);
        boolean isTest = false;
        if (IS_TESTING.equals(strIsTest)) {
            LogUtil.logInfo(TAG, "generateRestoreIntent. Clicked the test link.");
            isTest = true;
        }
        final boolean finalIsTest = isTest;

        final String fcmToken = bundle.getString(AppLinkConstant.KEY_TOKEN);
        final String whisperPub = bundle.getString(AppLinkConstant.KEY_WHISPER_PUB);
        final String pushyToken = bundle.getString(AppLinkConstant.KEY_PUSHY_TOKEN);
        final String publicKey = bundle.getString(AppLinkConstant.KEY_PUBLIC);

        if (TextUtils.isEmpty(publicKey)) {
            LogUtil.logError(TAG, "publicKey is null or empty.",
                    new IllegalArgumentException("publicKey is null or empty."));
            return generateEntryActivityIntent(context);
        }
        final String phoneModel = bundle.getString(AppLinkConstant.KEY_RESTORE_TARGET_PHONE_MODEL);

        RestoreTargetUtil.get(context, emailHash, uuidHash, new LoadDataListener() {
            @Override
            public void onLoadFinished(
                    BackupSourceEntity backupSourceEntity,
                    BackupTargetEntity backupTargetEntity,
                    RestoreSourceEntity restoreSourceEntity,
                    RestoreTargetEntity restoreTargetEntity) {
                if (restoreTargetEntity == null) {
                    BackupSourceUtil.getOK(context, emailHash, backupUUIDHash,
                            new LoadDataListener() {
                                @Override
                                public void onLoadFinished(
                                        BackupSourceEntity backupSourceEntity,
                                        BackupTargetEntity backupTargetEntity,
                                        RestoreSourceEntity restoreSourceEntity,
                                        final RestoreTargetEntity restoreTargetEntity) {
                                    if (backupSourceEntity == null) {
                                        // I don't have Amy's Backup data, back to
                                        // EntryActivity
                                        String message = String.format(
                                                context.getResources().getString(
                                                        R.string.social_restore_no_backup_on_device),
                                                name);
                                        intent.set(generateDialogActivityIntent(
                                                context,
                                                DialogActivity.DIALOG_TYPE_NORMAL,
                                                null,
                                                message));
                                        LogUtil.logDebug(TAG, "No backup data"
                                                + ", n1 = " + emailHash.substring(0, 5)
                                                + ", n2 = " + (TextUtils.isEmpty(backupUUIDHash)
                                                ? "" : backupUUIDHash.substring(0, 5)));
                                        latch.countDown();
                                    } else {
                                        // Click shared link first times and I have Amy's
                                        // backup
                                        final RestoreTargetEntity target =
                                                new RestoreTargetEntity();
                                        target.setEmailHash(emailHash);
                                        target.setUUIDHash(uuidHash);
                                        target.setTzIdHash(tzIdHash);
                                        target.setIsTest(finalIsTest);
                                        target.setBackupUUIDHash(backupUUIDHash);
                                        target.setFcmToken(fcmToken);
                                        target.setWhisperPub(whisperPub);
                                        target.setPushyToken(pushyToken);
                                        target.setPublicKey(publicKey);
                                        target.setName(name);
                                        target.setTimeStamp(System.currentTimeMillis());
                                        target.setRetryTimes(Action.RESTORE_RETRY_TIMES);
                                        target.setPinCode(PinCodeUtil.newPinCode());
                                        target.setPhoneModel(phoneModel);
                                        RestoreTargetUtil.put(context, target,
                                                new DatabaseCompleteListener() {
                                                    @Override
                                                    public void onComplete() {
                                                        intent.set(generateRestoreTargetIntent(
                                                                context, target));
                                                        latch.countDown();
                                                    }

                                                    @Override
                                                    public void onError(
                                                            Exception exception) {
                                                        LogUtil.logError(TAG, "put() failed, "
                                                                + "e=" + exception);
                                                    }
                                                });
                                    }
                                }
                            });

                } else {
                    LogUtil.logDebug(TAG, "Click shared link more than one times, "
                            + "not need to build RestoreTarget again.");
                    // Click shared link more than one times, not need to build RestoreTarget again.
                    intent.set(generateRestoreTargetIntent(context, restoreTargetEntity));
                    latch.countDown();
                }
            }
        });

        if (intent.get() == null) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                LogUtil.logError(TAG, "thread interrupted");
            }
        }
        return intent.get();
    }

    // Notification or Request from friends list
    public static Intent generateBackupTargetIntent(
            @NonNull Context context,
            @NonNull BackupTargetEntity target) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(target, "backupTarget is null");
        if (!target.isStatusPending()) {
            LogUtil.logError(TAG, "incorrect status=" + target.getStatus(),
                    new IllegalStateException("incorrect status=" + target.getStatus()));
            return generateEntryActivityIntent(context);
        }
        return BackupShowVerificationCodeActivity.generateIntent(context, target.getUUIDHash());
    }

    // Request from friends list
    @NonNull
    public static Intent generateBackupSourceIntent(
            @NonNull Context context,
            @NonNull BackupSourceEntity backupSource,
            String backupTargetName,
            String backupTargetUUIDHash) {
        Objects.requireNonNull(context, "context is null");

        final String emailHash = backupSource.getEmailHash();
        final String uuidHash = backupSource.getUUIDHash();
        final String name = backupSource.getName();
        final String fcmToken = backupSource.getFcmToken();
        final String whisperPub = backupSource.getWhisperPub();
        final String pushyToken = backupSource.getPushyToken();
        final String publicKey = backupSource.getPublicKey();
        final String tzIdHash = backupSource.getTzIdHash();
        final boolean isTest = backupSource.getIsTest();

        Intent intent;
        switch (backupSource.getStatus()) {
            case BACKUP_SOURCE_STATUS_REQUEST:
                final String myName = backupSource.getMyName();
                final long lastRequestTime = backupSource.getLastRequestTime();
                final String pinCode = backupSource.getPinCode();
                final int isPinCodeError = backupSource.getIsPinCodeError();
                final int retryTimes = backupSource.getRetryTimes();
                final long retryWaitStartTime = backupSource.getRetryWaitStartTime();
                final long lastVerifyTime = backupSource.getLastVerifyTime();
                intent = VerificationCodeActivity.generateIntent(
                        context,
                        myName,
                        backupTargetName,
                        backupTargetUUIDHash,
                        emailHash,
                        uuidHash,
                        name,
                        fcmToken,
                        whisperPub,
                        pushyToken,
                        publicKey,
                        tzIdHash,
                        lastRequestTime,
                        pinCode,
                        isPinCodeError,
                        retryTimes,
                        retryWaitStartTime,
                        lastVerifyTime,
                        false,
                        isTest);
                break;
            case BACKUP_SOURCE_STATUS_OK:
                if (TextUtils.isEmpty(backupTargetName)) {
                    intent = VerificationOkCongratulationActivity.generateIntent(
                            context, emailHash, uuidHash, name);
                } else {
                    LogUtil.logInfo(TAG, "Generating intent for resend...");
                    intent = VerificationRequestActivity.generateIntentForResend(
                            context,
                            emailHash,
                            uuidHash,
                            name,
                            backupTargetName,
                            backupTargetUUIDHash,
                            fcmToken,
                            whisperPub,
                            pushyToken,
                            publicKey,
                            tzIdHash,
                            isTest);
                }
                break;
            case BACKUP_SOURCE_STATUS_REQUEST_AUTO_BACKUP:
                // In this case, we also show Bob already backup
            case BACKUP_SOURCE_STATUS_DONE:
                if (TextUtils.isEmpty(backupTargetName)) {
                    intent = VerificationRequestActivity.generateIntent(
                            context,
                            emailHash,
                            uuidHash,
                            name,
                            fcmToken,
                            whisperPub,
                            pushyToken,
                            publicKey,
                            tzIdHash,
                            isTest);
                } else {
                    LogUtil.logInfo(TAG, "Generating intent for resend...");
                    intent = VerificationRequestActivity.generateIntentForResend(
                            context,
                            emailHash,
                            uuidHash,
                            name,
                            backupTargetName,
                            backupTargetUUIDHash,
                            fcmToken,
                            whisperPub,
                            pushyToken,
                            publicKey,
                            tzIdHash,
                            isTest);
                }
                break;
            case BACKUP_SOURCE_STATUS_FULL:
                final String myNameForFull = backupSource.getMyName();
                final long lastRequestTimeForFull = backupSource.getLastRequestTime();
                final String pinCodeForFull = backupSource.getPinCode();
                final int isPinCodeErrorForFull = backupSource.getIsPinCodeError();
                final int retryTimesForFull = backupSource.getRetryTimes();
                final long retryWaitStartTimeForFull = backupSource.getRetryWaitStartTime();
                final long lastVerifyTimeForFull = backupSource.getLastVerifyTime();
                intent = VerificationCodeActivity.generateIntent(
                        context,
                        myNameForFull,
                        backupTargetName,
                        backupTargetUUIDHash,
                        emailHash,
                        uuidHash,
                        name,
                        fcmToken,
                        whisperPub,
                        pushyToken,
                        publicKey,
                        tzIdHash,
                        lastRequestTimeForFull,
                        pinCodeForFull,
                        isPinCodeErrorForFull,
                        retryTimesForFull,
                        retryWaitStartTimeForFull,
                        lastVerifyTimeForFull,
                        true,
                        isTest);
                break;
            default:
                LogUtil.logError(TAG, "Incorrect status=" + backupSource.getStatus(),
                        new IllegalStateException("Incorrect status=" + backupSource.getStatus()));
                intent = generateEntryActivityIntent(context);
        }
        return intent;
    }

    // Request from friends list
    @NonNull
    public static Intent generateBackupSourceIntent(
            @NonNull Context context, @NonNull BackupSourceEntity backupSource) {
        // TODO store backupTargetName adn backupTargetUUIDHash to db, merge
        // generateBackupSourceIntent to one
        return generateBackupSourceIntent(context, backupSource, "", "");
    }

    // Request from friends list
    @NonNull
    public static Intent generateRestoreTargetIntent(
            @NonNull Context context, @NonNull RestoreTargetEntity target) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(target, "restoreTarget is null");

        // Send RestoreTargetVerifyAction first or again

        Map<String, String> map = new ArrayMap<>();
        map.put(Action.KEY_PUBLIC_KEY, target.getPublicKey());
        map.put(Action.KEY_RESTORE_TARGET_UUID_HASH, target.getBackupUUIDHash());
        map.put(Action.KEY_RESTORE_TARGET_EMAIL_HASH, target.getEmailHash());
        map.put(Action.KEY_LINK_UUID_HASH, target.getUUIDHash());
        new RestoreTargetVerifyAction().send(
                context,
                target.getFcmToken(),
                target.getWhisperPub(),
                target.getPushyToken(),
                map);

        return VerificationSharingActivity.generateIntent(
                context,
                target.getEmailHash(),
                target.getUUIDHash(),
                target.getFcmToken(),
                target.getWhisperPub(),
                target.getPushyToken(),
                target.getName(),
                target.getPinCode(),
                target.getPhoneModel());
    }

    @NonNull
    public static Intent generateEntryActivityIntent(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");

        EntryActivityAdapter adapter = ZionSkrSdkManager.getInstance().getEntryActivityAdapter();
        Class entryActivityClass = null;
        if (adapter != null) {
            entryActivityClass = adapter.getEntryActivityClass();
        }

        Intent intent;
        if (entryActivityClass == null) {
            // Use packageManager get launch activity
            PackageManager pm = context.getPackageManager();
            String packageName = context.getPackageName();
            intent = pm.getLaunchIntentForPackage(packageName);
            Objects.requireNonNull(intent, "launcher intent is null");
        } else {
            intent = new Intent(context, entryActivityClass);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    private static Intent generateDialogActivityIntent(
            @NonNull Context context,
            @DialogActivity.DialogType int dialogType,
            @Nullable String title,
            @Nullable String message) {
        Objects.requireNonNull(context, "context is null");
        final Intent intent = new Intent(context, DialogActivity.class);
        intent.putExtra(KEY_DIALOG_TYPE, dialogType);
        intent.putExtra(KEY_DIALOG_TITLE, title);
        intent.putExtra(KEY_DIALOG_MESSAGE, message);
        return intent;
    }

    @LinkStateType
    private static int checkLinkState(@NonNull Bundle bundle) {
        Objects.requireNonNull(bundle, "bundle is null");

        String linkVersionStr = bundle.getString(AppLinkConstant.KEY_VERSION);
        // We set version 1 when there is no version content
        // If parseInt failed, we set link version to 0, make this link expired
        final int linkVersion =
                TextUtils.isEmpty(linkVersionStr) ? 1 : tryParseInt(linkVersionStr, 0);

        if (linkVersion == AppLinkConstant.APP_LINK_VERSION) {
            // Same link version, then check link timestamp
            String linkTimeStampStr = bundle.getString(AppLinkConstant.KEY_TIMESTAMP);
            final long linkTimeStamp = tryParseLong(linkTimeStampStr, 0);
            final long currentTime = System.currentTimeMillis();
            if (currentTime > (linkTimeStamp + APP_LINK_VALIDITY_TIME + MAX_DEVICE_TIME_TOLERANCE)
                    || currentTime < (linkTimeStamp - MAX_DEVICE_TIME_TOLERANCE)) {
                // Link expired or invalid link timestamp (future timestamp)
                return LINK_STATE_EXPIRE;
            }
            return LINK_STATE_PASS;
        } else if (linkVersion > AppLinkConstant.APP_LINK_VERSION) {
            // Shared link's version is newer than this app
            return LINK_STATE_UPDATE;
        } else {
            // Shared link's version is older than this app
            return LINK_STATE_EXPIRE;
        }
    }

    private static int tryParseInt(String intStr, int defaultValue) {
        if (TextUtils.isEmpty(intStr)) {
            LogUtil.logDebug(TAG, "intStr is null or empty");
            return defaultValue;
        }
        try {
            return Integer.parseInt(intStr);
        } catch (NumberFormatException e) {
            LogUtil.logError(TAG, "tryParseInt failed", new IllegalStateException());
            return defaultValue;
        }
    }

    private static long tryParseLong(String longStr, long defaultValue) {
        if (TextUtils.isEmpty(longStr)) {
            LogUtil.logDebug(TAG, "longStr is null or empty");
            return defaultValue;
        }
        try {
            return Long.parseLong(longStr);
        } catch (NumberFormatException e) {
            LogUtil.logError(TAG, "tryParseLong failed", new IllegalStateException());
            return defaultValue;
        }
    }
}
