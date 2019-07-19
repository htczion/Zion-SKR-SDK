package com.htc.wallet.skrsdk.sqlite.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;

import java.util.List;

@Dao
public interface RestoreSourceDao {
    @WorkerThread
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertAll(List<RestoreSourceEntity> restoreSourceEntityList);

    @WorkerThread
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(RestoreSourceEntity restoreSourceEntity);

    @WorkerThread
    @Query("SELECT * FROM restoreSource")
    List<RestoreSourceEntity> getAll();

    @Query("SELECT * FROM restoreSource WHERE uuidHash =:uuidHash")
    RestoreSourceEntity getWithUUIDHash(String uuidHash);

    @WorkerThread
    @Delete
    int remove(@NonNull RestoreSourceEntity restoreSourceEntity);

    @WorkerThread
    @Delete
    int removeAll(@NonNull List<RestoreSourceEntity> restoreSourceEntityList);

    @WorkerThread
    @Query("DELETE FROM restoreSource")
    void clearData();

    @WorkerThread
    @Update
    int update(RestoreSourceEntity... restoreSourceEntities);
}
