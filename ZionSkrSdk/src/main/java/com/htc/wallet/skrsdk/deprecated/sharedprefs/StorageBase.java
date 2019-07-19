package com.htc.wallet.skrsdk.deprecated.sharedprefs;

public abstract class StorageBase {
    protected static final int STORAGE_VERSION = 1;

    protected int mVersion = -1;

    public int getVersion() {
        return mVersion;
    }

    abstract boolean upgrade();
}
