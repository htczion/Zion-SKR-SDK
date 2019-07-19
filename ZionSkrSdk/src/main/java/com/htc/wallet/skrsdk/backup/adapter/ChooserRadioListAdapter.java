package com.htc.wallet.skrsdk.backup.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.backup.listener.RestoreChooserListAdapterListener;
import com.htc.wallet.skrsdk.util.LogUtil;

import java.util.Objects;

public class ChooserRadioListAdapter extends BaseAdapter {
    private static final String TAG = "ChooserRadioListAdapter";
    private static final int POSITION_FOR_REMIDER_ACCOUNT_CHOOSER = 2;
    private final LayoutInflater mInflater;
    private final String[] mTitles;
    private final String[] mSubTitles;
    private final RestoreChooserListAdapterListener mListener;
    private int mRadioButtonIndex = -1;
    private boolean mIsRestoreChooser;

    public ChooserRadioListAdapter(
            @NonNull final Context context,
            @NonNull final RestoreChooserListAdapterListener listener,
            String[] titles,
            String[] subTitles,
            boolean isRestoreChooser) {
        super();
        mListener = Objects.requireNonNull(listener);
        mTitles = titles;
        mSubTitles = subTitles;
        mInflater = LayoutInflater.from(Objects.requireNonNull(context));
        mIsRestoreChooser = isRestoreChooser;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return !isTextListItem(position);
    }

    @Override
    public int getCount() {
        return mTitles.length;
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
        if (isTextListItem(position)) {
            convertView = createTextItemConvertView(holder);
            setupTextItemHolder(holder, position);
        } else {
            convertView = createRadioItemConvertView(holder);
            setupRadioItemHolder(holder, position);
        }
        return convertView;
    }

    private View createRadioItemConvertView(@NonNull final viewHolder holder) {
        if (holder == null) {
            LogUtil.logDebug(TAG, "createRadioItemConvertView, holder is null");
        }
        View convertView =
                mInflater.inflate(R.layout.social_backup_restore_chooser_dialog_radio_item, null);

        holder.title = convertView.findViewById(R.id.tv_title);
        holder.subTitle = convertView.findViewById(R.id.tv_sub_title);
        holder.selectBtn = convertView.findViewById(R.id.rb_chosen);
        holder.rootView = convertView.findViewById(R.id.rl_dialog_radio_item);
        convertView.setTag(holder);
        return convertView;
    }

    private View createTextItemConvertView(@NonNull final viewHolder holder) {
        if (holder == null) {
            LogUtil.logDebug(TAG, "createTextItemConvertView, holder is null");
        }
        View convertView =
                mInflater.inflate(R.layout.social_backup_restore_chooser_dialog_text_item, null);
        holder.title = convertView.findViewById(R.id.tv_title);
        convertView.setTag(holder);
        return convertView;
    }

    private void setupRadioItemHolder(@NonNull final viewHolder holder, final int position) {
        if (holder == null) {
            LogUtil.logDebug(TAG, "setupRadioItemHolder, holder is null");
        }

        holder.title.setText(mTitles[position]);
        // Keep setText here is pre-calculate the view height.
        holder.subTitle.setText(mSubTitles[position]);
        // Wrap text with correct new line.
        holder.subTitle.setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY);
        holder.subTitle.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
        if (position == POSITION_FOR_REMIDER_ACCOUNT_CHOOSER) {
            holder.subTitle.setTextColor(Color.RED);
        }
        holder.selectBtn.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mRadioButtonIndex = position;
                            mListener.onItemClick(mRadioButtonIndex);
                            notifyDataSetChanged();
                        }
                    }
                });
        holder.rootView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mRadioButtonIndex = position;
                        mListener.onItemClick(mRadioButtonIndex);
                        notifyDataSetChanged();
                    }
                });
        if (mRadioButtonIndex == position) {
            holder.selectBtn.setChecked(true);
        } else {
            holder.selectBtn.setChecked(false);
        }
    }

    private void setupTextItemHolder(@NonNull final viewHolder holder, final int position) {
        if (holder == null) {
            LogUtil.logDebug(TAG, "setupTextItemHolder, holder is null");
        }

        holder.title.setText(mTitles[position]);
    }

    private boolean isTextListItem(int position) {
        return !mIsRestoreChooser && (position == 0 || position == 1);
    }

    private static class viewHolder {
        TextView title;
        TextView subTitle;
        RadioButton selectBtn;
        View rootView;
    }
}
