package com.gestorfacil.app.data.repository

import com.gestorfacil.app.data.auth.SupabaseProvider
import com.gestorfacil.app.data.database.AppDatabase
import com.gestorfacil.app.data.database.BudgetEntity
import com.gestorfacil.app.data.database.CategoryEntity
import com.gestorfacil.app.data.database.TransactionEntity
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FinanceRepository(private val db: AppDatabase) {

    private val transactionDao = db.transactionDao()
    private val budgetDao = db.budgetDao()
    private val categoryDao = db.categoryDao()

    fun allTransactions(userId: String): Flow<List<TransactionEntity>> =
        transactionDao.getAllByUserFlow(userId)

    fun totalIncome(userId: String): Flow<Double> =
        transactionDao.totalIncomeByUserFlow(userId)

    fun totalExpense(userId: String): Flow<Double> =
        transactionDao.totalExpenseByUserFlow(userId)

    fun allCategories(userId: String): Flow<List<CategoryEntity>> =
        categoryDao.getAllByUserFlow(userId)

    fun categoriesByType(userId: String, type: String): Flow<List<CategoryEntity>> =
        flow {
            emit(categoryDao.getByUserAndType(userId, type))
        }

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

    suspend fun syncCategoriesFromCloud(userId: String) {
        try {
            val remote = SupabaseProvider.db.from("categories")
                .select(Columns.list("id", "user_id", "name", "type", "icon", "color", "order", "is_default", "created_at", "updated_at"))
                .decodeList<SupabaseCategory>()
            if (remote.isNotEmpty()) {
                categoryDao.deleteAllByUser(userId)
                categoryDao.insertAll(remote.map { c ->
                    CategoryEntity(
                        id = c.id ?: "",
                        userId = c.user_id ?: userId,
                        name = c.name ?: "",
                        type = c.type ?: "expense",
                        icon = c.icon,
                        color = c.color,
                        order = c.order ?: 0,
                        isDefault = c.is_default ?: false,
                        createdAt = c.created_at ?: System.currentTimeMillis(),
                        updatedAt = c.updated_at ?: System.currentTimeMillis()
                    )
                })
            } else {
                // Seed defaults if empty
                seedDefaultCategories(userId)
            }
        } catch (_: Exception) {
            seedDefaultCategories(userId)
        }
    }

    private suspend fun seedDefaultCategories(userId: String) {
        val defaults = listOf(
            CategoryEntity("cat_food_${System.currentTimeMillis()}", userId, "Alimentación", "expense", isDefault = true),
            CategoryEntity("cat_transport_${System.currentTimeMillis()}", userId, "Transporte", "expense", isDefault = true),
            CategoryEntity("cat_fun_${System.currentTimeMillis()}", userId, "Ocio", "expense", isDefault = true),
            CategoryEntity("cat_home_${System.currentTimeMillis()}", userId, "Hogar", "expense", isDefault = true),
            CategoryEntity("cat_health_${System.currentTimeMillis()}", userId, "Salud", "expense", isDefault = true),
            CategoryEntity("cat_edu_${System.currentTimeMillis()}", userId, "Educación", "expense", isDefault = true),
            CategoryEntity("cat_salary_${System.currentTimeMillis()}", userId, "Salario", "income", isDefault = true),
            CategoryEntity("cat_freelance_${System.currentTimeMillis()}", userId, "Freelance", "income", isDefault = true),
            CategoryEntity("cat_invest_${System.currentTimeMillis()}", userId, "Inversión", "income", isDefault = true),
            CategoryEntity("cat_other_${System.currentTimeMillis()}", userId, "Otros", "expense", isDefault = true),
        )
        categoryDao.insertAll(defaults)
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

    suspend fun addCategory(category: CategoryEntity, userId: String) {
        val local = category.copy(userId = userId)
        categoryDao.insert(local)
        try {
            SupabaseProvider.db.from("categories").insert(
                SupabaseCategory(
                    id = local.id,
                    user_id = local.userId,
                    name = local.name,
                    type = local.type,
                    icon = local.icon,
                    color = local.color,
                    order = local.order,
                    is_default = local.isDefault,
                    created_at = local.createdAt,
                    updated_at = local.updatedAt
                )
            )
        } catch (_: Exception) {
        }
    }

    suspend fun updateCategory(category: CategoryEntity, userId: String) {
        val local = category.copy(userId = userId, updatedAt = System.currentTimeMillis())
        categoryDao.update(local)
        try {
            SupabaseProvider.db.from("categories").update(
                SupabaseCategory(
                    id = local.id,
                    user_id = local.userId,
                    name = local.name,
                    type = local.type,
                    icon = local.icon,
                    color = local.color,
                    order = local.order,
                    is_default = local.isDefault,
                    created_at = local.createdAt,
                    updated_at = local.updatedAt
                )
            ) { filter { eq("id", local.id) } }
        } catch (_: Exception) {
        }
    }

    suspend fun deleteCategory(id: String, userId: String) {
        categoryDao.deleteById(id, userId)
        try {
            SupabaseProvider.db.from("categories")
                .delete { filter { eq("id", id) } }
        } catch (_: Exception) {
        }
    }

    suspend fun getSpentByCategory(category: String, userId: String): Double =
        transactionDao.totalSpentByCategory(category, userId)

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

@kotlinx.serialization.Serializable
data class SupabaseCategory(
    val id: String? = null,
    val user_id: String? = null,
    val name: String? = null,
    val type: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val order: Int? = null,
    val is_default: Boolean? = null,
    val created_at: Long? = null,
    val updated_at: Long? = null
)