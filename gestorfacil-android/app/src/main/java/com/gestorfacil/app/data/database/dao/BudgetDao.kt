package com.gestorfacil.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gestorfacil.app.data.database.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY category ASC")
    fun getAllFlow(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Query("UPDATE budgets SET `limit` = :limit WHERE category = :category")
    suspend fun updateLimit(category: String, limit: Double)
}
