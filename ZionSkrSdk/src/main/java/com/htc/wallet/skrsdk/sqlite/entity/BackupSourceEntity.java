package com.htc.wallet.skrsdk.sqlite.entity;

import static com.htc.wallet.skrsdk.sqlite.util.BackupDataConstants.BACKUP_DATA_CLEAR_LEGACY_V1_VERSION;
import static com.htc.wallet.skrsdk.sqlite.util.BackupDataConstants.BACKUP_DATA_CURRENT_VERSION;
import static com.htc.wallet.skrsdk.sqlite.util.BackupDataConstants.BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.crypto.util.GenericCipherUtil;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

import javax.annotation.Nonnegative;

@Entity(tableName = "backupSource")
public class BackupSourceEntity implements DataEntity {
    public static final String TAG = "BackupSourceEntity";
    @Ignore
    public static final int BACKUP_SOURCE_STATUS_REQUEST = 0;
    @Ignore
    public static final int BACKUP_SOURCE_STATUS_OK = 1;
    @Ignore
    public static final int BACKUP_SOURCE_STATUS_DONE = 2;
    @Ignore
    public static final int BACKUP_SOURCE_STATUS_FULL = 3;

    @Ignore
    // After restore, wait new partial seed from Amy
    public static final int BACKUP_SOURCE_STATUS_REQUEST_AUTO_BACKUP = 4;

    @Ignore
    public static final long UNDEFINED_TIME_STAMP = -1L;
    @Ignore
    public static final int INIT_RETRY_TIMES = 0;
    @Ignore
    public static final long INIT_RETRY_WAIT_START_TIME = 0;
    @Ignore
    public static final long INIT_LAST_VERIFY_TIME = 0;
    @Ignore
    public static final int PIN_CODE_NO_ERROR = 0;
    @Ignore
    public static final int PIN_CODE_ERROR = 1;
    @Ignore
    public static final String INIT_PIN_CODE = "";
    @Ignore
    private final Object mLock = new Object();

    @ColumnInfo(name = "version")
    private volatile int mVersion = BACKUP_DATA_CURRENT_VERSION;

    @ColumnInfo(name = "status")
    private volatile int mStatus;

    @ColumnInfo(name = "emailHash")
    private volatile String mEmailHash;

    @ColumnInfo(name = "fcmToken")
    private volatile String mFcmToken;

    @ColumnInfo(name = "whisperPub")
    private volatile String mWhisperPub;

    @ColumnInfo(name = "pushyToken")
    private volatile String mPushyToken;

    @ColumnInfo(name = "name")
    private volatile String mName;

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "uuidHash")
    private volatile String mUUIDHash;

    @ColumnInfo(name = "tzIdHash")
    private volatile String mTzIdHash;

    @ColumnInfo(name = "isTest")
    private volatile boolean mIsTest;

    @ColumnInfo(name = "timeStamp")
    private volatile long mTimeStamp = UNDEFINED_TIME_STAMP;

    @ColumnInfo(name = "publicKey")
    private volatile String mPublicKey;

    @ColumnInfo(name = "myName")
    private volatile String mMyName;

    @ColumnInfo(name = "seed")
    private volatile String mSeed;

    @ColumnInfo(name = "checkSum")
    private volatile String mCheckSum;

    @ColumnInfo(name = "retryTimes")
    private volatile int mRetryTimes = INIT_RETRY_TIMES;

    @ColumnInfo(name = "retryWaitStartTime")
    private volatile long mRetryWaitStartTime = INIT_RETRY_WAIT_START_TIME;

    @ColumnInfo(name = "pinCode")
    private volatile String mPinCode = INIT_PIN_CODE;

    @ColumnInfo(name = "lastVerifyTime")
    private volatile long mLastVerifyTime = INIT_LAST_VERIFY_TIME;

    @ColumnInfo(name = "isPinCodeError")
    private volatile int mIsPinCodeError = PIN_CODE_NO_ERROR;

    @ColumnInfo(name = "lastRequestTime")
    private volatile long mLastRequestTime;

    public BackupSourceEntity() {
    }

    public BackupSourceEntity(BackupSourceEntity source) {
        Objects.requireNonNull(source, "backupSourceEntity is null");
        synchronized (mLock) {
            mVersion = source.mVersion;
            mStatus = source.mStatus;
            mEmailHash = source.mEmailHash;
            mFcmToken = source.mFcmToken;
            mWhisperPub = source.mWhisperPub;
            mPushyToken = source.mPushyToken;
            mName = source.mName;
            mUUIDHash = source.mUUIDHash;
            mTzIdHash = source.mTzIdHash;
            mIsTest = source.mIsTest;
            mTimeStamp = source.mTimeStamp;
            mPublicKey = source.mPublicKey;
            mMyName = source.mMyName;
            mSeed = source.mSeed;
            mCheckSum = source.mCheckSum;
            mRetryTimes = source.mRetryTimes;
            mRetryWaitStartTime = source.mRetryWaitStartTime;
            mPinCode = source.mPinCode;
            mLastVerifyTime = source.mLastVerifyTime;
            mIsPinCodeError = source.mIsPinCodeError;
            mLastRequestTime = source.mLastRequestTime;
        }
    }

    public int getVersion() {
        return mVersion;
    }

    public void setVersion(int version) {
        mVersion = version;
    }

    public int getStatus() {
        return mStatus;
    }

    public void setStatus(int status) {
        mStatus = status;
    }

    public String getEmailHash() {
        return mEmailHash;
    }

    public void setEmailHash(String emailHash) {
        mEmailHash = emailHash;
    }

    public String getFcmToken() {
        return mFcmToken;
    }

    public void setFcmToken(String fcmToken) {
        mFcmToken = fcmToken;
    }

    public String getWhisperPub() {
        return mWhisperPub;
    }

    public void setWhisperPub(String whisperPub) {
        mWhisperPub = whisperPub;
    }

    public String getPushyToken() {
        return mPushyToken;
    }

    public void setPushyToken(String pushyToken) {
        mPushyToken = pushyToken;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getUUIDHash() {
        return mUUIDHash;
    }

    public void setUUIDHash(String uuidHash) {
        mUUIDHash = uuidHash;
    }

    public String getTzIdHash() {
        return mTzIdHash;
    }

    public void setTzIdHash(String tzIdHash) {
        mTzIdHash = tzIdHash;
    }

    public boolean getIsTest() {
        return mIsTest;
    }

    public void setIsTest(boolean isTest) {
        mIsTest = isTest;
    }

    public long getTimeStamp() {
        return mTimeStamp;
    }

    public void setTimeStamp(@Nonnegative long timeStamp) {
        mTimeStamp = timeStamp;
    }

    public String getPublicKey() {
        return mPublicKey;
    }

    public void setPublicKey(String publicKey) {
        mPublicKey = publicKey;
    }

    public String getMyName() {
        return mMyName;
    }

    public void setMyName(String myName) {
        mMyName = myName;
    }

    public int getRetryTimes() {
        return mRetryTimes;
    }

    public void setRetryTimes(Integer retryTimes) {
        if (retryTimes < 0) {
            LogUtil.logError(TAG, "retryTimes incorrect");
            return;
        }
        mRetryTimes = retryTimes;
    }

    public long getRetryWaitStartTime() {
        return mRetryWaitStartTime;
    }

    public void setRetryWaitStartTime(Long retryWaitStartTime) {
        mRetryWaitStartTime = retryWaitStartTime;
    }

    public String getPinCode() {
        return mPinCode;
    }

    public void setPinCode(String pinCode) {
        mPinCode = pinCode;
    }

    public long getLastVerifyTime() {
        return mLastVerifyTime;
    }

    public void setLastVerifyTime(Long lastVerifyTime) {
        mLastVerifyTime = lastVerifyTime;
    }

    public int getIsPinCodeError() {
        return mIsPinCodeError;
    }

    public void setIsPinCodeError(Integer isPinCodeError) {
        mIsPinCodeError = isPinCodeError;
    }

    public long getLastRequestTime() {
        return mLastRequestTime;
    }

    public void setLastRequestTime(Long lastRequestTime) {
        mLastRequestTime = lastRequestTime;
    }

    public String getSeed() {
        return mSeed;
    }

    public void setSeed(String secretKey) {
        mSeed = secretKey;
    }

    public String getCheckSum() {
        return mCheckSum;
    }

    public void setCheckSum(String checkSum) {
        mCheckSum = checkSum;
    }

    @WorkerThread
    @Override
    public void encrypt() {
        synchronized (mLock) {
            // AES
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();

            String encFcmToken = null;
            String encWhisperPub = null;
            String encPushyToken = null;
            // Encrypt AES FCM token
            if (!TextUtils.isEmpty(mFcmToken)) {
                encFcmToken = genericCipherUtil.encryptData(mFcmToken);
                if (TextUtils.isEmpty(encFcmToken)) {
                    LogUtil.logError(TAG, "Failed to encrypt FCM token");
                    return;
                }
            }

            // Encrypt AES Whisper Public Key and Pushy Token
            if (!TextUtils.isEmpty(mWhisperPub) && !TextUtils.isEmpty(mPushyToken)) {
                encWhisperPub = genericCipherUtil.encryptData(mWhisperPub);
                if (TextUtils.isEmpty(encWhisperPub)) {
                    LogUtil.logError(TAG, "Failed to encrypt Whisper Public Key");
                    return;
                }

                encPushyToken = genericCipherUtil.encryptData(mPushyToken);
                if (TextUtils.isEmpty(encPushyToken)) {
                    LogUtil.logError(TAG, "Failed to encrypt Pushy Token");
                    return;
                }
            }

            // Encrypt AES Public Key
            String encPublicKey = genericCipherUtil.encryptData(mPublicKey);
            if (TextUtils.isEmpty(encPublicKey)) {
                LogUtil.logError(TAG, "Failed to encrypt Public Key");
                return;
            }
            // Encrypt AES name
            String encName = null;
            if (!TextUtils.isEmpty(mName)) {
                encName = genericCipherUtil.encryptData(mName);
                if (TextUtils.isEmpty(encName)) {
                    LogUtil.logError(TAG, "Failed to encrypt name");
                    return;
                }
            }
            // Encrypt AES tzIdHash
            String encTzIdHash = null;
            if (!TextUtils.isEmpty(mTzIdHash)) {
                encTzIdHash = genericCipherUtil.encryptData(mTzIdHash);
                if (TextUtils.isEmpty(encTzIdHash)) {
                    LogUtil.logError(TAG, "Failed to encrypt tzIdHash");
                    return;
                }
            }
            // Encrypt AES myName
            String encMyName = null;
            if (!TextUtils.isEmpty(mMyName)) {
                encMyName = genericCipherUtil.encryptData(mMyName);
                if (TextUtils.isEmpty(encMyName)) {
                    LogUtil.logError(TAG, "Failed to encrypt myName");
                    return;
                }
            }

            // Encrypt AES pinCode
            String encPinCode = null;
            if (!TextUtils.isEmpty(mPinCode)) {
                encPinCode = genericCipherUtil.encryptData(mPinCode);
                if (TextUtils.isEmpty(encPinCode)) {
                    LogUtil.logError(TAG, "Failed to encrypt pinCode");
                    return;
                }
            }
            setFcmToken(encFcmToken);
            setWhisperPub(encWhisperPub);
            setPushyToken(encPushyToken);
            setPublicKey(encPublicKey);
            setName(encName);
            setMyName(encMyName);
            setTzIdHash(encTzIdHash);
            setPinCode(encPinCode);
        }
    }

    @WorkerThread
    @Override
    public void decrypt() {
        synchronized (mLock) {
            // AES
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();

            // Decrypt AES FCM token
            String decFcmToken = null;
            if (!TextUtils.isEmpty(mFcmToken)) {
                decFcmToken = genericCipherUtil.decryptData(mFcmToken);
                if (TextUtils.isEmpty(decFcmToken)) {
                    LogUtil.logError(TAG, "Failed to decrypt FCM token");
                    return;
                }
            }
            // Decrypt AES Whisper Public Key and Pushy Token
            String decWhisperPub = null;
            String decPushyToken = null;
            if (!TextUtils.isEmpty(mWhisperPub) && !TextUtils.isEmpty(mPushyToken)) {
                decWhisperPub = genericCipherUtil.decryptData(mWhisperPub);
                if (TextUtils.isEmpty(decWhisperPub)) {
                    LogUtil.logError(TAG, "Failed to decrypt Whisper Public Key");
                    return;
                }

                decPushyToken = genericCipherUtil.decryptData(mPushyToken);
                if (TextUtils.isEmpty(decPushyToken)) {
                    LogUtil.logError(TAG, "Failed to decrypt Pushy Token");
                    return;
                }
            }

            // Decrypt AES Public Key
            String decPublicKey = genericCipherUtil.decryptData(mPublicKey);
            if (TextUtils.isEmpty(decPublicKey)) {
                LogUtil.logError(TAG, "Failed to decrypt Public Key");
                return;
            }

            setFcmToken(decFcmToken);
            setWhisperPub(decWhisperPub);
            setPushyToken(decPushyToken);
            setPublicKey(decPublicKey);

            if (isSensitiveDataEncrypted()) {
                // Decrypt AES name
                String decName = null;
                if (!TextUtils.isEmpty(mName)) {
                    decName = genericCipherUtil.decryptData(mName);
                    if (TextUtils.isEmpty(decName)) {
                        LogUtil.logError(TAG, "Failed to decrypt name");
                        return;
                    }
                }
                // Decrypt AES tzIdHash
                String decTzIdHash = null;
                if (!TextUtils.isEmpty(mTzIdHash)) {
                    decTzIdHash = genericCipherUtil.decryptData(mTzIdHash);
                    if (TextUtils.isEmpty(decTzIdHash)) {
                        LogUtil.logError(TAG, "Failed to decrypt tzIdHash");
                        return;
                    }
                }
                // Decrypt AES myName
                String decMyName = null;
                if (!TextUtils.isEmpty(mMyName)) {
                    decMyName = genericCipherUtil.decryptData(mMyName);
                    if (TextUtils.isEmpty(decMyName)) {
                        LogUtil.logError(TAG, "Failed to decrypt myName");
                        return;
                    }
                }

                // Decrypt AES pinCode
                String decPinCode = null;
                if (!TextUtils.isEmpty(mPinCode)) {
                    decPinCode = genericCipherUtil.decryptData(mPinCode);
                    if (TextUtils.isEmpty(decPinCode)) {
                        LogUtil.logError(TAG, "Failed to decrypt pinCode");
                        return;
                    }
                }
                setName(decName);
                setMyName(decMyName);
                setTzIdHash(decTzIdHash);
                setPinCode(decPinCode);
            }
        }
    }

    public boolean compareStatus(@BackupSourceStatusType int status) {
        return mStatus == status;
    }

    public void setDone() {
        mStatus = BACKUP_SOURCE_STATUS_DONE;
    }

    public void clearPinCode() {
        mPinCode = INIT_PIN_CODE;
        mLastVerifyTime = INIT_LAST_VERIFY_TIME;
        mIsPinCodeError = PIN_CODE_NO_ERROR;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            BACKUP_SOURCE_STATUS_REQUEST,
            BACKUP_SOURCE_STATUS_OK,
            BACKUP_SOURCE_STATUS_DONE,
            BACKUP_SOURCE_STATUS_FULL,
            BACKUP_SOURCE_STATUS_REQUEST_AUTO_BACKUP
    })
    public @interface BackupSourceStatusType {
    }

    public void updateStatusToFull() {
        if (mStatus == BACKUP_SOURCE_STATUS_REQUEST) {
            mStatus = BACKUP_SOURCE_STATUS_FULL;
        } else {
            LogUtil.logDebug(
                    TAG, "Can not update to BACKUP_SOURCE_STATUS_FULL if mStatus=" + mStatus);
        }
    }

    @Override
    public boolean isSensitiveDataEncrypted() {
        return getVersion() >= BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION;
    }

    @Override
    public boolean isLegacyDataUpdatedV1() {
        return getVersion() >= BACKUP_DATA_CLEAR_LEGACY_V1_VERSION;
    }
}
