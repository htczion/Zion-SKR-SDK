package com.htc.wallet.skrsdk.applink;

import static com.htc.wallet.skrsdk.applink.AppLinkConstant.FLOW_TYPE_RESTORE;
import static com.htc.wallet.skrsdk.applink.AppLinkConstant.KEY_ADDRESS_SIGNATURE;
import static com.htc.wallet.skrsdk.applink.AppLinkConstant.KEY_BACKUP_TARGET_NAME;
import static com.htc.wallet.skrsdk.applink.AppLinkConstant.KEY_BACKUP_TARGET_UUID_HASH;
import static com.htc.wallet.skrsdk.applink.AppLinkConstant.KEY_DEVICE_ID;
import static com.htc.wallet.skrsdk.applink.AppLinkConstant.KEY_FLOW_TYPE;
import static com.htc.wallet.skrsdk.applink.AppLinkConstant.KEY_GOOGLE_DRIVE_DEVICE_ID;
import static com.htc.wallet.skrsdk.applink.AppLinkConstant.KEY_PUBLIC;
import static com.htc.wallet.skrsdk.applink.AppLinkConstant.KEY_PUSHY_TOKEN;
import static com.htc.wallet.skrsdk.applink.AppLinkConstant.KEY_RESTORE_TARGET_PHONE_MODEL;
import static com.htc.wallet.skrsdk.applink.AppLinkConstant.KEY_TEST;
import static com.htc.wallet.skrsdk.applink.AppLinkConstant.KEY_TIMESTAMP;
import static com.htc.wallet.skrsdk.applink.AppLinkConstant.KEY_TOKEN;
import static com.htc.wallet.skrsdk.applink.AppLinkConstant.KEY_TZ_ID_HASH;
import static com.htc.wallet.skrsdk.applink.AppLinkConstant.KEY_USER_NAME;
import static com.htc.wallet.skrsdk.applink.AppLinkConstant.KEY_VERSION;
import static com.htc.wallet.skrsdk.applink.AppLinkConstant.KEY_WHISPER_PUB;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.SocialBaseActivity;
import com.htc.wallet.skrsdk.applink.crypto.AppLinkParamsCryptoUtil;
import com.htc.wallet.skrsdk.sqlite.util.RestoreTargetUtil;
import com.htc.wallet.skrsdk.util.IntentUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.ProgressDialogUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;

public class BaseAppLinkReceiverActivity extends SocialBaseActivity {
    private static final String TAG = "BaseAppLinkReceiverActivity";
    private static final String[] INTENT_KEYS =
            new String[]{
                    KEY_PUBLIC,
                    KEY_TOKEN,
                    KEY_WHISPER_PUB,
                    KEY_PUSHY_TOKEN,
                    KEY_ADDRESS_SIGNATURE,
                    KEY_USER_NAME,
                    KEY_BACKUP_TARGET_NAME,
                    KEY_DEVICE_ID,
                    KEY_TZ_ID_HASH,
                    KEY_TEST,
                    KEY_GOOGLE_DRIVE_DEVICE_ID,
                    KEY_RESTORE_TARGET_PHONE_MODEL,
                    KEY_FLOW_TYPE,
                    KEY_VERSION,
                    KEY_TIMESTAMP,
                    KEY_BACKUP_TARGET_UUID_HASH
            };

    private static final String[] NEED_DECRYPT_INTENT_KEYS =
            new String[]{
                    KEY_PUBLIC,
                    KEY_TOKEN,
                    KEY_WHISPER_PUB,
                    KEY_PUSHY_TOKEN,
                    KEY_ADDRESS_SIGNATURE,
                    KEY_USER_NAME,
                    KEY_BACKUP_TARGET_NAME,
                    KEY_DEVICE_ID,
                    KEY_TZ_ID_HASH,
                    KEY_GOOGLE_DRIVE_DEVICE_ID
            };
    private static final int CHECK_TIME_OUT = 20000;
    private static final int CHECK_TIME_OUT_INTERVAL = 1000;

    private AppLinkParamsCryptoUtil mParamsCryptoUtil;
    private ProgressDialog mProgressDialog;
    private ProgressDialogUtil mProgressDialogUtil;
    private CountDownTimer mCountDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake_layout);

        mParamsCryptoUtil = new AppLinkParamsCryptoUtil(this);
    }

    public void checkFirebaseLink() {
        FirebaseDynamicLinks.getInstance().getDynamicLink(getIntent())
                .addOnSuccessListener(this, new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(
                            final PendingDynamicLinkData pendingDynamicLinkData) {
                        if (pendingDynamicLinkData != null) {
                            Uri deepLink = pendingDynamicLinkData.getLink();
                            if (deepLink != null) {
                                Map<String, String> data = wrapUriIntoMap(deepLink);
                                if (data == null) {
                                    LogUtil.logDebug(TAG, "FirebaseLink data is null");
                                    finish();
                                    return;
                                }
                                checkLinkDataCanStartActivity(data);
                            } else {
                                LogUtil.logDebug(TAG, "checkFirebaseLink, deepLink is null");
                            }
                        } else {
                            LogUtil.logDebug(TAG,
                                    "checkFirebaseLink, no pendingDynamicLinkData found");
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        LogUtil.logError(TAG, "checkFirebaseLink, onFailure error = " + e);
                    }
                });
    }

    public void checkBranchLink() {
        // Branch init
        Branch.getInstance().initSession(new Branch.BranchReferralInitListener() {
            @Override
            public void onInitFinished(
                    JSONObject referringParams, BranchError error) {
                if (!isBranchAppLink(referringParams)) {
                    LogUtil.logWarning(TAG, "this applink isn't branch applink");
                    return;
                }
                if (error == null) {
                    Map<String, String> data = wrapJSONObjectIntoMap(referringParams);
                    if (data == null) {
                        LogUtil.logDebug(TAG, "BranchLink data is null");
                        finish();
                        return;
                    }
                    checkLinkDataCanStartActivity(data);
                } else {
                    LogUtil.logDebug(TAG, "onInitFinished, error = " + error.getMessage());
                }
            }
        }, getIntent().getData(), this);
    }

    private boolean isBranchAppLink(final JSONObject params) {
        if (params == null) {
            LogUtil.logWarning(TAG, "isBranchAppLink(), params is null");
            return false;
        }
        final Iterator<String> keys = params.keys();
        final String branchApplinkDomain = getResources().getString(R.string.branch_applink_domain);
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                Object object = params.get(key);
                if (object == null) {
                    continue;
                }
                String value = object.toString();
                if (TextUtils.isEmpty(value)) {
                    continue;
                }
                if (value.contains(branchApplinkDomain)) {
                    return true;
                }
            } catch (JSONException e) {
                LogUtil.logError(TAG, "onInitFinished(), error = " + e);
            }
        }
        return false;
    }

    private void checkLinkDataCanStartActivity(@NonNull final Map<String, String> linkData) {
        if (linkData == null) {
            LogUtil.logDebug(TAG, "checkLinkCanStartActivity, linkData is null");
            finish();
            return;
        }

        final Intent intent = setupIntent(linkData);
        String flowType = linkData.get(KEY_FLOW_TYPE);
        String googleDriveDeviceID = linkData.get(KEY_GOOGLE_DRIVE_DEVICE_ID);

        if (FLOW_TYPE_RESTORE.equals(flowType)
                && !TextUtils.isEmpty(flowType)
                && TextUtils.isEmpty(googleDriveDeviceID)) {
            setupProgressDialog();
            startCountdownTimer(linkData);
            mProgressDialog = mProgressDialogUtil.show();
        } else {
            if (intent != null) {
                startActivity(intent);
            } else {
                LogUtil.logDebug(TAG, "checkLinkCanStartActivity, intent is null");
            }
            finish();
        }
    }

    private Intent setupIntent(@NonNull final Map<String, String> linkData) {
        if (linkData == null) {
            return null;
        }

        Bundle bundle = new Bundle();
        String linkVersionStr = linkData.get(KEY_VERSION);
        for (String key : INTENT_KEYS) {
            String data = linkData.get(key);
            if (needBeParamOfAppLinkDecrypted(key, linkVersionStr)) {
                data = mParamsCryptoUtil.decrypt(data);
            } else {
                LogUtil.logInfo(TAG, "param [" + key + "] of app link do not need to decrypt");
            }
            bundle.putString(key, data);
        }

        String flowType = linkData.get(KEY_FLOW_TYPE);
        if (!FLOW_TYPE_RESTORE.equals(flowType) && TextUtils.isEmpty(flowType)) {
            // Backup flow
            return IntentUtil.generateBackupIntent(this, bundle);
        } else {
            // Restore flow
            return IntentUtil.generateRestoreIntent(this, bundle);
        }
    }

    private boolean needBeParamOfAppLinkDecrypted(final String paramKey,
            final String appLinkVersion) {
        if (TextUtils.isEmpty(appLinkVersion)) {
            LogUtil.logInfo(TAG,
                    "appLinkVersion is empty, can't determine if params need be decrypted");
            return false;
        }
        final List<String> needDecryptIntentKeysList =
                new ArrayList<>(Arrays.asList(NEED_DECRYPT_INTENT_KEYS));
        return !TextUtils.isEmpty(paramKey) && needDecryptIntentKeysList.contains(paramKey)
                && isParamOfAppLinkEncrypted(Integer.valueOf(appLinkVersion));
    }

    private boolean isParamOfAppLinkEncrypted(int appLinkVersion) {
        // Add params encryption in version 3.
        return appLinkVersion > 2;
    }

    private void setupProgressDialog() {
        String dialogTitle =
                getResources().getString(R.string.backup_choose_google_drive_dialog_title);
        String dialogContent =
                getResources().getString(R.string.backup_choose_google_drive_dialog_content);
        mProgressDialogUtil = new ProgressDialogUtil(this, dialogTitle, dialogContent);
    }

    private void startCountdownTimer(@NonNull final Map<String, String> linkData) {
        synchronized (this) {
            if (linkData == null) {
                return;
            }
            cancelCountDownTimer();
            mCountDownTimer = new CountDownTimer(CHECK_TIME_OUT, CHECK_TIME_OUT_INTERVAL) {
                @Override
                public void onFinish() {
                    String restoreTargetEmailHash = linkData.get(KEY_ADDRESS_SIGNATURE);
                    String restoreTargetUuidHash = linkData.get(KEY_DEVICE_ID);
                    RestoreTargetUtil.remove(getBaseContext(), restoreTargetEmailHash,
                            restoreTargetUuidHash);
                    mProgressDialogUtil.dismiss(mProgressDialog);
                    Intent intent = IntentUtil.generateEntryActivityIntent(getBaseContext());
                    cancelCountDownTimer();
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onTick(long millisUntilFinished) {
                }
            }.start();
        }
    }

    private void cancelCountDownTimer() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }

    private Map<String, String> wrapJSONObjectIntoMap(@NonNull final JSONObject jsonObject) {
        if (jsonObject == null) {
            LogUtil.logDebug(TAG, "wrapJSONObjectIntoMap, jsonObject is null");
            return null;
        }
        final HashMap<String, String> map = new HashMap<>();
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            try {
                String key = keys.next();
                String value = jsonObject.getString(key);
                map.put(key, value);
            } catch (JSONException e) {
                LogUtil.logError(TAG, "wrapJSONObjectIntoMap, error = " + e);
            }
        }
        return map;
    }

    private Map<String, String> wrapUriIntoMap(@NonNull final Uri uri) {
        if (uri == null) {
            LogUtil.logDebug(TAG, "wrapUriIntoMap, uri is null");
            return null;
        }
        final HashMap<String, String> map = new HashMap<>();
        Set<String> paramsNames = uri.getQueryParameterNames();
        for (String param : paramsNames) {
            map.put(param, uri.getQueryParameter(param));
        }
        return map;
    }
}
