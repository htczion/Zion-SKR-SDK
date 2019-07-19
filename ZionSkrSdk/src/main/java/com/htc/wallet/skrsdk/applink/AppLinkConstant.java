package com.htc.wallet.skrsdk.applink;

import android.text.format.DateUtils;

public class AppLinkConstant {
    public static final String KEY_PUBLIC = "publickey";
    public static final String KEY_TOKEN = "token";
    public static final String KEY_WHISPER_PUB = "whisperPub";
    public static final String KEY_PUSHY_TOKEN = "pushyToken";
    public static final String KEY_ADDRESS_SIGNATURE = "addressSignature";
    public static final String KEY_USER_NAME = "username";
    public static final String KEY_BACKUP_TARGET_NAME = "backupTargetName";
    public static final String KEY_BACKUP_TARGET_UUID_HASH = "backupTargetUUIDHash";
    public static final String KEY_DEVICE_ID = "deviceId";
    public static final String KEY_TZ_ID_HASH = "tzIdHash";
    public static final String KEY_GOOGLE_DRIVE_DEVICE_ID = "googleDriveDeviceId";
    public static final String KEY_RESTORE_TARGET_PHONE_MODEL = "phoneModel";
    public static final String KEY_FLOW_TYPE = "flowType";
    public static final String FLOW_TYPE_RESTORE = "restore";
    public static final String KEY_VERSION = "version";
    public static final String KEY_TIMESTAMP = "ts";

    public static final String KEY_TEST = "test";
    public static final String IS_TESTING = "1";

    // 1 init version, no value means 1 too
    // 2 add timestamp, link expired
    // 3 encrypt link
    // 4 security protection, update Legacy Backup Data V1, force update
    // 5 switch to whisper to do message transition
    public static final int APP_LINK_VERSION = 5;
    // start to use branch link service
    public static final int APP_LINK_BRANCH_START_VERSION = 10;
    public static final String APP_LINK_VERSION_STR = String.valueOf(APP_LINK_VERSION);
    public static final long APP_LINK_VALIDITY_TIME =
            2 * DateUtils.DAY_IN_MILLIS; // Shared link has 2 days validity time

    private AppLinkConstant() {
        throw new AssertionError();
    }
}
