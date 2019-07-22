package com.shxhzhxx.housekeeper

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


const val DATABASE_NAME = "housekeeper-db"

@Database(entities = [Block::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        // For Singleton instantiation
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            ).build().also {
                instance = it
            }
        }
    }
    abstract fun blockDao(): BlockDao
}