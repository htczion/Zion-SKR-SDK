package com.htc.wallet.skrsdk.verification;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.htc.wallet.skrsdk.R;

import java.util.ArrayList;

public class VerificationCodeNumberKeyboardView extends LinearLayout
        implements View.OnClickListener {
    private static final String TAG = "VerificationCodeNumberKeyboardView";

    private KeyType[] mKeyTypes =
            new KeyType[]{
                    KeyType.ZERO,
                    KeyType.ONE,
                    KeyType.TWO,
                    KeyType.THREE,
                    KeyType.FOUR,
                    KeyType.FIVE,
                    KeyType.SIX,
                    KeyType.SEVEN,
                    KeyType.EIGHT,
                    KeyType.NINE,
                    KeyType.DEL,
                    KeyType.DONE
            };

    private Callback mCallback;
    private ArrayList<View> mIbtnKeyboardList;

    public VerificationCodeNumberKeyboardView(Context context) {
        this(context, null);
    }

    public VerificationCodeNumberKeyboardView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerificationCodeNumberKeyboardView(
            Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        layoutInflater.inflate(R.layout.layout_number_keyboard, this, true);

        mIbtnKeyboardList = new ArrayList<>();
        mIbtnKeyboardList.add(0, this.findViewById(R.id.ibtn_key_num0));
        mIbtnKeyboardList.add(1, this.findViewById(R.id.ibtn_key_num1));
        mIbtnKeyboardList.add(2, this.findViewById(R.id.ibtn_key_num2));
        mIbtnKeyboardList.add(3, this.findViewById(R.id.ibtn_key_num3));
        mIbtnKeyboardList.add(4, this.findViewById(R.id.ibtn_key_num4));
        mIbtnKeyboardList.add(5, this.findViewById(R.id.ibtn_key_num5));
        mIbtnKeyboardList.add(6, this.findViewById(R.id.ibtn_key_num6));
        mIbtnKeyboardList.add(7, this.findViewById(R.id.ibtn_key_num7));
        mIbtnKeyboardList.add(8, this.findViewById(R.id.ibtn_key_num8));
        mIbtnKeyboardList.add(9, this.findViewById(R.id.ibtn_key_num9));
        mIbtnKeyboardList.add(10, this.findViewById(R.id.ibtn_key_del));

        for (int i = 0; i < mIbtnKeyboardList.size(); i++) {
            ImageButton btnKey = (ImageButton) mIbtnKeyboardList.get(i);
            btnKey.setOnClickListener(this);
        }
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    @Override
    public void onClick(View v) {
        if (mCallback != null) {
            mCallback.onKeyEvent(mKeyTypes[mIbtnKeyboardList.indexOf(v)]);
        }
    }

    public enum KeyType {
        ONE,
        TWO,
        THREE,
        FOUR,
        FIVE,
        SIX,
        SEVEN,
        EIGHT,
        NINE,
        ZERO,
        DEL,
        DONE
    }

    public interface Callback {
        void onKeyEvent(KeyType keyType);
    }
}
