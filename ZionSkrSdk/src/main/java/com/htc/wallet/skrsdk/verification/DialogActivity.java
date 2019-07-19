package com.htc.wallet.skrsdk.verification;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class DialogActivity extends AppCompatActivity {
    public static final String KEY_DIALOG_TYPE = "key_dialog_type";
    public static final String KEY_DIALOG_TITLE = "key_dialog_title";
    public static final String KEY_DIALOG_MESSAGE = "key_dialog_message";
    public static final int DIALOG_TYPE_NORMAL = 0;
    public static final int DIALOG_TYPE_APP_UPDATE = 1;
    private static final String TAG = "DialogActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake_layout);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final Intent intent = getIntent();
        if (intent == null) {
            LogUtil.logError(TAG, "intent is null!", new IllegalStateException("intent is null!"));
            return;
        }

        final int dialogType = intent.getIntExtra(KEY_DIALOG_TYPE, DIALOG_TYPE_NORMAL);
        switch (dialogType) {
            case DIALOG_TYPE_NORMAL:
                final String title = intent.getStringExtra(KEY_DIALOG_TITLE);
                final String message = intent.getStringExtra(KEY_DIALOG_MESSAGE);
                showDialog(title, message);
                break;
            case DIALOG_TYPE_APP_UPDATE:
                showAppUpgradeAlertDialog();
                break;
            default:
                LogUtil.logError(TAG, "unknown type", new IllegalStateException());
                finishAndRemoveTask();
        }
    }

    private void showDialog(@Nullable String title, @NonNull String message) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        if (!TextUtils.isEmpty(title)) {
            alertDialog.setTitle(title);
        }
        if (TextUtils.isEmpty(message)) {
            LogUtil.logError(
                    TAG, "message is empty!", new IllegalStateException("message is empty!"));
            return;
        }
        alertDialog.setMessage(message);
        alertDialog.setPositiveButton(
                R.string.ver_dialog_btn_ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishAndRemoveTask();
                        dialog.dismiss();
                    }
                });
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    private void showAppUpgradeAlertDialog() {
        AlertDialog.Builder upgradeAlertDialog = new AlertDialog.Builder(this);
        upgradeAlertDialog.setTitle(R.string.gen_new_version);
        upgradeAlertDialog.setMessage(R.string.gen_new_version_hint);
        upgradeAlertDialog.setPositiveButton(R.string.gen_update_now,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("market://details?id=com.htc.wallet"));
                        startActivity(intent);
                        failExit();
                    }
                });
        upgradeAlertDialog.setNegativeButton(R.string.Button_no,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        failExit();
                    }
                });
        upgradeAlertDialog.setCancelable(false);
        upgradeAlertDialog.create();
        upgradeAlertDialog.show();
    }

    private void failExit() {
        finishAffinity();
        System.exit(0);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DIALOG_TYPE_NORMAL, DIALOG_TYPE_APP_UPDATE})
    public @interface DialogType {
    }
}
