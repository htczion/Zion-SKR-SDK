package com.htc.wallet.skrsdk.sqlite.dao;

import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_DONE;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_FULL;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_OK;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_REQUEST;
import static com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity.BACKUP_SOURCE_STATUS_REQUEST_AUTO_BACKUP;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;

import java.util.List;

@Dao
public interface BackupSourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertAll(List<BackupSourceEntity> backupSources);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(BackupSourceEntity backupSources);

    @WorkerThread
    @Query("SELECT * FROM backupSource")
    List<BackupSourceEntity> getAll();

    @WorkerThread
    @Query(
            "SELECT * FROM backupSource WHERE status IN ("
                    + BACKUP_SOURCE_STATUS_REQUEST
                    + ","
                    + BACKUP_SOURCE_STATUS_OK
                    + ","
                    + BACKUP_SOURCE_STATUS_FULL
                    + ")")
    List<BackupSourceEntity> getAllPending();

    // Fixed issue, getAllOK only can get status BACKUP_SOURCE_STATUS_DONE.
    @WorkerThread
    @Query(
            "SELECT * FROM backupSource WHERE status = "
                    + BACKUP_SOURCE_STATUS_OK
                    + " OR status = "
                    + BACKUP_SOURCE_STATUS_DONE)
    List<BackupSourceEntity> getAllOK();

    @WorkerThread
    @Query("SELECT * FROM backupSource WHERE status = " + BACKUP_SOURCE_STATUS_REQUEST_AUTO_BACKUP)
    List<BackupSourceEntity> getRequestAutoBackup();

    @Nullable
    @WorkerThread
    @Query("SELECT * FROM backupSource WHERE emailHash = :emailHash AND uuidHash = :uuidHash")
    BackupSourceEntity getWithUUIDHash(@NonNull String emailHash, @NonNull String uuidHash);

    @WorkerThread
    @Query("SELECT * FROM backupSource WHERE emailHash = :emailHash")
    List<BackupSourceEntity> getAllByEmailHash(@NonNull String emailHash);

    // Only recover flow
    @WorkerThread
    @Query(
            "SELECT * FROM backupSource WHERE emailHash = :emailHash AND uuidHash = :uuidHash AND"
                    + " (status = "
                    + BACKUP_SOURCE_STATUS_OK
                    + " OR status = "
                    + BACKUP_SOURCE_STATUS_DONE
                    + ")")
    BackupSourceEntity getOK(@NonNull String emailHash, @NonNull String uuidHash);

    @WorkerThread
    @Delete
    int remove(@NonNull BackupSourceEntity backupSourceEntity);

    @WorkerThread
    @Update
    int update(BackupSourceEntity... backupSourceEntities);
}
