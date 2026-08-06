package com.cryptowallet.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY `index` ASC")
    fun getAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY `index` ASC")
    suspend fun getAllSync(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Int): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: AccountEntity)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}

@Dao
interface TokenDao {
    @Query("SELECT * FROM tokens WHERE enabled = 1 ORDER BY isNative DESC, symbol ASC")
    fun getAll(): Flow<List<TokenEntity>>

    @Query("SELECT * FROM tokens WHERE enabled = 1 ORDER BY isNative DESC, symbol ASC")
    suspend fun getAllSync(): List<TokenEntity>

    @Query("SELECT * FROM tokens WHERE chainId = :chainId AND enabled = 1 ORDER BY isNative DESC, symbol ASC")
    suspend fun getForChainSync(chainId: Long): List<TokenEntity>

    @Query("SELECT * FROM tokens WHERE id = :id")
    suspend fun getById(id: String): TokenEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(token: TokenEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tokens: List<TokenEntity>)

    @Query("UPDATE tokens SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Delete
    suspend fun delete(token: TokenEntity)

    @Query("DELETE FROM tokens")
    suspend fun deleteAll()
}

@Dao
interface TxDao {
    @Query("SELECT * FROM tx_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<TxRecordEntity>>

    @Query("SELECT * FROM tx_history ORDER BY timestamp DESC")
    suspend fun getAllSync(): List<TxRecordEntity>

    @Query("SELECT * FROM tx_history WHERE id = :id")
    suspend fun getById(id: String): TxRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tx: TxRecordEntity)

    @Query("UPDATE tx_history SET status = :status WHERE hash = :hash AND chainId = :chainId")
    suspend fun updateStatus(hash: String, chainId: Long, status: String)

    @Query("DELETE FROM tx_history WHERE status IN ('success', 'failed') AND timestamp < :before")
    suspend fun prune(before: Long)

    @Query("DELETE FROM tx_history")
    suspend fun deleteAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Query("SELECT `key`, value FROM settings")
    suspend fun getAll(): List<SettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(setting: SettingEntity)

    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun remove(key: String)

    @Query("DELETE FROM settings")
    suspend fun deleteAll()
}
