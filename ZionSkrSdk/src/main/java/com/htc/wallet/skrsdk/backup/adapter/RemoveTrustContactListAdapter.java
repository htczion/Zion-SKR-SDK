package com.htc.wallet.skrsdk.backup.adapter;

import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_BAD;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_NO_RESPONSE;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_OK;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_REQUEST_WAIT_OK;
import static com.htc.wallet.skrsdk.verification.VerificationConstants.ACTION_TRIGGER_BROADCAST;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.action.Action;
import com.htc.wallet.skrsdk.action.BackupDeleteAction;
import com.htc.wallet.skrsdk.backup.listener.RecyclerViewItemClickListener;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.listener.DatabaseCompleteListener;
import com.htc.wallet.skrsdk.sqlite.util.BackupTargetUtil;
import com.htc.wallet.skrsdk.util.DateTimeUtil;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class RemoveTrustContactListAdapter
        extends RecyclerView.Adapter<RemoveTrustContactListAdapter.ViewHolder> {

    private static final String TAG = "RemoveTrustContactListAdapter";

    private static final String KEY_FCM_TOKEN = "key_fcm_token";
    private static final String KEY_WHISPER_PUB = "key_whisper_pub";
    private static final String KEY_PUSHY_TOKEN = "key_pushy_token";

    private final Context mContext;
    private final List<BackupTargetEntity> mData;
    private final Map<Integer, Pair<String, Map<String, String>>> mRemoveMap =
            new TreeMap<>(Collections.reverseOrder());
    private RecyclerViewItemClickListener mOnItemCheckListener;

    public RemoveTrustContactListAdapter(
            @NonNull Context context,
            @NonNull List<BackupTargetEntity> data,
            @NonNull RecyclerViewItemClickListener onItemCheckListener) {
        mContext = Objects.requireNonNull(context);
        mData = Objects.requireNonNull(data);
        mOnItemCheckListener = Objects.requireNonNull(onItemCheckListener);
    }

    @NonNull
    @Override
    public RemoveTrustContactListAdapter.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        final View view =
                LayoutInflater.from(mContext)
                        .inflate(
                                R.layout.social_backup_remove_trust_contact_item,
                                Objects.requireNonNull(parent),
                                false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull final RemoveTrustContactListAdapter.ViewHolder holder, int position) {
        final BackupTargetEntity backupTarget = mData.get(position);
        if (backupTarget == null || holder == null) {
            LogUtil.logDebug(TAG, "onBindViewHolder fails. backupTarget or holder is null");
            return;
        }

        holder.cbRemove.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Map<String, String> fcmAndWhisperToken = new HashMap<>();
                        fcmAndWhisperToken.put(KEY_FCM_TOKEN, backupTarget.getFcmToken());
                        fcmAndWhisperToken.put(KEY_WHISPER_PUB, backupTarget.getWhisperPub());
                        fcmAndWhisperToken.put(KEY_PUSHY_TOKEN, backupTarget.getPushyToken());
                        final Pair<String, Map<String, String>> pair =
                                new Pair<>(backupTarget.getUUIDHash(), fcmAndWhisperToken);
                        if (holder.cbRemove.isChecked()) {
                            mRemoveMap.put(holder.getAdapterPosition(), pair);
                            mOnItemCheckListener.onItemCheck(mRemoveMap);
                        } else {
                            mRemoveMap.remove(holder.getAdapterPosition(), pair);
                            mOnItemCheckListener.onItemUnCheck(mRemoveMap);
                        }
                    }
                });

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
                                        backupTarget.getLastCheckedTime())));
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
                                        backupTarget.getLastCheckedTime())));
                break;
            // 3) Orange: Pending request
            case BACKUP_TARGET_STATUS_REQUEST:
            case BACKUP_TARGET_STATUS_REQUEST_WAIT_OK:
                setUpHolderData(
                        holder,
                        backupTarget.getName(),
                        R.drawable.shape_social_backup_status_pending,
                        mContext.getString(R.string.backup_trust_item_sub2));
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
                                        backupTarget.getLastCheckedTime())));
                break;
            default:
                LogUtil.logError(TAG, "Unknown status", new IllegalStateException());
        }
    }

    private void setUpHolderData(
            @NonNull final RemoveTrustContactListAdapter.ViewHolder holder,
            @NonNull String name,
            int resId,
            @NonNull String checkedTime) {
        if (holder == null
                || TextUtils.isEmpty(name)
                || resId == 0
                || TextUtils.isEmpty(checkedTime)) {
            LogUtil.logError(TAG, "holder, name, resId or checkedTime is null");
            return;
        }
        holder.tvContactName.setText(name);
        holder.ivBackupStatus.setImageResource(resId);
        holder.tvCheckedTime.setText(checkedTime);
    }

    public boolean isNoItemSelected() {
        return mRemoveMap.isEmpty();
    }

    public void remove(final Context context) {
        for (int position : mRemoveMap.keySet()) {
            if (mRemoveMap.get(position) == null) {
                LogUtil.logError(TAG, "get mRemoveMap is null in position= " + position);
                return;
            }
            final String uuidHash = mRemoveMap.get(position).first;
            final Map<String, String> fcmAndWhisperToken = mRemoveMap.get(position).second;
            if (fcmAndWhisperToken == null) {
                LogUtil.logError(
                        TAG,
                        "fcmAndWhisperToken is null",
                        new IllegalStateException("fcmAndWhisperToken is null"));
                return;
            }

            final String fcmToken = fcmAndWhisperToken.get(KEY_FCM_TOKEN);
            final String whisperPub = fcmAndWhisperToken.get(KEY_WHISPER_PUB);
            final String pushyToken = fcmAndWhisperToken.get(KEY_PUSHY_TOKEN);
            if (TextUtils.isEmpty(uuidHash)
                    || (TextUtils.isEmpty(fcmToken)
                    && TextUtils.isEmpty(whisperPub)
                    && TextUtils.isEmpty(pushyToken))) {
                if (mData.get(position) == null) {
                    LogUtil.logError(
                            TAG, "get backupTargetEntity is null in position= " + position);
                    return;
                }
                final String removeName = mData.get(position).getName();
                if (TextUtils.isEmpty(removeName)) {
                    LogUtil.logError(TAG, "get removeName is empty in position= " + position);
                    return;
                }
                BackupTargetUtil.removeWithOnlyName(
                        context,
                        removeName,
                        new DatabaseCompleteListener() {
                            @Override
                            public void onComplete() {
                                LogUtil.logDebug(TAG, "Remove trust contact: " + removeName);
                                Intent intentToNotifyUpdateUI =
                                        new Intent(ACTION_TRIGGER_BROADCAST);
                                LocalBroadcastManager.getInstance(context)
                                        .sendBroadcast(intentToNotifyUpdateUI);
                            }

                            @Override
                            public void onError(Exception exception) {
                                LogUtil.logError(
                                        TAG, "Remove trust contact error, e= " + exception);
                            }
                        });
                continue;
            }
            mData.remove(position);
            final Map<String, String> map = new ArrayMap<>();
            map.put(Action.KEY_UUID_HASH, uuidHash);
            new BackupDeleteAction().send(mContext, fcmToken, whisperPub, pushyToken, map);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, getItemCount());
        }
        mRemoveMap.clear();
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBackupStatus;
        TextView tvContactName;
        TextView tvCheckedTime;
        CheckBox cbRemove;

        ViewHolder(@NonNull final View view) {
            super(view);
            ivBackupStatus = view.findViewById(R.id.iv_remove_status_icon);
            tvContactName = view.findViewById(R.id.tv_remove_contact_name);
            tvCheckedTime = view.findViewById(R.id.tv_remove_last_check);
            cbRemove = view.findViewById(R.id.cb_remove_contact);
        }
    }
}
