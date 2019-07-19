package com.htc.wallet.skrsdk.verification.uiadapter;

import static com.htc.wallet.skrsdk.verification.constant.SocialKeyRecoveryRequestConstants.TYPE_BACKUP_SOURCE;
import static com.htc.wallet.skrsdk.verification.constant.SocialKeyRecoveryRequestConstants.TYPE_BACKUP_TARGET;
import static com.htc.wallet.skrsdk.verification.constant.SocialKeyRecoveryRequestConstants.TYPE_RESTORE_TARGET;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.util.LogUtil;
import com.htc.wallet.skrsdk.verification.uilistener.RecyclerViewClickListener;

import java.util.List;
import java.util.Objects;

public class RequestListAdapter extends RecyclerView.Adapter<RequestListAdapter.ViewHolder> {
    private static final String TAG = "RequestListAdapter";
    private static final long ITEM_CLICK_TIME_INTERVAL = 300;
    private final Context mContext;
    private final List<BackupTargetEntity> mBackupTargetData;
    private final List<BackupSourceEntity> mBackupSourceData;
    private final List<RestoreTargetEntity> mRestoreTargetData;
    private final RecyclerViewClickListener mRecyclerViewClickListener;
    private long mLastClickTime = System.currentTimeMillis();

    public RequestListAdapter(
            @NonNull Context context,
            @NonNull List<BackupTargetEntity> backupTarget,
            @NonNull List<BackupSourceEntity> backupSources,
            @NonNull List<RestoreTargetEntity> restoreTargets,
            @NonNull RecyclerViewClickListener listener) {
        mContext = Objects.requireNonNull(context);
        mBackupTargetData = Objects.requireNonNull(backupTarget);
        mBackupSourceData = Objects.requireNonNull(backupSources);
        mRestoreTargetData = Objects.requireNonNull(restoreTargets);
        mRecyclerViewClickListener = Objects.requireNonNull(listener);
    }

    @NonNull
    @Override
    public RequestListAdapter.ViewHolder onCreateViewHolder(
            @NonNull final ViewGroup parent, final int viewType) {
        final View view =
                LayoutInflater.from(mContext)
                        .inflate(
                                R.layout.social_backup_request_list_item,
                                Objects.requireNonNull(parent),
                                false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull final RequestListAdapter.ViewHolder holder, int position) {
        holder.itemView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final long now = System.currentTimeMillis();
                        if (now - mLastClickTime < ITEM_CLICK_TIME_INTERVAL) {
                            return;
                        }
                        mLastClickTime = now;

                        switch (holder.getItemViewType()) {
                            case TYPE_BACKUP_TARGET:
                                mRecyclerViewClickListener.onItemClick(
                                        v, TYPE_BACKUP_TARGET, holder.getAdapterPosition());
                                LogUtil.logDebug(
                                        TAG, "TYPE_BACKUP_TARGET" + holder.getAdapterPosition());
                                break;
                            case TYPE_BACKUP_SOURCE:
                                mRecyclerViewClickListener.onItemClick(
                                        v,
                                        TYPE_BACKUP_SOURCE,
                                        holder.getAdapterPosition() - mBackupTargetData.size());
                                LogUtil.logDebug(
                                        TAG,
                                        "TYPE_BACKUP_SOURCE"
                                                + (holder.getAdapterPosition()
                                                - mBackupTargetData.size()));
                                break;
                            case TYPE_RESTORE_TARGET:
                                mRecyclerViewClickListener.onItemClick(
                                        v,
                                        TYPE_RESTORE_TARGET,
                                        holder.getAdapterPosition()
                                                - mBackupTargetData.size()
                                                - mBackupSourceData.size());
                                LogUtil.logDebug(
                                        TAG,
                                        "TYPE_RESTORE_TARGET"
                                                + (holder.getAdapterPosition()
                                                - mBackupTargetData.size()
                                                - mBackupSourceData.size()));
                                break;
                        }
                    }
                });
        if (holder == null) {
            return;
        }
        switch (holder.getItemViewType()) {
            case TYPE_BACKUP_TARGET:
                final BackupTargetEntity backupTarget = mBackupTargetData.get(position);
                if (backupTarget == null) {
                    LogUtil.logDebug(
                            TAG, "onBindViewHolder fails. backupTarget  or holder is null");
                    return;
                }
                setUpHolderData(
                        holder,
                        backupTarget.getName(),
                        mContext.getString(R.string.social_restore_backup_tap_to_generate_code));
                break;
            case TYPE_BACKUP_SOURCE:
                final BackupSourceEntity backupSource =
                        mBackupSourceData.get(position - mBackupTargetData.size());
                if (backupSource == null) {
                    LogUtil.logDebug(
                            TAG, "onBindViewHolder fails. backupSource  or holder is null");
                    return;
                }
                setUpHolderData(holder,
                        String.format(mContext.getString(R.string.social_restore_backup_requests),
                                backupSource.getName()),
                        mContext.getString(R.string.social_restore_backup_tap_to_enter_code));
                return;
            case TYPE_RESTORE_TARGET:
                final RestoreTargetEntity restoreTarget =
                        mRestoreTargetData.get(
                                position - mBackupTargetData.size() - mBackupSourceData.size());
                if (restoreTarget == null) {
                    LogUtil.logDebug(
                            TAG, "onBindViewHolder fails. restoreTarget  or holder is null");
                    return;
                }
                setUpHolderData(holder, String.format(mContext.getString(
                        R.string.social_restore_backup_recovery_support_request_from),
                        restoreTarget.getName()),
                        mContext.getString(R.string.social_restore_backup_tap_to_generate_code));
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < mBackupTargetData.size()) {
            return TYPE_BACKUP_TARGET;
        } else if (position - mBackupTargetData.size() < mBackupSourceData.size()) {
            return TYPE_BACKUP_SOURCE;
        } else if (position - mBackupTargetData.size() - mBackupSourceData.size()
                < mRestoreTargetData.size()) {
            return TYPE_RESTORE_TARGET;
        }
        return -1;
    }

    private void setUpHolderData(
            @NonNull final RequestListAdapter.ViewHolder holder,
            @NonNull String name,
            @NonNull String hintText) {
        if (holder == null || TextUtils.isEmpty(name) || TextUtils.isEmpty(hintText)) {
            LogUtil.logDebug(TAG, "holder, name or hintText is null");
            return;
        }
        holder.tvContactName.setText(name);
        holder.tvHintText.setText(hintText);
    }

    @Override
    public int getItemCount() {
        return mBackupTargetData.size() + mBackupSourceData.size() + mRestoreTargetData.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvContactName;
        TextView tvHintText;

        ViewHolder(@NonNull final View view) {
            super(view);
            tvContactName = view.findViewById(R.id.tv_contact_name);
            tvHintText = view.findViewById(R.id.tv_hint);
        }
    }
}
