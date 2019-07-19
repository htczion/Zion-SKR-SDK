package com.htc.wallet.skrsdk.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.ArraySet;

import com.htc.wallet.skrsdk.crypto.util.GenericCipherUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class SkrSharedPrefs {
    private static final String TAG = "SkrSharedPrefs";

    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String SKR_PREFS_NAME = "SocialKeyRecoveryEncPrefs";

    private SkrSharedPrefs() {
    }

    private static class SingletonHolder {
        private static final SkrSharedPrefs INSTANCE = new SkrSharedPrefs();
    }

    public static SkrSharedPrefs getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private SharedPreferences getDefaultPrefs(@NonNull Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private SharedPreferences.Editor getDefaultPrefsEditor(@NonNull Context context) {
        return getDefaultPrefs(context).edit();
    }

    private SharedPreferences getSkrEncPrefs(@NonNull Context context) {
        return context.getSharedPreferences(SKR_PREFS_NAME, Context.MODE_PRIVATE);
    }

    private SharedPreferences.Editor getSkrEncPrefsEditor(@NonNull Context context) {
        return getSkrEncPrefs(context).edit();
    }

    public void putString(@NonNull Context context, String key, String value) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        SharedPreferences.Editor editor = getDefaultPrefsEditor(context);
        editor.putString(key, value);
        editor.apply();
    }

    public void putBoolean(@NonNull Context context, String key, boolean value) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        SharedPreferences.Editor editor = getDefaultPrefsEditor(context);
        editor.putBoolean(key, value);
        editor.apply();
    }

    public void putInt(@NonNull Context context, String key, int value) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        SharedPreferences.Editor editor = getDefaultPrefsEditor(context);
        editor.putInt(key, value);
        editor.apply();
    }

    public void putLong(@NonNull Context context, String key, long value) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        SharedPreferences.Editor editor = getDefaultPrefsEditor(context);
        editor.putLong(key, value);
        editor.apply();
    }

    public void putStringSet(@NonNull Context context, String key, Set<String> values) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        SharedPreferences.Editor editor = getDefaultPrefsEditor(context);
        editor.putStringSet(key, values);
        editor.apply();
    }

    public String getString(@NonNull Context context, String key, String defValue) {
        if (TextUtils.isEmpty(key)) {
            return defValue;
        }
        return getDefaultPrefs(context).getString(key, defValue);
    }

    public boolean getBoolean(@NonNull Context context, String key, boolean defValue) {
        if (TextUtils.isEmpty(key)) {
            return defValue;
        }
        return getDefaultPrefs(context).getBoolean(key, defValue);
    }

    public int getInt(@NonNull Context context, String key, int defValue) {
        if (TextUtils.isEmpty(key)) {
            return defValue;
        }
        return getDefaultPrefs(context).getInt(key, defValue);
    }

    public long getLong(@NonNull Context context, String key, long defValue) {
        if (TextUtils.isEmpty(key)) {
            return defValue;
        }
        return getDefaultPrefs(context).getLong(key, defValue);
    }

    public Set<String> getStringSet(@NonNull Context context, String key, Set<String> defValues) {
        if (TextUtils.isEmpty(key)) {
            return defValues;
        }
        return getDefaultPrefs(context).getStringSet(key, defValues);
    }

    public void remove(@NonNull Context context, String key) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        SharedPreferences.Editor editor = getDefaultPrefsEditor(context);
        editor.remove(key);
        editor.apply();
    }

    public void remove(@NonNull Context context, List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        SharedPreferences.Editor editor = getDefaultPrefsEditor(context);
        for (String key : keys) {
            if (TextUtils.isEmpty(key)) {
                continue;
            }
            editor.remove(key);
        }
        editor.apply();
    }

    public void putStringToEncPrefs(@NonNull Context context, String key, String value) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        SharedPreferences.Editor editor = getSkrEncPrefsEditor(context);
        editor.putString(key, value);
        editor.apply();
    }

    public void putBooleanToEncPrefs(@NonNull Context context, String key, boolean value) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        SharedPreferences.Editor editor = getSkrEncPrefsEditor(context);
        editor.putBoolean(key, value);
        editor.apply();
    }

    public void putIntToEncPrefs(@NonNull Context context, String key, int value) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        SharedPreferences.Editor editor = getSkrEncPrefsEditor(context);
        editor.putInt(key, value);
        editor.apply();
    }

    public void putLongToEncPrefs(@NonNull Context context, String key, long value) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        SharedPreferences.Editor editor = getSkrEncPrefsEditor(context);
        editor.putLong(key, value);
        editor.apply();
    }

    public void putStringSetToEncPrefs(@NonNull Context context, String key, Set<String> values) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        SharedPreferences.Editor editor = getSkrEncPrefsEditor(context);
        editor.putStringSet(key, values);
        editor.apply();
    }

    public String getStringFromEncPrefs(@NonNull Context context, String key, String defValue) {
        if (TextUtils.isEmpty(key)) {
            return defValue;
        }
        return getSkrEncPrefs(context).getString(key, defValue);
    }

    public boolean getBooleanFromEncPrefs(@NonNull Context context, String key, boolean defValue) {
        if (TextUtils.isEmpty(key)) {
            return defValue;
        }
        return getSkrEncPrefs(context).getBoolean(key, defValue);
    }

    public int getIntFromEncPrefs(@NonNull Context context, String key, int defValue) {
        if (TextUtils.isEmpty(key)) {
            return defValue;
        }
        return getSkrEncPrefs(context).getInt(key, defValue);
    }

    public long getLongFromEncPrefs(@NonNull Context context, String key, long defValue) {
        if (TextUtils.isEmpty(key)) {
            return defValue;
        }
        return getSkrEncPrefs(context).getLong(key, defValue);
    }

    public Set<String> getStringSetFromEncPrefs(@NonNull Context context, String key,
            Set<String> defValues) {
        if (TextUtils.isEmpty(key)) {
            return defValues;
        }
        return getSkrEncPrefs(context).getStringSet(key, defValues);
    }

    public void removeFromEncPrefs(@NonNull Context context, String key) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        SharedPreferences.Editor editor = getSkrEncPrefsEditor(context);
        editor.remove(key);
        editor.apply();
    }

    public void removeFromEncPrefs(@NonNull Context context, List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        SharedPreferences.Editor editor = getSkrEncPrefsEditor(context);
        for (String key : keys) {
            if (TextUtils.isEmpty(key)) {
                continue;
            }
            editor.remove(key);
        }
        editor.apply();
    }

    public static void clearAllSocialKeyRecoveryData(@NonNull Context context) {
        List<String> keys =
                Arrays.asList(
                        "userToken",
                        "whisper_prv_key",
                        "pushy_token",
                        "GoogleAccountEmail",
                        "GoogleAccountUserName",
                        "BackupTargets",
                        "socialKMId",
                        "should_check_social_key_recovery_active",
                        "check_social_key_recovery_active_count",
                        "check_social_key_recovery_active_time",
                        "should_show_not_active_notification",
                        "RestoreSources",
                        "restoreWaitTime",
                        "restoreRetryTime",
                        "RestoreTrustNames",
                        "CurrentRestoreEmail");
        getInstance().remove(context, keys);

        List<String> keysSkrEnc =
                Arrays.asList(
                        "GoogleAccountEmail",
                        "GoogleAccountUserName",
                        "RestoreTrustNames",
                        "CurrentRestoreEmail",
                        "EmailServiceType",
                        "skr_reconnect_names",
                        "isOneDriveDataHasUpdatedTheFirstTime");
        getInstance().removeFromEncPrefs(context, keysSkrEnc);
    }

    public static void putBackupTargets(@NonNull Context context, String backupTargets) {
        getInstance().putString(context, "BackupTargets", backupTargets);
    }

    public static String getBackupTargets(@NonNull Context context) {
        return getInstance().getString(context, "BackupTargets", "");
    }

    public static void putBackupSources(@NonNull Context context, String backupSources) {
        getInstance().putString(context, "BackupSources", backupSources);
    }

    public static String getBackupSources(@NonNull Context context) {
        return getInstance().getString(context, "BackupSources", "");
    }

    public static void putRestoreTargets(@NonNull Context context, String restoreTargets) {
        getInstance().putString(context, "RestoreTargets", restoreTargets);
    }

    public static String getRestoreSources(@NonNull Context context) {
        return getInstance().getString(context, "RestoreSources", "");
    }

    public static void putRestoreSources(@NonNull Context context, String restoreSources) {
        getInstance().putString(context, "RestoreSources", restoreSources);
    }

    public static String getRestoreTargets(@NonNull Context context) {
        return getInstance().getString(context, "RestoreTargets", "");
    }

    @WorkerThread
    public static void putRestoreTrustNames(@NonNull Context context, String restoreTrustNames) {
        if (TextUtils.isEmpty(restoreTrustNames)) {
            LogUtil.logWarning(TAG, "restoreTrustNames is empty!");
            return;
        }
        GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
        // Encrypt AES restoreTrustNames
        String encRestoreTrustNames = genericCipherUtil.encryptData(restoreTrustNames);
        if (TextUtils.isEmpty(encRestoreTrustNames)) {
            LogUtil.logError(TAG, "Failed to encrypt restoreTrustNames");
            getInstance().putString(context, "RestoreTrustNames", restoreTrustNames);
            return;
        }

        // Remove old non-encrypt data
        getInstance().remove(context, "RestoreTrustNames");

        getInstance().putStringToEncPrefs(context, "RestoreTrustNames", encRestoreTrustNames);
    }

    @Nullable
    @WorkerThread
    public static String getRestoreTrustNames(@NonNull Context context) {
        // Check old non-encrypt data, if exists return
        String oldRestoreTrustNames = getInstance().getString(context, "RestoreTrustNames", "");
        if (!TextUtils.isEmpty(oldRestoreTrustNames)) {
            return oldRestoreTrustNames;
        }

        String restoreTrustNames = getInstance().getStringFromEncPrefs(context, "RestoreTrustNames",
                "");
        String decRestoreTrustNames = null;
        GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
        // Decrypt AES RestoreTrustNames
        if (!TextUtils.isEmpty(restoreTrustNames)) {
            decRestoreTrustNames = genericCipherUtil.decryptData(restoreTrustNames);
            if (TextUtils.isEmpty(decRestoreTrustNames)) {
                LogUtil.logError(TAG, "Failed to decrypt RestoreTrustNames");
                return restoreTrustNames;
            }
        }
        return decRestoreTrustNames;
    }

    public static void putShouldShowTrySocialKeyRecoveryDialog(@NonNull Context context,
            boolean show) {
        getInstance().putBoolean(context, "should_show_try_social_key_recovery_db", show);
    }

    public static boolean getShouldShowTrySocialKeyRecoveryDialog(@NonNull Context context) {
        return getInstance().getBoolean(context, "should_show_try_social_key_recovery_db", false);
    }

    // SocialKM ID
    public static String getSocialKMId(@NonNull Context context) {
        String socialKMId = getInstance().getString(context, "socialKMId", "");
        if (TextUtils.isEmpty(socialKMId)) {
            setSocialKMId(context, UUID.randomUUID().toString());
        }
        return getInstance().getString(context, "socialKMId", "");
    }

    private static void setSocialKMId(@NonNull Context context, String uuid) {
        // Force update SocialKM ID Synchronously.
        getInstance().getDefaultPrefsEditor(context)
                .putString("socialKMId", uuid)
                .commit();
    }

    // SocialKM Email
    @WorkerThread
    public static void putSocialKMBackupEmail(@NonNull Context context, String email) {
        if (TextUtils.isEmpty(email)) {
            LogUtil.logWarning(TAG, "email is empty!");
            getInstance().putString(context, "GoogleAccountEmail", email);
            return;
        }

        GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
        // Encrypt AES GoogleAccountEmail
        String encGoogleAccountEmail = genericCipherUtil.encryptData(email);
        if (TextUtils.isEmpty(encGoogleAccountEmail)) {
            LogUtil.logError(TAG, "Failed to encrypt GoogleAccountEmail");
            getInstance().putString(context, "GoogleAccountEmail", email);
            return;
        }

        // Remove old non-encrypt data
        getInstance().remove(context, "GoogleAccountEmail");

        getInstance().putStringToEncPrefs(context, "GoogleAccountEmail", encGoogleAccountEmail);
    }

    @Nullable
    @WorkerThread
    public static String getSocialKMBackupEmail(@NonNull Context context) {
        // Check old non-encrypt data, if exists return
        String oldEmail = getInstance().getString(context, "GoogleAccountEmail", "");
        if (!TextUtils.isEmpty(oldEmail)) {
            return oldEmail;
        }

        String googleAccountEmail = getInstance().getStringFromEncPrefs(context,
                "GoogleAccountEmail", "");
        String decGoogleAccountEmail = null;
        GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
        // Decrypt AES GoogleAccountEmail
        if (!TextUtils.isEmpty(googleAccountEmail)) {
            decGoogleAccountEmail = genericCipherUtil.decryptData(googleAccountEmail);
            if (TextUtils.isEmpty(decGoogleAccountEmail)) {
                LogUtil.logError(TAG, "Failed to decrypt GoogleAccountEmail");
                return googleAccountEmail;
            }
        }
        return decGoogleAccountEmail;
    }

    public static void putUserToken(@NonNull Context context, String userToken) {
        getInstance().putString(context, "userToken", userToken);
    }

    public static String getUserToken(@NonNull Context context) {
        return getInstance().getString(context, "userToken", "");
    }

    // SocialKM Email
    @WorkerThread
    public static void putSocialKMUserName(@NonNull Context context, String googleAccountUserName) {
        if (TextUtils.isEmpty(googleAccountUserName)) {
            getInstance().putString(context, "GoogleAccountUserName", googleAccountUserName);
            return;
        }

        GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
        // Encrypt AES GoogleAccountUserName
        String encGoogleAccountUserName = genericCipherUtil.encryptData(googleAccountUserName);
        if (TextUtils.isEmpty(encGoogleAccountUserName)) {
            LogUtil.logError(TAG, "Failed to encrypt GoogleAccountUserName");
            getInstance().putString(context, "GoogleAccountUserName", googleAccountUserName);
            return;
        }

        // Remove old non-encrypt data
        getInstance().remove(context, "GoogleAccountUserName");

        getInstance().putStringToEncPrefs(context, "GoogleAccountUserName",
                encGoogleAccountUserName);
    }

    @Nullable
    @WorkerThread
    public static String getSocialKMUserName(@NonNull Context context) {
        // Check old non-encrypt data, if exists return
        String oldAccountUserName = getInstance().getString(context, "GoogleAccountUserName", "");
        if (!TextUtils.isEmpty(oldAccountUserName)) {
            return oldAccountUserName;
        }

        String googleAccountUserName = getInstance().getStringFromEncPrefs(context,
                "GoogleAccountUserName", "");
        String decGoogleAccountUserName = null;
        GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
        // Decrypt AES GoogleAccountEmail
        if (!TextUtils.isEmpty(googleAccountUserName)) {
            decGoogleAccountUserName = genericCipherUtil.decryptData(googleAccountUserName);
            if (TextUtils.isEmpty(decGoogleAccountUserName)) {
                LogUtil.logError(TAG, "Failed to decrypt GoogleAccountUserName");
                return googleAccountUserName;
            }
        }
        return decGoogleAccountUserName;
    }

    // Device Attest Begin
    @Nullable
    @WorkerThread
    public static String getDeviceAttestToken(@NonNull Context context, boolean isTest) {
        String keyToken;
        if (isTest) {
            keyToken = "device_attest_token_stage";
        } else {
            keyToken = "device_attest_token_production";
        }

        String encToken = getInstance().getStringFromEncPrefs(context, keyToken, "");
        if (TextUtils.isEmpty(encToken)) {
            LogUtil.logWarning(TAG, "encrypted token is empty");
            return null;
        }
        GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
        String decToken = genericCipherUtil.decryptData(encToken);
        if (TextUtils.isEmpty(decToken)) {
            LogUtil.logError(TAG, "decrypt token failed");
        }
        return decToken;
    }

    @WorkerThread
    public static void putDeviceAttestToken(@NonNull Context context, String token,
            boolean isTest) {
        if (TextUtils.isEmpty(token)) {
            LogUtil.logError(TAG, "device check token is empty");
            return;
        }

        String keyToken;
        String keyTimestamp;
        if (isTest) {
            keyToken = "device_attest_token_stage";
            keyTimestamp = "device_attest_token_timestamp_stage";
        } else {
            keyToken = "device_attest_token_production";
            keyTimestamp = "device_attest_token_timestamp_production";
        }

        GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
        String encToken = genericCipherUtil.encryptData(token);
        if (TextUtils.isEmpty(encToken)) {
            LogUtil.logError(TAG, "encrypt token failed");
            getInstance().putStringToEncPrefs(context, keyToken, token);
            getInstance().putLongToEncPrefs(context, keyTimestamp, System.currentTimeMillis());
            return;
        }
        getInstance().putStringToEncPrefs(context, keyToken, encToken);
        getInstance().putLongToEncPrefs(context, keyTimestamp, System.currentTimeMillis());
    }

    public static long getDeviceAttestTokenTimestamp(@NonNull Context context, boolean isTest) {
        String keyTimestamp;
        if (isTest) {
            keyTimestamp = "device_attest_token_timestamp_stage";
        } else {
            keyTimestamp = "device_attest_token_timestamp_production";
        }
        return getInstance().getLongFromEncPrefs(context, keyTimestamp, 0);
    }

    public static void clearDeviceAttestToken(@NonNull Context context) {
        List<String> keys =
                Arrays.asList(
                        "device_attest_token_stage",
                        "device_attest_token_production",
                        "device_attest_token_timestamp_stage",
                        "device_attest_token_timestamp_production");
        getInstance().removeFromEncPrefs(context, keys);
    }
    // Device Attest End

    // LegacySkrV1 Begin
    public static String getLegacySkrV1(@NonNull Context context) {
        return getInstance().getStringFromEncPrefs(context, "LegacySkrV1", "");
    }

    public static void putLegacySkrV1(@NonNull Context context, String jsonStr) {
        getInstance().putStringToEncPrefs(context, "LegacySkrV1", jsonStr);
    }

    @WorkerThread
    public static List<String> getLegacySKRListV1(@NonNull Context context) {
        Set<String> encUUIDHashSet = getInstance().getStringSetFromEncPrefs(context,
                "LegacyUUIDHashV1", new ArraySet<String>());
        GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
        List<String> uuidHashList = new ArrayList<>();
        for (String encUUIDHash : encUUIDHashSet) {
            if (!TextUtils.isEmpty(encUUIDHash)) {
                // decrypt
                String uuidHash = genericCipherUtil.decryptData(encUUIDHash);
                uuidHashList.add(uuidHash);
            }
        }
        return uuidHashList;
    }

    // putLegacySKRListV1 is deprecated
    public static void clearLegacySKRListV1(@NonNull Context context) {
        getInstance().removeFromEncPrefs(context, "LegacyUUIDHashV1");
    }
    // LegacySkrV1 End

    // Reconnect Name Begin
    public static String getSkrReconnectNamesToJson(@NonNull Context context) {
        return getInstance().getStringFromEncPrefs(context, "skr_reconnect_names", null);
    }

    public static void putSkrReconnectNamesFromJson(@NonNull Context context, String names) {
        getInstance().putStringToEncPrefs(context, "skr_reconnect_names", names);
    }

    public static void clearSkrReconnectNames(@NonNull Context context) {
        getInstance().removeFromEncPrefs(context, "skr_reconnect_names");
    }
    // Reconnect Name End

    // Is oneDrive data update first time Begin
    public static void putIsOneDriveDataHasUpdatedTheFirstTime(@NonNull Context context,
            boolean isFirstTime) {
        getInstance().putBooleanToEncPrefs(context, "isOneDriveDataHasUpdatedTheFirstTime",
                isFirstTime);
    }

    public static boolean getIsOneDriveDataHasUpdatedTheFirstTime(@NonNull Context context) {
        return getInstance().getBooleanFromEncPrefs(context, "isOneDriveDataHasUpdatedTheFirstTime",
                true);
    }
    // Is oneDrive data update first time End

    // Email service type Begin
    public static void putSocialKMEmailServiceType(@NonNull Context context, String type) {
        getInstance().putStringToEncPrefs(context, "EmailServiceType", type);
    }

    public static String getSocialKMEmailServiceType(@NonNull Context context) {
        return getInstance().getStringFromEncPrefs(context, "EmailServiceType",
                Integer.toString(-1));
    }
    // Email service type End

    // SocialKmDatabaseUpdateReceiver Begin
    public static int getCurrentSkrBackupVersion(@NonNull Context context) {
        return getInstance().getInt(context, "CurrentSkrBackupVersion", -1);
    }

    public static void putCurrentSkrBackupVersion(@NonNull Context context, int version) {
        getInstance().putInt(context, "CurrentSkrBackupVersion", version);
    }

    public static boolean getCheckSocialKeyRecoverySaveToDb(@NonNull Context context) {
        return getInstance().getBoolean(context, "check_social_key_recovery_save_to_db", false);
    }

    public static void putCheckSocialKeyRecoverySaveToDb(@NonNull Context context,
            boolean isComplete) {
        getInstance().putBoolean(context, "check_social_key_recovery_save_to_db", isComplete);
    }

    public static void clearDeprecatedSkrBackupData(@NonNull Context context) {
        List<String> keys =
                Arrays.asList("BackupTargets", "BackupSources", "RestoreSources", "RestoreTargets");
        getInstance().remove(context, keys);
    }
    // SocialKmDatabaseUpdateReceiver End

    // Legacy V1 Begin
    public static boolean getShouldShowSkrSecurityUpdateDialogV1(@NonNull Context context) {
        return getInstance().getBoolean(context, "ShouldShowSkrSecurityUpdateDialogV1", false);
    }

    public static void putShouldShowSkrSecurityUpdateDialogV1(@NonNull Context context,
            boolean shouldShow) {
        getInstance().putBoolean(context, "ShouldShowSkrSecurityUpdateDialogV1", shouldShow);
    }

    public static boolean getUserAgreeDeleteLegacySkrBackupDataV1(@NonNull Context context) {
        return getInstance().getBoolean(context, "UserAgreeDeleteLegacySkrBackupDataV1", false);
    }

    public static void putUserAgreeDeleteLegacySkrBackupDataV1(@NonNull Context context,
            boolean userAgree) {
        getInstance().putBoolean(context, "UserAgreeDeleteLegacySkrBackupDataV1", userAgree);
    }

    public static boolean getShouldShowSkrSecurityUpdateHeaderV1(@NonNull Context context) {
        return getInstance().getBoolean(context, "ShouldShowSkrSecurityUpdateHeaderV1", false);
    }

    public static void putShouldShowSkrSecurityUpdateHeaderV1(@NonNull Context context,
            boolean shouldShow) {
        getInstance().putBoolean(context, "ShouldShowSkrSecurityUpdateHeaderV1", shouldShow);
    }
    // Legacy V1 End

    // Restore Begin
    public static void putRestoreRetryTime(@NonNull Context context, String restoreRetryTime) {
        getInstance().putString(context, "restoreRetryTime", restoreRetryTime);
    }

    public static String getRestoreRetryTime(@NonNull Context context) {
        return getInstance().getString(context, "restoreRetryTime", "");
    }

    public static void putRestoreWaitTime(@NonNull Context context, String restoreWaitTime) {
        getInstance().putString(context, "restoreWaitTime", restoreWaitTime);
    }

    public static String getRestoreWaitTime(@NonNull Context context) {
        return getInstance().getString(context, "restoreWaitTime", "");
    }

    public static void putRestoreRemainTime(@NonNull Context context, String restoreRemainTime) {
        getInstance().putString(context, "restoreRemainTime", restoreRemainTime);
    }

    public static String getRestoreRemainTime(@NonNull Context context) {
        return getInstance().getString(context, "restoreRemainTime", "");
    }
    // Restore End

    // TODO: Same as Zion SkrSharedPrefs, find another method to access them
    // Same as Zion SkrSharedPrefs Begin
    public static void putLastPasscodeTime(@NonNull Context context, long time) {
        getInstance().putLong(context, "lastPCTime", time);
    }

    public static String getDeviceId(@NonNull Context context) {
        String deviceId = getInstance().getString(context, "userId", "");
        if (deviceId.isEmpty()) {
            setDeviceId(context, UUID.randomUUID().toString());
        }
        return getInstance().getString(context, "userId", "");
    }

    private static void setDeviceId(@NonNull Context context, String uuid) {
        // Force update uuid synchronously.
        getInstance().getDefaultPrefsEditor(context)
                .putString("userId", uuid)
                .commit();
    }

    public static boolean getIsProductionServer(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("isProductionServer", false);
    }
    // Same as Zion SkrSharedPrefs End

    public static void putPushyToken(@NonNull Context context, String token) {
        getInstance().putString(context, "pushy_token", token);
    }

    public static String getPushyToken(@NonNull Context context) {
        return getInstance().getString(context, "pushy_token", "");
    }

    public static void putWhisperPrvKey(@NonNull Context context, String prvKey) {
        getInstance().putString(context, "whisper_prv_key", prvKey);
    }

    public static String getWhisperPrvKey(@NonNull Context context) {
        return getInstance().getString(context, "whisper_prv_key", "");
    }

    public static String getSkrRestoreEditTextStatusToJson(Context context, int position) {
        if (position < 0 || position > 2) {
            LogUtil.logError(TAG, "getSkrRestoreEditTextStatusToJson() failed, "
                    + "incorrect position=" + position);
            return "";
        }
        String key = "skr_restore_editText_status_" + position;
        return getInstance().getString(context, key, "");
    }

    public static void putSkrRestoreEditTextStatusFromJson(Context context, int position,
            String jsonStr) {
        if (position < 0 || position > 2) {
            LogUtil.logError(TAG, "putSkrRestoreEditTextStatusFromJson() failed, "
                    + "incorrect position=" + position);
            return;
        }

        String key = "skr_restore_editText_status_" + position;
        getInstance().putString(context, key, jsonStr);
    }

    public static void resetSkrRestoreEditTextStatus(Context context, int position) {
        if (position < 0 || position > 2) {
            LogUtil.logError(TAG, "resetSkrRestoreEditTextStatus() failed, "
                    + "incorrect position=" + position);
            return;
        }

        String key = "skr_restore_editText_status_" + position;
        getInstance().remove(context, key);
    }

    public static void resetSkrRestoreStatus(Context context) {

        // "socialKMId"
        // In backup flow, remove this key (SKR logout) via clearAllSocialKeyRecoveryData()
        // In restore flow, remove this key (restore target change) by this function

        // Clear SocialKMId
        getInstance().remove(context, "socialKMId");

        // Reset restore editText status
        for (int i = 0; i <= 2; i++) {
            resetSkrRestoreEditTextStatus(context, i);
        }
    }

    public static void putUserAgreeDeleteLegacySkrBackupDataV1(@NonNull Context context,
            Boolean userAgree) {
        getInstance().putBoolean(context, "UserAgreeDeleteLegacySkrBackupDataV1", userAgree);
    }

    public static void putRestoreUUIDHash(@NonNull Context context, String restoreUUIDHash) {
        getInstance().putString(context, "RestoreUUIDHash", restoreUUIDHash);
    }

    public static String getRestoreUUIDHash(@NonNull Context context) {
        return getInstance().getString(context, "RestoreUUIDHash", "");
    }

    public static void putShouldShowNotActiveNotification(@NonNull Context context, Boolean show) {
        getInstance().putBoolean(context, "should_show_not_active_notification", show);
    }

    public static Boolean getShouldShowNotActiveNotification(@NonNull Context context) {
        return getInstance().getBoolean(context, "should_show_not_active_notification", false);
    }

    public static void putShouldCheckSocialKeyRecoveryActive(@NonNull Context context,
            Boolean show) {
        getInstance().putBoolean(context, "should_check_social_key_recovery_active", show);
    }

    public static Boolean getShouldCheckSocialKeyRecoveryActive(@NonNull Context context) {
        return getInstance().getBoolean(context, "should_check_social_key_recovery_active", false);
    }

    public static void putCheckSocialKeyRecoveryActiveCount(@NonNull Context context, int count) {
        getInstance().putInt(context, "check_social_key_recovery_active_count", count);
    }

    public static int getCheckSocialKeyRecoveryActiveCount(@NonNull Context context) {
        return getInstance().getInt(context, "check_social_key_recovery_active_count", 0);
    }

    public static void putCheckSocialKeyRecoveryActiveTime(@NonNull Context context,
            Long showTime) {
        getInstance().putLong(context, "check_social_key_recovery_active_time", showTime);
    }

    public static Long getCheckSocialKeyRecoveryActiveTime(@NonNull Context context) {
        return getInstance().getLong(context, "check_social_key_recovery_active_time", 0);
    }

    @Nullable
    @WorkerThread
    public static String getCurrentRestoreEmail(@NonNull Context context) {
        // Check old non-encrypt data, if exists return
        String oldCurrentRestoreEmail = getInstance().getString(context, "CurrentRestoreEmail", "");
        if (!TextUtils.isEmpty(oldCurrentRestoreEmail)) {
            return oldCurrentRestoreEmail;
        }

        String currentRestoreEmail = getInstance().getStringFromEncPrefs(context,
                "CurrentRestoreEmail", "");
        String decCurrentRestoreEmail = null;
        GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
        // Decrypt AES GoogleAccountEmail
        if (!TextUtils.isEmpty(currentRestoreEmail)) {
            decCurrentRestoreEmail = genericCipherUtil.decryptData(currentRestoreEmail);
            if (TextUtils.isEmpty(decCurrentRestoreEmail)) {
                LogUtil.logError(TAG, "Failed to decrypt CurrentRestoreEmail");
                return currentRestoreEmail;
            }
        }
        return decCurrentRestoreEmail;
    }

    @WorkerThread
    public static void putCurrentRestoreEmail(@NonNull Context context,
            String currentRestoreEmail) {
        if (TextUtils.isEmpty(currentRestoreEmail)) {
            LogUtil.logWarning(TAG, "currentRestoreEmail is empty!");
            getInstance().putString(context, "CurrentRestoreEmail", currentRestoreEmail);
            return;
        }

        GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
        // Encrypt AES CurrentRestoreEmail
        String encCurrentRestoreEmail = genericCipherUtil.encryptData(currentRestoreEmail);
        if (TextUtils.isEmpty(encCurrentRestoreEmail)) {
            LogUtil.logError(TAG, "Failed to encrypt currentRestoreEmail");
            getInstance().putString(context, "CurrentRestoreEmail", currentRestoreEmail);
            return;
        }

        // Remove old non-encrypt data
        getInstance().remove(context, "CurrentRestoreEmail");

        getInstance().putStringToEncPrefs(context, "CurrentRestoreEmail", encCurrentRestoreEmail);
    }

    public static void putSocialKMFlowType(@NonNull Context context, String type) {
        getInstance().putStringToEncPrefs(context, "FlowType", type);
    }

    public static String getSocialKMFlowType(@NonNull Context context) {
        return getInstance().getStringFromEncPrefs(context, "FlowType", Integer.toString(-1));
    }
}
