package com.gestorfacil.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val description: String,
    val amount: Double,
    val category: String,
    val type: String  // "income" or "expense"
)
