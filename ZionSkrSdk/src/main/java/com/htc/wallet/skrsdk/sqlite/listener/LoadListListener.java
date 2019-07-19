package com.htc.wallet.skrsdk.sqlite.listener;

import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;

import java.util.List;

public interface LoadListListener {
    void onLoadFinished(
            final List<BackupSourceEntity> backupSourceEntityList,
            final List<BackupTargetEntity> backupTargetEntityList,
            final List<RestoreSourceEntity> restoreSourceEntityList,
            final List<RestoreTargetEntity> restoreTargetEntityList);
}
