package com.htc.wallet.skrsdk.applink.crypto;

import android.content.Context;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.adapter.ApiKeyAdapter;
import com.htc.wallet.skrsdk.crypto.Base64Util;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class AppLinkParamsCryptoUtil {
    private static final String TAG = "AppLinkParamsCryptoUtil";
    private static final String ALGORITHM = KeyProperties.KEY_ALGORITHM_AES;
    private static final String BLOCKING_MODE = KeyProperties.BLOCK_MODE_ECB;
    private static final String PADDING_TYPE = KeyProperties.ENCRYPTION_PADDING_PKCS7;
    private static final String FULL_ALGORITHM =
            String.format("%s/%s/%s", ALGORITHM, BLOCKING_MODE, PADDING_TYPE);
    private static final int AES_KEY_LENGTH = 16;

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private final Cipher mEncryptCipher;
    private final Cipher mDecryptCipher;

    public AppLinkParamsCryptoUtil(@NonNull final Context context) {
        if (context == null) {
            LogUtil.logError(
                    TAG, "constructor init fails, context is null", new IllegalArgumentException());
        }

        mEncryptCipher = initEncryptCipher(getCipherKeyBytes(context));
        mDecryptCipher = initDecryptCipher(getCipherKeyBytes(context));
    }

    private Cipher initEncryptCipher(byte[] keyBytesArray) {
        if (keyBytesArray == null) {
            LogUtil.logError(TAG, "initEncryptCipher, keyBytesArray is null");
            return null;
        }
        try {
            SecretKeySpec secretKeySpec = provideSecretKeySpec(keyBytesArray);
            if (secretKeySpec == null) {
                LogUtil.logError(TAG, "initEncryptCipher, secretKeySpec is null");
                return null;
            }
            Cipher cipher = Cipher.getInstance(FULL_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            return cipher;
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            LogUtil.logError(TAG, "initEncryptCipher, error = " + e);
        }
        return null;
    }

    private Cipher initDecryptCipher(byte[] keyBytesArray) {
        if (keyBytesArray == null) {
            LogUtil.logError(TAG, "initDecryptCipher, keyBytesArray is null");
            return null;
        }
        try {
            SecretKeySpec secretKeySpec = provideSecretKeySpec(keyBytesArray);
            if (secretKeySpec == null) {
                LogUtil.logError(TAG, "initDecryptCipher, secretKeySpec is null");
                return null;
            }
            Cipher cipher = Cipher.getInstance(FULL_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            return cipher;
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            LogUtil.logError(TAG, "initDecryptCipher, error = " + e);
        }
        return null;
    }

    private SecretKeySpec provideSecretKeySpec(byte[] keyBytesArray) {
        if (keyBytesArray == null) {
            LogUtil.logDebug(TAG, "provideSecretKeySpec, keyBytesArray is null");
            return null;
        }
        return new SecretKeySpec(keyBytesArray, FULL_ALGORITHM);
    }

    public String encrypt(final String plainText) {
        if (TextUtils.isEmpty(plainText)) {
            LogUtil.logDebug(TAG, "plainText is empty");
            return plainText;
        }
        if (mEncryptCipher == null) {
            LogUtil.logWarning(TAG, "encryptCipher not init");
            return plainText;
        }
        try {
            return Base64Util.encodeToString(mEncryptCipher.doFinal(plainText.getBytes(CHARSET)));
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            LogUtil.logError(TAG, "encrypt, error = " + e);
        }
        return plainText;
    }

    public String decrypt(final String cipherText) {
        if (TextUtils.isEmpty(cipherText)) {
            LogUtil.logDebug(TAG, "cipherText is empty");
            return cipherText;
        }
        if (mDecryptCipher == null) {
            LogUtil.logWarning(TAG, "decryptCipher not init");
            return cipherText;
        }
        try {
            return new String(mDecryptCipher.doFinal(Base64Util.decode(cipherText)), CHARSET);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            LogUtil.logError(TAG, "decrypt, error = " + e);
        }
        return cipherText;
    }

    private byte[] getCipherKeyBytes(@NonNull final Context context) {
        if (context == null) {
            LogUtil.logError(TAG, "getCipherKeyBytes, context is null");
            return null;
        }

        ApiKeyAdapter apiKeyAdapter = ZionSkrSdkManager.getInstance().getApiKeyAdapter();
        if (apiKeyAdapter == null) {
            throw new IllegalStateException("apiKeyAdapter is null");
        }

        String aesKeyString = apiKeyAdapter.getAesKey();
        if (TextUtils.isEmpty(aesKeyString)) {
            LogUtil.logError(TAG, "aesKeyString is empty");
            return null;
        }
        byte[] aesKeyArray = aesKeyString.getBytes(CHARSET);

        if (aesKeyArray == null) {
            LogUtil.logDebug(TAG, "KeyArray is null");
            return null;
        }
        if (aesKeyArray.length != AES_KEY_LENGTH) {
            LogUtil.logDebug(TAG, "KeyArray length isn't correct");
            return null;
        }

        return aesKeyArray;
    }
}
