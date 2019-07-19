package com.htc.wallet.skrsdk.tools.root;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.util.LogUtil;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

class ShellUtil {

    private static final String TAG = "ShellUtil";

    private ShellUtil() {
    }

    @Nullable
    public static String execute(@NonNull String command) {
        if (TextUtils.isEmpty(command)) {
            LogUtil.logDebug(TAG, "command is null or empty");
            return null;
        }

        Process process = null;
        BufferedReader bufferedReader = null;
        try {
            LogUtil.logDebug(TAG, "exec command = " + command);
            process = Runtime.getRuntime().exec(command);
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return IOUtils.toString(bufferedReader);
        } catch (IOException e) {
            LogUtil.logDebug(TAG, "IOException e = " + e);
            return null;
        } finally {
            if (process != null) {
                process.destroy();
            }
            if (bufferedReader != null) {
                IOUtils.closeQuietly(bufferedReader);
            }
        }
    }
}
