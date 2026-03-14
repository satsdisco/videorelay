package com.videorelay.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        VideoEntity::class,
        DownloadEntity::class,
        ViewHistoryEntity::class,
        ProfileEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun downloadDao(): DownloadDao
    abstract fun viewHistoryDao(): ViewHistoryDao
    abstract fun profileDao(): ProfileDao
}
