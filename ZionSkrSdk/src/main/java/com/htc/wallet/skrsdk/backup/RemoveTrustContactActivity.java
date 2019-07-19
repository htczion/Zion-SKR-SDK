package com.htc.wallet.skrsdk.backup;

import static com.htc.wallet.skrsdk.util.PinCodeConfirmConstant.RES_REMOVE_TRUSTED_CONTACT;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_TRIGGER_BROADCAST;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

import com.htc.htcwalletsdk.Export.HtcWalletSdkManager;
import com.htc.htcwalletsdk.Export.RESULT;
import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.SocialBaseActivity;
import com.htc.wallet.skrsdk.backup.adapter.RemoveTrustContactListAdapter;
import com.htc.wallet.skrsdk.backup.listener.RecyclerViewItemClickListener;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupTargetUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.WalletSdkUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RemoveTrustContactActivity extends SocialBaseActivity {
    private static final String TAG = "RemoveTrustContactActivity";
    private final int REMOVE_COUNT_DEFAULT = 0;
    private BroadcastReceiver mBackupTargetsReceiver;
    private Button mBtnRemove;
    private RemoveTrustContactListAdapter mAdapter;
    private volatile ActivityHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_social_backup_remove_trust_contact);
        initView();
        mHandler = new ActivityHandler(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupTriggerReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeTriggerReceiver();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.tb_social_backup_remove_toolbar);
        setSupportActionBar(toolbar);
        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onBackPressed();
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                break;
        }
        return true;
    }

    private void initView() {
        setupToolbar();
        setupButtons();
        setupContactsRecyclerView();
    }

    private void setupContactsRecyclerView() {
        final RecyclerView recyclerView = findViewById(R.id.rv_remove);
        BackupTargetUtil.getAll(
                this,
                new LoadListListener() {

                    @Override
                    public void onLoadFinished(
                            List<BackupSourceEntity> backupSourceEntityList,
                            List<BackupTargetEntity> backupTargetEntityList,
                            List<RestoreSourceEntity> restoreSourceEntityList,
                            List<RestoreTargetEntity> restoreTargetEntityList) {
                        if (backupTargetEntityList == null) {
                            backupTargetEntityList = new ArrayList<>();
                        }
                        mAdapter =
                                new RemoveTrustContactListAdapter(
                                        getBaseContext(),
                                        backupTargetEntityList,
                                        new RecyclerViewItemClickListener() {
                                            @Override
                                            public void onItemCheck(
                                                    Map<Integer, Pair<String, Map<String, String>>>
                                                            item) {
                                                mBtnRemove.setText(
                                                        String.format(
                                                                getString(
                                                                        R.string
                                                                                .backup_remove_button_delete),
                                                                item.size()));
                                            }

                                            @Override
                                            public void onItemUnCheck(
                                                    Map<Integer, Pair<String, Map<String, String>>>
                                                            item) {
                                                mBtnRemove.setText(
                                                        String.format(
                                                                getString(
                                                                        R.string
                                                                                .backup_remove_button_delete),
                                                                item.size()));
                                            }
                                        });
                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        recyclerView.setLayoutManager(
                                                new LinearLayoutManager(getBaseContext()));
                                        recyclerView.setAdapter(mAdapter);
                                    }
                                });
                    }
                });
    }

    private void setupButtons() {
        Button btnCancel = findViewById(R.id.btn_remove_cancel);
        mBtnRemove = findViewById(R.id.btn_remove);
        mBtnRemove.setText(
                String.format(
                        getString(R.string.backup_remove_button_delete), REMOVE_COUNT_DEFAULT));
        btnCancel.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onBackPressed();
                    }
                });

        mBtnRemove.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Thread thread =
                                new Thread(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    if (mAdapter.isNoItemSelected()) {
                                                        LogUtil.logDebug(
                                                                TAG, "No removed item selected.");
                                                        return;
                                                    }
                                                    HtcWalletSdkManager htcWalletSdkManager =
                                                            HtcWalletSdkManager.getInstance();
                                                    int initRet = htcWalletSdkManager.init(
                                                            getBaseContext());

                                                    if (initRet == RESULT.SUCCESS) {
                                                        long uniqueId = WalletSdkUtil.getUniqueId(
                                                                getBaseContext());
                                                        int ret =
                                                                HtcWalletSdkManager.getInstance()
                                                                        .confirmPIN(
                                                                                uniqueId,
                                                                                RES_REMOVE_TRUSTED_CONTACT);
                                                        if (ret == RESULT.SUCCESS) {
                                                            if (mHandler != null) {
                                                                mHandler.sendEmptyMessage(
                                                                        RES_REMOVE_TRUSTED_CONTACT);
                                                            }
                                                        }
                                                    } else {
                                                        LogUtil.logError(
                                                                TAG,
                                                                "init failed, ret = " + initRet);
                                                    }
                                                } catch (RuntimeException e) {
                                                    LogUtil.logError(
                                                            TAG, "confirmPIN, error  = " + e);
                                                }
                                            }
                                        });
                        thread.start();
                    }
                });
    }

    private void setupTriggerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_TRIGGER_BROADCAST);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        mBackupTargetsReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        setupContactsRecyclerView();
                    }
                };
        localBroadcastManager.registerReceiver(mBackupTargetsReceiver, intentFilter);
    }

    private void removeTriggerReceiver() {
        if (mBackupTargetsReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBackupTargetsReceiver);
        }
    }

    private void handleActivityMessage(android.os.Message message) {
        switch (message.what) {
            case RES_REMOVE_TRUSTED_CONTACT:
                mAdapter.remove(this);
                mBtnRemove.setText(
                        String.format(
                                getString(R.string.backup_remove_button_delete),
                                REMOVE_COUNT_DEFAULT));
                break;
        }
    }

    private static class ActivityHandler extends Handler {
        private WeakReference<RemoveTrustContactActivity> mActivity;

        ActivityHandler(RemoveTrustContactActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(android.os.Message message) {
            super.handleMessage(message);
            if (mActivity.get() != null) {
                mActivity.get().handleActivityMessage(message);
            }
        }
    }
}
