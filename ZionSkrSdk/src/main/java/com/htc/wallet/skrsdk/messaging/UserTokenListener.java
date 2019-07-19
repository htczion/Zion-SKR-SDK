package com.htc.wallet.skrsdk.messaging;

import android.support.annotation.NonNull;

public interface UserTokenListener {
    void onUserTokenReceived(@NonNull final String token);

    void onUserTokenError(Exception exception);
}
