package com.htc.wallet.skrsdk.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.text.TextUtils;
import android.util.ArraySet;

import com.htc.wallet.skrsdk.crypto.util.GenericCipherUtil;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class SkrSharedPrefsTest {
    private final Context mContext = InstrumentationRegistry.getTargetContext();

    @Test
    public void putString() {
        SkrSharedPrefs.getInstance().putString(mContext, "test_string", "testString");
        String result = SkrSharedPrefs.getInstance().getString(mContext, "test_string", null);
        assertEquals("testString", result);
    }

    @Test
    public void putBoolean() {
        SkrSharedPrefs.getInstance().putBoolean(mContext, "test_boolean", true);
        boolean result = SkrSharedPrefs.getInstance().getBoolean(mContext, "test_boolean", false);
        assertTrue(result);
    }

    @Test
    public void putInt() {
        SkrSharedPrefs.getInstance().putInt(mContext, "test_int", 87);
        int result = SkrSharedPrefs.getInstance().getInt(mContext, "test_int", 0);
        assertNotEquals(88, result);
    }

    @Test
    public void putLong() {
        SkrSharedPrefs.getInstance().putLong(mContext, "test_long", 878787878787L);
        long result = SkrSharedPrefs.getInstance().getLong(mContext, "test_long", 0L);
        assertEquals(878787878787L, result);
    }

    @Test
    public void putStringSet() {
        Set<String> testSets = new HashSet<>(Arrays.asList("A", "B", "C"));
        SkrSharedPrefs.getInstance().putStringSet(mContext, "test_string_set", testSets);
        Set<String> result = SkrSharedPrefs.getInstance().getStringSet(mContext, "test_string_set",
                new ArraySet<String>());
        assertEquals(3, result.size());
        assertTrue(result.contains("B"));
        assertFalse(result.contains("D"));
    }

    @Test
    public void remove() {
        putString();
        assertNotNull(SkrSharedPrefs.getInstance().getString(mContext, "test_string", null));

        SkrSharedPrefs.getInstance().remove(mContext, "test_string");
        String result = SkrSharedPrefs.getInstance().getString(mContext, "test_string", null);
        assertNull(result);
    }

    @Test
    public void remove1() {
        putString();
        putBoolean();
        putInt();
        assertNotNull(SkrSharedPrefs.getInstance().getString(mContext, "test_string", null));
        assertTrue(SkrSharedPrefs.getInstance().getBoolean(mContext, "test_boolean", false));
        assertNotEquals(0, SkrSharedPrefs.getInstance().getInt(mContext, "test_int", 0));

        List<String> listToRemove = new ArrayList<>(
                Arrays.asList("test_string", "test_boolean", "test_int"));
        SkrSharedPrefs.getInstance().remove(mContext, listToRemove);
        assertNull(SkrSharedPrefs.getInstance().getString(mContext, "test_string", null));
        assertFalse(SkrSharedPrefs.getInstance().getBoolean(mContext, "test_boolean", false));
        assertEquals(0, SkrSharedPrefs.getInstance().getInt(mContext, "test_int", 0));
    }

    @Test
    public void putStringToEncPrefs() {
        GenericCipherUtil cipherUtil = new GenericCipherUtil();
        String encString = cipherUtil.encryptData("testEncString");
        assertFalse(TextUtils.isEmpty(encString));

        SkrSharedPrefs.getInstance().putStringToEncPrefs(mContext, "test_enc_string", encString);
        String result = SkrSharedPrefs.getInstance().getStringFromEncPrefs(mContext,
                "test_enc_string", null);
        assertNotNull(result);
        assertNotEquals("testEncString", result);

        String decString = cipherUtil.decryptData(result);
        assertEquals("testEncString", decString);
    }

    @Test
    public void putBooleanToEncPrefs() {
        SkrSharedPrefs.getInstance().putBooleanToEncPrefs(mContext, "test_enc_boolean", true);
        boolean result = SkrSharedPrefs.getInstance().getBooleanFromEncPrefs(mContext,
                "test_enc_boolean", false);
        assertTrue(result);
    }

    @Test
    public void putIntToEncPrefs() {
        SkrSharedPrefs.getInstance().putIntToEncPrefs(mContext, "test_enc_int", 87);
        int result = SkrSharedPrefs.getInstance().getIntFromEncPrefs(mContext, "test_enc_int", 0);
        assertNotEquals(88, result);
    }

    @Test
    public void putLongToEncPrefs() {
        SkrSharedPrefs.getInstance().putLongToEncPrefs(mContext, "test_enc_long", 878787878787L);
        long result = SkrSharedPrefs.getInstance().getLongFromEncPrefs(mContext, "test_enc_long",
                0L);
        assertEquals(878787878787L, result);
    }

    @Test
    public void putStringSetToEncPrefs() {
        Set<String> testSets = new HashSet<>(Arrays.asList("A", "B", "C"));
        SkrSharedPrefs.getInstance().putStringSetToEncPrefs(mContext, "test_enc_string_set",
                testSets);
        Set<String> result = SkrSharedPrefs.getInstance().getStringSetFromEncPrefs(mContext,
                "test_enc_string_set",
                new ArraySet<String>());
        assertEquals(3, result.size());
        assertTrue(result.contains("B"));
        assertFalse(result.contains("D"));
    }

    @Test
    public void removeFromEncPrefs() {
        putStringToEncPrefs();
        assertNotNull(
                SkrSharedPrefs.getInstance().getStringFromEncPrefs(mContext, "test_enc_string",
                        null));

        SkrSharedPrefs.getInstance().removeFromEncPrefs(mContext, "test_enc_string");
        String result = SkrSharedPrefs.getInstance().getStringFromEncPrefs(mContext,
                "test_enc_string", null);
        assertNull(result);
    }

    @Test
    public void removeFromEncPrefs1() {
        putStringToEncPrefs();
        putBooleanToEncPrefs();
        putIntToEncPrefs();
        assertNotNull(
                SkrSharedPrefs.getInstance().getStringFromEncPrefs(mContext, "test_enc_string",
                        null));
        assertTrue(SkrSharedPrefs.getInstance().getBooleanFromEncPrefs(mContext, "test_enc_boolean",
                false));
        assertNotEquals(0,
                SkrSharedPrefs.getInstance().getIntFromEncPrefs(mContext, "test_enc_int", 0));

        List<String> listToRemove = new ArrayList<>(
                Arrays.asList("test_enc_string", "test_enc_boolean", "test_enc_int"));
        SkrSharedPrefs.getInstance().removeFromEncPrefs(mContext, listToRemove);
        assertNull(SkrSharedPrefs.getInstance().getStringFromEncPrefs(mContext, "test_enc_string",
                null));
        assertFalse(
                SkrSharedPrefs.getInstance().getBooleanFromEncPrefs(mContext, "test_enc_boolean",
                        false));
        assertEquals(0,
                SkrSharedPrefs.getInstance().getIntFromEncPrefs(mContext, "test_enc_int", 0));
    }
}
