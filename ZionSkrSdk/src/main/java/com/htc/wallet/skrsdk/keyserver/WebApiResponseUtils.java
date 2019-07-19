package com.htc.wallet.skrsdk.keyserver;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.htc.wallet.skrsdk.util.LogUtil;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Response;

public abstract class WebApiResponseUtils {
    private static final String TAG = "WebApiResponseUtils";

    private static final String SEPARATOR = ", ";

    /**
     * Get error message 404, {"error":"This device is not found"}
     *
     * @param response response from onResponse
     * @return error message
     */
    @Nullable
    public static String getErrorMsg(@NonNull final Response<?> response) {
        if (response == null) {
            return null;
        }

        final StringBuilder errorMsg = new StringBuilder();
        errorMsg.append(response.code());

        try {
            final ResponseBody errorBody = response.errorBody();
            if (errorBody != null) {
                errorMsg.append(SEPARATOR);
                errorMsg.append(errorBody.string());
            }
        } catch (IOException e) {
            LogUtil.logDebug(TAG, "get errorBody message failed, IOException e=" + e);
        }

        return errorMsg.toString();
    }
}
