package com.meghamamuni.assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Voice Manager - TTS handler for Megha Mamuni
 * মেঘা মামুনি এর ভয়েস সিস্টেম (Mayra AI style - pitch 1.2f)
 */
class VoiceManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("bn", "BD"))
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
                tts?.setPitch(1.2f)        // Mayra AI style
                tts?.setSpeechRate(0.9f)    // Natural speed
                isReady = true
            }
        }
    }

    fun speak(text: String) {
        if (isReady) {
            tts?.stop()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "megha_tts")
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}
