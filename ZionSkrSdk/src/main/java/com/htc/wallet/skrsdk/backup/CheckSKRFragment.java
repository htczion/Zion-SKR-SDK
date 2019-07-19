package com.htc.wallet.skrsdk.backup;

import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_TRIGGER_BROADCAST;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.sqlite.util.BackupTargetUtil;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.util.SkrSharedPrefs;

// Only for security protection update
public class CheckSKRFragment extends Fragment {
    private static final String TAG = "CheckSKRFragment";

    private Handler mUiHandler;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_check_skr, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUiHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onResume() {
        super.onResume();
        final Activity activity = getActivity();
        if (activity == null) {
            LogUtil.logError(TAG, "activity is null");
            return;
        }

        if (SkrSharedPrefs.getShouldShowSkrSecurityUpdateHeaderV1(
                activity.getApplicationContext())) {

            // Local broadcast
            mBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context, Intent intent) {
                    new Thread() {
                        @Override
                        public void run() {
                            if (context == null) {
                                LogUtil.logError(TAG, "context is null");
                            } else if (BackupTargetUtil.getBadCount(context.getApplicationContext())
                                    > 0) {
                                mUiHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        show();
                                    }
                                });
                            } else {
                                // There are no "Bad" backup, user may rebuild all backup or
                                // delete them all.
                                // Therefore not need to show Security Protection Header
                                LogUtil.logDebug(TAG,
                                        "There are no Bad backupTarget, user may rebuild all "
                                                + "backup or delete them all,"
                                                + " do not show Security Protection Header "
                                                + "anymore");
                                SkrSharedPrefs.putShouldShowSkrSecurityUpdateHeaderV1(activity,
                                        false);
                                mUiHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        hide();
                                    }
                                });
                            }
                        }
                    }.start();
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_TRIGGER_BROADCAST);
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(
                    activity);
            localBroadcastManager.registerReceiver(mBroadcastReceiver, filter);

            final View view = getView();
            if (view == null) {
                LogUtil.logError(TAG, "getView() is null");
            } else {
                RelativeLayout layout = view.findViewById(R.id.check_skr_panel);
                layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LogUtil.logDebug(TAG, "Show Skr Security Update Dialog V1");
                        CheckSKRUtil.showSkrSecurityUpdateDialogV1(activity);
                    }
                });
            }

            new Thread() {
                @Override
                public void run() {
                    if (BackupTargetUtil.getBadCount(activity.getApplicationContext()) > 0) {
                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                show();
                            }
                        });
                    } else {
                        // There are no "Bad" backup, user may rebuild all backup or delete them
                        // all.
                        // Therefore not need to show Security Protection Header
                        LogUtil.logDebug(TAG,
                                "There are no Bad backupTarget, user may rebuild all backup or "
                                        + "delete them all,"
                                        + " do not show Security Protection Header anymore");
                        SkrSharedPrefs.putShouldShowSkrSecurityUpdateHeaderV1(
                                activity.getApplicationContext(), false);
                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                hide();
                            }
                        });
                    }
                }
            }.start();
        } else {
            LogUtil.logDebug(TAG, "Not need to show Security Protection Header");
            hide();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        final Activity activity = getActivity();
        if (activity == null) {
            LogUtil.logError(TAG, "activity is null");
            return;
        }

        if (mBroadcastReceiver != null) {
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(
                    activity);
            localBroadcastManager.unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
    }

    @UiThread
    private void show() {
        // show view
        final View view = getView();
        if (view == null) {
            LogUtil.logError(TAG, "getView() is null");
        } else {
            RelativeLayout layout = view.findViewById(R.id.check_skr_panel);
            if (layout.getVisibility() != View.VISIBLE) {
                layout.setVisibility(View.VISIBLE);
            }
        }
    }

    @UiThread
    private void hide() {
        // hide view
        final View view = getView();
        if (view == null) {
            LogUtil.logError(TAG, "getView() is null");
        } else {
            RelativeLayout layout = view.findViewById(R.id.check_skr_panel);
            if (layout.getVisibility() != View.GONE) {
                layout.setVisibility(View.GONE);
            }
        }
    }
}