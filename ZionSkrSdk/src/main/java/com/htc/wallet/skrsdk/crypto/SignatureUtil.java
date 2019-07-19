package com.htc.wallet.skrsdk.crypto;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.htc.wallet.skrsdk.keystore.SocialKeyStore;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Objects;

public class SignatureUtil {
    private static final String TAG = "SignatureUtil";
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private final SocialKeyStore mKeyStore;

    public SignatureUtil(@NonNull final SocialKeyStore keyStore) {
        mKeyStore = Objects.requireNonNull(keyStore);
    }

    public String generateSignature(final String message, @NonNull final PrivateKey privateKey) {
        if (TextUtils.isEmpty(message) || privateKey == null) {
            throw new IllegalArgumentException(
                    "generateSignature, message is empty or privateKey is null");
        }
        try {
            Signature signature = Signature.getInstance(mKeyStore.getSignAlgorithm());
            signature.initSign(privateKey);
            signature.update(message.getBytes(CHARSET));
            return Base64Util.encodeToString(signature.sign());
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            Log.e(TAG, "generateSignature, error = " + e);
        }
        return message;
    }

    public boolean verifySignature(
            final String message,
            @NonNull final String signedMessage,
            @NonNull final PublicKey publicKey) {
        if (TextUtils.isEmpty(message) || publicKey == null) {
            throw new IllegalArgumentException(
                    "verifySignature, message is empty or publicKey is null");
        }

        try {
            Signature signature = Signature.getInstance(mKeyStore.getSignAlgorithm());
            signature.initVerify(publicKey);
            signature.update(message.getBytes(CHARSET));
            return signature.verify(Base64Util.decode(signedMessage));
        } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
            Log.e(TAG, "verifySignature, error = " + e);
        }
        return false;
    }
}
