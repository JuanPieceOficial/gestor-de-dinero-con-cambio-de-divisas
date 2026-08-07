package com.cryptowallet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.selection.SelectionContainer
import com.cryptowallet.app.data.api.Chains
import com.cryptowallet.app.data.model.AccountInfo
import com.cryptowallet.app.data.model.Chain
import com.cryptowallet.app.ui.components.SecondaryButton
import com.cryptowallet.app.ui.components.formatAddress
import com.cryptowallet.app.ui.theme.NavyCard
import com.cryptowallet.app.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    account: AccountInfo?,
    accounts: List<AccountInfo> = emptyList(),
    chains: List<Chain>,
    testnetEnabled: Boolean,
    fiatCurrency: String,
    biometricEnabled: Boolean = false,
    biometricAvailable: Boolean = false,
    onToggleBiometric: (Boolean) -> Unit = {},
    onAddAccount: () -> Unit = {},
    onSwitchAccount: (Int) -> Unit = {},
    onRenameAccount: (Int, String) -> Unit = {_,_ ->},
    onToggleTestnet: (Boolean) -> Unit,
    onSelectCurrency: (String) -> Unit,
    onCopyAddress: (String) -> Unit,
    onChangePin: () -> Unit,
    onBackupPhrase: () -> Unit,
    onLock: () -> Unit,
    onDeleteWallet: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var currencyMenu by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<AccountInfo?>(null) }
    var renameText by remember { mutableStateOf("") }
    val currencyOptions = listOf("USD", "MXN", "EUR", "GBP")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            AccountCard(account = account, onCopy = { account?.let { onCopyAddress(it.address) } })
        }

        if (accounts.isNotEmpty()) {
            item { SectionTitle("Cuentas") }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NavyCard)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    accounts.forEach { acc ->
                        val isActive = acc.index == (account?.index ?: 0)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSwitchAccount(acc.index) }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = if (isActive) MaterialTheme.colorScheme.primary else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.size(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    acc.name,
                                    fontSize = 14.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isActive) MaterialTheme.colorScheme.primary else Color.White
                                )
                                Text(formatAddress(acc.address), fontSize = 11.sp, color = TextSecondary)
                            }
                            if (isActive) {
                                Icon(Icons.Default.Check, contentDescription = "Activa", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.size(4.dp))
                            androidx.compose.material3.IconButton(onClick = {
                                renameTarget = acc
                                renameText = acc.name
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Renombrar", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onAddAccount() }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Añadir cuenta", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item { SectionTitle("Seguridad") }
        item {
            if (biometricAvailable) {
                SwitchSettingRow(
                    icon = Icons.Default.Fingerprint,
                    title = "Desbloqueo con huella",
                    subtitle = "Abre la billetera con tu huella",
                    checked = biometricEnabled,
                    onCheckedChange = onToggleBiometric
                )
            }
        }
        item {
            SettingRow(Icons.Default.Password, "Cambiar PIN", "Actualiza tu PIN de acceso") { onChangePin() }
        }
        item {
            SettingRow(Icons.Default.Security, "Frase de respaldo", "Muestra tus 24 palabras") { onBackupPhrase() }
        }
        item {
            SettingRow(Icons.Default.Lock, "Bloquear ahora", "Borra la clave de la memoria") { onLock() }
        }

        item { SectionTitle("Preferencias") }
        item {
            SwitchSettingRow(
                icon = Icons.Default.Science,
                title = "Redes de prueba (testnet)",
                subtitle = "Sepolia, BSC testnet y Polygon Amoy. Sin valor real.",
                checked = testnetEnabled,
                onCheckedChange = onToggleTestnet
            )
        }
        item {
            Box(Modifier.fillMaxWidth()) {
                SettingRow(
                    Icons.Default.Payments,
                    "Moneda de saldos",
                    "Mostrar valores en $fiatCurrency"
                ) { currencyMenu = true }
                DropdownMenu(expanded = currencyMenu, onDismissRequest = { currencyMenu = false }) {
                    currencyOptions.forEach { code ->
                        DropdownMenuItem(
                            text = { Text(code) },
                            onClick = {
                                onSelectCurrency(code)
                                currencyMenu = false
                            }
                        )
                    }
                }
            }
        }

        item { SectionTitle("Redes compatibles") }
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NavyCard)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chains.forEach { chain ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(chain.name, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(chain.nativeSymbol, color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        item { SectionTitle("Información") }
        item {
            SettingRow(Icons.Default.Info, "Acerca de", "Versión 1.0.0 · No custodiada") { showAbout = true }
        }

        item {
            Spacer(Modifier.height(8.dp))
            SecondaryButton(text = "Eliminar billetera", onClick = { showDeleteDialog = true })
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar la billetera?") },
            text = {
                Text(
                    "Se borrarán las llaves y el historial de este dispositivo. " +
                        "Si no tienes tu frase de respaldo, perderás los fondos para siempre."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteWallet()
                    }
                ) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("CryptoWallet") },
            text = {
                Text(
                    "Billetera no custodiada para Android.\n\n" +
                        "Soporta Ethereum, BNB Smart Chain, Polygon, Arbitrum, Optimism, Base y Avalanche " +
                        "con miles de tokens ERC-20.\n\n" +
                        "Las llaves privadas se generan y cifran localmente con el Android Keystore y nunca " +
                        "abandonan tu dispositivo."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("Cerrar") }
            }
        )
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Renombrar cuenta") },
            text = {
                androidx.compose.foundation.text.BasicTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NavyCard)
                        .padding(12.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameAccount(target.index, renameText)
                        renameTarget = null
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun AccountCard(account: AccountInfo?, onCopy: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NavyCard)
            .padding(16.dp)
    ) {
        Text("Mi dirección", fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(
                    account?.address ?: "",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.size(8.dp))
            androidx.compose.material3.IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = TextSecondary)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(account?.name ?: "", fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun SwitchSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NavyCard)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NavyCard)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
