package com.cryptowallet.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cryptowallet.app.data.api.Chains
import com.cryptowallet.app.data.db.TxRecordEntity
import com.cryptowallet.app.data.model.AccountInfo
import com.cryptowallet.app.data.model.Chain
import com.cryptowallet.app.data.model.TokenBalance
import com.cryptowallet.app.data.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChainGroup(
    val chain: Chain,
    val items: List<TokenBalance>,
    val chainFiat: Double
)

data class PortfolioUiState(
    val totalFiat: Double = 0.0,
    val byChain: List<ChainGroup> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    val hasLoadedOnce: Boolean = false,
    val fiatCurrency: String = "USD"
)

class WalletViewModel(
    application: Application,
    private val repository: WalletRepository
) : AndroidViewModel(application) {

    private val _portfolio = MutableStateFlow(PortfolioUiState())
    val portfolio: StateFlow<PortfolioUiState> = _portfolio.asStateFlow()

    private val _account = MutableStateFlow<AccountInfo?>(null)
    val account: StateFlow<AccountInfo?> = _account.asStateFlow()

    private val _history = MutableStateFlow<List<TxRecordEntity>>(emptyList())
    val history: StateFlow<List<TxRecordEntity>> = _history.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _testnetEnabled = MutableStateFlow(false)
    val testnetEnabled: StateFlow<Boolean> = _testnetEnabled.asStateFlow()

    private val _fiatCurrency = MutableStateFlow("USD")
    val fiatCurrency: StateFlow<String> = _fiatCurrency.asStateFlow()

    val chains: List<Chain> get() = Chains.all

    suspend fun hasWallet(): Boolean = repository.hasWallet()

    suspend fun unlock(pin: String): Boolean = repository.unlock(pin)

    fun lock() {
        repository.lock()
        _account.value = null
    }

    fun loadAccount() {
        viewModelScope.launch {
            _account.value = repository.getActiveAccount()
        }
    }

    fun loadPreferences() {
        viewModelScope.launch {
            _testnetEnabled.value = repository.isTestnetEnabled()
            _fiatCurrency.value = repository.getFiatCurrency()
        }
    }

    fun setTestnetEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setTestnetEnabled(enabled)
            _testnetEnabled.value = enabled
            refresh(showSpinner = true)
        }
    }

    fun setFiatCurrency(code: String) {
        viewModelScope.launch {
            repository.setFiatCurrency(code)
            _fiatCurrency.value = code
            refresh()
        }
    }

    fun refresh(showSpinner: Boolean = false) {
        _portfolio.value = _portfolio.value.copy(loading = showSpinner, error = null)
        viewModelScope.launch {
            try {
                repository.refreshPendingTx()
                val result = repository.loadPortfolio()
                _account.value = repository.getActiveAccount()
                val byChain = result.balances
                    .groupBy { it.token.chainId }
                    .map { (chainId, items) ->
                        val chain = Chains.byId(chainId)
                        ChainGroup(chain ?: Chains.all.first(), items, items.sumOf { it.fiatValue })
                    }
                _portfolio.value = PortfolioUiState(
                    totalFiat = result.totalFiat,
                    byChain = byChain,
                    loading = false,
                    refreshing = false,
                    error = null,
                    hasLoadedOnce = true,
                    fiatCurrency = repository.getFiatCurrency()
                )
                _history.value = repository.getTxHistory()
            } catch (e: Exception) {
                _portfolio.value = _portfolio.value.copy(
                    loading = false,
                    refreshing = false,
                    error = e.message ?: "Error de red"
                )
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            try {
                repository.refreshPendingTx()
                _history.value = repository.getTxHistory()
            } catch (e: Exception) {
                // silencioso: el historial es secundario
            }
        }
    }

    suspend fun addCustomToken(chain: Chain, contractAddress: String) {
        repository.addCustomToken(chain, contractAddress)
    }

    fun removeToken(tokenId: String) {
        viewModelScope.launch {
            val token = repository.getEnabledTokens().firstOrNull { it.id == tokenId } ?: return@launch
            repository.removeToken(token)
            refresh()
        }
    }

    fun changePin(oldPin: String, newPin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(repository.changePin(oldPin, newPin))
        }
    }

    fun getBackupMnemonic(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            onResult(repository.getBackupMnemonic())
        }
    }

    fun deleteWallet(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteWallet()
            onDone()
        }
    }

    fun showMessage(message: String) {
        _actionMessage.value = message
    }

    fun clearMessage() {
        _actionMessage.value = null
    }
}
