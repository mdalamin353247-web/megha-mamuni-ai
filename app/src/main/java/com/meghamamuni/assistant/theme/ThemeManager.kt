package com.meghamamuni.assistant.theme

import android.content.Context

/**
 * Theme Manager - Dark/Light mode
 */
class ThemeManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("megha_prefs", Context.MODE_PRIVATE)

    fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", false)

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }
}
