package com.htc.wallet.skrsdk.deprecated.sharedprefs;

import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupTargetTest.UUID_HASH;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupTargetTest.assertBad;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupTargetTest.assertOK;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupTargetTest.assertSeededRequest;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupTargetTest.assertUnSeededRequest;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupTargetTest.newBad;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupTargetTest.newOK;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupTargetTest.newSeededRequest;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupTargetTest.newUnSeededRequest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.Test;

public class BackupTargetUtilTest {

    private static final Context CONTEXT = InstrumentationRegistry.getTargetContext();

    @Test
    public void putSeededRequestTest() {
        // Put
        boolean isSucceeded = BackupTargetUtil.put(CONTEXT, newSeededRequest());
        assertTrue(isSucceeded);
        // Get
        BackupTarget target = BackupTargetUtil.get(CONTEXT, UUID_HASH);
        assertNotNull(target);
        // Decrypt by BackupTargetUtil
        assertFalse(target.isEncrypted());
        // Member
        assertSeededRequest(target);
        // Remove
        boolean removeSucceeded = BackupTargetUtil.remove(CONTEXT, UUID_HASH);
        assertTrue(removeSucceeded);
        assertNull(BackupTargetUtil.get(CONTEXT, UUID_HASH));
    }

    @Test
    public void putUnSeededRequestTest() {
        // Put
        boolean isSucceeded = BackupTargetUtil.put(CONTEXT, newUnSeededRequest());
        assertTrue(isSucceeded);
        // Get
        BackupTarget target = BackupTargetUtil.get(CONTEXT, UUID_HASH);
        assertNotNull(target);
        // Decrypt by BackupTargetUtil
        assertFalse(target.isEncrypted());
        // Member
        assertUnSeededRequest(target);
        // Remove
        boolean removeSucceeded = BackupTargetUtil.remove(CONTEXT, UUID_HASH);
        assertTrue(removeSucceeded);
        assertNull(BackupTargetUtil.get(CONTEXT, UUID_HASH));
    }

    @Test
    public void putOKTest() {
        // Put
        boolean putSucceeded = BackupTargetUtil.put(CONTEXT, newOK());
        assertTrue(putSucceeded);
        // Get
        BackupTarget target = BackupTargetUtil.get(CONTEXT, UUID_HASH);
        assertNotNull(target);
        // Decrypt by BackupTargetUtil
        assertFalse(target.isEncrypted());
        // Member
        assertOK(target);
        // Remove
        boolean removeSucceeded = BackupTargetUtil.remove(CONTEXT, UUID_HASH);
        assertTrue(removeSucceeded);
        assertNull(BackupTargetUtil.get(CONTEXT, UUID_HASH));
    }

    @Test
    public void putBadTest() {
        // Put
        boolean putSucceeded = BackupTargetUtil.put(CONTEXT, newBad());
        assertTrue(putSucceeded);
        // Get
        BackupTarget target = BackupTargetUtil.get(CONTEXT, UUID_HASH);
        assertNotNull(target);
        // Decrypt by BackupTargetUtil
        assertFalse(target.isEncrypted());
        // Member
        assertBad(target);
        // Remove
        boolean removeSucceeded = BackupTargetUtil.remove(CONTEXT, UUID_HASH);
        assertTrue(removeSucceeded);
        assertNull(BackupTargetUtil.get(CONTEXT, UUID_HASH));
    }

    @Test
    public void putMultiTest() {
        boolean putSucceeded;
        BackupTarget target;
        // 1. UnSeeded Request
        putSucceeded = BackupTargetUtil.put(CONTEXT, newUnSeededRequest());
        assertTrue(putSucceeded);
        target = BackupTargetUtil.get(CONTEXT, UUID_HASH);
        assertNotNull(target);
        assertFalse(target.isEncrypted());
        assertUnSeededRequest(target);
        // 2. Seeded Request
        putSucceeded = BackupTargetUtil.put(CONTEXT, newSeededRequest());
        assertTrue(putSucceeded);
        target = BackupTargetUtil.get(CONTEXT, UUID_HASH);
        assertNotNull(target);
        assertFalse(target.isEncrypted());
        assertSeededRequest(target);
        // 3. OK
        putSucceeded = BackupTargetUtil.put(CONTEXT, newOK());
        assertTrue(putSucceeded);
        target = BackupTargetUtil.get(CONTEXT, UUID_HASH);
        assertNotNull(target);
        assertFalse(target.isEncrypted());
        assertOK(target);
        // 4. Bad
        putSucceeded = BackupTargetUtil.put(CONTEXT, newBad());
        assertTrue(putSucceeded);
        target = BackupTargetUtil.get(CONTEXT, UUID_HASH);
        assertNotNull(target);
        assertFalse(target.isEncrypted());
        assertBad(target);
        // Remove
        boolean removeSucceed = BackupTargetUtil.remove(CONTEXT, UUID_HASH);
        assertTrue(removeSucceed);
    }

    @Test
    public void removeAllTest() {
        // Put
        boolean isSucceeded = BackupTargetUtil.put(CONTEXT, newUnSeededRequest());
        assertTrue(isSucceeded);

        // Remove All
        boolean removeAllSucceeded = BackupTargetUtil.removeAll(CONTEXT);
        assertTrue(removeAllSucceeded);

        // Get
        assertNull(BackupTargetUtil.get(CONTEXT, UUID_HASH));
    }
}
