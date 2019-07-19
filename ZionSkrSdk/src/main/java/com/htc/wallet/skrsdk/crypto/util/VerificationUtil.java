package com.htc.wallet.skrsdk.crypto.util;

import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.crypto.AsymmetricCipherUtil;
import com.htc.wallet.skrsdk.crypto.Base64Util;
import com.htc.wallet.skrsdk.crypto.ChecksumUtil;
import com.htc.wallet.skrsdk.keystore.KeyStoreFactory;
import com.htc.wallet.skrsdk.keystore.SocialKeyStore;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.ThreadUtil;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.NoSuchPaddingException;

public class VerificationUtil {
    private static final String TAG = "VerificationUtil";
    private static final int KEY_SETUP_SLEEP_TIME = 1000;
    private SocialKeyStore mKeyStore;
    private PublicKey mPublicKey;
    private PrivateKey mPrivateKey;
    private String mKeyStoreAlias = BackupKeyStoreConstant.BACKUP_SOURCE_ASYMMETRIC_KEYSTORE_ALIAS;
    private AsymmetricCipherUtil mAsymmetricCipherUtil;
    private final AtomicBoolean mKeySetupCompleted = new AtomicBoolean(false);
    private final ThreadPoolExecutor mSingleThreadExecutor = ThreadUtil.newFixedThreadPool(1, "verify-util");

    public VerificationUtil(final boolean isBackupTarget) {
        setupKeystore(isBackupTarget);
    }

    private void setupKeystore(final boolean isBackupTarget) {
        mKeyStore = KeyStoreFactory.getKeyStore();
        if (isBackupTarget) {
            mKeyStore.changeToBackupTarget();
            mKeyStoreAlias = BackupKeyStoreConstant.BACKUP_TARGET_ASYMMETRIC_KEYSTORE_ALIAS;
        }
        if (!mKeyStore.anyKeysExist(mKeyStoreAlias) ||
                !mKeyStore.isCertificateExist(mKeyStoreAlias)) {
            mSingleThreadExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (VerificationUtil.class) {
                        if (!mKeyStore.anyKeysExist(mKeyStoreAlias)) {
                            mKeyStore.generateNewAsymmetricKeyPair(mKeyStoreAlias);
                        } else if (!mKeyStore.isCertificateExist(mKeyStoreAlias)) {
                            LogUtil.logWarning(TAG, "Key alias exist but certificate not exist");
                            mKeyStore.generateNewAsymmetricKeyPair(mKeyStoreAlias);
                        }
                        setupAsymmetricPublicKey();
                        setupCipherUtils();
                        mKeySetupCompleted.set(true);
                    }
                }
            });
        } else {
            synchronized (VerificationUtil.class) {
                setupAsymmetricPublicKey();
                setupCipherUtils();
                mKeySetupCompleted.set(true);
            }
        }
    }

    private void setupAsymmetricPublicKey() {
        mPublicKey = mKeyStore.getPublicKey(mKeyStoreAlias);
        mPrivateKey = mKeyStore.getPrivateKey(mKeyStoreAlias);
    }

    private void setupCipherUtils() {
        mAsymmetricCipherUtil = new AsymmetricCipherUtil(mKeyStore);
    }

    @WorkerThread
    public String encryptMessage(final String message, final String backupSourcePublicKeyString) {
        if (TextUtils.isEmpty(message) || TextUtils.isEmpty(backupSourcePublicKeyString)) {
            throw new IllegalArgumentException(
                    "encryptMessage, message or backupSourcePublicKeyString is empty");
        }
        waitKeySetup();
        try {
            final PublicKey needingBackupOwnerPublicKey =
                    mAsymmetricCipherUtil.convertToPublicKey(backupSourcePublicKeyString);
            return mAsymmetricCipherUtil.encrypt(
                    message, Objects.requireNonNull(needingBackupOwnerPublicKey));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeySpecException e) {
            LogUtil.logError(TAG, "encryptMessage, error = " + e);
        }
        return "";
    }

    @WorkerThread
    public String encryptMessage(final byte[] data, final String backupSourcePublicKeyString) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("decryptMessage, data is empty");
        }
        waitKeySetup();
        try {
            final PublicKey needingBackupOwnerPublicKey =
                    mAsymmetricCipherUtil.convertToPublicKey(backupSourcePublicKeyString);
            return Base64Util.encodeToString(
                    mAsymmetricCipherUtil.encrypt(
                            data, Objects.requireNonNull(needingBackupOwnerPublicKey)));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeySpecException e) {
            LogUtil.logError(TAG, "encryptMessage, error = " + e);
        }
        return "";
    }

    @WorkerThread
    public String decryptMessage(final String message) {
        if (TextUtils.isEmpty(message)) {
            throw new IllegalArgumentException("decryptMessage, message is empty");
        }
        waitKeySetup();
        try {
            return mAsymmetricCipherUtil.decrypt(message, Objects.requireNonNull(mPrivateKey));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            LogUtil.logError(TAG, "decryptMessage, error = " + e);
        }
        return message;
    }

    @WorkerThread
    public byte[] decryptMessage(final byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("decryptMessage, data is empty");
        }
        waitKeySetup();
        try {
            return mAsymmetricCipherUtil.decrypt(data, Objects.requireNonNull(mPrivateKey));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            LogUtil.logError(TAG, "decryptMessage, error = " + e);
        }
        return data;
    }

    @WorkerThread
    public String getPublicKeyString() {
        waitKeySetup();
        return mAsymmetricCipherUtil.convertToString(mPublicKey);
    }

    public String getChecksum(final String message) {
        if (TextUtils.isEmpty(message)) {
            throw new IllegalArgumentException("getChecksum, seed is null");
        }
        return ChecksumUtil.generateChecksum(message);
    }

    public boolean verifyChecksum(final String message, final String checksum) {
        if (TextUtils.isEmpty(message) || TextUtils.isEmpty(checksum)) {
            throw new IllegalArgumentException("verifyChecksum, message or checksum is null");
        }
        return ChecksumUtil.verifyMessageWithChecksum(message, checksum);
    }

    private void waitKeySetup() {
        while (!mKeySetupCompleted.get()) {
            try {
                Thread.sleep(KEY_SETUP_SLEEP_TIME);
            } catch (InterruptedException e) {
                LogUtil.logError(TAG, "waitKeySetup, error = " + e);
            }
        }
    }
}
