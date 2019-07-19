package com.htc.wallet.skrsdk.drives;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({UUIDStatusType.ERROR, UUIDStatusType.MATCH, UUIDStatusType.NOT_MATCH})
public @interface UUIDStatusType {
    int ERROR = -1;
    int MATCH = 1;
    int NOT_MATCH = 2;
}