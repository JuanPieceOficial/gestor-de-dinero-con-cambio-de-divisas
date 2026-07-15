package com.gestorfacil.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gestorfacil.app.data.database.dao.BudgetDao
import com.gestorfacil.app.data.database.dao.TransactionDao

@Database(
    entities = [TransactionEntity::class, BudgetEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gestorfacil_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
