package com.htc.wallet.skrsdk.crypto;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.support.test.runner.AndroidJUnit4;

import com.htc.wallet.skrsdk.keystore.KeyStoreFactory;
import com.htc.wallet.skrsdk.keystore.SocialKeyStore;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SocialKeyStoreTest {
    private static final String DEFAULT_ALIAS = "AndroidKeyStore";
    private final SocialKeyStore mKeyStore = KeyStoreFactory.getKeyStore();

    @Test
    public void testGenerateNewAsymmetricKeyPair() {
        if (mKeyStore.anyKeysExist(DEFAULT_ALIAS)) {
            mKeyStore.deleteKeys(DEFAULT_ALIAS);
        }
        mKeyStore.generateNewAsymmetricKeyPair(DEFAULT_ALIAS);
        assertTrue(mKeyStore.anyKeysExist(DEFAULT_ALIAS));
    }

    @Test
    public void testGenerateNewSymmetricKeys() {
        if (mKeyStore.anyKeysExist(DEFAULT_ALIAS)) {
            mKeyStore.deleteKeys(DEFAULT_ALIAS);
        }
        mKeyStore.generateNewSymmetricKeys(DEFAULT_ALIAS);
        assertTrue(mKeyStore.anyKeysExist(DEFAULT_ALIAS));
    }

    @Test
    public void testGenerateNewAsymmetricKeyPairWithDifferentAlias() {
        for (int i = 0; i < 5; i++) {
            mKeyStore.generateNewAsymmetricKeyPair(DEFAULT_ALIAS + i);
            assertTrue(mKeyStore.anyKeysExist(DEFAULT_ALIAS + i));
        }
    }

    @Test
    public void testGenerateNewSymmetricKeysWithDifferentAlias() {
        for (int i = 0; i < 5; i++) {
            mKeyStore.generateNewSymmetricKeys(DEFAULT_ALIAS + i);
            assertTrue(mKeyStore.anyKeysExist(DEFAULT_ALIAS + i));
        }
    }

    @Test
    public void testAnyKeysExist() {
        mKeyStore.deleteKeys(DEFAULT_ALIAS);
        assertFalse(mKeyStore.anyKeysExist(DEFAULT_ALIAS));

        mKeyStore.generateNewAsymmetricKeyPair(DEFAULT_ALIAS);
        assertTrue(mKeyStore.anyKeysExist(DEFAULT_ALIAS));

        mKeyStore.deleteKeys(DEFAULT_ALIAS);
        assertFalse(mKeyStore.anyKeysExist(DEFAULT_ALIAS));

        mKeyStore.generateNewSymmetricKeys(DEFAULT_ALIAS);
        assertTrue(mKeyStore.anyKeysExist(DEFAULT_ALIAS));
    }

    @Test
    public void testAnyKeysExistWithCaseSensitiveAlias() {
        final String upperCaseAlias = DEFAULT_ALIAS.toUpperCase();
        final String lowerCaseAlias = DEFAULT_ALIAS.toLowerCase();
        mKeyStore.deleteKeys(upperCaseAlias);
        mKeyStore.deleteKeys(lowerCaseAlias);

        mKeyStore.generateNewAsymmetricKeyPair(upperCaseAlias);
        assertTrue(mKeyStore.anyKeysExist(upperCaseAlias));
        assertFalse(mKeyStore.anyKeysExist(lowerCaseAlias));
        mKeyStore.deleteKeys(upperCaseAlias);

        mKeyStore.generateNewAsymmetricKeyPair(lowerCaseAlias);
        assertTrue(mKeyStore.anyKeysExist(lowerCaseAlias));
        assertFalse(mKeyStore.anyKeysExist(upperCaseAlias));
        mKeyStore.deleteKeys(lowerCaseAlias);

        mKeyStore.generateNewSymmetricKeys(upperCaseAlias);
        assertTrue(mKeyStore.anyKeysExist(upperCaseAlias));
        assertFalse(mKeyStore.anyKeysExist(lowerCaseAlias));
        mKeyStore.deleteKeys(upperCaseAlias);

        mKeyStore.generateNewSymmetricKeys(lowerCaseAlias);
        assertTrue(mKeyStore.anyKeysExist(lowerCaseAlias));
        assertFalse(mKeyStore.anyKeysExist(upperCaseAlias));
        mKeyStore.deleteKeys(lowerCaseAlias);
    }
}
