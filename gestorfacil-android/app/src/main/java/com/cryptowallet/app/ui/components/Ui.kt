package com.cryptowallet.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptowallet.app.ui.theme.Green
import com.cryptowallet.app.ui.theme.NavyCard
import com.cryptowallet.app.ui.theme.TextSecondary
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Green,
            contentColor = Color.Black,
            disabledContainerColor = NavyCard,
            disabledContentColor = TextSecondary
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.Black,
                strokeWidth = 2.5.dp
            )
        } else {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(text, fontSize = 15.sp)
    }
}

@Composable
fun CardBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NavyCard
    ) {
        Box(Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun CoinIcon(symbol: String, size: Int = 44, colors: List<Color> = coinPalette) {
    val color = colorFor(symbol, colors)
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol.take(1).uppercase(),
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.42).sp
        )
    }
}

val coinPalette: List<Color> = listOf(
    Color(0xFF00D68F), Color(0xFF4F8DF7), Color(0xFF9B5CF4),
    Color(0xFFF7931A), Color(0xFFE0569B), Color(0xFF35C2E3),
    Color(0xFFF5A623), Color(0xFF5C6BC0)
)

fun colorFor(symbol: String, colors: List<Color> = coinPalette): Color {
    val sum = symbol.fold(0) { acc, c -> acc + c.code }
    return colors[Math.floorMod(sum, colors.size)]
}

@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.error,
            fontSize = 13.sp
        )
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = Green,
            strokeWidth = 3.dp
        )
    }
}

@Composable
fun LabeledText(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = valueColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

fun formatFiat(value: Double, currencyCode: String = "USD"): String {
    val symbol = currencySymbol(currencyCode)
    if (value >= 1_000_000_000) return symbol + formatDecimal(value / 1_000_000_000) + "B"
    if (value >= 1_000_000) return symbol + formatDecimal(value / 1_000_000) + "M"
    if (value >= 10_000) return symbol + formatDecimal(value / 1_000) + "K"
    if (value >= 0.01) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag(localeTag(currencyCode))).format(value)
    }
    if (value > 0.0) {
        return symbol + "%.6f".format(value).trimEnd('0').trimEnd('.')
    }
    return symbol + "0.00"
}

fun currencySymbol(code: String): String = when (code.uppercase()) {
    "MXN", "USD", "CAD", "AUD" -> "$"
    "EUR" -> "€"
    "GBP" -> "£"
    "JPY" -> "¥"
    else -> "$"
}

private fun localeTag(code: String): String = when (code.uppercase()) {
    "MXN" -> "es-MX"
    "EUR" -> "de-DE"
    "GBP" -> "en-GB"
    "JPY" -> "ja-JP"
    else -> "en-US"
}

fun formatDecimal(value: Double): String {
    return DecimalFormat("#.##").format(value)
}

fun formatCryptoAmount(balance: BigDecimal, symbol: String): String {
    if (balance.compareTo(BigDecimal.ZERO) == 0) return "0 $symbol"
    val magnitude = if (balance.signum() == 0) 0 else balance.abs().precision() - balance.abs().scale()
    val decimals = when {
        balance.abs() >= BigDecimal.ONE -> minOf(6, maxOf(2, magnitude + 2))
        else -> 8
    }
    val rounded = balance.setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros()
    return "${rounded.toPlainString()} $symbol"
}

fun formatAddress(address: String, start: Int = 6, end: Int = 6): String {
    if (address.length <= start + end + 3) return address
    return "${address.take(start)}...${address.takeLast(end)}"
}

fun formatTxTime(timestamp: Long): String {    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60_000
    return when {
        minutes < 1 -> "Ahora"
        minutes < 60 -> "hace $minutes min"
        minutes < 1440 -> "hace ${minutes / 60} h"
        else -> {
            val fmt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
            fmt.format(java.util.Date(timestamp))
        }
    }
}

fun copyToClipboard(clipboardManager: ClipboardManager, text: String) {
    clipboardManager.setText(AnnotatedString(text))
}
