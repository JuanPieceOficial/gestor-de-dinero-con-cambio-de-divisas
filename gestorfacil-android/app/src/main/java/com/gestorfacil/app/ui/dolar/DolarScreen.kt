package com.gestorfacil.app.ui.dolar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestorfacil.app.GestorFacilApp
import com.gestorfacil.app.data.api.DolarApi
import com.gestorfacil.app.data.model.BolivarRate
import com.gestorfacil.app.ui.theme.Accent
import com.gestorfacil.app.ui.theme.OnMuted
import com.gestorfacil.app.ui.theme.Primary
import com.gestorfacil.app.ui.theme.Surface
import com.gestorfacil.app.ui.theme.SurfaceVariant
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private val UsdtColor = Color(0xFFF59E0B)
private val PromedioColor = Color(0xFF8B5CF6)
private val EuroColor = Color(0xFF06B6D4)

private data class RateInfo(
    val label: String,
    val value: Double,
    val color: Color
)

@Composable
fun DolarScreen() {
    val api = remember { DolarApi() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val app = LocalContext.current.applicationContext as GestorFacilApp

    var rate by remember { mutableStateOf(app.settingsManager.cachedRate) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var usdText by remember { mutableStateOf("") }
    var vesText by remember { mutableStateOf("") }
    var selectedIdx by remember { mutableIntStateOf(1) }

    LaunchedEffect(Unit) {
        loading = true
        error = false
        try {
            val r = api.fetchRates()
            rate = r
            app.settingsManager.cachedRate = r
        } catch (_: Exception) {
            if (rate == null) error = true
        }
        loading = false
    }

    val nf = remember { NumberFormat.getNumberInstance(Locale("es", "VE")) }
    nf.minimumFractionDigits = 2
    nf.maximumFractionDigits = 2

    val rateList = remember(rate) {
        val list = mutableListOf<RateInfo>()
        rate?.let { r ->
            list.add(RateInfo("Dólar BCV", r.oficial, Primary))
            r.binance?.let { b ->
                list.add(RateInfo("Dólar USDT", b.promedio, UsdtColor))
            }
            list.add(RateInfo("Dólar Promedio", r.paralelo, PromedioColor))
            if (r.euro > 0) list.add(RateInfo("Euro", r.euro, EuroColor))
        }
        if (list.isEmpty()) list.add(RateInfo("Dólar BCV", 0.0, Primary))
        list
    }
    if (selectedIdx >= rateList.size) selectedIdx = 0

    fun currentRate() = rateList.getOrNull(selectedIdx)?.value ?: 0.0

    fun updateFromUsd() {
        val u = usdText.toDoubleOrNull() ?: return
        val r = currentRate()
        if (r > 0) vesText = nf.format(u * r)
    }
    fun updateFromVes() {
        val v = vesText.toDoubleOrNull() ?: return
        val r = currentRate()
        if (r > 0) usdText = nf.format(v / r)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dólar Venezuela",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    loading = true
                    error = false
                    scope.launch {
                        try {
                            val r = api.fetchRates()
                            rate = r
                            app.settingsManager.cachedRate = r
                        } catch (_: Exception) { error = true }
                        loading = false
                    }
                },
                enabled = !loading
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Actualizar",
                        tint = Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Error
        if (error) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Error de conexión. Verifica tu internet.",
                    modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Rates card
        rate?.let { r ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tasas del Día",
                        style = MaterialTheme.typography.titleSmall,
                        color = OnMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    RateRow("Dólar BCV", r.oficial, Primary, nf)
                    Spacer(Modifier.height(10.dp))
                    r.binance?.let { b ->
                        RateRow("Dólar USDT", b.promedio, UsdtColor, nf)
                        Spacer(Modifier.height(10.dp))
                    }
                    RateRow("Dólar Promedio", r.paralelo, PromedioColor, nf)
                    if (r.euro > 0) {
                        Spacer(Modifier.height(10.dp))
                        RateRow("Euro", r.euro, EuroColor, nf)
                    }
                    r.binance?.let { b ->
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = OnMuted.copy(alpha = 0.15f))
                        Spacer(Modifier.height(10.dp))
                        val diff = b.promedio - r.oficial
                        val percent = if (r.oficial > 0) (diff / r.oficial * 100) else 0.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Brecha BCV ↔ USDT",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnMuted
                            )
                            Text(
                                text = "Bs. ${nf.format(diff)} (${nf.format(percent)}%)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (diff > 0) Color(0xFF22C55E) else Color(0xFFEF4444)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Fuente: Al Cambio · ${r.lastUpdated.take(10)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnMuted.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Empty state
        if (rate == null && !loading && !error) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        Icons.Default.CurrencyExchange,
                        contentDescription = null,
                        tint = Primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Presioná el icono de refrescar arriba para ver las tasas de hoy",
                        color = OnMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Converter
        if (rate != null) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Calculadora",
                        style = MaterialTheme.typography.titleSmall,
                        color = OnMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(14.dp))

                    RateSelector(
                        options = rateList,
                        selectedIndex = selectedIdx,
                        onSelect = { selectedIdx = it }
                    )

                    Spacer(Modifier.height(16.dp))

                    val selectedColor = rateList.getOrNull(selectedIdx)?.color ?: Accent
                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = selectedColor,
                        cursorColor = selectedColor,
                        focusedLabelColor = selectedColor,
                    )

                    OutlinedTextField(
                        value = usdText,
                        onValueChange = { usdText = it; updateFromUsd() },
                        label = { Text("USD") },
                        placeholder = { Text("0.00") },
                        prefix = { Text("$ ", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = vesText,
                        onValueChange = { vesText = it; updateFromVes() },
                        label = { Text("VES") },
                        placeholder = { Text("0.00") },
                        prefix = { Text("Bs. ", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        singleLine = true
                    )
                }
            }
        }
    }
}

@Composable
private fun RateRow(label: String, value: Double, color: Color, nf: NumberFormat) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Bs. ${nf.format(value)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun RateSelector(
    options: List<RateInfo>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariant),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        options.forEachIndexed { i, opt ->
            val bg by animateColorAsState(
                targetValue = if (i == selectedIndex) opt.color else Color.Transparent,
                animationSpec = tween(200),
                label = "bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (i == selectedIndex) Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label = "tc"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = opt.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        maxLines = 1
                    )
                    Text(
                        text = "Bs. ${NumberFormat.getNumberInstance(Locale("es", "VE")).apply {
                            minimumFractionDigits = 2; maximumFractionDigits = 2
                        }.format(opt.value)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor.copy(alpha = 0.85f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
