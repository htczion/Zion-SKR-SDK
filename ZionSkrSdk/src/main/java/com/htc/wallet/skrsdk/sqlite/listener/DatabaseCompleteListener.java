package com.htc.wallet.skrsdk.sqlite.listener;

public interface DatabaseCompleteListener {
    void onComplete();

    void onError(Exception exception);
}
