package com.htc.wallet.skrsdk.tools.security.attestation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.htc.wallet.skrsdk.util.LogUtil;

public class AttestationBootReceiver extends BroadcastReceiver {
    private static final String TAG = "AttestationBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            LogUtil.logError(TAG, "intent is null");
            return;
        }
        // UnsafeProtectedBroadcastReceiver
        // Due to we listen protected broadcast action, we need to check the action first
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            LogUtil.logDebug(TAG, "BOOT_COMPLETED");
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            LogUtil.logDebug(TAG, "MY_PACKAGE_REPLACED");
        } else {
            LogUtil.logWarning(TAG, "Unexpected Action = " + action);
            return;
        }

        SafetyNetWrapper.getInstance().clear(context);
    }
}
