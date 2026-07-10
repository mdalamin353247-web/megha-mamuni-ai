package com.meghamamuni.assistant.theme

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

/**
 * মেঘা মামুনি Theme Manager
 * Dark Mode: #0D1117 bg, #58A6FF accent, #F0F6FF text
 * Light Mode: #F8F9FF bg, #1565C0 accent, #1A1A2E text
 */
object ThemeManager {

    fun applyTheme(isDark: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun isDarkMode(context: Context): Boolean {
        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return uiMode == Configuration.UI_MODE_NIGHT_YES
    }

    // Color tokens
    object Dark {
        const val BACKGROUND = "#0D1117"
        const val SURFACE = "#161B22"
        const val ACCENT = "#58A6FF"
        const val TEXT_PRIMARY = "#F0F6FF"
        const val TEXT_SECONDARY = "#8B949E"
        const val BUBBLE_AI = "#21262D"
        const val BUBBLE_USER = "#1F6FEB"
        const val PINK = "#FF79C6"
    }

    object Light {
        const val BACKGROUND = "#F8F9FF"
        const val SURFACE = "#FFFFFF"
        const val ACCENT = "#1565C0"
        const val TEXT_PRIMARY = "#1A1A2E"
        const val TEXT_SECONDARY = "#666680"
        const val BUBBLE_AI = "#FFFFFF"
        const val BUBBLE_USER = "#1565C0"
        const val PINK = "#E91E63"
    }
}
