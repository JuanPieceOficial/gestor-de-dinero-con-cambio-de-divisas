package com.cryptowallet.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cryptowallet.app.data.model.Chain
import com.cryptowallet.app.data.model.TokenInfo
import com.cryptowallet.app.data.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddTokenViewModel(
    application: Application,
    private val repository: WalletRepository
) : AndroidViewModel(application) {

    private val _adding = MutableStateFlow(false)
    val adding: StateFlow<Boolean> = _adding.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun addToken(chain: Chain, contractAddress: String, onDone: (TokenInfo) -> Unit) {
        _adding.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val info = repository.addCustomToken(chain, contractAddress.trim())
                onDone(info)
            } catch (e: Exception) {
                _error.value = e.message ?: "No se pudo agregar el token"
            } finally {
                _adding.value = false
            }
        }
    }
}
