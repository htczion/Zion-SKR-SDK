package com.htc.wallet.skrsdk.backup;

public class DriveAccount {
    private String mName;
    private String mEmail;
    private boolean mIsChosen = false;

    DriveAccount(final String name, final String email) {
        mName = name;
        mEmail = email;
    }

    public void setIsChosen(boolean isChosen) {
        mIsChosen = isChosen;
    }

    public boolean getIsChosen() {
        return mIsChosen;
    }

    public String getName() {
        return mName;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setName(final String name) {
        mName = name;
    }

    public void setEmail(final String email) {
        mEmail = email;
    }
}
