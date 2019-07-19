package com.htc.wallet.skrsdk.keyserver;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface StatusCode {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TOKEN_INVALID_OR_EXPIRED, NOT_FOUND})
    @interface StatusCodeEnum {
    }

    int TOKEN_INVALID_OR_EXPIRED = 401;
    int NOT_FOUND = 404;
}
