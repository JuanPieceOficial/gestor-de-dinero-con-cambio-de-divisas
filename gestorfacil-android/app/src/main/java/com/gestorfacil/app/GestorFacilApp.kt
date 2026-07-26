package com.gestorfacil.app

import android.app.Application
import com.gestorfacil.app.data.auth.AuthRepository
import com.gestorfacil.app.data.database.AppDatabase
import com.gestorfacil.app.data.database.BudgetEntity
import com.gestorfacil.app.data.repository.FinanceRepository
import com.gestorfacil.app.data.settings.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GestorFacilApp : Application() {

    lateinit var database: AppDatabase
    lateinit var repository: FinanceRepository
    lateinit var settingsManager: SettingsManager
    val authRepository = AuthRepository()

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = FinanceRepository(database)
        settingsManager = SettingsManager(this)
        seedDefaultBudgets()
    }

    private fun seedDefaultBudgets() {
        CoroutineScope(Dispatchers.IO).launch {
            val categories = listOf(
                "Alimentaci\u00f3n", "Transporte", "Ocio",
                "Hogar", "Salud", "Educaci\u00f3n", "Otros"
            )
            for (cat in categories) {
                database.budgetDao().upsert(BudgetEntity(category = cat, limit = 500.0))
            }
        }
    }
}
