package com.cryptowallet.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Green,
    onPrimary = Color.Black,
    primaryContainer = NavyLight,
    onPrimaryContainer = TextPrimary,
    secondary = GreenDark,
    onSecondary = Color.Black,
    secondaryContainer = NavyCard,
    onSecondaryContainer = TextPrimary,
    tertiary = Blue,
    onTertiary = Color.Black,
    background = Navy,
    onBackground = TextPrimary,
    surface = NavyLight,
    onSurface = TextPrimary,
    surfaceVariant = NavyCard,
    onSurfaceVariant = TextSecondary,
    error = Error,
    onError = Color.Black,
    outline = Border,
    outlineVariant = Border
)

@Composable
fun CryptoWalletTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        content = content
    )
}
