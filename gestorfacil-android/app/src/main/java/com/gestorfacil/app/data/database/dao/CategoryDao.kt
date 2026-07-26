package com.gestorfacil.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gestorfacil.app.data.database.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY type ASC, name ASC")
    fun getAllByUserFlow(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY type ASC, name ASC")
    suspend fun getAllByUser(userId: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE userId = :userId AND type = :type ORDER BY name ASC")
    suspend fun getByUserAndType(userId: String, type: String): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id AND userId = :userId")
    suspend fun deleteById(id: String, userId: String)

    @Query("DELETE FROM categories WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: String)

    @Query("SELECT * FROM categories WHERE userId = :userId AND isDefault = 1")
    suspend fun getDefaultsByUser(userId: String): List<CategoryEntity>
}