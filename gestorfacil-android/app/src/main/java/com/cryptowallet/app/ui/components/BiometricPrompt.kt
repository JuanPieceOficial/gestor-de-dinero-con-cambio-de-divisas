package com.cryptowallet.app.ui.components

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Lanza el BiometricPrompt con un CryptoObject (cipher autenticado).
 * @param cipher cipher en modo ENCRYPT/DECRYPT con la clave biométrica
 * @param onSuccess se invoca tras autenticarse; usa el mismo [cipher]
 * @param onCancel se invoca si el usuario cancela o no hay hardware
 */
fun showBiometricPrompt(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    cipher: javax.crypto.Cipher?,
    onSuccess: () -> Unit,
    onCancel: () -> Unit = {}
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // ERROR_NEGATIVE_BUTTON = el usuario pulsó "cancelar"
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED
                ) {
                    onCancel()
                }
            }

            override fun onAuthenticationFailed() {
                // reintentar sin cerrar el prompt
            }
        }
    )
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        .build()
    if (cipher != null) {
        prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    } else {
        prompt.authenticate(promptInfo)
    }
}
