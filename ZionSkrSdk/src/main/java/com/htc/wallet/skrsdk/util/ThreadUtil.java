package com.htc.wallet.skrsdk.util;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ThreadUtil {
    private static final String TAG = "ThreadUtil";

    private static final int CORE_POOL_SIZE = 0;
    private static final int KEEP_ALIVE_TIME = 1;

    private static volatile Handler sMainHandler;
    private static volatile ExecutorService sBackgroundExecutor;

    public ThreadUtil() {
    }

    public static ExecutorService getBackgroundExecutor() {
        if (sBackgroundExecutor == null) {
            synchronized (ThreadUtil.class) {
                if (sBackgroundExecutor == null) {
                    sBackgroundExecutor = newFixedThreadPool(1, "skr-bg-executor");
                }
            }
        }
        return sBackgroundExecutor;
    }

    public static ThreadPoolExecutor newFixedThreadPool(int fixedPoolSize, String threadName) {
        if (TextUtils.isEmpty(threadName)) {
            threadName = "skr-thread-pool";
        }
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat(
                threadName + "-%d").build();
        return new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                fixedPoolSize,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(1024),
                namedThreadFactory,
                new ThreadPoolExecutor.AbortPolicy());
    }

    public static ThreadPoolExecutor newFixedThreadPoolNoQueue(int fixedPoolSize,
            String threadName) {
        if (TextUtils.isEmpty(threadName)) {
            threadName = "skr-thread-pool";
        }
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat(
                threadName + "-%d").build();
        return new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                fixedPoolSize,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                namedThreadFactory,
                new ThreadPoolExecutor.DiscardPolicy());
    }

    public static void sleep(long millis) {
        if (millis <= 0) {
            return;
        }

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    public static void checkNotOnMainThread() {
        if (isOnMainThread()) {
            LogUtil.logError(TAG, "This operation should not be run on main thread!",
                    new IllegalStateException("This operation should not be run on main thread!"));
        }
    }

    public static boolean isOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static void runOnMainThread(@NonNull Runnable runnable) {
        Preconditions.checkNotNull(runnable);

        if (isOnMainThread()) {
            runnable.run();
        } else {
            if (sMainHandler == null) {
                synchronized (ThreadUtil.class) {
                    if (sMainHandler == null) {
                        sMainHandler = new Handler(Looper.getMainLooper());
                    }
                }
            }
            sMainHandler.post(runnable);
        }
    }
}
