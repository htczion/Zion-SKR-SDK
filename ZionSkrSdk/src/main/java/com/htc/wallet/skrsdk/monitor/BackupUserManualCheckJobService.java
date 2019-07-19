package com.htc.wallet.skrsdk.monitor;

import static com.htc.wallet.skrsdk.jobs.JobIdManager.JOB_ID_SKR_USER_MANUAL_CHECK;
import static com.htc.wallet.skrsdk.jobs.JobUtil.isJobExist;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_TRIGGER_BROADCAST;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;

import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupTargetUtil;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BackupUserManualCheckJobService extends JobService {
    private static final String TAG = "BackupUserManualCheckJobService";
    private static final String THREAD_NAME = TAG;
    private static final String KEY_CHECK_TIME = "key_check_time";
    private static final long WAIT_TIME = 10 * DateUtils.MINUTE_IN_MILLIS;
    private static final Object LOCK = new Object();
    private volatile HandlerThread mHandlerThread;
    private volatile Handler mHandler;

    @Override
    public boolean onStartJob(final JobParameters params) {
        LogUtil.logDebug(TAG, "onStartJob");
        if (params == null) {
            LogUtil.logError(TAG, "params is null", new IllegalStateException());
            return false;
        }
        // JobParameters.getExtras() will never be null, default is PersistableBundle.EMPTY from
        // framework
        PersistableBundle bundle = params.getExtras();
        final long checkTime = bundle.getLong(KEY_CHECK_TIME);

        clearHandlerThread();
        synchronized (LOCK) {
            mHandlerThread = new HandlerThread(THREAD_NAME);
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                LogUtil.logInfo(TAG, "Check no response");
                BackupTargetUtil.getAllOK(getApplicationContext(), new LoadListListener() {
                    @Override
                    public void onLoadFinished(
                            List<BackupSourceEntity> backupSourceEntityList,
                            List<BackupTargetEntity> backupTargetEntityList,
                            List<RestoreSourceEntity> restoreSourceEntityList,
                            List<RestoreTargetEntity> restoreTargetEntityList) {
                        if (backupTargetEntityList == null) {
                            LogUtil.logError(TAG, "backupTargetEntityList is null",
                                    new IllegalStateException());
                            clearHandlerThread();
                            jobFinished(params, false);
                            return;
                        }

                        // Collect no response contacts
                        List<BackupTargetEntity> noResponses = new ArrayList<>();
                        for (BackupTargetEntity target : backupTargetEntityList) {
                            // Now is after "checkTime" about 10 minutes
                            // If any backup's last checked time before "checkTime",
                            // we should update them to status no response
                            if (target.getLastCheckedTime() < checkTime) {
                                noResponses.add(target);
                            }
                        }

                        LogUtil.logInfo(TAG, "No response = " + noResponses.size());
                        if (noResponses.isEmpty()) {
                            LogUtil.logDebug(TAG, "Without no response, jobFinished");
                            clearHandlerThread();
                            jobFinished(params, false);
                            return;
                        }

                        // TODO: Notification user ?
                        // If there are some no response contacts
                        for (int i = 0; i < noResponses.size(); i++) {
                            final BackupTargetEntity target = noResponses.get(i);
                            // Check if the last element to update
                            final boolean isLastElement = (i == noResponses.size() - 1);
                            // Update status to no response
                            target.updateStatusToNoResponse();
                            BackupTargetUtil.update(getApplicationContext(), target,
                                    new DatabaseCompleteListener() {
                                        @Override
                                        public void onComplete() {
                                            LogUtil.logDebug(TAG, "Update "
                                                    + target.getName() + " to no response");
                                            if (isLastElement) {
                                                // Notify UI to update
                                                LocalBroadcastManager localBroadcastManager =
                                                        LocalBroadcastManager.getInstance(
                                                                getApplicationContext());
                                                Intent intent =
                                                        new Intent(ACTION_TRIGGER_BROADCAST);
                                                localBroadcastManager.sendBroadcast(intent);
                                                LogUtil.logDebug(TAG, "Last element, jobFinished");
                                                clearHandlerThread();
                                                jobFinished(params, false);
                                            }
                                        }

                                        @Override
                                        public void onError(Exception exception) {
                                            LogUtil.logError(TAG,
                                                    "update() failed, e=" + exception);
                                        }
                                    });
                        }
                    }
                });
            }
        };

        if (System.currentTimeMillis() > (checkTime + WAIT_TIME)) {
            LogUtil.logInfo(TAG, "Current time is too late, check immediately");
            mHandler.post(runnable);
        } else {
            mHandler.postDelayed(runnable, (checkTime + WAIT_TIME) - System.currentTimeMillis());
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        LogUtil.logDebug(TAG, "onStopJob");
        clearHandlerThread();
        // Unexpected stop, reschedule it
        return true;
    }

    private void clearHandlerThread() {
        synchronized (LOCK) {
            if (mHandler != null) {
                mHandler.removeCallbacksAndMessages(null);
                mHandler = null;
            }
            if (mHandlerThread != null) {
                mHandlerThread.quit();
                mHandlerThread = null;
            }
        }
    }

    public static void schedule(@NonNull final Context context) {
        Objects.requireNonNull(context, "context is null");
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) {
            LogUtil.logError(TAG, "jobScheduler is null",
                    new IllegalStateException("jobScheduler is null"));
            return;
        }

        if (isJobExist(jobScheduler, JOB_ID_SKR_USER_MANUAL_CHECK)) {
            LogUtil.logInfo(TAG, "job already enqueue, ignore it");
        } else {
            LogUtil.logInfo(TAG, "schedule new job");
            PersistableBundle bundle = new PersistableBundle();
            bundle.putLong(KEY_CHECK_TIME, System.currentTimeMillis());
            JobInfo jobInfo = new JobInfo.Builder(JOB_ID_SKR_USER_MANUAL_CHECK,
                    new ComponentName(context, BackupUserManualCheckJobService.class))
                    .setOverrideDeadline(WAIT_TIME)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .setExtras(bundle)
                    .build();
            int result = jobScheduler.schedule(jobInfo);
            if (result != JobScheduler.RESULT_SUCCESS) {
                LogUtil.logError(TAG, "schedule failed");
            }
        }
    }
}
