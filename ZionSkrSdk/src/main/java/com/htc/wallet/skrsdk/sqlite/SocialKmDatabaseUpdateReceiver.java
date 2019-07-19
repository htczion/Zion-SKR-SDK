package com.htc.wallet.skrsdk.sqlite;

import static android.content.Context.JOB_SCHEDULER_SERVICE;

import static com.htc.wallet.skrsdk.sqlite.util.BackupDataConstants.BACKUP_DATA_CLEAR_LEGACY_V1_VERSION;
import static com.htc.wallet.skrsdk.sqlite.util.BackupDataConstants.BACKUP_DATA_DB_INIT_VERSION;
import static com.htc.wallet.skrsdk.sqlite.util.BackupDataConstants.BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.htc.wallet.skrsdk.jobs.JobIdManager;
import com.htc.wallet.skrsdk.monitor.BackupHealthCheckJobService;
import com.htc.wallet.skrsdk.monitor.BackupHealthReportJobService;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupDataStatusUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;
import com.htc.wallet.skrsdk.util.ThreadUtil;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// android.intent.action.BOOT_COMPLETED
// android.intent.action.MY_PACKAGE_REPLACED
public class SocialKmDatabaseUpdateReceiver extends BroadcastReceiver {
    public static final String TAG = "SocialKmDatabaseUpdateReceiver";

    private static final int MAX_POOL_SIZE = 1;
    private static final ThreadPoolExecutor sDbUpdateWorker =
            ThreadUtil.newFixedThreadPool(MAX_POOL_SIZE, "skr-db-update");

    private static final int TIMEOUT = 30; // seconds

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent == null) {
            LogUtil.logError(TAG, "intent is null");
            return;
        }
        // UnsafeProtectedBroadcastReceiver
        // Due to we listen protected broadcast action, we need to check the action first
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            LogUtil.logDebug(TAG, "BOOT_COMPLETED");
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            LogUtil.logDebug(TAG, "MY_PACKAGE_REPLACED");
        } else {
            LogUtil.logWarning(TAG, "Unexpected Action = " + action);
            return;
        }

        // Check and schedule jobService if needed
        checkAndScheduleJobServiceIfNeeded(context);

        sDbUpdateWorker.execute(new Runnable() {
            @Override
            public void run() {
                // Backup data version flow:
                // saveSharePrefsToDb, -1 --> 0
                // updateDataWithEncrypted, 0 --> 1
                // updateLegacyBackupDataV1, 1 --> 2 (current)

                // Check Skr data upgrade to DB already
                if (!SkrSharedPrefs.getCheckSocialKeyRecoverySaveToDb(context)) {
                    saveSharePrefsToDb(context);
                }

                // SkrSharedPrefs.INSTANCE.getCurrentSkrBackupVersion(context)
                // SkrSharedPrefs.INSTANCE.setCurrentSkrBackupVersion(context, version)
                // If no set is -1, We add this value after
                // BACKUP_DATA_CLEAR_LEGACY_V1_VERSION (2)

                // If Skr data success upgrade to DB and current version is
                // BACKUP_DATA_DB_INIT_VERSION (0),
                // we update to BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION (1)
                if (SkrSharedPrefs.getCheckSocialKeyRecoverySaveToDb(context)
                        && SkrSharedPrefs.getCurrentSkrBackupVersion(context)
                        == BACKUP_DATA_DB_INIT_VERSION) {
                    updateDataWithEncrypted(context);
                }

                // If current version is BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION (1), we
                // update to BACKUP_DATA_CLEAR_LEGACY_V1_VERSION (2)
                if (SkrSharedPrefs.getCurrentSkrBackupVersion(context)
                        == BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION) {
                    updateLegacyBackupDataV1(context);
                }

                // Check whether update to latest backup data version
                if (SkrSharedPrefs.getCurrentSkrBackupVersion(context)
                        == BACKUP_DATA_CLEAR_LEGACY_V1_VERSION) {
                    LogUtil.logDebug(TAG, "It's last backup data version");
                } else {
                    LogUtil.logError(TAG, "Update to latest backup data version failed",
                            new IllegalStateException(
                                    "Update to latest backup data version failed"));
                }
            }
        });
    }

    // sharedPrefs to 0
    @WorkerThread
    private void saveSharePrefsToDb(@NonNull final Context context) {
        Objects.requireNonNull(context, "context is null");

        LogUtil.logDebug(TAG, "save to db");

        final CountDownLatch latch = new CountDownLatch(1);

        BackupDataStatusUtil.saveSharePrefsToDb(context, new DatabaseCompleteListener() {
            @Override
            public void onComplete() {
                // Clear deprecated SKR backup data from sharedPrefs
                LogUtil.logDebug(TAG, "clear deprecated Skr backup data");
                SkrSharedPrefs.clearDeprecatedSkrBackupData(context);

                // Put Skr data already upgrade to DB
                SkrSharedPrefs.putCheckSocialKeyRecoverySaveToDb(context, true);
                LogUtil.logInfo(TAG, "Save SharePrefs to database successfully");

                // Put backup version BACKUP_DATA_DB_INIT_VERSION (0) to sharedPrefs
                SkrSharedPrefs.putCurrentSkrBackupVersion(context, BACKUP_DATA_DB_INIT_VERSION);
                latch.countDown();
            }

            @Override
            public void onError(Exception exception) {
                LogUtil.logError(TAG, "Save SharePrefs to database failed, "
                        + "e=" + exception);
                latch.countDown();
            }
        });

        try {
            latch.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LogUtil.logError(TAG, "InterruptedException, e=" + e);
        }
    }

    // 0 to 1
    @WorkerThread
    private void updateDataWithEncrypted(@NonNull final Context context) {
        Objects.requireNonNull(context, "context is null");

        LogUtil.logDebug(TAG, "Update Data With Encrypted");

        final CountDownLatch latch = new CountDownLatch(1);

        BackupDataStatusUtil.updateDataWithEncrypted(context, new DatabaseCompleteListener() {
            @Override
            public void onComplete() {
                LogUtil.logInfo(TAG, "Update Data With Encrypted successfully");

                // Put backup version BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION (1) to
                // sharedPrefs
                SkrSharedPrefs.putCurrentSkrBackupVersion(
                        context, BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION);
                latch.countDown();
            }

            @Override
            public void onError(Exception exception) {
                LogUtil.logError(TAG, "Encrypted SocialKeyRecovery db failed, "
                        + "e=" + exception);
                latch.countDown();
            }
        });

        try {
            latch.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LogUtil.logError(TAG, "InterruptedException, e=" + e);
        }
    }

    // 1 to 2
    @WorkerThread
    private void updateLegacyBackupDataV1(@NonNull final Context context) {
        Objects.requireNonNull(context, "context is null");

        LogUtil.logDebug(TAG, "Update Legacy Backup Data V1");

        final CountDownLatch latch = new CountDownLatch(1);

        BackupDataStatusUtil.updateLegacyBackupDataV1(context, new DatabaseCompleteListener() {
            @Override
            public void onComplete() {
                LogUtil.logInfo(TAG, "Update Legacy Backup Data V1 successfully");

                // Put backup version BACKUP_DATA_CLEAR_LEGACY_V1_VERSION (2) to sharedPrefs
                SkrSharedPrefs.putCurrentSkrBackupVersion(
                        context, BACKUP_DATA_CLEAR_LEGACY_V1_VERSION);
                latch.countDown();
            }

            @Override
            public void onError(Exception exception) {
                LogUtil.logError(TAG, "Update Legacy Backup Data V1 failed, "
                        + "e=" + exception);
                latch.countDown();
            }
        });

        try {
            latch.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LogUtil.logError(TAG, "InterruptedException, e=" + e);
        }
    }

    private void checkAndScheduleJobServiceIfNeeded(@NonNull final Context context) {
        Objects.requireNonNull(context, "context is null");

        // Check isExodus first ?
        // Reschedule Backup Health Check JobService if needed
        BackupHealthCheckJobService.schedule(context);

        // Reschedule Backup Health Report JobService if needed
        BackupHealthReportJobService.schedule(context);

        // Cancel deprecated job id if needed
        cancelDeprecateJobIdIfNeeded(context);
    }

    public static void cancelDeprecateJobIdIfNeeded(@NonNull final Context context) {
        Objects.requireNonNull(context, "context is null");

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) {
            LogUtil.logError(TAG, "jobScheduler is null");
        } else {
            final List<JobInfo> allPendingJobs = jobScheduler.getAllPendingJobs();
            for (JobInfo jobInfo : allPendingJobs) {
                final int jobId = jobInfo.getId();
                if (JobIdManager.DEPRECATED_JOB_IDS.contains(jobId)) {
                    LogUtil.logDebug(TAG, "Cancel deprecated job id = " + jobId);
                    jobScheduler.cancel(jobId);
                }
            }
        }
    }
}
