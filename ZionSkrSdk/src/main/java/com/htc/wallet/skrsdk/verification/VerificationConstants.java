package com.htc.wallet.skrsdk.verification;

public class VerificationConstants {
    public static final String EMPTY_STRING = "";

    public static final int MILLIS_IN_MINUTE = 60 * 1000;
    public static final long VERIFY_ACTION_TIMEOUT = 60 * 1000;
    public static final long NO_RESPONSE_HINT_TIMER = 180 * 1000;

    public static final String ACTION_TRIGGER_BROADCAST = "com.htc.wallet.socialkm.backupbroadcast";
    public static final String ACTION_CLOSE_SHARING_ACTIVITY =
            "com.htc.wallet.socialkm.closesharingactivity";
    public static final String ACTION_NOTIFY_UI_UPDATE_AFTER_ERROR_PINCODE =
            "com.htc.wallet.socialkm.notifyuiupdateaftererrorpin";
    public static final String ACTION_OK_SENT = "com.htc.wallet.socialkm.oksent";
    public static final String ACTION_RECEIVE_REPORT_OK = "com.htc.wallet.socialkm.receivereportok";
    public static final String ACTION_RECEIVE_DELETE = "com.htc.wallet.socialkm.receivedelete";
    public static final String ACTION_NOTIFY_PINCODE_UPDATE =
            "com.htc.wallet.socialkm.notifypincodeupdate";
    public static final String ACTION_NOTIFY_BACKUP_FULL =
            "com.htc.wallet.socialkm.notifybackupfull";
    public static final String ACTION_RESTORE_NOTIFY_UI_UPDATE_AFTER_ERROR_PINCODE =
            "com.htc.wallet.socialkm.restorenofifyuiupdateafterrorpin";
    public static final String ACTION_RESTORE_NOTIFY_UI_UPDATE_AFTER_OK_PINCODE =
            "com.htc.wallet.socialkm.restorenofifyuiupdateafterokpin";
    public static final String ACTION_RESTORE_TARGET_VERITY_ACTION =
            "com.htc.wallet.socialkm.restoretargetverifyaction";
    public static final String ACTION_CHECK_DRIVE = "com.htc.wallet.socialkm.check.drive";
    public static final String ACTION_DRIVE_AUTH = "com.htc.wallet.socialkm.drive.auth";

    @Deprecated
    public static final String KEY_CAPTURE_TYPE = "wallet_type_scan";
    @Deprecated
    public static final String CAPTURE_TYPE_VERIFICATION_CODE = "VerificationCode";

    public static final int SOCIAL_KEY_RECOVERY_ACTIVE_THRESHOLD = 3;
}
