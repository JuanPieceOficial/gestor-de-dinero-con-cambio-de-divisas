package com.gestorfacil.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gestorfacil.app.data.database.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun getAllFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'income'")
    fun totalIncomeFlow(): Flow<Double>

    @Query("SELECT COALESCE(SUM(ABS(amount)), 0) FROM transactions WHERE type = 'expense'")
    fun totalExpenseFlow(): Flow<Double>

    @Query("SELECT COALESCE(SUM(ABS(amount)), 0) FROM transactions WHERE type = 'expense' AND category = :category")
    suspend fun totalSpentByCategory(category: String): Double
}
