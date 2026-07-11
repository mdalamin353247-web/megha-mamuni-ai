package com.meghamamuni.assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * মেঘা মামুনি Voice Manager
 * STT (Speech-to-Text) + TTS (Text-to-Speech)
 * Cute female voice with pitch 1.2f and speed 0.9f (like Mayra AI)
 */
class VoiceManager(private val context: Context) {

    interface VoiceCallback {
        fun onSpeechResult(text: String)
        fun onSpeakingStarted()
        fun onSpeakingFinished()
        fun onListeningStarted()
        fun onError(message: String)
        fun onWakeWordDetected()
    }

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var callback: VoiceCallback? = null
    private var isTTSReady = false
    private var currentLanguage = "bn" // Default Bangla

    // Wake word detection
    private var isListeningForWakeWord = false
    private val wakeWords = listOf("মেঘা মামুনি", "hey megha", "মেঘা", "megha mamuni")

    init {
        initTTS()
        initSpeechRecognizer()
    }

    private fun initTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTTSReady = true
                setLanguage(currentLanguage)

                // 🎀 Cute female voice settings (like Mayra AI)
                tts?.setPitch(1.2f)     // Higher pitch = cuter, more feminine
                tts?.setSpeechRate(0.9f) // Slightly slower = more natural

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        callback?.onSpeakingStarted()
                    }
                    override fun onDone(utteranceId: String?) {
                        callback?.onSpeakingFinished()
                    }
                    override fun onError(utteranceId: String?) {
                        callback?.onSpeakingFinished()
                    }
                })
            }
        }
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    callback?.onListeningStarted()
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: return

                    if (isListeningForWakeWord) {
                        // Check for wake words
                        val lower = text.lowercase()
                        if (wakeWords.any { lower.contains(it.lowercase()) }) {
                            callback?.onWakeWordDetected()
                        } else {
                            // Continue listening for wake word
                            startWakeWordListening()
                        }
                    } else {
                        callback?.onSpeechResult(text)
                    }
                }
                override fun onError(error: Int) {
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "কথা বুঝতে পারিনি, আবার বলুন।"
                        SpeechRecognizer.ERROR_NETWORK -> "নেটওয়ার্ক সমস্যা।"
                        SpeechRecognizer.ERROR_AUDIO -> "মাইক্রোফোন সমস্যা।"
                        else -> "ভয়েস চেনার সমস্যা হয়েছে।"
                    }
                    callback?.onError(msg)
                    if (isListeningForWakeWord) {
                        startWakeWordListening() // Restart wake word listening
                    }
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    /**
     * মেঘা মামুনিকে কথা বলাও
     */
    fun speak(text: String) {
        if (!isTTSReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "megha_utterance_${System.currentTimeMillis()}")
    }

    /**
     * ভয়েস ইনপুট শুরু করো
     */
    fun startListening() {
        isListeningForWakeWord = false
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (currentLanguage == "bn") "bn-BD" else "en-US")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, if (currentLanguage == "bn") "bn-BD" else "en-US")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
    }

    /**
     * ওয়েক ওয়ার্ড শোনা শুরু করো
     */
    fun startWakeWordListening() {
        isListeningForWakeWord = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListeningForWakeWord = false
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun setLanguage(lang: String) {
        currentLanguage = lang
        val locale = when (lang) {
            "bn" -> Locale("bn", "BD")
            "en" -> Locale.US
            else -> Locale("bn", "BD")
        }
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to English if Bangla not available
            tts?.setLanguage(Locale.US)
        }
    }

    fun setCallback(cb: VoiceCallback) {
        this.callback = cb
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
