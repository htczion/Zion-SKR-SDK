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

public class BackupTarget extends StorageBase {
    public static final int BACKUP_TARGET_STATUS_REQUEST = 0;
    public static final int BACKUP_TARGET_STATUS_OK = 1;
    public static final int BACKUP_TARGET_STATUS_BAD = 2;
    public static final int BACKUP_TARGET_STATUS_NO_RESPONSE = 3;

    private static final String TAG = "BackupTarget";
    private static final String EMPTY_STRING = "";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            BACKUP_TARGET_STATUS_REQUEST,
            BACKUP_TARGET_STATUS_OK,
            BACKUP_TARGET_STATUS_BAD,
            BACKUP_TARGET_STATUS_NO_RESPONSE
    })
    public @interface BackupTargetStatusType {
    }

    public static final long UNDEFINED_LAST_CHECKED_TIME = -1L;
    public static final int UNDEFINED_SEED_INDEX = -1;

    private static final int INIT_RETRY_TIMES = 0;
    private static final long INIT_RETRY_WAIT_START_TIME = 0;

    // Status
    private volatile int mStatus;
    private final Object mLock = new Object();
    private volatile boolean mIsEncrypted = false;

    // General
    private volatile String mFcmToken; // AES
    private final String mName;
    private volatile String mPublicKey;
    private String mUUIDHash;
    private volatile long mLastCheckedTime;
    private int mSeedIndex; // isSeeded
    private String mCheckSum; // isSeeded

    // BACKUP_TARGET_STATUS_REQUEST
    private volatile Integer mRetryTimes; // Not Check
    private volatile Long mRetryWaitStartTime; // Not Check
    private volatile String mPhoneNumber; // AES
    private volatile String mPhoneModel;
    private volatile String mPinCode; // AES, isSeeded
    private volatile String mSeed; // isSeeded

    @Override
    boolean upgrade() {
        if (mVersion == STORAGE_VERSION) {
            return false;
        }
        // Do upgrade here
        return true;
    }

    private BackupTarget(
            int version,
            int status,
            String fcmToken,
            String name,
            String publicKey,
            String uuidHash,
            long lastCheckedTime,
            int seedIndex,
            String checkSum,
            Integer retryTimes,
            Long retryWaitStartTime,
            String phoneNumber,
            String phoneModel,
            String pinCode,
            String seed) {
        mVersion = version;
        mStatus = status;
        mFcmToken = fcmToken;
        mName = name;
        mPublicKey = publicKey;
        mUUIDHash = uuidHash;
        mLastCheckedTime = lastCheckedTime;
        mSeedIndex = seedIndex;
        mCheckSum = checkSum;
        mRetryTimes = retryTimes;
        mRetryWaitStartTime = retryWaitStartTime;
        mPhoneNumber = phoneNumber;
        mPhoneModel = phoneModel;
        mPinCode = pinCode;
        mSeed = seed;
    }

    BackupTarget(@NonNull BackupTarget target) {
        Objects.requireNonNull(target, "backupTarget is null");
        synchronized (mLock) {
            mVersion = target.mVersion;
            mStatus = target.mStatus;
            mIsEncrypted = target.mIsEncrypted;
            mFcmToken = target.mFcmToken;
            mName = target.mName;
            mPublicKey = target.mPublicKey;
            mUUIDHash = target.mUUIDHash;
            mLastCheckedTime = target.mLastCheckedTime;
            mSeedIndex = target.mSeedIndex;
            mCheckSum = target.mCheckSum;
            mRetryTimes = target.mRetryTimes;
            mRetryWaitStartTime = target.mRetryWaitStartTime;
            mPhoneNumber = target.mPhoneNumber;
            mPhoneModel = target.mPhoneModel;
            mPinCode = target.mPinCode;
            mSeed = target.mSeed;
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
            // Encrypt AES FCM Token
            String encFcmToken = EMPTY_STRING;
            if (!TextUtils.isEmpty(mFcmToken)) {
                encFcmToken = genericCipherUtil.encryptData(mFcmToken);
                if (TextUtils.isEmpty(encFcmToken)) {
                    LogUtil.logError(TAG, "Failed to encrypt FCM token", new EncryptException());
                    return;
                }
            }

            // Encrypt AES Public Key
            String encPublicKey = EMPTY_STRING;
            if (!TextUtils.isEmpty(mPublicKey)) {
                encPublicKey = genericCipherUtil.encryptData(mPublicKey);
                if (TextUtils.isEmpty(encPublicKey)) {
                    LogUtil.logError(TAG, "Failed to encrypt Public Key", new EncryptException());
                    return;
                }
            }

            // Request Member
            String encPhoneNumber = null;
            String encPinCode = null;
            if (compareStatus(BACKUP_TARGET_STATUS_REQUEST)) {
                // Encrypt AES Phone Number
                if (!TextUtils.isEmpty(mPhoneNumber)) {
                    encPhoneNumber = genericCipherUtil.encryptData(mPhoneNumber);
                    if (TextUtils.isEmpty(encPhoneNumber)) {
                        LogUtil.logError(
                                TAG, "Failed to encrypt phone number", new EncryptException());
                        return;
                    }
                }
                // Encrypt AES Pin Code
                if (isSeeded()) {
                    encPinCode = genericCipherUtil.encryptData(mPinCode);
                    if (TextUtils.isEmpty(encPinCode)) {
                        LogUtil.logError(TAG, "Failed to encrypt PIN code", new EncryptException());
                        return;
                    }
                }
            }
            // Update
            mIsEncrypted = true;
            mFcmToken = encFcmToken;
            mPublicKey = encPublicKey;
            mPhoneNumber = encPhoneNumber;
            mPinCode = encPinCode;
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
            // Decrypt AES FCM Token
            String decFcmToken = EMPTY_STRING;
            if (!TextUtils.isEmpty(mFcmToken)) {
                decFcmToken = genericCipherUtil.decryptData(mFcmToken);
                if (TextUtils.isEmpty(decFcmToken)) {
                    LogUtil.logError(TAG, "Failed to decrypt FCM token", new DecryptException());
                    return;
                }
            }
            // Decrypt AES Public Key
            String decPublicKey = EMPTY_STRING;
            if (!TextUtils.isEmpty(mPublicKey)) {
                decPublicKey = genericCipherUtil.decryptData(mPublicKey);
                if (TextUtils.isEmpty(decPublicKey)) {
                    LogUtil.logError(TAG, "Failed to decrypt Public Key", new DecryptException());
                    return;
                }
            }

            // Request Member
            String decPhoneNumber = null;
            String decPinCode = null;
            if (compareStatus(BACKUP_TARGET_STATUS_REQUEST)) {
                // Decrypt AES Phone Number
                if (!TextUtils.isEmpty(mPhoneNumber)) {
                    decPhoneNumber = genericCipherUtil.decryptData(mPhoneNumber);
                    if (TextUtils.isEmpty(decFcmToken)) {
                        LogUtil.logError(
                                TAG, "Failed to decrypt phone number", new DecryptException());
                        return;
                    }
                }
                // Decrypt AES Pin Code
                if (isSeeded()) {
                    if (!TextUtils.isEmpty(mPinCode)) {
                        decPinCode = genericCipherUtil.decryptData(mPinCode);
                        if (TextUtils.isEmpty(decPinCode)) {
                            LogUtil.logError(
                                    TAG, "Failed to decrypt PIN code", new DecryptException());
                            return;
                        }
                    }
                }
            }
            // Update
            mIsEncrypted = false;
            mFcmToken = decFcmToken;
            mPublicKey = decPublicKey;
            mPhoneNumber = decPhoneNumber;
            mPinCode = decPinCode;
        }
    }

    public int getStatus() {
        return mStatus;
    }

    public String getFcmToken() {
        if (mIsEncrypted) {
            LogUtil.logError(TAG, "Wrong encrypt state");
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
            String decFcmToken = EMPTY_STRING;
            if (!TextUtils.isEmpty(mFcmToken)) {
                decFcmToken = genericCipherUtil.decryptData(mFcmToken);
                if (TextUtils.isEmpty(decFcmToken)) {
                    LogUtil.logError(TAG, "Failed to decrypt FCM token", new DecryptException());
                    return EMPTY_STRING;
                }
            }
            return decFcmToken;
        }
        return mFcmToken;
    }

    public String getName() {
        return mName;
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

    public String getUUIDHash() {
        return mUUIDHash;
    }

    public long getLastCheckedTime() {
        return mLastCheckedTime;
    }

    public int getSeedIndex() {
        return mSeedIndex;
    }

    public void setSeedIndex(int seedIndex) {
        mSeedIndex = seedIndex;
    }

    @Nullable
    public String getCheckSum() {
        return mCheckSum;
    }

    public void setCheckSum(String checkSum) {
        mCheckSum = checkSum;
    }

    public int getRetryTimes() {
        requireStatus(BACKUP_TARGET_STATUS_REQUEST);
        if (mRetryTimes == null) {
            return INIT_RETRY_TIMES;
        }
        return mRetryTimes;
    }

    public long getRetryWaitStartTime() {
        requireStatus(BACKUP_TARGET_STATUS_REQUEST);
        if (mRetryWaitStartTime == null) {
            return INIT_RETRY_WAIT_START_TIME;
        }
        return mRetryWaitStartTime;
    }

    @Nullable
    public String getPhoneNumber() {
        requireStatus(BACKUP_TARGET_STATUS_REQUEST);
        if (mIsEncrypted) {
            LogUtil.logError(TAG, "Wrong encrypt state");
            if (TextUtils.isEmpty(mPhoneNumber)) {
                return mPhoneNumber;
            }
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
            String decPhoneNumber = genericCipherUtil.decryptData(mPhoneNumber);
            if (TextUtils.isEmpty(decPhoneNumber)) {
                LogUtil.logError(TAG, "Failed to decrypt phone number", new DecryptException());
            }
            return decPhoneNumber;
        }
        return mPhoneNumber;
    }

    @Nullable
    public String getPhoneModel() {
        requireStatus(BACKUP_TARGET_STATUS_REQUEST);
        return mPhoneModel;
    }

    @Nullable
    public String getPinCode() {
        requireStatus(BACKUP_TARGET_STATUS_REQUEST);
        if (mIsEncrypted) {
            LogUtil.logError(TAG, "Wrong encrypt state");
            if (TextUtils.isEmpty(mPinCode)) {
                return mPinCode;
            }
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
            String decPinCode = genericCipherUtil.decryptData(mPinCode);
            if (TextUtils.isEmpty(decPinCode)) {
                LogUtil.logError(TAG, "Failed to decrypt PIN code", new DecryptException());
            }
            return decPinCode;
        }
        return mPinCode;
    }

    @Nullable
    public String getSeed() {
        return mSeed;
    }

    public void setSeed(String seed) {
        mSeed = seed;
    }

    public boolean isSeeded() {
        switch (mStatus) {
            case BACKUP_TARGET_STATUS_REQUEST:
                return mSeedIndex != UNDEFINED_SEED_INDEX;
            case BACKUP_TARGET_STATUS_OK:
            case BACKUP_TARGET_STATUS_BAD:
            case BACKUP_TARGET_STATUS_NO_RESPONSE:
                return true;
            default:
                throw new IllegalStateException("Incorrect status");
        }
    }

    public boolean isEncrypted() {
        return mIsEncrypted;
    }

    public boolean compareStatus(@BackupTargetStatusType int status) {
        return mStatus == status;
    }

    public boolean comparePinCode(String pinCode) {
        if (TextUtils.isEmpty(pinCode)) {
            LogUtil.logDebug(TAG, "pinCode is empty or null");
        }
        if (compareStatus(BACKUP_TARGET_STATUS_REQUEST) && isSeeded()) {
            if (mIsEncrypted) {
                // AES
                GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
                String decryptedPinCode = genericCipherUtil.decryptData(mPinCode);
                if (TextUtils.isEmpty(decryptedPinCode)) {
                    LogUtil.logError(TAG, "Failed to decrypt PIN code", new DecryptException());
                    return false;
                }
                return decryptedPinCode.equals(pinCode);
            }
            return mPinCode.equals(pinCode);
        } else {
            return false;
        }
    }

    public boolean compareFcmToken(String fcmToken) {
        if (TextUtils.isEmpty(fcmToken)) {
            LogUtil.logDebug(TAG, "fcmToken is empty or null");
        }
        if (mIsEncrypted) {
            // AES
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
            String decryptedFcmToken = genericCipherUtil.decryptData(mFcmToken);
            if (TextUtils.isEmpty(decryptedFcmToken)) {
                LogUtil.logError(TAG, "Failed to decrypt FCM token", new DecryptException());
                return false;
            }
            return decryptedFcmToken.equals(fcmToken);
        }
        return mFcmToken.equals(fcmToken);
    }

    public boolean compareUUIDHash(String uuidHash) {
        if (TextUtils.isEmpty(mUUIDHash)) {
            LogUtil.logDebug(TAG, "uuidHash is empty or null");
            return false;
        }
        return mUUIDHash.equals(uuidHash);
    }

    public void updateLastCheckedTime() {
        mLastCheckedTime = System.currentTimeMillis();
    }

    public void updateStatusToBad() {
        switch (mStatus) {
            case BACKUP_TARGET_STATUS_REQUEST:
                if (!isSeeded()) {
                    LogUtil.logError(
                            TAG, "incorrect status", new IllegalStateException("incorrect status"));
                    break;
                }
                break;
            case BACKUP_TARGET_STATUS_OK:
            case BACKUP_TARGET_STATUS_NO_RESPONSE:
                break;
            case BACKUP_TARGET_STATUS_BAD:
                LogUtil.logError(
                        TAG, "incorrect status", new IllegalStateException("incorrect status"));
                break;
        }
        mStatus = BACKUP_TARGET_STATUS_BAD;
        mFcmToken = null;
        mUUIDHash = null;
        mPublicKey = null;
        mRetryTimes = null;
        mRetryWaitStartTime = null;
        mPhoneNumber = null;
        mPhoneModel = null;
        mPinCode = null;
        mSeed = null;
    }

    public void updateStatusToOK() {
        switch (mStatus) {
            case BACKUP_TARGET_STATUS_REQUEST:
                if (!isSeeded()) {
                    LogUtil.logError(
                            TAG, "incorrect status", new IllegalStateException("incorrect status"));
                    break;
                }
                mStatus = BACKUP_TARGET_STATUS_OK;
                mRetryTimes = null;
                mRetryWaitStartTime = null;
                mPhoneNumber = null;
                mPhoneModel = null;
                mPinCode = null;
                mSeed = null;
                break;
            case BACKUP_TARGET_STATUS_OK:
            case BACKUP_TARGET_STATUS_NO_RESPONSE:
            case BACKUP_TARGET_STATUS_BAD:
                mStatus = BACKUP_TARGET_STATUS_OK;
                break;
        }
    }

    public void updateStatusToNoResponse() {
        if (compareStatus(BACKUP_TARGET_STATUS_OK)) {
            mStatus = BACKUP_TARGET_STATUS_NO_RESPONSE;
        } else {
            LogUtil.logError(
                    TAG, "incorrect status", new IllegalStateException("incorrect status"));
        }
    }

    private void requireStatus(@BackupTargetStatusType int status) {
        if (mStatus != status) {
            LogUtil.logError(
                    TAG, "Incorrect status", new IllegalStateException("Incorrect status"));
        }
    }

    public void increaseRetryTimes() {
        requireStatus(BACKUP_TARGET_STATUS_REQUEST);
        synchronized (mLock) {
            mRetryTimes++;
            mLastCheckedTime = System.currentTimeMillis();
            if (mRetryTimes % RetryUtil.MAXIMUM_TRY_NUMBER == 0) {
                mRetryWaitStartTime = mLastCheckedTime;
                String pinCode = PinCodeUtil.newPinCode();
                if (mIsEncrypted) {
                    LogUtil.logError(TAG, "increase RetryTimes at wrong encrypted state");
                    // AES
                    GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
                    // Encrypt AES
                    String encPinCode = genericCipherUtil.encryptData(mPinCode);
                    if (TextUtils.isEmpty(encPinCode)) {
                        LogUtil.logError(TAG, "Failed to encrypt Pin Code", new EncryptException());
                        mPinCode = pinCode;
                    }
                    mPinCode = encPinCode;
                } else {
                    mPinCode = pinCode;
                }
            }
        }
    }

    public static final class Builder {

        // General
        private int mStatus;
        private String mFcmToken = null;
        private String mName = null;
        private String mPublicKey = null;
        private String mUUIDHash = null;
        private long mLastCheckedTime = UNDEFINED_LAST_CHECKED_TIME;
        private int mSeedIndex = UNDEFINED_SEED_INDEX; // Request may have
        private String mCheckSum = null;

        // Status: Request
        private Integer mRetryTimes = null; // Not Check
        private Long mRetryWaitStartTime = null; // Not Check
        private String mPhoneNumber = null;
        private String mPhoneModel = null;
        private String mPinCode = null;
        private String mSeed = null;

        public Builder(@BackupTargetStatusType int status) {
            mStatus = status;
        }

        public Builder setFcmToken(String fcmToken) {
            mFcmToken = fcmToken;
            return this;
        }

        public Builder setName(String name) {
            mName = name;
            return this;
        }

        public Builder setPublicKey(String publicKey) {
            mPublicKey = publicKey;
            return this;
        }

        public Builder setUUIDHash(String uuidHash) {
            mUUIDHash = uuidHash;
            return this;
        }

        public Builder setLastCheckedTime(long lastCheckedTime) {
            mLastCheckedTime = lastCheckedTime;
            return this;
        }

        public Builder setSeedIndex(int seedIndex) {
            mSeedIndex = seedIndex;
            return this;
        }

        public Builder setCheckSum(String checkSum) {
            mCheckSum = checkSum;
            return this;
        }

        public Builder setPhoneNumber(String phoneNumber) {
            if (mStatus != BACKUP_TARGET_STATUS_REQUEST) {
                LogUtil.logError(TAG, "Incorrect status");
                return this;
            }
            mPhoneNumber = phoneNumber;
            return this;
        }

        public Builder setPhoneModel(String phoneModel) {
            if (mStatus != BACKUP_TARGET_STATUS_REQUEST) {
                LogUtil.logError(TAG, "Incorrect status");
                return this;
            }
            mPhoneModel = phoneModel;
            return this;
        }

        public Builder setPinCode(String pinCode) {
            if (mStatus != BACKUP_TARGET_STATUS_REQUEST) {
                LogUtil.logError(TAG, "Incorrect status");
                return this;
            }
            mPinCode = pinCode;
            return this;
        }

        public Builder setSeed(String seed) {
            if (mStatus != BACKUP_TARGET_STATUS_REQUEST) {
                LogUtil.logError(TAG, "Incorrect status");
                return this;
            }
            mSeed = seed;
            return this;
        }

        public BackupTarget build() {
            requireNotEmpty(mName, "name is null or empty");
            if (mLastCheckedTime == UNDEFINED_LAST_CHECKED_TIME || mLastCheckedTime < 0) {
                throw new IllegalArgumentException("lastCheckedTime is incorrect or undefined");
            }

            boolean isSeeded =
                    (mStatus == BACKUP_TARGET_STATUS_OK)
                            || (mStatus == BACKUP_TARGET_STATUS_BAD)
                            || (mStatus == BACKUP_TARGET_STATUS_NO_RESPONSE)
                            || (mSeedIndex != UNDEFINED_SEED_INDEX)
                            || (!TextUtils.isEmpty(mCheckSum));
            if (isSeeded) {
                if (mSeedIndex == UNDEFINED_SEED_INDEX || mSeedIndex < 0) {
                    throw new IllegalArgumentException("seedIndex is incorrect or undefined");
                }
                requireNotEmpty(mCheckSum, "checkSum is null or empty");
            }

            switch (mStatus) {
                case BACKUP_TARGET_STATUS_REQUEST:
                    mRetryTimes = INIT_RETRY_TIMES;
                    mRetryWaitStartTime = INIT_RETRY_WAIT_START_TIME;
                    // phoneNumber might be null
                    // phoneModel might be null
                    if (isSeeded) {
                        requireNotEmpty(mPinCode, "pinCode is null or empty");
                        requireNotEmpty(mSeed, "seed is null or empty");
                    }
                    break;
                case BACKUP_TARGET_STATUS_OK:
                case BACKUP_TARGET_STATUS_BAD:
                case BACKUP_TARGET_STATUS_NO_RESPONSE:
                    break;
                default:
                    throw new IllegalArgumentException("status incorrect");
            }

            return new BackupTarget(
                    STORAGE_VERSION,
                    mStatus,
                    mFcmToken,
                    mName,
                    mPublicKey,
                    mUUIDHash,
                    mLastCheckedTime,
                    mSeedIndex,
                    mCheckSum,
                    mRetryTimes,
                    mRetryWaitStartTime,
                    mPhoneNumber,
                    mPhoneModel,
                    mPinCode,
                    mSeed);
        }

        private void requireNotEmpty(@Nullable CharSequence str, String message) {
            if (TextUtils.isEmpty(str)) {
                LogUtil.logError(TAG, message);
            }
        }
    }
}
