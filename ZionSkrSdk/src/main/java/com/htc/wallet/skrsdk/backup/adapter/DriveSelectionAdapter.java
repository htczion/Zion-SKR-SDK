package com.htc.wallet.skrsdk.backup.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.Objects;

public class DriveSelectionAdapter extends BaseAdapter {
    private static final String TAG = "DriveSelectionAdapter";
    private final LayoutInflater mInflater;
    private final DriveSelectionAdapterCallback mCallback;
    private String[] mServices;
    private int mRadioButtonIndex = -1;

    public DriveSelectionAdapter(
            @NonNull final Context context,
            @NonNull final DriveSelectionAdapterCallback callback,
            String[] services) {
        super();
        mCallback = Objects.requireNonNull(callback);
        mServices = services;
        mInflater = LayoutInflater.from(Objects.requireNonNull(context));
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public int getCount() {
        return mServices.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        viewHolder holder = new viewHolder();
        convertView = createRadioItemConvertView(holder);
        setupRadioItemHolder(holder, position);
        return convertView;
    }

    private View createRadioItemConvertView(@NonNull final viewHolder holder) {
        if (holder == null) {
            LogUtil.logDebug(TAG, "createRadioItemConvertView, holder is null");
        }
        View convertView =
                mInflater.inflate(R.layout.social_backup_drive_chooser_dialog_radio_item, null);

        holder.title = convertView.findViewById(R.id.tv_title);
        holder.selectBtn = convertView.findViewById(R.id.rb_chosen);
        holder.rootView = convertView.findViewById(R.id.rl_dialog_radio_item);
        convertView.setTag(holder);
        return convertView;
    }

    private void setupRadioItemHolder(@NonNull final viewHolder holder, final int position) {
        if (holder == null) {
            LogUtil.logDebug(TAG, "setupRadioItemHolder, holder is null");
        }

        holder.title.setText(mServices[position]);
        holder.selectBtn.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mRadioButtonIndex = position;
                            mCallback.onItemClick(mRadioButtonIndex);
                            notifyDataSetChanged();
                        }
                    }
                });
        holder.rootView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mRadioButtonIndex = position;
                        mCallback.onItemClick(mRadioButtonIndex);
                        notifyDataSetChanged();
                    }
                });
        if (mRadioButtonIndex == position) {
            holder.selectBtn.setChecked(true);
        } else {
            holder.selectBtn.setChecked(false);
        }
    }

    private static class viewHolder {
        TextView title;
        RadioButton selectBtn;
        View rootView;
    }

    public interface DriveSelectionAdapterCallback {
        void onItemClick(int position);
    }
}
