package com.htc.wallet.skrsdk.messaging;

import android.support.annotation.Nullable;

public interface MultiTokenListener {
    void onUserTokenReceived(@Nullable final String pushyToken, @Nullable final String fcmToken);

    void onUserTokenError(Exception exception);
}
