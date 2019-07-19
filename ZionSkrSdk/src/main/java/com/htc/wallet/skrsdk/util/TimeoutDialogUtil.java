package com.htc.wallet.skrsdk.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.annotation.NonNull;

public class TimeoutDialogUtil {

    public static void setupTimeOutDialog(
            @NonNull final Activity activity, final String title, final String content) {
        if (activity == null) {
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton(
                        android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                .show();
    }
}
