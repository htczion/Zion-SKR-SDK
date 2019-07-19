package com.htc.wallet.skrsdk.drives;

import static com.htc.wallet.skrsdk.jobs.JobIdManager.JOB_ID_SKR_UPLOAD_TRUST_CONTACTS;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_CHECK_DRIVE;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_DRIVE_AUTH;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.Objects;

public class DriveJobIntentService extends JobIntentService {
    private static final String TAG = "DriveJobIntentService";

    private static final String KEY_JOB_ID = "key_job_id";
    private static final int DEFAULT_VALUE = -1;

    public static void enqueueUpload(@NonNull Context context, int jobId) {
        Objects.requireNonNull(context, "context is null");

        if (jobId == JOB_ID_SKR_UPLOAD_TRUST_CONTACTS) {
            Intent intent = new Intent();
            intent.putExtra(KEY_JOB_ID, jobId);
            enqueueWork(context, DriveJobIntentService.class, jobId, intent);
            LogUtil.logDebug(TAG, "Enqueue upload trust contacts");
        } else {
            LogUtil.logError(TAG, "Unknown jobId=" + jobId,
                    new IllegalArgumentException("Unknown jobId=" + jobId));
        }

        // TODO: add upload UUID (hashed)
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (intent == null) {
            LogUtil.logError(TAG, "intent is null");
            return;
        }

        Context context = getBaseContext();
        if (context == null) {
            LogUtil.logError(TAG, "context is null");
            return;
        }

        int jobId = intent.getIntExtra(KEY_JOB_ID, DEFAULT_VALUE);
        if (jobId == JOB_ID_SKR_UPLOAD_TRUST_CONTACTS) {
            LogUtil.logDebug(TAG, "Upload trust contacts");
            if (DriveUtil.driveAuth(context)) {
                LogUtil.logDebug(TAG, "Drive auth success");
                int uuidStatus = DriveUtil.getUUIDStatus(context);
                switch (uuidStatus) {
                    case UUIDStatusType.ERROR:
                        LogUtil.logWarning(TAG, "Check UUID status failed, skip");
                        break;
                    case UUIDStatusType.MATCH:
                        LogUtil.logDebug(TAG, "UUID match");
                        if (DriveUtil.uploadTrustContacts(context)) {
                            LogUtil.logDebug(TAG, "Upload trust contacts success");

                            // check and delete deprecated trust contacts file
                            DriveUtil.checkDeprecatedTrustContactsFile(context);
                        } else {
                            LogUtil.logError(TAG, "Upload trust contacts failed");
                            // drive auth pass but upload failed. It's seem not happened.
                            // it should re-upload next time Activity launched
                        }
                        break;
                    case UUIDStatusType.NOT_MATCH:
                        LogUtil.logDebug(TAG, "UUID not match, skip");
                        sendLocalBroadcast(context, ACTION_CHECK_DRIVE);
                        break;
                    default:
                        LogUtil.logError(TAG, "Unknown uuidStatus=" + uuidStatus);
                }
            } else {
                LogUtil.logError(TAG, "Drive auth failed");
                sendLocalBroadcast(context, ACTION_DRIVE_AUTH);
            }
        } else {
            LogUtil.logError(TAG, "Unknown jobId=" + jobId);
        }
    }

    private static void sendLocalBroadcast(Context context, String action) {
        Objects.requireNonNull(context, "context is null");
        if (TextUtils.isEmpty(action)) {
            LogUtil.logError(TAG, "action is null or empty",
                    new IllegalArgumentException("action is null or empty"));
            return;
        }

        Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
