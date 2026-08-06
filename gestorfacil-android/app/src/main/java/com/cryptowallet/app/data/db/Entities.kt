package com.cryptowallet.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: Int = 0,
    val index: Int,
    val address: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tokens")
data class TokenEntity(
    @PrimaryKey val id: String,
    val chainId: Long,
    val address: String,
    val symbol: String,
    val name: String,
    val decimals: Int,
    val isNative: Boolean = false,
    val enabled: Boolean = true,
    val custom: Boolean = false
)

@Entity(tableName = "tx_history")
data class TxRecordEntity(
    @PrimaryKey val id: String,
    val chainId: Long,
    val hash: String,
    val from: String,
    val to: String,
    val tokenAddress: String?,
    val tokenSymbol: String,
    val amount: String,
    val amountRaw: String,
    val feeWei: String,
    val status: String,
    val timestamp: Long,
    val type: String
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
