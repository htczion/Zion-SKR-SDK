package com.htc.wallet.skrsdk.util;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.R;

import java.util.Objects;

public class NotificationUtil {
    private static final String TAG = "NotificationUtil";

    private static final int REQUEST_ID_SHOW_SOCIAL_BACKUP = 1;

    public static final int NOTIFICATION_ID_VERIFICATION_SHARING = 100;
    public static final int NOTIFICATION_ID_SOCIAL_BACKUP = 200;
    public static final int NOTIFICATION_ID_SECURITY_PROTECTION = 300;

    // Verification Request
    public static void showVerificationSharingNotification(@NonNull Context context,
            @NonNull Intent intent, @NonNull String name, @NonNull String uuidHash) {
        // Intent
        if (intent == null) {
            LogUtil.logError(TAG, "intent is null");
        } else if (TextUtils.isEmpty(name)) {
            LogUtil.logError(TAG, "name is empty");
        } else if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(TAG, "uuidHash is empty");
        } else {
            // Pending Intent
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, uuidHash.hashCode(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
            // Build notification, use UUID be notification tag
            buildVerificationSharingNotification(context, pendingIntent, name, uuidHash,
                    NOTIFICATION_ID_VERIFICATION_SHARING);
        }
    }

    private static void buildVerificationSharingNotification(
            @NonNull Context context,
            @NonNull PendingIntent pendingIntent,
            @NonNull String senderName,
            @NonNull String notificationTag,
            int notificationId) {
        if (context == null) {
            LogUtil.logError(TAG, "context is null");
            return;
        }
        if (!PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("allow_push_notification", true)) {
            LogUtil.logDebug(TAG, "not allow_push_notification ***");
            return;
        }
        if (pendingIntent == null) {
            LogUtil.logError(TAG, "pendingIntent is null");
            return;
        }
        if (TextUtils.isEmpty(notificationTag)) {
            LogUtil.logError(TAG, "notificationTag is empty or null");
            return;
        }
        if (TextUtils.isEmpty(senderName)) {
            LogUtil.logError(TAG, "senderName is null");
            return;
        }

        Resources resources = context.getResources();
        String channelId = resources.getString(R.string.ver_notification_channel_id);
        String channelName = resources.getString(R.string.ver_notification_channel_name);
        String title =
                String.format(
                        resources.getString(R.string.ver_notification_request_title), senderName);
        String buttonStr = resources.getString(R.string.ver_notification_request_button);

        NotificationChannel channel =
                new NotificationChannel(
                        channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            LogUtil.logError(TAG, "notificationManager is null");
            return;
        }
        notificationManager.createNotificationChannel(channel);

        Notification.Action action =
                new Notification.Action.Builder(null, buttonStr, pendingIntent).build();
        Notification notification =
                new Notification.Builder(context, channelId)
                        .setContentTitle(title)
                        .setContentText(
                                String.format(
                                        resources.getString(
                                                R.string.ver_notification_request_content),
                                        senderName))
                        .setSmallIcon(R.drawable.stat_notify_skr)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .addAction(action)
                        .build();
        notificationManager.notify(notificationTag, notificationId, notification);
    }

    /**
     * SKR Health Problem
     *
     * @param context Context
     * @param intent  SocialBackupIntroductionActivity
     */
    public static void showBackupHealthProblemNotification(
            @NonNull Context context,
            @NonNull Intent intent) {
        if (context == null) {
            LogUtil.logError(TAG, "context is null");
            return;
        } else if (intent == null) {
            LogUtil.logError(TAG, "intent is null");
            return;
        }

        // Issue fixed
        // Start SocialBackupActivity should pass email address and username, or it will crash
        // intent.putExtra(BackupSourceConstants.ADDRESS, mEmail);
        // intent.putExtra(BackupSourceConstants.NAME, mUserName);

        // Start intent (SocialBackupIntroductionActivity)
        // SocialBackupIntroductionActivity, it will pass to SocialBackupActivity automatically
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_ID_SHOW_SOCIAL_BACKUP,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        buildBackupHealthProblemNotification(context, pendingIntent, NOTIFICATION_ID_SOCIAL_BACKUP);
    }

    private static void buildBackupHealthProblemNotification(
            @NonNull Context context, @NonNull PendingIntent pendingIntent, int notificationId) {
        if (context == null) {
            LogUtil.logError(TAG, "context is null");
            return;
        }
        if (pendingIntent == null) {
            LogUtil.logError(TAG, "pendingIntent is null");
            return;
        }
        if (!PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("allow_push_notification", true)) {
            LogUtil.logDebug(TAG, "not allow_push_notification ***");
            return;
        }

        Resources resources = context.getResources();
        String channelId = resources.getString(R.string.ver_notification_channel_id);
        String channelName = resources.getString(R.string.ver_notification_channel_name);
        String title = resources.getString(R.string.social_backup_oops_inactive);
        String content =
                resources.getString(
                        R.string.social_backup_check_with_your_trusted_contact_to_secure);

        NotificationChannel channel =
                new NotificationChannel(
                        channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            LogUtil.logError(TAG, "notificationManager is null");
            return;
        }
        notificationManager.createNotificationChannel(channel);

        Notification notification =
                new Notification.Builder(context, channelId)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setSmallIcon(R.drawable.stat_notify_skr)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .build();
        notificationManager.notify(notificationId, notification);
    }

    /**
     * Security Protection
     *
     * @param context Context
     * @param intent  EntryActivity
     */
    public static void showUpdateLegacyBackupDataV1Notification(
            @NonNull Context context,
            @NonNull Intent intent) {
        if (context == null) {
            LogUtil.logError(TAG, "context is null");
            return;
        } else if (intent == null) {
            LogUtil.logError(TAG, "intent is null");
            return;
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_ID_SHOW_SOCIAL_BACKUP,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        buildUpdateLegacyBackupDataV1Notification(
                context, pendingIntent, NOTIFICATION_ID_SECURITY_PROTECTION);
    }

    private static void buildUpdateLegacyBackupDataV1Notification(
            @NonNull Context context, @NonNull PendingIntent pendingIntent, int notificationId) {
        if (context == null) {
            LogUtil.logError(TAG, "context is null");
            return;
        }
        if (pendingIntent == null) {
            LogUtil.logError(TAG, "pendingIntent is null");
            return;
        }
        if (!PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("allow_push_notification", true)) {
            LogUtil.logDebug(TAG, "not allow_push_notification ***");
            return;
        }

        Resources resources = context.getResources();
        String channelId = resources.getString(R.string.ver_notification_channel_id);
        String channelName = resources.getString(R.string.ver_notification_channel_name);
        String title = resources.getString(R.string.security_protection_dialog_title);
        String content = resources.getString(R.string.security_protection_notification_message);

        NotificationChannel channel =
                new NotificationChannel(
                        channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            LogUtil.logError(TAG, "notificationManager is null");
            return;
        }
        notificationManager.createNotificationChannel(channel);

        Notification notification =
                new Notification.Builder(context, channelId)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setSmallIcon(R.drawable.stat_notify_skr)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .build();
        notificationManager.notify(notificationId, notification);
    }

    public static void cancelNotification(
            @NonNull Context context, @Nullable String tag, int notificationId) {
        Objects.requireNonNull(context, "context is null or empty");
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            if (TextUtils.isEmpty(tag)) {
                notificationManager.cancel(notificationId);
            } else {
                notificationManager.cancel(tag, notificationId);
            }
        }
    }
}
