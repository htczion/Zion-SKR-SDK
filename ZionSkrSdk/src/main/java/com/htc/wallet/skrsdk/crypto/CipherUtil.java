package com.htc.wallet.skrsdk.crypto;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.htc.wallet.skrsdk.keystore.SocialKeyStore;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;

class CipherUtil {
    private static final String TAG = "CipherUtil";
    private static final String SPLIT_REGEX = ":";
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    final SocialKeyStore keyStore;

    CipherUtil(@NonNull final SocialKeyStore keyStore) {
        this.keyStore = Objects.requireNonNull(keyStore);
    }

    String encrypt(
            final String plainText,
            @NonNull final Key key,
            @NonNull final Cipher cipher,
            AlgorithmParameterSpec spec) {
        if (TextUtils.isEmpty(plainText) || key == null || cipher == null) {
            throw new IllegalArgumentException("plainText is empty or key, cipher is null");
        }

        try {
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            return generateCipherText(plainText, cipher);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            Log.e(TAG, "encrypt, error = " + e);
        }
        return plainText;
    }

    byte[] encrypt(
            final byte[] data,
            @NonNull final Key key,
            @NonNull final Cipher cipher,
            AlgorithmParameterSpec spec) {
        if (data == null || data.length == 0 || key == null || cipher == null || spec == null) {
            throw new IllegalArgumentException("data is empty or key, cipher, spec is null");
        }

        try {
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            return cipher.doFinal(data);
        } catch (InvalidKeyException
                | InvalidAlgorithmParameterException
                | IllegalBlockSizeException
                | BadPaddingException e) {
            Log.e(TAG, "encrypt, error = " + e);
        }
        return data;
    }

    private String generateCipherText(final String plainText, @NonNull final Cipher cipher) {
        try {
            if (TextUtils.isEmpty(plainText) || cipher == null) {
                throw new IllegalArgumentException("setupCipherText, plainText, cipher is null");
            }
            // Because operation of Cipher is not thread-safe, we will create multi cipher instance
            // for encryption or decryption.
            // But getIV() of each instance returns different IV.
            // We need to save each IV for different cipher instance.
            // Thus, we append decryption text with IV.
            final byte[] IV = cipher.getIV();
            final String cipherText =
                    Base64Util.encodeToString(cipher.doFinal(plainText.getBytes(CHARSET)));
            if (IV != null) {
                StringBuilder builder = new StringBuilder(cipherText);
                builder.append(SPLIT_REGEX);
                builder.append(Base64Util.encodeToString(IV));
                return builder.toString();
            } else {
                return cipherText;
            }
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            Log.e(TAG, "setupCipherText, error = " + e);
        }

        return plainText;
    }

    String decrypt(final String cipherText, @NonNull final Key key, @NonNull final Cipher cipher) {
        if (TextUtils.isEmpty(cipherText) || key == null || cipher == null) {
            throw new IllegalArgumentException("cipherText is empty or key, cipher is null");
        }
        try {
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64Util.decode(cipherText)), CHARSET);
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            Log.e(TAG, "decrypt, error = " + e);
        }
        return cipherText;
    }

    byte[] decrypt(final byte[] data, @NonNull final Key key, @NonNull final Cipher cipher) {
        if (data == null || data.length == 0 || key == null || cipher == null) {
            throw new IllegalArgumentException("data is empty or key, cipher is null");
        }
        try {
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            Log.e(TAG, "decrypt, error = " + e);
        }
        return data;
    }

    String decrypt(
            final byte[] IV,
            final String cipherText,
            @NonNull final Key key,
            @NonNull final Cipher cipher) {
        if (IV == null
                || IV.length == 0
                || TextUtils.isEmpty(cipherText)
                || key == null
                || cipher == null) {
            throw new IllegalArgumentException(
                    "cipherText is empty or key, IV, cipherText, cipher, key is null");
        }
        try {
            IvParameterSpec spec = new IvParameterSpec(IV);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            return new String(cipher.doFinal(Base64Util.decode(cipherText)), CHARSET);
        } catch (InvalidKeyException
                | BadPaddingException
                | IllegalBlockSizeException
                | InvalidAlgorithmParameterException e) {
            Log.e(TAG, "decrypt, error = " + e);
        }
        return cipherText;
    }

    String convertToString(@NonNull final Key key) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        return Base64Util.encodeToString(key.getEncoded());
    }
}
