package com.meghamamuni.assistant.security

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * মেঘা মামুনি Security Manager
 * PIN + Fingerprint সুরক্ষা
 */
class SecurityManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("megha_security", Context.MODE_PRIVATE)

    val isPinEnabled: Boolean get() = prefs.getBoolean("pin_enabled", false)
    val isFingerprintEnabled: Boolean get() = prefs.getBoolean("fingerprint_enabled", false)
    val isPrivacyMode: Boolean get() = prefs.getBoolean("privacy_mode", false)

    fun setupPin(pin: String) {
        prefs.edit()
            .putString("pin_hash", pin.hashCode().toString())
            .putBoolean("pin_enabled", true)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString("pin_hash", "") ?: ""
        return pin.hashCode().toString() == stored
    }

    fun disablePin() {
        prefs.edit()
            .putBoolean("pin_enabled", false)
            .remove("pin_hash")
            .apply()
    }

    fun isFingerprintAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showFingerprintPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
                override fun onAuthenticationFailed() {
                    onError("আঙুলের ছাপ মেলেনি। আবার চেষ্টা করুন।")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("মেঘা মামুনি লক")
            .setSubtitle("আপনার আঙুলের ছাপ দিয়ে আনলক করুন")
            .setNegativeButtonText("PIN ব্যবহার করুন")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    fun setPrivacyMode(enabled: Boolean) {
        prefs.edit().putBoolean("privacy_mode", enabled).apply()
    }

    fun setFingerprintEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("fingerprint_enabled", enabled).apply()
    }
}
