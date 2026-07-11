package com.meghamamuni.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnMic: ImageButton
    private lateinit var tvStatus: TextView

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTTSReady = false
    private var isListening = false

    private val messages = mutableListOf<Pair<String, Boolean>>()
    private lateinit var adapter: ChatAdapter

    private val prefs by lazy { getSharedPreferences("megha_prefs", MODE_PRIVATE) }
    private val userName get() = prefs.getString("user_name", "বন্ধু") ?: "বন্ধু"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        etInput = findViewById(R.id.etInput)
        btnSend = findViewById(R.id.btnSend)
        btnMic = findViewById(R.id.btnMic)
        tvStatus = findViewById(R.id.tvStatus)

        adapter = ChatAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        recyclerView.adapter = adapter

        btnSend.setOnClickListener {
            val t = etInput.text.toString().trim()
            if (t.isNotEmpty()) { etInput.setText(""); handleInput(t) }
        }
        btnMic.setOnClickListener { if (isListening) stopListen() else startListen() }

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale("bn", "BD"))
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED)
                    tts?.setLanguage(Locale.ENGLISH)
                tts?.setPitch(1.2f); tts?.setSpeechRate(0.9f)
                isTTSReady = true
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greet = when { hour < 12 -> "সুপ্রভাত"; hour < 17 -> "শুভ অপরাহ্ন"; else -> "শুভ সন্ধ্যা" }
        val msg = "$greet $userName! আমি মেঘা মামুনি 💕 আপনাকে কিভাবে সাহায্য করতে পারি?"
        addMsg(msg, false)
        Handler(Looper.getMainLooper()).postDelayed({ speak(msg) }, 600)
    }

    private fun handleInput(input: String) {
        addMsg(input, true)
        val reply = respond(input.lowercase(Locale.getDefault()))
        Handler(Looper.getMainLooper()).postDelayed({ addMsg(reply, false); speak(reply) }, 300)
    }

    private fun respond(q: String): String = when {
        q.contains("সময়") || q.contains("কটা") ->
            "এখন ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())} ⏰"
        q.contains("তারিখ") || q.contains("date") ->
            "আজ ${SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())} 📅"
        q.contains("ব্যাটারি") || q.contains("চার্জ") -> {
            val bm = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
            "ব্যাটারি ${bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)}% 🔋"
        }
        q.contains("youtube") || q.contains("ইউটিউব") -> {
            tryOpen("com.google.android.youtube") ?: run { openUrl("https://youtube.com") }; "YouTube খুলছি ▶️"
        }
        q.contains("facebook") || q.contains("ফেসবুক") -> {
            tryOpen("com.facebook.katana") ?: openUrl("https://facebook.com"); "Facebook খুলছি 👍"
        }
        q.contains("whatsapp") || q.contains("হোয়াটসঅ্যাপ") -> {
            tryOpen("com.whatsapp") ?: "WhatsApp নেই!"; "WhatsApp খুলছি 💬"
        }
        q.contains("হ্যালো") || q.contains("hello") || q.contains("সালাম") ->
            "ওয়ালাইকুম সালাম $userName! 😊 কেমন আছেন?"
        q.contains("কেমন আছ") || q.contains("how are") ->
            "আলহামদুলিল্লাহ ভালো! আপনি কেমন আছেন $userName? 💕"
        q.contains("ধন্যবাদ") || q.contains("thanks") ->
            "আপনাকে স্বাগতম $userName! 🌸"
        q.contains("তোমার নাম") || q.contains("your name") ->
            "আমি মেঘা মামুনি 💕 আপনার AI বন্ধু!"
        q.contains("ভালোবাসি") || q.contains("love") ->
            "আমিও ভালোবাসি $userName! 💕🌸"
        q.contains("জোকস") || q.contains("joke") || q.contains("হাসি") ->
            "প্রোগ্রামার দোকানে গেল। বলল: দুধ লাগবে। দোকানদার: কত? সে: while(আছে) নিতে থাকো 😂"
        q.contains("গান") || q.contains("music") -> {
            tryOpen("com.spotify.music") ?: tryOpen("com.google.android.youtube"); "গান চালু করছি 🎵"
        }
        q.contains("সেটিং") || q.contains("setting") -> {
            startActivity(Intent(this, com.meghamamuni.assistant.ui.SettingsActivity::class.java)); "সেটিংস খুলছি ⚙️"
        }
        q.contains("বিদায়") || q.contains("bye") ->
            "আল্লাহ হাফেজ $userName! ভালো থাকুন 💕"
        else -> listOf(
            "বুঝিনি $userName, একটু অন্যভাবে বলুন? 🤔",
            "আরেকটু বিস্তারিত বলুন! 💕",
            "এটা শিখছি এখনো 😊 অন্য কিছু জিজ্ঞেস করুন!"
        ).random()
    }

    private fun tryOpen(pkg: String): String? = try {
        packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it); "ok" }
    } catch (e: Exception) { null }

    private fun openUrl(url: String) = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    private fun addMsg(text: String, isUser: Boolean) {
        messages.add(Pair(text, isUser))
        adapter.notifyItemInserted(messages.size - 1)
        recyclerView.smoothScrollToPosition(messages.size - 1)
    }

    private fun speak(text: String) {
        if (isTTSReady) { tts?.stop(); tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null) }
    }

    private fun startListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "মাইক্রোফোন অনুমতি দিন", Toast.LENGTH_SHORT).show(); return
        }
        isListening = true; tvStatus.visibility = View.VISIBLE
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(r: Bundle?) {
                r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { handleInput(it) }
                stopListen()
            }
            override fun onError(e: Int) { stopListen() }
            override fun onReadyForSpeech(p: Bundle?) {}; override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(r: Float) {}; override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}; override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        try {
            speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            })
        } catch (e: Exception) { stopListen() }
    }

    private fun stopListen() {
        isListening = false; tvStatus.visibility = View.GONE
        speechRecognizer?.destroy(); speechRecognizer = null
    }

    override fun onDestroy() { super.onDestroy(); tts?.shutdown(); speechRecognizer?.destroy() }

    inner class ChatAdapter(private val list: List<Pair<String, Boolean>>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        inner class UVH(v: View) : RecyclerView.ViewHolder(v) { val tv: TextView = v.findViewById(R.id.tvMessage) }
        inner class BVH(v: View) : RecyclerView.ViewHolder(v) { val tv: TextView = v.findViewById(R.id.tvMessage) }

        override fun getItemViewType(p: Int) = if (list[p].second) 1 else 0
        override fun getItemCount() = list.size

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
            val layout = if (type == 1) R.layout.item_message_user else R.layout.item_message_ai
            val v = LayoutInflater.from(parent.context).inflate(layout, parent, false)
            return if (type == 1) UVH(v) else BVH(v)
        }

        override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
            val (txt, _) = list[pos]
            when (h) { is UVH -> h.tv.text = txt; is BVH -> h.tv.text = txt }
        }
    }
}

