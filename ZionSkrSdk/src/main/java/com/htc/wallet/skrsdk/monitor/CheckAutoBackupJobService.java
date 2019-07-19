package com.htc.wallet.skrsdk.monitor;

import static com.htc.wallet.skrsdk.action.Action.KEY_CHECKSUM;
import static com.htc.wallet.skrsdk.action.Action.KEY_ENCRYPTED_SEED;
import static com.htc.wallet.skrsdk.action.Action.KEY_NAME;
import static com.htc.wallet.skrsdk.action.Action.KEY_PUBLIC_KEY;
import static com.htc.wallet.skrsdk.jobs.JobIdManager.JOB_ID_SKR_CHECK_AUTO_BACKUP;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_REQUEST_AUTO_BACKUP;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.app.job.JobWorkItem;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.ArrayMap;

import com.htc.wallet.skrsdk.action.BackupHealthReportAction;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupSourceUtil;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

// After restore, we have temporary status BACKUP_SOURCE_STATUS_REQUEST_AUTO_BACKUP to wait new
// partial seed
// Thus, this JobService is for check new seed has came in 1 hour, or notify Amy and delete this
// temporary data
public class CheckAutoBackupJobService extends JobService {
    private static final String TAG = "CheckAutoBackupJobService";
    private static final String THREAD_NAME = TAG;
    private static final String JOB_KEY_EMAIL_HASH = "job_key_email_hash";
    private static final String JOB_KEY_UUID_HASH = "job_key_uuid_hash";
    private static final String JOB_KEY_TIMESTAMP = "job_key_timestamp";
    private static final String JOB_KEY_JOB_WORK_ITEM = "job_key_job_work_item";
    private static final String FAKE_CHECKSUM = "fake_checksum";
    private static final String FAKE_ENC_SEED = "fake_enc_seed";
    private static final int MSG_DEQUEUE = 1001;
    private static final int MSG_CHECK = 1002;
    private static final int TIMEOUT = 10; // seconds
    private static final int MIN_HASH_LENGTH = 5;
    private static final long DEFAULT_VALUE = -1;

    static final long REQUEST_AUTO_BACKUP_VALIDITY_TIME = DateUtils.HOUR_IN_MILLIS;

    private volatile HandlerThread mHandlerThread;
    private volatile Handler mHandler;
    private final AtomicBoolean mIsJobServiceStopped = new AtomicBoolean(false);

    @Override
    public boolean onStartJob(final JobParameters params) {

        // The next onStartJob after onStopJob will run on a new instance,
        // we still set mIsJobServiceStopped false here
        mIsJobServiceStopped.set(false);

        clearHandlerThread();
        synchronized (CheckAutoBackupJobService.class) {
            mHandlerThread = new HandlerThread(THREAD_NAME);
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    switch (msg.what) {
                        case MSG_DEQUEUE:
                            final JobWorkItem jobWorkItem = params.dequeueWork();
                            if (jobWorkItem == null) {
                                LogUtil.logInfo(TAG, "No more work, finish");
                                // Set is jobService stopped true
                                mIsJobServiceStopped.set(true);
                                // Clear Handler and HandlerThread
                                clearHandlerThread();
                                // Finish this jobService
                                jobFinished(params, false);
                            } else {
                                Intent intent = jobWorkItem.getIntent();
                                if (intent == null) {
                                    LogUtil.logError(TAG, "intent is null");
                                    completeWork(params, jobWorkItem);
                                    sendEmptyMessage(MSG_DEQUEUE);
                                } else {
                                    final String emailHash =
                                            intent.getStringExtra(JOB_KEY_EMAIL_HASH);
                                    final String uuidHash =
                                            intent.getStringExtra(JOB_KEY_UUID_HASH);
                                    final long timestamp =
                                            intent.getLongExtra(
                                                    JOB_KEY_TIMESTAMP, DEFAULT_VALUE);

                                    Bundle bundle = new Bundle();
                                    bundle.putString(JOB_KEY_EMAIL_HASH, emailHash);
                                    bundle.putString(JOB_KEY_UUID_HASH, uuidHash);
                                    bundle.putLong(JOB_KEY_TIMESTAMP, timestamp);
                                    bundle.putParcelable(JOB_KEY_JOB_WORK_ITEM, jobWorkItem);

                                    Message message = Message.obtain();
                                    message.what = MSG_CHECK;
                                    message.setData(bundle);

                                    // emailHash, uuidHash and timeStamp check at MSG_CHECK
                                    // if timestamp use defaultValue, will trigger MSG_CHECK
                                    // immediately
                                    final long waitTime =
                                            (timestamp + REQUEST_AUTO_BACKUP_VALIDITY_TIME)
                                                    - System.currentTimeMillis();
                                    if (waitTime > 0) {
                                        LogUtil.logInfo(TAG, "check delay " + waitTime);
                                        sendMessageDelayed(message, waitTime);
                                    } else {
                                        LogUtil.logInfo(TAG, "expire, check now");
                                        sendMessage(message);
                                    }
                                }
                            }
                            break;
                        case MSG_CHECK:
                            final Bundle bundle = msg.getData();
                            if (bundle == null) {
                                LogUtil.logError(TAG, "bundle is null");
                                sendEmptyMessage(MSG_DEQUEUE);
                                break;
                            }

                            final JobWorkItem workItem =
                                    bundle.getParcelable(JOB_KEY_JOB_WORK_ITEM);
                            if (workItem == null) {
                                LogUtil.logError(TAG, "workItem is null");
                                sendEmptyMessage(MSG_DEQUEUE);
                                break;
                            }

                            final String emailHash = bundle.getString(JOB_KEY_EMAIL_HASH);
                            if (TextUtils.isEmpty(emailHash)) {
                                LogUtil.logError(TAG, "emailHash is null or empty");
                                completeWork(params, workItem);
                                sendEmptyMessage(MSG_DEQUEUE);
                                break;
                            }

                            final String uuidHash = bundle.getString(JOB_KEY_UUID_HASH);
                            if (TextUtils.isEmpty(uuidHash)) {
                                LogUtil.logError(TAG, "uuidHash is null or empty");
                                completeWork(params, workItem);
                                sendEmptyMessage(MSG_DEQUEUE);
                                break;
                            }

                            final long timestamp =
                                    bundle.getLong(JOB_KEY_TIMESTAMP, DEFAULT_VALUE);
                            if (timestamp == DEFAULT_VALUE) {
                                LogUtil.logError(TAG, "incorrect timestamp");
                                completeWork(params, workItem);
                                sendEmptyMessage(MSG_DEQUEUE);
                                break;
                            }

                            // Check auto backup
                            // Get BackupSourceEntity and check it's status
                            // If still BACKUP_SOURCE_STATUS_REQUEST_AUTO_BACKUP, then send
                            // error back and remove it
                            BackupSourceEntity backupSourceEntity = getBackupSource(
                                    getApplicationContext(), emailHash, uuidHash);
                            if (backupSourceEntity == null) {
                                LogUtil.logInfo(TAG,
                                        "backupSourceEntity is null, may remove by Amy");
                            } else if (backupSourceEntity.compareStatus(
                                    BACKUP_SOURCE_STATUS_REQUEST_AUTO_BACKUP)) {
                                LogUtil.logInfo(TAG, "Request auto backup expired, "
                                        + "send error health report back and remove it");
                                // Send error health report back to Amy
                                sendErrorHealthReport(
                                        getApplicationContext(),
                                        backupSourceEntity.getFcmToken(),
                                        backupSourceEntity.getWhisperPub(),
                                        backupSourceEntity.getPushyToken(),
                                        backupSourceEntity.getPublicKey(),
                                        backupSourceEntity.getMyName());
                                // Remove it
                                BackupSourceUtil.remove(
                                        getApplicationContext(), emailHash, uuidHash);
                            } else {
                                LogUtil.logInfo(TAG, "Not at request auto backup status, "
                                        + "not need to check afterward");
                            }
                            LogUtil.logDebug(TAG, "check"
                                    + ", emailHash=" + emailHash.substring(0, 5).hashCode()
                                    + ", uuidHash=" + uuidHash.substring(0, 5).hashCode());
                            completeWork(params, workItem);
                            sendEmptyMessage(MSG_DEQUEUE);
                            break;
                        default:
                            LogUtil.logError(TAG, "Unknown message = " + msg.what,
                                    new IllegalStateException());
                    }
                }
            };
        }
        mHandler.sendEmptyMessage(MSG_DEQUEUE);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        LogUtil.logDebug(TAG, "onStopJob");
        // Set is jobService stopped true
        mIsJobServiceStopped.set(true);
        // Clear Handler and HandlerThread
        clearHandlerThread();
        // Unexpected stop, reschedule it
        return true;
    }

    private void clearHandlerThread() {
        synchronized (CheckAutoBackupJobService.class) {
            if (mHandler != null) {
                mHandler.removeCallbacksAndMessages(null);
                mHandler = null;
            }
            if (mHandlerThread != null) {
                mHandlerThread.quitSafely();
                mHandlerThread = null;
            }
        }
    }

    private void completeWork(
            @NonNull final JobParameters params, @NonNull final JobWorkItem jobWorkItem) {
        Objects.requireNonNull(params, "params is null");
        Objects.requireNonNull(jobWorkItem, "jobWorkItem is null");

        if (mIsJobServiceStopped.get()) {
            LogUtil.logDebug(TAG, "JobService is stopped, skip completeWork()");
        } else {
            params.completeWork(jobWorkItem);
        }
    }

    public static void enqueue(
            @NonNull final Context context,
            @NonNull final String emailHash,
            @NonNull final String uuidHash) {
        // Check arguments
        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(emailHash) || emailHash.length() < MIN_HASH_LENGTH) {
            LogUtil.logError(TAG, "emailHash is incorrect", new IllegalArgumentException());
            return;
        }
        if (TextUtils.isEmpty(uuidHash) || uuidHash.length() < MIN_HASH_LENGTH) {
            LogUtil.logError(TAG, "uuidHash is incorrect", new IllegalArgumentException());
            return;
        }
        // Get JobScheduler
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) {
            LogUtil.logError(TAG, "jobScheduler is null",
                    new IllegalStateException("jobScheduler is null"));
            return;
        }

        // JobWorkItem's intent
        Intent intent = new Intent();
        intent.putExtra(JOB_KEY_EMAIL_HASH, emailHash);
        intent.putExtra(JOB_KEY_UUID_HASH, uuidHash);
        intent.putExtra(JOB_KEY_TIMESTAMP, System.currentTimeMillis());

        // JobInfo
        JobInfo jobInfo = new JobInfo.Builder(JOB_ID_SKR_CHECK_AUTO_BACKUP,
                new ComponentName(context, CheckAutoBackupJobService.class))
                .setOverrideDeadline(REQUEST_AUTO_BACKUP_VALIDITY_TIME)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build();
        // Enqueue
        int result = jobScheduler.enqueue(jobInfo, new JobWorkItem(intent));
        if (result == JobScheduler.RESULT_SUCCESS) {
            LogUtil.logInfo(TAG, "enqueue new job");
        } else {
            LogUtil.logError(TAG, "enqueue failed");
        }
        LogUtil.logDebug(TAG, "enqueue "
                + ", emailHash = " + emailHash.substring(0, 5).hashCode()
                + ", uuidHash = " + uuidHash.substring(0, 5).hashCode());
    }

    @WorkerThread
    @Nullable
    private BackupSourceEntity getBackupSource(Context context, String emailHash, String uuidHash) {
        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(emailHash)) {
            LogUtil.logError(TAG, "emailHash is null or empty", new IllegalStateException());
            return null;
        }
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logError(TAG, "uuidHash is null or empty", new IllegalStateException());
            return null;
        }
        final AtomicReference<BackupSourceEntity> backupSourceReference = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        BackupSourceUtil.getWithUUIDHash(context, emailHash, uuidHash, new LoadDataListener() {
            @Override
            public void onLoadFinished(
                    BackupSourceEntity backupSourceEntity,
                    BackupTargetEntity backupTargetEntity,
                    RestoreSourceEntity restoreSourceEntity,
                    RestoreTargetEntity restoreTargetEntity) {
                backupSourceReference.set(backupSourceEntity);
                latch.countDown();
            }
        });
        try {
            latch.await(TIMEOUT, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            LogUtil.logWarning(TAG, "InterruptedException e = " + e);
        }
        return backupSourceReference.get();
    }

    @WorkerThread
    static void sendErrorHealthReport(
            @NonNull Context context,
            String fcmToken,
            String whisperPub,
            String pushyToken,
            String publicKey,
            String myName) {
        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(fcmToken)) {
            LogUtil.logError(TAG, "fcmToken is null or empty");
            return;
        }
        if (TextUtils.isEmpty(publicKey)) {
            LogUtil.logError(TAG, "publicKey is null or empty");
            return;
        }
        if (TextUtils.isEmpty(myName)) {
            LogUtil.logError(TAG, "myName is null or empty");
            return;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        Map<String, String> msgToSend = new ArrayMap<>();
        msgToSend.put(KEY_CHECKSUM, FAKE_CHECKSUM);
        msgToSend.put(KEY_ENCRYPTED_SEED, FAKE_ENC_SEED);
        msgToSend.put(KEY_PUBLIC_KEY, publicKey);
        msgToSend.put(KEY_NAME, myName);
        BackupHealthReportAction backupHealthReportAction = new BackupHealthReportAction();
        backupHealthReportAction.setSendCompleteListener(
                new BackupHealthReportAction.SendCompleteListener() {
                    @Override
                    public void onSendComplete() {
                        latch.countDown();
                    }
                });
        backupHealthReportAction.send(context, fcmToken, whisperPub, pushyToken, msgToSend);
        try {
            latch.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LogUtil.logDebug(TAG, "InterruptedException e = " + e);
        }
    }
}
