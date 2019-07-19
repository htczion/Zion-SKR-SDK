package com.htc.wallet.skrsdk.keystore;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import com.htc.wallet.skrsdk.util.LogUtil;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class AndroidKeyStore implements SocialKeyStore {
    private static final String TAG = "AndroidKeyStore";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";

    private static final String ASYMMETRIC_KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_RSA;
    private static final String SYMMETRIC_KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES;
    private static final String SIGN_ALGORITHM = "SHA256withRSA";

    private static final String SIGNATURE_PADDING_TYPE = KeyProperties.SIGNATURE_PADDING_RSA_PKCS1;
    private static final String SYMMETRIC_ENCRYPTION_PADDING_TYPE =
            KeyProperties.ENCRYPTION_PADDING_PKCS7;
    private static final String ASYMMETRIC_ENCRYPTION_PADDING_TYPE =
            KeyProperties.ENCRYPTION_PADDING_RSA_OAEP;
    private static final String ASYMMETRIC_ENCRYPTION_CIPHER_PADDING_TYPE =
            "OAEPWithSHA-1AndMGF1Padding";

    private static final String ASYMMETRIC_BLOCKING_MODE = KeyProperties.BLOCK_MODE_ECB;
    private static final String SYMMETRIC_BLOCKING_MODE = KeyProperties.BLOCK_MODE_CBC;

    private static final int PURPOSE =
            KeyProperties.PURPOSE_ENCRYPT
                    | KeyProperties.PURPOSE_DECRYPT
                    | KeyProperties.PURPOSE_SIGN
                    | KeyProperties.PURPOSE_VERIFY;

    private static final String[] DIGEST_TYPES =
            new String[]{KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA256};

    // Maximum number of bytes for 4096 bits RSA encrypting is 512 bytes.
    // However, public key size of 4096 bits RSA is about 780 bytes.
    // Therefore, we need shorten public key size backup target.
    private static final int BACKUP_SOURCE_ASYMMETRIC_KEY_BITS_SIZE = 4096;
    private static final int BACKUP_TARGET_ASYMMETRIC_KEY_BITS_SIZE = 2048;
    private static final int SYMMETRIC_KEY_BYTES_SIZE = 256;
    private boolean isBackupTarget = false;

    @Override
    @WorkerThread
    public void generateNewAsymmetricKeyPair(final String alias) {
        if (TextUtils.isEmpty(alias)) {
            throw new IllegalArgumentException("generateNewAsymmetricKeyPair, alias is empty");
        }
        try {
            KeyPairGenerator pairGenerator =
                    KeyPairGenerator.getInstance(ASYMMETRIC_KEY_ALGORITHM, KEYSTORE_PROVIDER);
            AlgorithmParameterSpec spec = createAsymmetricKeyGenParameterSpec(alias);
            pairGenerator.initialize(spec);
            pairGenerator.generateKeyPair();
        } catch (InvalidAlgorithmParameterException
                | NoSuchAlgorithmException
                | NoSuchProviderException e) {
            Log.e(TAG, "generateNewAsymmetricKeyPair, error = " + e);
        }
    }

    private AlgorithmParameterSpec createAsymmetricKeyGenParameterSpec(final String alias) {
        if (TextUtils.isEmpty(alias)) {
            throw new IllegalArgumentException(
                    "createAsymmetricKeyGenParameterSpec, alias is empty");
        }
        KeyGenParameterSpec.Builder specBuilder =
                new KeyGenParameterSpec.Builder(alias, PURPOSE)
                        .setEncryptionPaddings(ASYMMETRIC_ENCRYPTION_PADDING_TYPE)
                        .setDigests(DIGEST_TYPES)
                        .setSignaturePaddings(SIGNATURE_PADDING_TYPE)
                        .setBlockModes(ASYMMETRIC_BLOCKING_MODE);

        if (isBackupTarget) {
            specBuilder.setKeySize(BACKUP_TARGET_ASYMMETRIC_KEY_BITS_SIZE);
        } else {
            specBuilder.setKeySize(BACKUP_SOURCE_ASYMMETRIC_KEY_BITS_SIZE);
        }
        return specBuilder.build();
    }

    @Override
    @WorkerThread
    public void generateNewSymmetricKeys(final String alias) {
        if (TextUtils.isEmpty(alias)) {
            throw new IllegalArgumentException("generateNewSymmetricKeys, alias is empty");
        }
        try {
            KeyGenerator keyGenerator =
                    KeyGenerator.getInstance(SYMMETRIC_KEY_ALGORITHM, KEYSTORE_PROVIDER);
            AlgorithmParameterSpec keySpec = createSymmetryKeyGenParameterSpec(alias);
            keyGenerator.init(keySpec);
            keyGenerator.generateKey();
        } catch (InvalidAlgorithmParameterException
                | NoSuchAlgorithmException
                | NoSuchProviderException e) {
            Log.e(TAG, "generateNewSymmetricKeys, error = " + e);
        }
    }

    private AlgorithmParameterSpec createSymmetryKeyGenParameterSpec(final String alias) {
        if (TextUtils.isEmpty(alias)) {
            throw new IllegalArgumentException("createSymmetryKeyGenParameterSpec, alias is empty");
        }
        return new KeyGenParameterSpec.Builder(
                alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setEncryptionPaddings(SYMMETRIC_ENCRYPTION_PADDING_TYPE)
                .setBlockModes(SYMMETRIC_BLOCKING_MODE)
                .setKeySize(SYMMETRIC_KEY_BYTES_SIZE)
                .build();
    }

    @Override
    public void deleteKeys(final String alias) {
        if (TextUtils.isEmpty(alias)) {
            throw new IllegalArgumentException("deleteKeys, alias is empty");
        }
        if (!isKeyStoreExist(alias)) {
            LogUtil.logDebug(TAG, "deleteKeys, store doesn't exist");
            return;
        }
        if (loadKeyStore() == null) {
            LogUtil.logDebug(TAG, "deleteKeys, loadKeyStore() is null");
            return;
        }
        try {
            loadKeyStore().deleteEntry(alias);
        } catch (KeyStoreException e) {
            Log.e(TAG, "deleteKeys, error = " + e);
        }
    }

    @Override
    @Nullable
    public PublicKey getPublicKey(final String alias) {
        if (TextUtils.isEmpty(alias)) {
            throw new IllegalArgumentException("getPublicKey, alias is empty");
        }
        if (!anyKeysExist(alias)) {
            return null;
        }
        return fetchKeyPair(loadKeyStore(), alias).getPublic();
    }

    @Override
    @Nullable
    public PrivateKey getPrivateKey(final String alias) {
        if (TextUtils.isEmpty(alias)) {
            throw new IllegalArgumentException("getPrivateKey, alias is empty");
        }
        if (!anyKeysExist(alias)) {
            return null;
        }
        return fetchKeyPair(loadKeyStore(), alias).getPrivate();
    }

    @Override
    @Nullable
    public SecretKey getSecretKey(final String alias) {
        if (TextUtils.isEmpty(alias)) {
            throw new IllegalArgumentException("getSecretKey, alias is empty");
        }
        if (!anyKeysExist(alias)) {
            return null;
        }
        SecretKey secretKey = null;
        try {
            secretKey = (SecretKey) loadKeyStore().getKey(alias, null);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            Log.e(TAG, "getSecretKey, error = " + e);
        }
        return secretKey;
    }

    @Override
    @NonNull
    public Cipher getAsymmetricCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        return Cipher.getInstance(
                String.format(
                        "%s/%s/%s",
                        ASYMMETRIC_KEY_ALGORITHM,
                        ASYMMETRIC_BLOCKING_MODE,
                        ASYMMETRIC_ENCRYPTION_CIPHER_PADDING_TYPE));
    }

    @Override
    @NonNull
    public Cipher getSymmetricCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        return Cipher.getInstance(
                String.format(
                        "%s/%s/%s",
                        SYMMETRIC_KEY_ALGORITHM,
                        SYMMETRIC_BLOCKING_MODE,
                        SYMMETRIC_ENCRYPTION_PADDING_TYPE));
    }

    @Override
    public String getSignAlgorithm() {
        return SIGN_ALGORITHM;
    }

    @Override
    public String getAsymmetricKeyAlgorithm() {
        return ASYMMETRIC_KEY_ALGORITHM;
    }

    @Override
    public boolean anyKeysExist(final String alias) {
        if (TextUtils.isEmpty(alias)) {
            throw new IllegalArgumentException("anyKeysExist, alias is empty");
        }
        if (isKeyStoreExist(alias)) {
            try {
                return loadKeyStore().isKeyEntry(alias);
            } catch (KeyStoreException e) {
                Log.e(TAG, "anyKeysExist, error = " + e);
            }
        }
        return false;
    }

    @Override
    public boolean isCertificateExist(String alias) {
        if (TextUtils.isEmpty(alias)) {
            throw new IllegalArgumentException("isCertificateExist, alias is empty");
        }

        KeyStore keystore = loadKeyStore();
        if (keystore == null) {
            Log.e(TAG, "keystore is null");
            return false;
        }

        try {
            Certificate certificate = keystore.getCertificate(alias);
            return (certificate != null);
        } catch (KeyStoreException e) {
            Log.e(TAG, "getCertificate error, e=" + e);
        }

        return false;
    }

    @Override
    public int getSymmetricKeyBytesSize() {
        return SYMMETRIC_KEY_BYTES_SIZE;
    }

    @Override
    public void changeToBackupTarget() {
        isBackupTarget = true;
    }

    private boolean isKeyStoreExist(final String alias) {
        KeyStore keystore = loadKeyStore();
        return keystore != null && anyAliasExist(keystore, alias);
    }

    private boolean anyAliasExist(@NonNull final KeyStore keystore, final String alias) {
        if (TextUtils.isEmpty(alias) || keystore == null) {
            throw new IllegalArgumentException(
                    "anyAliasExist, keystore is null or alias is empty ");
        }
        try {
            Enumeration<String> aliases = keystore.aliases();
            while (aliases.hasMoreElements()) {
                // alias is case-sensitive
                if (aliases.nextElement().equals(alias)) {
                    return true;
                }
            }
        } catch (KeyStoreException e) {
            Log.e(TAG, "anyAliasExist, error = " + e);
        }
        return false;
    }

    private KeyStore loadKeyStore() {
        KeyStore keystore = null;
        try {
            keystore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keystore.load(null);
        } catch (KeyStoreException
                | CertificateException
                | NoSuchAlgorithmException
                | IOException e) {
            Log.e(TAG, "loadKeyStore, error = " + e);
        }
        return keystore;
    }

    private KeyPair fetchKeyPair(@NonNull final KeyStore keystore, final String alias) {
        if (TextUtils.isEmpty(alias) || keystore == null) {
            throw new IllegalArgumentException("fetchKeyPair, alias is empty or keystore is null");
        }
        KeyPair keyPair = new KeyPair(null, null);
        try {
            // Workaround for exception on Android P
            // Ref:
            // https://stackoverflow.com/questions/52024752/android-p-keystore-exception-android
            // -os-servicespecificexception
            final int ANDROID_VERSION_CODES_P = 28;
            if (Build.VERSION.SDK_INT >= ANDROID_VERSION_CODES_P) {
                PrivateKey privateKey = (PrivateKey) keystore.getKey(alias, null);
                PublicKey publicKey = keystore.getCertificate(alias).getPublicKey();
                keyPair = buildupKeyPair(publicKey, privateKey);
            } else {
                KeyStore.Entry entry = keystore.getEntry(alias, null);
                if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
                    throw new IllegalArgumentException("Not an instance of a PrivateKeyEntry");
                }

                Certificate certificate = keystore.getCertificate(alias);
                keyPair = buildupKeyPair(certificate, (KeyStore.PrivateKeyEntry) entry);
            }
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException e) {
            Log.e(TAG, "fetchKeyPair error, e=" + e);
        }
        return keyPair;
    }

    private KeyPair buildupKeyPair(
            @NonNull final Certificate certificate, @NonNull final KeyStore.PrivateKeyEntry entry) {
        if (certificate == null || entry == null) {
            throw new IllegalArgumentException("buildupKeyPair, certificate or entry is null");
        }
        PublicKey publicKey = certificate.getPublicKey();
        PrivateKey privateKey = entry.getPrivateKey();
        return new KeyPair(publicKey, privateKey);
    }

    private KeyPair buildupKeyPair(
            @NonNull final PublicKey publicKey, @NonNull final PrivateKey privateKey) {
        if (publicKey == null || privateKey == null) {
            throw new IllegalArgumentException("buildupKeyPair, publicKey or privateKey is null");
        }
        return new KeyPair(publicKey, privateKey);
    }
}
