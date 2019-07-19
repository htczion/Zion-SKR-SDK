package com.htc.wallet.skrsdk.restore.util;

import static com.htc.wallet.skrsdk.restore.RestoreVerificationCodeView.ET_PIN_SIZE;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.util.JsonUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;

import java.util.Objects;

public final class SkrRestoreEditTextStatusUtil {
    private static final String TAG = "SkrRestoreEditTextStatusUtil";

    private SkrRestoreEditTextStatusUtil() {
    }

    public static SkrRestoreEditTextStatus getStatus(@NonNull Context context, int position) {
        Objects.requireNonNull(context, "context is null");
        if (position < 0 || position >= ET_PIN_SIZE) {
            throw new IllegalArgumentException("Incorrect position=" + position);
        }

        String jsonStr = SkrSharedPrefs.getSkrRestoreEditTextStatusToJson(context, position);
        if (TextUtils.isEmpty(jsonStr)) {
            return new SkrRestoreEditTextStatus();
        }

        SkrRestoreEditTextStatus skrRestoreEditTextStatus =
                JsonUtil.jsonToObject(jsonStr, SkrRestoreEditTextStatus.class);
        if (skrRestoreEditTextStatus == null) {
            LogUtil.logError(TAG, "skrRestoreEditTextStatus parse failed, jsonStr=" + jsonStr);
            return new SkrRestoreEditTextStatus();
        }

        return skrRestoreEditTextStatus;
    }

    public static void putStatus(@NonNull Context context, int position,
            @NonNull SkrRestoreEditTextStatus skrRestoreEditTextStatus) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(skrRestoreEditTextStatus, "skrRestoreEditTextStatus is null");
        if (position < 0 || position >= ET_PIN_SIZE) {
            throw new IllegalArgumentException("Incorrect position=" + position);
        }

        String jsonStr = JsonUtil.toJson(skrRestoreEditTextStatus);
        SkrSharedPrefs.putSkrRestoreEditTextStatusFromJson(context, position, jsonStr);
    }
}
