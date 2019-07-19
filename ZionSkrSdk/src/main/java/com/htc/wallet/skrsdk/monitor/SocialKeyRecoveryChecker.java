package com.htc.wallet.skrsdk.monitor;

import static com.htc.wallet.skrsdk.monitor.CheckAutoBackupJobService.REQUEST_AUTO_BACKUP_VALIDITY_TIME;
import static com.htc.wallet.skrsdk.monitor.CheckAutoBackupJobService.sendErrorHealthReport;

import android.content.Context;
import android.support.annotation.NonNull;

import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupSourceUtil;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.List;
import java.util.Objects;

public class SocialKeyRecoveryChecker {
    private static final String TAG = "SocialKeyRecoveryChecker";

    private SocialKeyRecoveryChecker() {
    }

    public static void check(@NonNull final Context context) {
        Objects.requireNonNull(context, "context is null");

        // If device reboot during enqueue CheckAutoBackupJobIntentService 1 hour
        // enqueue will invalid, therefore, we use this to check each start this application
        checkAutoBackupExpired(context.getApplicationContext());

        // Cancel deprecated Job id (Move to SocialKmDatabaseUpdateReceiver)
        // cancelDeprecateJobIdIfNeeded(context.getApplicationContext());

        // It can add another Social Key Recovery's check function if needed
    }

    private static void checkAutoBackupExpired(@NonNull final Context context) {
        Objects.requireNonNull(context, "context is null");

        BackupSourceUtil.getAllRequestAutoBackup(context, new LoadListListener() {
            @Override
            public void onLoadFinished(
                    List<BackupSourceEntity> backupSourceEntityList,
                    List<BackupTargetEntity> backupTargetEntityList,
                    List<RestoreSourceEntity> restoreSourceEntityList,
                    List<RestoreTargetEntity> restoreTargetEntityList) {
                if (backupSourceEntityList == null) {
                    LogUtil.logError(TAG, "backupSourceEntityList is null");
                    return;
                }

                final long currentTime = System.currentTimeMillis();
                for (BackupSourceEntity backupSourceEntity : backupSourceEntityList) {
                    if (currentTime
                            > backupSourceEntity.getTimeStamp()
                            + REQUEST_AUTO_BACKUP_VALIDITY_TIME) {
                        String emailHash = backupSourceEntity.getEmailHash();
                        String uuidHash = backupSourceEntity.getUUIDHash();
                        String name = backupSourceEntity.getName();

                        // Send error health report back to Amy
                        sendErrorHealthReport(
                                context,
                                backupSourceEntity.getFcmToken(),
                                backupSourceEntity.getWhisperPub(),
                                backupSourceEntity.getPushyToken(),
                                backupSourceEntity.getPublicKey(),
                                backupSourceEntity.getMyName());
                        // Remove it
                        BackupSourceUtil.remove(context, emailHash, uuidHash);
                        LogUtil.logDebug(TAG, name + "'s auto backup expired, remove it");
                    }
                }
            }
        });
    }
}
