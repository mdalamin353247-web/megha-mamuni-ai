package com.meghamamuni.assistant

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * মেঘা মামুনি Smart Commands
 * Jarvis-style intelligent assistant commands
 */
class AssistantCommands(private val context: Context) {

    private val WEATHER_API_KEY = "YOUR_WEATHER_API_KEY" // 🌤️ এখানে OpenWeatherMap API key দাও
    private var isFlashlightOn = false

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * ব্যবহারকারীর কমান্ড বিশ্লেষণ করো এবং সঠিক action নাও
     */
    suspend fun processCommand(input: String): CommandResult {
        val lower = input.lowercase().trim()

        return when {
            // ⏰ সময় ও তারিখ
            lower.contains("সময়") || lower.contains("কয়টা বাজে") || lower.contains("time") ->
                CommandResult(getTime(), CommandType.TIME)

            lower.contains("তারিখ") || lower.contains("আজকে কত") || lower.contains("date") || lower.contains("today") ->
                CommandResult(getDate(), CommandType.DATE)

            // 🔋 ব্যাটারি
            lower.contains("ব্যাটারি") || lower.contains("battery") || lower.contains("চার্জ") ->
                CommandResult(getBatteryInfo(), CommandType.BATTERY)

            // 🔦 ফ্ল্যাশলাইট
            lower.contains("ফ্ল্যাশলাইট") || lower.contains("টর্চ") || lower.contains("flashlight") || lower.contains("torch") -> {
                if (lower.contains("বন্ধ") || lower.contains("off")) {
                    toggleFlashlight(false)
                    CommandResult("ফ্ল্যাশলাইট বন্ধ করা হয়েছে! 🔦", CommandType.FLASHLIGHT)
                } else {
                    toggleFlashlight(true)
                    CommandResult("ফ্ল্যাশলাইট চালু করা হয়েছে! 💡", CommandType.FLASHLIGHT)
                }
            }

            // 🌤️ আবহাওয়া
            lower.contains("আবহাওয়া") || lower.contains("weather") || lower.contains("তাপমাত্রা") ->
                CommandResult(getWeather(), CommandType.WEATHER)

            // 🔍 ইন্টারনেট সার্চ
            lower.contains("সার্চ") || lower.contains("search") || lower.contains("খুঁজে দাও") -> {
                val query = extractSearchQuery(input)
                openInternetSearch(query)
                CommandResult("\"$query\" সার্চ করা হচ্ছে! 🔍", CommandType.SEARCH)
            }

            // 📱 অ্যাপ খোলা
            lower.contains("youtube") || lower.contains("ইউটিউব") -> {
                openApp("com.google.android.youtube", "https://youtube.com")
                CommandResult("YouTube খুলছি! ▶️", CommandType.APP)
            }
            lower.contains("facebook") || lower.contains("ফেসবুক") -> {
                openApp("com.facebook.katana", "https://facebook.com")
                CommandResult("Facebook খুলছি! 📘", CommandType.APP)
            }
            lower.contains("whatsapp") || lower.contains("হোয়াটসঅ্যাপ") -> {
                openApp("com.whatsapp", "https://whatsapp.com")
                CommandResult("WhatsApp খুলছি! 💬", CommandType.APP)
            }
            lower.contains("messenger") || lower.contains("মেসেঞ্জার") -> {
                openApp("com.facebook.orca", "https://messenger.com")
                CommandResult("Messenger খুলছি! 💬", CommandType.APP)
            }

            // ⏰ অ্যালার্ম সেট
            lower.contains("অ্যালার্ম") || lower.contains("alarm") -> {
                setAlarm(input)
                CommandResult("অ্যালার্ম সেট করা হয়েছে! ⏰", CommandType.ALARM)
            }

            // 🧮 ক্যালকুলেটর
            lower.contains("হিসাব") || lower.contains("calculate") || lower.contains("যোগ") ||
            lower.contains("বিয়োগ") || lower.contains("গুণ") || lower.contains("ভাগ") -> {
                val result = calculate(input)
                CommandResult(result, CommandType.CALCULATOR)
            }

            // 🎵 মিউজিক কন্ট্রোল
            lower.contains("গান বন্ধ") || lower.contains("pause music") || lower.contains("stop music") -> {
                controlMedia(false)
                CommandResult("গান বন্ধ করা হয়েছে! 🎵", CommandType.MEDIA)
            }
            lower.contains("গান চালু") || lower.contains("play music") -> {
                controlMedia(true)
                CommandResult("গান চালু হয়েছে! 🎶", CommandType.MEDIA)
            }

            // 📞 ফোন কল
            lower.contains("কল করো") || lower.contains("call") -> {
                val contact = extractContact(input)
                CommandResult("$contact-কে কল করতে চান? নিশ্চিত করুন।", CommandType.CALL_CONFIRM, contact)
            }

            else -> CommandResult("", CommandType.AI_CHAT) // AI-এর কাছে পাঠাও
        }
    }

    private fun getTime(): String {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale("bn", "BD"))
        val time = timeFormat.format(Date())
        return "এখন $time বাজে। ⏰"
    }

    private fun getDate(): String {
        val dateFormat = SimpleDateFormat("dd MMMM, yyyy", Locale("bn", "BD"))
        val day = SimpleDateFormat("EEEE", Locale("bn", "BD"))
        val date = dateFormat.format(Date())
        val dayName = day.format(Date())
        return "আজকে $dayName, $date 📅"
    }

    private fun getBatteryInfo(): String {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = (level * 100 / scale.toFloat()).toInt()
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        return if (isCharging) {
            "ব্যাটারি $pct% - চার্জ হচ্ছে ⚡"
        } else {
            val emoji = when {
                pct > 80 -> "🔋"
                pct > 50 -> "🔋"
                pct > 20 -> "🪫"
                else -> "🔴"
            }
            "ব্যাটারি $pct% $emoji"
        }
    }

    private fun toggleFlashlight(on: Boolean) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, on)
            isFlashlightOn = on
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun getWeather(): String = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.openweathermap.org/data/2.5/weather?q=Dhaka&appid=$WEATHER_API_KEY&units=metric&lang=bn"
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: return@withContext "আবহাওয়া তথ্য পাওয়া যায়নি।")
            val temp = json.getJSONObject("main").getDouble("temp").toInt()
            val desc = json.getJSONArray("weather").getJSONObject(0).getString("description")
            "ঢাকায় এখন তাপমাত্রা $temp°C, $desc 🌤️"
        } catch (e: Exception) {
            "আবহাওয়া তথ্য পেতে পারিনি। ইন্টারনেট চেক করুন। 🌐"
        }
    }

    private fun openInternetSearch(query: String) {
        val uri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun openApp(packageName: String, fallbackUrl: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } else {
                val uri = Uri.parse(fallbackUrl)
                context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setAlarm(input: String) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(AlarmClock.EXTRA_MESSAGE, "মেঘা মামুনি অ্যালার্ম")
        }
        context.startActivity(intent)
    }

    private fun calculate(input: String): String {
        return try {
            // Simple math extraction
            val numbers = Regex("\\d+(\\.\\d+)?").findAll(input).map { it.value.toDouble() }.toList()
            val lower = input.lowercase()
            val result = when {
                lower.contains("যোগ") || lower.contains("+") || lower.contains("add") ->
                    numbers.sum()
                lower.contains("বিয়োগ") || lower.contains("-") || lower.contains("subtract") ->
                    if (numbers.size >= 2) numbers[0] - numbers[1] else 0.0
                lower.contains("গুণ") || lower.contains("×") || lower.contains("multiply") ->
                    numbers.reduce { acc, d -> acc * d }
                lower.contains("ভাগ") || lower.contains("÷") || lower.contains("divide") ->
                    if (numbers.size >= 2 && numbers[1] != 0.0) numbers[0] / numbers[1] else 0.0
                else -> numbers.sum()
            }
            "উত্তর: ${if (result == result.toLong().toDouble()) result.toLong() else result} 🧮"
        } catch (e: Exception) {
            "ক্যালকুলেটর খুলছি! 🧮"
        }
    }

    private fun controlMedia(play: Boolean) {
        val intent = Intent(if (play) "android.intent.action.MUSIC_PLAYER" else "android.intent.action.PAUSE_MUSIC")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        try { context.startActivity(intent) } catch (e: Exception) { e.printStackTrace() }
    }

    private fun extractSearchQuery(input: String): String {
        return input.replace(Regex("সার্চ|search|খুঁজে দাও", RegexOption.IGNORE_CASE), "").trim()
    }

    private fun extractContact(input: String): String {
        return input.replace(Regex("কল করো|call|কে কল", RegexOption.IGNORE_CASE), "").trim()
    }

    fun confirmCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

data class CommandResult(
    val response: String,
    val type: CommandType,
    val extra: String? = null
)

enum class CommandType {
    TIME, DATE, BATTERY, FLASHLIGHT, WEATHER, SEARCH, APP, ALARM, CALCULATOR, MEDIA, CALL_CONFIRM, AI_CHAT
}
