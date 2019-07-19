package com.htc.wallet.skrsdk.drives;

import android.content.Context;
import android.support.annotation.NonNull;

import com.htc.wallet.skrsdk.drives.googledrive.GoogleDriveUtil;
import com.htc.wallet.skrsdk.drives.onedrive.OneDriveUtil;
import com.htc.wallet.skrsdk.util.LogUtil;

public class DriveUtilFactory {
    private static final String TAG = "DriveUtilFactory";

    private DriveUtilFactory() {
        throw new AssertionError();
    }

    public static DriveUtil getDriveUtil(@NonNull final Context context, final String address) {
        if (context == null) {
            throw new IllegalArgumentException("getDriveUtil(), context is null");
        }
        int type = DriveUtil.getServiceType(context);
        LogUtil.logDebug(TAG, "getDriveUtil(), type = " + type);
        final DriveUtil util;
        switch (type) {
            case DriveServiceType.undefined:
            case DriveServiceType.googleDrive:
                // google as default service
                util = new GoogleDriveUtil(context, address);
                break;
            case DriveServiceType.oneDrive:
                util = new OneDriveUtil();
                break;
            default:
                // google as default service
                util = new GoogleDriveUtil(context, address);
                break;
        }
        return util;
    }

    public static DriveUtil getOneDriveUtil() {
        return new OneDriveUtil();
    }
}
