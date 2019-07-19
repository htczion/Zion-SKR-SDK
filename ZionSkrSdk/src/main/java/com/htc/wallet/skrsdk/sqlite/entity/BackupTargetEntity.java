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
import com.htc.wallet.skrsdk.error.EncryptException;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PinCodeUtil;
import com.htc.wallet.skrsdk.util.RetryUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

@Entity(tableName = "backupTarget")
public class BackupTargetEntity implements DataEntity {
    public static final String TAG = "BackupTargetEntity";
    @Ignore
    public static final int BACKUP_TARGET_STATUS_REQUEST = 0;
    @Ignore
    public static final int BACKUP_TARGET_STATUS_OK = 1;
    @Ignore
    public static final int BACKUP_TARGET_STATUS_BAD = 2;
    @Ignore
    public static final int BACKUP_TARGET_STATUS_NO_RESPONSE = 3;
    @Ignore
    public static final int BACKUP_TARGET_STATUS_REQUEST_WAIT_OK = 10;
    @Ignore
    public static final long UNDEFINED_LAST_CHECKED_TIME = -1L;
    @Ignore
    public static final int UNDEFINED_SEED_INDEX = -1;
    @Ignore
    private static final int INIT_RETRY_TIMES = 0;
    @Ignore
    private static final long INIT_RETRY_WAIT_START_TIME = 0;
    @Ignore
    private static final String EMPTY_STRING = "";
    @Ignore
    private final Object mLock = new Object();
    @Ignore
    private volatile boolean mIsEncrypted = false;

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "version")
    private int mVersion = BACKUP_DATA_CURRENT_VERSION;

    @ColumnInfo(name = "status")
    private volatile int mStatus;

    @ColumnInfo(name = "fcmToken")
    private volatile String mFcmToken; // AES

    @ColumnInfo(name = "whisperPub")
    private volatile String mWhisperPub;

    @ColumnInfo(name = "pushyToken")
    private volatile String mPushyToken;

    @ColumnInfo(name = "name")
    private volatile String mName;

    @ColumnInfo(name = "publicKey")
    private volatile String mPublicKey;

    @NonNull
    @ColumnInfo(name = "uuidHash")
    private volatile String mUUIDHash;

    @ColumnInfo(name = "lastCheckedTime")
    private volatile long mLastCheckedTime = UNDEFINED_LAST_CHECKED_TIME;

    @ColumnInfo(name = "seedIndex")
    private volatile int mSeedIndex = UNDEFINED_SEED_INDEX;

    @ColumnInfo(name = "checkSum")
    private volatile String mCheckSum;

    @ColumnInfo(name = "retryTimes")
    private volatile int mRetryTimes = INIT_RETRY_TIMES;

    @ColumnInfo(name = "retryWaitStartTime")
    private volatile long mRetryWaitStartTime = INIT_RETRY_WAIT_START_TIME;

    @ColumnInfo(name = "phoneNumber")
    private volatile String mPhoneNumber;

    @ColumnInfo(name = "phoneModel")
    private volatile String mPhoneModel;

    @ColumnInfo(name = "pinCode")
    private volatile String mPinCode = EMPTY_STRING;

    @ColumnInfo(name = "seed")
    private volatile String mSeed;

    @ColumnInfo(name = "encCodePk")
    private volatile String mEncCodePk;

    @ColumnInfo(name = "encCodePkSign")
    private volatile String mEncCodePkSign;

    @ColumnInfo(name = "encAseKey")
    private volatile String encAseKey;

    @ColumnInfo(name = "encAseKeySign")
    private volatile String encAseKeySign;

    public BackupTargetEntity() {
    }

    public BackupTargetEntity(@NonNull BackupTargetEntity target) {
        Objects.requireNonNull(target, "backupTargetEntity is null");
        synchronized (mLock) {
            id = target.id;
            mVersion = target.mVersion;
            mStatus = target.mStatus;
            mFcmToken = target.mFcmToken;
            mWhisperPub = target.mWhisperPub;
            mPushyToken = target.mPushyToken;
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
            mEncCodePk = target.mEncCodePk;
            mEncCodePkSign = target.mEncCodePkSign;
            encAseKey = target.encAseKey;
            encAseKeySign = target.encAseKeySign;
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getPublicKey() {
        return mPublicKey;
    }

    public void setPublicKey(String publicKey) {
        mPublicKey = publicKey;
    }

    public String getUUIDHash() {
        return mUUIDHash;
    }

    public void setUUIDHash(String UUIDHash) {
        mUUIDHash = UUIDHash;
    }

    public long getLastCheckedTime() {
        return mLastCheckedTime;
    }

    public void setLastCheckedTime(long lastCheckedTime) {
        mLastCheckedTime = lastCheckedTime;
    }

    public int getSeedIndex() {
        return mSeedIndex;
    }

    public void setSeedIndex(int seedIndex) {
        mSeedIndex = seedIndex;
    }

    public String getCheckSum() {
        return mCheckSum;
    }

    public void setCheckSum(String checkSum) {
        mCheckSum = checkSum;
    }

    public int getRetryTimes() {
        return mRetryTimes;
    }

    public void setRetryTimes(Integer retryTimes) {
        mRetryTimes = retryTimes;
    }

    public long getRetryWaitStartTime() {
        return mRetryWaitStartTime;
    }

    public void setRetryWaitStartTime(Long retryWaitStartTime) {
        mRetryWaitStartTime = retryWaitStartTime;
    }

    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        mPhoneNumber = phoneNumber;
    }

    public String getPhoneModel() {
        return mPhoneModel;
    }

    public void setPhoneModel(String phoneModel) {
        mPhoneModel = phoneModel;
    }

    public String getPinCode() {
        return mPinCode;
    }

    public void setPinCode(String pinCode) {
        mPinCode = pinCode;
    }

    @Deprecated
    public String getSeed() {
        return mSeed;
    }

    @Deprecated
    public void setSeed(String seed) {
        mSeed = seed;
    }

    public void setEncCodePk(String encCodePk) {
        mEncCodePk = encCodePk;
    }

    public String getEncCodePk() {
        return mEncCodePk;
    }

    public void setEncCodePkSign(String encCodePkSign) {
        mEncCodePkSign = encCodePkSign;
    }

    public String getEncCodePkSign() {
        return mEncCodePkSign;
    }

    public void setEncAseKey(String encAseKey) {
        this.encAseKey = encAseKey;
    }

    public String getEncAseKey() {
        return encAseKey;
    }

    public void setEncAseKeySign(String encAseKeySign) {
        this.encAseKeySign = encAseKeySign;
    }

    public String getEncAseKeySign() {
        return encAseKeySign;
    }

    @WorkerThread
    @Override
    public void encrypt() {
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
            String encFcmToken = null;
            if (!TextUtils.isEmpty(mFcmToken)) {
                encFcmToken = genericCipherUtil.encryptData(mFcmToken);
                if (TextUtils.isEmpty(encFcmToken)) {
                    LogUtil.logError(TAG, "Failed to encrypt FCM token");
                    return;
                }
            }
            // Encrypt AES Whisper Public Key and Pushy Token
            String encWhisperPub = null;
            String encPushyToken = null;
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
            String encPublicKey = null;
            if (!TextUtils.isEmpty(mPublicKey)) {
                encPublicKey = genericCipherUtil.encryptData(mPublicKey);
                if (TextUtils.isEmpty(encPublicKey)) {
                    LogUtil.logError(TAG, "Failed to encrypt Public Key");
                    return;
                }
            }

            // Request Member
            String encPhoneNumber = null;
            String encPinCode = null;
            if (compareStatus(BACKUP_TARGET_STATUS_REQUEST)
                    || compareStatus(BACKUP_TARGET_STATUS_REQUEST_WAIT_OK)) {
                // Encrypt AES Phone Number
                if (!TextUtils.isEmpty(mPhoneNumber)) {
                    encPhoneNumber = genericCipherUtil.encryptData(mPhoneNumber);
                    if (TextUtils.isEmpty(encPhoneNumber)) {
                        LogUtil.logError(TAG, "Failed to encrypt phone number");
                        return;
                    }
                }
                // Encrypt AES Pin Code
                if (isSeeded()) {
                    if (!TextUtils.isEmpty(mPinCode)) {
                        encPinCode = genericCipherUtil.encryptData(mPinCode);
                        if (TextUtils.isEmpty(encPinCode)) {
                            LogUtil.logError(TAG, "Failed to encrypt PIN code");
                            return;
                        }
                    }
                }
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
            // Update
            mIsEncrypted = true;
            setFcmToken(encFcmToken);
            setWhisperPub(encWhisperPub);
            setPushyToken(encPushyToken);
            setPublicKey(encPublicKey);
            setPhoneNumber(encPhoneNumber);
            setPinCode(encPinCode);
            setName(encName);
        }
    }

    @WorkerThread
    @Override
    public void decrypt() {
        synchronized (mLock) {
            // AES
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
            // Decrypt AES FCM Token
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
            String decPublicKey = null;
            if (!TextUtils.isEmpty(mPublicKey)) {
                decPublicKey = genericCipherUtil.decryptData(mPublicKey);
                if (TextUtils.isEmpty(decPublicKey)) {
                    LogUtil.logError(TAG, "Failed to decrypt Public Key");
                    return;
                }
            }

            // Request Member
            String decPhoneNumber = null;
            String decPinCode = null;
            if (compareStatus(BACKUP_TARGET_STATUS_REQUEST)
                    || compareStatus(BACKUP_TARGET_STATUS_REQUEST_WAIT_OK)) {
                // Decrypt AES Phone Number
                if (!TextUtils.isEmpty(mPhoneNumber)) {
                    decPhoneNumber = genericCipherUtil.decryptData(mPhoneNumber);
                    if (TextUtils.isEmpty(decPhoneNumber)) {
                        LogUtil.logError(TAG, "Failed to decrypt phone number");
                        return;
                    }
                }
                // Decrypt AES Pin Code
                if (isSeeded()) {
                    if (!TextUtils.isEmpty(mPinCode)) {
                        decPinCode = genericCipherUtil.decryptData(mPinCode);
                        if (TextUtils.isEmpty(decPinCode)) {
                            LogUtil.logError(TAG, "Failed to decrypt PIN code");
                            return;
                        }
                    }
                }
            }
            // Update
            mIsEncrypted = false;
            setFcmToken(decFcmToken);
            setWhisperPub(decWhisperPub);
            setPushyToken(decPushyToken);
            setPublicKey(decPublicKey);
            setPhoneNumber(decPhoneNumber);
            setPinCode(decPinCode);

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
                setName(decName);
            }
        }
    }

    public boolean isSeeded() {
        switch (mStatus) {
            case BACKUP_TARGET_STATUS_REQUEST:
                return mSeedIndex != UNDEFINED_SEED_INDEX;
            case BACKUP_TARGET_STATUS_OK:
            case BACKUP_TARGET_STATUS_BAD:
            case BACKUP_TARGET_STATUS_NO_RESPONSE:
            case BACKUP_TARGET_STATUS_REQUEST_WAIT_OK:
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

    public boolean isStatusPending() {
        return mStatus == BACKUP_TARGET_STATUS_REQUEST
                || mStatus == BACKUP_TARGET_STATUS_REQUEST_WAIT_OK;
    }

    public void updateLastCheckedTime() {
        mLastCheckedTime = System.currentTimeMillis();
    }

    public void updateStatusToBad() {
        switch (mStatus) {
            case BACKUP_TARGET_STATUS_REQUEST:
            case BACKUP_TARGET_STATUS_REQUEST_WAIT_OK:
                if (!isSeeded()) {
                    LogUtil.logError(
                            TAG, "incorrect status", new IllegalStateException("incorrect status"));
                    break;
                }
                mStatus = BACKUP_TARGET_STATUS_BAD;
                mRetryTimes = 0;
                mRetryWaitStartTime = 0L;
                mPhoneNumber = null;
                mPhoneModel = null;
                mPinCode = null;
                mSeed = null;
                break;
            case BACKUP_TARGET_STATUS_OK:
            case BACKUP_TARGET_STATUS_NO_RESPONSE:
                mStatus = BACKUP_TARGET_STATUS_BAD;
                break;
            case BACKUP_TARGET_STATUS_BAD:
                mStatus = BACKUP_TARGET_STATUS_BAD;
                // Issue fixed, by health monitor procedure, bad status may update from bad status
                LogUtil.logInfo(TAG, "Update status to bad from bad");
                break;
        }
    }

    public void updateStatusToNoResponse() {
        if (compareStatus(BACKUP_TARGET_STATUS_OK)) {
            mStatus = BACKUP_TARGET_STATUS_NO_RESPONSE;
        } else if (compareStatus(BACKUP_TARGET_STATUS_NO_RESPONSE)) {
            LogUtil.logInfo(TAG, "update status to no response with no response status");
        } else {
            LogUtil.logError(
                    TAG, "incorrect status", new IllegalStateException("incorrect status"));
        }
    }

    public void increaseRetryTimes() {
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

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            BACKUP_TARGET_STATUS_REQUEST,
            BACKUP_TARGET_STATUS_OK,
            BACKUP_TARGET_STATUS_BAD,
            BACKUP_TARGET_STATUS_NO_RESPONSE,
            BACKUP_TARGET_STATUS_REQUEST_WAIT_OK
    })
    public @interface BackupTargetStatusType {
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
