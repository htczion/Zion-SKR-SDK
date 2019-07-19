package com.htc.wallet.skrsdk.demoapp.util;

import static android.content.Context.ACTIVITY_SERVICE;

import android.app.ActivityManager;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.htc.htcwalletsdk.Export.RESULT;
import com.htc.htcwalletsdk.Protect.ISdkProtector;
import com.htc.wallet.skrsdk.demoapp.DemoApplication;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class SdkErrorHandler implements ISdkProtector {
    private static final String TAG = "SdkErrorHandler";

    private static final String TRY_OFTEN_ACTIVITY_ON_CREATE = "UITryOftenAct.Activity.onCreate";
    private static final String TRY_OFTEN_ACTIVITY_ON_CLICK = "UITryOftenAct.Button.onClick";
    private static final String TRY_OFTEN_ACTIVITY_ON_BACK_PRESSED = "UITryOftenAct.Button.onBackPressed";

    private Context mContext;
    private final AtomicBoolean mShouldClearAll = new AtomicBoolean(false);

    SdkErrorHandler(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        mContext = context.getApplicationContext();
    }

    @Override
    public int onErrorFeedback(int errorCode, int param1, String param2) {
        Log.d(TAG, "onErrorFeedback(" + errorCode + ", " + param1 + ", " + param2 + ")");
        return RESULT.SUCCESS;
    }

    @Override
    public int onNotify(NotifyType event, int iData, String strData, byte[] byteData) {
        Log.d(TAG, "onNotify(" + event + ", " + iData + ", " + strData + ", " +
                Arrays.toString(byteData) + ")");

        switch (event) {
            case ALARMEVENT:
                break;
            case UIEVENT:
                if (iData != RESULT.E_TEEKM_RETRY_RESET || TextUtils.isEmpty(strData)) {
                    break;
                }

                if (strData.equals(TRY_OFTEN_ACTIVITY_ON_CREATE)) {
                    Log.w(TAG, "Tz reset wallet because user try wrong pin code too often");
                    mShouldClearAll.set(true);

                    DemoApplication.addOnAppGotoBackgroundListener(new DemoApplication.AppGotoBackgroundListener() {
                        @Override
                        public void onAppGotoBackground() {
                            if (mShouldClearAll.get()) {
                                mShouldClearAll.set(false);
                                clearApplicationUserData();
                            }
                        }
                    });
                } else if (strData.equals(TRY_OFTEN_ACTIVITY_ON_CLICK) || strData.equals(TRY_OFTEN_ACTIVITY_ON_BACK_PRESSED)) {
                    Log.i(TAG, "User close UITryOftenAct");
                    if (mShouldClearAll.get()) {
                        mShouldClearAll.set(false);
                        clearApplicationUserData();
                    }
                }
                break;
            case APIEVENT:
                break;
        }
        return RESULT.SUCCESS;
    }

    private void clearApplicationUserData() {
        Log.w(TAG, "Clear app data");

        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(ACTIVITY_SERVICE);
        if (activityManager == null) {
            Log.e(TAG, "activityManager is null");
        } else {
            activityManager.clearApplicationUserData();
        }
    }
}
