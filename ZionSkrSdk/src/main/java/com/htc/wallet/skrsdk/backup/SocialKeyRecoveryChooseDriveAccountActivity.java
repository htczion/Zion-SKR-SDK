package com.htc.wallet.skrsdk.backup;

import static com.htc.wallet.skrsdk.util.PinCodeConfirmConstant.RES_USE_THIS_ACCOUNT;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.htc.htcwalletsdk.Export.HtcWalletSdkManager;
import com.htc.htcwalletsdk.Export.RESULT;
import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.SocialBaseActivity;
import com.htc.wallet.skrsdk.backup.adapter.ChooserRadioListAdapter;
import com.htc.wallet.skrsdk.backup.adapter.DriveSelectionAdapter;
import com.htc.wallet.skrsdk.backup.adapter.GoogleAccountsListAdapter;
import com.htc.wallet.skrsdk.backup.constants.BackupSourceConstants;
import com.htc.wallet.skrsdk.backup.constants.RestoreChooserConstants;
import com.htc.wallet.skrsdk.backup.listener.GoogleRecyclerViewClickListener;
import com.htc.wallet.skrsdk.backup.listener.RestoreChooserListAdapterListener;
import com.htc.wallet.skrsdk.crypto.util.VerificationUtil;
import com.htc.wallet.skrsdk.drives.BackupTargetForDrive;
import com.htc.wallet.skrsdk.drives.DriveCallback;
import com.htc.wallet.skrsdk.drives.DriveServiceType;
import com.htc.wallet.skrsdk.drives.DriveUtil;
import com.htc.wallet.skrsdk.drives.DriveUtilFactory;
import com.htc.wallet.skrsdk.drives.googledrive.GoogleAuthUtil;
import com.htc.wallet.skrsdk.drives.googledrive.OnTokenAcquiredListener;
import com.htc.wallet.skrsdk.drives.onedrive.AuthenticationManager;
import com.htc.wallet.skrsdk.drives.onedrive.MSALAuthenticationCallback;
import com.htc.wallet.skrsdk.drives.onedrive.MSLAuthCallbackFactory;
import com.htc.wallet.skrsdk.drives.onedrive.OneDriveRetryException;
import com.htc.wallet.skrsdk.restore.SocialRestoreActivity;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;
import com.htc.wallet.skrsdk.util.ProgressDialogUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;
import com.htc.wallet.skrsdk.util.TimeoutDialogUtil;
import com.htc.wallet.skrsdk.util.WalletSdkUtil;
import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.MsalClientException;
import com.microsoft.identity.client.User;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SocialKeyRecoveryChooseDriveAccountActivity extends SocialBaseActivity {
    private static final Object sLock = new Object();
    private static final String TAG = "SocialKeyRecoveryChooseDriveAccountActivity";
    private static final int LOGIN_TIME_OUT = 60_000; // 60 sec
    private static final int LOGIN_TIME_OUT_INTERVAL = 1000;

    private static final int RESTORE_SOURCES_THRESHOLD = 3;
    private static final int PERMISSION_REQUEST = 10000;
    private static final int UNSPECIFIED_FOR_CHOOSER_DIALOG = -1;
    private static final int CONFIRM_FOR_CHOOSER_DIALOG = 2;
    private static final int CANCEL_FOR_CHOOSER_DIALOG = 3;
    private static final int GOOGLE_FOR_DRIVE_SERVICE_DIALOG = 0;
    private static final int ONEDRIVE_FOR_DRIVE_SERVICE_DIALOG = 1;
    private static final int REQUEST_CODE_GOOGLE_SIGN_IN = 10005;
    // com.microsoft.identity.client.InteractiveRequest#BROWSER_FLOW
    private static final int REQUEST_CODE_ONE_DRIVE_ADD_ACCOUNT = 1001;

    private static final String GOOGLE_APP_PKG_NAME = "com.google.android.googlequicksearchbox";
    private static final String ONE_DRIVE_PKG_NAME = "com.microsoft.skydrive";
    private final ArrayList<Account> mGoogleAccounts = new ArrayList<>();
    private final ArrayList<DriveAccount> mDriveAccounts = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private volatile Account mChosenAccount;
    private volatile DriveAccount mChosenDriveAccount;
    private ProgressDialog mProgressDialog;
    private ProgressDialogUtil mProgressDialogUtil;
    private Button mServiceSwitchBtn;
    private final OnAccountsUpdateListener mAccountsUpdateListener =
            new OnAccountsUpdateListener() {
                @Override
                public void onAccountsUpdated(Account[] accounts) {
                    mProgressDialogUtil.dismiss(mProgressDialog);
                    clearAccounts();
                    fillingDriveAccounts();
                    adjustAccountListView();
                }
            };
    private CountDownTimer mCountDownTimer;
    private String mDriveDeviceId;
    private String mBackupTargets;
    private int mChooserPosition = UNSPECIFIED_FOR_CHOOSER_DIALOG;
    private int mDriveServicePosition = UNSPECIFIED_FOR_CHOOSER_DIALOG;
    private volatile AlertDialog mChooserDialog = null;
    private String[] mTitles = new String[4];
    private String[] mSubTitles = new String[4];
    private volatile ActivityHandler mHandler;
    private volatile AlertDialog mRestoreNotEnoughDialog = null;
    private volatile AlertDialog mConnectFailedDialog = null;
    private User mUser;
    private boolean mIsAddOneDriveAccountAuth = false;

    // Due to oneDrive, we add prefix on trust contact's filename (1.06)
    // We should check trust contact filename with/without prefix
    private boolean mIsTrustContactsNeedDoubleCheck = false;
    private boolean mIsUUIDNeedDoubleCheck = false;
    private final MSLAuthCallbackFactory.AuthResultCallback mAuthResultCallback =
            new MSLAuthCallbackFactory.AuthResultCallback() {
                @Override
                public void getAuthResult(
                        final AuthenticationResult result,
                        MSLAuthCallbackFactory.MSLResultCallback callback) {
                    if (result == null) {
                        LogUtil.logError(TAG, "getAuthResult(), result is null");
                        return;
                    }

                    LogUtil.logDebug(TAG, "auth expire on : " + result.getExpiresOn());
                    if (callback != null) {
                        callback.onCall();
                    }

                    clearAccountList();
                    fillingDriveAccounts();
                    adjustAccountListView();
                    setupRSAKeyPair(
                            new RSACallback() {
                                @Override
                                public void onComplete() {
                                    mUser = result.getUser();
                                    if (!mIsAddOneDriveAccountAuth) {
                                        if (isRestoreFlow()) {
                                            checkDrive(
                                                    mUser.getDisplayableId(),
                                                    DriveCheckType.checkUUIDInRestoreFlow);
                                        } else {
                                            checkDrive(
                                                    mUser.getDisplayableId(),
                                                    DriveCheckType.checkUUIDInBackupFlowAtFirst);
                                        }
                                    } else {
                                        if (mProgressDialog != null
                                                && mProgressDialog.isShowing()) {
                                            mProgressDialogUtil.dismiss(mProgressDialog);
                                        }
                                    }
                                }
                            });
                }

                @Override
                public void onCancel() {
                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialogUtil.dismiss(mProgressDialog);
                    }
                }
            };
    private MSALAuthenticationCallback mMSALAuthenticationCallback =
            MSLAuthCallbackFactory.getMSALAuthenticationCallback(
                    mAuthResultCallback, null, SocialKeyRecoveryChooseDriveAccountActivity.this);
    private @DriveServiceType
    int mDriveServiceType = DriveServiceType.googleDrive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_social_backup_choose_google_drive);
        checkPermissions();
        initViews();
        fillingDriveAccounts();
        adjustAccountListView();
        setupAccountUpdatedListener();
        mHandler = new ActivityHandler(this);
        DriveUtil.putServiceType(this, DriveServiceType.googleDrive);
    }

    @Override
    protected void onResume() {
        super.onResume();
        clearAccountList();
        fillingDriveAccounts();
        adjustAccountListView();
    }

    @Override
    protected void onPause() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
        clearRestoreNotEnoughDialog();
        clearConnectFailedDialog();
        clearChooserDialog();
        mProgressDialogUtil.dismiss(mProgressDialog);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mAccountsUpdateListener != null) {
            AccountManager.get(this).removeOnAccountsUpdatedListener(mAccountsUpdateListener);
        }
        SkrSharedPrefs.putSocialKMFlowType(this, String.valueOf(FlowType.undefined));
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_CODE_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);
            try {
                mProgressDialog = mProgressDialogUtil.show();
                GoogleSignInAccount account = task.getResult(ApiException.class);
                googleSignInSilently(account.getEmail());
            } catch (ApiException apiException) {
                LogUtil.logError(TAG, "onActivityResult(), error = " + apiException);
                mProgressDialogUtil.dismiss(mProgressDialog);
            }
        } else if (requestCode == REQUEST_CODE_ONE_DRIVE_ADD_ACCOUNT) {
            if (AuthenticationManager.getInstance().getPublicClient() != null) {
                mProgressDialog = mProgressDialogUtil.show();
                AuthenticationManager.getInstance().getPublicClient()
                        .handleInteractiveRequestRedirect(requestCode, resultCode, intent);
            }
        } else {
            LogUtil.logWarning(TAG, "onActivityResult, unknown requestCode=" + requestCode);
        }
    }

    @Override
    public void onBackPressed() {
        DriveUtil.putServiceType(this, DriveServiceType.undefined);
        super.onBackPressed();
    }

    private void initViews() {
        setupProgressDialog();
        setupToolbar();
        mRecyclerView = findViewById(R.id.lv_accounts);
        ((Button) findViewById(R.id.btn_cancel))
                .setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                onBackPressed();
                            }
                        });
        ((Button) findViewById(R.id.btn_continue))
                .setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (mChosenDriveAccount == null) {
                                    return;
                                }

                                // Reset to init value (false)
                                mIsTrustContactsNeedDoubleCheck = false;
                                mIsUUIDNeedDoubleCheck = false;

                                mProgressDialog = mProgressDialogUtil.show();
                                startCountdownTimer();
                                if (mDriveServiceType == DriveServiceType.googleDrive) {
                                    googleSignInSilently(mChosenDriveAccount.getEmail());
                                } else if (mDriveServiceType == DriveServiceType.oneDrive) {
                                    oneDriveAuth();
                                }
                            }
                        });
        mServiceSwitchBtn = (Button) findViewById(R.id.btn_switch_service);
        mServiceSwitchBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setupDriveServiceDialog();
                    }
                });
        Drawable narrow_drawable =
                DrawableCompat.wrap(getResources().getDrawable(R.drawable.common_expand_small));
        DrawableCompat.setTintList(narrow_drawable, ColorStateList.valueOf(Color.WHITE));
        mServiceSwitchBtn.setCompoundDrawablesWithIntrinsicBounds(
                null, null, narrow_drawable, null);

        if (isRestoreFlow()) {
            ((TextView) findViewById(R.id.tv_title))
                    .setText(
                            getResources()
                                    .getString(
                                            R.string.backup_choose_google_drive_title_for_restore));
            ((TextView) findViewById(R.id.tv_sub_title))
                    .setText(
                            getResources()
                                    .getString(
                                            R.string
                                                    .backup_choose_google_drive_sub_title_for_restore));
            SkrSharedPrefs.putSocialKMFlowType(this, String.valueOf(FlowType.restore));
        } else {
            // google as default service
            String googleDriveService =
                    getResources().getString(R.string.service_item_google_drive);
            String subTitle =
                    String.format(
                            getResources().getString(R.string.backup_choose_google_drive_sub_title),
                            googleDriveService);
            ((TextView) findViewById(R.id.tv_sub_title)).setText(subTitle);
        }
        adjustSubTitle();
    }

    private void setupProgressDialog() {
        String dialogTitle =
                getResources().getString(R.string.backup_choose_google_drive_dialog_title);
        String dialogContent =
                getResources().getString(R.string.backup_choose_google_drive_dialog_content);
        mProgressDialogUtil = new ProgressDialogUtil(this, dialogTitle, dialogContent);
    }

    private void clearAccountList() {
        mGoogleAccounts.clear();
        mDriveAccounts.clear();
    }

    private void fillingDriveAccounts() {
        if (mDriveServiceType == DriveServiceType.oneDrive) {
            AuthenticationManager mgr = AuthenticationManager.getInstance();
            try {
                List<User> users = mgr.getPublicClient().getUsers();
                if (users != null && users.size() > 0) {
                    for (User user : users) {
                        DriveAccount googleAccount =
                                new DriveAccount(user.getDisplayableId(), user.getDisplayableId());
                        mDriveAccounts.add(googleAccount);
                    }
                }
            } catch (MsalClientException e) {
                LogUtil.logError(TAG, "fillingDriveAccounts(), MsalClientException, error = " + e);
            }
        } else {
            // google as default service
            final Pattern emailPattern = Patterns.EMAIL_ADDRESS;
            final Account[] allAccounts = AccountManager.get(this).getAccounts();
            for (Account account : allAccounts) {
                if (emailPattern.matcher(account.name).matches()
                        && DriveUtil.hasGooglePattern(account.name)) {
                    mGoogleAccounts.add(account);
                    DriveAccount googleAccount = new DriveAccount(account.name, account.name);
                    mDriveAccounts.add(googleAccount);
                }
            }
        }
        // Add an extra empty google account for item of adding account
        DriveAccount addAccount = new DriveAccount("AddAccount", "AddAccount");
        mDriveAccounts.add(addAccount);
    }

    private void googleAccountAuth(final GoogleSignInAccount signInAccount) {
        GoogleAuthUtil googleAuthUtil =
                new GoogleAuthUtil(SocialKeyRecoveryChooseDriveAccountActivity.this);
        if (signInAccount == null) {
            LogUtil.logError(TAG, "googleAccountAuth(), signInAccount is null");
            return;
        }
        if (TextUtils.isEmpty(signInAccount.getEmail())) {
            LogUtil.logError(TAG, "googleAccountAuth(), email is null");
            return;
        }
        googleAuthUtil.setOnTokenAcquiredListener(
                new OnTokenAcquiredListener() {
                    @Override
                    public void OnTokenAcquiredFinished(boolean isSuccessful) {
                        if (isSuccessful) {
                            setupRSAKeyPair(
                                    new RSACallback() {
                                        @Override
                                        public void onComplete() {
                                            accessGoogleDrive(signInAccount);
                                        }
                                    });

                        } else {
                            mProgressDialogUtil.dismiss(mProgressDialog);
                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            showTimeoutDialog();
                                        }
                                    });
                        }
                        LogUtil.logInfo(TAG, "OnTokenAcquiredFinished: " + isSuccessful);
                    }
                });
        googleAuthUtil.executeAccountAuth(mChosenAccount);
    }

    private void adjustAccountListView() {
        Drawable profileIcon = null;
        try {
            if (mDriveServiceType == DriveServiceType.googleDrive) {
                profileIcon = getPackageManager().getApplicationIcon(GOOGLE_APP_PKG_NAME);
            } else if (mDriveServiceType == DriveServiceType.oneDrive) {
                profileIcon = getPackageManager().getApplicationIcon(ONE_DRIVE_PKG_NAME);
            }
        } catch (PackageManager.NameNotFoundException e) {
            LogUtil.logError(TAG, "adjustAccountListView(), error = " + e);
        }
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(
                new GoogleAccountsListAdapter(
                        this,
                        mDriveAccounts,
                        profileIcon,
                        new GoogleRecyclerViewClickListener() {
                            @Override
                            public void onItemCheck(int position) {
                                if (mDriveAccounts.size() == 0) {
                                    return;
                                }
                                if (checkIfListAccountPosition(position)) {
                                    resetAllCustomGoogleAccountsChosenStatus();
                                    setChosenAccount(position);
                                    adjustAccountListView();
                                } else {
                                    if (mDriveServiceType == DriveServiceType.googleDrive) {
                                        startAddGoogleAccountIntent();
                                    } else if (mDriveServiceType == DriveServiceType.oneDrive) {
                                        startAddOneDriveAccountIntent();
                                    }
                                }
                            }

                            @Override
                            public void onItemUnCheck(int position) {
                                if (mDriveAccounts.size() == 0) {
                                    return;
                                }
                                if (checkIfListAccountPosition(position)) {
                                    resetAllCustomGoogleAccountsChosenStatus();
                                    unsetChosenAccount();
                                    adjustAccountListView();
                                }
                            }
                        }));
    }

    private void setupAccountUpdatedListener() {
        AccountManager.get(this).addOnAccountsUpdatedListener(mAccountsUpdateListener, null, false);
    }

    private void resetAllCustomGoogleAccountsChosenStatus() {
        for (int i = 0; i < mDriveAccounts.size(); i++) {
            DriveAccount googleAccount = mDriveAccounts.get(i);
            googleAccount.setIsChosen(false);
            mDriveAccounts.set(i, googleAccount);
        }
    }

    private void clearAccounts() {
        mDriveAccounts.clear();
        mGoogleAccounts.clear();
    }

    private boolean checkIfListAccountPosition(int position) {
        return mDriveAccounts.size() <= 0 || position < (mDriveAccounts.size() - 1);
    }

    private void setChosenAccount(int position) {
        synchronized (sLock) {
            mChosenDriveAccount = mDriveAccounts.get(position);
            mChosenDriveAccount.setIsChosen(true);
            mDriveAccounts.set(position, mChosenDriveAccount);
            if (mDriveServiceType == DriveServiceType.googleDrive) {
                mChosenAccount = mGoogleAccounts.get(position);
            } else if (mDriveServiceType == DriveServiceType.oneDrive) {
                AuthenticationManager mgr = AuthenticationManager.getInstance();
                try {
                    List<User> users = mgr.getPublicClient().getUsers();
                    if (users != null && users.size() > 0) {
                        for (User user : users) {
                            if (user.getDisplayableId().equals(mChosenDriveAccount.getEmail())) {
                                mUser = user;
                                break;
                            }
                        }
                    }
                    mChosenDriveAccount.setEmail(mUser.getDisplayableId());
                    if (TextUtils.isEmpty(mUser.getName())) {
                        mChosenDriveAccount.setName(mUser.getDisplayableId());
                    } else {
                        mChosenDriveAccount.setName(mUser.getName());
                    }
                } catch (MsalClientException e) {
                    LogUtil.logError(TAG, "setChosenAccount(), MsalClientException, error = " + e);
                }
            }
        }
    }

    private void unsetChosenAccount() {
        synchronized (sLock) {
            mChosenAccount = null;
            mChosenDriveAccount = null;
        }
    }

    private void setupRSAKeyPair(final RSACallback callback) {
        if (callback == null) {
            LogUtil.logError(TAG, "setupRSAKeyPair(), callback is null");
            return;
        }
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        // Pre-produce RSA keys because time of producing 4096 RSA is too
                        // long.
                        VerificationUtil util = new VerificationUtil(false);
                        if (TextUtils.isEmpty(util.getPublicKeyString())) {
                            LogUtil.logWarning(TAG, "creating RSA key pair fails");
                        }
                        callback.onComplete();
                    }
                })
                .start();
    }

    private void startGoogleSignInIntent(final Intent intent) {
        if (intent == null) {
            LogUtil.logError(TAG, "startSignInIntent(), intent is null");
            return;
        }
        startActivityForResult(intent, REQUEST_CODE_GOOGLE_SIGN_IN);
    }

    private void googleSignInSilently(final String googleAddress) {
        if (TextUtils.isEmpty(googleAddress)) {
            LogUtil.logError(TAG, "googleSignInSilently(), googleAddress is empty");
            return;
        }

        // We should check this again, due to user can cancel the error dialog
        int errorCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (errorCode != ConnectionResult.SUCCESS) {
            // Cancel progress dialog if needed
            cancelCountDownTimer();
            mProgressDialogUtil.dismiss(mProgressDialog);
            // Show Google dialog to handle this problem
            showGooglePlayServiceNotAvailableDialog(true, errorCode);
            return;
        }

        final GoogleSignInOptions gso = setupGoogleSignInOptions(googleAddress);
        if (gso == null) {
            LogUtil.logError(TAG, "googleSignInSilently(), gso is null");
            return;
        }
        final GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignInClient.signOut();
        final Task<GoogleSignInAccount> task = googleSignInClient.silentSignIn();
        if (task.isSuccessful()) {
            // There's immediate result available
            try {
                LogUtil.logInfo(TAG, "googleSignInSilently(), immediately success");
                final GoogleSignInAccount signInAccount = task.getResult(ApiException.class);
                googleAccountAuth(signInAccount);
            } catch (ApiException e) {
                LogUtil.logError(
                        TAG, "googleSignInSilently(), task is immediately available, error = " + e);
                // Maybe occur ApiException: 4
                // It implies the client attempt to connect to the service but the user is not
                // signed in.
                // We need to sign in manually before you use silentSignIn
                // https://stackoverflow.com/questions/48923966/apiexception-on-silent-signing
                // -using-googlesigninclient-on-android/50218272

                startGoogleSignInIntent(googleSignInClient.getSignInIntent());
            }
        } else {
            // There's no immediate result ready, waits for the async callback.
            task.addOnCompleteListener(
                    new OnCompleteListener<GoogleSignInAccount>() {
                        @Override
                        public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                            try {
                                LogUtil.logInfo(
                                        TAG, "googleSignInSilently(), not immediately success");
                                final GoogleSignInAccount signInAccount =
                                        task.getResult(ApiException.class);
                                googleAccountAuth(signInAccount);
                            } catch (ApiException e) {
                                LogUtil.logError(
                                        TAG,
                                        "googleSignInSilently(), task isn't immediately "
                                                + "available, error = "
                                                + e);
                                startGoogleSignInIntent(googleSignInClient.getSignInIntent());
                            }
                        }
                    });
        }
    }

    private void oneDriveAuth() {
        final String address = mChosenDriveAccount.getEmail();
        AuthenticationManager mgr = AuthenticationManager.getInstance();
        try {
            final List<User> users = mgr.getPublicClient().getUsers();
            if (users != null && users.size() > 0) {
                for (User user : users) {
                    if (user.getDisplayableId().equals(address)) {
                        mUser = user;
                        break;
                    }
                }
            }
            mIsAddOneDriveAccountAuth = false;
            mgr.callAcquireTokenSilent(mUser, true, mMSALAuthenticationCallback);
        } catch (MsalClientException e) {
            LogUtil.logError(TAG, "fillingDriveAccounts(), MsalClientException, error = " + e);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                LogUtil.logDebug(TAG, "permission is granted");
                clearAccountList();
            } else {
                LogUtil.logDebug(TAG, "permission is denied");
            }
        }
    }

    private void checkPermissions() {
        // AccountManager on O80 https://blog.csdn.net/dzkdxyx/article/details/78821384
        if (checkSelfPermission(Manifest.permission.GET_ACCOUNTS)
                == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(Manifest.permission.WRITE_CONTACTS)
                == PackageManager.PERMISSION_DENIED) {
            String[] permissions = {
                    Manifest.permission.GET_ACCOUNTS,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS
            };
            requestPermissions(permissions, PERMISSION_REQUEST);
        }
    }

    private void accessGoogleDrive(@NonNull final GoogleSignInAccount signInAccount) {
        if (signInAccount == null) {
            LogUtil.logError(TAG, "accessGoogleDrive(), signInAccount is null");
            return;
        }

        final String googleAddress = signInAccount.getEmail();
        if (TextUtils.isEmpty(googleAddress)) {
            LogUtil.logError(TAG, "accessGoogleDrive(), googleAddress is null");
            return;
        }
        try {
            String userName = signInAccount.getDisplayName();
            if (TextUtils.isEmpty(userName)) {
                userName = signInAccount.getGivenName();
            }
            if (TextUtils.isEmpty(userName)) {
                userName = signInAccount.getFamilyName();
            }
            if (TextUtils.isEmpty(userName)) {
                userName = signInAccount.getEmail();
                if (!TextUtils.isEmpty(userName)) {
                    int atIndex = userName.indexOf('@');
                    if (atIndex != -1) {
                        userName = userName.substring(0, atIndex);
                    }
                } else {
                    LogUtil.logError(TAG, "accessGoogleDrive(), userName is null");
                }
            }
            synchronized (sLock) {
                mChosenDriveAccount = new DriveAccount(userName, googleAddress);
            }

            if (isRestoreFlow()) {
                checkDrive(googleAddress, DriveCheckType.checkUUIDInRestoreFlow);
                SkrSharedPrefs.putSocialKMFlowType(this, String.valueOf(FlowType.restore));
            } else {
                checkDrive(googleAddress, DriveCheckType.checkUUIDInBackupFlowAtFirst);
            }
        } catch (Throwable throwable) {
            LogUtil.logError(TAG, "accessGoogleDrive(), Throwable = " + throwable);
        }
    }

    private GoogleSignInOptions setupGoogleSignInOptions(final String googleAddress) {
        if (TextUtils.isEmpty(googleAddress)) {
            LogUtil.logWarning(TAG, "setupGoogleSignInOptions(), googleAddress is null");
            return null;
        }
        return new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .setAccountName(googleAddress)
                .requestScopes(new Scope(Scopes.PROFILE))
                .requestScopes(new Scope(Scopes.EMAIL))
                .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
                .requestScopes(new Scope(Scopes.DRIVE_FULL))
                .build();
    }

    private void startSocialBackupActivity() {
        if (mChosenDriveAccount == null) {
            LogUtil.logError(TAG, "startSocialBackupActivity(), mDriveAccount is null");
            return;
        }
        mProgressDialogUtil.dismiss(mProgressDialog);
        final Intent intent =
                new Intent(
                        SocialKeyRecoveryChooseDriveAccountActivity.this,
                        SocialBackupActivity.class);
        intent.putExtra(BackupSourceConstants.ADDRESS, mChosenDriveAccount.getEmail());
        intent.putExtra(BackupSourceConstants.NAME, mChosenDriveAccount.getName());
        startActivity(intent);
        finish();
    }

    private void setupToolbar() {
        Toolbar mToolbar = findViewById(R.id.tb_social_backup_toolbar);
        setSupportActionBar(mToolbar);
        if (isRestoreFlow()) {
            View toolBarLayout = findViewById(R.id.app_layout_social_backup_google_drive_toolbar);
            toolBarLayout.setVisibility(View.INVISIBLE);
            SkrSharedPrefs.putSocialKMFlowType(this, String.valueOf(FlowType.restore));
        } else {
            ImageView ivBack = findViewById(R.id.iv_back);
            ivBack.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onBackPressed();
                        }
                    });
        }
    }

    private boolean isRestoreFlow() {
        Intent intent = getIntent();
        final String activity = intent.getStringExtra(RestoreChooserConstants.KEY_FROM_ACTIVITY);
        if (TextUtils.isEmpty(activity)
                || !activity.equals(RestoreChooserConstants.VALUE_FROM_ENTRY_ACTIVITY)) {
            final String flowType = SkrSharedPrefs.getSocialKMFlowType(this);
            if (Integer.valueOf(flowType).equals(FlowType.undefined)) {
                return false;
            }
        }
        return true;
    }

    private void storeEmailAndUserName() {
        if (mChosenDriveAccount == null) {
            if (mDriveServiceType == DriveServiceType.googleDrive) {
                LogUtil.logError(
                        TAG, "storeEmailAndUserName(), mChosenCustomGoogleAccount is null");
                return;
            }
        }
        String name;
        String email;
        if (mDriveServiceType == DriveServiceType.oneDrive) {
            if (mUser == null) {
                LogUtil.logError(TAG, "storeEmailAndUserName(), mUser is null");
                return;
            }
            name = mUser.getName();
            if (TextUtils.isEmpty(name)) {
                name = mUser.getDisplayableId();
            }
            email = mUser.getDisplayableId();
        } else {
            name = mChosenDriveAccount.getName();
            email = mChosenDriveAccount.getEmail();
        }

        if (TextUtils.isEmpty(name)) {
            LogUtil.logError(TAG, "storeEmailAndUserName(), name is null");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            LogUtil.logError(TAG, "storeEmailAndUserName(), email is null");
            return;
        }
        SkrSharedPrefs.putSocialKMBackupEmail(getBaseContext(), email);
        SkrSharedPrefs.putSocialKMUserName(getBaseContext(), name);
    }

    private void startAddGoogleAccountIntent() {

        // We should check this again, due to user can cancel the error dialog
        int errorCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (errorCode != ConnectionResult.SUCCESS) {
            showGooglePlayServiceNotAvailableDialog(true, errorCode);
            return;
        }

        Intent addAccountIntent = new Intent(Settings.ACTION_ADD_ACCOUNT);
        addAccountIntent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, new String[]{"com.google"});
        startActivity(addAccountIntent);
    }

    private void startAddOneDriveAccountIntent() {
        mIsAddOneDriveAccountAuth = true;
        AuthenticationManager mgr = AuthenticationManager.getInstance();
        mgr.callAcquireToken(this, mMSALAuthenticationCallback);
    }

    private void startSocialRestoreActivity() {
        mProgressDialogUtil.dismiss(mProgressDialog);
        if (mBackupTargets != null && mDriveDeviceId != null && mChosenDriveAccount != null) {
            Intent intent = new Intent(this, SocialRestoreActivity.class);
            // Restore flow use putCurrentRestoreEmail and getCurrentRestoreEmail
            // But backup and restore flow use the same getGoogleAccountUserName
            // Change to use getSocialKMUserName, due to add OneDrive (2019-04-10)
            SkrSharedPrefs.putSocialKMUserName(
                    getBaseContext(), mChosenDriveAccount.getName());
            intent.putExtra(BackupSourceConstants.BACKUPTARGETS, mBackupTargets);
            intent.putExtra(BackupSourceConstants.NAME, mChosenDriveAccount.getName());
            intent.putExtra(BackupSourceConstants.ADDRESS, mChosenDriveAccount.getEmail());
            intent.putExtra(BackupSourceConstants.UUID_HASH, mDriveDeviceId);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            mBackupTargets = null;
            mDriveDeviceId = null;
            finish();
        }
    }

    private boolean isRestoreSourcesNumberEnough(final String message) {
        if (TextUtils.isEmpty(message)) {
            return false;
        }
        List<BackupTargetForDrive> backupTargetForDrives = DriveUtil.stringToList(message);
        if (backupTargetForDrives == null) {
            LogUtil.logInfo(TAG, "backupTargetForDrives is null");
            return false;
        }

        int legalNameCount = 0;
        for (int i = 0; i < backupTargetForDrives.size(); i++) {
            BackupTargetForDrive backupTargetForDrive = backupTargetForDrives.get(i);
            if (backupTargetForDrive == null) {
                LogUtil.logError(TAG, "backupTargetForDrive is null");
            } else if (TextUtils.isEmpty(backupTargetForDrive.getName())) {
                // Prevent 1.0 issue, the Bob's name in google drive may be null
                LogUtil.logWarning(TAG, "BackupTargetForDrive " + i + "'s name is null or empty");
            } else {
                legalNameCount++;
            }
        }
        return legalNameCount >= RESTORE_SOURCES_THRESHOLD;
    }

    private void showRestoreNotEnoughDialog() {
        cancelCountDownTimer();
        final String title =
                getResources()
                        .getString(
                                R.string
                                        .backup_choose_google_drive_restore_source_not_enough_dialog_title);
        final String message =
                getResources()
                        .getString(
                                R.string
                                        .backup_choose_google_drive_restore_source_not_enough_dialog_content);
        synchronized (sLock) {
            if (mRestoreNotEnoughDialog == null) {
                mRestoreNotEnoughDialog =
                        new AlertDialog.Builder(this)
                                .setTitle(title)
                                .setMessage(message)
                                .setPositiveButton(
                                        R.string.ver_dialog_btn_ok,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int id) {
                                                mProgressDialogUtil.dismiss(mProgressDialog);
                                                mRestoreNotEnoughDialog.dismiss();
                                            }
                                        })
                                .create();
                if (!isFinishing()) {
                    mRestoreNotEnoughDialog.show();
                    TextView messageView =
                            mRestoreNotEnoughDialog.findViewById(android.R.id.message);
                    if (messageView != null) {
                        messageView.setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY);
                        messageView.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
                    }
                }
            } else {
                if (!isFinishing() && !mRestoreNotEnoughDialog.isShowing()) {
                    mRestoreNotEnoughDialog.show();
                }
            }
        }
    }

    private void clearRestoreNotEnoughDialog() {
        synchronized (sLock) {
            if (mRestoreNotEnoughDialog != null) {
                if (mRestoreNotEnoughDialog.isShowing()) {
                    mRestoreNotEnoughDialog.dismiss();
                }
                mRestoreNotEnoughDialog = null;
            }
        }
    }

    private void showConnectFailedDialog() {
        cancelCountDownTimer();
        final String title = getResources().getString(R.string.socialkm_network_connection_title);
        final String message = getResources().getString(R.string.ver_request_timeout);
        synchronized (sLock) {
            if (mConnectFailedDialog == null) {
                mConnectFailedDialog =
                        new AlertDialog.Builder(this)
                                .setTitle(title)
                                .setMessage(message)
                                .setPositiveButton(
                                        R.string.ver_dialog_btn_ok,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int id) {
                                                mProgressDialogUtil.dismiss(mProgressDialog);
                                                mConnectFailedDialog.dismiss();
                                            }
                                        })
                                .create();
                if (!isFinishing()) {
                    mConnectFailedDialog.show();
                }
            } else {
                if (!isFinishing() && !mConnectFailedDialog.isShowing()) {
                    mConnectFailedDialog.show();
                }
            }
        }
    }

    private void clearConnectFailedDialog() {
        synchronized (sLock) {
            if (mConnectFailedDialog != null) {
                if (mConnectFailedDialog.isShowing()) {
                    mConnectFailedDialog.dismiss();
                }
                mConnectFailedDialog = null;
            }
        }
    }

    private void clearChooserDialog() {
        synchronized (sLock) {
            if (mChooserDialog != null) {
                if (mChooserDialog.isShowing()) {
                    mChooserDialog.dismiss();
                }
                mChooserDialog = null;
            }
        }
    }

    private void startCountdownTimer() {
        synchronized (sLock) {
            cancelCountDownTimer();
            mCountDownTimer =
                    new CountDownTimer(LOGIN_TIME_OUT, LOGIN_TIME_OUT_INTERVAL) {
                        @Override
                        public void onFinish() {
                            mProgressDialogUtil.dismiss(mProgressDialog);
                            showTimeoutDialog();
                        }

                        @Override
                        public void onTick(long millisUntilFinished) {
                        }
                    }.start();
        }
    }

    private void showTimeoutDialog() {
        final String title =
                getResources().getString(R.string.backup_restore_login_timeout_dialog_title);
        final String content =
                getResources().getString(R.string.backup_restore_login_timeout_dialog_content);
        TimeoutDialogUtil.setupTimeOutDialog(
                SocialKeyRecoveryChooseDriveAccountActivity.this, title, content);
    }

    private void cancelCountDownTimer() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }

    private void checkDrive(@Nullable final String address, final @DriveCheckType int flowType) {
        if (address == null) {
            LogUtil.logWarning(TAG, "checkDrive(), googleAddress is null");
            if (mDriveServiceType == DriveServiceType.googleDrive) {
                return;
            }
        }
        final DriveUtil util = DriveUtilFactory.getDriveUtil(this, address);

        final DriveCallback<String> loadCallback = setupDriveLoadCallback(flowType);
        final DriveCallback<Void> saveCallback = setupDriveSaveCallback(address, flowType);

        LogUtil.logInfo(TAG, "checkDrive(), flowType: " + flowType);
        switch (flowType) {
            case DriveCheckType.checkUUIDInBackupFlowAtFirst:
            case DriveCheckType.checkUUIDInBackupFlowAgain:
            case DriveCheckType.checkUUIDInRestoreFlow:
                util.loadUUIDHash(loadCallback);
                break;
            case DriveCheckType.checkTrustContactsInRestoreFlow:
                if (!mIsTrustContactsNeedDoubleCheck) {
                    // We use partial uuidHash as trust contacts file name prefix
                    String filePrefix = DriveUtil.getFilePrefix(mDriveDeviceId);
                    if (TextUtils.isEmpty(filePrefix)) {
                        LogUtil.logError(TAG, "filePrefix is empty");
                    }
                    util.loadTrustContacts(filePrefix, loadCallback);
                } else {
                    util.loadTrustContacts(null, loadCallback);
                }
                break;
            case DriveCheckType.saveUUID:
                final String currentUUIDHash = PhoneUtil.getSKRIDHash(this);
                util.saveUUIDHash(currentUUIDHash, saveCallback);
                break;
        }
    }

    private DriveCallback<String> setupDriveLoadCallback(final @DriveCheckType int flowType) {
        return new DriveCallback<String>() {
            @Override
            public void onComplete(final String message) {
                runOnUiThread(
                        new CheckDriveRunnable(
                                SocialKeyRecoveryChooseDriveAccountActivity.this,
                                message,
                                flowType));
            }

            @Override
            public void onFailure(Exception e) {
                LogUtil.logError(TAG, "loadDrive(), error = " + e);
                switch (flowType) {
                    // It can fail for new user, because no data is on drive.
                    case DriveCheckType.checkUUIDInBackupFlowAtFirst:
                        runOnUiThread(
                                new CheckDriveRunnable(
                                        SocialKeyRecoveryChooseDriveAccountActivity.this,
                                        null,
                                        flowType));
                        return;
                    case DriveCheckType.checkUUIDInBackupFlowAgain:
                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        showConnectFailedDialog();
                                    }
                                });
                        return;
                    case DriveCheckType.checkUUIDInRestoreFlow:
                    case DriveCheckType.checkTrustContactsInRestoreFlow:
                        // We need to double check trust contacts on drive using prefix file and
                        // non-prefix file,
                        // because it save trust contacts on drive using non-prefix file in old
                        // version SKR.
                        if (flowType == DriveCheckType.checkTrustContactsInRestoreFlow
                                && !mIsTrustContactsNeedDoubleCheck) {
                            LogUtil.logInfo(TAG, "First check for trust contacts in restore flow");
                            runOnUiThread(
                                    new CheckDriveRunnable(
                                            SocialKeyRecoveryChooseDriveAccountActivity.this,
                                            null,
                                            flowType));
                            return;
                        }
                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        showRestoreNotEnoughDialog();
                                    }
                                });
                        return;
                    default:
                        LogUtil.logError(TAG, "unknown check type");
                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        showConnectFailedDialog();
                                    }
                                });
                        return;
                }
            }
        };
    }

    private DriveCallback<Void> setupDriveSaveCallback(
            final String address, final @DriveCheckType int flowType) {
        if (TextUtils.isEmpty(address)) {
            LogUtil.logError(TAG, "setupDriveSaveCallback(), address is null");
            return null;
        }
        return new DriveCallback<Void>() {
            @Override
            public void onComplete(Void aVoid) {
                LogUtil.logInfo(TAG, "saveDrive(), onComplete");
                runOnUiThread(
                        new CheckDriveRunnable(
                                SocialKeyRecoveryChooseDriveAccountActivity.this,
                                address,
                                flowType));
            }

            @Override
            public void onFailure(Exception e) {
                LogUtil.logError(TAG, "saveDrive(), error = " + e);
                if (e instanceof OneDriveRetryException) {
                    MSLAuthCallbackFactory.MSLResultCallback callback =
                            new MSLAuthCallbackFactory.MSLResultCallback() {
                                @Override
                                public void onCall() {
                                    if (mChosenDriveAccount != null) {
                                        checkDrive(
                                                mChosenDriveAccount.getEmail(),
                                                DriveCheckType.saveUUID);
                                    } else {
                                        LogUtil.logError(
                                                TAG, "saveDrive(), chosenDriveAccount is null");
                                    }
                                }
                            };
                    mMSALAuthenticationCallback =
                            MSLAuthCallbackFactory.getMSALAuthenticationCallback(
                                    mAuthResultCallback,
                                    callback,
                                    SocialKeyRecoveryChooseDriveAccountActivity.this);
                } else {
                    runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    showConnectFailedDialog();
                                }
                            });
                }
            }
        };
    }

    private synchronized void setupChooserDialog() {
        cancelCountDownTimer();
        setupTitleStrings();
        RestoreChooserListAdapterListener listener = new RestoreChooserListAdapterListener() {
            @Override
            public void onItemClick(int position) {
                if (canBeChosenItem(position)) {
                    mChooserPosition = position;
                }
            }
        };

        clearChooserDialog();

        ChooserRadioListAdapter adapter =
                new ChooserRadioListAdapter(this, listener, mTitles, mSubTitles, false);
        mChooserDialog = new AlertDialog.Builder(SocialKeyRecoveryChooseDriveAccountActivity.this)
                .setTitle(R.string.db_account_used)
                .setAdapter(adapter, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mChooserPosition == CONFIRM_FOR_CHOOSER_DIALOG) {

                            // Use progress dialog already
                            // Not need to show loading view here

                            WalletSdkUtil.enqueue(new WalletSdkUtil.TzWorkItem() {
                                @Override
                                public void run() {
                                    HtcWalletSdkManager htcWalletSdkManager =
                                            HtcWalletSdkManager.getInstance();
                                    int initRet = htcWalletSdkManager.init(getBaseContext());

                                    if (initRet == RESULT.SUCCESS) {
                                        long uniqueId = WalletSdkUtil.getUniqueId(getBaseContext());
                                        int ret = HtcWalletSdkManager.getInstance().confirmPIN(
                                                uniqueId, RES_USE_THIS_ACCOUNT);
                                        if (ret == RESULT.SUCCESS) {
                                            if (mHandler != null) {
                                                mHandler.sendEmptyMessage(RES_USE_THIS_ACCOUNT);
                                            }
                                        } else {
                                            LogUtil.logWarning(TAG,
                                                    "confirmPIN failed, ret=" + ret);
                                            mProgressDialogUtil.dismiss(mProgressDialog);
                                        }
                                    } else {
                                        LogUtil.logError(TAG, "init failed, ret=" + initRet);
                                        mProgressDialogUtil.dismiss(mProgressDialog);
                                    }
                                }
                            });
                            mChooserDialog.dismiss();
                        } else if (mChooserPosition == CANCEL_FOR_CHOOSER_DIALOG) {
                            mProgressDialogUtil.dismiss(mProgressDialog);
                            mChooserDialog.dismiss();
                        } else {
                            mProgressDialogUtil.dismiss(mProgressDialog);
                            mChooserDialog.dismiss();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mProgressDialogUtil.dismiss(mProgressDialog);
                        mChooserDialog.dismiss();
                    }
                })
                .setCancelable(false)
                .create();

        mChooserDialog.setCanceledOnTouchOutside(false);
        if (!isFinishing()) {
            mChooserDialog.show();
        }
    }

    private void setupTitleStrings() {
        mTitles[0] = getResources().getString(R.string.db_security_concern);
        mTitles[1] = getResources().getString(R.string.db_still_want_to_use_account);
        mTitles[2] = getResources().getString(R.string.db_use_this_account);
        mTitles[3] = getResources().getString(R.string.db_use_another_account);
        mSubTitles[0] = "";
        mSubTitles[1] = "";
        mSubTitles[2] = getResources().getString(R.string.db_account_cant_be_restore);
        mSubTitles[3] = getResources().getString(R.string.db_keep_previous_and_create_new);
    }

    private boolean canBeChosenItem(int position) {
        return position == CONFIRM_FOR_CHOOSER_DIALOG || position == CANCEL_FOR_CHOOSER_DIALOG;
    }

    private synchronized void setupDriveServiceDialog() {
        cancelCountDownTimer();
        DriveSelectionAdapter.DriveSelectionAdapterCallback callback =
                new DriveSelectionAdapter.DriveSelectionAdapterCallback() {
                    @Override
                    public void onItemClick(int position) {
                        mDriveServicePosition = position;
                    }
                };
        final String[] services = new String[]{
                getResources().getString(R.string.service_item_google_drive),
                getResources().getString(R.string.service_item_one_drive)
        };
        DriveSelectionAdapter adapter = new DriveSelectionAdapter(this, callback, services);

        String title = getResources().getString(R.string.service_selection_dialog_title_backup);
        if (isRestoreFlow()) {
            title = getResources().getString(R.string.service_selection_dialog_title_recover);
        } else {
            title = getResources().getString(R.string.service_selection_dialog_title_backup);
        }

        clearChooserDialog();

        mChooserDialog = new AlertDialog.Builder(SocialKeyRecoveryChooseDriveAccountActivity.this)
                .setTitle(title)
                .setAdapter(adapter, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mDriveServicePosition == GOOGLE_FOR_DRIVE_SERVICE_DIALOG) {
                            mDriveServiceType = DriveServiceType.googleDrive;
                            mServiceSwitchBtn.setText(getResources()
                                    .getString(
                                            R.string
                                                    .service_item_google_drive));
                        } else if (mDriveServicePosition == ONEDRIVE_FOR_DRIVE_SERVICE_DIALOG) {
                            mDriveServiceType = DriveServiceType.oneDrive;
                            mServiceSwitchBtn.setText(getResources()
                                    .getString(
                                            R.string
                                                    .service_item_one_drive));
                        }
                        DriveUtil.putServiceType(getBaseContext(), mDriveServiceType);
                        clearAccountList();
                        fillingDriveAccounts();
                        adjustAccountListView();
                        adjustSubTitle();
                        mProgressDialogUtil.dismiss(mProgressDialog);
                        mChooserDialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mProgressDialogUtil.dismiss(mProgressDialog);
                        mChooserDialog.dismiss();
                    }
                })
                .setCancelable(false)
                .create();
        if (!isFinishing()) {
            mChooserDialog.show();
        }
    }

    private void adjustSubTitle() {
        String subTitle = getResources().getString(R.string.backup_choose_google_drive_sub_title);
        if (isRestoreFlow()) {
            subTitle =
                    getResources()
                            .getString(R.string.backup_choose_google_drive_sub_title_for_restore);
        } else {
            subTitle = getResources().getString(R.string.backup_choose_google_drive_sub_title);
        }
        final String[] services =
                new String[]{
                        getResources().getString(R.string.service_item_google_drive),
                        getResources().getString(R.string.service_item_one_drive)
                };

        if (mDriveServicePosition == GOOGLE_FOR_DRIVE_SERVICE_DIALOG) {
            subTitle = String.format(subTitle, services[0]);
        } else if (mDriveServicePosition == ONEDRIVE_FOR_DRIVE_SERVICE_DIALOG) {
            subTitle = String.format(subTitle, services[1]);
        } else {
            // google as default service
            subTitle = String.format(subTitle, services[0]);
        }
        ((TextView) findViewById(R.id.tv_sub_title)).setText(subTitle);
    }

    private void handleActivityMessage(android.os.Message message) {
        switch (message.what) {
            case RES_USE_THIS_ACCOUNT:
                if (mChosenDriveAccount == null) {
                    LogUtil.logError(TAG, "reuse this account, mChosenCustomGoogleAccount is null");
                    return;
                }
                if (TextUtils.isEmpty(mChosenDriveAccount.getEmail())) {
                    LogUtil.logError(TAG, "reuse this account, email is null");
                    return;
                }
                if (mProgressDialogUtil != null
                        && mProgressDialog != null
                        && !mProgressDialog.isShowing()) {
                    runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    mProgressDialog = mProgressDialogUtil.show();
                                }
                            });
                }
                checkDrive(mChosenDriveAccount.getEmail(), DriveCheckType.saveUUID);
                break;
        }
    }

    private interface RSACallback {
        void onComplete();
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            FlowType.backup,
            FlowType.restore,
    })
    private @interface FlowType {
        int undefined = -1;
        int backup = 1;
        int restore = 2;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            DriveCheckType.checkUUIDInBackupFlowAtFirst,
            DriveCheckType.checkUUIDInBackupFlowAgain,
            DriveCheckType.checkUUIDInRestoreFlow,
            DriveCheckType.checkTrustContactsInRestoreFlow,
            DriveCheckType.saveUUID
    })
    private @interface DriveCheckType {
        int checkUUIDInBackupFlowAtFirst = 1;
        int checkUUIDInBackupFlowAgain = 2;
        int checkUUIDInRestoreFlow = 3;
        int checkTrustContactsInRestoreFlow = 4;
        int saveUUID = 5;
    }

    private static class ActivityHandler extends Handler {
        private WeakReference<SocialKeyRecoveryChooseDriveAccountActivity> mActivity;

        ActivityHandler(SocialKeyRecoveryChooseDriveAccountActivity activity) {
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

    private static class CheckDriveRunnable implements Runnable {
        private final WeakReference<SocialKeyRecoveryChooseDriveAccountActivity> mActivity;
        private final WeakReference<String> mMessage;
        private final @DriveCheckType
        int mCheckType;

        CheckDriveRunnable(
                SocialKeyRecoveryChooseDriveAccountActivity activity,
                String message,
                @DriveCheckType int checkType) {
            mActivity = new WeakReference<>(activity);
            mMessage = new WeakReference<String>(message);
            mCheckType = checkType;
        }

        @Override
        public void run() {
            checkAnyMembersNull();
            switch (mCheckType) {
                case DriveCheckType.checkUUIDInBackupFlowAtFirst:
                    checkUUIDInBackupFlowAtFirst();
                    break;
                case DriveCheckType.checkUUIDInBackupFlowAgain:
                    checkUUIDInBackupFlowAgain();
                    break;
                case DriveCheckType.checkUUIDInRestoreFlow:
                    checkUUIDInRestoreFlow();
                    break;
                case DriveCheckType.checkTrustContactsInRestoreFlow:
                    checkTrustContactsInRestoreFlow();
                    break;
                case DriveCheckType.saveUUID:
                    saveUUID();
                    break;
            }
        }

        private void checkUUIDInBackupFlowAtFirst() {
            SocialKeyRecoveryChooseDriveAccountActivity activity = mActivity.get();
            String message = mMessage.get();
            // We need to double check UUID on drive and local file for onedrive to confirm it
            // really doesn't exist.
            if (activity.mDriveServiceType == DriveServiceType.oneDrive) {
                if (!activity.mIsUUIDNeedDoubleCheck) {
                    if (DriveUtil.isUUIDHashDifferentFromDriveAtFirst(activity, message)) {
                        User user = activity.mUser;
                        activity.checkDrive(
                                user.getDisplayableId(),
                                DriveCheckType.checkUUIDInBackupFlowAtFirst);
                        activity.mIsUUIDNeedDoubleCheck = true;
                        return;
                    }
                } else {
                    if (DriveUtil.isUUIDHashDifferentFromLocalFile(activity)) {
                        activity.setupChooserDialog();
                        return;
                    }
                }
                User user = activity.mUser;
                activity.checkDrive(user.getDisplayableId(), DriveCheckType.saveUUID);
            } else if (activity.mDriveServiceType == DriveServiceType.googleDrive) {
                if (DriveUtil.isUUIDHashDifferentFromDriveAtFirst(activity, message)) {
                    activity.setupChooserDialog();
                } else {
                    DriveAccount account = activity.mChosenDriveAccount;
                    activity.checkDrive(account.getEmail(), DriveCheckType.saveUUID);
                }
            } else {
                LogUtil.logError(TAG, "checkUUIDInBackupFlowAtFirst(), unknown checkType");
            }
        }

        // Prevent saving flow of google drive isn't completed,
        // So we need to check again.
        private void checkUUIDInBackupFlowAgain() {
            SocialKeyRecoveryChooseDriveAccountActivity activity = mActivity.get();
            String message = mMessage.get();
            if (activity.mProgressDialogUtil != null) {
                activity.mProgressDialogUtil.dismiss(activity.mProgressDialog);
            }

            if (DriveUtil.isUUIDHashDifferentFromDrive(activity, message)) {
                activity.setupChooserDialog();
            } else {
                activity.storeEmailAndUserName();
                activity.startSocialBackupActivity();
            }
        }

        private void checkUUIDInRestoreFlow() {
            SocialKeyRecoveryChooseDriveAccountActivity activity = mActivity.get();
            String message = mMessage.get();
            activity.mDriveDeviceId = message;
            if (TextUtils.isEmpty(message)) {
                activity.showRestoreNotEnoughDialog();
            } else {
                DriveAccount account = activity.mChosenDriveAccount;
                activity.checkDrive(
                        account.getEmail(), DriveCheckType.checkTrustContactsInRestoreFlow);
            }
        }

        private void checkTrustContactsInRestoreFlow() {
            SocialKeyRecoveryChooseDriveAccountActivity activity = mActivity.get();
            String message = mMessage.get();
            if (activity.mProgressDialogUtil != null) {
                activity.mProgressDialogUtil.dismiss(activity.mProgressDialog);
            }
            if (activity.isRestoreSourcesNumberEnough(message)) {
                activity.mBackupTargets = message;
                SkrSharedPrefs.putSocialKMFlowType(activity, String.valueOf(-1));
                activity.startSocialRestoreActivity();
            } else {
                if (activity.mIsTrustContactsNeedDoubleCheck) {
                    activity.showRestoreNotEnoughDialog();
                } else {
                    // For prevent from overwriting file,
                    // we save trust contacts on different files for different devices with same
                    // account.
                    // But we should also check old file, when we recover the wallet.
                    activity.mIsTrustContactsNeedDoubleCheck = true;
                    DriveAccount account = activity.mChosenDriveAccount;
                    activity.checkDrive(
                            account.getEmail(), DriveCheckType.checkTrustContactsInRestoreFlow);
                }
            }
        }

        private void saveUUID() {
            SocialKeyRecoveryChooseDriveAccountActivity activity = mActivity.get();
            String message = mMessage.get();
            // Because onedrive service saves uuid on local and remote.
            // These two file confirms correctness.
            // Thus we doesn't check uuid again.
            if (activity.mDriveServiceType == DriveServiceType.oneDrive) {
                if (activity.mProgressDialogUtil != null) {
                    activity.mProgressDialogUtil.dismiss(activity.mProgressDialog);
                }
                activity.storeEmailAndUserName();
                activity.startSocialBackupActivity();
                return;
            }
            activity.checkDrive(message, DriveCheckType.checkUUIDInBackupFlowAgain);
        }

        private void checkAnyMembersNull() {
            if (mActivity == null) {
                LogUtil.logError(TAG, "checkAnyMembersNull(), mActivity is null");
                return;
            }
            if (mMessage == null) {
                LogUtil.logError(TAG, "checkAnyMembersNull(), mMessage is null");
                return;
            }
            SocialKeyRecoveryChooseDriveAccountActivity activity = mActivity.get();
            String message = mMessage.get();
            if (activity == null) {
                LogUtil.logError(TAG, "checkAnyMembersNull(), activity is null");
                return;
            }
            if (TextUtils.isEmpty(message)) {
                LogUtil.logError(TAG, "checkAnyMembersNull(), message is null");
                return;
            }
            DriveAccount account = activity.mChosenDriveAccount;
            if (account == null) {
                LogUtil.logError(TAG, "checkAnyMembersNull(), account is null");
                return;
            }
        }
    }
}
