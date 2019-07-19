package com.htc.wallet.skrsdk.sqlite.entity;

public interface DataEntity {

    void encrypt();

    void decrypt();

    boolean isSensitiveDataEncrypted();

    boolean isLegacyDataUpdatedV1();
}
