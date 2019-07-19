package com.htc.wallet.skrsdk.util;

import static android.content.Context.TELEPHONY_SERVICE;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.crypto.ChecksumUtil;

import java.util.List;
import java.util.Objects;

public class PhoneUtil {
    private static final String TAG = "PhoneUtil";
    private static final String EMPTY_STRING = "";

    private PhoneUtil() {
    }

    @TargetApi(Build.VERSION_CODES.M)
    // @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_NUMBERS,
    // Manifest.permission.READ_PHONE_STATE})
    public static String getNumber(@NonNull Context context) {
        Objects.requireNonNull(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            LogUtil.logDebug(TAG, "Without phone permissions");
            return EMPTY_STRING;
        }

        String phoneNumber = null;
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            try {
                phoneNumber = telephonyManager.getLine1Number();
                LogUtil.logDebug(TAG, "phone: " + phoneNumber);
            } catch (SecurityException e) {
                LogUtil.logWarning(TAG, "Failed to get phone number from telephony");
            }
        }

        // Fallback to get phone number from subscription.
        if (TextUtils.isEmpty(phoneNumber)) {
            List<SubscriptionInfo> subscriptionInfos = null;
            try {
                subscriptionInfos =
                        SubscriptionManager.from(context).getActiveSubscriptionInfoList();
            } catch (SecurityException e) {
                LogUtil.logWarning(TAG, "Failed to get phone number from subscription");
            }

            if (subscriptionInfos != null && !subscriptionInfos.isEmpty()) {
                // Only get the first phone number.
                SubscriptionInfo info = subscriptionInfos.get(0);
                phoneNumber = info.getNumber();
                LogUtil.logDebug(TAG, "phone: " + phoneNumber + ", " + info.getCarrierName());
                // Check if we need to show carrier or other info when we failed to get number.
                // info.getCarrierName, info.getCountryIso
            }
        }

        return phoneNumber != null ? phoneNumber : EMPTY_STRING;
    }

    public static String getModel() {
        return Build.MODEL;
    }

    // Amy use
    public static String getSKRID(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        return SkrSharedPrefs.getSocialKMId(context);
    }

    // Amy use
    public static String getSKRIDHash(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        String uuid = getSKRID(context);
        if (TextUtils.isEmpty(uuid)) {
            LogUtil.logError(
                    TAG,
                    "uuid is null or empty",
                    new IllegalStateException("uuid is null or empty"));
            return EMPTY_STRING;
        }
        return ChecksumUtil.generateChecksum(uuid);
    }

    // Amy use, Backup flow only
    public static String getSKREmail(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        return SkrSharedPrefs.getSocialKMBackupEmail(context);
    }

    // Amy use, Backup flow only
    public static String getSKREmailHash(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        String email = getSKREmail(context);
        if (TextUtils.isEmpty(email)) {
            LogUtil.logError(
                    TAG,
                    "email is null or empty",
                    new IllegalStateException("email is null or empty"));
            return EMPTY_STRING;
        }
        return ChecksumUtil.generateChecksum(email);
    }

    // Bob use
    public static String getDeviceId(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        return SkrSharedPrefs.getDeviceId(context);
    }
}
