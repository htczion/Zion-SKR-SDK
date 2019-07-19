package com.htc.wallet.skrsdk.demoapp.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import com.htc.htcwalletsdk.Export.HtcWalletSdkManager;
import com.htc.htcwalletsdk.Export.RESULT;
import com.htc.htcwalletsdk.Native.Type.ByteArrayHolder;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class ZKMASdkUtil {
    private static final String TAG = "ZKMASdkUtil";
    private static final ExecutorService sSingleThreadExecutor =
            Executors.newSingleThreadExecutor();

    private static final String API_VERSION_SPLITTER = "\\.";
    private static final String VERSION_CODE_TZ_SUPPORTED = "0";
    private static final String VERSION_CODE_NON_TZ_SUPPORTED = "1";

    private static volatile String sTZIDHash;
    private static volatile long sUniqueId = RESULT.REGISTER_FAILED;

    private ZKMASdkUtil() {
    }

    @WorkerThread
    @Nullable
    public static String getTZIDHash(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");
        final Context appContext = context.getApplicationContext();

        if (!TextUtils.isEmpty(sTZIDHash)) {
            return sTZIDHash;
        }

        Future<?> future = sSingleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {

                if (!TextUtils.isEmpty(sTZIDHash)) {
                    Log.d(TAG, "sTZIDHash exists");
                    return;
                }

                int initRet = HtcWalletSdkManager.getInstance().init(appContext);
                if (initRet != RESULT.SUCCESS) {
                    Log.e(TAG, "Init failed, initRet=" + initRet,
                            new IllegalStateException("Init failed, initRet=" + initRet));
                    return;
                }

                // get TZIDHash
                ByteArrayHolder byteArrayHolder = new ByteArrayHolder();
                int getRet = HtcWalletSdkManager.getInstance().getTZIDHash(byteArrayHolder);
                if (getRet != RESULT.SUCCESS) {
                    Log.e(TAG, "getTZIDHash failed, ret=" + getRet);
                    return;
                }

                if (byteArrayHolder.byteArray == null) {
                    Log.e(TAG, "TZIDHash byteArray is null");
                    return;
                }
                int length = byteArrayHolder.byteArray.length;
                if (length <= 1) {
                    Log.e(TAG, "TZIDHash incorrect");
                    return;
                }
                // remove last '\0'
                if (byteArrayHolder.byteArray[(length - 1)] == 0) {
                    length--;
                }

                byte[] tzIdHashBytes = Arrays.copyOf(byteArrayHolder.byteArray, length);
                String tzidHash = new String(tzIdHashBytes);
                if (TextUtils.isEmpty(tzidHash)) {
                    Log.e(TAG, "tzidHash is null or empty");
                    return;
                }

                sTZIDHash = tzidHash;
                Log.d(TAG, "sTZIDHash=" + sTZIDHash);
            }
        });

        try {
            future.get();
        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException e=" + e);
        } catch (ExecutionException e) {
            Log.e(TAG, "ExecutionException e=" + e.getCause());
        }
        return sTZIDHash;
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
                    Log.e(TAG, "HtcWalletSdkManager init() failed, ret=" + ret);
                    return false;
                }

                String apiVersion = HtcWalletSdkManager.getInstance().getApiVersion();
                if (TextUtils.isEmpty(apiVersion)) {
                    Log.e(TAG, "apiVersion is null or empty");
                    return false;
                }

                String[] apiVersionSplit = apiVersion.split(API_VERSION_SPLITTER);
                String apiVersionCode = apiVersionSplit[0];

                if (VERSION_CODE_TZ_SUPPORTED.equals(apiVersionCode)) {
                    return true;
                } else if (VERSION_CODE_NON_TZ_SUPPORTED.equals(apiVersionCode)) {
                    return false;
                } else {
                    Log.e(TAG, "Unknown apiVersion=" + apiVersion);
                    return false;
                }
            }
        });

        try {
            return future.get();
        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException e=" + e);
            return false;
        } catch (ExecutionException e) {
            Log.e(TAG, "ExecutionException cause=" + e.getCause());
            return false;
        }
    }

    @WorkerThread
    public static boolean isSeedExists(@NonNull final Context context) {
        Objects.requireNonNull(context, "context is null");
        final Context appContext = context.getApplicationContext();

        Future<Boolean> future = sSingleThreadExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {

                HtcWalletSdkManager htcWalletSdkManager = HtcWalletSdkManager.getInstance();

                int initRet = htcWalletSdkManager.init(appContext);
                if (initRet != RESULT.SUCCESS) {
                    throw new IllegalStateException("Init failed, ret=" + initRet);
                }

                refreshUniqueIdIfNeeded(context);
                boolean isSeedExists =
                        (htcWalletSdkManager.isSeedExists(sUniqueId) == RESULT.SUCCESS);
                Log.d(TAG, "isSeedExists=" + isSeedExists);
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
    public static long getUniqueId(@NonNull final Context context) {
        Objects.requireNonNull(context, "context is null");

        if (sUniqueId != RESULT.REGISTER_FAILED) {
            return sUniqueId;
        }

        Future<?> future = sSingleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                refreshUniqueIdIfNeeded(context);
            }
        });

        try {
            future.get();
        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException e=" + e);
        } catch (ExecutionException e) {
            Log.e(TAG, "ExecutionException cause=" + e.getCause());
        }
        return sUniqueId;
    }

    @WorkerThread
    private static void refreshUniqueIdIfNeeded(@NonNull Context context) {
        Objects.requireNonNull(context, "context is null");

        if (sUniqueId != RESULT.REGISTER_FAILED) {
            return;
        }

        synchronized (ZKMASdkUtil.class) {
            if (sUniqueId != RESULT.REGISTER_FAILED) {
                return;
            }

            int ret = HtcWalletSdkManager.getInstance().init(context);
            if (ret != RESULT.SUCCESS) {
                Log.e(TAG, "Init failed, ret=" + ret);
                return;
            }

            String walletName = context.getPackageName();
            @SuppressLint("HardwareIds")
            String androidId = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            String walletSha256 = ChecksumUtil.generateChecksum(androidId);

            HtcWalletSdkManager.getInstance().setSdkProtectorListener(new SdkErrorHandler(context));

            sUniqueId = HtcWalletSdkManager.getInstance().register(walletName, walletSha256);
            Log.d(TAG, "Set sUniqueId=" + sUniqueId);
        }
    }
}
