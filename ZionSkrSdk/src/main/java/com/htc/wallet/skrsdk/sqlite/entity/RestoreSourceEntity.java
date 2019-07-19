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

@Entity(tableName = "restoreSource")
public class RestoreSourceEntity implements DataEntity {
    public static final String TAG = "RestoreSourceEntity";
    @Ignore
    public static final int RESTORE_SOURCE_STATUS_REQUEST = 0;
    @Ignore
    public static final int RESTORE_SOURCE_STATUS_OK = 1;
    @Ignore
    public static final long UNDEFINED_TIME_STAMP = -1L;
    @Ignore
    private static final String EMPTY_STRING = "";
    @Ignore
    private final Object mLock = new Object();

    @ColumnInfo(name = "version")
    private volatile int mVersion = BACKUP_DATA_CURRENT_VERSION;

    @ColumnInfo(name = "status")
    private volatile int mStatus;

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "uuidHash")
    private volatile String mUUIDHash;

    @ColumnInfo(name = "fcmToken")
    private volatile String mFcmToken; // AES

    @ColumnInfo(name = "whisperPub")
    private volatile String mWhisperPub;

    @ColumnInfo(name = "pushyToken")
    private volatile String mPushyToken;

    @ColumnInfo(name = "publicKey")
    private volatile String mPublicKey; // AES

    @ColumnInfo(name = "timeStamp")
    private volatile long mTimeStamp = UNDEFINED_TIME_STAMP;

    @ColumnInfo(name = "seed")
    private volatile String mSeed; // encrypted by Self's(Amy's) publicKey

    @ColumnInfo(name = "encSeedSigned")
    private volatile String mEncSeedSigned;

    @ColumnInfo(name = "pincodePosition")
    private volatile int mPinCodePosition = -1;

    @ColumnInfo(name = "pinCode")
    private volatile String mPinCode = EMPTY_STRING;

    @ColumnInfo(name = "name")
    private volatile String mName;

    @ColumnInfo(name = "encCodePk")
    private volatile String mEncCodePk;

    @ColumnInfo(name = "encCodePkSign")
    private volatile String mEncCodePkSign;

    @ColumnInfo(name = "encAseKey")
    private volatile String encAseKey;

    @ColumnInfo(name = "encAseKeySign")
    private volatile String encAseKeySign;

    public RestoreSourceEntity() {
    }

    public RestoreSourceEntity(@NonNull RestoreSourceEntity source) {
        Objects.requireNonNull(source, "restoreSourceEntity is null");
        synchronized (mLock) {
            mVersion = source.mVersion;
            mStatus = source.mStatus;
            mUUIDHash = source.mUUIDHash;
            mFcmToken = source.mFcmToken;
            mWhisperPub = source.mWhisperPub;
            mPushyToken = source.mPushyToken;
            mPublicKey = source.mPublicKey;
            mTimeStamp = source.mTimeStamp;
            mSeed = source.mSeed;
            mEncSeedSigned = source.mEncSeedSigned;
            mPinCodePosition = source.mPinCodePosition;
            mPinCode = source.mPinCode;
            mName = source.mName;
            mEncCodePk = source.mEncCodePk;
            mEncCodePkSign = source.mEncCodePkSign;
            encAseKey = source.encAseKey;
            encAseKeySign = source.encAseKeySign;
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

    public String getUUIDHash() {
        return mUUIDHash;
    }

    public void setUUIDHash(String UUIDHash) {
        mUUIDHash = UUIDHash;
    }

    public long getTimeStamp() {
        return mTimeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        mTimeStamp = timeStamp;
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

    public String getSeed() {
        return mSeed;
    }

    public void setSeed(String seed) {
        mSeed = seed;
    }

    public String getEncSeedSigned() {
        return mEncSeedSigned;
    }

    public void setEncSeedSigned(String encSeedSigned) {
        mEncSeedSigned = encSeedSigned;
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

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
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
        synchronized (mLock) {
            // AES
            GenericCipherUtil genericCipherUtil = new GenericCipherUtil();

            // Encrypt AES
            String encFcmToken = null;
            if (!TextUtils.isEmpty(mFcmToken)) {
                encFcmToken = genericCipherUtil.encryptData(mFcmToken);
                if (TextUtils.isEmpty(encFcmToken)) {
                    LogUtil.logError(TAG, "Failed to encrypt FCM token");
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
            // Encrypt AES pinCode
            String encPinCode = null;
            if (!TextUtils.isEmpty(mPinCode)) {
                encPinCode = genericCipherUtil.encryptData(mPinCode);
                if (TextUtils.isEmpty(encPinCode)) {
                    LogUtil.logError(TAG, "Failed to encrypt pinCode");
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
                    LogUtil.logError(TAG, "Failed to decrypt FCM token");
                    return;
                }
            }

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
                // Decrypt AES pinCode
                String decPinCode = null;
                if (!TextUtils.isEmpty(mPinCode)) {
                    decPinCode = genericCipherUtil.decryptData(mPinCode);
                    if (TextUtils.isEmpty(decPinCode)) {
                        LogUtil.logError(TAG, "Failed to decrypt pinCode");
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
                setPinCode(decPinCode);
                setName(decName);
            }
        }
    }

    public boolean compareStatus(@RestoreSourceStatusType int status) {
        return mStatus == status;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RESTORE_SOURCE_STATUS_REQUEST, RESTORE_SOURCE_STATUS_OK})
    public @interface RestoreSourceStatusType {
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
