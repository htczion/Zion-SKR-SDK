package com.htc.wallet.skrsdk.crypto;

import static org.junit.Assert.assertTrue;

import android.support.test.runner.AndroidJUnit4;

import com.htc.wallet.skrsdk.keystore.KeyStoreFactory;
import com.htc.wallet.skrsdk.keystore.SocialKeyStore;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SignatureUtilTest {
    private static final String DEFAULT_ALIAS = "SignatureUtilTest";
    private static final String RAW_MESSAGE = "message";
    private final SocialKeyStore mKeyStore = KeyStoreFactory.getKeyStore();
    private final SignatureUtil mSignatureUtil = new SignatureUtil(mKeyStore);

    @Test
    public void testVerifySignature() {
        if (!mKeyStore.anyKeysExist(DEFAULT_ALIAS)) {
            mKeyStore.generateNewAsymmetricKeyPair(DEFAULT_ALIAS);
        }
        String signature = mSignatureUtil.generateSignature(RAW_MESSAGE, mKeyStore.getPrivateKey(DEFAULT_ALIAS));
        assertTrue(mSignatureUtil.verifySignature(RAW_MESSAGE, signature, mKeyStore.getPublicKey(DEFAULT_ALIAS)));
    }
}
