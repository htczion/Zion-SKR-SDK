package com.htc.wallet.skrsdk.verification;

import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_TRIGGER_BROADCAST;
import static com.htc.wallet.skrsdk.verification.constant.SocialKeyRecoveryRequestConstants.TYPE_BACKUP_SOURCE;
import static com.htc.wallet.skrsdk.verification.constant.SocialKeyRecoveryRequestConstants.TYPE_BACKUP_TARGET;
import static com.htc.wallet.skrsdk.verification.constant.SocialKeyRecoveryRequestConstants.TYPE_RESTORE_TARGET;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.SocialBaseActivity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupDataStatusUtil;
import com.htc.wallet.skrsdk.util.IntentUtil;
import com.htc.wallet.skrsdk.verification.uiadapter.RequestListAdapter;
import com.htc.wallet.skrsdk.verification.uilistener.RecyclerViewClickListener;

import java.util.List;

public class SocialKeyRecoveryRequestActivity extends SocialBaseActivity {
    private static final String TAG = "SocialKeyRecoveryRequestActivity";
    private BroadcastReceiver mBackupTargetsReceiver;
    private RecyclerView mRecyclerView;
    private TextView mEmptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_social_backup_pending_backup_target);
        setupToolbar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupContactsRecyclerView();
        setupTriggerReceiver();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBackupTargetsReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBackupTargetsReceiver);
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.tb_social_backup_pending_toolbar);
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

    private void setupContactsRecyclerView() {
        mRecyclerView = findViewById(R.id.rv_pending);
        mEmptyText = findViewById(R.id.tv_null_contact);
        BackupDataStatusUtil.getAllPending(
                this,
                new LoadListListener() {
                    @Override
                    public void onLoadFinished(
                            final List<BackupSourceEntity> backupSourceEntityList,
                            final List<BackupTargetEntity> backupTargetEntityList,
                            final List<RestoreSourceEntity> restoreSourceEntityList,
                            final List<RestoreTargetEntity> restoreTargetEntityList) {
                        final int size =
                                backupSourceEntityList.size()
                                        + backupTargetEntityList.size()
                                        + restoreTargetEntityList.size();
                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        if (size == 0) {
                                            hideRequestContactList();
                                        } else {
                                            showRequestContactList(
                                                    backupTargetEntityList,
                                                    backupSourceEntityList,
                                                    restoreTargetEntityList);
                                        }
                                    }
                                });
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

    private void showRequestContactList(
            @NonNull final List<BackupTargetEntity> backupTargets,
            @NonNull final List<BackupSourceEntity> backupSources,
            @NonNull final List<RestoreTargetEntity> restoreTargets) {
        mRecyclerView.setVisibility(View.VISIBLE);
        mEmptyText.setVisibility(View.GONE);
        RequestListAdapter adapter =
                setRequestContactListAdapter(backupTargets, backupSources, restoreTargets);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(adapter);
    }

    private void hideRequestContactList() {
        mRecyclerView.setVisibility(View.GONE);
        mEmptyText.setVisibility(View.VISIBLE);
    }

    private RequestListAdapter setRequestContactListAdapter(
            @NonNull final List<BackupTargetEntity> backupTargets,
            @NonNull final List<BackupSourceEntity> backupSources,
            @NonNull final List<RestoreTargetEntity> restoreTargets) {
        return new RequestListAdapter(
                this,
                backupTargets,
                backupSources,
                restoreTargets,
                new RecyclerViewClickListener() {

                    @Override
                    public void onItemClick(View view, int type, int position) {
                        Intent intent;
                        switch (type) {
                            case TYPE_BACKUP_TARGET:
                                BackupTargetEntity backupTarget = backupTargets.get(position);
                                intent = IntentUtil.generateBackupTargetIntent(getBaseContext(),
                                        backupTarget);
                                startActivity(intent);
                                break;
                            case TYPE_BACKUP_SOURCE:
                                BackupSourceEntity backupSource = backupSources.get(position);
                                intent = IntentUtil.generateBackupSourceIntent(getBaseContext(),
                                        backupSource);
                                startActivity(intent);
                                break;
                            case TYPE_RESTORE_TARGET:
                                RestoreTargetEntity restoreTarget = restoreTargets.get(position);
                                intent = IntentUtil.generateRestoreTargetIntent(getBaseContext(),
                                        restoreTarget);
                                startActivity(intent);
                                break;
                        }
                    }
                });
    }
}
