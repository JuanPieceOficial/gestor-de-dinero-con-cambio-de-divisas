package com.gestorfacil.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val type: String, // "income" | "expense"
    val icon: String? = null,
    val color: String? = null,
    val order: Int = 0,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)