package com.htc.wallet.skrsdk.tools.root;

import static com.htc.wallet.skrsdk.tools.root.RootCheckUtil.LINE_SEPARATOR;

import android.text.TextUtils;

import com.htc.wallet.skrsdk.util.LogUtil;

// e. property fail: ro.boot.veritymode != enforcing, ro.boot.verifiedbootstate != green or orange
class SystemProperty {
    private static final String TAG = "SystemProperty";

    private static final String GET_PROP = "getprop";

    private static final String VERIFY_MODE = "[ro.boot.veritymode]";
    private static final String VERIFY_MODE_VALUE = "[enforcing]";

    private static final String VERIFIED_BOOT_STATE = "[ro.boot.verifiedbootstate]";
    private static final String VERIFIED_BOOT_STATE_GREEN = "[green]";
    private static final String VERIFIED_BOOT_STATE_ORANGE = "[orange]";

    private SystemProperty() {
    }

    // [ro.boot.verifiedbootstate]: [red]
    // [ro.boot.veritymode]: [enforcing]

    static boolean haveDangerousProperties() {

        String propsResult = ShellUtil.execute(GET_PROP);

        if (TextUtils.isEmpty(propsResult)) {
            LogUtil.logDebug(TAG, "getprop empty, assume false");
            return false;
        }

        boolean isDangerous = false;
        String[] props = propsResult.split(LINE_SEPARATOR);
        for (String prop : props) {
            if (prop.contains(VERIFY_MODE)) {
                if (!prop.contains(VERIFY_MODE_VALUE)) {
                    LogUtil.logDebug(TAG, prop);
                    isDangerous = true;
                }
            } else if (prop.contains(VERIFIED_BOOT_STATE)) {
                if (!prop.contains(VERIFIED_BOOT_STATE_GREEN) && !prop.contains(
                        VERIFIED_BOOT_STATE_ORANGE)) {
                    LogUtil.logDebug(TAG, prop);
                    isDangerous = true;
                }
            }
        }
        return isDangerous;
    }

}
