package com.htc.wallet.skrsdk.drives;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({DriveServiceType.undefined, DriveServiceType.googleDrive, DriveServiceType.oneDrive})
public @interface DriveServiceType {
    int undefined = -1;
    int googleDrive = 1;
    int oneDrive = 2;
}
