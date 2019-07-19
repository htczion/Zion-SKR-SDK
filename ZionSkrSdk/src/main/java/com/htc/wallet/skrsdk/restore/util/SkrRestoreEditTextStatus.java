package com.htc.wallet.skrsdk.restore.util;

import static com.htc.wallet.skrsdk.restore.RestoreVerificationCodeView.VERIFY_TIME_OUT;
import static com.htc.wallet.skrsdk.restore.RestoreVerificationCodeView.WAIT_TIMES;

public class SkrRestoreEditTextStatus {

    private static final long VERIFY_TIMEOUT = VERIFY_TIME_OUT;

    // If restore verify nothing can be sent. We will wait 20 seconds first.
    // If still nothing can be sent, we show error pin style
    private static final long NOTHING_SENT_TIMEOUT = WAIT_TIMES * 1000;

    private long mSentTimeMs = 0L;
    private int mReceivedPinCount = 0;
    private int mReceivedPinMaxCount = 0;

    public long getSentTimeMs() {
        return mSentTimeMs;
    }

    public void setSentTimeMs(long sentTimeMs) {
        mSentTimeMs = sentTimeMs;
    }

    public int getReceivedPinCount() {
        return mReceivedPinCount;
    }

    public void increaseReceivedPinCount() {
        mReceivedPinCount++;
    }

    public int getReceivedPinMaxCount() {
        return mReceivedPinMaxCount;
    }

    public void setReceivedPinMaxCount(int receivedPinMaxCount) {
        mReceivedPinMaxCount = receivedPinMaxCount;
    }

    public boolean hasSent() {
        return mSentTimeMs != 0L;
    }

    public boolean isTimeout() {
        if (mReceivedPinMaxCount == 0) {
            return (System.currentTimeMillis() - mSentTimeMs) > NOTHING_SENT_TIMEOUT;
        }
        return (System.currentTimeMillis() - mSentTimeMs) > VERIFY_TIMEOUT;
    }

    public long getRemainingTimeBeforeTimeout() {
        if (mReceivedPinMaxCount == 0) {
            return mSentTimeMs + NOTHING_SENT_TIMEOUT - System.currentTimeMillis();
        }
        return mSentTimeMs + VERIFY_TIMEOUT - System.currentTimeMillis();
    }
}
