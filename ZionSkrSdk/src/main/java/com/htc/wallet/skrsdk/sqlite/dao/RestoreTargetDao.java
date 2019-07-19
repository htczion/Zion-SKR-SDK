package com.htc.wallet.skrsdk.sqlite.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;

import java.util.List;

@Dao
public interface RestoreTargetDao {
    @WorkerThread
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertAll(List<RestoreTargetEntity> restoreTargetEntityList);

    @WorkerThread
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(RestoreTargetEntity restoreTargetEntity);

    @WorkerThread
    @Query("SELECT * FROM restoreTarget")
    List<RestoreTargetEntity> getAll();

    @WorkerThread
    @Query("SELECT * FROM restoreTarget WHERE emailHash = :emailHash AND uuidHash = :uuidHash")
    RestoreTargetEntity get(@NonNull String emailHash, @NonNull String uuidHash);

    @WorkerThread
    @Query("SELECT * FROM restoreTarget WHERE uuidHash = :uuidHash")
    RestoreTargetEntity getWithUUIDHash(@NonNull String uuidHash);

    @WorkerThread
    @Query("SELECT * FROM restoreTarget WHERE emailHash = :emailHash")
    List<RestoreTargetEntity> getAllByEmailHash(String emailHash);

    @WorkerThread
    @Delete
    int remove(@NonNull RestoreTargetEntity restoreTargetEntity);

    @WorkerThread
    @Delete
    int removeAll(@NonNull List<RestoreTargetEntity> restoreTargetEntityList);

    @WorkerThread
    @Update
    int update(RestoreTargetEntity... restoreTargetEntities);
}
