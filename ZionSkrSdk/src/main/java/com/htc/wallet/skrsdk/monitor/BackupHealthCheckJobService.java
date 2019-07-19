package com.htc.wallet.skrsdk.monitor;

import static com.htc.wallet.skrsdk.jobs.JobIdManager.JOB_ID_SKR_HEALTH_CHECK;
import static com.htc.wallet.skrsdk.jobs.JobUtil.isJobExist;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.SOCIAL_KEY_RECOVERY_ACTIVE_THRESHOLD;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;

import com.htc.wallet.skrsdk.backup.SocialBackupIntroductionActivity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupTargetUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.NotificationUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/*
 * Deprecated on 2018/11/23, it brought out the iOS devices show up notifications frequently.
 * Change to check and adjust BackupTarget 2018/11/27, OK to NO_RESPONSE
 * */
public class BackupHealthCheckJobService extends JobService {
    private static final String TAG = "BackupHealthCheckJobService";
    // Set the flex time, because default flex time is equal to interval time.
    // If we only set interval time 24 hours, that next job will be run  between 0 to 48 hours.
    // Change to 3 days, 2019/03/21
    private static final long TIME_INTERVAL_NO_RESPONSE = DateUtils.DAY_IN_MILLIS * 3;
    private static final long TIME_INTERVAL_CHECK = DateUtils.DAY_IN_MILLIS;
    private static final long TIME_FLEX_CHECK = DateUtils.HOUR_IN_MILLIS * 12;
    private static final int TIMEOUT = 30; // seconds

    @Override
    public boolean onStartJob(final JobParameters params) {
        LogUtil.logDebug(TAG, "onStartJob");

        // Check and adjust status
        final long currentTime = System.currentTimeMillis();
        BackupTargetUtil.getAllOK(this, new LoadListListener() {
            @Override
            public void onLoadFinished(
                    List<BackupSourceEntity> backupSourceEntityList,
                    List<BackupTargetEntity> backupTargetEntityList,
                    List<RestoreSourceEntity> restoreSourceEntityList,
                    List<RestoreTargetEntity> restoreTargetEntityList) {
                if (backupTargetEntityList == null) {
                    LogUtil.logError(TAG, "backupTargetEntityList is null");
                    jobFinished(params, false);
                    return;
                }

                // If there is nothing to check, just finish
                final int targetListSize = backupTargetEntityList.size();
                if (targetListSize == 0) {
                    LogUtil.logDebug(TAG, "No OK backupTarget, not need to check");
                    jobFinished(params, false);
                    return;
                }

                // Use CountDownLatch, because each BackupTargetUtil.update work in new
                // thread
                // We must wait all things be done before call jobFinished
                final CountDownLatch latch = new CountDownLatch(targetListSize);
                for (BackupTargetEntity target : backupTargetEntityList) {
                    if (currentTime - target.getLastCheckedTime()
                            > TIME_INTERVAL_NO_RESPONSE) {
                        LogUtil.logDebug(TAG, target.getName() + "'s last checked time"
                                + " expired, update to status no response");
                        target.updateStatusToNoResponse();
                        BackupTargetUtil.update(getBaseContext(), target,
                                new DatabaseCompleteListener() {
                                    @Override
                                    public void onComplete() {
                                        // Check Social Key Recovery active
                                        checkSocialKeyRecoveryActive();
                                        latch.countDown();
                                    }

                                    @Override
                                    public void onError(Exception exception) {
                                        LogUtil.logError(
                                                TAG, "update error, e= " + exception);
                                        latch.countDown();
                                    }
                                });
                    } else {
                        latch.countDown();
                    }
                }

                try {
                    latch.await(TIMEOUT, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    LogUtil.logDebug(TAG, "InterruptedException e = " + e);
                }

                jobFinished(params, false);
            }
        });

        return true;
    }

    private void checkSocialKeyRecoveryActive() {
        BackupTargetUtil.getAllOK(getBaseContext(), new LoadListListener() {
            @Override
            public void onLoadFinished(
                    List<BackupSourceEntity> backupSourceEntityList,
                    List<BackupTargetEntity> backupTargetEntityList,
                    List<RestoreSourceEntity> restoreSourceEntityList,
                    List<RestoreTargetEntity> restoreTargetEntityList) {
                // Fixed issue, use wrong list, may get null object reference
                if (backupTargetEntityList == null) {
                    LogUtil.logError(TAG, "backupTargetEntityList is null");
                    return;
                }

                if (backupTargetEntityList.size() < SOCIAL_KEY_RECOVERY_ACTIVE_THRESHOLD
                        && SkrSharedPrefs.getShouldShowNotActiveNotification(getBaseContext())) {
                    LogUtil.logInfo(TAG, "Show notification to notify user SKR not active");
                    Intent intent =
                            new Intent(getBaseContext(), SocialBackupIntroductionActivity.class);
                    NotificationUtil.showBackupHealthProblemNotification(getBaseContext(), intent);
                    SkrSharedPrefs.putShouldShowNotActiveNotification(getBaseContext(), false);
                }
            }
        });
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

        if (isJobExist(jobScheduler, JOB_ID_SKR_HEALTH_CHECK)) {
            LogUtil.logDebug(TAG, "job already schedule, ignore it");
        } else {
            LogUtil.logInfo(TAG, "schedule new job");
            JobInfo jobInfo = new JobInfo.Builder(JOB_ID_SKR_HEALTH_CHECK,
                    new ComponentName(context, BackupHealthCheckJobService.class))
                    .setPeriodic(TIME_INTERVAL_CHECK, TIME_FLEX_CHECK)
                    .setPersisted(true)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .build();
            int result = jobScheduler.schedule(jobInfo);
            if (result != JobScheduler.RESULT_SUCCESS) {
                LogUtil.logError(TAG, "schedule failed");
            }
        }
    }
}
