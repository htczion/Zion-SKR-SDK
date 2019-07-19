package com.htc.wallet.skrsdk.messaging;

import static com.htc.wallet.skrsdk.messaging.MessageConstants.TYPE_UNDEFINED;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.action.BackupDeleteAction;
import com.htc.wallet.skrsdk.action.BackupErrorAction;
import com.htc.wallet.skrsdk.action.BackupFullAction;
import com.htc.wallet.skrsdk.action.BackupHealthCheckAction;
import com.htc.wallet.skrsdk.action.BackupHealthReportAction;
import com.htc.wallet.skrsdk.action.BackupOkAction;
import com.htc.wallet.skrsdk.action.BackupReportExistedAction;
import com.htc.wallet.skrsdk.action.BackupRequestAction;
import com.htc.wallet.skrsdk.action.BackupSeedAction;
import com.htc.wallet.skrsdk.action.BackupVerifyAction;
import com.htc.wallet.skrsdk.action.ResendNameAction;
import com.htc.wallet.skrsdk.action.RestoreDeleteAction;
import com.htc.wallet.skrsdk.action.RestoreErrorAction;
import com.htc.wallet.skrsdk.action.RestoreOKAction;
import com.htc.wallet.skrsdk.action.RestoreSeedAction;
import com.htc.wallet.skrsdk.action.RestoreTargetVerifyAction;
import com.htc.wallet.skrsdk.action.RestoreVerifyAction;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.Objects;

public class MessageTypeWrapper {
    private static final String TAG = "MessageTypeWrapper";

    static void toAction(
            @NonNull Context context,
            @Nullable String senderFcm,
            @Nullable String receiverFcm,
            @Nullable String senderWhisperPub,
            @Nullable String receiverWhisperPub,
            @Nullable String senderPushyToken,
            @Nullable String receiverPushyToken,
            @NonNull String message,
            int messageType) {
        Objects.requireNonNull(context);
        if (TextUtils.isEmpty(message) || messageType == TYPE_UNDEFINED) {
            LogUtil.logError(TAG, "message or messageType is null");
            return;
        }

        switch (messageType) {
            case MessageConstants.TYPE_BACKUP_REQUEST:
                new BackupRequestAction()
                        .onReceive(
                                context,
                                senderFcm,
                                receiverFcm,
                                senderWhisperPub,
                                receiverWhisperPub,
                                senderPushyToken,
                                receiverPushyToken,
                                message);
                break;
            case MessageConstants.TYPE_BACKUP_VERIFY:
                new BackupVerifyAction()
                        .onReceive(
                                context,
                                senderFcm,
                                receiverFcm,
                                senderWhisperPub,
                                receiverWhisperPub,
                                senderPushyToken,
                                receiverPushyToken,
                                message);
                break;
            case MessageConstants.TYPE_BACKUP_SEED:
                new BackupSeedAction()
                        .onReceive(
                                context,
                                senderFcm,
                                receiverFcm,
                                senderWhisperPub,
                                receiverWhisperPub,
                                senderPushyToken,
                                receiverPushyToken,
                                message);
                break;
            case MessageConstants.TYPE_BACKUP_OK:
                new BackupOkAction()
                        .onReceive(
                                context,
                                senderFcm,
                                receiverFcm,
                                senderWhisperPub,
                                receiverWhisperPub,
                                senderPushyToken,
                                receiverPushyToken,
                                message);
                break;
            case MessageConstants.TYPE_BACKUP_ERROR:
                new BackupErrorAction()
                        .onReceive(
                                context,
                                senderFcm,
                                receiverFcm,
                                senderWhisperPub,
                                receiverWhisperPub,
                                senderPushyToken,
                                receiverPushyToken,
                                message);
                break;
            case MessageConstants.TYPE_BACKUP_DELETE:
                new BackupDeleteAction()
                        .onReceive(
                                context,
                                senderFcm,
                                receiverFcm,
                                senderWhisperPub,
                                receiverWhisperPub,
                                senderPushyToken,
                                receiverPushyToken,
                                message);
                break;
            case MessageConstants.TYPE_BACKUP_FULL:
                new BackupFullAction()
                        .onReceive(
                                context,
                                senderFcm,
                                receiverFcm,
                                senderWhisperPub,
                                receiverWhisperPub,
                                senderPushyToken,
                                receiverPushyToken,
                                message);
                break;
            case MessageConstants.TYPE_RESTORE_TARGET_VERIFY:
                new RestoreTargetVerifyAction()
                        .onReceive(
                                context,
                                senderFcm,
                                receiverFcm,
                                senderWhisperPub,
                                receiverWhisperPub,
                                senderPushyToken,
                                receiverPushyToken,
                                message);
                break;
            case MessageConstants.TYPE_RESTORE_VERIFY:
                new RestoreVerifyAction()
                        .onReceive(
                                context,
                                senderFcm,
                                receiverFcm,
                                senderWhisperPub,
                                receiverWhisperPub,
                                senderPushyToken,
                                receiverPushyToken,
                                message);
                break;
            case MessageConstants.TYPE_RESTORE_SEED:
                new RestoreSeedAction()
                        .onReceive(
                                context,
                                senderFcm,
                                receiverFcm,
                                senderWhisperPub,
                                receiverWhisperPub,
                                senderPushyToken,
                                receiverPushyToken,
                                message);
                break;
            case MessageConstants.TYPE_RESTORE_OK:
                new RestoreOKAction()
                        .onReceive(
                                context,
                                senderFcm,
                                receiverFcm,
                                senderWhisperPub,
                                receiverWhisperPub,
                                senderPushyToken,
                                receiverPushyToken,
                                message);
                break;
            case MessageConstants.TYPE_RESTORE_ERROR:
                new RestoreErrorAction()
                        .onReceive(
                                context,
                                senderFcm,
                                receiverFcm,
                                senderWhisperPub,
                                receiverWhisperPub,
                                senderPushyToken,
                                receiverPushyToken,
                                message);
                break;
            case MessageConstants.TYPE_CHECK_BACKUP_STATUS:
                new BackupHealthCheckAction()
                        .onReceive(
                                context,
                                senderFcm,
                                receiverFcm,
                                senderWhisperPub,
                                receiverWhisperPub,
                                senderPushyToken,
                                receiverPushyToken,
                                message);
                break;
            case MessageConstants.TYPE_REPORT_BACKUP_STATUS:
                new BackupHealthReportAction()
                        .onReceive(
                                context,
                                senderFcm,
                                receiverFcm,
                                senderWhisperPub,
                                receiverWhisperPub,
                                senderPushyToken,
                                receiverPushyToken,
                                message);
                break;
            case MessageConstants.TYPE_RESTORE_UUID_CHECK:
                LogUtil.logWarning(TAG, "Receive deprecated action, ignore it");
                break;
            case MessageConstants.TYPE_RESTORE_DELETE:
                new RestoreDeleteAction()
                        .onReceive(
                                context,
                                senderFcm,
                                receiverFcm,
                                senderWhisperPub,
                                receiverWhisperPub,
                                senderPushyToken,
                                receiverPushyToken,
                                message);
                break;
            case MessageConstants.TYPE_RESEND_NAME:
                new ResendNameAction()
                        .onReceive(
                                context,
                                senderFcm,
                                receiverFcm,
                                senderWhisperPub,
                                receiverWhisperPub,
                                senderPushyToken,
                                receiverPushyToken,
                                message);
                break;
            case MessageConstants.TYPE_REPORT_BACKUP_EXISTED:
                new BackupReportExistedAction()
                        .onReceive(
                                context,
                                senderFcm,
                                receiverFcm,
                                senderWhisperPub,
                                receiverWhisperPub,
                                senderPushyToken,
                                receiverPushyToken,
                                message);
                break;
            default:
                LogUtil.logError(TAG, "Unknown message, type=" + messageType);
        }
    }
}
