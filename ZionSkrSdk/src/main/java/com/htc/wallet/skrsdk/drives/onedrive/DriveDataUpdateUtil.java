package com.htc.wallet.skrsdk.drives.onedrive;

import static com.htc.wallet.skrsdk.drives.onedrive.OneDriveDataUpdateWorker.WORK_TAG_ONE_DRIVE_SYNC_ONE_TIME;
import static com.htc.wallet.skrsdk.drives.onedrive.OneDriveDataUpdateWorker.WORK_TAG_ONE_DRIVE_SYNC_PERIODIC;

import android.content.Context;
import android.support.annotation.NonNull;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.common.util.concurrent.ListenableFuture;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;
import com.htc.wallet.skrsdk.util.ThreadUtil;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// Only for backup flow
public class DriveDataUpdateUtil {
    private static final String TAG = "DriveDataUpdateUtil";

    private final Context mContext;
    private final ThreadPoolExecutor mSingleThreadExecutor;

    public DriveDataUpdateUtil(@NonNull final Context context) {
        mContext = Objects.requireNonNull(context, "context is null");
        mSingleThreadExecutor = ThreadUtil.newFixedThreadPool(1, "drive-update-util");
    }

    public void startSyncDriveData() {
        final boolean isFirstTime = SkrSharedPrefs.getIsOneDriveDataHasUpdatedTheFirstTime(
                mContext);
        // Use work thread, prevent isWorkExisting() lock the UI thread.
        mSingleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                startSyncDriveDataImpl(isFirstTime);
            }
        });
    }

    private void startSyncDriveDataImpl(boolean isFirstTime) {
        LogUtil.logInfo(TAG, "startSyncDriveDataImpl(), isFirstTime=" + isFirstTime);

        if (isFirstTime) {

            // The first time we start sync, we will enqueue periodic work.
            // The work will first execute after 15 minutes.
            // Every time we start sync in this 15 minutes,
            // we will get "true" both of isFirstTime and isWorkExisting().
            if (isWorkExisting(WORK_TAG_ONE_DRIVE_SYNC_PERIODIC)) {
                LogUtil.logInfo(TAG, "isFirstTime, and work existing, skip");
                return;
            }

            // Periodic request, we set constraints network connected and battery not low
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build();
            WorkRequest workRequest = new PeriodicWorkRequest.Builder(
                    OneDriveDataUpdateWorker.class,
                    PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS,
                    PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS)
                    .addTag(WORK_TAG_ONE_DRIVE_SYNC_PERIODIC)
                    .setConstraints(constraints)
                    .build();
            WorkManager.getInstance().enqueue(workRequest);
        } else {
            // One Time request, only set network connected constraint
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            WorkRequest workRequest = new OneTimeWorkRequest.Builder(OneDriveDataUpdateWorker.class)
                    .addTag(WORK_TAG_ONE_DRIVE_SYNC_ONE_TIME)
                    .setInitialDelay(0, TimeUnit.MILLISECONDS)
                    .setConstraints(constraints)
                    .build();
            WorkManager.getInstance().enqueue(workRequest);
        }
    }

    private boolean isWorkExisting(@NonNull String tag) {
        Objects.requireNonNull(tag, "tag is null");
        ListenableFuture<List<WorkInfo>> future = WorkManager.getInstance().getWorkInfosByTag(tag);
        try {
            List<WorkInfo> list = future.get();
            return list != null && !list.isEmpty();
        } catch (ExecutionException e) {
            LogUtil.logError(TAG, "future.get() failed, ExecutionException cause=" + e.getCause());
        } catch (InterruptedException e) {
            LogUtil.logError(TAG, "future.get() failed, InterruptedException e=" + e);
        }

        // Some unexpected error was happened !!
        // We return true, prevent duplicate enqueue.
        return true;
    }

    public void cancelSyncDriveData() {
        cancelSyncDriveDataImpl();
    }

    private void cancelSyncDriveDataImpl() {
        WorkManager.getInstance().cancelAllWorkByTag(WORK_TAG_ONE_DRIVE_SYNC_PERIODIC);
        WorkManager.getInstance().cancelAllWorkByTag(WORK_TAG_ONE_DRIVE_SYNC_ONE_TIME);
    }
}
