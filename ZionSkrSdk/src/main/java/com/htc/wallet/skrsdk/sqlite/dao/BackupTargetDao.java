package com.htc.wallet.skrsdk.sqlite.dao;

import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_BAD;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_NO_RESPONSE;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_OK;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity.BACKUP_TARGET_STATUS_REQUEST;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;

import java.util.List;

@Dao
public interface BackupTargetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertAll(List<BackupTargetEntity> backupTargetEntityList);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(BackupTargetEntity backupTargetEntity);

    @WorkerThread
    @Query("SELECT * FROM backupTarget")
    List<BackupTargetEntity> getAll();

    @WorkerThread
    @Query("SELECT * FROM backupTarget WHERE status = " + BACKUP_TARGET_STATUS_REQUEST)
    List<BackupTargetEntity> getAllPending();

    @WorkerThread
    @Query("SELECT * FROM backupTarget WHERE status = " + BACKUP_TARGET_STATUS_OK)
    List<BackupTargetEntity> getAllOK();

    @WorkerThread
    @Query(
            "SELECT * FROM backupTarget WHERE status = "
                    + BACKUP_TARGET_STATUS_OK
                    + " OR status = "
                    + BACKUP_TARGET_STATUS_NO_RESPONSE)
    List<BackupTargetEntity> getAllOKAndNoResponse();

    @WorkerThread
    @Query("SELECT * FROM backupTarget WHERE status = " + BACKUP_TARGET_STATUS_BAD)
    List<BackupTargetEntity> getAllBad();

    @WorkerThread
    @Query("SELECT * FROM backupTarget WHERE uUIDHash = :uuidHash")
    BackupTargetEntity get(String uuidHash);

    @WorkerThread
    @Delete
    int remove(@NonNull BackupTargetEntity backupTargetEntity);

    @WorkerThread
    @Query("SELECT * FROM backupTarget WHERE (uuidHash IS null OR uuidHash = '')")
    List<BackupTargetEntity> getOnlyNameList();

    @WorkerThread
    @Query("SELECT seedIndex FROM backupTarget")
    List<Integer> getSeedIndex();

    @WorkerThread
    @Query("DELETE FROM backupTarget")
    void clearData();

    @WorkerThread
    @Update
    int update(BackupTargetEntity... backupTargetEntities);
}
