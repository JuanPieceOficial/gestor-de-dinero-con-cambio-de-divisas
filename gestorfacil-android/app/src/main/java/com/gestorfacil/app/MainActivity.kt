package com.gestorfacil.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gestorfacil.app.data.database.TransactionEntity
import com.gestorfacil.app.ui.components.TransactionSheetDialog
import com.gestorfacil.app.ui.dolar.DolarScreen
import com.gestorfacil.app.ui.home.HomeScreen
import com.gestorfacil.app.ui.navigation.Screen
import com.gestorfacil.app.ui.settings.SettingsScreen
import com.gestorfacil.app.ui.theme.GestorFacilTheme
import com.gestorfacil.app.ui.transactions.TransactionsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = (applicationContext as GestorFacilApp)
            GestorFacilTheme(darkMode = app.settingsManager.useDarkMode) {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val app = LocalContext.current.applicationContext as GestorFacilApp
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var editingTransaction by rememberSaveable { mutableStateOf<TransactionEntity?>(null) }

    val sheetDismiss: () -> Unit = {
        showSheet = false
        editingTransaction = null
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (!showSettings) {
                TopAppBar(
                    title = {
                        Text(
                            text = "GestorFácil",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configuración"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        floatingActionButton = {
            if (!showSettings && selectedTab != 2) {
                FloatingActionButton(
                    onClick = { showSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar transacción"
                    )
                }
            }
        },
        bottomBar = {
            if (!showSettings) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    Screen.tabs.forEachIndexed { index, screen ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.label
                                )
                            },
                            label = {
                                Text(
                                    text = screen.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = showSettings,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            modifier = Modifier.padding(innerPadding),
            label = "content_anim"
        ) { settings ->
            if (settings) {
                SettingsScreen(
                    settingsManager = app.settingsManager,
                    onBack = { showSettings = false }
                )
            } else {
                val currency = app.settingsManager.selectedCurrency
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (slideInHorizontally { it * direction } + fadeIn())
                            .togetherWith(slideOutHorizontally { it * -direction } + fadeOut())
                    },
                    label = "tab_content"
                ) { tab ->
                    when (tab) {
                        0 -> HomeScreen(repository = app.repository, currency = currency)
                        1 -> TransactionsScreen(
                            repository = app.repository,
                            currency = currency,
                            onEdit = { t -> editingTransaction = t }
                        )
                        2 -> DolarScreen()
                    }
                }
            }
        }
    }

    if (showSheet || editingTransaction != null) {
        TransactionSheetDialog(
            settingsManager = app.settingsManager,
            onDismiss = sheetDismiss,
            onSave = { transaction ->
                CoroutineScope(Dispatchers.IO).launch {
                    if (editingTransaction != null) {
                        app.repository.updateTransaction(transaction)
                    } else {
                        app.repository.addTransaction(transaction)
                    }
                }
                sheetDismiss()
            },
            editTransaction = editingTransaction
        )
    }
}
