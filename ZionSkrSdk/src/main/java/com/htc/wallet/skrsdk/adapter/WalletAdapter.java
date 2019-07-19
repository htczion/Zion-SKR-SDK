package com.htc.wallet.skrsdk.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

public interface WalletAdapter {

    @WorkerThread
    long getUniqueId(@NonNull Context context);
}
