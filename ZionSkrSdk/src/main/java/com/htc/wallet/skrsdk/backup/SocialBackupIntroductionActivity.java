package com.htc.wallet.skrsdk.backup;

import static com.htc.wallet.skrsdk.backup.constants.BackupSourceConstants.SOCIAL_BACKUP_INFO_CLICK;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.SocialBaseActivity;
import com.htc.wallet.skrsdk.backup.constants.BackupSourceConstants;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.PhoneUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;

public class SocialBackupIntroductionActivity extends SocialBaseActivity {
    private static final String TAG = "SocialBackupIntroductionActivity";
    private String mEmail;
    private String mUserName;

    private boolean mSetupButtonClicked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_social_backup_introduction);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSetupButtonClicked = false;
        setupUserData();
        initViews();
        if (isUserDataExisted() && !isFromBackupInfoClick()) {
            startSocialBackupActivity();
        }
    }

    private void initViews() {
        Button setupButton = findViewById(R.id.btn_setup);
        TextView textView = findViewById(R.id.tv_not_now);
        if (!isFromBackupInfoClick()) {
            setupButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            // Fast click setup twice will startActivity twice when this application
                            // just start
                            // Prevent click more than one time
                            if (mSetupButtonClicked) {
                                return;
                            }
                            mSetupButtonClicked = true;

                            if (isUserDataExisted()) {
                                startSocialBackupActivity();
                            } else {
                                Intent intent =
                                        new Intent(
                                                SocialBackupIntroductionActivity.this,
                                                SocialKeyRecoveryChooseDriveAccountActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        }
                    });
            textView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            finish();
                        }
                    });
        } else {
            setupButton.setText(R.string.backup_introduction_ok);
            setupButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onBackPressed();
                            finish();
                        }
                    });
            textView.setVisibility(View.GONE);
        }
    }

    private boolean isUserDataExisted() {
        return !TextUtils.isEmpty(mEmail);
    }

    private boolean isFromBackupInfoClick() {
        Bundle bundle = getIntent().getExtras();
        boolean isBackupInfoClick = false;
        if (bundle != null) {
            isBackupInfoClick = bundle.getBoolean(SOCIAL_BACKUP_INFO_CLICK, false);
        }
        return isBackupInfoClick;
    }

    private void startSocialBackupActivity() {
        if (!isUserDataExisted()) {
            LogUtil.logDebug(TAG, "startSocialBackupActivity, userData doesn't exist");
            return;
        }
        final Intent intent =
                new Intent(SocialBackupIntroductionActivity.this, SocialBackupActivity.class);
        intent.putExtra(BackupSourceConstants.ADDRESS, mEmail);
        intent.putExtra(BackupSourceConstants.NAME, mUserName);
        startActivity(intent);
        finish();
    }

    private void setupUserData() {
        mEmail = PhoneUtil.getSKREmail(this);
        mUserName = SkrSharedPrefs.getSocialKMUserName(this);
    }
}
