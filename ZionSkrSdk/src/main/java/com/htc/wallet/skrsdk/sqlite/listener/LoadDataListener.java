package com.htc.wallet.skrsdk.sqlite.listener;

import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;

public interface LoadDataListener {
    void onLoadFinished(
            final BackupSourceEntity backupSourceEntity,
            final BackupTargetEntity backupTargetEntity,
            final RestoreSourceEntity restoreSourceEntity,
            final RestoreTargetEntity restoreTargetEntity);
}
