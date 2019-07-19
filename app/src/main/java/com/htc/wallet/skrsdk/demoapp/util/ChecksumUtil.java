package com.htc.wallet.skrsdk.demoapp.util;

import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Log;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChecksumUtil {
    private static final String TAG = "ChecksumUtil";
    private static final String CHECKSUM_ALGORITHM = KeyProperties.DIGEST_SHA256;
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    public static final int SHA256_HEX_LENGTH = 64;

    private ChecksumUtil() {
        throw new AssertionError();
    }

    public static String generateChecksum(final String message) {
        if (TextUtils.isEmpty(message)) {
            throw new IllegalArgumentException("generateChecksum, message is empty");
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
            return Hex.encodeToUppercase(messageDigest.digest(message.getBytes(CHARSET)));
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "generateChecksum, error = " + e);
        }

        return message;
    }

    public static boolean verifyMessageWithChecksum(final String message, final String checksum) {
        if (TextUtils.isEmpty(message) || TextUtils.isEmpty(checksum)) {
            throw new IllegalArgumentException(
                    "verifyMessageWithChecksum, message or checksum is empty");
        }
        return generateChecksum(message).equals(checksum);
    }

    public static int getHexChecksumLength() {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
            // To transform Hex String, it needs 2 times space of byte array to record.
            // e.g. byte array {0xBf} -> String "Bf"
            return messageDigest.getDigestLength() * 2;
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "getChecksumLength, error = " + e);
        }
        // In this case, length is always 64
        return SHA256_HEX_LENGTH;
    }
}
