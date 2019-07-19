package com.htc.wallet.skrsdk.crypto;

import static org.junit.Assert.assertTrue;

import com.htc.wallet.skrsdk.crypto.util.GenericCipherUtil;

import org.junit.Test;

public class GenericCipherUtilTest {
    private static final String TOKEN =
            "ddRhOS6vwvI:APA91bH8pFKv"
                    +
                    "-NtL4Bkk491P4rzWVHyf9eMgeRsfLFQVj5lje_ZSQpxgr8eapt7rcnT2y4KET0xyC2FA_DpKJb6mDmAjT5ezuGEUK5dcD9JKVdAQr3optiXkhd1yjmDfSP6aaIXUEDBj";
    private final GenericCipherUtil mTrustContactDataCipherUtil = new GenericCipherUtil();

    @Test
    public void testEncryptAndDecryptToken() {
        String encryptedTrustContactToken = mTrustContactDataCipherUtil.encryptData(TOKEN);
        final String decryptedTrustContactToken = mTrustContactDataCipherUtil.decryptData(encryptedTrustContactToken);

        assertTrue(TOKEN.equals(decryptedTrustContactToken));
    }
}
