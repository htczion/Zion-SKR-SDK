package com.htc.wallet.skrsdk.deprecated.sharedprefs;

import static com.htc.wallet.skrsdk.deprecated.sharedprefs.RestoreTargetTest.EMAIL_HASH;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.RestoreTargetTest.UUID_HASH;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.RestoreTargetTest.UUID_HASH2;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.RestoreTargetTest.assertRestoreTarget1;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.RestoreTargetTest.assertRestoreTarget2;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.RestoreTargetTest.newRestoreTarget1;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.RestoreTargetTest.newRestoreTarget2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.Test;

public class RestoreTargetUtilTest {

    // Util
    private static final Context CONTEXT = InstrumentationRegistry.getTargetContext();

    @Test
    public void putTest() {
        // Put
        boolean putSucceeded = RestoreTargetUtil.put(CONTEXT, newRestoreTarget1());
        assertTrue(putSucceeded);
        // Get
        RestoreTarget source = RestoreTargetUtil.get(CONTEXT, EMAIL_HASH, UUID_HASH);
        assertNotNull(source);
        // Decrypt by RestoreTargetUtil
        assertFalse(source.isEncrypted());
        // Member
        assertRestoreTarget1(source);
        // Remove
        boolean removeSucceeded = RestoreTargetUtil.remove(CONTEXT, EMAIL_HASH, UUID_HASH);
        assertTrue(removeSucceeded);
        assertNull(RestoreTargetUtil.get(CONTEXT, EMAIL_HASH, UUID_HASH));
    }

    @Test
    public void putMultiSameEmailHashDiffUUIDTest() {
        boolean putSucceeded;
        boolean removeSucceed;
        RestoreTarget source;
        // 1. Put RestoreTarget 1
        putSucceeded = RestoreTargetUtil.put(CONTEXT, newRestoreTarget1());
        assertTrue(putSucceeded);
        source = RestoreTargetUtil.get(CONTEXT, EMAIL_HASH, UUID_HASH);
        assertNotNull(source);
        assertFalse(source.isEncrypted());
        assertRestoreTarget1(source);
        // 2. Put RestoreTarget 2
        putSucceeded = RestoreTargetUtil.put(CONTEXT, newRestoreTarget2());
        assertTrue(putSucceeded);
        source = RestoreTargetUtil.get(CONTEXT, EMAIL_HASH, UUID_HASH2);
        assertNotNull(source);
        assertFalse(source.isEncrypted());
        assertRestoreTarget2(source);
        // 3. Try to get RestoreTarget 1
        source = RestoreTargetUtil.get(CONTEXT, EMAIL_HASH, UUID_HASH);
        assertNotNull(source);
        assertFalse(source.isEncrypted());
        assertRestoreTarget1(source);
        // 4. Remove RestoreTarget 1
        removeSucceed = RestoreTargetUtil.remove(CONTEXT, EMAIL_HASH, UUID_HASH);
        assertTrue(removeSucceed);
        // 5. Remove RestoreTarget 2
        removeSucceed = RestoreTargetUtil.remove(CONTEXT, EMAIL_HASH, UUID_HASH2);
        assertTrue(removeSucceed);
        // 6. Put RestoreTarget 1 again
        putSucceeded = RestoreTargetUtil.put(CONTEXT, newRestoreTarget1());
        assertTrue(putSucceeded);
        assertNotNull(RestoreTargetUtil.get(CONTEXT, EMAIL_HASH, UUID_HASH));
        // 7. Put RestoreTarget 2 again
        putSucceeded = RestoreTargetUtil.put(CONTEXT, newRestoreTarget2());
        assertTrue(putSucceeded);
        assertNotNull(RestoreTargetUtil.get(CONTEXT, EMAIL_HASH, UUID_HASH2));
        // 8. Remove All By Email Hash
        removeSucceed = RestoreTargetUtil.removeAllByEmailHash(CONTEXT, EMAIL_HASH);
        assertTrue(removeSucceed);
        // 9. Try to get RestoreTarget 1
        assertNull(RestoreTargetUtil.get(CONTEXT, EMAIL_HASH, UUID_HASH));
        // 10. Try to get RestoreTarget 2
        assertNull(RestoreTargetUtil.get(CONTEXT, EMAIL_HASH, UUID_HASH2));
        // 11. Try to Remove All By Email Hash  again
        removeSucceed = RestoreTargetUtil.removeAllByEmailHash(CONTEXT, EMAIL_HASH);
        assertFalse(removeSucceed);
    }
}
