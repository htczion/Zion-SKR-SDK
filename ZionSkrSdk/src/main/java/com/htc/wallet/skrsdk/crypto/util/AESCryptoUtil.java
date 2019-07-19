package com.htc.wallet.skrsdk.crypto.util;

import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.crypto.Base64Util;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESCryptoUtil {
    private static final String TAG = "AESCryptoUtil";

    // AES/CBC/PKCS7Padding
    private static final String KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES;
    private static final String BLOCKING_MODE = KeyProperties.BLOCK_MODE_CBC;
    private static final String ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7;

    private static final String CIPHER_SETTING = String.format("%s/%s/%s", KEY_ALGORITHM,
            BLOCKING_MODE, ENCRYPTION_PADDING);

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    // KEY:IV
    private static final String SPLIT_REGEX = ":";

    private static final int KEY_LENGTH = 32;
    private static final int IV_LENGTH = 16;

    private final SecretKeySpec mSecretKeySpec;
    private final IvParameterSpec mIvParameterSpec;
    private final String mKeyAndIv;

    private AESCryptoUtil(byte[] key, byte[] iv) {
        mSecretKeySpec = new SecretKeySpec(key, KEY_ALGORITHM);
        mIvParameterSpec = new IvParameterSpec(iv);
        mKeyAndIv = Base64Util.encodeToString(key) + SPLIT_REGEX + Base64Util.encodeToString(iv);
    }

    public static AESCryptoUtil newInstance() {
        byte[] key = new byte[KEY_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(key);
        secureRandom.nextBytes(iv);

        return new AESCryptoUtil(key, iv);
    }

    @Nullable
    public static AESCryptoUtil getInstance(@NonNull String keyAndIv) {
        if (TextUtils.isEmpty(keyAndIv)) {
            LogUtil.logError(TAG, "keyAndIv is null or empty");
            return null;
        }

        String[] keyAndIvArray = keyAndIv.split(SPLIT_REGEX);
        if (keyAndIvArray.length != 2) {
            LogUtil.logError(TAG, "Incorrect format, length=" + keyAndIvArray.length);
            return null;
        }


        byte[] key = tryDecodeBase64(keyAndIvArray[0]);
        if (key == null || key.length != KEY_LENGTH) {
            LogUtil.logError(TAG, "Incorrect key=" + keyAndIvArray[0]);
            return null;
        }

        byte[] iv = tryDecodeBase64(keyAndIvArray[1]);
        if (iv == null || iv.length != IV_LENGTH) {
            LogUtil.logError(TAG, "Incorrect iv=" + keyAndIvArray[1]);
            return null;
        }

        return new AESCryptoUtil(key, iv);
    }

    @Nullable
    private static byte[] tryDecodeBase64(String text) {
        try {
            return Base64Util.decode(text);
        } catch (IllegalArgumentException e) {
            LogUtil.logError(TAG, "Base64Util.decode() failed, e=" + e);
            return null;
        }
    }

    public String getKeyAndIv() {
        return mKeyAndIv;
    }

    @Nullable
    public String encrypt(@NonNull String data) {
        if (TextUtils.isEmpty(data)) {
            LogUtil.logError(TAG, "encrypt() failed, data is empty");
            return null;
        }

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_SETTING);
            cipher.init(Cipher.ENCRYPT_MODE, mSecretKeySpec, mIvParameterSpec);
            return Base64Util.encodeToString(cipher.doFinal(data.getBytes(CHARSET)));
        } catch (NoSuchAlgorithmException e) {
            LogUtil.logError(TAG, "encrypt() failed, NoSuchAlgorithmException e=" + e);
        } catch (NoSuchPaddingException e) {
            LogUtil.logError(TAG, "encrypt() failed, NoSuchPaddingException e=" + e);
        } catch (InvalidAlgorithmParameterException e) {
            LogUtil.logError(TAG, "encrypt() failed, InvalidAlgorithmParameterException e=" + e);
        } catch (InvalidKeyException e) {
            LogUtil.logError(TAG, "encrypt() failed, InvalidKeyException e=" + e);
        } catch (BadPaddingException e) {
            LogUtil.logError(TAG, "encrypt() failed, BadPaddingException e=" + e);
        } catch (IllegalBlockSizeException e) {
            LogUtil.logError(TAG, "encrypt() failed, IllegalBlockSizeException e=" + e);
        }
        return null;
    }

    @Nullable
    public String decrypt(@NonNull String encryptedData) {
        if (TextUtils.isEmpty(encryptedData)) {
            LogUtil.logError(TAG, "decrypt() failed, encryptedData is empty");
            return null;
        }

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_SETTING);
            cipher.init(Cipher.DECRYPT_MODE, mSecretKeySpec, mIvParameterSpec);
            return new String(cipher.doFinal(Base64Util.decode(encryptedData)), CHARSET);
        } catch (NoSuchAlgorithmException e) {
            LogUtil.logError(TAG, "decrypt() failed, NoSuchAlgorithmException e=" + e);
        } catch (NoSuchPaddingException e) {
            LogUtil.logError(TAG, "decrypt() failed, NoSuchPaddingException e=" + e);
        } catch (InvalidAlgorithmParameterException e) {
            LogUtil.logError(TAG, "decrypt() failed, InvalidAlgorithmParameterException e=" + e);
        } catch (InvalidKeyException e) {
            LogUtil.logError(TAG, "decrypt() failed, InvalidKeyException e=" + e);
        } catch (BadPaddingException e) {
            LogUtil.logError(TAG, "decrypt() failed, BadPaddingException e=" + e);
        } catch (IllegalBlockSizeException e) {
            LogUtil.logError(TAG, "decrypt() failed, IllegalBlockSizeException e=" + e);
        } catch (IllegalArgumentException e) {
            LogUtil.logError(TAG, "encrypt() failed, IllegalArgumentException e=" + e);
        }
        return null;
    }
}
