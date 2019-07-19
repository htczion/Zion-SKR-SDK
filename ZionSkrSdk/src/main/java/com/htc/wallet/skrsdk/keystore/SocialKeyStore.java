package com.htc.wallet.skrsdk.keystore;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public interface SocialKeyStore {
    void generateNewAsymmetricKeyPair(final String alias);

    void generateNewSymmetricKeys(final String alias);

    void deleteKeys(final String alias);

    PublicKey getPublicKey(final String alias);

    PrivateKey getPrivateKey(final String alias);

    SecretKey getSecretKey(final String alias);

    Cipher getAsymmetricCipher() throws NoSuchPaddingException, NoSuchAlgorithmException;

    Cipher getSymmetricCipher() throws NoSuchPaddingException, NoSuchAlgorithmException;

    String getSignAlgorithm();

    String getAsymmetricKeyAlgorithm();

    boolean anyKeysExist(final String alias);

    // Asymmetric encryption keys should check certificate too
    // While generate key pair not finish, anyKeysExist() return true but without certificate
    boolean isCertificateExist(final String alias);

    int getSymmetricKeyBytesSize();

    void changeToBackupTarget();
}
