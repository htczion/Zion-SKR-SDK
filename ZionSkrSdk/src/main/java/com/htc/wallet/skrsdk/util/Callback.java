package com.htc.wallet.skrsdk.util;

/**
 * Use for single object callback
 *
 * @param <T> callback object type
 */
public interface Callback<T> {
    void onResponse(T t);
}
