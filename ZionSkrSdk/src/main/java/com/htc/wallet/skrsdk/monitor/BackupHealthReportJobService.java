package com.htc.wallet.skrsdk.monitor;

import static com.htc.wallet.skrsdk.jobs.JobIdManager.JOB_ID_SKR_HEALTH_REPORT;
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

import com.htc.wallet.skrsdk.action.Action;
import com.htc.wallet.skrsdk.action.BackupHealthReportAction;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.Objects;

public class BackupHealthReportJobService extends JobService {
    private static final String TAG = "BackupHealthReportJobService";
    // Set the flex time, because default flex time is equal to interval time.
    // If we only set interval time 24 hours, that next job will be run  between 0 to 48 hours.
    private static final long TIME_INTERVAL_HEALTH_REPORT = DateUtils.DAY_IN_MILLIS;
    private static final long TIME_FLEX_HEALTH_REPORT = DateUtils.HOUR_IN_MILLIS * 12;

    @Override
    public boolean onStartJob(final JobParameters params) {
        LogUtil.logDebug(TAG, "onStartJob");
        BackupHealthReportAction backupHealthReportAction = new BackupHealthReportAction();
        backupHealthReportAction.setSendCompleteListener(
                new BackupHealthReportAction.SendCompleteListener() {
                    @Override
                    public void onSendComplete() {
                        jobFinished(params, false);
                    }
                });
        backupHealthReportAction.send(
                this,
                Action.MULTI_TOKEN,
                Action.MULTI_WHISPER_PUB,
                Action.MULTI_PUSHY_TOKEN,
                new ArrayMap<String, String>());
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

        if (isJobExist(jobScheduler, JOB_ID_SKR_HEALTH_REPORT)) {
            LogUtil.logDebug(TAG, "job already schedule, ignore it");
        } else {
            LogUtil.logInfo(TAG, "schedule new job");
            JobInfo jobInfo = new JobInfo.Builder(JOB_ID_SKR_HEALTH_REPORT,
                    new ComponentName(context, BackupHealthReportJobService.class))
                    .setPeriodic(TIME_INTERVAL_HEALTH_REPORT, TIME_FLEX_HEALTH_REPORT)
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
