package com.cryptowallet.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cryptowallet.app.data.api.Chains
import com.cryptowallet.app.data.db.TokenEntity
import com.cryptowallet.app.data.model.Chain
import com.cryptowallet.app.data.model.GasFees
import com.cryptowallet.app.data.model.PendingTx
import com.cryptowallet.app.data.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

data class SendEstimateState(
    val fees: GasFees? = null,
    val gasLimit: Long = 0,
    val feeNativeWei: BigInteger = BigInteger.ZERO,
    val loading: Boolean = false,
    val error: String? = null
)

class SendViewModel(
    application: Application,
    private val repository: WalletRepository
) : AndroidViewModel(application) {

    var chain: Chain = Chains.all.first()
        private set

    private val _token = MutableStateFlow<TokenEntity?>(null)
    val token: StateFlow<TokenEntity?> = _token.asStateFlow()

    private val _estimate = MutableStateFlow(SendEstimateState())
    val estimate: StateFlow<SendEstimateState> = _estimate.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    private val _result = MutableStateFlow<PendingTx?>(null)
    val result: StateFlow<PendingTx?> = _result.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun init(chainId: Long, tokenId: String) {
        chain = Chains.byId(chainId) ?: Chains.all.first()
        viewModelScope.launch {
            _token.value = repository.getEnabledTokens().firstOrNull { it.id == tokenId }
        }
    }

    fun loadEstimate(recipient: String, amountText: String) {
        val token = _token.value ?: return
        val amountWei = parseAmount(amountText, token.decimals) ?: run {
            _estimate.value = SendEstimateState(error = "Importe inválido")
            return
        }
        if (!isValidRecipient(recipient)) {
            _estimate.value = SendEstimateState(error = "Dirección inválida")
            return
        }
        _estimate.value = SendEstimateState(loading = true, error = null)
        viewModelScope.launch {
            try {
                val estimate = repository.estimateSend(chain, token, recipient, amountWei)
                val feeWei = if (estimate.fees.supportsEip1559 && estimate.fees.maxFeePerGas != null) {
                    estimate.fees.maxFeePerGas.multiply(BigInteger.valueOf(estimate.gasLimit))
                } else if (estimate.fees.gasPrice != null) {
                    estimate.fees.gasPrice.multiply(BigInteger.valueOf(estimate.gasLimit))
                } else {
                    BigInteger.ZERO
                }
                _estimate.value = SendEstimateState(
                    fees = estimate.fees,
                    gasLimit = estimate.gasLimit,
                    feeNativeWei = feeWei,
                    loading = false,
                    error = null
                )
            } catch (e: Exception) {
                _estimate.value = SendEstimateState(
                    loading = false,
                    error = e.message ?: "Error estimando la transacción"
                )
            }
        }
    }

    fun send(recipient: String, amountText: String) {
        val token = _token.value ?: return
        val amountWei = parseAmount(amountText, token.decimals) ?: return
        val fees = _estimate.value.fees ?: return
        val gasLimit = _estimate.value.gasLimit
        _sending.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val tx = repository.send(chain, token, recipient, amountWei, gasLimit, fees)
                _result.value = tx
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al enviar la transacción"
            } finally {
                _sending.value = false
            }
        }
    }

    fun reset() {
        _result.value = null
        _error.value = null
        _estimate.value = SendEstimateState()
        _sending.value = false
    }

    fun chainNativeSymbol(): String = chain.nativeSymbol

    suspend fun currentBalanceWei(): BigInteger {
        val token = _token.value ?: return BigInteger.ZERO
        val address = repository.getActiveAddress()
        return repository.getTokenBalanceWei(chain, token, address)
    }

    fun feeInNative(feeWei: BigInteger): String {
        return BigDecimal(feeWei)
            .movePointLeft(chain.nativeDecimals)
            .setScale(6, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }

    fun totalInNative(amountText: String, feeWei: BigInteger): String {
        val amountWei = parseAmount(amountText, _token.value?.decimals ?: 18) ?: BigInteger.ZERO
        val isNative = _token.value?.isNative == true
        val total = if (isNative) amountWei.add(feeWei) else feeWei
        return BigDecimal(total)
            .movePointLeft(chain.nativeDecimals)
            .setScale(6, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }

    companion object {
        fun parseAmount(text: String, decimals: Int): BigInteger? {
            val normalized = text.trim().replace(",", ".")
            if (normalized.isEmpty() || !normalized.matches(Regex("^\\d+(\\.\\d+)?$"))) return null
            val decimal = BigDecimal(normalized)
            if (decimal.signum() < 0) return null
            return decimal.movePointRight(decimals).toBigInteger()
        }

        fun isValidRecipient(address: String): Boolean {
            return com.cryptowallet.app.data.crypto.EthCrypto.isValidAddress(address)
        }
    }
}
