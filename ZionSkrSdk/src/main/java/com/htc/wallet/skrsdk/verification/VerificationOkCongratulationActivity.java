package com.htc.wallet.skrsdk.verification;

import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_OK;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.SocialBaseActivity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.listener.LoadDataListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupSourceUtil;
import com.htc.wallet.skrsdk.util.IntentUtil;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.IllegalFormatException;
import java.util.Objects;

public class VerificationOkCongratulationActivity extends SocialBaseActivity {
    private static final String TAG = "VerificationOkCongratulationActivity";

    private static final String KEY_VER_OK_EMAIL_HASH = "key_ver_ok_email_hash";
    private static final String KEY_VER_OK_UUID_HASH = "key_ver_ok_uuid_hash";
    private static final String KEY_VER_OK_NAME = "key_ver_ok_name";

    private Button mBtnDone;
    private TextView mTvCongratsTitle;
    private TextView mTvCongratsMsg;

    private boolean mBtnDoneClicked = false;

    // Amy's
    private String mEmailHash;
    private String mUUIDHash;
    private String mName;

    @NonNull
    public static Intent generateIntent(
            @NonNull Context context, String emailHash, String uuidHash, String name) {
        Objects.requireNonNull(context, "context is null");
        Intent intent = new Intent(context, VerificationOkCongratulationActivity.class);
        intent.putExtra(KEY_VER_OK_EMAIL_HASH, emailHash);
        intent.putExtra(KEY_VER_OK_UUID_HASH, uuidHash);
        intent.putExtra(KEY_VER_OK_NAME, name);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.verification_ok_congratulation_activity);

        Intent intentFromOkActionSend = getIntent();
        if (intentFromOkActionSend == null) {
            LogUtil.logError(TAG, "intentFromOkActionSend is null");
            return;
        }

        initVariable(intentFromOkActionSend);
        initComponents(intentFromOkActionSend);
        setDoneBtn();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBtnDoneClicked = false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        backToEntryActivity();
    }

    private void initVariable(@NonNull Intent intent) {
        Objects.requireNonNull(intent, "intent is null");
        mEmailHash = intent.getStringExtra(KEY_VER_OK_EMAIL_HASH);
        if (TextUtils.isEmpty(mEmailHash)) {
            LogUtil.logError(
                    TAG,
                    "emailHash is null or empty",
                    new IllegalStateException("emailHash is null or empty"));
        }
        mUUIDHash = intent.getStringExtra(KEY_VER_OK_UUID_HASH);
        if (TextUtils.isEmpty(mUUIDHash)) {
            LogUtil.logError(
                    TAG,
                    "UUIDHash is null or empty",
                    new IllegalStateException("UUIDHash is null or empty"));
        }
        mName = intent.getStringExtra(KEY_VER_OK_NAME);
        if (TextUtils.isEmpty(mName)) {
            LogUtil.logError(
                    TAG,
                    "UUIDHash is null or empty",
                    new IllegalStateException("UUIDHash is null or empty"));
        }
    }

    private void initComponents(@NonNull Intent intent) {
        Objects.requireNonNull(intent, "intent is null");

        mTvCongratsTitle = findViewById(R.id.tv_ok_congratulation_title);
        String congratsTitle =
                String.format(
                        getResources().getString(R.string.ver_ok_congratulation_title),
                        TextUtils.isEmpty(mName)
                                ? getResources().getString(R.string.ver_unknown_name)
                                : mName);
        mTvCongratsTitle.setText(congratsTitle);

        mTvCongratsMsg = findViewById(R.id.tv_ok_congratulation_msg);
        final String userName =
                TextUtils.isEmpty(mName)
                        ? getResources().getString(R.string.ver_unknown_name)
                        : mName;
        String congratsMsg;
        try {
            congratsMsg =
                    String.format(
                            getResources().getString(R.string.ver_ok_congratulation_msg),
                            userName,
                            userName);
        } catch (IllegalFormatException | NullPointerException e) {
            LogUtil.logWarning(TAG, "Unexpected error when format string, e=" + e);
            congratsMsg =
                    String.format(
                            getResources().getString(R.string.ver_ok_congratulation_msg), userName);
        }
        mTvCongratsMsg.setText(congratsMsg);

        mBtnDone = findViewById(R.id.btn_done);
    }

    private void setDoneBtn() {
        mBtnDone.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        // Fast click many times, will crash in UserTrail Rom
                        // Prevent click more than one time
                        if (mBtnDoneClicked) {
                            return;
                        }
                        mBtnDoneClicked = true;

                        final Context context = VerificationOkCongratulationActivity.this;
                        BackupSourceUtil.getWithUUIDHash(
                                context,
                                mEmailHash,
                                mUUIDHash,
                                new LoadDataListener() {
                                    @Override
                                    public void onLoadFinished(
                                            BackupSourceEntity backupSourceEntity,
                                            BackupTargetEntity backupTargetEntity,
                                            RestoreSourceEntity restoreSourceEntity,
                                            RestoreTargetEntity restoreTargetEntity) {
                                        if (backupSourceEntity == null) {
                                            LogUtil.logDebug(TAG, "BackupSource has been delete.");
                                            backToEntryActivity();
                                        } else {
                                            // Check status
                                            if (backupSourceEntity.compareStatus(
                                                    BACKUP_SOURCE_STATUS_OK)) {
                                                backupSourceEntity.setDone();
                                                BackupSourceUtil.update(
                                                        context,
                                                        backupSourceEntity,
                                                        new DatabaseCompleteListener() {
                                                            @Override
                                                            public void onComplete() {
                                                                backToEntryActivity();
                                                            }

                                                            @Override
                                                            public void onError(
                                                                    Exception exception) {
                                                                LogUtil.logError(
                                                                        TAG,
                                                                        "update error, e= "
                                                                                + exception);
                                                            }
                                                        });
                                            } else {
                                                // Unexpected error, status is not equal to
                                                // BACKUP_SOURCE_STATUS_OK
                                                LogUtil.logError(
                                                        TAG,
                                                        "incorrect status "
                                                                + backupSourceEntity.getStatus(),
                                                        new IllegalStateException(
                                                                "incorrect status "
                                                                        + backupSourceEntity
                                                                        .getStatus()));
                                                backToEntryActivity();
                                            }
                                        }
                                    }
                                });
                    }
                });
    }

    private void backToEntryActivity() {
        Intent intent = IntentUtil.generateEntryActivityIntent(this);
        startActivity(intent);
        finish();
    }
}
