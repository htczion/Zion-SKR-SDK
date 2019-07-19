package com.htc.wallet.skrsdk.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.htc.htcwalletsdk.Export.HtcWalletSdkManager;
import com.htc.htcwalletsdk.Export.RESULT;
import com.htc.htcwalletsdk.Native.Type.ByteArrayHolder;
import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.adapter.WalletAdapter;
import com.htc.wallet.skrsdk.error.TzApiInitException;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public final class WalletSdkUtil {
    private static final String TAG = "WalletSdkUtil";
    private static final ThreadPoolExecutor sSingleThreadExecutor =
            ThreadUtil.newFixedThreadPool(1, "wallet-sdk-util");

    private static final String API_VERSION_SPLITTER = "\\.";
    private static final String VERSION_CODE_TZ_SUPPORTED = "0";
    private static final String VERSION_CODE_NON_TZ_SUPPORTED = "1";

    private static volatile String sTZIDHash;

    private WalletSdkUtil() {
    }

    @WorkerThread
    @Nullable
    public static String getTZIDHash(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        final Context appContext = context.getApplicationContext();

        // Check run on main thread
        ThreadUtil.checkNotOnMainThread();

        if (!TextUtils.isEmpty(sTZIDHash)) {
            return sTZIDHash;
        }

        Future<?> future = sSingleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {

                if (!TextUtils.isEmpty(sTZIDHash)) {
                    LogUtil.logDebug(TAG, "sTZIDHash exists");
                    return;
                }

                int initRet = HtcWalletSdkManager.getInstance().init(appContext);
                if (initRet != RESULT.SUCCESS) {
                    LogUtil.logError(TAG, "Init failed, initRet=" + initRet,
                            new TzApiInitException("Init failed, initRet=" + initRet));
                    return;
                }

                // get TZIDHash
                ByteArrayHolder byteArrayHolder = new ByteArrayHolder();
                int getRet = HtcWalletSdkManager.getInstance().getTZIDHash(byteArrayHolder);
                if (getRet != RESULT.SUCCESS) {
                    LogUtil.logError(TAG, "getTZIDHash failed, ret=" + getRet);
                    return;
                }

                if (byteArrayHolder.byteArray == null) {
                    LogUtil.logError(TAG, "TZIDHash byteArray is null");
                    return;
                }
                int length = byteArrayHolder.byteArray.length;
                if (length <= 1) {
                    LogUtil.logError(TAG, "TZIDHash incorrect");
                    return;
                }
                // remove last '\0'
                if (byteArrayHolder.byteArray[(length - 1)] == 0) {
                    length--;
                }

                byte[] tzIdHashBytes = Arrays.copyOf(byteArrayHolder.byteArray, length);
                String tzidHash = new String(tzIdHashBytes);
                if (TextUtils.isEmpty(tzidHash)) {
                    LogUtil.logError(TAG, "tzidHash is null or empty");
                    return;
                }

                sTZIDHash = tzidHash;
                LogUtil.logDebug(TAG, "sTZIDHash=" + sTZIDHash);
            }
        });

        try {
            future.get();
        } catch (InterruptedException e) {
            LogUtil.logError(TAG, "InterruptedException e=" + e);
        } catch (ExecutionException e) {
            LogUtil.logError(TAG, "ExecutionException e=" + e.getCause());
        }
        return sTZIDHash;
    }

    @WorkerThread
    public static boolean isSeedExists(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        final Context appContext = context.getApplicationContext();

        Future<Boolean> future = sSingleThreadExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {

                HtcWalletSdkManager htcWalletSdkManager = HtcWalletSdkManager.getInstance();

                int initRet = htcWalletSdkManager.init(appContext);
                if (initRet != RESULT.SUCCESS) {
                    throw new TzApiInitException("Init failed, ret=" + initRet);
                }

                long uniqueId = getUniqueId(appContext);
                boolean isSeedExists =
                        (htcWalletSdkManager.isSeedExists(uniqueId) == RESULT.SUCCESS);
                LogUtil.logDebug(TAG, "isSeedExists=" + isSeedExists);
                return isSeedExists;
            }
        });

        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new IllegalStateException("InterruptedException e=" + e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("ExecutionException cause=" + e.getCause());
        }
    }

    @WorkerThread
    public static boolean isExodus(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        final Context appContext = context.getApplicationContext();

        Future<Boolean> future = sSingleThreadExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {

                int ret = HtcWalletSdkManager.getInstance().init(appContext);
                if (ret != RESULT.SUCCESS) {
                    LogUtil.logError(TAG, "HtcWalletSdkManager init() failed, ret=" + ret);
                    return false;
                }

                String apiVersion = HtcWalletSdkManager.getInstance().getApiVersion();
                if (TextUtils.isEmpty(apiVersion)) {
                    LogUtil.logError(TAG, "apiVersion is null or empty");
                    return false;
                }

                String[] apiVersionSplit = apiVersion.split(API_VERSION_SPLITTER);
                String apiVersionCode = apiVersionSplit[0];

                if (VERSION_CODE_TZ_SUPPORTED.equals(apiVersionCode)) {
                    return true;
                } else if (VERSION_CODE_NON_TZ_SUPPORTED.equals(apiVersionCode)) {
                    return false;
                } else {
                    LogUtil.logError(TAG, "Unknown apiVersion=" + apiVersion);
                    return false;
                }
            }
        });

        try {
            return future.get();
        } catch (InterruptedException e) {
            LogUtil.logError(TAG, "InterruptedException e=" + e);
            return false;
        } catch (ExecutionException e) {
            LogUtil.logError(TAG, "ExecutionException cause=" + e.getCause());
            return false;
        }
    }

    // Don't use singleThreadExecutor in this function, or it may be deadlock
    @WorkerThread
    public static long getUniqueId(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");

        WalletAdapter walletAdapter = ZionSkrSdkManager.getInstance().getWalletAdapter();
        if (walletAdapter == null) {
            throw new IllegalStateException("walletAdapter is null");
        }

        // According ZKMA document, unique_id is 0 that means the registration is failed
        // https://github.com/htczion/ZKMA/wiki#35-register-your-wallet
        long uniqueId = walletAdapter.getUniqueId(context);
        if (uniqueId == RESULT.REGISTER_FAILED) {
            throw new IllegalStateException("Incorrect uniqueId=" + uniqueId);
        }

        return uniqueId;
    }

    @NonNull
    public static ThreadPoolExecutor getThreadPoolExecutor() {
        return sSingleThreadExecutor;
    }

    public static void enqueue(@NonNull TzWorkItem workItem) {
        sSingleThreadExecutor.execute(workItem);
    }

    public interface TzWorkItem extends Runnable {
    }
}
