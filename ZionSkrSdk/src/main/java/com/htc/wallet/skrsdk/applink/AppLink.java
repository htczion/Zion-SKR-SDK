package com.htc.wallet.skrsdk.applink;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public interface AppLink {

    void createLinkTask(Context context);

    void setShortLinkTaskListener(ShortLinkTaskListener shortLinkTaskListener);

    void setFailureListener(ShortLinkFailureListener failureListener);

    class ParamsBuilder {
        private String mWhisperPub;
        private String mPushyToken;
        private String mToken;
        private String mPublicKey;
        private String mAddressSignature;
        private String mUserName;
        private String mBackupTargetName;
        private String mBackupTargetUUIDHash;
        private String mDeviceId;
        private String mTzIdHash;
        private String mGoogleDriveDeviceId;
        private String mPhoneModel;
        private String mFlowType;
        private String mKeyTest;

        public AppLink.ParamsBuilder publicKey(final String publicKey) {
            mPublicKey = publicKey;
            return this;
        }

        public AppLink.ParamsBuilder whisperPub(final String whisperPub) {
            mWhisperPub = whisperPub;
            return this;
        }

        public AppLink.ParamsBuilder pushyToken(final String pushyToken) {
            mPushyToken = pushyToken;
            return this;
        }

        public AppLink.ParamsBuilder token(final String token) {
            mToken = token;
            return this;
        }

        public AppLink.ParamsBuilder addressSignature(final String signature) {
            mAddressSignature = signature;
            return this;
        }

        public AppLink.ParamsBuilder userName(final String name) {
            mUserName = name;
            return this;
        }

        // For resending link
        public AppLink.ParamsBuilder backupTargetName(final String backupTargetName) {
            mBackupTargetName = backupTargetName;
            return this;
        }

        // For resending link, real Bad
        public AppLink.ParamsBuilder backupTargetUUIDHash(final String backupTargetUUIDHash) {
            mBackupTargetUUIDHash = backupTargetUUIDHash;
            return this;
        }

        public AppLink.ParamsBuilder deviceId(final String deviceId) {
            mDeviceId = deviceId;
            return this;
        }

        public AppLink.ParamsBuilder tzIdHash(final String tzIdHash) {
            mTzIdHash = tzIdHash;
            return this;
        }

        public AppLink.ParamsBuilder googleDriveDeviceId(final String googleDriveDeviceId) {
            mGoogleDriveDeviceId = googleDriveDeviceId;
            return this;
        }

        public AppLink.ParamsBuilder phoneModel(final String phoneModel) {
            mPhoneModel = phoneModel;
            return this;
        }

        public AppLink.ParamsBuilder flowType(final String type) {
            mFlowType = type;
            return this;
        }

        public AppLink.ParamsBuilder isTest(final String isTest) {
            mKeyTest = isTest;
            return this;
        }

        public Uri setupLink(@NonNull final Uri deepLink, @NonNull final Map<String, String> map) {
            Uri.Builder builder = deepLink.buildUpon();
            Set<String> keys = map.keySet();
            for (String key : keys) {
                builder.appendQueryParameter(key, map.get(key));
            }
            return builder.build();
        }

        public Map<String, String> setupParamsMap() {
            HashMap<String, String> map = new HashMap<>();
            if (mPublicKey != null) {
                map.put(AppLinkConstant.KEY_PUBLIC, mPublicKey);
            }
            if (mWhisperPub != null) {
                map.put(AppLinkConstant.KEY_WHISPER_PUB, mWhisperPub);
            }
            if (mPushyToken != null) {
                map.put(AppLinkConstant.KEY_PUSHY_TOKEN, mPushyToken);
            }
            if (mToken != null) {
                map.put(AppLinkConstant.KEY_TOKEN, mToken);
            }
            if (mAddressSignature != null) {
                map.put(AppLinkConstant.KEY_ADDRESS_SIGNATURE, mAddressSignature);
            }
            if (mUserName != null) {
                map.put(AppLinkConstant.KEY_USER_NAME, mUserName);
            }
            // For resending link
            if (mBackupTargetName != null) {
                map.put(AppLinkConstant.KEY_BACKUP_TARGET_NAME, mBackupTargetName);
            }
            // For resending link, real Bad
            if (mBackupTargetUUIDHash != null) {
                map.put(AppLinkConstant.KEY_BACKUP_TARGET_UUID_HASH, mBackupTargetUUIDHash);
            }
            if (mDeviceId != null) {
                map.put(AppLinkConstant.KEY_DEVICE_ID, mDeviceId);
            }
            if (mTzIdHash != null) {
                map.put(AppLinkConstant.KEY_TZ_ID_HASH, mTzIdHash);
            }
            if (mGoogleDriveDeviceId != null) {
                map.put(AppLinkConstant.KEY_GOOGLE_DRIVE_DEVICE_ID, mGoogleDriveDeviceId);
            }
            if (mPhoneModel != null) {
                map.put(AppLinkConstant.KEY_RESTORE_TARGET_PHONE_MODEL, mPhoneModel);
            }
            if (mFlowType != null) {
                map.put(AppLinkConstant.KEY_FLOW_TYPE, mFlowType);
            }
            if (mKeyTest != null) {
                map.put(AppLinkConstant.KEY_TEST, mKeyTest);
            }
            map.put(AppLinkConstant.KEY_VERSION, AppLinkConstant.APP_LINK_VERSION_STR);
            map.put(AppLinkConstant.KEY_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
            return map;
        }
    }
}
