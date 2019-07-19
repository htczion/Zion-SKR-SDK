package com.htc.wallet.skrsdk.restore.util;

import static com.htc.wallet.skrsdk.restore.RestoreVerificationCodeView.ET_PIN_SIZE;
import static com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity.RESTORE_SOURCE_STATUS_OK;
import static com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity.RESTORE_SOURCE_STATUS_REQUEST;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.action.RestoreOKAction;
import com.htc.wallet.skrsdk.crypto.ChecksumUtil;
import com.htc.wallet.skrsdk.crypto.util.VerificationUtil;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.sqlite.util.RestoreSourceUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SocialRestoreUtil {
    private static final String TAG = "SocialRestoreUtil";

    private static final int TIMEOUT = 60; // seconds

    public static void saveRestoreSourceWhileRequest(
            @NonNull final Context context,
            final String uuidHash,
            final String token,
            final String whisperPub,
            final String pushyToken,
            final String publicKey,
            final long timeStamp) {
        Objects.requireNonNull(context, "context is null");

        final RestoreSourceEntity restoreSourceEntity = new RestoreSourceEntity();
        restoreSourceEntity.setStatus(RESTORE_SOURCE_STATUS_REQUEST);
        restoreSourceEntity.setUUIDHash(uuidHash);
        restoreSourceEntity.setFcmToken(token);
        restoreSourceEntity.setWhisperPub(whisperPub);
        restoreSourceEntity.setPushyToken(pushyToken);
        restoreSourceEntity.setPublicKey(publicKey);
        restoreSourceEntity.setTimeStamp(timeStamp);
        restoreSourceEntity.setPinCodePosition(0);

        RestoreSourceUtil.put(context, restoreSourceEntity, new DatabaseCompleteListener() {
            @Override
            public void onComplete() {
            }

            @Override
            public void onError(Exception exception) {
                LogUtil.logError(TAG, "put error, e= " + exception);
            }
        });
    }

    public static void saveRestoreSourceAndSendOK(
            @NonNull final Context context,
            final String uuidHash,
            final String token,
            final String whisperPub,
            final String pushyToken,
            final String publicKey,
            final long timeStamp,
            final String pinCode,
            final int pinCodePosition,
            final String seed,
            final String encSeedSign,
            final String name,
            final String encCodePk,
            final String encCodePkSign,
            final String encAseKey,
            final String encAseKeySign) {
        Objects.requireNonNull(context, "context is null");

        final RestoreSourceEntity restoreSourceEntity = new RestoreSourceEntity();
        restoreSourceEntity.setUUIDHash(uuidHash);
        restoreSourceEntity.setFcmToken(token);
        restoreSourceEntity.setWhisperPub(whisperPub);
        restoreSourceEntity.setPushyToken(pushyToken);
        restoreSourceEntity.setPublicKey(publicKey);
        restoreSourceEntity.setTimeStamp(timeStamp);
        restoreSourceEntity.setPinCode(pinCode);
        restoreSourceEntity.setPinCodePosition(pinCodePosition);
        restoreSourceEntity.setSeed(seed);
        restoreSourceEntity.setEncSeedSigned(encSeedSign);
        restoreSourceEntity.setStatus(RESTORE_SOURCE_STATUS_OK);
        restoreSourceEntity.setName(name);
        restoreSourceEntity.setEncCodePk(encCodePk);
        restoreSourceEntity.setEncCodePkSign(encCodePkSign);
        restoreSourceEntity.setEncAseKey(encAseKey);
        restoreSourceEntity.setEncAseKeySign(encAseKeySign);
        RestoreSourceUtil.update(context, restoreSourceEntity, new DatabaseCompleteListener() {
            @Override
            public void onComplete() {
                LogUtil.logDebug(TAG, "saveRestoreSourceAndSendOKIfNeeded completely, "
                        + "sendRestoreOKAction");
                final String email = SkrSharedPrefs.getCurrentRestoreEmail(context);
                if (TextUtils.isEmpty(email)) {
                    LogUtil.logError(TAG, "email is null or empty", new IllegalStateException());
                    return;
                }
                final String emailHash = ChecksumUtil.generateChecksum(email);
                if (TextUtils.isEmpty(emailHash)) {
                    LogUtil.logError(
                            TAG, "emailHash is null or empty", new IllegalStateException());
                    return;
                }
                sendRestoreOKAction(context, token, whisperPub, pushyToken, publicKey, emailHash);
            }

            @Override
            public void onError(Exception exception) {
                LogUtil.logError(TAG, "update error, e= " + exception);
            }
        });
    }

    private static void sendRestoreOKAction(
            @NonNull final Context context,
            final String restoreSourceToken,
            final String restoreSourceWhisperPub,
            final String restoreSourcePushyToken,
            final String restoreSourcePublicKey,
            final String emailHash) {

        final String uuid = PhoneUtil.getSKRID(context);
        final VerificationUtil verificationUtil = new VerificationUtil(false);
        final String encryptedUUID = verificationUtil.encryptMessage(uuid, restoreSourcePublicKey);
        final RestoreOKAction restoreOKAction = new RestoreOKAction();

        final Map<String, String> messagesToRestoreSources =
                restoreOKAction.createOKMessage(encryptedUUID, emailHash);
        restoreOKAction.send(
                context,
                restoreSourceToken,
                restoreSourceWhisperPub,
                restoreSourcePushyToken,
                messagesToRestoreSources);
    }

    @WorkerThread
    public static boolean isEditTextPositionHasBeenRestored(@NonNull Context context,
            final int position) {
        Objects.requireNonNull(context, "context is null");
        if (position < 0 || position >= ET_PIN_SIZE) {
            throw new IllegalArgumentException("Incorrect position=" + position);
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean isRestoreOK = new AtomicBoolean(false);

        RestoreSourceUtil.getAll(context, new LoadListListener() {
            @Override
            public void onLoadFinished(
                    List<BackupSourceEntity> backupSourceEntityList,
                    List<BackupTargetEntity> backupTargetEntityList,
                    List<RestoreSourceEntity> restoreSourceEntityList,
                    List<RestoreTargetEntity> restoreTargetEntityList) {

                // It should not happen
                if (restoreSourceEntityList == null) {
                    LogUtil.logError(TAG, "restoreSourceEntityList is null");
                    latch.countDown();
                    return;
                }

                for (RestoreSourceEntity restoreSourceEntity : restoreSourceEntityList) {
                    if ((position == restoreSourceEntity.getPinCodePosition()) &&
                            restoreSourceEntity.compareStatus(RESTORE_SOURCE_STATUS_OK)) {
                        isRestoreOK.set(true);
                        break;
                    }
                }

                latch.countDown();
            }
        });

        try {
            latch.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LogUtil.logError(TAG, "InterruptedException e=" + e);
        }

        return isRestoreOK.get();
    }
}
