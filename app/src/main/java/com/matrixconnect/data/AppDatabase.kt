package com.matrixconnect.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.matrixconnect.data.dao.ConnectionHistoryDao
import com.matrixconnect.data.dao.ServerConfigDao
import com.matrixconnect.data.entities.ConnectionHistory
import com.matrixconnect.data.entities.ServerConfig

@Database(entities = [ServerConfig::class, ConnectionHistory::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverConfigDao(): ServerConfigDao
    abstract fun connectionHistoryDao(): ConnectionHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "matrix_connect_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
