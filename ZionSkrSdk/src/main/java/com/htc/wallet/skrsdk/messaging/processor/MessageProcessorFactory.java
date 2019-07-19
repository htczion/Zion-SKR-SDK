package com.htc.wallet.skrsdk.messaging.processor;

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

import com.htc.wallet.skrsdk.util.LogUtil;

public class MessageProcessorFactory {
    public static final int FIREBASE_MESSAGE_PROCESSOR = 1;
    public static final int PUSHY_MESSAGE_PROCESSOR = 2;

    private static final String TAG = "MessageProcessorFactory";

    private static volatile MessageProcessorFactory sInstance = null;

    public static MessageProcessorFactory getInstance() {
        if (sInstance == null) {
            synchronized (MessageProcessorFactory.class) {
                if (sInstance == null) {
                    sInstance = new MessageProcessorFactory();
                }
            }
        }
        return sInstance;
    }

    @Nullable
    public UpstreamMessageProcessor getMessageProcessor(@ProcessorType int processorType) {
        switch (processorType) {
            case FIREBASE_MESSAGE_PROCESSOR:
                return new FirebaseMessageProcessor();
            case PUSHY_MESSAGE_PROCESSOR:
                return new PushyMessageProcessor();
            default:
                LogUtil.logError(
                        TAG,
                        "No matching processor!",
                        new IllegalStateException("No matching processor!"));
                return null;
        }
    }

    @IntDef({FIREBASE_MESSAGE_PROCESSOR, PUSHY_MESSAGE_PROCESSOR})
    public @interface ProcessorType {
    }
}
