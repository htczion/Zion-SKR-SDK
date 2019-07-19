package com.htc.wallet.skrsdk.tools.root;

import android.content.Context;
import android.support.annotation.NonNull;

import com.htc.wallet.skrsdk.util.LogUtil;
import com.scottyab.rootbeer.RootBeer;

import java.util.Objects;

// adb logcat -s RootCheckUtil,RootBeer,ProcessStatus,PathMountPermission,SystemProperty,
// FilePermission

public class RootCheckUtil {

    private static final String TAG = "RootCheckUtil";

    static final String LINE_SEPARATOR = System.lineSeparator();
    static final String SPACE_SEPARATOR = "\\s+";


    public static boolean isRooted(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");

        // a. "su" exist in /sbin or /system/bin or /system/xbin
        // b. "su" can be executed
        // e. property fail: ro.secure = 0, ro.debuggable = 1
        // RootBeer
        RootBeer rootBeer = new RootBeer(context);
        if (rootBeer.isRootedWithoutBusyBoxCheck()) {
            LogUtil.logDebug(TAG, "RootBeer detect Root");
            return true;
        }

        // c. /sbin/adbd or /system/bin/sh is running as root
        if (ProcessStatus.isRunningAsRoot()) {
            LogUtil.logDebug(TAG, "Some app is running as root");
            return true;
        }

        // d. rootfs or /system is mounted as read-write
        if (PathMountPermission.isRootOrSystemCanRW()) {
            LogUtil.logDebug(TAG, "Some path is mounted as read-write, rooted");
            return true;
        }

        // e. property fail: ro.boot.veritymode != enforcing, ro.boot.verifiedbootstate != green
        // or orange
        if (SystemProperty.haveDangerousProperties()) {
            LogUtil.logDebug(TAG, "Have some dangerous property, rooted");
            return true;
        }

        // f. check file permission (/data, /dev/block/mmcblk)
        if (FilePermission.isSystemDirCanRW()) {
            LogUtil.logDebug(TAG, "Some file can be read-write, rooted");
            return true;
        }

        return false;
    }
}
