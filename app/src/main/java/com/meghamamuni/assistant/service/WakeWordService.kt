package com.meghamamuni.assistant.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.meghamamuni.app.MainActivity

/**
 * মেঘা মামুনি Wake Word Service
 * Background-এ "মেঘা মামুনি" / "Hey Megha" শোনে
 * Battery-friendly: 15% এর নিচে হলে বন্ধ হয়
 */
class WakeWordService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private val CHANNEL_ID = "megha_wake_word_channel"
    private val NOTIFICATION_ID = 101
    private val handler = Handler(Looper.getMainLooper())

    private val wakeWords = listOf("মেঘা মামুনি", "hey megha", "মেঘা", "megha mamuni", "হেই মেঘা")

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startWakeWordListening()
    }

    private fun startWakeWordListening() {
        if (!isBatteryOk()) {
            stopSelf()
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            stopSelf()
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.lowercase() ?: ""

                if (wakeWords.any { text.contains(it.lowercase()) }) {
                    // Wake word detected! Open app
                    openMainActivity()
                } else {
                    // Restart listening
                    handler.postDelayed({ restartListening() }, 500)
                }
            }
            override fun onError(error: Int) {
                handler.postDelayed({ restartListening() }, 2000)
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun restartListening() {
        if (isBatteryOk()) {
            speechRecognizer?.destroy()
            startWakeWordListening()
        } else {
            stopSelf()
        }
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("from_wake_word", true)
        }
        startActivity(intent)
    }

    private fun isBatteryOk(): Boolean {
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, 100) ?: 100
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = (level * 100 / scale.toFloat()).toInt()
        return pct > 15 // Stop if battery below 15%
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "মেঘা মামুনি ওয়েক ওয়ার্ড",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "ওয়েক ওয়ার্ড শোনার জন্য চলছে"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("মেঘা মামুনি")
            .setContentText("\"মেঘা মামুনি\" বলুন...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
