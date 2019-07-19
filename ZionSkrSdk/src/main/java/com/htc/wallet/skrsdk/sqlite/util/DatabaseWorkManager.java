package com.htc.wallet.skrsdk.sqlite.util;

import android.os.Process;
import android.support.annotation.NonNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DatabaseWorkManager {
    public static final String TAG = "DatabaseWorkManager";

    private static final int DB_READ_CORE_POOL_SIZE = 1;
    private static final int DB_READ_MAX_POOL_SIZE = 5;
    private static final long DB_READ_THREAD_KEEP_ALIVE_TIME = 30L; // 30 seconds

    private final BlockingQueue<Runnable> mDbReadQueue;
    private final ExecutorService mDbReadThreadPool;
    private final ExecutorService mDbWriteThreadPool;

    private DatabaseWorkManager() {
        mDbReadQueue = new LinkedBlockingQueue<>();
        mDbReadThreadPool = new ThreadPoolExecutor(
                DB_READ_CORE_POOL_SIZE,
                DB_READ_MAX_POOL_SIZE,
                DB_READ_THREAD_KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                mDbReadQueue,
                new ThreadFactory() {
                    @Override
                    public Thread newThread(@NonNull final Runnable runnable) {
                        return new Thread(new Runnable() {
                            @Override
                            public void run() {
                                android.os.Process.setThreadPriority(
                                        Process.THREAD_PRIORITY_BACKGROUND);
                                runnable.run();
                            }
                        }, "DbReadThread");
                    }
                });

        mDbWriteThreadPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull final Runnable runnable) {
                return new Thread(new Runnable() {
                    @Override
                    public void run() {
                        android.os.Process.setThreadPriority(
                                Process.THREAD_PRIORITY_BACKGROUND);
                        runnable.run();
                    }
                }, "DbWriteThread");
            }
        });
    }

    private static class SingletonHolder {
        static final DatabaseWorkManager INSTANCE = new DatabaseWorkManager();
    }

    public static DatabaseWorkManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void enqueueWrite(@NonNull DatabaseWorkItem workItem) {
        mDbWriteThreadPool.execute(workItem);
    }

    public void enqueueRead(@NonNull DatabaseWorkItem workItem) {
        mDbReadThreadPool.execute(workItem);
    }

    public interface DatabaseWorkItem extends Runnable {
    }
}
