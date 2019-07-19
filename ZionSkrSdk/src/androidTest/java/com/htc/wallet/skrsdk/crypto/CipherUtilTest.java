package com.htc.wallet.skrsdk.crypto;

import static org.junit.Assert.assertTrue;

import android.util.Log;

import com.htc.wallet.skrsdk.keystore.KeyStoreFactory;
import com.htc.wallet.skrsdk.keystore.SocialKeyStore;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class CipherUtilTest {
    private static final String TAG = "CipherUtilTest";
    private static final String DEFAULT_ALIAS = "AndroidKeyStore";
    private final SocialKeyStore mKeyStore = KeyStoreFactory.getKeyStore();
    private final SymmetricCipherUtil mSymmetricCipherUtil = new SymmetricCipherUtil(mKeyStore);
    private final AsymmetricCipherUtil mAsymmetricCipherUtil = new AsymmetricCipherUtil(mKeyStore);

    @Test
    public void testEncryptDecryptWithAsymmetricKeyPair() {
        mKeyStore.generateNewAsymmetricKeyPair(DEFAULT_ALIAS);
        PublicKey publicKey = mKeyStore.getPublicKey(DEFAULT_ALIAS);
        PrivateKey privateKey = mKeyStore.getPrivateKey(DEFAULT_ALIAS);
        String plainText = createBigText();

        try {
            String cipherText = mAsymmetricCipherUtil.encrypt(plainText, publicKey);
            String decryptedText = mAsymmetricCipherUtil.decrypt(cipherText, privateKey);

            assertTrue(plainText.equals(decryptedText));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Log.e(TAG, "testEncryptDecryptWithAsymmetricKeyPair error = " + e);
        }
    }

    @Test
    public void testEncryptDecryptWithMultiAsymmetricInstance() {
        try {
            mKeyStore.generateNewAsymmetricKeyPair(DEFAULT_ALIAS);
            PublicKey publicKey = mKeyStore.getPublicKey(DEFAULT_ALIAS);
            PrivateKey privateKey = mKeyStore.getPrivateKey(DEFAULT_ALIAS);

            final AsymmetricCipherUtil mAsymmetricCipherUtil1 = new AsymmetricCipherUtil(mKeyStore);
            final AsymmetricCipherUtil mAsymmetricCipherUtil2 = new AsymmetricCipherUtil(mKeyStore);
            final AsymmetricCipherUtil mAsymmetricCipherUtil3 = new AsymmetricCipherUtil(mKeyStore);

            String text1 = "123";
            String text2 = "456";
            String text3 = "789";
            String cipherText1 = mAsymmetricCipherUtil1.encrypt(text1, publicKey);
            String cipherText2 = mAsymmetricCipherUtil2.encrypt(text2, publicKey);
            String cipherText3 = mAsymmetricCipherUtil3.encrypt(text3, publicKey);
            String decryptedText1 = mAsymmetricCipherUtil1.decrypt(cipherText1, privateKey);
            String decryptedText2 = mAsymmetricCipherUtil2.decrypt(cipherText2, privateKey);
            String decryptedText3 = mAsymmetricCipherUtil3.decrypt(cipherText3, privateKey);

            assertTrue(text1.equals(decryptedText1));
            assertTrue(text2.equals(decryptedText2));
            assertTrue(text3.equals(decryptedText3));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Log.e(TAG, "testEncryptDecryptTimeMultiAsymmetricInstance error = " + e);
        }
    }

    @Test
    public void testEncryptDecryptWithDifferentAsymmetricInstance() {
        try {
            mKeyStore.generateNewAsymmetricKeyPair(DEFAULT_ALIAS);
            PublicKey publicKey = mKeyStore.getPublicKey(DEFAULT_ALIAS);
            PrivateKey privateKey = mKeyStore.getPrivateKey(DEFAULT_ALIAS);
            final AsymmetricCipherUtil mAsymmetricCipherUtil1 = new AsymmetricCipherUtil(mKeyStore);
            final AsymmetricCipherUtil mAsymmetricCipherUtil2 = new AsymmetricCipherUtil(mKeyStore);
            String text = "123";
            String cipherText = mAsymmetricCipherUtil1.encrypt(text, publicKey);
            String decryptedText = mAsymmetricCipherUtil2.decrypt(cipherText, privateKey);

            assertTrue(text.equals(decryptedText));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Log.e(TAG, "testEncryptDecryptTimeDifferentAsymmetricInstance error = " + e);
        }
    }

    @Test
    public void testEncryptDecryptTimeWithSymmetricKeys() {
        mKeyStore.generateNewSymmetricKeys(DEFAULT_ALIAS);
        try {
            String plainText = createText(32);
            SecretKey secretKey = mKeyStore.getSecretKey(DEFAULT_ALIAS);
            String cipherText = mSymmetricCipherUtil.encrypt(plainText, secretKey);
            String decryptedText = mSymmetricCipherUtil.decrypt(cipherText, secretKey);
            assertTrue(plainText.equals(decryptedText));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Log.e(TAG, "testEncryptDecryptTimeWithSymmetricKeys error = " + e);
        }
    }

    @Test
    public void testEncryptDecryptWithMultiSymmetricInstance() {
        mKeyStore.generateNewSymmetricKeys(DEFAULT_ALIAS);
        try {
            SecretKey secretKey = mKeyStore.getSecretKey(DEFAULT_ALIAS);
            final SymmetricCipherUtil mSymmetricCipherUtil1 = new SymmetricCipherUtil(mKeyStore);
            final SymmetricCipherUtil mSymmetricCipherUtil2 = new SymmetricCipherUtil(mKeyStore);
            final SymmetricCipherUtil mSymmetricCipherUtil3 = new SymmetricCipherUtil(mKeyStore);
            String text1 = "123";
            String text2 = "456";
            String text3 = "789";
            String cipherText1 = mSymmetricCipherUtil1.encrypt(text1, secretKey);
            String cipherText2 = mSymmetricCipherUtil2.encrypt(text2, secretKey);
            String cipherText3 = mSymmetricCipherUtil3.encrypt(text3, secretKey);
            String decryptedText1 = mSymmetricCipherUtil1.decrypt(cipherText1, secretKey);
            String decryptedText2 = mSymmetricCipherUtil2.decrypt(cipherText2, secretKey);
            String decryptedText3 = mSymmetricCipherUtil3.decrypt(cipherText3, secretKey);

            assertTrue(text1.equals(decryptedText1));
            assertTrue(text2.equals(decryptedText2));
            assertTrue(text3.equals(decryptedText3));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Log.e(TAG, "testEncryptDecryptTimeMultiSymmetricInstance error = " + e);
        }
    }

    @Test
    public void testEncryptDecryptWithDifferentSymmetricInstance() {
        mKeyStore.generateNewSymmetricKeys(DEFAULT_ALIAS);
        try {
            SecretKey secretKey = mKeyStore.getSecretKey(DEFAULT_ALIAS);
            final SymmetricCipherUtil mSymmetricCipherUtil1 = new SymmetricCipherUtil(mKeyStore);
            final SymmetricCipherUtil mSymmetricCipherUtil2 = new SymmetricCipherUtil(mKeyStore);
            String text1 = "123";
            String cipherText = mSymmetricCipherUtil1.encrypt(text1, secretKey);
            String decryptedText = mSymmetricCipherUtil2.decrypt(cipherText, secretKey);

            assertTrue(text1.equals(decryptedText));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Log.e(TAG, "testEncryptDecryptTimeDifferentSymmetricInstance error = " + e);
        }
    }

    private String createBigText() {
        return createText(245);
    }

    private String createText(final int length) {
        char[] charArray = new char[length];
        for (int i = 0; i < length; i++) {
            charArray[i] = '1';
        }
        return new String(charArray);
    }
}
