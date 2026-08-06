package com.cryptowallet.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptowallet.app.ui.components.CardBox
import com.cryptowallet.app.ui.components.PrimaryButton
import com.cryptowallet.app.ui.components.SecondaryButton
import com.cryptowallet.app.ui.theme.Green
import com.cryptowallet.app.ui.theme.TextSecondary

@Composable
fun WelcomeScreen(
    onCreate: () -> Unit,
    onRestore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Green),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "CryptoWallet",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tu billetera no custodiada de criptomonedas.\nSolo tú controlas tus llaves privadas.",
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            color = TextSecondary
        )
        Spacer(Modifier.height(32.dp))
        CardBox {
            Column(Modifier.padding(4.dp)) {
                FeatureRow("Llaves 100% locales", "El seed se cifra con tu PIN y el Keystore de Android; nunca sale del dispositivo.")
                Spacer(Modifier.height(14.dp))
                FeatureRow("Multi-cadena", "Ethereum, BNB Smart Chain, Polygon, Arbitrum, Optimism, Base y Avalanche con miles de tokens.")
                Spacer(Modifier.height(14.dp))
                FeatureRow("Con respaldo BIP-39", "Frases de recuperación estándar de 24 palabras compatibles con cualquier wallet.")
            }
        }
        Spacer(Modifier.height(32.dp))
        PrimaryButton(text = "Crear nueva billetera", onClick = onCreate)
        Spacer(Modifier.height(12.dp))
        SecondaryButton(text = "Restaurar billetera existente", onClick = onRestore)
    }
}

@Composable
private fun FeatureRow(title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Green
        )
        Spacer(Modifier.size(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(2.dp))
            Text(description, fontSize = 12.sp, color = TextSecondary)
        }
    }
}
