package com.gestorfacil.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gestorfacil.app.data.database.TransactionEntity
import com.gestorfacil.app.data.settings.SettingsManager
import com.gestorfacil.app.ui.theme.Primary
import com.gestorfacil.app.ui.theme.Surface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionSheetDialog(
    settingsManager: SettingsManager,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit,
    editTransaction: TransactionEntity? = null
) {
    val isEditing = editTransaction != null
    var type by remember { mutableStateOf(editTransaction?.type ?: "expense") }
    var description by remember { mutableStateOf(editTransaction?.description ?: "") }
    var amount by remember {
        mutableStateOf(editTransaction?.let { abs(it.amount).toString() } ?: "")
    }
    var category by remember { mutableStateOf(editTransaction?.category ?: "") }
    var date by remember {
        mutableStateOf(editTransaction?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val categories = listOf(
        "Alimentación", "Transporte", "Ocio",
        "Hogar", "Salud", "Educación", "Otros"
    )

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        date = sdf.format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize().clickable { onDismiss() }
        )

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                // Pull handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .padding(bottom = 4.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {}
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = if (isEditing) "Editar Transacción" else "Nueva Transacción",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(20.dp))

                // Type toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TypeButton(
                        text = "Gasto",
                        selected = type == "expense",
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f),
                        onClick = { type = "expense" }
                    )
                    TypeButton(
                        text = "Ingreso",
                        selected = type == "income",
                        color = Color(0xFF29996E),
                        modifier = Modifier.weight(1f),
                        onClick = { type = "income" }
                    )
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    placeholder = { Text("Ej: Compra supermercado") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monto") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    label = { Text("Fecha") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Seleccionar fecha",
                            modifier = Modifier.clickable { showDatePicker = true }
                        )
                    }
                )

                Spacer(Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        label = { Text("Categoría") },
                        placeholder = { Text("Selecciona categoría") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                        shape = RoundedCornerShape(14.dp),
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                        val entity = if (isEditing) {
                            editTransaction.copy(
                                date = date,
                                description = description,
                                amount = if (type == "expense") -parsedAmount else parsedAmount,
                                category = category,
                                type = type
                            )
                        } else {
                            TransactionEntity(
                                date = date,
                                description = description,
                                amount = if (type == "expense") -parsedAmount else parsedAmount,
                                category = category,
                                type = type
                            )
                        }
                        onSave(entity)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    enabled = description.isNotBlank() && amount.isNotBlank() && category.isNotBlank()
                ) {
                    Text(
                        text = if (isEditing) "Guardar cambios" else "Guardar",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TypeButton(
    text: String,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) color.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
