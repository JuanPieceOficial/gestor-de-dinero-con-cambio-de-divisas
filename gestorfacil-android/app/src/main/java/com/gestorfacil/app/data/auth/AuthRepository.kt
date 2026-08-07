package com.gestorfacil.app.data.auth

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Autenticación con Supabase, pero la app funciona igual sin sesión
 * (modo local): cuando no hay login, [userId] devuelve [LOCAL_USER_ID]
 * para que los datos se guarden solo en el dispositivo.
 */
class AuthRepository {

    private val _userId = MutableStateFlow(LOCAL_USER_ID)
    val userId: StateFlow<String> = _userId.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _email = MutableStateFlow<String?>(null)
    val email: StateFlow<String?> = _email.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val session = SupabaseProvider.auth.currentSessionOrNull()
                applySession(session?.user?.id, session?.user?.email)
            } catch (_: Exception) {
                // Supabase caído: seguimos en modo local
            } finally {
                _loading.value = false
            }
            try {
                SupabaseProvider.auth.sessionStatus.collect { status ->
                    val authenticated = status as? SessionStatus.Authenticated
                    applySession(authenticated?.session?.user?.id, authenticated?.session?.user?.email)
                }
            } catch (_: Exception) {
                // sin suscripción si Supabase no responde
            }
        }
        // Timeout de seguridad: si Supabase no responde, la app igual carga
        CoroutineScope(Dispatchers.IO).launch {
            delay(2500)
            _loading.value = false
        }
    }

    private fun applySession(id: String?, email: String?) {
        _isLoggedIn.value = id != null
        _userId.value = id ?: LOCAL_USER_ID
        _email.value = email
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
        try {
            SupabaseProvider.auth.signOut()
        } catch (_: Exception) {
        }
        applySession(null, null)
    }

    companion object {
        const val LOCAL_USER_ID = "local"
    }
}
