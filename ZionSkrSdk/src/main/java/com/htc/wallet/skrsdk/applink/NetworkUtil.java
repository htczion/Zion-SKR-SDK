package com.htc.wallet.skrsdk.applink;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;

import java.util.Objects;

public class NetworkUtil {

    private NetworkUtil() {
        throw new AssertionError();
    }

    public static boolean isNetworkConnected(@NonNull final Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager)
                        Objects.requireNonNull(context)
                                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnected();
        } else {
            return false;
        }
    }
}
