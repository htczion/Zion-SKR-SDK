package com.htc.wallet.skrsdk.crypto;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChecksumUtilTest {
    private static final String RAW_MESSAGE = "message";

    @Test
    public void testVerifyChecksum() {
        String checksum = ChecksumUtil.generateChecksum(RAW_MESSAGE);
        assertTrue(ChecksumUtil.verifyMessageWithChecksum(RAW_MESSAGE, checksum));
    }

    @Test
    public void testCheckLength() {
        String checksum = ChecksumUtil.generateChecksum(RAW_MESSAGE);
        assertTrue(ChecksumUtil.getHexChecksumLength() == checksum.length());
    }
}
