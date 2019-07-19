package com.htc.wallet.skrsdk.util;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtil {
    private static final String TAG = "DateTimeUtil";

    @NonNull
    public static String formatUTCToLocalTime(long UTCTime) {
        // UNDEFINED_LAST_CHECKED_TIME(-1) is auto backup init last checked time
        // Long.MAX_VALUE is previous version's init last checked time
        if (UTCTime < 0 || UTCTime == Long.MAX_VALUE) {
            LogUtil.logInfo(TAG, "UTCTime is " + UTCTime);
            return "";
        }
        Date currentTime = new Date(UTCTime);
        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY/MM/dd HH:mm", Locale.US);

        String dateString = dateFormat.format(currentTime);
        if (TextUtils.isEmpty(dateString)) {
            return "";
        }
        return dateString;
    }
}
