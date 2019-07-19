package com.htc.wallet.skrsdk.demoapp.util;


import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;
import java.util.Objects;

public class AppVisibilityDetector {
    private static final boolean DEBUG = false;
    private static final String TAG = "AppVisibilityDetector";

    private static final int MSG_GOTO_FOREGROUND = 1;
    private static final int MSG_GOTO_BACKGROUND = 2;

    private static volatile AppVisibilityCallback sAppVisibilityCallback = null;
    private static volatile boolean sIsForeground = false;
    private static volatile Handler sHandler = null;

    public static void init(Application app, AppVisibilityCallback appVisibilityCallback) {
        if (!checkIsMainProcess(app)) {
            return;
        }

        sAppVisibilityCallback = appVisibilityCallback;
        app.registerActivityLifecycleCallbacks(new AppActivityLifecycleCallbacks());

        sHandler = new Handler(app.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_GOTO_FOREGROUND:
                        if (DEBUG) {
                            Log.d(TAG, "handleMessage(MSG_GOTO_FOREGROUND)");
                        }
                        performAppGotoForeground();
                        break;
                    case MSG_GOTO_BACKGROUND:
                        if (DEBUG) {
                            Log.d(TAG, "handleMessage(MSG_GOTO_BACKGROUND)");
                        }
                        performAppGotoBackground();
                        break;
                    default:
                }
            }
        };
    }

    private static boolean checkIsMainProcess(Application app) {
        ActivityManager activityManager =
                (ActivityManager) app.getSystemService(Context.ACTIVITY_SERVICE);
        Objects.requireNonNull(activityManager, "activityManager is null");
        List<ActivityManager.RunningAppProcessInfo>
                runningAppProcessInfoList = activityManager.getRunningAppProcesses();
        if (runningAppProcessInfoList == null) {
            return false;
        }

        String currProcessName = null;
        int currPid = android.os.Process.myPid();
        //find the process name
        for (ActivityManager.RunningAppProcessInfo processInfo : runningAppProcessInfoList) {
            if (null != processInfo && processInfo.pid == currPid) {
                currProcessName = processInfo.processName;
            }
        }

        //is current process the main process
        return TextUtils.equals(currProcessName, app.getPackageName());

    }

    private static void performAppGotoForeground() {
        if (!sIsForeground && null != sAppVisibilityCallback) {
            sIsForeground = true;
            sAppVisibilityCallback.onAppGotoForeground();
        }
    }

    private static void performAppGotoBackground() {
        if (sIsForeground && null != sAppVisibilityCallback) {
            sIsForeground = false;
            sAppVisibilityCallback.onAppGotoBackground();
        }
    }

    public interface AppVisibilityCallback {
        void onAppGotoForeground();

        void onAppGotoBackground();
    }

    private static class AppActivityLifecycleCallbacks implements
            Application.ActivityLifecycleCallbacks {
        int activityDisplayCount = 0;

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            if (DEBUG) {
                Log.d(TAG, activity.getLocalClassName() + " onActivityCreated");
            }
        }

        @Override
        public void onActivityStarted(Activity activity) {
            sHandler.removeMessages(MSG_GOTO_FOREGROUND);
            sHandler.removeMessages(MSG_GOTO_BACKGROUND);
            if (activityDisplayCount == 0) {
                sHandler.sendEmptyMessage(MSG_GOTO_FOREGROUND);
            }
            activityDisplayCount++;

            if (DEBUG) {
                Log.d(TAG, activity.getLocalClassName() + " onActivityStarted "
                        + " activityDisplayCount: " + activityDisplayCount);
            }
        }

        @Override
        public void onActivityResumed(Activity activity) {
            if (DEBUG) {
                Log.d(TAG, activity.getLocalClassName() + " onActivityResumed");
            }
        }

        @Override
        public void onActivityPaused(Activity activity) {
            if (DEBUG) {
                Log.d(TAG, activity.getLocalClassName() + " onActivityPaused");
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            if (DEBUG) {
                Log.d(TAG,
                        activity.getLocalClassName() + " onActivitySaveInstanceState");
            }
        }

        @Override
        public void onActivityStopped(Activity activity) {
            sHandler.removeMessages(MSG_GOTO_FOREGROUND);
            sHandler.removeMessages(MSG_GOTO_BACKGROUND);
            if (activityDisplayCount > 0) {
                activityDisplayCount--;
            }

            if (activityDisplayCount == 0) {
                sHandler.sendEmptyMessage(MSG_GOTO_BACKGROUND);
            }

            if (DEBUG) {
                Log.d(TAG, activity.getLocalClassName() + " onActivityStopped "
                        + " activityDisplayCount: " + activityDisplayCount);
            }
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            if (DEBUG) {
                Log.d(TAG, activity.getLocalClassName() + " onActivityDestroyed");
            }
        }
    }
}
