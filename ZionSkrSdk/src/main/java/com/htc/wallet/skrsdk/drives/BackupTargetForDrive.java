package com.htc.wallet.skrsdk.drives;

import com.google.gson.annotations.SerializedName;

public class BackupTargetForDrive {
    @SerializedName("mName")
    private String mName;

    public BackupTargetForDrive(final String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setName(final String name) {
        mName = name;
    }
}
