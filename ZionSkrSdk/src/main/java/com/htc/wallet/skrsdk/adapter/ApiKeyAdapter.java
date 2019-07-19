package com.htc.wallet.skrsdk.adapter;

import android.support.annotation.Nullable;

public abstract class ApiKeyAdapter {
    @Nullable
    public abstract String getGoogleApiKey();

    @Nullable
    public abstract String getGoogleClientId();

    @Nullable
    public abstract String getGoogleClientSecret();

    @Nullable
    public abstract String getTZApiKeyStage();

    @Nullable
    public abstract String getTZApiKeyProduction();

    @Nullable
    public abstract String getAesKey();

    @Nullable
    public abstract String getSafetyNetApiKeyStage();

    @Nullable
    public abstract String getSafetyNetApiKeyProduction();

    @Nullable
    public String getBranchKey() {
        return null;
    }

    @Nullable
    public String getWhisperApiKeyStage() {
        return null;
    }

    @Nullable
    public String getMailServerPwdStage() {
        return null;
    }
}
