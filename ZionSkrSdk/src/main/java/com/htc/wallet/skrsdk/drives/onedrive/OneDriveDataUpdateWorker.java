package com.htc.wallet.skrsdk.drives.onedrive;

import android.content.Context;
import android.support.annotation.NonNull;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.htc.wallet.skrsdk.drives.DriveCallback;
import com.htc.wallet.skrsdk.drives.DriveUtil;
import com.htc.wallet.skrsdk.drives.DriveUtilFactory;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class OneDriveDataUpdateWorker extends Worker {

    private static final String TAG = "OneDriveDataUpdateWorker";

    static final String WORK_TAG_ONE_DRIVE_SYNC_PERIODIC =
            "work_tag_one_drive_sync_periodic";
    static final String WORK_TAG_ONE_DRIVE_SYNC_ONE_TIME =
            "work_tag_one_drive_sync_one_time";

    private static final long TIMEOUT = 60L; // 60 sec

    public OneDriveDataUpdateWorker(@NonNull Context context,
            @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        final CountDownLatch latch = new CountDownLatch(2);

        DriveUtil util = DriveUtilFactory.getOneDriveUtil();
        util.loadUUIDHash(new DriveCallback<String>() {
            @Override
            public void onComplete(String message) {
                LogUtil.logInfo(TAG, "loadUUIDHash() completed");
                DriveUtil.writeUUIDHashOnLocal(message);
                setOneDriveDataHasUpdatedTheFirstTime();
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                if (e instanceof OneDriveUtilException) {
                    LogUtil.logError(TAG, "loadUUIDHash() failed, e=" + e);
                    DriveUtil.deleteUUIDHashLocalFile();
                }
                latch.countDown();
            }
        });

        // We use partial uuidHash as trust contacts file name prefix
        final String uuidHash = PhoneUtil.getSKRIDHash(getApplicationContext());
        final String fileNamePrefix = DriveUtil.getFilePrefix(uuidHash);
        util.loadTrustContacts(fileNamePrefix, new DriveCallback<String>() {
            @Override
            public void onComplete(String message) {
                LogUtil.logInfo(TAG, "loadTrustContacts() completed");
                DriveUtil.writeTrustContactsOnLocal(fileNamePrefix, message);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                if (e instanceof OneDriveUtilException) {
                    LogUtil.logError(TAG, "loadTrustContacts() failed, e=" + e);
                    DriveUtil.deleteTrustContactsLocalFile();
                }
                latch.countDown();
            }
        });

        try {
            latch.await(TIMEOUT, TimeUnit.SECONDS);
            return Result.success();
        } catch (InterruptedException e) {
            LogUtil.logError(TAG, "doWork() failed, InterruptedException e=" + e);
            return Result.failure();
        }
    }

    private void setOneDriveDataHasUpdatedTheFirstTime() {
        boolean isPeriodicWork = getTags().contains(WORK_TAG_ONE_DRIVE_SYNC_PERIODIC);
        if (isPeriodicWork) {
            LogUtil.logInfo(TAG, "setOneDriveDataHasUpdatedTheFirstTime, "
                    + "time: " + System.currentTimeMillis());
            SkrSharedPrefs.putIsOneDriveDataHasUpdatedTheFirstTime(getApplicationContext(), false);
        }
    }
}
