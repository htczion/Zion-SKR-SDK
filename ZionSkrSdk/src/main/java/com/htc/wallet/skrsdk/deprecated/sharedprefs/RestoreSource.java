package com.htc.wallet.skrsdk.deprecated.sharedprefs;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.crypto.ChecksumUtil;
import com.htc.wallet.skrsdk.crypto.util.GenericCipherUtil;
import com.htc.wallet.skrsdk.error.DecryptException;
import com.htc.wallet.skrsdk.error.EncryptException;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

public class RestoreSource extends StorageBase {
    public static final int RESTORE_SOURCE_STATUS_REQUEST = 0;
    public static final int RESTORE_SOURCE_STATUS_OK = 1;

    private static final String TAG = "RestoreSource";
    private static final String EMPTY_STRING = "";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RESTORE_SOURCE_STATUS_REQUEST, RESTORE_SOURCE_STATUS_OK})
    public @interface RestoreSourceStatusType {
    }

    public static final long UNDEFINED_TIME_STAMP = -1L;

    // Status
    private final int mStatus;
    private final Object mLock = new Object();
    private volatile boolean mIsEncrypted = false;

    // General, Result
    private final String mUUIDHash;
    private volatile String mFcmToken; // AES
    private volatile String mPublicKey; // AES
    private final long mTimeStamp;
    private final String mName;

    // OK
    private final String mSeed; // encrypted by Self's(Amy's) publicKey

    private volatile int mPinCodePosition = -1;

    private volatile String mPinCode = "";

    @Override
    boolean upgrade() {
        if (mVersion == STORAGE_VERSION) {
            return false;
        }
        // Do upgrade here
        return true;
    }

    private RestoreSource(
            int version,
            int status,
            String uuidHash,
            String fcmToken,
            String publicKey,
            long timeStamp,
            String seed,
            String name) {
        mVersion = version;
        mStatus = status;
        mUUIDHash = uuidHash;
        mTimeStamp = timeStamp;
        mFcmToken = fcmToken;
        mPublicKey = publicKey;
        mSeed = seed;
        mName = name;
    }

    RestoreSource(@NonNull RestoreSource source) {
        Objects.requireNonNull(source, "restoreSource is null");
        synchronized (mLock) {
            mVersion = source.mVersion;
            mStatus = source.mStatus;
            mIsEncrypted = source.mIsEncrypted;
            mUUIDHash = source.mUUIDHash;
            mTimeStamp = source.mTimeStamp;
            mFcmToken = source.mFcmToken;
            mPublicKey = source.mPublicKey;
            mSeed = source.mSeed;
            mPinCode = source.mPinCode;
            mPinCodePosition = source.mPinCodePosition;
            mName = source.mName;
        }
    }

    @WorkerThread
    void encrypt() {
        if (mIsEncrypted) {
            return;
        }

        synchronized (mLock) {
            if (mIsEncrypted) {
                return;
            }

            // AES
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
            // Encrypt AES
            String encFcmToken = genericCipherUtil.encryptData(mFcmToken);
            if (TextUtils.isEmpty(encFcmToken)) {
                LogUtil.logError(TAG, "Failed to encrypt FCM token", new EncryptException());
                return;
            }
            String encPublicKey = genericCipherUtil.encryptData(mPublicKey);
            if (TextUtils.isEmpty(encPublicKey)) {
                LogUtil.logError(TAG, "Failed to encrypt Public Key", new EncryptException());
                return;
            }
            // Update
            mIsEncrypted = true;
            mFcmToken = encFcmToken;
            mPublicKey = encPublicKey;
        }
    }

    @WorkerThread
    void decrypt() {
        if (!mIsEncrypted) {
            return;
        }

        synchronized (mLock) {
            if (!mIsEncrypted) {
                return;
            }
            // AES
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
            // Decrypt AES
            String decFcmToken = genericCipherUtil.decryptData(mFcmToken);
            if (TextUtils.isEmpty(decFcmToken)) {
                LogUtil.logError(TAG, "Failed to decrypt FCM token", new DecryptException());
                return;
            }
            String decPublicKey = genericCipherUtil.decryptData(mPublicKey);
            if (TextUtils.isEmpty(decPublicKey)) {
                LogUtil.logError(TAG, "Failed to decrypt Public Key", new DecryptException());
                return;
            }
            // Update
            mIsEncrypted = false;
            mFcmToken = decFcmToken;
            mPublicKey = decPublicKey;
        }
    }

    public int getStatus() {
        return mStatus;
    }

    public String getUUIDHash() {
        return mUUIDHash;
    }

    public long getTimeStamp() {
        return mTimeStamp;
    }

    public String getFcmToken() {
        if (mIsEncrypted) {
            LogUtil.logError(TAG, "Wrong encrypt state");
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
            String decFcmToken = genericCipherUtil.decryptData(mFcmToken);
            if (TextUtils.isEmpty(decFcmToken)) {
                LogUtil.logError(TAG, "Failed to decrypt FCM token", new DecryptException());
            }
            return decFcmToken;
        }
        return mFcmToken;
    }

    public String getPublicKey() {
        if (mIsEncrypted) {
            LogUtil.logError(TAG, "Wrong encrypt state");
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
            String decPublicKey = genericCipherUtil.decryptData(mPublicKey);
            if (TextUtils.isEmpty(decPublicKey)) {
                LogUtil.logError(TAG, "Failed to decrypt Public Key", new DecryptException());
                return EMPTY_STRING;
            }
            return decPublicKey;
        }
        return mPublicKey;
    }

    public String getSeed() {
        requireStatus(RESTORE_SOURCE_STATUS_OK);
        return mSeed;
    }

    public String getName() {
        return mName;
    }

    public int getPinCodePosition() {
        return mPinCodePosition;
    }

    public void setPinCodePosition(int position) {
        mPinCodePosition = position;
    }

    public String getPinCode() {
        return mPinCode;
    }

    public void setPinCode(String pinCode) {
        mPinCode = pinCode;
    }

    public boolean isEncrypted() {
        return mIsEncrypted;
    }

    public boolean compareStatus(@RestoreSourceStatusType int status) {
        return mStatus == status;
    }

    public boolean compareUUID(String uuid) {
        if (TextUtils.isEmpty(uuid)) {
            LogUtil.logDebug(TAG, "uuid is null or empty");
            return false;
        }

        String uuidHash = ChecksumUtil.generateChecksum(uuid);
        return mUUIDHash.equals(uuidHash);
    }

    public boolean compareUUIDHash(String uuidHash) {
        return mUUIDHash.equals(uuidHash);
    }

    private void requireStatus(@RestoreSourceStatusType int... statuses) {
        // OR, any one status match than pass
        for (int status : statuses) {
            if (mStatus == status) {
                return;
            }
        }
        LogUtil.logError(TAG, "Incorrect status", new IllegalStateException("Incorrect status"));
    }

    public static final class Builder {
        // Status
        private int mStatus;

        // General, Request
        private String mUUIDHash = null;
        private String mFcmToken = null;
        private String mPublicKey = null;
        private String mName = null;
        private long mTimeStamp = UNDEFINED_TIME_STAMP;

        // OK
        private String mSeed = null;

        public Builder(@RestoreSourceStatusType int status) {
            mStatus = status;
        }

        public Builder setUUIDHash(String uuidHash) {
            mUUIDHash = uuidHash;
            return this;
        }

        public Builder setTimeStamp(long timeStamp) {
            mTimeStamp = timeStamp;
            return this;
        }

        public Builder setFcmToken(String fcmToken) {
            if (mStatus != RESTORE_SOURCE_STATUS_REQUEST && mStatus != RESTORE_SOURCE_STATUS_OK) {
                LogUtil.logError(TAG, "Incorrect status");
                return this;
            }
            mFcmToken = fcmToken;
            return this;
        }

        public Builder setPublicKey(String publicKey) {
            if (mStatus != RESTORE_SOURCE_STATUS_REQUEST && mStatus != RESTORE_SOURCE_STATUS_OK) {
                LogUtil.logError(TAG, "Incorrect status");
                return this;
            }
            mPublicKey = publicKey;
            return this;
        }

        public Builder setName(String name) {
            mName = name;
            return this;
        }

        public Builder setSeed(String seed) {
            if (mStatus != RESTORE_SOURCE_STATUS_OK) {
                LogUtil.logError(TAG, "Incorrect status");
                return this;
            }
            mSeed = seed;
            return this;
        }

        public RestoreSource build() {
            requireNotEmpty(mUUIDHash, "uuidHash is null or empty");
            requireNotEmpty(mFcmToken, "fcmToken is null or empty");
            requireNotEmpty(mPublicKey, "publicKey is null or empty");
            if (mTimeStamp == UNDEFINED_TIME_STAMP || mTimeStamp < 0) {
                throw new IllegalArgumentException("timeStamp is incorrect or undefined");
            }

            switch (mStatus) {
                case RESTORE_SOURCE_STATUS_REQUEST:
                    break;
                case RESTORE_SOURCE_STATUS_OK:
                    requireNotEmpty(mSeed, "seed is null or empty");
                    break;
                default:
                    throw new IllegalArgumentException("status incorrect");
            }
            return new RestoreSource(
                    STORAGE_VERSION,
                    mStatus,
                    mUUIDHash,
                    mFcmToken,
                    mPublicKey,
                    mTimeStamp,
                    mSeed,
                    mName);
        }

        private void requireNotEmpty(@Nullable CharSequence str, String message) {
            if (TextUtils.isEmpty(str)) {
                throw new IllegalArgumentException(message);
            }
        }
    }
}
