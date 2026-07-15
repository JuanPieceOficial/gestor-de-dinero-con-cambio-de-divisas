package com.gestorfacil.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val category: String,
    val limit: Double
)
