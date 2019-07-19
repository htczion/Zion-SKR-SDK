package com.htc.wallet.skrsdk.keystore;

public class KeyStoreFactory {

    private KeyStoreFactory() {
        throw new AssertionError();
    }

    public static SocialKeyStore getKeyStore() {
        return new AndroidKeyStore();
    }
}
