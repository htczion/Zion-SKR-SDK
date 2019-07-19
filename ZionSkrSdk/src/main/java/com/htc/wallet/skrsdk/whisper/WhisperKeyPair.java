package com.htc.wallet.skrsdk.whisper;

import android.support.annotation.NonNull;

import java.util.Objects;

public class WhisperKeyPair {
    private final String mKeyPairId;
    private final String mPrivateKey;
    private final String mPublicKey;

    public WhisperKeyPair(
            @NonNull String keyPairId, @NonNull String privateKey, @NonNull String publicKey) {
        Objects.requireNonNull(keyPairId, "keyPairId is empty");
        Objects.requireNonNull(privateKey, "privateKey is empty");
        Objects.requireNonNull(publicKey, "publicKey is empty");
        mKeyPairId = keyPairId;
        mPrivateKey = privateKey;
        mPublicKey = publicKey;
    }

    public String getKeyPairId() {
        return mKeyPairId;
    }

    public String getPrivateKey() {
        return mPrivateKey;
    }

    public String getPublicKey() {
        return mPublicKey;
    }
}
