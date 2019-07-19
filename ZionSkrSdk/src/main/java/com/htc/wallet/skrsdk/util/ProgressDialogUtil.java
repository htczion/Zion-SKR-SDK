package com.htc.wallet.skrsdk.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.support.annotation.NonNull;

import java.util.Objects;

public class ProgressDialogUtil {
    private static final String TAG = "ProgressDialogUtil";
    private final Activity mActivity;
    private final String mTitle;
    private final String mContent;

    public ProgressDialogUtil(
            @NonNull final Activity activity, final String title, final String content) {
        mActivity = Objects.requireNonNull(activity);
        mTitle = title;
        mContent = content;
    }

    public ProgressDialog show() {
        synchronized (this) {
            return ProgressDialog.show(mActivity, mTitle, mContent, true);
        }
    }

    public void dismiss(@NonNull final ProgressDialog progressDialog) {
        if (progressDialog == null) {
            LogUtil.logDebug(TAG, "dismiss, progressDialog is null");
            return;
        }
        if (mActivity != null && !mActivity.isFinishing()) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    }
}
