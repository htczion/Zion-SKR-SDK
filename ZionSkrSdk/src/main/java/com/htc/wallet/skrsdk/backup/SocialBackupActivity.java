package com.htc.wallet.skrsdk.backup;

import static com.htc.wallet.skrsdk.action.Action.KEY_CHECK_BACKUP_HEALTH;
import static com.htc.wallet.skrsdk.action.Action.MSG_CHECK_BACKUP_STATUS;
import static com.htc.wallet.skrsdk.action.Action.MULTI_PUSHY_TOKEN;
import static com.htc.wallet.skrsdk.action.Action.MULTI_TOKEN;
import static com.htc.wallet.skrsdk.action.Action.MULTI_WHISPER_PUB;
import static com.htc.wallet.skrsdk.backup.constants.BackupSourceConstants.SOCIAL_BACKUP_INFO_CLICK;
import static com.htc.wallet.skrsdk.backup.constants.SocialKeyRecoveryRequestConstants.TYPE_BACKUP_TARGET;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_BAD;
import static com.htc.wallet.skrsdk.util.NotificationUtil.NOTIFICATION_ID_SOCIAL_BACKUP;
import static com.htc.wallet.skrsdk.util.NotificationUtil.cancelNotification;
import static com.htc.wallet.skrsdk.util.PinCodeConfirmConstant.RES_SIGN_OUT;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_CHECK_DRIVE;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_DRIVE_AUTH;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_TRIGGER_BROADCAST;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.htc.htcwalletsdk.Export.HtcWalletSdkManager;
import com.htc.htcwalletsdk.Export.RESULT;
import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.SocialBaseActivity;
import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.action.BackupHealthCheckAction;
import com.htc.wallet.skrsdk.applink.NetworkUtil;
import com.htc.wallet.skrsdk.backup.adapter.TrustContactListAdapter;
import com.htc.wallet.skrsdk.backup.constants.BackupSourceConstants;
import com.htc.wallet.skrsdk.backup.listener.RecyclerViewClickListener;
import com.htc.wallet.skrsdk.drives.DriveCallback;
import com.htc.wallet.skrsdk.drives.DriveJobIntentService;
import com.htc.wallet.skrsdk.drives.DriveServiceType;
import com.htc.wallet.skrsdk.drives.DriveUtil;
import com.htc.wallet.skrsdk.drives.DriveUtilException;
import com.htc.wallet.skrsdk.drives.DriveUtilFactory;
import com.htc.wallet.skrsdk.drives.onedrive.AuthenticationManager;
import com.htc.wallet.skrsdk.drives.onedrive.DriveDataUpdateUtil;
import com.htc.wallet.skrsdk.drives.onedrive.MSALAuthenticationCallback;
import com.htc.wallet.skrsdk.drives.onedrive.MSLAuthCallbackFactory;
import com.htc.wallet.skrsdk.jobs.JobIdManager;
import com.htc.wallet.skrsdk.messaging.MessageServiceType;
import com.htc.wallet.skrsdk.monitor.BackupUserManualCheckJobService;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.LoadListListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupDataStatusUtil;
import com.htc.wallet.skrsdk.sqlite.util.BackupTargetUtil;
import com.htc.wallet.skrsdk.util.Callback;
import com.htc.wallet.skrsdk.util.IntentUtil;
import com.htc.wallet.skrsdk.util.InvitationUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;
import com.htc.wallet.skrsdk.util.WalletSdkUtil;
import com.htc.wallet.skrsdk.verification.SocialKeyRecoveryRequestActivity;
import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.MsalClientException;
import com.microsoft.identity.client.User;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

import me.pushy.sdk.Pushy;

public class SocialBackupActivity extends SocialBaseActivity {
    private static final String TAG = "SocialBackupActivity";
    private static final int STATUS_OK_BACKUP_TARGETS_THRESHOLD = 3;
    private static final int PERMISSION_REQUEST = 0;

    private static final int WARNING_HEADER_MAX_LINES = 3;
    private static final long ANIMATION_DURATION = 300;

    private Button mBtnSharingLink;
    private RecyclerView mRecyclerView;
    private Toolbar mToolbar;
    private TextView mEmptyText;
    private TextView mRequestShowNum;
    private BroadcastReceiver mBackupTargetsReceiver;
    private Intent mIntent;
    private InvitationUtil mInvitationUtil;
    private DriveDataUpdateUtil mDriveDataUpdateUtil;
    private volatile ActivityHandler mHandler;
    private volatile AlertDialog mUsedInAnotherDeviceDialog;
    private final MSLAuthCallbackFactory.AuthResultCallback mAuthResultCallback =
            new MSLAuthCallbackFactory.AuthResultCallback() {
                @Override
                public void getAuthResult(
                        AuthenticationResult result,
                        MSLAuthCallbackFactory.MSLResultCallback callback) {
                    if (result == null) {
                        LogUtil.logError(
                                TAG,
                                "getAuthResult(), result is null. it imply auth fail. logging out");
                        clearAllUserData();
                        return;
                    }
                    if (callback != null) {
                        callback.onCall();
                    }
                    LogUtil.logInfo(TAG, " auth success");

                    // We can only print log while upload trust contacts failed in background
                    // Therefore, we should try via this activity's onCreate
                    Context context = ZionSkrSdkManager.getInstance().getAppContext();
                    if (context == null) {
                        LogUtil.logError(TAG, "context is null");
                    } else {
                        DriveJobIntentService.enqueueUpload(context,
                                JobIdManager.JOB_ID_SKR_UPLOAD_TRUST_CONTACTS);
                    }
                }

                @Override
                public void onCancel() {
                }
            };
    private MSALAuthenticationCallback mMSALAuthenticationCallback =
            MSLAuthCallbackFactory.getMSALAuthenticationCallback(
                    mAuthResultCallback, null, SocialBackupActivity.this);

    @DriveServiceType
    private int mDriveServiceType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_social_backup);
        mIntent = getIntent();
        mHandler = new ActivityHandler(this);
        mDriveServiceType = DriveUtil.getServiceType(this);
        if (mDriveServiceType == DriveServiceType.oneDrive) {
            final String address = mIntent.getStringExtra(BackupSourceConstants.ADDRESS);
            oneDriveAuth(address);
            mDriveDataUpdateUtil = new DriveDataUpdateUtil(this);
        } else {
            // We can only print log while upload trust contacts failed in background
            // Therefore, we should try via this activity's onCreate
            DriveJobIntentService.enqueueUpload(this,
                    JobIdManager.JOB_ID_SKR_UPLOAD_TRUST_CONTACTS);
        }
        initViews();

        int msgServiceType = ZionSkrSdkManager.getInstance().getMessageServiceType();
        boolean isUsedPushy = (msgServiceType == MessageServiceType.WHISPER)
                || (msgServiceType == MessageServiceType.MULTI);
        if (ZionSkrSdkManager.getInstance().getIsDebug() && isUsedPushy
                && !Pushy.isRegistered(this)) {
            checkExternalStoragePermission();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        adjustSharingButtonStatus();
        adjustToolbarBackupStatus();
        setupBottomRequestView();
        setupContactsRecyclerView();
        setupBroadcastReceiver();
        // Check UUID match
        checkDrive(DriveCheckType.checkUUIDOnFile);
    }

    @Override
    protected void onPause() {
        removeBroadcastReceiver();
        if (mInvitationUtil != null) {
            mInvitationUtil.cancelCountdownTimer();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mUsedInAnotherDeviceDialog != null && mUsedInAnotherDeviceDialog.isShowing()) {
            mUsedInAnotherDeviceDialog.dismiss();
        }

        super.onDestroy();
    }

    // Entry point of oneDrive service
    private void oneDriveAuth(final String address) {
        if (TextUtils.isEmpty(address)) {
            LogUtil.logError(TAG, "oneDriveAuth(), address is null");
            clearAllUserData();
            return;
        }
        AuthenticationManager mgr = AuthenticationManager.getInstance();
        try {
            User loggingUser = null;
            List<User> users = mgr.getPublicClient().getUsers();
            for (User user : users) {
                if (address.equals(user.getDisplayableId())) {
                    loggingUser = user;
                    break;
                }
            }
            if (loggingUser != null) {
                mgr.callAcquireTokenSilent(loggingUser, true, mMSALAuthenticationCallback);
            } else {
                LogUtil.logError(TAG, "no logging user, logging out");
                clearAllUserData();
            }

        } catch (MsalClientException e) {
            LogUtil.logError(TAG, "MSAL Exception Generated while getting users: " + e);
        } catch (IndexOutOfBoundsException e) {
            LogUtil.logError(TAG, "User at this position does not exist: " + e);
        } catch (IllegalStateException e) {
            LogUtil.logError(TAG, "MSAL Exception Generated: " + e);
        } catch (Exception e) {
            LogUtil.logError(TAG, "Other Exception: " + e);
        }
    }

    private void setupBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_TRIGGER_BROADCAST);
        filter.addAction(ACTION_CHECK_DRIVE);
        filter.addAction(ACTION_DRIVE_AUTH);
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        mBackupTargetsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intent) {
                if (context == null) {
                    LogUtil.logError(TAG, "context is null");
                    return;
                } else if (intent == null) {
                    LogUtil.logError(TAG, "intent is null");
                    return;
                }

                String action = intent.getAction();
                if (TextUtils.isEmpty(action)) {
                    LogUtil.logError(TAG, "action is empty");
                    return;
                }

                switch (action) {
                    case ACTION_TRIGGER_BROADCAST:
                        setupContactsRecyclerView();
                        setupBottomRequestView();
                        adjustSharingButtonStatus();
                        adjustToolbarBackupStatus();
                        break;
                    case ACTION_CHECK_DRIVE:
                        // Check UUID match
                        checkDrive(DriveCheckType.checkUUIDOnFile);
                        break;
                    case ACTION_DRIVE_AUTH:
                        if (mDriveServiceType == DriveServiceType.oneDrive) {
                            MSLAuthCallbackFactory.MSLResultCallback callback =
                                    new MSLAuthCallbackFactory.MSLResultCallback() {
                                        @Override
                                        public void onCall() {
                                            DriveJobIntentService.enqueueUpload(
                                                    context,
                                                    JobIdManager.JOB_ID_SKR_UPLOAD_TRUST_CONTACTS);
                                        }
                                    };
                            mMSALAuthenticationCallback =
                                    MSLAuthCallbackFactory.getMSALAuthenticationCallback(
                                            mAuthResultCallback,
                                            callback,
                                            SocialBackupActivity.this);
                            String address = PhoneUtil.getSKREmail(context);
                            oneDriveAuth(address);
                        } else {
                            LogUtil.logError(TAG, "Receive ACTION_DRIVE_AUTH, mDriveServiceType="
                                    + mDriveServiceType);
                        }
                        break;
                    default:
                        LogUtil.logError(TAG, "unknown action=" + action);
                }
            }
        };
        localBroadcastManager.registerReceiver(mBackupTargetsReceiver, filter);
    }

    private void removeBroadcastReceiver() {
        if (mBackupTargetsReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBackupTargetsReceiver);
            mBackupTargetsReceiver = null;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem infoMenuItem = menu.findItem(R.id.action_social_toolbar_backup_info);
        RelativeLayout infoMenuView = (RelativeLayout) infoMenuItem.getActionView();
        infoMenuView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(infoMenuItem);
            }
        });

        final MenuItem moreMenuItem = menu.findItem(R.id.action_more_info);
        RelativeLayout moreMenuView = (RelativeLayout) moreMenuItem.getActionView();
        moreMenuView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(moreMenuItem);
            }
        });
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_social_backup, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            onBackPressed();

        } else if (i == R.id.action_social_toolbar_backup_info) {
            Intent introIntent = new Intent(this, SocialBackupIntroductionActivity.class);
            Bundle bundle = new Bundle();
            bundle.putBoolean(SOCIAL_BACKUP_INFO_CLICK, true);
            introIntent.putExtras(bundle);
            startActivity(introIntent);

        } else if (i == R.id.action_more_info) {
            popMoreMenu();

        } else {
        }
        return true;
    }

    private void popMoreMenu() {
        View popMenuView =
                LayoutInflater.from(this).inflate(R.layout.menu_socialkm_popwindow_layout, null);
        final PopupWindow popupWindow =
                new PopupWindow(
                        popMenuView,
                        getResources().getDimensionPixelSize(R.dimen.popupmenu_width),
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setAnimationStyle(R.style.DropDownDownTopCenter);
        popupWindow.setElevation(20F);
        popupWindow.showAsDropDown(mToolbar, -20, -20, Gravity.END);
        popMenuView.findViewById(R.id.menu_remove_trust_contact)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent removeIntent = new Intent(
                                SocialBackupActivity.this,
                                RemoveTrustContactActivity.class);
                        startActivity(removeIntent);
                        popupWindow.dismiss();
                    }
                });

        popMenuView.findViewById(R.id.menu_check_backup_status)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Map<String, String> msgToSend = new ArrayMap<>();
                        msgToSend.put(KEY_CHECK_BACKUP_HEALTH, MSG_CHECK_BACKUP_STATUS);
                        new BackupHealthCheckAction().send(view.getContext(), MULTI_TOKEN,
                                MULTI_WHISPER_PUB, MULTI_PUSHY_TOKEN, msgToSend);
                        BackupUserManualCheckJobService.schedule(getApplicationContext());
                        popupWindow.dismiss();
                    }
                });

        popMenuView.findViewById(R.id.menu_log_out)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        // show loading view
                        setLoadingViewVisibility(true);

                        WalletSdkUtil.enqueue(new WalletSdkUtil.TzWorkItem() {
                            @Override
                            public void run() {
                                try {
                                    HtcWalletSdkManager htcWalletSdkManager =
                                            HtcWalletSdkManager.getInstance();
                                    int initRet = htcWalletSdkManager.init(getBaseContext());

                                    if (initRet == RESULT.SUCCESS) {
                                        long uniqueId = WalletSdkUtil.getUniqueId(getBaseContext());
                                        int ret = HtcWalletSdkManager.getInstance()
                                                .confirmPIN(uniqueId, RES_SIGN_OUT);
                                        if (ret == RESULT.SUCCESS) {
                                            if (mHandler != null) {
                                                mHandler.sendEmptyMessage(RES_SIGN_OUT);
                                            }
                                        }
                                    } else {
                                        LogUtil.logError(TAG, "init failed, ret = " + initRet);
                                    }
                                } catch (RuntimeException e) {
                                    LogUtil.logError(TAG, "confirmPIN, error  = " + e);
                                } finally {
                                    // hide loading view
                                    setLoadingViewVisibility(false);
                                }
                            }
                        });
                        popupWindow.dismiss();
                    }
                });
    }

    private void initViews() {
        setupToolbar();
        setupInvitationContentView();
        setupBottomRequestView();
        cancelNotification(this, null, NOTIFICATION_ID_SOCIAL_BACKUP);
    }

    private void setupToolbar() {
        mToolbar = findViewById(R.id.tb_social_backup_toolbar);
        setSupportActionBar(mToolbar);
        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onBackPressed();
                    }
                });
    }

    private void setupInvitationContentView() {
        TextView ownerText = findViewById(R.id.tv_invitation_owner);
        final String backupSourceName = mIntent.getStringExtra(BackupSourceConstants.NAME);
        final String address = mIntent.getStringExtra(BackupSourceConstants.ADDRESS);
        mInvitationUtil = new InvitationUtil(SocialBackupActivity.this, backupSourceName, address);

        final String ownerName =
                String.format(
                        getResources().getString(R.string.backup_invitation_hi), backupSourceName);
        ownerText.setText(ownerName);

        mBtnSharingLink = findViewById(R.id.btn_invitation_sharing);

        mBtnSharingLink.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // To make sure that sharing link without backupTargetName (which is added
                        // by resend flow)
                        mInvitationUtil.clearBackupTargetName();
                        mInvitationUtil.sharingInvitation();
                    }
                });
    }

    private void adjustBottomRequestView(boolean show) {
        View requestShow = findViewById(R.id.rl_social_backup_bottom_request_show);
        if (show) {
            requestShow.setVisibility(View.VISIBLE);
        } else {
            requestShow.setVisibility(View.GONE);
        }
    }

    @UiThread
    private void refreshRequestShowNumView(int requestNumber) {
        if (requestNumber > 0) {
            RelativeLayout rlRequestShow = findViewById(R.id.rl_request_show);
            mRequestShowNum = findViewById(R.id.tv_backup_target_request_num);
            if (requestNumber > 99) {
                mRequestShowNum.setText(
                        String.format(getString(R.string.backup_bottom_request_show_num), "99+"));
            } else {
                mRequestShowNum.setText(
                        String.format(getString(R.string.backup_bottom_request_show_num),
                                String.valueOf(requestNumber)));
            }
            rlRequestShow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Intent intent = new Intent(getBaseContext(),
                            SocialKeyRecoveryRequestActivity.class);
                    startActivity(intent);
                }
            });
            adjustBottomRequestView(true);
        } else {
            adjustBottomRequestView(false);
        }
    }

    private void setupBottomRequestView() {
        BackupDataStatusUtil.getRequestsCount(this, new Callback<Integer>() {
            @Override
            public void onResponse(final Integer count) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshRequestShowNumView(count);
                    }
                });
            }
        });
    }


    private void setupContactsRecyclerView() {
        mRecyclerView = findViewById(R.id.recyclerView);
        mEmptyText = findViewById(R.id.tv_null_contact);
        BackupTargetUtil.getAll(
                this,
                new LoadListListener() {
                    @Override
                    public void onLoadFinished(
                            List<BackupSourceEntity> backupSourceEntityList,
                            final List<BackupTargetEntity> backupTargetEntityList,
                            List<RestoreSourceEntity> restoreSourceEntityList,
                            List<RestoreTargetEntity> restoreTargetEntityList) {
                        if (backupTargetEntityList == null || backupTargetEntityList.isEmpty()) {
                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            hideTrustContactList();
                                        }
                                    });
                        } else {
                            final boolean isBackupFull = BackupTargetUtil.getFreeSeedIndex(
                                    SocialBackupActivity.this).isEmpty();
                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            showTrustContactList(backupTargetEntityList,
                                                    isBackupFull);
                                        }
                                    });
                        }
                    }
                });
    }

    private void adjustToolbarBackupStatus() {
        final ImageView toolbarStatusIcon = findViewById(R.id.iv_status_icon);
        final TextView toolbarSubTitle = findViewById(R.id.tv_toolbar_sub_title);
        final Drawable noActiveDrawable =
                ContextCompat.getDrawable(
                        this, R.drawable.shape_social_backup_toolbar_no_active_status_icon);
        final Drawable activeDrawable =
                ContextCompat.getDrawable(
                        this, R.drawable.shape_social_backup_toolbar_active_status_icon);
        final String noActiveText =
                getResources().getString(R.string.backup_toolbar_sub_title_no_active_status);
        final String activeText =
                getResources().getString(R.string.backup_toolbar_sub_title_active_status);

        BackupTargetUtil.getAllOK(this, new LoadListListener() {
            @Override
            public void onLoadFinished(
                    List<BackupSourceEntity> backupSourceEntityList,
                    List<BackupTargetEntity> backupTargetEntityList,
                    List<RestoreSourceEntity> restoreSourceEntityList,
                    List<RestoreTargetEntity> restoreTargetEntityList) {
                final int statusOkNumber = backupTargetEntityList.size();
                if (statusOkNumber >= STATUS_OK_BACKUP_TARGETS_THRESHOLD) {
                    runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    toolbarStatusIcon.setBackground(activeDrawable);
                                    toolbarSubTitle.setText(activeText);
                                    adjustWarningVis(false);
                                }
                            });

                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toolbarStatusIcon.setBackground(noActiveDrawable);
                            toolbarSubTitle.setText(noActiveText);
                            adjustWarningVis(true);
                        }
                    });
                }

                if (SkrSharedPrefs.getShouldShowSkrSecurityUpdateHeaderV1(
                        SocialBackupActivity.this)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideBackupWarningHeaderAndKeepViewHeight();
                        }
                    });
                }
            }
        });
    }

    private void adjustWarningVis(boolean show) {
        final RelativeLayout rlBackupWarning = findViewById(R.id.rl_social_backup_warning);
        final RelativeLayout rlBackupInvitation = findViewById(R.id.rl_social_backup_invitation);
        RelativeLayout.LayoutParams lp =
                (RelativeLayout.LayoutParams) rlBackupInvitation.getLayoutParams();
        if (show) {
            rlBackupWarning.setVisibility(View.VISIBLE);
            TextView title = rlBackupWarning.findViewById(R.id.tv_warning_title);
            adjustTitleViewHeight(title, rlBackupWarning);
            lp.addRule(RelativeLayout.BELOW, R.id.rl_social_backup_warning);
        } else {
            rlBackupWarning.setVisibility(View.GONE);
            lp.addRule(RelativeLayout.BELOW, R.id.app_layout_social_backup_toolbar);
        }
    }

    @UiThread
    private void hideBackupWarningHeaderAndKeepViewHeight() {
        // Copy from adjustWarningVis, keep layout high but set rlBackupWarning invisible
        final RelativeLayout rlBackupWarning = findViewById(R.id.rl_social_backup_warning);
        final RelativeLayout rlBackupInvitation = findViewById(R.id.rl_social_backup_invitation);
        RelativeLayout.LayoutParams lp =
                (RelativeLayout.LayoutParams) rlBackupInvitation.getLayoutParams();
        rlBackupWarning.setVisibility(View.INVISIBLE);
        TextView title = rlBackupWarning.findViewById(R.id.tv_warning_title);
        adjustTitleViewHeight(title, rlBackupWarning);
        lp.addRule(RelativeLayout.BELOW, R.id.rl_social_backup_warning);
    }

    private void adjustTitleViewHeight(
            final TextView titleView, final RelativeLayout relativeLayout) {
        titleView.post(new Runnable() {
            @Override
            public void run() {
                int lineCount = titleView.getLineCount();
                float height = getResources().getDimension(
                        R.dimen.skr_warning_header_height_two_line);
                if (lineCount >= WARNING_HEADER_MAX_LINES) {
                    height = getResources().getDimension(
                            R.dimen.skr_warning_header_height_three_line);
                }

                ValueAnimator anim = ValueAnimator.ofFloat(0, height);
                anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        Float value = (Float) valueAnimator.getAnimatedValue();
                        relativeLayout.getLayoutParams().height = value.intValue();
                        relativeLayout.requestLayout();
                    }
                });
                anim.setDuration(ANIMATION_DURATION);
                anim.start();
            }
        });
    }

    private void adjustSharingButtonStatus() {
        BackupTargetUtil.getAll(this, new LoadListListener() {
            @Override
            public void onLoadFinished(
                    List<BackupSourceEntity> backupSourceEntityList,
                    List<BackupTargetEntity> backupTargetEntityList,
                    List<RestoreSourceEntity> restoreSourceEntityList,
                    List<RestoreTargetEntity> restoreTargetEntityList) {
                final boolean hasFreeSeedIndex = !BackupTargetUtil.getFreeSeedIndex(
                        SocialBackupActivity.this).isEmpty();
                boolean hasBadTarget = false;
                for (BackupTargetEntity backupTarget : backupTargetEntityList) {
                    if (backupTarget.compareStatus(BACKUP_TARGET_STATUS_BAD)) {
                        hasBadTarget = true;
                        break;
                    }
                }
                mBtnSharingLink.setEnabled(hasFreeSeedIndex && !hasBadTarget);
            }
        });
    }

    private void checkDrive(@DriveCheckType int checkType) {
        final String address = mIntent.getStringExtra(BackupSourceConstants.ADDRESS);
        if (TextUtils.isEmpty(address)) {
            LogUtil.logError(TAG, "checkDrive(), address is null");
            return;
        }

        if (!NetworkUtil.isNetworkConnected(getBaseContext())) {
            LogUtil.logInfo(TAG, "Network not available, skip check drive");
            return;
        }

        final DriveUtil util = DriveUtilFactory.getDriveUtil(this, address);

        switch (checkType) {
            case DriveCheckType.checkUUIDOnFile:
                // Check uuid on local file at first, because onedrive sync isn't immediate
                // And then check uuid on drive regularly
                if (mDriveServiceType == DriveServiceType.oneDrive) {
                    if (DriveUtil.isUUIDHashDifferentFromLocalFile(getBaseContext())) {
                        LogUtil.logInfo(TAG, "UUID different, logging out...");
                        clearAllUserData();
                    } else {
                        LogUtil.logInfo(TAG,
                                "checkUUIDOnFile, time: " + System.currentTimeMillis());
                        if (mDriveDataUpdateUtil != null) {
                            mDriveDataUpdateUtil.startSyncDriveData();
                        }
                    }
                } else {
                    checkDrive(DriveCheckType.checkUUIDOnDrive);
                }
                break;
            case DriveCheckType.saveTrustContacts:
                // Move to background (DriveJobIntentService)
                LogUtil.logWarning(TAG, "saveTrustContacts is move to background");
                break;
            case DriveCheckType.checkUUIDOnDrive:
                util.loadUUIDHash(new DriveCallback<String>() {
                    @Override
                    public void onComplete(String message) {
                        if (DriveUtil.isUUIDHashDifferentFromDrive(SocialBackupActivity.this,
                                message)) {
                            LogUtil.logInfo(TAG, "UUID different, logging out...");
                            clearAllUserData();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        LogUtil.logError(TAG, "checkUUIDByDrive(), error = " + e);
                        // Only DriveUtilException should clear all user data
                        if (e instanceof DriveUtilException) {
                            LogUtil.logInfo(TAG,
                                    "It's instanceof DriveUtilException, clearAllUserData");
                            clearAllUserData();
                        }
                    }
                });
                break;
        }
    }

    private void clearAllUserData() {
        SkrSharedPrefs.clearAllSocialKeyRecoveryData(getBaseContext());
        BackupTargetUtil.clearData(getBaseContext());
        if (mDriveDataUpdateUtil != null) {
            mDriveDataUpdateUtil.cancelSyncDriveData();
        }
        DriveUtil.removeLocalFiles();
        showUsedInAnotherDeviceDialog();
    }

    private void showUsedInAnotherDeviceDialog() {
        if (!this.isFinishing()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mUsedInAnotherDeviceDialog == null) {
                        synchronized (SocialBackupActivity.class) {
                            if (mUsedInAnotherDeviceDialog == null) {
                                mUsedInAnotherDeviceDialog = new AlertDialog.Builder(
                                        SocialBackupActivity.this)
                                        .setTitle(R.string.social_backup_used_another_device_title)
                                        .setMessage(
                                                R.string.social_backup_used_another_device_content)
                                        .setPositiveButton(R.string.Button_ok,
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog,
                                                            int which) {
                                                        finish();
                                                        dialog.dismiss();
                                                    }
                                                })
                                        .setCancelable(false)
                                        .create();
                            }
                        }
                    }
                    if (!mUsedInAnotherDeviceDialog.isShowing()) {
                        mUsedInAnotherDeviceDialog.show();
                    }
                }
            });
        }
    }

    private void showTrustContactList(@NonNull final List<BackupTargetEntity> backupTargets,
            final boolean isBackupFull) {
        mRecyclerView.setVisibility(View.VISIBLE);
        mEmptyText.setVisibility(View.GONE);
        TrustContactListAdapter adapter = setTrustContactListAdapter(backupTargets, isBackupFull);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(adapter);
    }

    private void hideTrustContactList() {
        mRecyclerView.setVisibility(View.GONE);
        mEmptyText.setVisibility(View.VISIBLE);
    }

    private TrustContactListAdapter setTrustContactListAdapter(
            @NonNull final List<BackupTargetEntity> backupTargets, final boolean isBackupFull) {
        return new TrustContactListAdapter(this, backupTargets, isBackupFull, mInvitationUtil,
                new RecyclerViewClickListener() {
                    @Override
                    public void onItemClick(View view, int type, int position) {
                        if (type == TYPE_BACKUP_TARGET) {
                            BackupTargetEntity backupTarget = backupTargets.get(position);
                            if (backupTarget != null) {
                                if (backupTarget.isStatusPending()) {
                                    final Intent intent = IntentUtil.generateBackupTargetIntent(
                                            getBaseContext(), backupTarget);
                                    startActivity(intent);
                                }
                            }
                        } else {
                            LogUtil.logError(
                                    TAG, "Wrong type = " + type, new IllegalStateException());
                        }
                    }
                });
    }

    private void handleActivityMessage(android.os.Message message) {
        if (message == null) {
            LogUtil.logError(TAG, "handleActivityMessage(), message is null");
            return;
        }

        switch (message.what) {
            case RES_SIGN_OUT:
                LogUtil.logInfo(TAG, "message = " + message.what + ", logging out...");
                SkrSharedPrefs.clearAllSocialKeyRecoveryData(this);
                BackupTargetUtil.clearData(this);
                if (mDriveDataUpdateUtil != null) {
                    mDriveDataUpdateUtil.cancelSyncDriveData();
                }
                DriveUtil.removeLocalFiles();
                finish();
                break;
            default:
                LogUtil.logError(TAG, "Unknown message=" + message.what);
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            DriveCheckType.checkUUIDOnFile,
            DriveCheckType.checkUUIDOnDrive,
            DriveCheckType.saveTrustContacts
    })
    private @interface DriveCheckType {
        int checkUUIDOnFile = 1;
        int checkUUIDOnDrive = 2;
        int saveTrustContacts = 3;
    }

    private static class ActivityHandler extends Handler {
        private WeakReference<SocialBackupActivity> mActivity;

        ActivityHandler(SocialBackupActivity activity) {
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

    private void checkExternalStoragePermission() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                LogUtil.logDebug(TAG, "WRITE_EXTERNAL_STORAGE permission is granted");
            } else {
                LogUtil.logDebug(TAG, "WRITE_EXTERNAL_STORAGE permission is denied");
            }
        }
    }
}
