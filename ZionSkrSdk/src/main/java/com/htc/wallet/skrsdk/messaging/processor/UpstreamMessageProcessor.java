package com.htc.wallet.skrsdk.messaging.processor;

import android.content.Context;
import android.support.annotation.NonNull;

import com.htc.wallet.skrsdk.messaging.UserTokenListener;
import com.htc.wallet.skrsdk.messaging.message.Message;


public interface UpstreamMessageProcessor {
    int sendMessage(@NonNull Context context, @NonNull Message message);

    void getUserToken(@NonNull Context context, @NonNull UserTokenListener userTokenListener);
}
