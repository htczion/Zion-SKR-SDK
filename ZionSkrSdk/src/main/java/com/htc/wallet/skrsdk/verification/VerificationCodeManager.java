package com.htc.wallet.skrsdk.verification;

import android.view.KeyEvent;
import android.widget.EditText;

public class VerificationCodeManager {
    EditText mEtpinCodeText;

    public VerificationCodeManager(EditText textView) {
        mEtpinCodeText = textView;
        mEtpinCodeText.setKeyListener(null);
    }

    public void inputKey(VerificationCodeNumberKeyboardView.KeyType keyType) {
        String key;

        switch (keyType) {
            case ZERO:
                key = "0";
                break;
            case ONE:
                key = "1";
                break;
            case TWO:
                key = "2";
                break;
            case THREE:
                key = "3";
                break;
            case FOUR:
                key = "4";
                break;
            case FIVE:
                key = "5";
                break;
            case SIX:
                key = "6";
                break;
            case SEVEN:
                key = "7";
                break;
            case EIGHT:
                key = "8";
                break;
            case NINE:
                key = "9";
                break;
            case DEL:
                deletePinCode();
                return;
            case DONE:
                //                    keyboard_frameLayout.setVisibility(View.INVISIBLE);
                return;
            default:
                return;
        }
        mEtpinCodeText.setText(key);
    }

    private void deletePinCode() {
        mEtpinCodeText.dispatchKeyEvent(
                new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, 0));
    }
}
