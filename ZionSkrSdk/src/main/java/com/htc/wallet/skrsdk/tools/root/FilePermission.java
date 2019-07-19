package com.htc.wallet.skrsdk.tools.root;

import com.htc.wallet.skrsdk.util.LogUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

// f. check file permission (/data, /dev/block/mmcblk)
class FilePermission {
    private static final String TAG = "FilePermission";

    private static final List<String> FILES_SHOULD_NOT_RW = Arrays.asList("/data",
            "/dev/block/mmcblk");

    private FilePermission() {
    }

    static boolean isSystemDirCanRW() {
        for (String path : FILES_SHOULD_NOT_RW) {
            File file = new File(path);
            boolean canRead = file.canRead();
            boolean canWrite = file.canWrite();
            if (canRead || canWrite) {
                LogUtil.logDebug(TAG, path + " [r, w] = [" + canRead + ", " + canWrite + "]");
                return true;
            }
        }
        return false;
    }

}
