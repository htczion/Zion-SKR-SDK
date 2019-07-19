package com.htc.wallet.skrsdk.sqlite.entity;

import static com.htc.wallet.skrsdk.sqlite.util.BackupDataConstants.BACKUP_DATA_CLEAR_LEGACY_V1_VERSION;
import static com.htc.wallet.skrsdk.sqlite.util.BackupDataConstants.BACKUP_DATA_CURRENT_VERSION;
import static com.htc.wallet.skrsdk.sqlite.util.BackupDataConstants.BACKUP_DATA_SENSITIVE_ENCRYPT_VERSION;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.crypto.util.GenericCipherUtil;
import com.htc.wallet.skrsdk.error.DecryptException;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.Objects;

import javax.annotation.Nonnegative;

@Entity(tableName = "restoreTarget")
public class RestoreTargetEntity implements DataEntity {
    public static final String TAG = "RestoreTargetEntity";
    @Ignore
    public static final long UNDEFINED_TIME_STAMP = -1L;
    @Ignore
    public static final int UNDEFINED_RETRY_TIMES = -1;
    @Ignore
    private static final String EMPTY_STRING = "";
    @Ignore
    private final Object mLock = new Object();

    @ColumnInfo(name = "version")
    private int mVersion = BACKUP_DATA_CURRENT_VERSION;

    @ColumnInfo(name = "emailHash")
    private String mEmailHash;

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "uuidHash")
    private volatile String mUUIDHash;

    @ColumnInfo(name = "backupUUIDHash")
    private volatile String mBackupUUIDHash;

    @ColumnInfo(name = "tzIdHash")
    private volatile String mTzIdHash;

    @ColumnInfo(name = "isTest")
    private volatile boolean mIsTest;

    @ColumnInfo(name = "fcmToken")
    private volatile String mFcmToken; // AES

    @ColumnInfo(name = "whisperPub")
    private volatile String mWhisperPub;

    @ColumnInfo(name = "pushyToken")
    private volatile String mPushyToken;

    @ColumnInfo(name = "publicKey")
    private volatile String mPublicKey; // AES

    @ColumnInfo(name = "name")
    private volatile String mName;

    @ColumnInfo(name = "timeStamp")
    private volatile long mTimeStamp = UNDEFINED_TIME_STAMP;

    @ColumnInfo(name = "retryTimes")
    private volatile int mRetryTimes = UNDEFINED_RETRY_TIMES;

    @ColumnInfo(name = "pinCode")
    private volatile String mPinCode = EMPTY_STRING; // AES

    @ColumnInfo(name = "phoneModel")
    private volatile String mPhoneModel;

    public RestoreTargetEntity() {
    }

    public RestoreTargetEntity(@NonNull RestoreTargetEntity target) {
        Objects.requireNonNull(target, "restoreTargetEntity is null");
        synchronized (mLock) {
            mVersion = target.mVersion;
            mEmailHash = target.mEmailHash;
            mUUIDHash = target.mUUIDHash;
            mBackupUUIDHash = target.mBackupUUIDHash;
            mTzIdHash = target.mTzIdHash;
            mIsTest = target.mIsTest;
            mFcmToken = target.mFcmToken;
            mWhisperPub = target.mWhisperPub;
            mPushyToken = target.mPushyToken;
            mPublicKey = target.mPublicKey;
            mName = target.mName;
            mTimeStamp = target.mTimeStamp;
            mRetryTimes = target.mRetryTimes;
            mPinCode = target.mPinCode;
            mPhoneModel = target.mPhoneModel;
        }
    }

    public int getVersion() {
        return mVersion;
    }

    public void setVersion(int version) {
        mVersion = version;
    }

    public String getEmailHash() {
        return mEmailHash;
    }

    public void setEmailHash(String emailHash) {
        mEmailHash = emailHash;
    }

    public String getUUIDHash() {
        return mUUIDHash;
    }

    public void setUUIDHash(String UUIDHash) {
        mUUIDHash = UUIDHash;
    }

    public String getBackupUUIDHash() {
        return mBackupUUIDHash;
    }

    public void setBackupUUIDHash(String backupUUIDHash) {
        mBackupUUIDHash = backupUUIDHash;
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

    public String getPublicKey() {
        return mPublicKey;
    }

    public void setPublicKey(String publicKey) {
        mPublicKey = publicKey;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public long getTimeStamp() {
        return mTimeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        mTimeStamp = timeStamp;
    }

    public int getRetryTimes() {
        return mRetryTimes;
    }

    public void setRetryTimes(@Nonnegative int retryTimes) {
        if (retryTimes < 0) {
            LogUtil.logError(TAG, "retryTimes incorrect");
            return;
        }

        if (mRetryTimes == retryTimes) {
            LogUtil.logDebug(TAG, "set the same retryTimes");
            return;
        }
        mRetryTimes = retryTimes;
    }

    public String getPinCode() {
        return mPinCode;
    }

    public void setPinCode(String pinCode) {
        mPinCode = pinCode;
    }

    @Nullable
    public String getPhoneModel() {
        return mPhoneModel;
    }

    public void setPhoneModel(String phoneModel) {
        mPhoneModel = phoneModel;
    }

    @WorkerThread
    @Override
    public void encrypt() {
        synchronized (mLock) {
            // AES
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
            // Encrypt AES
            String encFcmToken = null;
            if (!TextUtils.isEmpty(mFcmToken)) {
                encFcmToken = genericCipherUtil.encryptData(mFcmToken);
                if (TextUtils.isEmpty(encFcmToken)) {
                    LogUtil.logError(TAG, "Failed to encrypt Fcm Token");
                    return;
                }
            }

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

            String encPublicKey = genericCipherUtil.encryptData(mPublicKey);
            if (TextUtils.isEmpty(encPublicKey)) {
                LogUtil.logError(TAG, "Failed to encrypt Public Key");
                return;
            }
            String encPinCode = genericCipherUtil.encryptData(mPinCode);
            if (TextUtils.isEmpty(encPinCode)) {
                LogUtil.logError(TAG, "Failed to encrypt Pin Code");
                return;
            }
            // Encrypt AES tzIdHash
            String encTzIdHash = null;
            if (!TextUtils.isEmpty(mTzIdHash)) {
                encTzIdHash = genericCipherUtil.encryptData(mTzIdHash);
                if (TextUtils.isEmpty(encTzIdHash)) {
                    LogUtil.logError(TAG, "Failed to tzIdHash Pin Code");
                    return;
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
            setFcmToken(encFcmToken);
            setWhisperPub(encWhisperPub);
            setPushyToken(encPushyToken);
            setPublicKey(encPublicKey);
            setPinCode(encPinCode);
            setTzIdHash(encTzIdHash);
            setName(encName);
        }
    }

    @WorkerThread
    @Override
    public void decrypt() {
        synchronized (mLock) {
            // AES
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();
            // Decrypt AES
            String decFcmToken = null;
            if (!TextUtils.isEmpty(mFcmToken)) {
                decFcmToken = genericCipherUtil.decryptData(mFcmToken);
                if (TextUtils.isEmpty(decFcmToken)) {
                    LogUtil.logError(TAG, "Failed to decrypt Fcm Token", new DecryptException());
                    return;
                }
            }

            String decWhisperPub = null;
            String decPushyToken = null;
            if (!TextUtils.isEmpty(mWhisperPub) && !TextUtils.isEmpty(mPushyToken)) {
                decWhisperPub = genericCipherUtil.decryptData(mWhisperPub);
                if (TextUtils.isEmpty(decWhisperPub)) {
                    LogUtil.logError(
                            TAG, "Failed to decrypt Whisper Public Key", new DecryptException());
                    return;
                }
                decPushyToken = genericCipherUtil.decryptData(mPushyToken);
                if (TextUtils.isEmpty(decPushyToken)) {
                    LogUtil.logError(TAG, "Failed to decrypt Pushy Token", new DecryptException());
                    return;
                }
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
            setFcmToken(decFcmToken);
            setWhisperPub(decWhisperPub);
            setPushyToken(decPushyToken);
            setPublicKey(decPublicKey);
            setPinCode(decPinCode);

            if (isSensitiveDataEncrypted()) {
                // Decrypt AES tzIdHash
                String decTzIdHash = null;
                if (!TextUtils.isEmpty(mTzIdHash)) {
                    decTzIdHash = genericCipherUtil.decryptData(mTzIdHash);
                    if (TextUtils.isEmpty(decTzIdHash)) {
                        LogUtil.logError(TAG, "Failed to decrypt tzIdHash Pin Code");
                        return;
                    }
                }
                // Decrypt AES name
                String decName = null;
                if (!TextUtils.isEmpty(mName)) {
                    decName = genericCipherUtil.decryptData(mName);
                    if (TextUtils.isEmpty(decName)) {
                        LogUtil.logError(TAG, "Failed to decrypt name");
                        return;
                    }
                }
                setTzIdHash(decTzIdHash);
                setName(decName);
            }
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
