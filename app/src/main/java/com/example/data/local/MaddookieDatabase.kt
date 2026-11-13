package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WatchlistEntity::class], version = 1, exportSchema = false)
abstract class MaddookieDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        @Volatile
        private var INSTANCE: MaddookieDatabase? = null

        fun getInstance(context: Context): MaddookieDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MaddookieDatabase::class.java,
                    "maddookie_tracking.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
