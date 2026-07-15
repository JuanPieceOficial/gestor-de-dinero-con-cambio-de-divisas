package com.gestorfacil.legacy

import androidx.multidex.MultiDexApplication
import com.gestorfacil.legacy.data.database.AppDatabase
import com.gestorfacil.legacy.data.database.BudgetEntity
import com.gestorfacil.legacy.data.repository.FinanceRepository
import com.gestorfacil.legacy.data.settings.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GestorFacilLegacyApp : MultiDexApplication() {

    lateinit var database: AppDatabase
    lateinit var repository: FinanceRepository
    lateinit var settingsManager: SettingsManager

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
                "Alimentación", "Transporte", "Ocio",
                "Hogar", "Salud", "Educación", "Otros"
            )
            for (cat in categories) {
                database.budgetDao().upsert(BudgetEntity(category = cat, limit = 500.0))
            }
        }
    }
}
