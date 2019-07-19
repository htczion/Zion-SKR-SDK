package com.htc.wallet.skrsdk.tools.security.attestation;

import static com.htc.wallet.skrsdk.keyserver.KeyServerCallbackWithRetry.TOTAL_RETRIES;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.adapter.ApiKeyAdapter;
import com.htc.wallet.skrsdk.applink.NetworkUtil;
import com.htc.wallet.skrsdk.crypto.Base64Util;
import com.htc.wallet.skrsdk.keyserver.KeyServerApiService;
import com.htc.wallet.skrsdk.keyserver.KeyServerManager;
import com.htc.wallet.skrsdk.keyserver.requestbody.AttestationsRequestBody;
import com.htc.wallet.skrsdk.keyserver.requestbody.GetNonceRequestBody;
import com.htc.wallet.skrsdk.keyserver.responsebody.AttestationsResponseBody;
import com.htc.wallet.skrsdk.keyserver.responsebody.GetNonceResponseBody;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;
import com.htc.wallet.skrsdk.util.ThreadUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Response;

public class SafetyNetWrapper {
    private static final String TAG = "SafetyNetWrapper";

    public static final String ACTION_ATTEST_FAILED = "com.htc.wallet.action.ATTEST_FAILED";
    public static final String ACTION_GOOGLE_PLAY_SERVICE_NOT_AVAILABLE =
            "com.htc.wallet.action.GOOGLE_PLAY_SERVICE_NOT_AVAILABLE";
    public static final String KEY_ERROR_CODE_GOOGLE_PLAY_SERVICE =
            "key_error_code_google_play_service";

    private static final long TOKEN_VALIDITY_TIME = 30 * DateUtils.MINUTE_IN_MILLIS; // 30 min
    private static final long RETRY_WAIT_TIME = DateUtils.SECOND_IN_MILLIS; // 1 sec
    private static final int TIMEOUT = 30; // seconds
    private static final int NONCE_MIN_LENGTH = 16;
    private static final String ATTEST_TOKEN_PREFIX = "Bearer ";

    private static final String JWT_SPLIT = "\\.";
    private static final int JWT_SECTION = 3;
    private static final int JWT_PAYLOAD_INDEX = 1;

    private static final String KEY_BASIC_INTEGRITY = "basicIntegrity";
    private static final String KEY_CTS_PROFILE_MATCH = "ctsProfileMatch";
    private static final String KEY_ADVICE = "advice";

    private static final ExecutorService sSingleThreadExecutor =
            Executors.newSingleThreadExecutor();

    private SafetyNetWrapper() {
    }

    private static class SingletonHolder {
        private static final SafetyNetWrapper INSTANCE = new SafetyNetWrapper();
    }

    public static SafetyNetWrapper getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Get device attestation token.
     * Auto choose server (stage and production) by DEBUG flag and setting
     * If attest token is still fresh, return value from sharedPrefs directly
     * When Google Play Service not Available or get token failed, it will send broadcast to show
     * alertDialog
     *
     * @return device attest toke with prefix "Bearer ", if any error may get null and log error
     * @see #ACTION_ATTEST_FAILED
     * @see #ACTION_GOOGLE_PLAY_SERVICE_NOT_AVAILABLE
     * @see #KEY_ERROR_CODE_GOOGLE_PLAY_SERVICE
     */
    @WorkerThread
    @Nullable
    public String getDeviceAttestToken(@NonNull final Context context) {
        Objects.requireNonNull(context, "context is null");
        return getDeviceAttestToken(context, false);
    }

    /**
     * Get device attestation token.
     * Auto choose server (stage and production) by DEBUG flag and setting
     * When Google Play Service not Available or get token failed, it will send broadcast to show
     * alertDialog
     *
     * @param isForce true, acquire new attest token even old one still fresh
     * @return device attest toke with prefix "Bearer ", if any error may get null and log error
     * @see #ACTION_ATTEST_FAILED
     * @see #ACTION_GOOGLE_PLAY_SERVICE_NOT_AVAILABLE
     * @see #KEY_ERROR_CODE_GOOGLE_PLAY_SERVICE
     */
    @WorkerThread
    @Nullable
    public String getDeviceAttestToken(@NonNull final Context context, final boolean isForce) {
        Objects.requireNonNull(context, "context is null");
        final boolean isTest = !ZionSkrSdkManager.getInstance().getIsUsingProductionServer();
        return getDeviceAttestToken(context, isTest, isForce);
    }

    /**
     * Get device attestation token.
     * If token is fresh enough, just return sharedPrefs value, or get the new one
     * When Google Play Service not Available or get token failed, it will send broadcast to show
     * alertDialog
     *
     * @param isTest  true for stage, false for production
     * @param isForce true, acquire new attest token even old one still fresh
     * @return device attest toke with prefix "Bearer ", if any error may get null and log error
     * @see #ACTION_ATTEST_FAILED
     * @see #ACTION_GOOGLE_PLAY_SERVICE_NOT_AVAILABLE
     * @see #KEY_ERROR_CODE_GOOGLE_PLAY_SERVICE
     */
    @WorkerThread
    @Nullable
    public String getDeviceAttestToken(@NonNull final Context context, final boolean isTest,
            final boolean isForce) {
        Objects.requireNonNull(context, "context is null");

        // Check google play service available first
        final int errorCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                context);
        if (errorCode != ConnectionResult.SUCCESS) {
            LogUtil.logDebug(TAG, "Send google play service not available broadcast");
            Intent intent = new Intent(ACTION_GOOGLE_PLAY_SERVICE_NOT_AVAILABLE);
            intent.putExtra(KEY_ERROR_CODE_GOOGLE_PLAY_SERVICE, errorCode);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            return null;
        }

        // Make all sharedPrefs read/write run on a single thread
        Future<String> future = sSingleThreadExecutor.submit(new Callable<String>() {
            @Override
            public String call() throws IOException, SafetyNetFailureException {
                return getDeviceAttestTokenInternal(context, isTest, isForce);
            }
        });

        String attestToken = null;

        try {
            // Add timeout 60 sec ? prevent lock the thread ?
            // In test case, one full getDeviceAttestTokenInternal spend 4 to 7 seconds (30 times
            // test on stage server)

            // future.get() will lock caller's thread until task finish and return
            attestToken = future.get();

            // Send attest failed broadcast to show dialog
            if (TextUtils.isEmpty(attestToken)) {
                LogUtil.logWarning(TAG, "Send SafetyNet attest failed broadcast");
                Intent intent = new Intent(ACTION_ATTEST_FAILED);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        } catch (InterruptedException e) {
            LogUtil.logDebug(TAG, "getDeviceAttestToken error, e=" + e);
        } catch (ExecutionException e) {
            LogUtil.logWarning(TAG, "getDeviceAttestToken error, e=" + e.getCause());
        }

        return attestToken;
    }

    @WorkerThread
    @Nullable
    private String getDeviceAttestTokenInternal(@NonNull final Context context,
            final boolean isTest, final boolean isForce)
            throws IOException, SafetyNetFailureException {
        Objects.requireNonNull(context, "context is null");

        if (isTest) {
            LogUtil.logDebug(TAG, "get attest token on stage server");
        } else {
            LogUtil.logDebug(TAG, "get attest token on production server");
        }

        // token is fresh just return
        if (!isForce && !isAttestTokenRefreshNeeded(context, isTest)) {
            final String token = SkrSharedPrefs.getDeviceAttestToken(context, isTest);
            if (TextUtils.isEmpty(token)) {
                LogUtil.logDebug(TAG,
                        "token is empty with fresh timestamp, may encrypt/decrypt error");
            } else {
                return ATTEST_TOKEN_PREFIX + token;
            }
        }

        LogUtil.logDebug(TAG, "acquire new token");

        // Check network
        if (!NetworkUtil.isNetworkConnected(context)) {
            LogUtil.logWarning(TAG, "Network not available");
            throw new IOException("Network not available");
        }

        // Step 1, get nonce
        final byte[] nonce = getNonce(context, isTest);

        if (nonce == null || nonce.length < NONCE_MIN_LENGTH) {
            LogUtil.logDebug(TAG, "Incorrect nonce");
            return null;
        }

        // Step 2, attest from SafetyNet
        final String safetyNetJwsResult = getSafetyNetAttestationJwsResult(context, isTest, nonce);
        if (TextUtils.isEmpty(safetyNetJwsResult)) {
            LogUtil.logWarning(TAG, "safetyNetJwsResult is empty");
            return null;
        }

        // Check SafetyNet result
        if (!isSafetyNetResultValid(safetyNetJwsResult)) {
            LogUtil.logWarning(TAG, "safetyNetJwsResult not valid");
            if (isTest) {
                LogUtil.logDebug(TAG, "isTest is true, just print to log, keep going");
            } else {
                LogUtil.logDebug(TAG, "isTest is false, return null");
                return null;
            }
        }

        // Step 3, post to keyserver
        final String attestToken = postAttestToken(context, isTest, safetyNetJwsResult);
        if (TextUtils.isEmpty(attestToken)) {
            LogUtil.logDebug(TAG, "attestToken is empty");
            return null;
        }

        // Put fresh token to sharedPrefs
        SkrSharedPrefs.putDeviceAttestToken(context, attestToken, isTest);

        return ATTEST_TOKEN_PREFIX + attestToken;
    }

    @WorkerThread
    private boolean isAttestTokenRefreshNeeded(@NonNull final Context context,
            final boolean isTest) {
        Objects.requireNonNull(context, "context is null");
        long timestamp = SkrSharedPrefs.getDeviceAttestTokenTimestamp(context, isTest);
        long currentTime = System.currentTimeMillis();
        return (currentTime < timestamp) // future timestamp
                || (currentTime > timestamp + TOKEN_VALIDITY_TIME); // expired
    }

    /**
     * Get nonce from key server
     *
     * @param isTest use stage or production server
     * @return nonce from keyServer
     * @throws IOException maybe network problem
     */
    @WorkerThread
    @Nullable
    private byte[] getNonce(@NonNull Context context, boolean isTest) throws IOException {
        Objects.requireNonNull(context, "context is null");

        final KeyServerManager keyServerManager = KeyServerManager.getInstance(isTest);
        final String tzApiKey = keyServerManager.getTzApiKey(context);
        final KeyServerApiService keyServerApiService = keyServerManager.getKeyServerApiService();

        final GetNonceRequestBody requestBody = new GetNonceRequestBody(context, tzApiKey);

        Response<GetNonceResponseBody> response;
        byte[] nonce = null;
        for (int i = 1; i <= TOTAL_RETRIES; i++) {
            try {
                response = keyServerApiService.getNonceV2(requestBody).execute();
                if (response.isSuccessful()) {
                    GetNonceResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        LogUtil.logDebug(TAG, "responseBody is null");
                        continue;
                    }

                    String nonceStr = responseBody.getNonce();
                    if (TextUtils.isEmpty(nonceStr)) {
                        LogUtil.logDebug(TAG, "nonce is empty");
                        continue;
                    }
                    nonce = nonceStr.getBytes();
                    break;
                } else {
                    String errorMsg = "get nonce failed, " + response.code();
                    if (response.errorBody() != null) {
                        errorMsg += ", " + response.errorBody().string();
                    }
                    LogUtil.logDebug(TAG, errorMsg);
                    LogUtil.logDebug(TAG, "Retrying... (" + i + " out of " + TOTAL_RETRIES + ")");
                    ThreadUtil.sleep(RETRY_WAIT_TIME);
                }
            } catch (IOException e) {
                LogUtil.logDebug(TAG, "IOException, e=" + e);
                LogUtil.logDebug(TAG, "Retrying... (" + i + " out of " + TOTAL_RETRIES + ")");
                // last retry
                if (i >= TOTAL_RETRIES) {
                    throw e;
                } else {
                    ThreadUtil.sleep(RETRY_WAIT_TIME);
                }
            }
        }
        return nonce;
    }

    /**
     * Get attest result by SafetyNet Attestation API
     *
     * @param nonce from keyServer, min length 16 bytes
     * @return SafetyNet Attestation Api JwsResult
     * @throws SafetyNetFailureException exception wrapper, include ApiException
     * @see com.google.android.gms.common.api.ApiException
     */
    @WorkerThread
    @Nullable
    private String getSafetyNetAttestationJwsResult(@NonNull Context context, final boolean isTest,
            @NonNull final byte[] nonce)
            throws SafetyNetFailureException {
        Objects.requireNonNull(context, "context is null");

        if (nonce == null || nonce.length < NONCE_MIN_LENGTH) {
            LogUtil.logDebug(TAG, "Incorrect nonce");
            return null;
        }

        ApiKeyAdapter apiKeyAdapter = ZionSkrSdkManager.getInstance().getApiKeyAdapter();
        if (apiKeyAdapter == null) {
            LogUtil.logError(TAG, "apiKeyAdapter is null",
                    new IllegalStateException("apiKeyAdapter is null"));
            return null;
        }

        String safetyNetApiKey;
        if (isTest) {
            safetyNetApiKey = apiKeyAdapter.getSafetyNetApiKeyStage();
        } else {
            safetyNetApiKey = apiKeyAdapter.getSafetyNetApiKeyProduction();
        }
        if (TextUtils.isEmpty(safetyNetApiKey)) {
            LogUtil.logDebug(TAG, "safetyNetApiKey is null or empty");
            return null;
        }

        // Check google play services available
        final int errorCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                context);
        if (errorCode != ConnectionResult.SUCCESS) {
            // It's almost impossible, we have check this before. But we still keep this check here.
            LogUtil.logDebug(TAG, "Google Play Services is not available, errorCode=" + errorCode);
            throw new SafetyNetFailureException(
                    "Google Play Services is not available, errorCode=" + errorCode);
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> result = new AtomicReference<>();
        final AtomicReference<SafetyNetFailureException> exceptionRef = new AtomicReference<>();

        // Get with safety net attestation jws result
        Task<SafetyNetApi.AttestationResponse> responseTask = SafetyNet.getClient(context).attest(
                nonce, safetyNetApiKey);
        responseTask.addOnSuccessListener(
                new OnSuccessListener<SafetyNetApi.AttestationResponse>() {
                    @Override
                    public void onSuccess(SafetyNetApi.AttestationResponse attestationResponse) {
                        LogUtil.logDebug(TAG, "SafetyNet Attestation success");
                        String jwsResult = attestationResponse.getJwsResult();
                        if (TextUtils.isEmpty(jwsResult)) {
                            LogUtil.logDebug(TAG, "jwsResult is null or empty");
                        }
                        result.set(jwsResult);
                        latch.countDown();
                    }
                });
        responseTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                LogUtil.logDebug(TAG, "SafetyNet attest failed, e=" + e);
                exceptionRef.set(new SafetyNetFailureException(e.toString()));
                latch.countDown();
            }
        });

        try {
            latch.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LogUtil.logDebug(TAG, "InterruptedException e=" + e);
        }

        if (exceptionRef.get() != null) {
            throw exceptionRef.get();
        }

        return result.get();
    }

    /**
     * Parse SafetyNet result, and check content
     *
     * @param jwsResult result from SafetyNet Attestation Api
     * @return true, true, if all content is expected
     */
    private boolean isSafetyNetResultValid(@NonNull final String jwsResult) {
        if (TextUtils.isEmpty(jwsResult)) {
            LogUtil.logDebug(TAG, "jwsResult is null or empty");
            return false;
        }

        String[] resultArray = jwsResult.split(JWT_SPLIT);
        if (resultArray.length != JWT_SECTION) {
            LogUtil.logDebug(TAG, "incorrect resultArray length=" + resultArray.length);
            return false;
        }

        String payloadBase64 = resultArray[JWT_PAYLOAD_INDEX];
        String payload;
        try {
            byte[] result = Base64Util.decodeDefault(payloadBase64);
            payload = new String(result);
        } catch (IllegalArgumentException e) {
            LogUtil.logDebug(TAG, "IllegalArgumentException e=" + e);
            return false;
        } catch (Exception e) {
            LogUtil.logDebug(TAG, "Exception e=" + e);
            return false;
        }

        if (TextUtils.isEmpty(payload)) {
            LogUtil.logDebug(TAG, "payload is null or empty");
            return false;
        }

        try {
            JSONObject jsonObject = new JSONObject(payload);
            LogUtil.logDebug(TAG, "jsonObject=" + jsonObject);
            boolean basicIntegrity = jsonObject.getBoolean(KEY_BASIC_INTEGRITY);
            boolean ctsProfileMatch = jsonObject.getBoolean(KEY_CTS_PROFILE_MATCH);
            // TODO: Add check ctsProfileMatch (QSU5 only check basicIntegrity)
            if (!basicIntegrity) {
                LogUtil.logWarning(TAG,
                        "[basic, cts] = [" + basicIntegrity + ", " + ctsProfileMatch + "]");
                String advice = jsonObject.getString(KEY_ADVICE);
                LogUtil.logDebug(TAG, "advice=" + advice);
                return false;
            }
            // Not need to check more (apkPackageName, cert, nonce)
            return true;
        } catch (JSONException e) {
            LogUtil.logDebug(TAG, "JSONException e=" + e);
            return false;
        }
    }

    /**
     * Get attest token from key server
     *
     * @param isTest            use stage or production server
     * @param signedAttestation jwsResult from SafetyNet Attestation Api
     * @return token from keyServer
     * @throws IOException maybe network problem
     */
    @WorkerThread
    @Nullable
    private String postAttestToken(@NonNull Context context, boolean isTest,
            @NonNull String signedAttestation) throws IOException {
        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(signedAttestation)) {
            LogUtil.logDebug(TAG, "signedAttestation is null or empty");
            return null;
        }

        final KeyServerManager keyServerManager = KeyServerManager.getInstance(isTest);
        final String tzApiKey = keyServerManager.getTzApiKey(context);
        final KeyServerApiService keyServerApiService = keyServerManager.getKeyServerApiService();

        final AttestationsRequestBody requestBody = new AttestationsRequestBody(context, tzApiKey,
                signedAttestation);
        Response<AttestationsResponseBody> response;
        String token = null;
        for (int i = 1; i <= TOTAL_RETRIES; i++) {
            try {
                response = keyServerApiService.postAttestationsResultV2(requestBody).execute();
                if (response.isSuccessful()) {
                    AttestationsResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        LogUtil.logDebug(TAG, "responseBody is null");
                        continue;
                    }

                    token = responseBody.getToken();
                    if (TextUtils.isEmpty(token)) {
                        LogUtil.logDebug(TAG, "token is empty");
                    } else {
                        break;
                    }
                } else {
                    String errorMsg = "post attest failed, code=" + response.code();
                    if (response.errorBody() != null) {
                        errorMsg += ", " + response.errorBody().string();
                    }
                    LogUtil.logDebug(TAG, errorMsg);
                    LogUtil.logDebug(TAG, "Retrying... (" + i + " out of " + TOTAL_RETRIES + ")");
                    ThreadUtil.sleep(RETRY_WAIT_TIME);
                }
            } catch (IOException e) {
                LogUtil.logDebug(TAG, "IOException, e=" + e);
                LogUtil.logDebug(TAG, "Retrying... (" + i + " out of " + TOTAL_RETRIES + ")");
                // last retry
                if (i >= TOTAL_RETRIES) {
                    throw e;
                } else {
                    ThreadUtil.sleep(RETRY_WAIT_TIME);
                }
            }
        }
        return token;
    }

    /**
     * for application startup prepare only for production
     */
    public void update(@NonNull final Context context) {
        Objects.requireNonNull(context, "context is null");
        sSingleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                boolean isTest = !ZionSkrSdkManager.getInstance().getIsUsingProductionServer();
                String token;
                try {
                    token = getDeviceAttestTokenInternal(context, isTest, false);
                    if (TextUtils.isEmpty(token)) {
                        LogUtil.logDebug(TAG, "update device attest token failed");
                    } else {
                        LogUtil.logDebug(TAG, "update device attest token success");
                    }
                } catch (Exception e) {
                    LogUtil.logDebug(TAG, "Update device attest token error, e=" + e);
                }
            }
        });
    }

    /**
     * Clear attest token (Both stage and production)
     * Trigger when device shutdown or restart
     */
    void clear(@NonNull final Context context) {
        Objects.requireNonNull(context, "context is null");
        sSingleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                LogUtil.logDebug(TAG, "clear device attest token");
                SkrSharedPrefs.clearDeviceAttestToken(context);
            }
        });
    }
}
