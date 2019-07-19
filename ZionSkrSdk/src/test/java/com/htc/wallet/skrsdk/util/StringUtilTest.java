package com.htc.wallet.skrsdk.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StringUtilTest {

    @Test
    public void asciiTest() {
        assertFalse(StringUtil.isAscii("Réal"));
        assertTrue(StringUtil.isAscii("Real"));
    }
}
