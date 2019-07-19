package com.htc.wallet.skrsdk.deprecated.sharedprefs;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.crypto.util.GenericCipherUtil;
import com.htc.wallet.skrsdk.error.DecryptException;
import com.htc.wallet.skrsdk.error.EncryptException;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PinCodeUtil;

import java.util.Objects;

public class RestoreTarget extends StorageBase {
    private static final String TAG = "RestoreTarget";

    public static final long UNDEFINED_TIME_STAMP = -1L;
    public static final int UNDEFINED_RETRY_TIMES = -1;
    private static final String EMPTY_STRING = "";

    // Util
    private final Object mLock = new Object();
    private volatile boolean mIsEncrypted = false;

    // Member
    private final String mEmailHash;
    private final String mUUIDHash;
    @Nullable
    private final String mBackupUUIDHash;
    private volatile String mFcmToken; // AES
    private volatile String mPublicKey; // AES
    private final String mName;
    private final long mTimeStamp;
    private volatile int mRetryTimes;
    private volatile String mPinCode; // AES
    @Nullable
    private final String mPhoneModel; // Nullable

    @Override
    boolean upgrade() {
        if (mVersion == STORAGE_VERSION) {
            return false;
        }
        // Do upgrade here
        return true;
    }

    private RestoreTarget(
            int version,
            String emailHash,
            String uuidHash,
            @Nullable String backupUUIDHash,
            String fcmToken,
            String publicKey,
            String name,
            long timeStamp,
            int retryTimes,
            String pinCode,
            @Nullable String phoneModel) {
        mVersion = version;
        mEmailHash = emailHash;
        mUUIDHash = uuidHash;
        mBackupUUIDHash = backupUUIDHash;
        mFcmToken = fcmToken;
        mPublicKey = publicKey;
        mName = name;
        mTimeStamp = timeStamp;
        mRetryTimes = retryTimes;
        mPinCode = pinCode;
        mPhoneModel = phoneModel;
    }

    RestoreTarget(@NonNull RestoreTarget target) {
        Objects.requireNonNull(target, "restoreTarget is null");
        synchronized (mLock) {
            mVersion = target.mVersion;
            mIsEncrypted = target.mIsEncrypted;
            mEmailHash = target.mEmailHash;
            mUUIDHash = target.mUUIDHash;
            mBackupUUIDHash = target.mBackupUUIDHash;
            mFcmToken = target.mFcmToken;
            mPublicKey = target.mPublicKey;
            mName = target.mName;
            mTimeStamp = target.mTimeStamp;
            mRetryTimes = target.mRetryTimes;
            mPinCode = target.mPinCode;
            mPhoneModel = target.mPhoneModel;
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
                LogUtil.logError(TAG, "Failed to encrypt Fcm Token", new EncryptException());
                return;
            }
            String encPublicKey = genericCipherUtil.encryptData(mPublicKey);
            if (TextUtils.isEmpty(encPublicKey)) {
                LogUtil.logError(TAG, "Failed to encrypt Public Key", new EncryptException());
                return;
            }
            String encPinCode = genericCipherUtil.encryptData(mPinCode);
            if (TextUtils.isEmpty(encPinCode)) {
                LogUtil.logError(TAG, "Failed to encrypt Pin Code", new EncryptException());
                return;
            }

            // Assign encrypt result to variable
            mIsEncrypted = true;
            mFcmToken = encFcmToken;
            mPublicKey = encPublicKey;
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
            // Decrypt AES
            String decFcmToken = genericCipherUtil.decryptData(mFcmToken);
            if (TextUtils.isEmpty(decFcmToken)) {
                LogUtil.logError(TAG, "Failed to decrypt Fcm Token", new DecryptException());
                return;
            }
            String decPublicKey = genericCipherUtil.decryptData(mPublicKey);
            if (TextUtils.isEmpty(decPublicKey)) {
                LogUtil.logError(TAG, "Failed to decrypt Public Key", new DecryptException());
                return;
            }
            String decPinCode = genericCipherUtil.decryptData(mPinCode);
            if (TextUtils.isEmpty(decPinCode)) {
                LogUtil.logError(TAG, "Failed to decrypt Pin Code", new DecryptException());
                return;
            }

            // Assign decrypt result to variable
            mIsEncrypted = false;
            mFcmToken = decFcmToken;
            mPublicKey = decPublicKey;
            mPinCode = decPinCode;
        }
    }

    public String getEmailHash() {
        return mEmailHash;
    }

    public String getUUIDHash() {
        return mUUIDHash;
    }

    @Nullable
    public String getBackupUUIDHash() {
        return mBackupUUIDHash;
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

    public String getName() {
        return mName;
    }

    public long getTimeStamp() {
        return mTimeStamp;
    }

    public int getRetryTimes() {
        return mRetryTimes;
    }

    public String getPinCode() {
        if (mIsEncrypted) {
            LogUtil.logError(TAG, "Wrong encrypt state");
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
            String decPinCode = genericCipherUtil.decryptData(mPinCode);
            if (TextUtils.isEmpty(decPinCode)) {
                LogUtil.logError(TAG, "Failed to decrypt Pin code", new DecryptException());
            }
            return decPinCode;
        }
        return mPinCode;
    }

    @Nullable
    public String getPhoneModel() {
        return mPhoneModel;
    }

    public void setRetryTimes(int retryTimes) {
        if (retryTimes < 0) {
            LogUtil.logError(TAG, "retryTimes incorrect");
            return;
        }

        if (mRetryTimes == retryTimes) {
            LogUtil.logDebug(TAG, "set the same retryTimes");
            return;
        }

        synchronized (mLock) {
            mRetryTimes = retryTimes;
        }
    }

    public void setPinCode(String pinCode) {
        if (!PinCodeUtil.isValidPinCode(pinCode)) {
            LogUtil.logError(
                    TAG, "PinCode incorrect", new IllegalStateException("PinCode incorrect"));
            return;
        }
        mPinCode = pinCode;
    }

    public boolean isEncrypted() {
        return mIsEncrypted;
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

    public boolean comparePinCode(String pinCode) {
        if (TextUtils.isEmpty(pinCode)) {
            LogUtil.logDebug(TAG, "pinCode is empty or null");
        }
        if (mIsEncrypted) {
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
            String decPinCode = genericCipherUtil.decryptData(mPinCode);
            if (TextUtils.isEmpty(decPinCode)) {
                LogUtil.logError(TAG, "Failed to decrypt pinCode", new DecryptException());
                return false;
            }
            return decPinCode.equals(pinCode);
        }
        return mPinCode.equals(pinCode);
    }

    public static final class Builder {
        private String mEmailHash = null;
        private String mUUIDHash = null;
        private String mBackupUUIDHash = null;
        private String mFcmToken = null;
        private String mPublicKey = null;
        private String mName = null;
        private long mTimeStamp = UNDEFINED_TIME_STAMP;
        private int mRetryTimes = UNDEFINED_RETRY_TIMES;
        private String mPinCode = null;
        private String mPhoneModel = null;

        public Builder setEmailHash(String emailHash) {
            mEmailHash = emailHash;
            return this;
        }

        public Builder setUUIDHash(String UUIDHash) {
            mUUIDHash = UUIDHash;
            return this;
        }

        public Builder setBackupUUIDHash(String backupUUIDHash) {
            mBackupUUIDHash = backupUUIDHash;
            return this;
        }

        public Builder setFcmToken(String fcmToken) {
            mFcmToken = fcmToken;
            return this;
        }

        public Builder setPublicKey(String publicKey) {
            mPublicKey = publicKey;
            return this;
        }

        public Builder setName(String name) {
            mName = name;
            return this;
        }

        public Builder setTimeStamp(long timeStamp) {
            mTimeStamp = timeStamp;
            return this;
        }

        public Builder setRetryTimes(int retryTimes) {
            mRetryTimes = retryTimes;
            return this;
        }

        public Builder setPinCode(String pinCode) {
            mPinCode = pinCode;
            return this;
        }

        public Builder setPhoneModel(String phoneModel) {
            mPhoneModel = phoneModel;
            return this;
        }

        public RestoreTarget build() {
            requireNotEmpty(mEmailHash, "emailHash is null or empty");
            requireNotEmpty(mUUIDHash, "uuidHash is null or empty");
            if (TextUtils.isEmpty(mBackupUUIDHash)) {
                LogUtil.logError(TAG, "Backup UUID Hash is null or empty");
            }
            requireNotEmpty(mFcmToken, "fcm token is null or empty");
            requireNotEmpty(mPublicKey, "publicKey is null or empty");
            requireNotEmpty(mName, "name is null or empty");
            if (mTimeStamp == UNDEFINED_TIME_STAMP || mTimeStamp < 0) {
                throw new IllegalArgumentException("timeStamp is incorrect or undefined");
            }
            if (mRetryTimes == UNDEFINED_RETRY_TIMES || mRetryTimes < 0) {
                throw new IllegalArgumentException("retryTimes is incorrect or undefined");
            }
            requireNotEmpty(mPinCode, "pinCode is null or empty");
            return new RestoreTarget(
                    STORAGE_VERSION,
                    mEmailHash,
                    mUUIDHash,
                    mBackupUUIDHash,
                    mFcmToken,
                    mPublicKey,
                    mName,
                    mTimeStamp,
                    mRetryTimes,
                    mPinCode,
                    mPhoneModel);
        }

        private void requireNotEmpty(@Nullable CharSequence str, String message) {
            if (TextUtils.isEmpty(str)) {
                throw new IllegalArgumentException(message);
            }
        }
    }
}
