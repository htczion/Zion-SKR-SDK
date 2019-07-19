package com.htc.wallet.skrsdk.drives.onedrive;

import android.app.IntentService;
import android.content.Intent;

import com.htc.wallet.skrsdk.drives.DriveCallback;
import com.htc.wallet.skrsdk.drives.DriveUtil;
import com.htc.wallet.skrsdk.drives.DriveUtilFactory;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;

// OneDrive only

// OneDrive may not sync data immediately
// Thus we should schedule a time to download the file to local
// And check the local file next time
@Deprecated
public class DriveDataUpdateService extends IntentService {

    public static final String TAG = "DriveDataUpdateService";

    public DriveDataUpdateService() {
        super("DriveDataUpdateService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final DriveUtil util = DriveUtilFactory.getOneDriveUtil();
        util.loadUUIDHash(
                new DriveCallback<String>() {
                    @Override
                    public void onComplete(String message) {
                        LogUtil.logInfo(TAG, "loadUUIDHash() completed");
                        DriveUtil.writeUUIDHashOnLocal(message);
                        setOneDriveDataHasUpdatedTheFirstTime();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (e instanceof OneDriveUtilException) {
                            LogUtil.logError(
                                    TAG, "loadUUIDHash() failed, e = " + e);
                            DriveUtil.deleteUUIDHashLocalFile();
                        }
                    }
                });

        // We use partial uuidHash as trust contacts file name prefix
        final String uuidHash = PhoneUtil.getSKRIDHash(getBaseContext());
        final String fileNamePrefix = DriveUtil.getFilePrefix(uuidHash);
        util.loadTrustContacts(fileNamePrefix, new DriveCallback<String>() {
            @Override
            public void onComplete(String message) {
                LogUtil.logInfo(TAG, "loadTrustContacts() completed");
                DriveUtil.writeTrustContactsOnLocal(fileNamePrefix, message);
            }

            @Override
            public void onFailure(Exception e) {
                if (e instanceof OneDriveUtilException) {
                    LogUtil.logError(TAG, "loadTrustContacts() failed, e=" + e);
                    DriveUtil.deleteTrustContactsLocalFile();
                }
            }
        });
    }

    private void setOneDriveDataHasUpdatedTheFirstTime() {
        LogUtil.logInfo(TAG, "setOneDriveDataHasUpdatedTheFirstTime, "
                + "time: " + System.currentTimeMillis());
        SkrSharedPrefs.putIsOneDriveDataHasUpdatedTheFirstTime(this, false);
    }
}
