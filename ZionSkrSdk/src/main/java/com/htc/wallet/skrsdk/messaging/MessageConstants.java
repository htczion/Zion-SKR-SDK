package com.htc.wallet.skrsdk.messaging;

public final class MessageConstants {
    public static final String RECEIVER = "receiver";
    public static final String SENDER = "sender";
    public static final String MESSAGE = "message";
    public static final String TIMESTAMP = "timestamp";
    public static final String TOPIC = "topic";
    public static final String MESSAGE_TYPE = "messageType";
    public static final int TYPE_UNDEFINED = 0;
    public static final int TYPE_BACKUP_REQUEST = 1;
    public static final int TYPE_BACKUP_VERIFY = 2;
    public static final int TYPE_BACKUP_SEED = 3;
    public static final int TYPE_BACKUP_OK = 4;
    public static final int TYPE_BACKUP_ERROR = 5;
    public static final int TYPE_BACKUP_DELETE = 6;
    public static final int TYPE_BACKUP_FULL = 7;
    public static final int TYPE_RESTORE_TARGET_VERIFY = 8;
    public static final int TYPE_RESTORE_VERIFY = 9;
    public static final int TYPE_RESTORE_SEED = 10;
    public static final int TYPE_RESTORE_OK = 11;
    public static final int TYPE_RESTORE_ERROR = 12;
    public static final int TYPE_CHECK_BACKUP_STATUS = 13;
    public static final int TYPE_REPORT_BACKUP_STATUS = 14;
    public static final int TYPE_RESTORE_UUID_CHECK = 15;
    public static final int TYPE_RESTORE_DELETE = 16;
    public static final int TYPE_RESEND_NAME = 17;
    public static final int TYPE_CHECK_BACKUP_VERSION = 18;
    // Amy to report backup is existed and Bob should keep it.
    public static final int TYPE_REPORT_BACKUP_EXISTED = 19;
}
