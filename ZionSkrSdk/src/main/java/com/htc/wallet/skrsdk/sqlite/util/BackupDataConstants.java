package com.htc.wallet.skrsdk.sqlite.util;

public final class BackupDataConstants {

    // SocialKmDatabase data version (different from db version)
    // 0 first db version
    // 1 encrypt more sensitive data (V1)
    // 2 clear legacy V1, solve the security issue in SSS that may leak the original seed with less
    // than 3 partial seeds
    public static final int BACKUP_DATA_DB_INIT_VERSION = 0;
    public static final int BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION = 1;
    public static final int BACKUP_DATA_CLEAR_LEGACY_V1_VERSION = 2;
    // Current version
    public static final int BACKUP_DATA_CURRENT_VERSION = BACKUP_DATA_CLEAR_LEGACY_V1_VERSION;

    private BackupDataConstants() {
    }
}
