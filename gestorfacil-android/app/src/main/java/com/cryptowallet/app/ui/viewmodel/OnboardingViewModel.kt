package com.cryptowallet.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cryptowallet.app.data.crypto.Bip39
import com.cryptowallet.app.data.model.AccountInfo
import com.cryptowallet.app.data.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    application: Application,
    private val repository: WalletRepository
) : AndroidViewModel(application) {

    private val _mnemonic = MutableStateFlow<String?>(null)
    val mnemonic: StateFlow<String?> = _mnemonic

    private val _creating = MutableStateFlow(false)
    val creating: StateFlow<Boolean> = _creating

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _restorePhrase = MutableStateFlow<String?>(null)
    val restorePhrase: StateFlow<String?> = _restorePhrase

    fun setRestorePhrase(phrase: String) {
        _restorePhrase.value = phrase
    }

    fun generateMnemonic() {
        _error.value = null
        viewModelScope.launch {
            _creating.value = true
            try {
                _mnemonic.value = Bip39.generateMnemonic(getApplication(), entropyBytes = 32)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _creating.value = false
            }
        }
    }

    fun validateMnemonic(phrase: String): Boolean {
        return try {
            Bip39.validateMnemonic(getApplication(), phrase)
        } catch (e: Exception) {
            false
        }
    }

    fun createWallet(pin: String, onDone: (AccountInfo) -> Unit) {
        val phrase = mnemonic.value ?: return
        viewModelScope.launch {
            _creating.value = true
            _error.value = null
            try {
                val account = repository.createWallet(phrase, pin)
                onDone(account)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _creating.value = false
            }
        }
    }

    fun restoreWallet(phrase: String, pin: String, onDone: (AccountInfo) -> Unit) {
        viewModelScope.launch {
            _creating.value = true
            _error.value = null
            try {
                val account = repository.restoreWallet(phrase, pin)
                onDone(account)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _creating.value = false
            }
        }
    }
}
