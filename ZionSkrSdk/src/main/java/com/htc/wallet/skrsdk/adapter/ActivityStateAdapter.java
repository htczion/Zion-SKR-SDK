package com.htc.wallet.skrsdk.adapter;

public abstract class ActivityStateAdapter {

    public boolean getIsAuthPassed() {
        return true;
    }

    public void setIsAuthPassed(boolean isAuthPassed) {
    }

    public boolean getIsCheckPhoneState() {
        return false;
    }

    public void setIsCheckPhoneState(boolean isCheckPhoneState) {
    }
}
