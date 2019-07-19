package com.htc.wallet.skrsdk.secretsharing;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.google.common.primitives.Bytes;
import com.htc.htcwalletsdk.Export.HtcWalletSdkManager;
import com.htc.htcwalletsdk.Export.RESULT;
import com.htc.htcwalletsdk.Native.Type.ByteArrayHolder;
import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.crypto.Base64Util;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.WalletSdkUtil;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SeedUtil {
    private static final String TAG = "SeedUtil";

    public static final int INDEX_MIN = 0;
    public static final int INDEX_MAX = 4;

    // getPartialSeedV2 Base64.Default
    public static final int ENC_CODE_PK_LENGTH = 556;
    public static final int ENC_CODE_PK_SIG_LENGTH = 344;
    public static final int ENC_AES_KEY_LENGTH = 344;
    public static final int ENC_AES_KEY_SIG_LENGTH = 344;

    // getPartialSeedV2 byte array
    private static final int ENC_CODE_PK_BYTES_LENGTH = 416;
    private static final int ENC_CODE_PK_SIG_BYTES_LENGTH = 256;
    private static final int ENC_AES_KEY_BYTES_LENGTH = 256;
    private static final int ENC_AES_KEY_SIG_BYTES_LENGTH = 256;

    // combineSeedV2
    private static final int ENC_SEED_LENGTH = 256;
    private static final int ENC_SEED_SIG_LENGTH = 256;

    private static final int TIMEOUT = 60;

    private SeedUtil() {
        // Prevent construct SeedUtil
    }

    @WorkerThread
    @Nullable
    public static String getPartialSeedV2(@NonNull final Context context, final int seedIndex,
            @NonNull final String encCodePK, @NonNull final String encCodePKSigned,
            @NonNull final String encAesKey, @NonNull final String encAesKeySigned) {
        Objects.requireNonNull(context, "context is null");

        if (seedIndex < INDEX_MIN || seedIndex > INDEX_MAX) {
            LogUtil.logError(TAG, "incorrect index", new IllegalArgumentException());
            return null;
        }

        ThreadPoolExecutor executor = WalletSdkUtil.getThreadPoolExecutor();
        Future<String> future = executor.submit(new Callable<String>() {
            @Override
            public String call() {

                HtcWalletSdkManager htcWalletSdkManager = HtcWalletSdkManager.getInstance();

                // Check init
                int initRet = htcWalletSdkManager.init(context);
                if (initRet != RESULT.SUCCESS) {
                    LogUtil.logError(TAG, "Init failed, initRet=" + initRet);
                    return null;
                }

                // UniqueId
                long uniqueId = ZionSkrSdkManager.getInstance().getWalletAdapter().getUniqueId(
                        context);
                if (uniqueId == 0) {
                    LogUtil.logError(TAG, "getPartialSeed_v2, uid=0");
                    return null;
                } else {
                    LogUtil.logDebug(TAG, "getPartialSeed_v2, uid=" + LogUtil.pii(uniqueId));
                }

                // Check seed exist
                int seedExists = htcWalletSdkManager.isSeedExists(uniqueId);
                if (seedExists != RESULT.SUCCESS) {
                    LogUtil.logError(TAG, "Seed not exists, seedExists=" + seedExists);
                    return null;
                }

                // Need to check the length of encCodePK and Signed?
                byte[] encCodePKBytes = Base64Util.decodeDefault(encCodePK);
                LogUtil.logDebug(TAG, "encCodePK length = " + encCodePKBytes.length);

                byte[] encCodePKSigBytes = Base64Util.decodeDefault(encCodePKSigned);
                LogUtil.logDebug(TAG, "encCodePKSigned length = " + encCodePKSigBytes.length);

                ByteArrayHolder encCodePKAndSignedHolder =
                        createByteArrayHolder(Bytes.concat(encCodePKBytes, encCodePKSigBytes));

                // Need to check the length of encAesKey and Signed?
                byte[] encAesKeyBytes = Base64Util.decodeDefault(encAesKey);
                LogUtil.logDebug(TAG, "encAesKey length = " + encAesKeyBytes.length);

                byte[] encAesKeySigBytes = Base64Util.decodeDefault(encAesKeySigned);
                LogUtil.logDebug(TAG, "encAesKeySigned length = " + encAesKeySigBytes.length);

                ByteArrayHolder encAesKeyAndSignedHolder =
                        createByteArrayHolder(Bytes.concat(encAesKeyBytes, encAesKeySigBytes));

                // getPartialSeed_v2
                ByteArrayHolder outputSeedHolder = new ByteArrayHolder();
                int ret = htcWalletSdkManager.getPartialSeed_v2(uniqueId, seedIndex,
                        encCodePKAndSignedHolder, encAesKeyAndSignedHolder, outputSeedHolder);
                if (ret == RESULT.SUCCESS) {
                    LogUtil.logDebug(TAG, "getPartialSeed_v2 success");
                    return Base64Util.encodeToString(outputSeedHolder.byteArray);
                } else {
                    switch (ret) {
                        case RESULT.E_TEEKM_SSS_VERIFY_CODE_NOT_MATCH: // -907
                            LogUtil.logWarning(TAG, "Verify code not match");
                            break;
                        case RESULT.E_TEEKM_SSS_RE_TRY_COUNT_0: // -927
                            LogUtil.logWarning(TAG,
                                    "Verify code not match, refresh new verify code");
                            break;
                        default:
                            LogUtil.logError(TAG, "getPartialSeed_v2 failed, ret=" + ret);
                    }
                    return null;
                }
            }
        });

        try {
            return future.get(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LogUtil.logError(TAG, "getPartialSeedV2() failed, InterruptedException e=" + e);
        } catch (ExecutionException e) {
            LogUtil.logError(TAG,
                    "getPartialSeedV2() failed, ExecutionException cause=" + e.getCause());
        } catch (TimeoutException e) {
            LogUtil.logError(TAG, "getPartialSeedV2() failed, TimeoutException e=" + e);
        }

        return null;
    }

    @WorkerThread
    public static int combineSeedV2(
            @NonNull final Context context,
            @NonNull final String encPartialSeed1,
            @NonNull final String encPartialSeed1Signed,
            @NonNull final String encPartialSeed2,
            @NonNull final String encPartialSeed2Signed,
            @NonNull final String encPartialSeed3,
            @NonNull final String encPartialSeed3Signed) {
        Objects.requireNonNull(context, "context is null");

        LogUtil.logInfo(TAG, "combineSeedV2");

        // Check arguments
        if (TextUtils.isEmpty(encPartialSeed1)) {
            LogUtil.logError(
                    TAG, "encPartialSeed1 is null or empty", new IllegalArgumentException());
            return RESULT.UNKNOWN;
        }
        if (TextUtils.isEmpty(encPartialSeed1Signed)) {
            LogUtil.logError(
                    TAG, "encPartialSeed1Signed is null or empty", new IllegalArgumentException());
            return RESULT.UNKNOWN;
        }
        if (TextUtils.isEmpty(encPartialSeed2)) {
            LogUtil.logError(
                    TAG, "encPartialSeed2 is null or empty", new IllegalArgumentException());
            return RESULT.UNKNOWN;
        }
        if (TextUtils.isEmpty(encPartialSeed2Signed)) {
            LogUtil.logError(
                    TAG, "encPartialSeed2Signed is null or empty", new IllegalArgumentException());
            return RESULT.UNKNOWN;
        }
        if (TextUtils.isEmpty(encPartialSeed3)) {
            LogUtil.logError(
                    TAG, "encPartialSeed3 is null or empty", new IllegalArgumentException());
            return RESULT.UNKNOWN;
        }
        if (TextUtils.isEmpty(encPartialSeed3Signed)) {
            LogUtil.logError(
                    TAG, "encPartialSeed3Signed is null or empty", new IllegalArgumentException());
            return RESULT.UNKNOWN;
        }


        ThreadPoolExecutor executor = WalletSdkUtil.getThreadPoolExecutor();
        Future<Integer> future = executor.submit(new Callable<Integer>() {
            @Override
            public Integer call() {

                HtcWalletSdkManager htcWalletSdkManager = HtcWalletSdkManager.getInstance();

                // Check init
                int initRet = htcWalletSdkManager.init(context);
                if (initRet != RESULT.SUCCESS) {
                    LogUtil.logError(TAG, "Init failed, initRet=" + initRet);
                    return null;
                }

                // UniqueId
                long uniqueId = ZionSkrSdkManager.getInstance().getWalletAdapter().getUniqueId(
                        context);
                if (uniqueId == 0) {
                    LogUtil.logError(TAG, "combineSeedV2, uid=0");
                    return null;
                } else {
                    LogUtil.logDebug(TAG, "combineSeedV2, uid=" + LogUtil.pii(uniqueId));
                }

                // Check seed exist
                int seedExists = htcWalletSdkManager.isSeedExists(uniqueId);
                if (seedExists == RESULT.SUCCESS) {
                    LogUtil.logError(TAG, "Seed exists");
                    return null;
                }

                // Check encrypted seed 1
                final byte[] encSeed1Bytes = Base64Util.decodeDefault(encPartialSeed1);
                if (encSeed1Bytes.length != ENC_SEED_LENGTH) {
                    LogUtil.logError(TAG,
                            "incorrect encPartialSeed1 length=" + encSeed1Bytes.length);
                }
                final byte[] encSeed1SigBytes = Base64Util.decodeDefault(encPartialSeed1Signed);
                if (encSeed1SigBytes.length != ENC_SEED_SIG_LENGTH) {
                    LogUtil.logError(TAG,
                            "incorrect encPartialSeed1Signed length=" + encSeed1SigBytes.length);
                }
                ByteArrayHolder seed1Holder =
                        createByteArrayHolder(Bytes.concat(encSeed1Bytes, encSeed1SigBytes));

                // Check encrypted seed 2
                final byte[] encSeed2Bytes = Base64Util.decodeDefault(encPartialSeed2);
                if (encSeed2Bytes.length != ENC_SEED_LENGTH) {
                    LogUtil.logError(TAG,
                            "incorrect encPartialSeed2 length=" + encSeed2Bytes.length);
                }
                final byte[] encSeed2SigBytes = Base64Util.decodeDefault(encPartialSeed2Signed);
                if (encSeed2SigBytes.length != ENC_SEED_SIG_LENGTH) {
                    LogUtil.logError(TAG,
                            "incorrect encPartialSeed2Signed length=" + encSeed2SigBytes.length);
                }
                ByteArrayHolder seed2Holder =
                        createByteArrayHolder(Bytes.concat(encSeed2Bytes, encSeed2SigBytes));

                // Check encrypted seed 3
                final byte[] encSeed3Bytes = Base64Util.decodeDefault(encPartialSeed3);
                if (encSeed3Bytes.length != ENC_SEED_LENGTH) {
                    LogUtil.logError(TAG,
                            "incorrect encPartialSeed3 length=" + encSeed3Bytes.length);
                }
                final byte[] encSeed3SigBytes = Base64Util.decodeDefault(encPartialSeed3Signed);
                if (encSeed3SigBytes.length != ENC_SEED_SIG_LENGTH) {
                    LogUtil.logError(TAG,
                            "incorrect encPartialSeed3Signed length=" + encSeed3SigBytes.length);
                }
                ByteArrayHolder seed3Holder =
                        createByteArrayHolder(Bytes.concat(encSeed3Bytes, encSeed3SigBytes));

                int combineRet = htcWalletSdkManager.combineSeeds_v2(
                        uniqueId,
                        seed1Holder,
                        seed2Holder,
                        seed3Holder);
                if (combineRet != RESULT.SUCCESS) {
                    LogUtil.logWarning(TAG, "combine failed, ret=" + combineRet);
                }
                return combineRet;
            }
        });

        try {
            return future.get(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LogUtil.logError(TAG, "combineSeedV2() failed, InterruptedException e=" + e);
        } catch (ExecutionException e) {
            LogUtil.logError(TAG,
                    "combineSeedV2() failed, ExecutionException cause=" + e.getCause());
        } catch (TimeoutException e) {
            LogUtil.logError(TAG, "combineSeedV2() failed, TimeoutException e=" + e);
        }

        return RESULT.UNKNOWN;
    }

    @NonNull
    private static ByteArrayHolder createByteArrayHolder(final byte[] data) {
        if (data == null || data.length == 0) {
            LogUtil.logError(TAG, "data is null or empty", new IllegalArgumentException());
            return new ByteArrayHolder();
        }
        final ByteArrayHolder byteArrayHolder = new ByteArrayHolder();
        byteArrayHolder.byteArray = data.clone();
        byteArrayHolder.receivedLength = data.length;
        return byteArrayHolder;
    }
}
