package com.htc.wallet.skrsdk.legacy.v1;

import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.crypto.util.GenericCipherUtil;
import com.htc.wallet.skrsdk.util.LogUtil;

public class LegacyBackupDataV1 {
    private static final String TAG = "LegacyBackupDataV1";

    private final Object mLock = new Object();

    private volatile boolean mIsEncrypted;

    private volatile String mUuidHash;
    private volatile String mPublicKey; // AES

    public LegacyBackupDataV1() {
        mIsEncrypted = false;
    }

    LegacyBackupDataV1(LegacyBackupDataV1 legacyBackupDataV1) {
        mIsEncrypted = legacyBackupDataV1.mIsEncrypted;
        mUuidHash = legacyBackupDataV1.mUuidHash;
        mPublicKey = legacyBackupDataV1.mPublicKey;
    }

    public void setUuidHash(String uuidHash) {
        mUuidHash = uuidHash;
    }

    public String getUuidHash() {
        return mUuidHash;
    }

    public void setPublicKey(String publicKey) {
        mPublicKey = publicKey;
    }

    public String getPublicKey() {
        return mPublicKey;
    }

    @WorkerThread
    void encrypt() {
        if (mIsEncrypted) {
            LogUtil.logWarning(TAG, "It's encrypted");
            return;
        }

        synchronized (mLock) {
            if (mIsEncrypted) {
                LogUtil.logWarning(TAG, "It's encrypted");
                return;
            }

            // AES
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();

            if (!TextUtils.isEmpty(mPublicKey)) {
                mPublicKey = genericCipherUtil.encryptData(mPublicKey);
            }

            mIsEncrypted = true;
        }
    }

    @WorkerThread
    void decrypt() {
        if (!mIsEncrypted) {
            LogUtil.logWarning(TAG, "It's decrypted");
            return;
        }

        synchronized (mLock) {
            if (!mIsEncrypted) {
                LogUtil.logWarning(TAG, "It's decrypted");
                return;
            }

            // AES
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();

            if (!TextUtils.isEmpty(mPublicKey)) {
                mPublicKey = genericCipherUtil.decryptData(mPublicKey);
            }

            mIsEncrypted = false;
        }
    }
}
