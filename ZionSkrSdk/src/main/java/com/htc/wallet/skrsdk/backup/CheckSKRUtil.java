package com.htc.wallet.skrsdk.backup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.action.Action;
import com.htc.wallet.skrsdk.action.BackupDeleteAction;
import com.htc.wallet.skrsdk.legacy.v1.LegacyBackupDataV1;
import com.htc.wallet.skrsdk.legacy.v1.LegacyV1Util;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupDataConstants;
import com.htc.wallet.skrsdk.sqlite.util.BackupTargetUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;
import com.htc.wallet.skrsdk.util.ThreadUtil;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;


// Only for security protection update
public final class CheckSKRUtil {
    private static final String TAG = "CheckSKRUtil";

    private static final ThreadPoolExecutor sSingleThreadPoolExecutor =
            ThreadUtil.newFixedThreadPool(1, "check-skr-util");

    public static void showSkrSecurityUpdateDialogV1(final Activity activity) {
        LogUtil.logDebug(TAG, "Show Skr Security Update Dialog V1");
        Objects.requireNonNull(activity, "activity is null");

        new AlertDialog.Builder(activity)
                .setTitle(activity.getResources().getString(
                        R.string.security_protection_dialog_title))
                .setMessage(activity.getResources().getString(
                        R.string.security_protection_dialog_content))
                .setCancelable(false)
                .setPositiveButton(R.string.security_protection_dialog_positive,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Send delete message if needed
                                sendDeleteMessageIfNeeded(activity);
                                // SocialBackupActivity not need to transform again
                                if (activity.getClass() != SocialBackupActivity.class) {
                                    // Start SocialBackupIntroductionActivity, and it will pass
                                    // to SocialBackupActivity automatically
                                    Intent intent = new Intent();
                                    intent.setClass(activity,
                                            SocialBackupIntroductionActivity.class);
                                    activity.startActivity(intent);
                                }
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(R.string.security_protection_dialog_negative,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                .show();
    }

    private static void sendDeleteMessageIfNeeded(Context context) {
        Objects.requireNonNull(context, "context is null");
        final Context appContext = context.getApplicationContext();

        if (SkrSharedPrefs.getUserAgreeDeleteLegacySkrBackupDataV1(appContext)) {
            // User has agree and delete Legacy Backup Data V1, still send delete message if needed
            LogUtil.logDebug(TAG, "User has agree and delete legacy backup data");
        } else {
            // User agree, put to sharedPrefs
            SkrSharedPrefs.putUserAgreeDeleteLegacySkrBackupDataV1(appContext, true);
            LogUtil.logDebug(TAG, "User agree and delete Legacy Backup Data V1");
        }

        sSingleThreadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                // Delete Bob's Legacy Backup Data V1 if needed
                final Map<String, LegacyBackupDataV1> legacySkrV1Map =
                        LegacyV1Util.getLegacyV1Map(appContext);
                for (final String uuidHash : legacySkrV1Map.keySet()) {
                    if (TextUtils.isEmpty(uuidHash)) {
                        LogUtil.logError(TAG, "uuidHash is empty");
                        continue;
                    }
                    // Check status, only in bad status we send delete message to Bob
                    // Amy may approved after new backup finish
                    BackupTargetUtil.get(appContext, uuidHash, new LoadDataListener() {
                        @Override
                        public void onLoadFinished(
                                BackupSourceEntity backupSourceEntity,
                                BackupTargetEntity backupTargetEntity,
                                RestoreSourceEntity restoreSourceEntity,
                                RestoreTargetEntity restoreTargetEntity) {

                            if (backupTargetEntity != null && backupTargetEntity.compareStatus(
                                    BackupTargetEntity.BACKUP_TARGET_STATUS_BAD)) {
                                String fcmToken = backupTargetEntity.getFcmToken();
                                // Only name Bad backupTarget without fcmToken
                                if (!TextUtils.isEmpty(fcmToken)) {
                                    LogUtil.logDebug(TAG, "Send delete message to " +
                                            backupTargetEntity.getName());
                                    Map<String, String> map = new ArrayMap<>();
                                    map.put(Action.KEY_UUID_HASH, uuidHash);
                                    map.put(Action.KEY_IS_RESEND, Action.MSG_IS_RESEND);
                                    // Put publicKey prevent backupTarget has been delete
                                    String publicKey = null;
                                    LegacyBackupDataV1 legacyBackupDataV1 =
                                            legacySkrV1Map.get(uuidHash);
                                    if (legacyBackupDataV1 != null) {
                                        publicKey = legacyBackupDataV1.getPublicKey();
                                    }
                                    if (!TextUtils.isEmpty(publicKey)) {
                                        map.put(Action.KEY_PUBLIC_KEY, publicKey);
                                    }
                                    // Let Bob delete when version less than or equal to
                                    // BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION (legacy V1, 1)
                                    map.put(Action.KEY_BACKUP_VERSION, String.valueOf(
                                            BackupDataConstants.BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION));
                                    new BackupDeleteAction().send(appContext, fcmToken, null, null,
                                            map);
                                }
                            }

                        }
                    });
                }
            }
        });


    }
}