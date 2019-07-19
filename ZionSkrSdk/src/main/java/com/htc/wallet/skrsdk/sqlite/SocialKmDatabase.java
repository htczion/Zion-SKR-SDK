package com.htc.wallet.skrsdk.sqlite;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.NonNull;

import com.htc.wallet.skrsdk.sqlite.dao.BackupSourceDao;
import com.htc.wallet.skrsdk.sqlite.dao.BackupTargetDao;
import com.htc.wallet.skrsdk.sqlite.dao.RestoreSourceDao;
import com.htc.wallet.skrsdk.sqlite.dao.RestoreTargetDao;
import com.htc.wallet.skrsdk.sqlite.entity.BackupSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.BackupTargetEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreSourceEntity;
import com.htc.wallet.skrsdk.sqlite.entity.RestoreTargetEntity;
import com.htc.wallet.skrsdk.util.LogUtil;

@Database(
        entities = {
                BackupSourceEntity.class,
                BackupTargetEntity.class,
                RestoreSourceEntity.class,
                RestoreTargetEntity.class
        },
        version = 3,
        exportSchema = false)
public abstract class SocialKmDatabase extends RoomDatabase {
    public static final String TAG = "SocialKmDatabase";
    public static final String DATABASE_NAME = "social_key_recovery.db";
    private static volatile SocialKmDatabase sInstance;

    public static SocialKmDatabase getInstance(@NonNull final Context context) {
        if (sInstance == null) {
            synchronized (SocialKmDatabase.class) {
                if (sInstance == null) {
                    sInstance = buildDatabase(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private static final Migration MIGRATION_1_2 =
            new Migration(1, 2) {
                @Override
                public void migrate(SupportSQLiteDatabase database) {
                    if (database.getVersion() == startVersion) {
                        LogUtil.logInfo(
                                TAG,
                                "Migration from ver"
                                        + database.getVersion()
                                        + " to ver"
                                        + endVersion);
                        database.execSQL(
                                "ALTER TABLE backupSource "
                                        + " ADD COLUMN isTest INTEGER NOT NULL DEFAULT 0");
                        database.execSQL(
                                "ALTER TABLE restoreTarget "
                                        + " ADD COLUMN isTest INTEGER NOT NULL DEFAULT 0");
                    }
                }
            };

    private static final Migration MIGRATION_2_3 =
            new Migration(2, 3) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase database) {
                    if (database.getVersion() == startVersion) {
                        LogUtil.logInfo(
                                TAG,
                                "Migration from ver"
                                        + database.getVersion()
                                        + " to ver"
                                        + endVersion);
                        database.execSQL(
                                "ALTER TABLE backupSource " + " ADD COLUMN whisperPub TEXT");
                        database.execSQL(
                                "ALTER TABLE backupSource " + " ADD COLUMN pushyToken TEXT");
                        database.execSQL(
                                "ALTER TABLE backupTarget " + " ADD COLUMN whisperPub TEXT");
                        database.execSQL(
                                "ALTER TABLE backupTarget " + " ADD COLUMN pushyToken TEXT");
                        database.execSQL(
                                "ALTER TABLE restoreSource " + " ADD COLUMN whisperPub TEXT");
                        database.execSQL(
                                "ALTER TABLE restoreSource " + " ADD COLUMN pushyToken TEXT");
                        database.execSQL(
                                "ALTER TABLE restoreTarget " + " ADD COLUMN whisperPub TEXT");
                        database.execSQL(
                                "ALTER TABLE restoreTarget " + " ADD COLUMN pushyToken TEXT");
                    }
                }
            };

    @NonNull
    private static SocialKmDatabase buildDatabase(@NonNull final Context appContext) {
        return Room.databaseBuilder(appContext, SocialKmDatabase.class, DATABASE_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build();
    }

    public static void closeDatabase() {
        if (sInstance != null) {
            sInstance.close();
            sInstance = null;
        }
    }

    public static void clearAllTablesData(@NonNull final Context context) {
        if (sInstance == null) {
            LogUtil.logError(TAG, "SocialKmDatabase instance is null");
            getInstance(context).clearAllTables();
            return;
        }
        sInstance.clearAllTables();
    }

    public abstract BackupSourceDao backupSourceDao();

    public abstract BackupTargetDao backupTargetDao();

    public abstract RestoreSourceDao restoreSourceDao();

    public abstract RestoreTargetDao restoreTargetDao();
}
