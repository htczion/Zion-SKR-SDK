package com.htc.wallet.skrsdk.tools.root;

import static com.htc.wallet.skrsdk.tools.root.RootCheckUtil.LINE_SEPARATOR;
import static com.htc.wallet.skrsdk.tools.root.RootCheckUtil.SPACE_SEPARATOR;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.Arrays;
import java.util.List;

// c. /sbin/adbd or /system/bin/sh is running as root
class ProcessStatus {

    private static final String TAG = "ProcessStatus";

    private static final String PID_OF = "pidof ";
    private static final String PS = "ps ";

    private static final String TITLE_USER = "USER";
    private static final String VALUE_ROOT = "root";

    private static final int PROCESS_STATUS_RESULT_LINE_TITLE = 0;
    private static final int PROCESS_STATUS_RESULT_LINE_VALUE = 1;
    private static final int PROCESS_STATUS_RESULT_LINE = 2;

    private static final List<String> APPS_SHOULD_NOT_RUNNING_AS_ROOT = Arrays.asList("adbd", "sh");

    private ProcessStatus() {
    }

    static boolean isRunningAsRoot() {
        boolean isRunningAsRoot = false;

        for (String app : APPS_SHOULD_NOT_RUNNING_AS_ROOT) {
            String pidResult = getPid(app);
            if (TextUtils.isEmpty(pidResult)) {
                // Can't get pid, assume false
                continue;
            }

            String[] pidArray = pidResult.split(SPACE_SEPARATOR);
            for (String pidStr : pidArray) {
                int pid = parseInt(pidStr);
                if (isPidRunningAsRoot(pid)) {
                    // Any one of this app's pid running as root, we assume it's rooted
                    isRunningAsRoot = true;
                }
            }

        }
        return isRunningAsRoot;
    }

    // 1|htc_exodugl:/ # pidof sh
    // 6234 27296
    @Nullable
    private static String getPid(@NonNull String name) {
        if (TextUtils.isEmpty(name)) {
            LogUtil.logDebug(TAG, "name is null or empty");
            return null;
        }

        return ShellUtil.execute(PID_OF + name);
    }

    // USER           PID  PPID     VSZ    RSS WCHAN            ADDR S NAME
    // shell         3205     1   30492   1360 0                   0 S adbd

    // USER           PID  PPID     VSZ    RSS WCHAN            ADDR S NAME
    // root          6234 27233    8980   1936 sigsuspend          0 S sh
    private static boolean isPidRunningAsRoot(int pid) {
        if (pid <= 0) {
            LogUtil.logDebug(TAG, "pid " + pid + " < 0");
            return false;
        }

        String processResult = ShellUtil.execute(PS + pid);
        if (TextUtils.isEmpty(processResult)) {
            LogUtil.logDebug(TAG, "ps result is empty, assume false");
            return false;
        }

        String[] resultLines = processResult.split(LINE_SEPARATOR);
        if (resultLines.length != PROCESS_STATUS_RESULT_LINE) {
            LogUtil.logDebug(TAG, "result lines " + resultLines.length + " != 2, assume false");
            return false;
        }

        String[] psTitles = resultLines[PROCESS_STATUS_RESULT_LINE_TITLE].split(SPACE_SEPARATOR);
        String[] psValues = resultLines[PROCESS_STATUS_RESULT_LINE_VALUE].split(SPACE_SEPARATOR);

        if (psTitles.length != psValues.length) {
            LogUtil.logDebug(TAG, "unexpected ps result");
            return false;
        }

        for (int i = 0; i < psTitles.length; i++) {
            if (psTitles[i].equals(TITLE_USER)) {
                return psValues[i].equals(VALUE_ROOT);
            }
        }

        LogUtil.logDebug(TAG, "Can't find USER");
        return false;
    }

    private static int parseInt(@NonNull final String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            LogUtil.logDebug(TAG, "NumberFormatException e = " + e);
        }
        return 0;
    }
}
