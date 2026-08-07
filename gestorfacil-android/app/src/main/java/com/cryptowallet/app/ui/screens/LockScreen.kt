package com.cryptowallet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import com.cryptowallet.app.ui.components.ErrorBanner
import com.cryptowallet.app.ui.components.PrimaryButton
import com.cryptowallet.app.ui.theme.Green
import com.cryptowallet.app.ui.theme.NavyCard
import com.cryptowallet.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    verifyPin: suspend (String) -> Boolean,
    lockoutRemaining: suspend () -> Long,
    onUnlocked: () -> Unit,
    onDeleteWallet: () -> Unit,
    biometricAvailable: Boolean = false,
    onBiometricUnlock: () -> Unit = {}
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (pin.length < 4 || checking) return
        checking = true
        error = null
        scope.launch {
            val lockout = lockoutRemaining()
            if (lockout > 0) {
                error = "Demasiados intentos fallidos. Espera ${(lockout / 1000) + 1}s."
                checking = false
                pin = ""
                return@launch
            }
            val ok = verifyPin(pin)
            checking = false
            if (ok) {
                onUnlocked()
            } else {
                error = "PIN incorrecto"
                pin = ""
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Green),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.Black
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("Billetera bloqueada", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Introduce tu PIN para continuar", color = TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter { c -> c.isDigit() }.take(8); error = null },
            label = { Text("PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        if (error != null) {
            ErrorBanner(error ?: "")
            Spacer(Modifier.height(12.dp))
        }
        PrimaryButton(
            text = "Desbloquear",
            onClick = { submit() },
            enabled = pin.length >= 4
        )
        if (biometricAvailable) {
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = NavyCard,
                onClick = onBiometricUnlock
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Usar huella",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = NavyCard,
            onClick = onDeleteWallet
        ) {
            Text(
                "¿Olvidaste tu PIN? Elimina la billetera",
                modifier = Modifier.padding(12.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
