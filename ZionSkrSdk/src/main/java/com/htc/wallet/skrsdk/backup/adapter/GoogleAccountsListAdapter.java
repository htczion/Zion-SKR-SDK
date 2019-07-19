package com.htc.wallet.skrsdk.backup.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.backup.DriveAccount;
import com.htc.wallet.skrsdk.backup.listener.GoogleRecyclerViewClickListener;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.List;
import java.util.Objects;

public class GoogleAccountsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "GoogleAccountsListAdapter";
    private static final int TYPE_LIST_ACCOUNT = 1;
    private static final int TYPE_ADD_ACCOUNT = 2;
    final GoogleRecyclerViewClickListener mRecyclerViewClickListener;
    private final Context mContext;
    private final List<DriveAccount> mData;
    private final Drawable mProfileIcon;

    public GoogleAccountsListAdapter(
            @NonNull Context context,
            @NonNull List<DriveAccount> data,
            @Nullable Drawable profileIcon,
            @NonNull GoogleRecyclerViewClickListener listener) {
        mContext = Objects.requireNonNull(context);
        mData = Objects.requireNonNull(data);
        mProfileIcon = profileIcon;
        mRecyclerViewClickListener = Objects.requireNonNull(listener);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mData.size() - 1) {
            return TYPE_ADD_ACCOUNT;
        }
        return TYPE_LIST_ACCOUNT;
    }

    @Override
    public void onBindViewHolder(
            @NonNull final RecyclerView.ViewHolder holder, final int position) {
        if (holder == null) {
            LogUtil.logDebug(TAG, "onBindViewHolder fails. holder is null");
            return;
        }

        if (holder.getItemViewType() == TYPE_LIST_ACCOUNT) {
            setupListAccountViewHolder(holder);
        } else {
            setupAddAccountViewHolder(holder);
        }
    }

    private void setupListAccountViewHolder(@NonNull final RecyclerView.ViewHolder holder) {
        if (holder == null) {
            LogUtil.logDebug(TAG, "setupListAccountViewHolder fails. holder is null");
            return;
        }
        final ListAccountViewHolder listAccountViewHolder = (ListAccountViewHolder) holder;
        final DriveAccount account = mData.get(listAccountViewHolder.getAdapterPosition());
        if (account == null) {
            LogUtil.logDebug(TAG, "setupListAccountViewHolder fails. account is null");
            return;
        }

        holder.itemView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listAccountViewHolder.cbChosen.isChecked()) {
                            listAccountViewHolder.cbChosen.setChecked(false);
                            mRecyclerViewClickListener.onItemUnCheck(
                                    listAccountViewHolder.getAdapterPosition());

                        } else {
                            listAccountViewHolder.cbChosen.setChecked(true);
                            mRecyclerViewClickListener.onItemCheck(
                                    listAccountViewHolder.getAdapterPosition());
                        }
                    }
                });

        listAccountViewHolder.tvMail.setText(account.getEmail());
        listAccountViewHolder.cbChosen.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (listAccountViewHolder.cbChosen.isChecked()) {
                            mRecyclerViewClickListener.onItemCheck(
                                    listAccountViewHolder.getAdapterPosition());
                        } else {
                            mRecyclerViewClickListener.onItemUnCheck(
                                    listAccountViewHolder.getAdapterPosition());
                        }
                    }
                });

        if (mProfileIcon != null) {
            listAccountViewHolder.ivProfile.setImageDrawable(mProfileIcon);
        }

        if (account.getIsChosen()) {
            listAccountViewHolder.cbChosen.setChecked(true);
        } else {
            listAccountViewHolder.cbChosen.setChecked(false);
        }
    }

    private void setupAddAccountViewHolder(@NonNull final RecyclerView.ViewHolder holder) {
        if (holder == null) {
            LogUtil.logDebug(TAG, "setupAddAccountViewHolder fails. holder is null");
            return;
        }
        final AddAccountViewHolder listAccountViewHolder = (AddAccountViewHolder) holder;

        holder.itemView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mRecyclerViewClickListener.onItemCheck(
                                listAccountViewHolder.getAdapterPosition());
                    }
                });
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ADD_ACCOUNT) {
            final View view =
                    LayoutInflater.from(mContext)
                            .inflate(
                                    R.layout.social_backup_choose_google_drive_add_account_item,
                                    Objects.requireNonNull(parent),
                                    false);
            return new AddAccountViewHolder(view);
        } else {
            final View view =
                    LayoutInflater.from(mContext)
                            .inflate(
                                    R.layout.social_backup_choose_google_drive_list_account_item,
                                    Objects.requireNonNull(parent),
                                    false);
            return new ListAccountViewHolder(view);
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    private static class ListAccountViewHolder extends RecyclerView.ViewHolder {
        View relativeLayout;
        ImageView ivProfile;
        TextView tvMail;
        CheckBox cbChosen;

        ListAccountViewHolder(@NonNull final View view) {
            super(view);
            relativeLayout = view.findViewById(R.id.rl_list_account);
            ivProfile = view.findViewById(R.id.iv_profile_icon);
            tvMail = view.findViewById(R.id.tv_account_mail);
            cbChosen = view.findViewById(R.id.cb_chosen);
        }
    }

    private static class AddAccountViewHolder extends RecyclerView.ViewHolder {
        View relativeLayout;
        ImageView ivAddAccount;
        TextView tvAddAccount;

        AddAccountViewHolder(@NonNull final View view) {
            super(view);
            relativeLayout = view.findViewById(R.id.rl_add_account);
            ivAddAccount = view.findViewById(R.id.iv_add_account);
            tvAddAccount = view.findViewById(R.id.tv_add_account);
        }
    }
}
