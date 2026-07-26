package com.gestorfacil.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestorfacil.app.data.database.CategoryEntity
import com.gestorfacil.app.data.model.Currency
import com.gestorfacil.app.data.repository.FinanceRepository
import com.gestorfacil.app.data.settings.SettingsManager
import com.gestorfacil.app.ui.theme.Accent
import com.gestorfacil.app.ui.theme.OnMuted
import com.gestorfacil.app.ui.theme.Primary
import com.gestorfacil.app.ui.theme.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    repository: FinanceRepository,
    userId: String,
    onBack: () -> Unit,
    onSignOut: () -> Unit = {}
) {
    val currentCurrency = remember { mutableStateOf(settingsManager.selectedCurrency) }
    val showAddCategory = remember { mutableStateOf(false) }
    val newCategoryName = remember { mutableStateOf("") }
    val newCategoryType = remember { mutableStateOf("expense") }
    val editingCategory = remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Text("Configuración", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Currency selection
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Moneda principal",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Currency.entries.forEach { currency ->
                        val isSelected = currentCurrency.value == currency
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentCurrency.value = currency
                                    settingsManager.selectedCurrency = currency
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${currency.symbol} ${currency.code}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Primary
                                    else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = currency.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = OnMuted
                            )
                            Spacer(Modifier.width(8.dp))
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (currency != Currency.entries.last()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }

            // Categories
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Categorías",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (!showAddCategory.value) {
                            IconButton(onClick = { showAddCategory.value = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Añadir", tint = Primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Add category form
                    if (showAddCategory.value || editingCategory.value != null) {
                        val isEditing = editingCategory.value != null
                        val nameInput = remember { mutableStateOf(editingCategory.value?.name ?: "") }
                        val typeInput = remember { mutableStateOf(editingCategory.value?.type ?: "expense") }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = nameInput.value,
                                onValueChange = { nameInput.value = it },
                                label = { Text("Nombre") },
                                placeholder = { Text("Ej: Suscripciones") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TypeChip(text = "Gasto", selected = typeInput.value == "expense", onClick = { typeInput.value = "expense" }, color = Color(0xFFEF4444))
                                TypeChip(text = "Ingreso", selected = typeInput.value == "income", onClick = { typeInput.value = "income" }, color = Color(0xFF29996E))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (isEditing) {
                                            val cat = editingCategory.value!!.copy(name = nameInput.value, type = typeInput.value)
                                            CoroutineScope(Dispatchers.IO).launch {
                                                repository.updateCategory(cat, userId)
                                            }
                                        } else {
                                            val cat = CategoryEntity(
                                                id = "cat_${System.currentTimeMillis()}",
                                                userId = userId,
                                                name = nameInput.value,
                                                type = typeInput.value,
                                                isDefault = false
                                            )
                                            CoroutineScope(Dispatchers.IO).launch {
                                                repository.addCategory(cat, userId)
                                            }
                                        }
                                        showAddCategory.value = false
                                        editingCategory.value = null
                                        nameInput.value = ""
                                        typeInput.value = "expense"
                                    },
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                                ) {
                                    Text(if (isEditing) "Guardar" else "Añadir", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        showAddCategory.value = false
                                        editingCategory.value = null
                                        nameInput.value = ""
                                        typeInput.value = "expense"
                                    },
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text("Cancelar")
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }

                    // Categories list - using repository flow
                    val expenseCategories = remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
                    val incomeCategories = remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
                    val allCategoriesFlow = repository.allCategories(userId)
                    androidx.compose.runtime.LaunchedEffect(allCategoriesFlow) {
                        allCategoriesFlow.collect { cats ->
                            expenseCategories.value = cats.filter { it.type == "expense" }
                            incomeCategories.value = cats.filter { it.type == "income" }
                        }
                    }

                    CategorySection(title = "Gastos", categories = expenseCategories.value, color = Color(0xFFEF4444))
                    if (incomeCategories.value.isNotEmpty()) {
                        CategorySection(title = "Ingresos", categories = incomeCategories.value, color = Color(0xFF29996E))
                    }
                }
            }

            // Dark mode
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Modo oscuro",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = settingsManager.useDarkMode,
                        onCheckedChange = {
                            settingsManager.useDarkMode = it
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                    )
                }
            }

            // About
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "GestorFácil",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Versión 1.0 · Tu salud financiera",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnMuted
                        )
                    }
                }
            }

            // Logout
            Button(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión", fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun CategorySection(
    title: String,
    categories: List<CategoryEntity>,
    color: Color
) {
    if (categories.isEmpty()) return
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        categories.forEach { cat ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cat.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (cat.isDefault) {
                        Text(
                            text = "Defecto",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .background(
                                    color = Primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!cat.isDefault) {
                        IconButton(onClick = { /* delete */ }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TypeChip(text: String, selected: Boolean, onClick: () -> Unit, color: Color) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 16.dp)
            .background(
                color = if (selected) color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = if (selected) color else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            ),
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) color else OnMuted,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}