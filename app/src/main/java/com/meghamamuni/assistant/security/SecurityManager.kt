package com.meghamamuni.assistant.security

import android.content.Context
import android.content.SharedPreferences

/**
 * Security Manager - PIN & Privacy
 * মেঘা মামুনি এর নিরাপত্তা ব্যবস্থাপনা
 */
class SecurityManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)

    fun setPin(pin: String) {
        prefs.edit().putString("pin_code", pin).apply()
    }

    fun checkPin(pin: String): Boolean {
        val stored = prefs.getString("pin_code", "") ?: ""
        return stored == pin
    }

    fun hasPin(): Boolean {
        return prefs.getString("pin_code", "")?.isNotEmpty() == true
    }

    fun setPrivacyMode(enabled: Boolean) {
        prefs.edit().putBoolean("privacy_mode", enabled).apply()
    }

    fun isPrivacyMode(): Boolean {
        return prefs.getBoolean("privacy_mode", false)
    }
}
