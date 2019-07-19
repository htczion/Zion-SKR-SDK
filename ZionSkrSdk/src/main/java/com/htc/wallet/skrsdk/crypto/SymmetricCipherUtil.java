package com.htc.wallet.skrsdk.crypto;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.keystore.SocialKeyStore;

import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class SymmetricCipherUtil extends CipherUtil {
    private static final String SPLIT_REGEX = ":";

    public SymmetricCipherUtil(@NonNull SocialKeyStore keyStore) {
        super(keyStore);
    }

    public String encrypt(final String plainText, @NonNull final SecretKey secretKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        return super.encrypt(plainText, secretKey, keyStore.getSymmetricCipher(), null);
    }

    public String decrypt(final String cipherAndIVText, @NonNull final SecretKey secretKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        final String[] cipherAndIVParts = splitCipherText(cipherAndIVText);
        final String cipherText = cipherAndIVParts[0];
        final byte[] IV = Base64Util.decode(cipherAndIVParts[1]);
        return super.decrypt(IV, cipherText, secretKey, keyStore.getSymmetricCipher());
    }

    private String[] splitCipherText(final String cipherAndIVText) {
        if (TextUtils.isEmpty(cipherAndIVText)) {
            throw new IllegalArgumentException("splitCipherText, cipherText is empty");
        }
        final String[] cipherAndIVParts = cipherAndIVText.split(SPLIT_REGEX);
        if (cipherAndIVParts.length != 2) {
            throw new IllegalArgumentException("splitCipherText fails, size is not correct.");
        }
        return cipherAndIVParts;
    }

    @Override
    public String convertToString(@NonNull final Key key) {
        return super.convertToString(key);
    }
}
