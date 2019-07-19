package com.htc.wallet.skrsdk.crypto;

import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.keystore.SocialKeyStore;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

public class AsymmetricCipherUtil extends CipherUtil {
    private static final String TAG = "AsymmetricCipherUtil";
    private static final String TYPE_PRIVATE_KEY = "PrivateKey";
    private static final String TYPE_PUBLIC_KEY = "PublicKey";

    public AsymmetricCipherUtil(@NonNull SocialKeyStore keyStore) {
        super(keyStore);
    }

    public String encrypt(final String plainText, @NonNull final PublicKey publicKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        OAEPParameterSpec spec =
                new OAEPParameterSpec(
                        "SHA-1",
                        "MGF1",
                        new MGF1ParameterSpec("SHA-1"),
                        PSource.PSpecified.DEFAULT);
        return super.encrypt(plainText, publicKey, super.keyStore.getAsymmetricCipher(), spec);
    }

    public byte[] encrypt(final byte[] data, @NonNull final PublicKey publicKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        OAEPParameterSpec spec =
                new OAEPParameterSpec(
                        "SHA-1",
                        "MGF1",
                        new MGF1ParameterSpec("SHA-1"),
                        PSource.PSpecified.DEFAULT);
        return super.encrypt(data, publicKey, super.keyStore.getAsymmetricCipher(), spec);
    }

    public String decrypt(final String cipherText, @NonNull final PrivateKey privateKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        return super.decrypt(cipherText, privateKey, keyStore.getAsymmetricCipher());
    }

    public byte[] decrypt(final byte[] data, @NonNull final PrivateKey privateKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        return super.decrypt(data, privateKey, keyStore.getAsymmetricCipher());
    }

    public PublicKey convertToPublicKey(final String key)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        final EncodedKeySpec keySpec = setupAsymmetricKeySpec(key, TYPE_PUBLIC_KEY);
        final KeyFactory keyFactory = setupAsymmetricKeyFactory();
        return keyFactory.generatePublic(keySpec);
    }

    public PrivateKey convertToPrivateKey(final String key)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        final EncodedKeySpec keySpec = setupAsymmetricKeySpec(key, TYPE_PRIVATE_KEY);
        final KeyFactory keyFactory = setupAsymmetricKeyFactory();
        return keyFactory.generatePrivate(keySpec);
    }

    private EncodedKeySpec setupAsymmetricKeySpec(final String key, @KeyType final String keyType) {
        if (TextUtils.isEmpty(key) || TextUtils.isEmpty(keyType)) {
            throw new IllegalArgumentException("key or keyType is empty");
        }

        byte[] buffer = Base64Util.decode(key);
        if (keyType.equals(TYPE_PRIVATE_KEY)) {
            return new PKCS8EncodedKeySpec(buffer);
        }
        return new X509EncodedKeySpec(buffer);
    }

    private KeyFactory setupAsymmetricKeyFactory() throws NoSuchAlgorithmException {
        return KeyFactory.getInstance(keyStore.getAsymmetricKeyAlgorithm());
    }

    @Override
    public String convertToString(@NonNull final Key key) {
        return super.convertToString(key);
    }

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({TYPE_PRIVATE_KEY, TYPE_PUBLIC_KEY})
    @interface KeyType {
    }
}
