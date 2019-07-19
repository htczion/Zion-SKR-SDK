package com.htc.wallet.skrsdk.messaging;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({MessageServiceType.FIREBASE, MessageServiceType.WHISPER, MessageServiceType.MULTI})
public @interface MessageServiceType {
    int FIREBASE = 1;
    int WHISPER = 2;
    int MULTI = FIREBASE | WHISPER;
}
