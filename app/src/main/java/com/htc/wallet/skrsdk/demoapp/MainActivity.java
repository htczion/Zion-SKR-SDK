package com.htc.wallet.skrsdk.demoapp;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.htc.htcwalletsdk.Export.HtcWalletSdkManager;
import com.htc.htcwalletsdk.Export.RESULT;
import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.demoapp.util.ZKMASdkUtil;
import com.htc.wallet.skrsdk.restore.reconnect.ReconnectNameUtils;
import com.htc.wallet.skrsdk.util.Callback;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private TextView mTextViewStatus;
    private Button mButtonSkrBackup;
    private Button mButtonSkrRestore;
    private Button mButtonCreate;
    private Button mButtonReset;
    private Button mButtonPhraseRestore;
    private Button mButtonLaunchRequests;
    private Switch mSwitchPwdType;

    private Callback<Integer> mSkrRequestsCountCallback;
    private BroadcastReceiver mSkrRequestsDataChangedReceiver;

    private AlertDialog mSkrReconnectRemindDialog;

    private final ExecutorService mSingleThreadExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextViewStatus = findViewById(R.id.textview_status);
        mButtonSkrBackup = findViewById(R.id.button_skr_backup);
        mButtonSkrRestore = findViewById(R.id.button_skr_restore);
        mButtonCreate = findViewById(R.id.button_create);
        mButtonReset = findViewById(R.id.button_reset);
        mButtonPhraseRestore = findViewById(R.id.button_phrase_restore);
        mButtonLaunchRequests = findViewById(R.id.button_request_list);
        mSwitchPwdType = findViewById(R.id.switch_password_type);
        mSwitchPwdType.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                setPasswordTypeAlphanumeric(isChecked);
            }
        });

        mSkrRequestsCountCallback = new Callback<Integer>() {
            @Override
            public void onResponse(final Integer count) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing()) {
                            mButtonLaunchRequests.setText("Start SKR Requests (" + count + ")");
                        }
                    }
                });
            }
        };

        setPasswordTypeAlphanumeric(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSingleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        });

        mSkrRequestsDataChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intent) {
                if (context == null) {
                    Log.e(TAG, "context is null");
                    return;
                } else if (intent == null) {
                    Log.e(TAG, "intent is null");
                    return;
                }

                String action = intent.getAction();
                if (TextUtils.isEmpty(action)) {
                    Log.e(TAG, "action is empty");
                    return;
                }

                switch (action) {
                    case ZionSkrSdkManager.ACTION_SKR_REQUESTS_DATA_CHANGED:
                        ZionSkrSdkManager.getInstance()
                                .getSkrRequestsCount(getBaseContext(), mSkrRequestsCountCallback);
                        break;
                    default:
                        Log.e(TAG, "unknown action=" + action);
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ZionSkrSdkManager.ACTION_SKR_REQUESTS_DATA_CHANGED);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(mSkrRequestsDataChangedReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mSkrRequestsDataChangedReceiver != null) {
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
            localBroadcastManager.unregisterReceiver(mSkrRequestsDataChangedReceiver);
            mSkrRequestsDataChangedReceiver = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (mSkrReconnectRemindDialog != null && mSkrReconnectRemindDialog.isShowing()) {
            mSkrReconnectRemindDialog.dismiss();
        }
        super.onDestroy();
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_skr_backup:
                ZionSkrSdkManager.getInstance().startSkrBackup(this);
                break;
            case R.id.button_skr_restore:
                ZionSkrSdkManager.getInstance().startSkrRestore(this);
                break;
            case R.id.button_phrase_restore:
                restoreWallet();
                break;
            case R.id.button_create:
                createWallet();
                break;
            case R.id.button_reset:
                resetWallet();
                break;
            case R.id.button_request_list:
                ZionSkrSdkManager.getInstance().launchSkrRequestsActivity(this);
                break;
        }
    }

    private void restoreWallet() {
        mSingleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                int ret = HtcWalletSdkManager.getInstance().init(getBaseContext());
                if (ret != RESULT.SUCCESS) {
                    Log.e(TAG, "init() failed, ret=" + ret);
                    return;
                }

                long uniqueId = ZKMASdkUtil.getUniqueId(getBaseContext());
                if (uniqueId == RESULT.REGISTER_FAILED) {
                    Log.e(TAG, "getUniqueId() failed");
                    return;
                }

                final int restoreRet = HtcWalletSdkManager.getInstance().restoreSeed(uniqueId);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (restoreRet == RESULT.SUCCESS) {
                            Toast.makeText(getBaseContext(), "Restore success",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getBaseContext(), "Restore failed, ret=" + restoreRet,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                refresh();
            }
        });
    }

    private void createWallet() {
        mSingleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                int ret = HtcWalletSdkManager.getInstance().init(getBaseContext());
                if (ret != RESULT.SUCCESS) {
                    Log.e(TAG, "init() failed, ret=" + ret);
                    return;
                }

                long uniqueId = ZKMASdkUtil.getUniqueId(getBaseContext());
                if (uniqueId == RESULT.REGISTER_FAILED) {
                    Log.e(TAG, "getUniqueId() failed");
                    return;
                }

                final int createRet = HtcWalletSdkManager.getInstance().createSeed(uniqueId);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (createRet == RESULT.SUCCESS) {
                            Toast.makeText(getBaseContext(), "Create success",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getBaseContext(), "Create failed, ret=" + createRet,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                refresh();
            }
        });
    }

    private void resetWallet() {
        mSingleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                int ret = HtcWalletSdkManager.getInstance().init(getBaseContext());
                if (ret != RESULT.SUCCESS) {
                    Log.e(TAG, "init() failed, ret=" + ret);
                    return;
                }

                long uniqueId = ZKMASdkUtil.getUniqueId(getBaseContext());
                if (uniqueId == RESULT.REGISTER_FAILED) {
                    Log.e(TAG, "getUniqueId() failed");
                    return;
                }

                final int clearRet = HtcWalletSdkManager.getInstance().clearSeed(uniqueId);

                if (clearRet == RESULT.SUCCESS) {
                    ActivityManager activityManager =
                            (ActivityManager) getBaseContext().getSystemService(ACTIVITY_SERVICE);
                    Objects.requireNonNull(activityManager, "activityManager is null");
                    activityManager.clearApplicationUserData();
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getBaseContext(), "Reset failed, ret=" + clearRet,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                refresh();
            }
        });
    }

    @WorkerThread
    private void refresh() {
        @UiType
        int type = UiType.NOT_EXODUS;
        boolean isExodus = ZKMASdkUtil.isExodus(getBaseContext());
        if (isExodus) {
            boolean isSeedExists = ZKMASdkUtil.isSeedExists(getBaseContext());
            if (isSeedExists) {
                type = UiType.SEED_EXIST;
            } else {
                type = UiType.SEED_NOT_EXIST;
            }
        }
        final List<String> reconnectNameList = ReconnectNameUtils.getNameList(getBaseContext());

        final int uiType = type;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setUi(uiType);
                showReconnectRemindDialog(reconnectNameList);
            }
        });

        ZionSkrSdkManager.getInstance().getSkrRequestsCount(getBaseContext(),
                mSkrRequestsCountCallback);
    }

    @UiThread
    private void setUi(@UiType int type) {
        boolean isBackupFlow = false;
        boolean isRestoreFlow = false;
        String text = "";
        switch (type) {
            case UiType.NOT_EXODUS:
                isBackupFlow = false;
                isRestoreFlow = false;
                text = "Not Exodus";
                break;
            case UiType.SEED_EXIST:
                isBackupFlow = true;
                isRestoreFlow = false;
                text = "Seed Exists";
                break;
            case UiType.SEED_NOT_EXIST:
                isBackupFlow = false;
                isRestoreFlow = true;
                text = "Seed Not Exists";
                break;
        }

        mTextViewStatus.setText(text);

        mButtonSkrBackup.setEnabled(isBackupFlow);
        mButtonReset.setEnabled(isBackupFlow);

        mButtonSkrRestore.setEnabled(isRestoreFlow);
        mButtonPhraseRestore.setEnabled(isRestoreFlow);
        mButtonCreate.setEnabled(isRestoreFlow);
        mSwitchPwdType.setEnabled(isRestoreFlow);
    }

    @UiThread
    private void showReconnectRemindDialog(List<String> reconnectNameList) {
        if (reconnectNameList == null || reconnectNameList.isEmpty()) {
            return;
        }

        String title = getResources().getString(
                R.string.social_restore_backup_rebuild_trust_contact_remind_dialog_title);
        String content;
        if (reconnectNameList.size() == 1) {
            content = getResources().getString(
                    R.string.social_restore_backup_rebuild_trust_contact_remind_dialog_content_one,
                    reconnectNameList.get(0));
        } else if (reconnectNameList.size() == 2) {
            content = getResources().getString(
                    R.string.social_restore_backup_rebuild_trust_contact_remind_dialog_content,
                    reconnectNameList.get(0), reconnectNameList.get(1));
        } else {
            Log.e(TAG, "no reasonable scenario");
            return;
        }

        if (mSkrReconnectRemindDialog == null) {
            mSkrReconnectRemindDialog = new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(content)
                    .setCancelable(false)
                    .setPositiveButton(R.string.backup_introduction_setup,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ReconnectNameUtils.clearNameList(getBaseContext());
                                    ZionSkrSdkManager.getInstance().startSkrBackup(
                                            getBaseContext());
                                    dialog.dismiss();
                                }
                            })
                    .setNegativeButton(R.string.social_backup_not_now,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ReconnectNameUtils.clearNameList(getBaseContext());
                                    dialog.dismiss();
                                }
                            })
                    .create();
        }

        if (!isFinishing() && !mSkrReconnectRemindDialog.isShowing()) {
            mSkrReconnectRemindDialog.show();
        }
    }

    private void setPasswordTypeAlphanumeric(final boolean isAlphanumeric) {
        mSingleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                boolean isExodus = ZKMASdkUtil.isExodus(getBaseContext());
                if (isExodus) {
                    boolean isSeedExists = ZKMASdkUtil.isSeedExists(getBaseContext());
                    if (!isSeedExists) {
                        long uniqueId = ZKMASdkUtil.getUniqueId(getBaseContext());
                        if (uniqueId == RESULT.REGISTER_FAILED) {
                            Log.e(TAG, "getUniqueId() failed");
                        } else {
                            int type = isAlphanumeric ? 0 : 1;
                            // 0 qwerty
                            // 1 number
                            HtcWalletSdkManager.getInstance().setKeyboardType(uniqueId, type);
                        }
                    }
                }
            }
        });
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({UiType.NOT_EXODUS, UiType.SEED_EXIST, UiType.SEED_NOT_EXIST})
    private @interface UiType {
        int NOT_EXODUS = 0;
        int SEED_EXIST = 1;
        int SEED_NOT_EXIST = 2;
    }
}
