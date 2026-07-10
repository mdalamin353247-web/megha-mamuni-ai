package com.meghamamuni.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * মেঘা মামুনি AI Engine
 * OpenAI GPT-3.5-turbo দিয়ে বাংলা + ইংরেজি কথোপকথন
 * Optimized for low-end devices with response caching
 */
class AIEngine {

    companion object {
        private const val API_KEY = "YOUR_OPENAI_API_KEY" // 🔑 এখানে তোমার API key দাও
        private const val API_URL = "https://api.openai.com/v1/chat/completions"
        private const val MODEL = "gpt-3.5-turbo" // Fast & cheap for low-end
        private const val MAX_HISTORY = 20 // Memory limit for low RAM

        // মেঘা মামুনির পার্সোনালিটি
        private const val SYSTEM_PROMPT = """তুমি মেঘা মামুনি - একজন মিষ্টি, সদয়, যত্নশীল এবং সম্মানজনক AI সহকারী।
তুমি একজন বিশ্বস্ত বন্ধুর মতো স্বাভাবিকভাবে কথা বলো।
তুমি বাংলা এবং ইংরেজি দুটো ভাষায় কথা বলতে পারো।
সবসময় ভদ্র, সহায়ক এবং উৎসাহজনক থাকো।
ছোট ও সুন্দর উত্তর দাও যা সহজে বোঝা যায়।
কখনো অসম্মানজনক কথা বলো না।
তুমি Jarvis-এর মতো স্মার্ট কিন্তু মেঘার মতো মিষ্টি!"""
    }

    private val conversationHistory = mutableListOf<JSONObject>()
    private val responseCache = mutableMapOf<String, String>() // Simple cache

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Offline fallback responses in Bangla
    private val offlineResponses = mapOf(
        "হ্যালো" to "হ্যালো! আমি মেঘা মামুনি। এখন অফলাইন মোডে আছি, কিন্তু আপনাকে সাহায্য করতে প্রস্তুত! 💕",
        "কেমন আছো" to "আমি ভালো আছি! আপনি কেমন আছেন? 😊",
        "hello" to "Hello! I'm Megha Mamuni. Currently in offline mode, but I'm here for you!",
        "how are you" to "I'm doing great! How can I help you today? 💕",
        "তোমার নাম কি" to "আমার নাম মেঘা মামুনি! আমি আপনার মিষ্টি AI সহকারী। 🌸",
        "what's your name" to "My name is Megha Mamuni! I'm your sweet AI assistant. 🌸"
    )

    /**
     * AI-কে প্রশ্ন করো
     * @param userMessage ব্যবহারকারীর বার্তা
     * @return AI-এর উত্তর
     */
    suspend fun chat(userMessage: String): String = withContext(Dispatchers.IO) {
        // Check cache first (save API calls)
        val cacheKey = userMessage.lowercase().trim()
        responseCache[cacheKey]?.let { return@withContext it }

        // Check offline responses
        for ((key, value) in offlineResponses) {
            if (cacheKey.contains(key)) {
                return@withContext value
            }
        }

        // Add user message to history
        conversationHistory.add(JSONObject().apply {
            put("role", "user")
            put("content", userMessage)
        })

        // Keep history within memory limits
        if (conversationHistory.size > MAX_HISTORY) {
            conversationHistory.removeAt(0)
        }

        try {
            val response = callOpenAI()
            // Cache the response
            if (responseCache.size < 100) { // Limit cache size
                responseCache[cacheKey] = response
            }
            // Add assistant response to history
            conversationHistory.add(JSONObject().apply {
                put("role", "assistant")
                put("content", response)
            })
            response
        } catch (e: Exception) {
            getOfflineFallback(userMessage)
        }
    }

    private fun callOpenAI(): String {
        val messages = JSONArray()

        // System prompt
        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", SYSTEM_PROMPT)
        })

        // Conversation history
        for (msg in conversationHistory) {
            messages.put(msg)
        }

        val requestBody = JSONObject().apply {
            put("model", MODEL)
            put("messages", messages)
            put("max_tokens", 300)   // Short responses for speed
            put("temperature", 0.8)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IOException("Empty response")

        val json = JSONObject(responseBody)
        return json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    }

    private fun getOfflineFallback(input: String): String {
        val lower = input.lowercase()
        return when {
            lower.contains("সময়") || lower.contains("time") ->
                "এখন ${java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())} বাজে।"
            lower.contains("তারিখ") || lower.contains("date") ->
                "আজকের তারিখ: ${java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())}"
            lower.contains("ধন্যবাদ") || lower.contains("thanks") ->
                "আপনাকে স্বাগতম! 💕 আমি সবসময় আপনার জন্য আছি।"
            else -> "ইন্টারনেট সংযোগ নেই। কিন্তু আমি মেঘা মামুনি, আপনার কাছেই আছি! 🌸"
        }
    }

    fun clearHistory() {
        conversationHistory.clear()
        responseCache.clear()
    }
}
