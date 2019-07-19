package com.htc.wallet.skrsdk.backup.adapter;

import static com.htc.wallet.skrsdk.action.Action.KEY_IS_RESEND;
import static com.htc.wallet.skrsdk.action.Action.KEY_UUID_HASH;
import static com.htc.wallet.skrsdk.action.Action.MSG_IS_RESEND;
import static com.htc.wallet.skrsdk.backup.constants.SocialKeyRecoveryRequestConstants.TYPE_BACKUP_TARGET;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_BAD;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_NO_RESPONSE;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_OK;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_REQUEST_WAIT_OK;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.EMPTY_STRING;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.action.BackupDeleteAction;
import com.htc.wallet.skrsdk.backup.listener.RecyclerViewClickListener;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.util.DateTimeUtil;
import com.htc.wallet.skrsdk.util.InvitationUtil;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TrustContactListAdapter
        extends RecyclerView.Adapter<TrustContactListAdapter.ViewHolder> {
    private static final String TAG = "TrustContactListAdapter";
    private final Context mContext;
    private final List<BackupTargetEntity> mData;
    private final RecyclerViewClickListener mRecyclerViewClickListener;
    private final InvitationUtil mInvitationUtil;
    private final boolean mIsBackupFull;

    private static final long ITEM_CLICK_TIME_INTERVAL = 300;
    private long mLastClickTime = System.currentTimeMillis();

    public TrustContactListAdapter(
            @NonNull Context context,
            @NonNull List<BackupTargetEntity> data,
            boolean isBackupFull,
            @NonNull InvitationUtil invitationUtil,
            @NonNull RecyclerViewClickListener listener) {
        mContext = Objects.requireNonNull(context);
        mData = Objects.requireNonNull(data);
        mInvitationUtil = Objects.requireNonNull(invitationUtil);
        mRecyclerViewClickListener = Objects.requireNonNull(listener);
        mIsBackupFull = isBackupFull;
    }

    @NonNull
    @Override
    public TrustContactListAdapter.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        final View view =
                LayoutInflater.from(mContext)
                        .inflate(
                                R.layout.social_backup_trust_contact_item,
                                Objects.requireNonNull(parent),
                                false);
        final ViewHolder viewHolder = new ViewHolder(view);

        view.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final long now = System.currentTimeMillis();
                        if (now - mLastClickTime < ITEM_CLICK_TIME_INTERVAL) {
                            return;
                        }
                        mLastClickTime = now;
                        mRecyclerViewClickListener.onItemClick(
                                view, TYPE_BACKUP_TARGET, viewHolder.getAdapterPosition());
                    }
                });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(
            @NonNull final TrustContactListAdapter.ViewHolder holder, int position) {

        final BackupTargetEntity backupTarget = mData.get(position);
        if (backupTarget == null || holder == null) {
            LogUtil.logDebug(TAG, "onBindViewHolder fails. trustContactInfo  or holder is null");
            return;
        }
        switch (backupTarget.getStatus()) {
            // 1) Green: Good
            case BACKUP_TARGET_STATUS_OK:
                setUpHolderData(
                        holder,
                        backupTarget.getName(),
                        R.drawable.shape_social_backup_status_good,
                        String.format(
                                mContext.getString(R.string.backup_trust_item_sub),
                                DateTimeUtil.formatUTCToLocalTime(
                                        backupTarget.getLastCheckedTime())),
                        false);
                break;
            // 2) Red: Something is wrong
            case BACKUP_TARGET_STATUS_BAD:
                setUpHolderData(
                        holder,
                        backupTarget.getName(),
                        R.drawable.shape_social_backup_status_bad,
                        String.format(
                                mContext.getString(R.string.backup_trust_item_sub),
                                DateTimeUtil.formatUTCToLocalTime(
                                        backupTarget.getLastCheckedTime())),
                        true);
                holder.btnResend.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                resendLink(backupTarget);
                            }
                        });
                break;
            // 3) Orange: Pending request
            case BACKUP_TARGET_STATUS_REQUEST:
            case BACKUP_TARGET_STATUS_REQUEST_WAIT_OK:
                if (!backupTarget.isSeeded() && mIsBackupFull) {
                    Map<String, String> map = new ArrayMap<>();
                    map.put(KEY_UUID_HASH, backupTarget.getUUIDHash());
                    new BackupDeleteAction()
                            .send(
                                    mContext,
                                    backupTarget.getFcmToken(),
                                    backupTarget.getWhisperPub(),
                                    backupTarget.getPushyToken(),
                                    map);
                    break;
                }
                setUpHolderData(
                        holder,
                        backupTarget.getName(),
                        R.drawable.shape_social_backup_status_pending,
                        mContext.getString(R.string.backup_trust_item_sub2),
                        false);
                break;
            // 4) Gray: No Response
            case BACKUP_TARGET_STATUS_NO_RESPONSE:
                setUpHolderData(
                        holder,
                        backupTarget.getName(),
                        R.drawable.shape_social_backup_status_no_response,
                        String.format(
                                mContext.getString(R.string.backup_trust_item_sub),
                                DateTimeUtil.formatUTCToLocalTime(
                                        backupTarget.getLastCheckedTime())),
                        false);
                break;
            default:
                LogUtil.logError(TAG, "Unknown status", new IllegalStateException());
        }
    }

    private void setUpHolderData(
            @NonNull final TrustContactListAdapter.ViewHolder holder,
            @NonNull String name,
            int resId,
            @NonNull String checkedTime,
            boolean resendBtnVis) {
        if (holder == null
                || TextUtils.isEmpty(name)
                || resId == 0
                || TextUtils.isEmpty(checkedTime)) {
            LogUtil.logDebug(TAG, "holder, name, resId or checkedTime is null");
            return;
        }
        holder.tvContactName.setText(name);
        holder.ivBackupStatus.setImageResource(resId);
        holder.tvCheckedTime.setText(checkedTime);
        if (resendBtnVis) {
            holder.btnResend.setVisibility(View.VISIBLE);
        }
    }

    private void resendLink(BackupTargetEntity backupTarget) {
        Objects.requireNonNull(backupTarget);

        // Check real bad or auto backup after recover (only have name)
        final String backupTargetToken = backupTarget.getFcmToken();
        final String backupTargetWhisperPub = backupTarget.getWhisperPub();
        final String backupTargetPushyToken = backupTarget.getPushyToken();
        // Real bad, before generate resend link delete Bob's bad data
        if (!TextUtils.isEmpty(backupTargetWhisperPub)
                && !TextUtils.isEmpty(backupTargetPushyToken)) {
            LogUtil.logInfo(
                    TAG,
                    "Resend flow. backupTargetWhisperPub is non-empty, send BackupDeleteAction");
            Map<String, String> map = new ArrayMap<>();
            map.put(KEY_UUID_HASH, backupTarget.getUUIDHash());
            map.put(KEY_IS_RESEND, MSG_IS_RESEND);
            new BackupDeleteAction()
                    .send(
                            mContext,
                            backupTargetToken,
                            backupTargetWhisperPub,
                            backupTargetPushyToken,
                            map);
        }

        String backupTargetName = backupTarget.getName();
        if (TextUtils.isEmpty(backupTargetName)) {
            LogUtil.logError(TAG, "backupTargetName is null or empty");
            backupTargetName = EMPTY_STRING;
        }
        mInvitationUtil.setBackupTargetName(backupTargetName);

        // Real Bad
        String uuidHash = backupTarget.getUUIDHash();
        if (!TextUtils.isEmpty(uuidHash)) {
            LogUtil.logDebug(TAG, "Real Bad, resend link add encrypted UUIDHash");
            mInvitationUtil.setBackupTargetUUIDHash(uuidHash);
        }

        mInvitationUtil.sharingInvitation();
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBackupStatus;
        TextView tvContactName;
        TextView tvCheckedTime;
        Button btnResend;

        ViewHolder(@NonNull final View view) {
            super(view);
            ivBackupStatus = view.findViewById(R.id.iv_status_icon);
            tvContactName = view.findViewById(R.id.tv_contact_name);
            tvCheckedTime = view.findViewById(R.id.tv_last_check);
            btnResend = view.findViewById(R.id.btn_resend);
        }
    }
}
