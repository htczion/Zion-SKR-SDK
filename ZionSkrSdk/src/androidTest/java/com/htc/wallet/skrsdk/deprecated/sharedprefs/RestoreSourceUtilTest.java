package com.htc.wallet.skrsdk.deprecated.sharedprefs;

import static com.htc.wallet.skrsdk.deprecated.sharedprefs.RestoreSourceTest.UUID_HASH;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.RestoreSourceTest.assertOK;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.RestoreSourceTest.assertRequest;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.RestoreSourceTest.newOK;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.RestoreSourceTest.newRequest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.Test;

public class RestoreSourceUtilTest {

    private static final Context CONTEXT = InstrumentationRegistry.getTargetContext();

    @Test
    public void putRequestTest() {
        // Put
        boolean isSucceeded = RestoreSourceUtil.put(CONTEXT, newRequest());
        assertTrue(isSucceeded);
        // Get
        RestoreSource target = RestoreSourceUtil.getWithUUIDHash(CONTEXT, UUID_HASH);
        assertNotNull(target);
        // Decrypt by RestoreSourceUtil
        assertFalse(target.isEncrypted());
        // Member
        assertRequest(target);
        // Remove
        boolean removeSucceeded = RestoreSourceUtil.removeWithUUIDHash(CONTEXT, UUID_HASH);
        assertTrue(removeSucceeded);
        assertNull(RestoreSourceUtil.getWithUUIDHash(CONTEXT, UUID_HASH));
    }

    @Test
    public void putOKTest() {
        // Put
        boolean isSucceeded = RestoreSourceUtil.put(CONTEXT, newOK());
        assertTrue(isSucceeded);
        // Get
        RestoreSource target = RestoreSourceUtil.getWithUUIDHash(CONTEXT, UUID_HASH);
        assertNotNull(target);
        // Decrypt by RestoreSourceUtil
        assertFalse(target.isEncrypted());
        // Member
        assertOK(target);
        // Remove
        boolean removeSucceeded = RestoreSourceUtil.removeWithUUIDHash(CONTEXT, UUID_HASH);
        assertTrue(removeSucceeded);
        assertNull(RestoreSourceUtil.getWithUUIDHash(CONTEXT, UUID_HASH));
    }

    @Test
    public void putMultiTest() {
        boolean putSucceeded;
        RestoreSource target;
        // 1. Request
        putSucceeded = RestoreSourceUtil.put(CONTEXT, newRequest());
        assertTrue(putSucceeded);
        target = RestoreSourceUtil.getWithUUIDHash(CONTEXT, UUID_HASH);
        assertNotNull(target);
        assertFalse(target.isEncrypted());
        assertRequest(target);
        // 2. OK
        putSucceeded = RestoreSourceUtil.put(CONTEXT, newOK());
        assertTrue(putSucceeded);
        target = RestoreSourceUtil.getWithUUIDHash(CONTEXT, UUID_HASH);
        assertNotNull(target);
        assertFalse(target.isEncrypted());
        assertOK(target);
        // Remove
        boolean removeSucceed = RestoreSourceUtil.removeWithUUIDHash(CONTEXT, UUID_HASH);
        assertTrue(removeSucceed);
    }

    @Test
    public void removeAllTest() {
        // Put
        boolean isSucceeded = RestoreSourceUtil.put(CONTEXT, newRequest());
        assertTrue(isSucceeded);

        // Remove All
        boolean removeAllSucceeded = RestoreSourceUtil.removeAll(CONTEXT);
        assertTrue(removeAllSucceeded);

        // Get
        assertNull(RestoreSourceUtil.getWithUUIDHash(CONTEXT, UUID_HASH));
    }
}
