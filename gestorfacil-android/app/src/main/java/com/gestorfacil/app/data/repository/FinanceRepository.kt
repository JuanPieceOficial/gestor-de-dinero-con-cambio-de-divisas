package com.gestorfacil.app.data.repository

import com.gestorfacil.app.data.auth.SupabaseProvider
import com.gestorfacil.app.data.database.AppDatabase
import com.gestorfacil.app.data.database.BudgetEntity
import com.gestorfacil.app.data.database.TransactionEntity
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val db: AppDatabase) {

    private val transactionDao = db.transactionDao()
    private val budgetDao = db.budgetDao()

    fun allTransactions(userId: String): Flow<List<TransactionEntity>> =
        transactionDao.getAllByUserFlow(userId)

    fun totalIncome(userId: String): Flow<Double> =
        transactionDao.totalIncomeByUserFlow(userId)

    fun totalExpense(userId: String): Flow<Double> =
        transactionDao.totalExpenseByUserFlow(userId)

    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllFlow()

    suspend fun syncFromCloud(userId: String) {
        try {
            val remote = SupabaseProvider.db.from("transactions")
                .select(Columns.list("id", "date", "description", "amount", "category", "type", "user_id"))
                .decodeList<SupabaseTransaction>()
            if (remote.isNotEmpty()) {
                transactionDao.deleteAllByUser(userId)
                transactionDao.insertAll(remote.map { t ->
                    TransactionEntity(
                        id = t.id ?: 0,
                        date = t.date ?: "",
                        description = t.description ?: "",
                        amount = t.amount ?: 0.0,
                        category = t.category ?: "",
                        type = t.type ?: "expense",
                        userId = userId
                    )
                })
            }
        } catch (_: Exception) {
        }
    }

    suspend fun addTransaction(transaction: TransactionEntity, userId: String): Long {
        val local = transaction.copy(userId = userId)
        val localId = transactionDao.insert(local)

        try {
            SupabaseProvider.db.from("transactions").insert(
                SupabaseTransaction(
                    date = local.date,
                    description = local.description,
                    amount = local.amount,
                    category = local.category,
                    type = local.type,
                    user_id = userId
                )
            )
        } catch (_: Exception) {
        }

        return localId
    }

    suspend fun deleteTransaction(id: Long, userId: String) {
        transactionDao.deleteById(id)
        try {
            SupabaseProvider.db.from("transactions")
                .delete { filter { eq("id", id) } }
        } catch (_: Exception) {
        }
    }

    suspend fun updateTransaction(transaction: TransactionEntity, userId: String) {
        transactionDao.update(transaction)
        try {
            SupabaseProvider.db.from("transactions").update(
                SupabaseTransaction(
                    id = transaction.id,
                    date = transaction.date,
                    description = transaction.description,
                    amount = transaction.amount,
                    category = transaction.category,
                    type = transaction.type,
                    user_id = userId
                )
            ) { filter { eq("id", transaction.id) } }
        } catch (_: Exception) {
        }
    }

    suspend fun getSpentByCategory(category: String): Double =
        transactionDao.totalSpentByCategory(category)

    suspend fun updateBudget(category: String, limit: Double) =
        budgetDao.updateLimit(category, limit)
}

@kotlinx.serialization.Serializable
data class SupabaseTransaction(
    val id: Long? = null,
    val date: String? = null,
    val description: String? = null,
    val amount: Double? = null,
    val category: String? = null,
    val type: String? = null,
    val user_id: String? = null
)
