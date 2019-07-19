package com.htc.wallet.skrsdk.crypto.util;

import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import com.htc.wallet.skrsdk.crypto.SymmetricCipherUtil;
import com.htc.wallet.skrsdk.keystore.KeyStoreFactory;
import com.htc.wallet.skrsdk.keystore.SocialKeyStore;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class GenericCipherUtil {
    private static final String TAG = "GenericCipherUtil";
    private static final String KEYSTORE_ALIAS =
            BackupKeyStoreConstant.BACKUP_TARGET_SYMMETRIC_KEYSTORE_ALIAS;
    private final CountDownLatch mLatch = new CountDownLatch(1);
    private SocialKeyStore mKeyStore;
    private SymmetricCipherUtil mSymmetricCipherUtil;
    private SecretKey mSecretKey;

    public GenericCipherUtil() {
        setupKeystore();
        setupSymmetricKey();
        setupCipherUtils();
    }

    private void setupKeystore() {
        mKeyStore = KeyStoreFactory.getKeyStore();
        if (!mKeyStore.anyKeysExist(KEYSTORE_ALIAS)) {
            try {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            mKeyStore.generateNewSymmetricKeys(KEYSTORE_ALIAS);
                        } finally {
                            mLatch.countDown();
                        }
                    }
                }.start();
                mLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupSymmetricKey() {
        mSecretKey = mKeyStore.getSecretKey(KEYSTORE_ALIAS);
    }

    private void setupCipherUtils() {
        mSymmetricCipherUtil = new SymmetricCipherUtil(mKeyStore);
    }

    @WorkerThread
    public String encryptData(final String data) {
        if (TextUtils.isEmpty(data)) {
            throw new IllegalArgumentException("encryptData, data is empty");
        }
        try {
            return mSymmetricCipherUtil.encrypt(data, mSecretKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Log.e(TAG, "encryptData, error = " + e);
        }
        return "";
    }

    @WorkerThread
    public String decryptData(final String data) {
        if (TextUtils.isEmpty(data)) {
            throw new IllegalArgumentException("decryptData, data is empty");
        }
        try {
            return mSymmetricCipherUtil.decrypt(data, mSecretKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Log.e(TAG, "decryptData, error = " + e);
        }
        return "";
    }
}
