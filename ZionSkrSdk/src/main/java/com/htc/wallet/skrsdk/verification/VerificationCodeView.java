package com.htc.wallet.skrsdk.verification;

import static android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.htc.wallet.skrsdk.R;
import com.htc.wallet.skrsdk.util.PinCodeUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VerificationCodeView extends LinearLayout {
    private static final String TAG = "VerificationCodeView";
    private static final String EMPTY_STRING = "";
    private static final int[] VIEW_IDS =
            new int[]{
                    R.id.tv_pin1, R.id.tv_pin2, R.id.tv_pin3,
                    R.id.tv_pin4, R.id.tv_pin5, R.id.tv_pin6,
            };
    private final StringBuffer mStringBuffer = new StringBuffer();
    Button mBtnVerify;
    private EditText mEtPin;
    private TextView mTvErrorHint;
    private TextView[] mTvPin;
    private int mPinCodeIndex = 0;

    // ui auto adjustment
    private ScrollView mScrollView;
    private RelativeLayout.LayoutParams mRlScrollView;
    //    private LinearLayout mPinCodeLayout;

    private VerificationCodeNumberKeyboardView mNumberKeyboardView;
    private VerificationCodeManager mCodeManager;
    private VerificationCodeNumberKeyboardView.Callback numberKeyboardViewCallback =
            new VerificationCodeNumberKeyboardView.Callback() {
                @Override
                public void onKeyEvent(VerificationCodeNumberKeyboardView.KeyType keyType) {
                    mCodeManager.inputKey(keyType);
                }
            };
    private PinCodeChangeListener mPinCodeChangeListener = null;

    public VerificationCodeView(Context context) {
        this(context, null);
    }

    public VerificationCodeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerificationCodeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        layoutInflater.inflate(R.layout.verification_pin_code_view, this, true);

        initComponents();
        setPinListener();
    }

    public boolean setVerificationPinCode(@NonNull String pinCode) {
        if (PinCodeUtil.isValidPinCode(pinCode)) {
            changePinStyle(
                    BackgroundType.verificationPinCodeOnView,
                    TextStyleType.verificationPinOnFrame,
                    INVISIBLE);
            mStringBuffer.delete(0, mStringBuffer.length());
            mStringBuffer.append(pinCode);
            mPinCodeIndex = mStringBuffer.length();
            for (int i = 0; i < PinCodeUtil.PIN_CODE_LENGTH; i++) {
                String pin = Character.toString(pinCode.charAt(i));
                mTvPin[i].setText(pin);
            }
            return true;
        }
        return false;
    }

    public void clearVerificationPinCode() {
        mStringBuffer.delete(0, mStringBuffer.length());
        for (TextView pinTextView : mTvPin) {
            pinTextView.setText(EMPTY_STRING);
        }
        changePinStyle(
                BackgroundType.verificationPinCodeOffView,
                TextStyleType.verificationPinOffFrame,
                INVISIBLE);
        // Because the keyboard show up when enter activity, set the first pin code on
        changeSinglePinStyle(
                0, BackgroundType.verificationPinCodeOnView, TextStyleType.verificationPinOnFrame);
    }

    public void setPinErrorStyle() {
        mTvErrorHint.setText(R.string.ver_pin_error_hint);
        changePinStyle(
                BackgroundType.verificationPinErrorCodeView,
                TextStyleType.verificationPinErrorFrame,
                View.VISIBLE);
    }

    public void setTimeoutStyle() {
        mTvErrorHint.setText(R.string.ver_request_timeout);
        changePinStyle(
                BackgroundType.verificationPinErrorCodeView,
                TextStyleType.verificationPinErrorFrame,
                View.VISIBLE);
    }

    public String getVerificationPinCode() {
        return mStringBuffer.toString();
    }

    private void changePinStyle(
            BackgroundType backgroundResId,
            TextStyleType textStyleType,
            @ViewVisibility int errorHintVisibility) {
        for (TextView tvPin : mTvPin) {
            tvPin.setBackgroundResource(backgroundResId.getValue());
            tvPin.setTextAppearance(textStyleType.getValue());
        }
        mTvErrorHint.setVisibility(errorHintVisibility);
    }

    private void changeSinglePinStyle(
            int position, BackgroundType backgroundType, TextStyleType textStyleType) {
        if (position >= 0 && position < mTvPin.length) {
            mTvPin[position].setBackgroundResource(backgroundType.getValue());
            mTvPin[position].setTextAppearance(textStyleType.getValue());
        }
    }

    private void initComponents() {
        mEtPin = findViewById(R.id.et_pin);
        mEtPin.setShowSoftInputOnFocus(false);

        mTvErrorHint = findViewById(R.id.tv_error_hint);
        mBtnVerify = findViewById(R.id.bt_verify);

        mNumberKeyboardView = findViewById(R.id.verify_num_key);
        mNumberKeyboardView.setCallback(numberKeyboardViewCallback);

        int pinLength = VIEW_IDS.length;
        mTvPin = new TextView[pinLength];
        for (int i = 0; i < pinLength; i++) {
            mTvPin[i] = this.findViewById(VIEW_IDS[i]);
        }

        mCodeManager = new VerificationCodeManager(mEtPin);

        mScrollView = findViewById(R.id.sv_ver_code);
        mRlScrollView = (RelativeLayout.LayoutParams) mScrollView.getLayoutParams();
        //        mPinCodeLayout = findViewById(R.id.ll_pin_code);
        //        mPinCodeLayout.setOnClickListener(new OnClickListener() {
        //            @Override
        //            public void onClick(View v) {
        //                setNumberKeyboardVisibility(VISIBLE);
        //                if (mTvPin[0].getText() == EMPTY_STRING) {
        //                    changeSinglePinStyle(0, R.drawable.verification_pin_code_on_view,
        // R.style.verification_pin_on_frame);
        //                }
        //            }
        //        });
    }

    private boolean isNumeric(String string) {
        if (TextUtils.isEmpty(string)) {
            Log.w(TAG, "string is null");
            return false;
        }
        Pattern pattern = Pattern.compile("[0-9]*");
        if (pattern == null) {
            Log.w(TAG, "pattern is null");
            return false;
        } else {
            Matcher isNum = pattern.matcher(string);
            return isNum.matches();
        }
    }

    private void setPinListener() {
        mEtPin.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        if (!editable.toString().equals(EMPTY_STRING)) {
                            if (mStringBuffer.length() > VIEW_IDS.length - 1) {
                                mEtPin.setText(EMPTY_STRING);
                                return;
                            } else {
                                mStringBuffer.append(editable);
                                mPinCodeIndex = mStringBuffer.length();
                                mEtPin.setText(EMPTY_STRING);
                            }
                            if (mStringBuffer.length() > 0) {
                                changeSinglePinStyle(
                                        mStringBuffer.length() - 1,
                                        BackgroundType.verificationPinCodeOnView,
                                        TextStyleType.verificationPinOnFrame);
                                changeSinglePinStyle(
                                        mStringBuffer.length(),
                                        BackgroundType.verificationPinCodeOnView,
                                        TextStyleType.verificationPinOnFrame);
                            }

                            for (int i = 0; i < mStringBuffer.length(); i++) {
                                mTvPin[i].setText(
                                        String.valueOf(mStringBuffer.toString().charAt(i)));
                            }
                        }
                    }
                });

        mEtPin.setOnKeyListener(
                new OnKeyListener() {
                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_DEL
                                && event.getAction() == KeyEvent.ACTION_DOWN) {
                            if (mStringBuffer.length() == PinCodeUtil.PIN_CODE_LENGTH) {
                                changePinStyle(
                                        BackgroundType.verificationPinCodeOnView,
                                        TextStyleType.verificationPinOnFrame,
                                        INVISIBLE);
                            }

                            if (mPinCodeIndex == 0) {
                                return true;
                            } else if (mPinCodeIndex > 0) {
                                mStringBuffer.delete(mPinCodeIndex - 1, mPinCodeIndex);
                                mPinCodeIndex--;
                                mTvPin[mStringBuffer.length()].setText(EMPTY_STRING);
                                changeSinglePinStyle(
                                        mStringBuffer.length(),
                                        BackgroundType.verificationPinCodeOnView,
                                        TextStyleType.verificationPinOnFrame);
                                changeSinglePinStyle(
                                        mStringBuffer.length() + 1,
                                        BackgroundType.verificationPinCodeOffView,
                                        TextStyleType.verificationPinOffFrame);
                                notifyPinCodeChange();
                            }
                            return true;
                        }
                        return false;
                    }
                });
    }

    void setPinCodeChangeListener(PinCodeChangeListener listener) {
        mPinCodeChangeListener = listener;
    }

    void notifyPinCodeChange() {
        if (mPinCodeChangeListener != null) {
            mPinCodeChangeListener.onPinCodeChanged();
        }
    }

    boolean isKeyboardShowing() {
        return mNumberKeyboardView.getVisibility() == VISIBLE;
    }

    void setNumberKeyboardVisibility(@ViewVisibility int visibility) {
        mNumberKeyboardView.setVisibility(visibility);
        refreshLayout(visibility);
    }

    void refreshLayout(@ViewVisibility int keyboardVisibility) {
        if (keyboardVisibility == VISIBLE) {
            mRlScrollView.removeRule(ALIGN_PARENT_BOTTOM);
            changeSinglePinStyle(
                    mStringBuffer.length(),
                    BackgroundType.verificationPinCodeOnView,
                    TextStyleType.verificationPinOnFrame);
        } else {
            mRlScrollView.addRule(ALIGN_PARENT_BOTTOM, 1);
            mRlScrollView.height = LayoutParams.MATCH_PARENT;
            changeSinglePinStyle(
                    mStringBuffer.length(),
                    BackgroundType.verificationPinCodeOffView,
                    TextStyleType.verificationPinOffFrame);
        }
        mScrollView.requestLayout();
    }

    private enum BackgroundType {
        verificationPinCodeOnView(R.drawable.verification_pin_code_on_view),
        verificationPinCodeOffView(R.drawable.verification_pin_code_off_view),
        verificationPinErrorCodeView(R.drawable.verification_pin_error_code_view);

        private final int value;

        BackgroundType(int value) {
            this.value = value;
        }

        int getValue() {
            return value;
        }
    }

    private enum TextStyleType {
        verificationPinOnFrame(R.style.verification_pin_on_frame),
        verificationPinOffFrame(R.style.verification_pin_off_frame),
        verificationPinErrorFrame(R.style.verification_pin_error_frame);

        private final int value;

        TextStyleType(int value) {
            this.value = value;
        }

        int getValue() {
            return value;
        }
    }

    @IntDef({VISIBLE, INVISIBLE})
    private @interface ViewVisibility {
    }

    interface PinCodeChangeListener {
        void onPinCodeChanged();
    }
}
