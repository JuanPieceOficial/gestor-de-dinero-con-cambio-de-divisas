package com.gestorfacil.app.data.repository

import com.gestorfacil.app.data.database.AppDatabase
import com.gestorfacil.app.data.database.BudgetEntity
import com.gestorfacil.app.data.database.TransactionEntity
import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val db: AppDatabase) {

    private val transactionDao = db.transactionDao()
    private val budgetDao = db.budgetDao()

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllFlow()
    val totalIncome: Flow<Double> = transactionDao.totalIncomeFlow()
    val totalExpense: Flow<Double> = transactionDao.totalExpenseFlow()
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllFlow()

    suspend fun addTransaction(transaction: TransactionEntity): Long =
        transactionDao.insert(transaction)

    suspend fun deleteTransaction(id: Long) =
        transactionDao.deleteById(id)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.update(transaction)

    suspend fun getSpentByCategory(category: String): Double =
        transactionDao.totalSpentByCategory(category)

    suspend fun updateBudget(category: String, limit: Double) =
        budgetDao.updateLimit(category, limit)
}
