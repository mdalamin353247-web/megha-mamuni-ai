package com.meghamamuni.assistant

import android.content.Context
import android.content.SharedPreferences

/**
 * Chat history - SharedPreferences based (no Room needed)
 * মেঘা মামুনি এর কথোপকথন সংরক্ষণ
 */
class ChatRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("chat_history", Context.MODE_PRIVATE)

    fun saveUserName(name: String) {
        prefs.edit().putString("user_name", name).apply()
    }

    fun getUserName(): String = prefs.getString("user_name", "বন্ধু") ?: "বন্ধু"

    fun clearHistory() {
        prefs.edit().clear().apply()
    }
}
