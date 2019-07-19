package com.htc.wallet.skrsdk.monitor;

import static com.htc.wallet.skrsdk.action.Action.KEY_BACKUP_VERSION;
import static com.htc.wallet.skrsdk.action.Action.KEY_CHECKSUM;
import static com.htc.wallet.skrsdk.action.Action.KEY_ENCRYPTED_SEED;
import static com.htc.wallet.skrsdk.action.Action.KEY_NAME;
import static com.htc.wallet.skrsdk.action.Action.KEY_PUBLIC_KEY;
import static com.htc.wallet.skrsdk.jobs.JobIdManager.JOB_ID_SKR_CHECK_BACKUP_VERSION;
import static com.htc.wallet.skrsdk.jobs.JobUtil.isJobExist;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.util.ArrayMap;

import com.htc.wallet.skrsdk.action.BackupHealthReportAction;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupSourceUtil;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CheckBackupVersionJobService extends JobService {
    private static final String TAG = "CheckBackupVersionJobService";
    // Set the flex time, because default flex time is equal to interval time.
    // If we only set interval time 24 hours, that next job will be run  between 0 to 48 hours.
    private static final long TIME_INTERVAL = DateUtils.DAY_IN_MILLIS;
    private static final long TIME_FLEX = DateUtils.HOUR_IN_MILLIS * 12;

    @Override
    public boolean onStartJob(final JobParameters params) {

        final Context context = this;

        BackupSourceUtil.getAllOK(context, new LoadListListener() {
            @Override
            public void onLoadFinished(
                    List<BackupSourceEntity> backupSourceEntityList,
                    List<BackupTargetEntity> backupTargetEntityList,
                    List<RestoreSourceEntity> restoreSourceEntityList,
                    List<RestoreTargetEntity> restoreTargetEntityList) {
                if (backupSourceEntityList == null) {
                    LogUtil.logError(TAG, "backupSourceEntityList is null");
                    // This is an unexpected error, needs reschedule
                    jobFinished(params, true);
                    return;
                }
                final AtomicInteger backupSourceCount =
                        new AtomicInteger(backupSourceEntityList.size());
                final AtomicBoolean cancelJob = new AtomicBoolean(true);
                if (backupSourceCount.get() == 0) {
                    // Cancel job
                    cancelJob();
                    LogUtil.logDebug(TAG, "There are no backupSource, "
                            + "not need to check backup version anymore");
                    jobFinished(params, false);
                }
                for (BackupSourceEntity backupSourceEntity : backupSourceEntityList) {
                    if (backupSourceEntity.isLegacyDataUpdatedV1()) {
                        if (backupSourceCount.decrementAndGet() == 0) {
                            if (cancelJob.get()) {
                                cancelJob();
                                LogUtil.logDebug(TAG, "There are no Legacy V1 backupSource, "
                                        + "not need to check backup version anymore");
                            }
                            jobFinished(params, false);
                        }
                    } else {
                        // Still have Legacy Backup Data V1, can't cancel this jobService
                        cancelJob.set(false);

                        Map<String, String> map = new ArrayMap<>();
                        // Verify the checksum in BackupHealthReportAction().sendInternal()
                        // because Bob can also report backup status
                        // without Amy's ask
                        map.put(KEY_CHECKSUM, backupSourceEntity.getCheckSum());
                        map.put(KEY_ENCRYPTED_SEED, backupSourceEntity.getSeed());
                        map.put(KEY_PUBLIC_KEY, backupSourceEntity.getPublicKey());
                        map.put(KEY_NAME, backupSourceEntity.getMyName());
                        // Check backup version
                        map.put(KEY_BACKUP_VERSION,
                                String.valueOf(backupSourceEntity.getVersion()));
                        BackupHealthReportAction backupHealthReportAction =
                                new BackupHealthReportAction();
                        backupHealthReportAction.setSendCompleteListener(
                                new BackupHealthReportAction.SendCompleteListener() {
                                    @Override
                                    public void onSendComplete() {
                                        if (backupSourceCount.decrementAndGet() == 0) {
                                            LogUtil.logDebug(TAG,
                                                    "Notify all Legacy Backup Data V1 "
                                                            + "finish, check again after 1 day");
                                            jobFinished(params, false);
                                        }
                                    }
                                });
                        backupHealthReportAction.send(
                                context,
                                backupSourceEntity.getFcmToken(),
                                backupSourceEntity.getWhisperPub(),
                                backupSourceEntity.getPushyToken(),
                                map);
                    }
                }
            }
        });

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        LogUtil.logDebug(TAG, "onStopJob");
        return false;
    }

    public static void schedule(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) {
            LogUtil.logError(TAG, "jobScheduler is null",
                    new IllegalStateException("jobScheduler is null"));
            return;
        }

        if (isJobExist(jobScheduler, JOB_ID_SKR_CHECK_BACKUP_VERSION)) {
            LogUtil.logDebug(TAG, "job already schedule, ignore it");
        } else {
            LogUtil.logInfo(TAG, "schedule new job");
            JobInfo jobInfo = new JobInfo.Builder(
                    JOB_ID_SKR_CHECK_BACKUP_VERSION,
                    new ComponentName(context, CheckBackupVersionJobService.class))
                    .setPeriodic(TIME_INTERVAL, TIME_FLEX)
                    .setPersisted(true)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .build();
            int result = jobScheduler.schedule(jobInfo);
            if (result != JobScheduler.RESULT_SUCCESS) {
                LogUtil.logError(TAG, "schedule failed");
            }
        }
    }

    private void cancelJob() {
        JobScheduler jobScheduler =
                (JobScheduler) getApplicationContext().getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            LogUtil.logInfo(TAG, "no more legacy backup data, cancel this jonService");
            jobScheduler.cancel(JOB_ID_SKR_CHECK_BACKUP_VERSION);
        }
    }
}
