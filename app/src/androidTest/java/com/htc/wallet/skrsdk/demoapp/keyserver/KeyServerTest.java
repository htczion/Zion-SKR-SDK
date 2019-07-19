package com.htc.wallet.skrsdk.demoapp.keyserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.htc.wallet.skrsdk.crypto.util.VerificationUtil;
import com.htc.wallet.skrsdk.demoapp.Hex;
import com.htc.wallet.skrsdk.keyserver.KeyServerApiService;
import com.htc.wallet.skrsdk.keyserver.KeyServerCallbackWithRetry;
import com.htc.wallet.skrsdk.keyserver.KeyServerManager;
import com.htc.wallet.skrsdk.keyserver.requestbody.BackupCodePkRequestBody;
import com.htc.wallet.skrsdk.keyserver.requestbody.RestoreCodeRequestBody;
import com.htc.wallet.skrsdk.keyserver.requestbody.RestoreSeedRequestBody;
import com.htc.wallet.skrsdk.keyserver.responsebody.BackupCodePkResponseBody;
import com.htc.wallet.skrsdk.keyserver.responsebody.RestoreCodeResponseBody;
import com.htc.wallet.skrsdk.keyserver.responsebody.RestoreSeedResponseBody;
import com.htc.wallet.skrsdk.tools.security.attestation.SafetyNetWrapper;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PinCodeUtil;

import org.junit.Test;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import retrofit2.Call;
import retrofit2.Response;

public class KeyServerTest {
    private static final String TAG = KeyServerTest.class.getSimpleName();

    private static final String RSA = "RSA";
    private static final String ASYMMETRIC_KEY_TRANSFORMATION =
            "RSA/ECB/OAEPWithSHA-1AndMGF1Padding";
    private static final String SYMMETRIC_KEY_TRANSFORMATION = "AES/CBC/PKCS7Padding";
    private static final String TZ_ID_HASH =
            "108d6efe55fe7365e1db722403a3c3fda1458c8b3b7c3a85244e29c449cf9684";
    private static final String TZ_PRIVATE_KEY_STR =
            "MIIEogIBAAKCAQEAoVIPhSf/wYlyA/V8pjJ5V9/oPoqEW16rr6gTVOSs5DcpW6hj\n"
                    + "qSQbMCKzukKoDrIzt9ZUpoU7AvYHuwx1/zew9z9cx+Ci/2GBheiWEYLqP21w6pO/\n"
                    + "hsafWOyWOcaGD3YB8HVXq3OukyI8dHrVMrZui5bPx5xvl/tlReDibk+/nRzEajLC\n"
                    + "N0ZExmiO0nevG54Lnv7U/iqF6oXcN6HceSQRQB1Pw9ywg1LRsKwGRTo8FM7DVcTm\n"
                    + "Bd+5B06u9/PQTsDjfFyCvLmfeyUpVZTU8TRD58s1ae3yor39Yl4l9P6mJHMJhoJT\n"
                    + "VZAM7WIK8ulBtBuMBKK2DGm68Rlo/eG7SV9ZkQIDAQABAoIBABL9mQwg2E/NQVnL\n"
                    + "9V+PQ4+fsTRjlA85htaH37a3sM6w27KJkSnhMT4qZ6P+otAQFFyI47AysO65TdGs\n"
                    + "NtvTj4abs+1nuWh87wV3iusG0VKkLI3A1OQlz65lM44bm76IMtQ+zBJH7P4vIpTH\n"
                    + "vP9aUIsoNs+Vz+FBW/us7Jr10mXJREaYkwWDpn0oqPXmj8UPRJLc7x2CFCHW7ZUY\n"
                    + "tCsTXHDB6A3dbTvkZN5zqJuir/kuYY4wuxCy6c3hDJijmmLiWG9/V5/G0ChTs3Nd\n"
                    + "vhX5YkiBCSPQdhtjbYUWfNEK4lTuBzhFp6h0I3yzuxBc0B0KKd+v9+HgAbOxYm4G\n"
                    + "5VNuEsECgYEA0+YHuOKTI2HMUnD6FS9eJefWNW6hUtuzkH493xNZDexhi89EFVW5\n"
                    + "Ghtk2sSQKQBqCmKwBqItQu+9sMnVrWubVztn5oNTciUNJT9jwZc4T0HtVxFd5XvW\n"
                    + "hH7kpaV53wdzZzLXoL9vy5mCfZ63CvPD7U5wEEPU2OshTB/1BqQIM2kCgYEAwuU7\n"
                    + "E3tNKWVvViUMdhDIJ75XpJ90+GUGzx/hcU9gpa7oCWFI5kI4CXLRRZtuuYaHNRkZ\n"
                    + "RwC/rywT6eqNZSLEA9jnXdaAfiFORq5TRXdEtypJMPlxXEvZ79zOBOwD89DVa+Xw\n"
                    + "DPAOZ7mAubC2Q0yPiOIA44HoSJlk+qz4X8FVN+kCgYA7SdSFTZf/wWBq/MNsZAmC\n"
                    + "r6CG6MJoraJLXpcvMHmtZKNSfBa/pXGaNWn9sBvp7Py+lShNYtkpLm0z5vVLhZ08\n"
                    + "RhnnLH2PpYBNGLRvuUD/JCIlR22vRPwbrGmLU4aK+cm3aUld59J+9B/HyD3M6bG7\n"
                    + "V0QRuPj4DKoPY62qQwEsaQKBgAXNSy/uSMLFuUXIPpG5OCwut284LARFBane2hhE\n"
                    + "c60bAt9cnQ31xLNLTr2Svf4Z6iaDg8QgWpR9bG5XRDRRj6JJr8GUZCjdFvZ1tBbq\n"
                    + "UQpe4OTrWBuWSnugiCWPXSLCAch/hODtuJAP7qwYfaSNP+ykRcCAGokmv+HSimZX\n"
                    + "XBEZAoGAPY1+1/xIc95QRmf+Wv/WLXoBCtiwzSIR7T3APgLlPfNB7O6uzQ6YL5jY\n"
                    + "o4mcvOJCk5bs8wh6bBQJrE7mOcQVPy5mZ7XqwZoo5kNVjGu7bfCfnJDn0HEtyWTJ\n"
                    + "DBoWSbbfCWP68qH5FD65IZ5AaKqovjGVKs2EjL+G+eVb+RLPayY=";
    private static final PrivateKey TZ_PRIVATE_KEY = createPrivateKey(TZ_PRIVATE_KEY_STR);
    private static final String SERVER_PUBLIC_KEY_STR =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArdSHNt5KAkEHjf8e4ZZC"
                    + "13jHX+7RkhKyocPGPkO2+76uupNtfVWht+/KhqUj0nv9CcA/zcqxxrMQGqM1HHNy"
                    + "5YBSiUwQytw7sLO75RZdoz+Nmj/ul9tOLlmf9b6SUEnhmvJFM4MDqn0djGBOwDwB"
                    + "sozFhuw22pBDmLcnhOYhxsDp2Ob5eZCkCrNr6jAZCuFMMbkMODrViLWTraDImvEA"
                    + "7I1lYNfwNP19gVeLY5vE0TjeOK23OXHIDyqwxPG/txBu70Dkxc9kSl5kNnjrloHW"
                    + "YqqrfNgBe5jgW7Q2HND4cfa7ZIYDDe1/5fsgG1Vbe9cTdwMsL8h+jGjjZt3TvUhw"
                    + "4QIDAQAB";
    // For testing production url
    private static final String SERVER_PUBLIC_KEY_PRODUCTION_STR =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuRFZnxnsKpV"
                    + "5jiIU8xySq3akMxtgRBxIi5SnUOXggMSDKbwsWuxeE6EDzeQkeRFIREEc2aLy4euP"
                    + "NYK1tf6e0fJ1bVCONAMRch2GSNaaza6Q0bbXFswrpu7Bu7b35vA1Rh2Si/Zz1hlJ6"
                    + "FXWaKBDKx0p8woSMyDxIFdkH4CGXAN2qPwnUa1f7iYosXhgnLi0k4FzTIij3OiaP9"
                    + "9pxtKdumgjpBIfBLfjqyqsjFLHejof71RkgjyHTYGppERUcUPeKufdLeuv81YzuZI"
                    + "a2M/iW50M3ek4aT6PQKXyd2jZlePFjB2bAFTdU7jmLaC0gYZSNx+fXWtqxyaR3WOQ"
                    + "JIH32wIDAQAB";

    private static final PublicKey SERVER_PUBLIC_KEY = createPublicKey(SERVER_PUBLIC_KEY_STR);
    private static final PublicKey SERVER_PUBLIC_KEY_PRODUCTION = createPublicKey(
            SERVER_PUBLIC_KEY_PRODUCTION_STR);

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private static final Context CONTEXT = InstrumentationRegistry.getTargetContext();

    private static PrivateKey createPrivateKey(String base64PrivateKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(RSA);
            byte[] buffer = Base64.decode(base64PrivateKey, Base64.DEFAULT);
            EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);
            return keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "NoSuchAlgorithmException e = " + e);
        } catch (InvalidKeySpecException e) {
            Log.e(TAG, "InvalidKeySpecException e = " + e);
        }
        return null;
    }

    private static PublicKey createPublicKey(String base64PublicKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(RSA);
            byte[] buffer = Base64.decode(base64PublicKey, Base64.DEFAULT);
            EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
            return keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "NoSuchAlgorithmException e = " + e);
        } catch (InvalidKeySpecException e) {
            Log.e(TAG, "InvalidKeySpecException e = " + e);
        }
        return null;
    }

    private byte[] encryptRSA(byte[] data, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance(ASYMMETRIC_KEY_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
            fail("decryptRSA failed");
            return null;
        }
    }

    private byte[] decryptRSA(byte[] encData, PrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance(ASYMMETRIC_KEY_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(encData);
        } catch (Exception e) {
            e.printStackTrace();
            fail("decryptRSA failed");
            return null;
        }
    }

    private String decryptAES(byte[] key, byte[] initVector, byte[] encData) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, SYMMETRIC_KEY_TRANSFORMATION);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(initVector);
            Cipher cipher = Cipher.getInstance(SYMMETRIC_KEY_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] decBytes = cipher.doFinal(encData);
            return new String(decBytes);
        } catch (Exception e) {
            e.printStackTrace();
            fail("decryptAES failed");
            return null;
        }
    }

    private byte[] sign(byte[] data, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (Exception e) {
            e.printStackTrace();
            fail("sign failed");
            return null;
        }
    }

    private boolean verify(byte[] data, byte[] signature, PublicKey publicKey) {
        try {
            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
            sig.initVerify(publicKey);
            sig.update(data);
            return sig.verify(signature);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Test
    public void postEncryptCodePKTest() {
        final VerificationUtil verificationUtil = new VerificationUtil(true);

        final String verifyCode = "123456";
        final String publicKey = verificationUtil.getPublicKeyString();

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final KeyServerManager keyServerManager = KeyServerManager.getInstance(true);
                final KeyServerApiService keyServerApiService =
                        keyServerManager.getKeyServerApiService();
                final BackupCodePkRequestBody requestBody = new BackupCodePkRequestBody(
                        CONTEXT, keyServerManager.getTzApiKey(CONTEXT), TZ_ID_HASH, verifyCode,
                        publicKey);
                final String attestToken = SafetyNetWrapper.getInstance().getDeviceAttestToken(
                        CONTEXT, true, false);
                assertFalse(TextUtils.isEmpty(attestToken));
                Call<BackupCodePkResponseBody> call = keyServerApiService.postEncryptCodePKV2(
                        attestToken, requestBody);
                call.enqueue(new KeyServerCallbackWithRetry<BackupCodePkResponseBody>(call) {
                    @Override
                    public void onResponse(@NonNull Call<BackupCodePkResponseBody> call,
                            @NonNull Response<BackupCodePkResponseBody> response) {
                        super.onResponse(call, response);
                        assertTrue(response.isSuccessful());
                        BackupCodePkResponseBody responseBody = response.body();
                        assertNotNull(responseBody);

                        final String encCodePK = responseBody.getEncCodePK();
                        final String encCodePKSigned = responseBody.getEncCodePKSigned();
                        final String encAesKey = responseBody.getEncAesKey();
                        final String encAesKeySigned = responseBody.getEncAesKeySigned();

                        byte[] encCodePKByte = Base64.decode(encCodePK, Base64.DEFAULT);
                        byte[] encCodePKSignedByte = Base64.decode(encCodePKSigned, Base64.DEFAULT);
                        byte[] encAesKeyByte = Base64.decode(encAesKey, Base64.DEFAULT);
                        byte[] encAesKeySignedByte = Base64.decode(encAesKeySigned, Base64.DEFAULT);

                        assertTrue(verify(encCodePKByte, encCodePKSignedByte, SERVER_PUBLIC_KEY));
                        assertTrue(verify(encAesKeyByte, encAesKeySignedByte, SERVER_PUBLIC_KEY));

                        // Decrypt RSA
                        byte[] decAesKey = decryptRSA(encAesKeyByte, TZ_PRIVATE_KEY);
                        assertNotNull(decAesKey);
                        String aesIVAndKey = new String(decAesKey);
                        byte[] aesIVAndKeyArray = Hex.decode(aesIVAndKey);
                        assertEquals((16 + 32), aesIVAndKeyArray.length);

                        // Decrypt AES
                        byte[] ivArray = Arrays.copyOfRange(aesIVAndKeyArray, 0, 16);
                        byte[] pkArray = Arrays.copyOfRange(aesIVAndKeyArray, 16, 48);
                        String decCodePK = decryptAES(pkArray, ivArray, encCodePKByte);
                        assertFalse(TextUtils.isEmpty(decCodePK));
                        String[] decCodePKArray = decCodePK.split(":");

                        assertEquals(verifyCode, decCodePKArray[0]);
                        assertEquals(publicKey, decCodePKArray[1]);

                        countDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@NonNull Call<BackupCodePkResponseBody> call,
                            @NonNull Throwable t) {
                        super.onFailure(call, t);
                        fail(t.toString());
                        countDownLatch.countDown();
                    }
                });
            }
        }).start();

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            fail(e.toString());
        }
    }

    @Test
    public void postVerifyCodeTest() {

        final String verifyCode = "123456";
        assertTrue(PinCodeUtil.isValidPinCode(verifyCode));

        byte[] encCodeArray = encryptRSA(verifyCode.getBytes(), SERVER_PUBLIC_KEY);
        final String encCode = Base64.encodeToString(encCodeArray, Base64.DEFAULT);

        // Sign encrypted code
        byte[] encCodeSignedArray = sign(encCodeArray, TZ_PRIVATE_KEY);
        final String encCodeSigned = Base64.encodeToString(encCodeSignedArray, Base64.DEFAULT);

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final KeyServerManager keyServerManager = KeyServerManager.getInstance(true);
                final KeyServerApiService keyServerApiService =
                        keyServerManager.getKeyServerApiService();
                final RestoreCodeRequestBody requestBody = new RestoreCodeRequestBody(
                        CONTEXT, keyServerManager.getTzApiKey(CONTEXT), encCode, encCodeSigned,
                        TZ_ID_HASH);

                // device attest token
                final String attestToken = SafetyNetWrapper.getInstance().getDeviceAttestToken(
                        CONTEXT, true, false);
                if (TextUtils.isEmpty(attestToken)) {
                    LogUtil.logError(TAG, "attestToken is null or empty",
                            new IllegalStateException("attestToken is null or empty"));
                    return;
                }

                Call<RestoreCodeResponseBody> call = keyServerApiService.postVerifyCodeV2(
                        attestToken, requestBody);
                call.enqueue(new KeyServerCallbackWithRetry<RestoreCodeResponseBody>(call) {
                    @Override
                    public void onResponse(@NonNull Call<RestoreCodeResponseBody> call,
                            @NonNull Response<RestoreCodeResponseBody> response) {
                        super.onResponse(call, response);
                        assertTrue(response.isSuccessful());
                        RestoreCodeResponseBody responseBody = response.body();
                        assertNotNull(responseBody);
                        assertEquals(verifyCode, responseBody.getCode());
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@NonNull Call<RestoreCodeResponseBody> call,
                            @NonNull Throwable t) {
                        super.onFailure(call, t);
                        fail(t.toString());
                        countDownLatch.countDown();
                    }
                });
            }
        }).start();

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            fail(e.toString());
        }
    }

    @Test
    public void postEncryptSeedTest() {

        final String seed =
                "Al1yVvIlDIRR/PvBxPWGvxI3EZei0d2xkMPk5Q7CcUhYnrXBplPE7a1pbgNh7tbqGn4VYYCz/1vg\n"
                        +
                        "QFDLc1bQ1RrbZoMqruHnQqx3Wdlxh92EhvBGzRaZTFZW5F2DLC6ahtYRsOgkrBKfPRg61VTowW4=\n";

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final KeyServerManager keyServerManager = KeyServerManager.getInstance(true);
                final KeyServerApiService keyServerApiService =
                        keyServerManager.getKeyServerApiService();
                final RestoreSeedRequestBody requestBody =
                        new RestoreSeedRequestBody(CONTEXT, keyServerManager.getTzApiKey(CONTEXT),
                                TZ_ID_HASH, seed);

                // device attest token
                final String attestToken = SafetyNetWrapper.getInstance().getDeviceAttestToken(
                        CONTEXT, true, false);
                if (TextUtils.isEmpty(attestToken)) {
                    LogUtil.logError(TAG, "attestToken is null or empty",
                            new IllegalStateException("attestToken is null or empty"));
                    return;
                }

                Call<RestoreSeedResponseBody> call = keyServerApiService.postEncryptSeedV2(
                        attestToken, requestBody);
                call.enqueue(new KeyServerCallbackWithRetry<RestoreSeedResponseBody>(call) {
                    @Override
                    public void onResponse(@NonNull Call<RestoreSeedResponseBody> call,
                            @NonNull Response<RestoreSeedResponseBody> response) {
                        super.onResponse(call, response);
                        assertTrue(response.isSuccessful());
                        RestoreSeedResponseBody responseBody = response.body();
                        assertNotNull(responseBody);

                        final String encSeed = responseBody.getEncSeed();
                        final String encSeedSigned = responseBody.getEncSeedSigned();
                        assertFalse(TextUtils.isEmpty(encSeed));
                        assertFalse(TextUtils.isEmpty(encSeedSigned));

                        byte[] encSeedByte = Base64.decode(encSeed, Base64.DEFAULT);
                        byte[] encSeedSignedByte = Base64.decode(encSeedSigned, Base64.DEFAULT);
                        assertTrue(verify(encSeedByte, encSeedSignedByte, SERVER_PUBLIC_KEY));

                        byte[] decSeedBytes = decryptRSA(encSeedByte, TZ_PRIVATE_KEY);
                        assertNotNull(decSeedBytes);
                        assertEquals(seed, Base64.encodeToString(decSeedBytes, Base64.DEFAULT));
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onFailure(@NonNull Call<RestoreSeedResponseBody> call,
                            @NonNull Throwable t) {
                        super.onFailure(call, t);
                        fail(t.toString());
                        countDownLatch.countDown();
                    }
                });
            }
        }).start();

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            fail(e.toString());
        }
    }
}
