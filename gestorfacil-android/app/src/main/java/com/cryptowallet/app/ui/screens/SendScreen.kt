@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptowallet.app.data.api.Chains
import com.cryptowallet.app.ui.components.CardBox
import com.cryptowallet.app.ui.components.CoinIcon
import com.cryptowallet.app.ui.components.ErrorBanner
import com.cryptowallet.app.ui.components.LabeledText
import com.cryptowallet.app.ui.components.PrimaryButton
import com.cryptowallet.app.ui.components.formatAddress
import com.cryptowallet.app.ui.components.walletViewModel
import com.cryptowallet.app.ui.theme.Green
import com.cryptowallet.app.ui.theme.NavyCard
import com.cryptowallet.app.ui.theme.TextSecondary
import com.cryptowallet.app.ui.viewmodel.SendViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun SendScreen(
    chainId: Long,
    tokenId: String,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val vm = walletViewModel<SendViewModel> { app -> SendViewModel(app, app.walletRepository) }
    val estimate by vm.estimate.collectAsState()
    val sending by vm.sending.collectAsState()
    val result by vm.result.collectAsState()
    val error by vm.error.collectAsState()
    val token by vm.token.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { vm.init(chainId, tokenId) }

    var step by remember { mutableStateOf(0) }
    var recipient by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val chain = Chains.byId(chainId) ?: Chains.all.first()

    fun validate(): Boolean {
        if (!SendViewModel.isValidRecipient(recipient.trim())) {
            localError = "Dirección del destinatario inválida"
            return false
        }
        if (SendViewModel.parseAmount(amountText, token?.decimals ?: 18) == null) {
            localError = "Importe inválido"
            return false
        }
        localError = null
        return true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enviar ${token?.symbol ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                result != null -> {
                    val tx = result!!
                    val explorer = Chains.byId(tx.chainId)?.explorerUrl ?: ""
                    SendResult(
                        hash = tx.hash,
                        explorerUrl = "$explorer/tx/${tx.hash}",
                        onDone = {
                            vm.reset()
                            onDone()
                        }
                    )
                }
                step == 0 -> SendForm(
                    tokenSymbol = token?.symbol ?: "",
                    tokenDecimals = token?.decimals ?: 18,
                    chainName = chain.name,
                    recipient = recipient,
                    onRecipientChange = { recipient = it },
                    amountText = amountText,
                    onAmountChange = { amountText = it },
                    onMax = {
                        scope.launch {
                            val balance = vm.currentBalanceWei()
                            val dec = token?.decimals ?: 18
                            amountText = BigDecimal(balance).movePointLeft(dec).setScale(6, RoundingMode.DOWN).stripTrailingZeros().toPlainString()
                        }
                    },
                    onContinue = {
                        if (validate()) {
                            vm.loadEstimate(recipient.trim(), amountText)
                            step = 1
                        }
                    },
                    error = localError
                )
                else -> SendConfirm(
                    chainName = chain.name,
                    nativeSymbol = chain.nativeSymbol,
                    tokenSymbol = token?.symbol ?: "",
                    recipient = recipient.trim(),
                    amountText = amountText,
                    estimateLoading = estimate.loading,
                    feeNative = estimate.feeNativeWei,
                    estimateError = estimate.error,
                    vm = vm,
                    onSend = {
                        vm.send(recipient.trim(), amountText)
                    },
                    sending = sending,
                    onBack = { step = 0; localError = null }
                )
            }
        }
    }
}

@Composable
private fun SendForm(
    tokenSymbol: String,
    tokenDecimals: Int,
    chainName: String,
    recipient: String,
    onRecipientChange: (String) -> Unit,
    amountText: String,
    onAmountChange: (String) -> Unit,
    onMax: () -> Unit,
    onContinue: () -> Unit,
    error: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        CardBox {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(tokenSymbol, size = 40)
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(tokenSymbol, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(chainName, color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = recipient,
            onValueChange = onRecipientChange,
            label = { Text("Dirección del destinatario (0x...)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = amountText,
            onValueChange = onAmountChange,
            label = { Text("Cantidad") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            suffix = {
                Text(tokenSymbol, color = TextSecondary)
                Spacer(Modifier.size(6.dp))
                OutlinedButton(onClick = onMax) {
                    Text("MAX", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
        Spacer(Modifier.height(20.dp))
        if (error != null) {
            ErrorBanner(error)
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(8.dp))
        PrimaryButton(
            text = "Continuar",
            enabled = recipient.isNotBlank() && amountText.isNotBlank(),
            onClick = onContinue
        )
    }
}

@Composable
private fun SendConfirm(
    chainName: String,
    nativeSymbol: String,
    tokenSymbol: String,
    recipient: String,
    amountText: String,
    estimateLoading: Boolean,
    feeNative: java.math.BigInteger,
    estimateError: String?,
    vm: SendViewModel,
    sending: Boolean,
    onSend: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        CardBox {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                LabeledText("Enviar", "$amountText $tokenSymbol")
                LabeledText("Destinatario", formatAddress(recipient, 10, 8))
                LabeledText("Red", chainName)
                if (estimateLoading) {
                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Green, strokeWidth = 2.5.dp)
                } else if (estimateError != null) {
                    ErrorBanner(estimateError)
                } else {
                    val feeStr = vm.feeInNative(feeNative)
                    LabeledText("Comisión estimada", "$feeStr $nativeSymbol")
                    LabeledText("Total", "${vm.totalInNative(amountText, feeNative)} $nativeSymbol")
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onBack, enabled = !sending, modifier = Modifier.fillMaxWidth()) {
            Text("Volver")
        }
        Spacer(Modifier.height(10.dp))
        PrimaryButton(
            text = if (sending) "Enviando..." else "Confirmar y enviar",
            onClick = onSend,
            enabled = estimateError == null && !estimateLoading,
            loading = sending
        )
    }
}

@Composable
private fun SendResult(
    hash: String,
    explorerUrl: String,
    onDone: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Green,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("Transacción enviada", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tu transacción está en la red y pendiente de confirmación.",
            color = TextSecondary,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(20.dp))
        Text(
            formatAddress(hash, 12, 10),
            color = TextSecondary,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse(explorerUrl)
            }
            context.startActivity(intent)
        }) {
            Text("Ver en el explorador")
        }
        Spacer(Modifier.height(10.dp))
        PrimaryButton(text = "Listo", onClick = onDone)
    }
}
