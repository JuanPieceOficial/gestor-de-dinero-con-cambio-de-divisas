package com.gestorfacil.app.data.auth

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthRepository {

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val session = SupabaseProvider.auth.currentSessionOrNull()
                _userId.value = session?.user?.id
            } catch (_: Exception) {
            } finally {
                _loading.value = false
            }
            SupabaseProvider.auth.sessionStatus.collect { status ->
                _userId.value = status.currentOrNull()?.user?.id
            }
        }
    }

    suspend fun signIn(email: String, password: String): String? {
        return try {
            SupabaseProvider.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            null
        } catch (e: Exception) {
            e.message ?: "Error desconocido"
        }
    }

    suspend fun signUp(email: String, password: String): String? {
        return try {
            SupabaseProvider.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            null
        } catch (e: Exception) {
            e.message ?: "Error desconocido"
        }
    }

    suspend fun signOut() {
        SupabaseProvider.auth.signOut()
        _userId.value = null
    }
}
