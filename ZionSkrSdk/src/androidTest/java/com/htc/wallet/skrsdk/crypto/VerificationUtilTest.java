package com.htc.wallet.skrsdk.crypto;

import static org.junit.Assert.assertTrue;

import com.htc.wallet.skrsdk.crypto.util.VerificationUtil;

import org.junit.Test;

public class VerificationUtilTest {
    private static final String TOKEN =
            "ddRhOS6vwvI:APA91bH8pFKv"
                    +
                    "-NtL4Bkk491P4rzWVHyf9eMgeRsfLFQVj5lje_ZSQpxgr8eapt7rcnT2y4KET0xyC2FA_DpKJb6mDmAjT5ezuGEUK5dcD9JKVdAQr3optiXkhd1yjmDfSP6aaIXUEDBj";
    private final VerificationUtil mBackupSourceUtil = new VerificationUtil(false);
    private final VerificationUtil mBackupTargetUtil = new VerificationUtil(true);


    @Test
    public void testVerifyMessage() {
        String encryptedMessage = mBackupTargetUtil.encryptMessage(TOKEN, mBackupSourceUtil.getPublicKeyString());
        String decryptedMessage = mBackupSourceUtil.decryptMessage(encryptedMessage);
        assertTrue(TOKEN.equals(decryptedMessage));

        encryptedMessage = mBackupSourceUtil.encryptMessage(TOKEN, mBackupTargetUtil.getPublicKeyString());
        decryptedMessage = mBackupTargetUtil.decryptMessage(encryptedMessage);
        assertTrue(TOKEN.equals(decryptedMessage));
    }
}
