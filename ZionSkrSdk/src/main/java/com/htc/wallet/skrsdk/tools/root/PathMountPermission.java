package com.htc.wallet.skrsdk.tools.root;

import static com.htc.wallet.skrsdk.tools.root.RootCheckUtil.LINE_SEPARATOR;
import static com.htc.wallet.skrsdk.tools.root.RootCheckUtil.SPACE_SEPARATOR;

import android.text.TextUtils;

import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.Arrays;
import java.util.List;

// d. rootfs or /system is mounted as read-write
class PathMountPermission {

    private static final String TAG = "PathMountPermission";

    private static final String MOUNT = "mount";
    private static final String RW = "rw";

    // rootfs on / type rootfs (ro,seclabel,size=1751312k,nr_inodes=437828)
    // /dev/root on / type ext4 (ro,seclabel,relatime,data=ordered)
    private static final int INDEX_PATH = 2;
    private static final int INDEX_RW = 5;
    private static final int RW_BEGIN = 1;
    private static final int RW_END = 3;

    private static final List<String> PATHS_SHOULD_NOT_MOUNTED_AS_RW = Arrays.asList("/",
            "/system");

    private PathMountPermission() {
    }

    static boolean isRootOrSystemCanRW() {
        String mountResult = ShellUtil.execute(MOUNT);
        if (TextUtils.isEmpty(mountResult)) {
            LogUtil.logDebug(TAG, "mount result empty, assume false");
            return false;
        }

        boolean isPathsCanRW = false;
        String[] lines = mountResult.split(LINE_SEPARATOR);
        for (String line : lines) {
            String[] options = line.split(SPACE_SEPARATOR);
            if (options.length < INDEX_RW) {
                // If we don't have enough options per line, skip this and log an error
                LogUtil.logDebug(TAG, "Error formatting mount line: " + line);
                continue;
            }

            if (PATHS_SHOULD_NOT_MOUNTED_AS_RW.contains(options[INDEX_PATH])) {
                if (options[INDEX_RW].length() < RW_END) {
                    // If we don't have enough length per options, skip this and log an error
                    LogUtil.logDebug(TAG, "Error formatting option: " + options[INDEX_RW]);
                } else {
                    String permission = options[INDEX_RW].substring(RW_BEGIN, RW_END);
                    if (permission.equals(RW)) {
                        LogUtil.logDebug(TAG,
                                "rw path: " + options[INDEX_PATH] + ", line = " + line);
                        isPathsCanRW = true;
                    }
                }
            }
        }
        return isPathsCanRW;
    }
}
