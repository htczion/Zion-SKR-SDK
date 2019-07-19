package com.htc.wallet.skrsdk.deprecated.sharedprefs;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.crypto.util.GenericCipherUtil;
import com.htc.wallet.skrsdk.error.DecryptException;
import com.htc.wallet.skrsdk.error.EncryptException;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PinCodeUtil;
import com.htc.wallet.skrsdk.util.RetryUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

public class BackupSource extends StorageBase {
    public static final int BACKUP_SOURCE_STATUS_REQUEST = 0;
    public static final int BACKUP_SOURCE_STATUS_OK = 1;
    public static final int BACKUP_SOURCE_STATUS_DONE = 2;
    public static final int BACKUP_SOURCE_STATUS_FULL = 3;

    private static final String TAG = "BackupSource";
    private static final String EMPTY_STRING = "";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            BACKUP_SOURCE_STATUS_REQUEST,
            BACKUP_SOURCE_STATUS_OK,
            BACKUP_SOURCE_STATUS_DONE,
            BACKUP_SOURCE_STATUS_FULL
    })
    public @interface BackupSourceStatusType {
    }

    public static final long UNDEFINED_TIME_STAMP = -1L;
    public static final int INIT_RETRY_TIMES = 0;
    public static final long INIT_RETRY_WAIT_START_TIME = 0;
    public static final long INIT_LAST_VERIFY_TIME = 0;
    public static final boolean INIT_IS_PIN_CODE_ERROR = false;
    public static final String INIT_PIN_CODE = "";

    // Status
    private volatile int mStatus;
    private final Object mLock = new Object();
    private volatile boolean mIsEncrypted = false;

    // General
    private final String mEmailHash;
    private volatile String mFcmToken;
    private final String mName;
    private String mUUIDHash;
    private volatile long mTimeStamp;
    private volatile String mPublicKey;
    private final String mMyName;

    // Not Check Begin
    private volatile Integer mRetryTimes = null;
    private volatile Long mRetryWaitStartTime = null;
    private volatile String mPinCode = null;
    private volatile Long mLastVerifyTime = null;
    private volatile Boolean mIsPinCodeError = null;
    private volatile Long mLastRequestTime = null;
    // Not Check End

    // BACKUP_SOURCE_STATUS_OK, BACKUP_SOURCE_STATUS_DONE
    private final String mSeed;
    private final String mCheckSum;

    @Override
    boolean upgrade() {
        if (mVersion == STORAGE_VERSION) {
            return false;
        }
        // Do upgrade here
        return true;
    }

    private BackupSource(
            int version,
            int status,
            String emailHash,
            String fcmToken,
            String name,
            String uuidHash,
            long timeStamp,
            String publicKey,
            String myName,
            String seed,
            String checkSum) {
        mVersion = version;
        mStatus = status;
        mEmailHash = emailHash;
        mFcmToken = fcmToken;
        mName = name;
        mUUIDHash = uuidHash;
        mTimeStamp = timeStamp;
        mPublicKey = publicKey;
        mMyName = myName;
        mSeed = seed;
        mCheckSum = checkSum;
    }

    BackupSource(BackupSource source) {
        Objects.requireNonNull(source, "backupSource is null");
        synchronized (mLock) {
            mVersion = source.mVersion;
            mStatus = source.mStatus;
            mIsEncrypted = source.mIsEncrypted;
            mEmailHash = source.mEmailHash;
            mFcmToken = source.mFcmToken;
            mName = source.mName;
            mUUIDHash = source.mUUIDHash;
            mTimeStamp = source.mTimeStamp;
            mPublicKey = source.mPublicKey;
            mMyName = source.mMyName;
            mRetryTimes = source.mRetryTimes;
            mRetryWaitStartTime = source.mRetryWaitStartTime;
            mPinCode = source.mPinCode;
            mLastVerifyTime = source.mLastVerifyTime;
            mIsPinCodeError = source.mIsPinCodeError;
            mLastRequestTime = source.mLastRequestTime;
            mSeed = source.mSeed;
            mCheckSum = source.mCheckSum;
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
            // Encrypt AES FCM token
            String encFcmToken = genericCipherUtil.encryptData(mFcmToken);
            if (TextUtils.isEmpty(encFcmToken)) {
                LogUtil.logError(TAG, "Failed to encrypt FCM token", new EncryptException());
                return;
            }
            // Encrypt AES Public Key
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
            // Decrypt AES FCM token
            String decFcmToken = genericCipherUtil.decryptData(mFcmToken);
            if (TextUtils.isEmpty(decFcmToken)) {
                LogUtil.logError(TAG, "Failed to decrypt FCM token", new DecryptException());
                return;
            }
            // Decrypt AES Public Key
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

    public String getEmailHash() {
        return mEmailHash;
    }

    public String getFcmToken() {
        if (mIsEncrypted) {
            LogUtil.logError(TAG, "Wrong encrypt state");
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
            String decFcmToken = genericCipherUtil.decryptData(mFcmToken);
            if (TextUtils.isEmpty(decFcmToken)) {
                LogUtil.logError(TAG, "Failed to decrypt FCM token", new DecryptException());
                return EMPTY_STRING;
            }
            return decFcmToken;
        }
        return mFcmToken;
    }

    public String getName() {
        return mName;
    }

    public String getUUIDHash() {
        return mUUIDHash;
    }

    public void setUUIDHash(String UUIDHash) {
        mUUIDHash = UUIDHash;
    }

    public long getTimeStamp() {
        return mTimeStamp;
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

    public String getMyName() {
        return mMyName;
    }

    public int getRetryTimes() {
        requireStatus(BACKUP_SOURCE_STATUS_REQUEST, BACKUP_SOURCE_STATUS_FULL);
        if (mRetryTimes == null) {
            return INIT_RETRY_TIMES;
        }
        return mRetryTimes;
    }

    public long getRetryWaitStartTime() {
        requireStatus(BACKUP_SOURCE_STATUS_REQUEST, BACKUP_SOURCE_STATUS_FULL);
        if (mRetryWaitStartTime == null) {
            return INIT_RETRY_WAIT_START_TIME;
        }
        return mRetryWaitStartTime;
    }

    public String getPinCode() {
        requireStatus(BACKUP_SOURCE_STATUS_REQUEST, BACKUP_SOURCE_STATUS_FULL);
        if (mPinCode == null) {
            return INIT_PIN_CODE;
        }
        return mPinCode;
    }

    public long getLastVerifyTime() {
        requireStatus(BACKUP_SOURCE_STATUS_REQUEST, BACKUP_SOURCE_STATUS_FULL);
        if (mLastVerifyTime == null) {
            return INIT_LAST_VERIFY_TIME;
        }
        return mLastVerifyTime;
    }

    public boolean getIsPinCodeError() {
        requireStatus(BACKUP_SOURCE_STATUS_REQUEST, BACKUP_SOURCE_STATUS_FULL);
        if (mIsPinCodeError == null) {
            return INIT_IS_PIN_CODE_ERROR;
        }
        return mIsPinCodeError;
    }

    public long getLastRequestTime() {
        requireStatus(BACKUP_SOURCE_STATUS_REQUEST, BACKUP_SOURCE_STATUS_FULL);
        if (mLastRequestTime == null) {
            return System.currentTimeMillis();
        }
        return mLastRequestTime;
    }

    public void updateStatusToFull() {
        if (mStatus == BACKUP_SOURCE_STATUS_REQUEST) {
            mStatus = BACKUP_SOURCE_STATUS_FULL;
        } else {
            LogUtil.logDebug(
                    TAG, "Can not update to BACKUP_SOURCE_STATUS_FULL if mStatus=" + mStatus);
        }
    }

    public String getSeed() {
        requireStatus(BACKUP_SOURCE_STATUS_OK, BACKUP_SOURCE_STATUS_DONE);
        return mSeed;
    }

    public String getCheckSum() {
        requireStatus(BACKUP_SOURCE_STATUS_OK, BACKUP_SOURCE_STATUS_DONE);
        return mCheckSum;
    }

    public void setPublicKey(String publicKey) {
        mPublicKey = publicKey;
    }

    public boolean isEncrypted() {
        return mIsEncrypted;
    }

    public boolean compareStatus(@BackupSourceStatusType int status) {
        return mStatus == status;
    }

    public boolean compareEmailHash(String emailHash) {
        if (TextUtils.isEmpty(emailHash)) {
            LogUtil.logDebug(TAG, "emailHash is empty or null");
        }
        return mEmailHash.equals(emailHash);
    }

    public boolean compareUUIDHash(String uuidHash) {
        if (TextUtils.isEmpty(uuidHash)) {
            LogUtil.logDebug(TAG, "uuidHash is empty or null");
        }
        return mUUIDHash.equals(uuidHash);
    }

    private void requireStatus(@BackupSourceStatusType int... statuses) {
        // OR, any one status match than pass
        synchronized (mLock) {
            for (int status : statuses) {
                if (mStatus == status) {
                    return;
                }
            }
        }
        LogUtil.logError(TAG, "Incorrect status", new IllegalStateException("Incorrect status"));
    }

    public void setRetryTimes(int retryTimes) {
        requireStatus(BACKUP_SOURCE_STATUS_REQUEST);
        if (retryTimes < 0) {
            LogUtil.logError(TAG, "retryTimes incorrect");
            return;
        }

        synchronized (mLock) {
            if (getRetryTimes() != retryTimes) {
                mRetryTimes = retryTimes;
                mTimeStamp = System.currentTimeMillis();
                if (mRetryTimes % RetryUtil.MAXIMUM_TRY_NUMBER == 0) {
                    mRetryWaitStartTime = mTimeStamp;
                }
            }
            mIsPinCodeError = true;
        }
    }

    public void setPinCode(@NonNull String pinCode) {
        requireStatus(BACKUP_SOURCE_STATUS_REQUEST);
        if (!PinCodeUtil.isValidPinCode(pinCode)) {
            LogUtil.logError(
                    TAG, "inValid Pin Code", new IllegalStateException("inValid Pin Code"));
            return;
        }
        mPinCode = pinCode;
        mLastVerifyTime = System.currentTimeMillis();
        mIsPinCodeError = null;
    }

    public void clearPinCode() {
        requireStatus(BACKUP_SOURCE_STATUS_REQUEST);
        mPinCode = null;
        mLastVerifyTime = null;
        mIsPinCodeError = null;
    }

    public void setLastRequestTime(long lastRequestTime) {
        requireStatus(BACKUP_SOURCE_STATUS_REQUEST);
        mLastRequestTime = lastRequestTime;
    }

    public void setDone() {
        requireStatus(BACKUP_SOURCE_STATUS_OK);
        mStatus = BACKUP_SOURCE_STATUS_DONE;
    }

    public static final class Builder {
        // General
        private int mStatus;
        private String mEmailHash = null;
        private String mFcmToken = null;
        private String mName = null;
        private String mUUIDHash = null;
        private long mTimeStamp = UNDEFINED_TIME_STAMP;
        private String mPublicKey = null;
        private String mMyName = null;

        // OK, DONE
        private String mSeed = null;
        private String mCheckSum = null;

        public Builder(@BackupSourceStatusType int status) {
            mStatus = status;
        }

        public Builder setEmailHash(String emailHash) {
            mEmailHash = emailHash;
            return this;
        }

        public Builder setFcmToken(String fcmToken) {
            mFcmToken = fcmToken;
            return this;
        }

        public Builder setName(String name) {
            mName = name;
            return this;
        }

        public Builder setUUIDHash(String uuidHash) {
            mUUIDHash = uuidHash;
            return this;
        }

        public Builder setTimeStamp(long timeStamp) {
            mTimeStamp = timeStamp;
            return this;
        }

        public Builder setPublicKey(String publicKey) {
            mPublicKey = publicKey;
            return this;
        }

        public Builder setMyName(String myName) {
            mMyName = myName;
            return this;
        }

        public Builder setSeed(String secretKey) {
            // Only BACKUP_SOURCE_STATUS_OK or BACKUP_SOURCE_STATUS_DONE can setSeed
            if (mStatus != BACKUP_SOURCE_STATUS_OK && mStatus != BACKUP_SOURCE_STATUS_DONE) {
                LogUtil.logError(TAG, "Incorrect status");
                return this;
            }
            mSeed = secretKey;
            return this;
        }

        public Builder setCheckSum(String checkSum) {
            // Only BACKUP_SOURCE_STATUS_OK or BACKUP_SOURCE_STATUS_DONE can setCheckSum
            if (mStatus != BACKUP_SOURCE_STATUS_OK && mStatus != BACKUP_SOURCE_STATUS_DONE) {
                LogUtil.logError(TAG, "Incorrect status");
                return this;
            }
            mCheckSum = checkSum;
            return this;
        }

        public BackupSource build() {
            requireNotEmpty(mEmailHash, "email hash is null or empty");
            requireNotEmpty(mFcmToken, "fcmToken is null or empty");
            requireNotEmpty(mName, "name is null or empty");
            requireNotEmpty(mUUIDHash, "uuidHash is null or empty");
            if (mTimeStamp == UNDEFINED_TIME_STAMP || mTimeStamp < 0) {
                throw new IllegalArgumentException("timeStamp is incorrect or undefined");
            }
            requireNotEmpty(mPublicKey, "public key is null or empty");
            switch (mStatus) {
                case BACKUP_SOURCE_STATUS_REQUEST:
                    requireNotEmpty(mMyName, "my name is null or empty");
                    break;
                case BACKUP_SOURCE_STATUS_OK:
                case BACKUP_SOURCE_STATUS_DONE:
                    requireNotEmpty(mSeed, "seed is null or empty");
                    requireNotEmpty(mCheckSum, "checksum is null or empty");
                    break;
                default:
                    throw new IllegalArgumentException("incorrect status");
            }
            return new BackupSource(
                    STORAGE_VERSION,
                    mStatus,
                    mEmailHash,
                    mFcmToken,
                    mName,
                    mUUIDHash,
                    mTimeStamp,
                    mPublicKey,
                    mMyName,
                    mSeed,
                    mCheckSum);
        }

        private void requireNotEmpty(@Nullable CharSequence str, String message) {
            if (TextUtils.isEmpty(str)) {
                throw new IllegalArgumentException(message);
            }
        }
    }
}
