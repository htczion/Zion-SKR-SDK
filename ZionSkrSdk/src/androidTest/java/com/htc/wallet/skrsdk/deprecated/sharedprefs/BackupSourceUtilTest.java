package com.htc.wallet.skrsdk.deprecated.sharedprefs;

import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupSourceTest.EMAIL_HASH;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupSourceTest.UUID_HASH;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupSourceTest.UUID_HASH2;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupSourceTest.assertBackupSourceOK;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupSourceTest.assertBackupSourceOK2;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupSourceTest.assertBackupSourceRequest;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupSourceTest.assertBackupSourceRequest2;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupSourceTest.newBackupSourceOK;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupSourceTest.newBackupSourceOK2;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupSourceTest.newBackupSourceRequest;
import static com.htc.wallet.skrsdk.deprecated.sharedprefs.BackupSourceTest.newBackupSourceRequest2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.Test;

public class BackupSourceUtilTest {

    // Util
    private static final Context CONTEXT = InstrumentationRegistry.getTargetContext();

    @Test
    public void putRequestTest() {
        // Put
        boolean putSucceeded = BackupSourceUtil.put(CONTEXT, newBackupSourceRequest());
        assertTrue(putSucceeded);
        // Get
        BackupSource source = BackupSourceUtil.getWithUUIDHash(CONTEXT, EMAIL_HASH, UUID_HASH);
        assertNotNull(source);
        // Decrypt by BackupSourceUtil
        assertFalse(source.isEncrypted());
        // Member
        assertBackupSourceRequest(source);
        // Remove
        boolean removeSucceeded = BackupSourceUtil.remove(CONTEXT, EMAIL_HASH, UUID_HASH);
        assertTrue(removeSucceeded);
        assertNull(BackupSourceUtil.getWithUUIDHash(CONTEXT, EMAIL_HASH, UUID_HASH));
    }


    @Test
    public void putOKTest() {
        // Put
        boolean putSucceeded = BackupSourceUtil.put(CONTEXT, newBackupSourceOK());
        assertTrue(putSucceeded);
        // Get
        BackupSource source = BackupSourceUtil.getWithUUIDHash(CONTEXT, EMAIL_HASH, UUID_HASH);
        assertNotNull(source);
        // Decrypt by BackupSourceUtil
        assertFalse(source.isEncrypted());
        // Member
        assertBackupSourceOK(source);
        // Remove
        boolean removeSucceeded = BackupSourceUtil.remove(CONTEXT, EMAIL_HASH, UUID_HASH);
        assertTrue(removeSucceeded);
        assertNull(BackupSourceUtil.getWithUUIDHash(CONTEXT, EMAIL_HASH, UUID_HASH));
    }

    @Test
    public void putMultiTest() {
        boolean putSucceeded;
        BackupSource source;
        // 1. Request
        putSucceeded = BackupSourceUtil.put(CONTEXT, newBackupSourceRequest());
        assertTrue(putSucceeded);
        source = BackupSourceUtil.getWithUUIDHash(CONTEXT, EMAIL_HASH, UUID_HASH);
        assertNotNull(source);
        assertFalse(source.isEncrypted());
        assertBackupSourceRequest(source);
        // 2. OK
        putSucceeded = BackupSourceUtil.put(CONTEXT, newBackupSourceOK());
        assertTrue(putSucceeded);
        source = BackupSourceUtil.getWithUUIDHash(CONTEXT, EMAIL_HASH, UUID_HASH);
        assertNotNull(source);
        assertFalse(source.isEncrypted());
        assertBackupSourceOK(source);
        // Remove
        boolean removeSucceed = BackupSourceUtil.remove(CONTEXT, EMAIL_HASH, UUID_HASH);
        assertTrue(removeSucceed);
    }

    @Test
    public void putMultiSameEmailHashTest() {
        boolean putSucceeded;
        boolean removeSucceed;
        BackupSource source;
        // 1. Put Request 1
        putSucceeded = BackupSourceUtil.put(CONTEXT, newBackupSourceRequest());
        assertTrue(putSucceeded);
        source = BackupSourceUtil.getWithUUIDHash(CONTEXT, EMAIL_HASH, UUID_HASH);
        assertNotNull(source);
        assertFalse(source.isEncrypted());
        assertBackupSourceRequest(source);
        // 2. Put Request 2
        putSucceeded = BackupSourceUtil.put(CONTEXT, newBackupSourceRequest2());
        assertTrue(putSucceeded);
        source = BackupSourceUtil.getWithUUIDHash(CONTEXT, EMAIL_HASH, UUID_HASH2);
        assertNotNull(source);
        assertFalse(source.isEncrypted());
        assertBackupSourceRequest2(source);
        // 3. Check UUID_HASH
        source = BackupSourceUtil.getWithUUIDHash(CONTEXT, EMAIL_HASH, UUID_HASH);
        assertNotNull(source);
        assertFalse(source.isEncrypted());
        assertBackupSourceRequest(source);
        // 4. Put OK 1
        putSucceeded = BackupSourceUtil.put(CONTEXT, newBackupSourceOK());
        assertTrue(putSucceeded);
        source = BackupSourceUtil.getWithUUIDHash(CONTEXT, EMAIL_HASH, UUID_HASH);
        assertNotNull(source);
        assertFalse(source.isEncrypted());
        assertBackupSourceOK(source);
        // 5. Check UUID_HASH2
        assertNull(BackupSourceUtil.getWithUUIDHash(CONTEXT, EMAIL_HASH, UUID_HASH2));
        // 6. Put OK 2
        putSucceeded = BackupSourceUtil.put(CONTEXT, newBackupSourceOK2());
        assertTrue(putSucceeded);
        source = BackupSourceUtil.getWithUUIDHash(CONTEXT, EMAIL_HASH, UUID_HASH2);
        assertNotNull(source);
        assertFalse(source.isEncrypted());
        assertBackupSourceOK2(source);
        // 7. Remove OK 1
        removeSucceed = BackupSourceUtil.remove(CONTEXT, EMAIL_HASH, UUID_HASH);
        assertTrue(removeSucceed);
        // 8. Remove OK 2
        removeSucceed = BackupSourceUtil.remove(CONTEXT, EMAIL_HASH, UUID_HASH2);
        assertTrue(removeSucceed);
    }
}
