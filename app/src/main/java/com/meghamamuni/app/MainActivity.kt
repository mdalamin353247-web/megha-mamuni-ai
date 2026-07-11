package com.meghamamuni.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
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

    private lateinit var tts: TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTTSReady = false
    private var isListening = false

    private lateinit var recyclerView: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnMic: ImageButton
    private lateinit var tvStatus: TextView
    private lateinit var avatarLayout: LinearLayout

    private val messages = mutableListOf<Pair<String, Boolean>>() // text, isUser
    private lateinit var adapter: ChatAdapter

    private val prefs by lazy { getSharedPreferences("megha_prefs", Context.MODE_PRIVATE) }
    private val userName get() = prefs.getString("user_name", "বন্ধু") ?: "বন্ধু"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initTTS()
        setupRecyclerView()
        setupListeners()
        requestPermissions()
        greetUser()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        etInput = findViewById(R.id.etInput)
        btnSend = findViewById(R.id.btnSend)
        btnMic = findViewById(R.id.btnMic)
        tvStatus = findViewById(R.id.tvStatus)
        avatarLayout = findViewById(R.id.avatarLayout)
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerView.adapter = adapter
    }

    private fun setupListeners() {
        btnSend.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                etInput.setText("")
                handleUserInput(text)
            }
        }

        btnMic.setOnClickListener {
            if (isListening) {
                stopListening()
            } else {
                startListening()
            }
        }
    }

    private fun greetUser() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "সুপ্রভাত"
            hour < 17 -> "শুভ অপরাহ্ন"
            else -> "শুভ সন্ধ্যা"
        }
        val msg = "$greeting $userName! আমি মেঘা মামুনি 💕 আপনাকে কিভাবে সাহায্য করতে পারি?"
        addMessage(msg, false)
        Handler(Looper.getMainLooper()).postDelayed({ speak(msg) }, 500)
    }

    private fun handleUserInput(input: String) {
        addMessage(input, true)
        val response = generateResponse(input.lowercase())
        Handler(Looper.getMainLooper()).postDelayed({
            addMessage(response, false)
            speak(response)
        }, 300)
    }

    private fun generateResponse(input: String): String {
        return when {
            // সময় ও তারিখ
            input.contains("সময়") || input.contains("time") || input.contains("কটা বাজে") -> {
                val time = SimpleDateFormat("hh:mm a", Locale("bn")).format(Date())
                "এখন সময় $time ⏰"
            }
            input.contains("তারিখ") || input.contains("date") || input.contains("আজকে কত") -> {
                val date = SimpleDateFormat("dd MMMM yyyy", Locale("bn")).format(Date())
                "আজকের তারিখ: $date 📅"
            }

            // ব্যাটারি
            input.contains("ব্যাটারি") || input.contains("battery") || input.contains("চার্জ") -> {
                val bm = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
                val level = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
                val charging = bm.isCharging
                "ব্যাটারি $level% আছে ${if (charging) "এবং চার্জ হচ্ছে ⚡" else "🔋"}"
            }

            // ক্যালকুলেটর
            input.contains("calculator") || input.contains("ক্যালকুলেটর") || input.contains("হিসাব") -> {
                openApp("com.android.calculator2") ?: run {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=calculator")))
                    "ক্যালকুলেটর খুলছি... 🔢"
                }
                "ক্যালকুলেটর খুলছি! 🔢"
            }

            // YouTube
            input.contains("youtube") || input.contains("ইউটিউব") -> {
                openApp("com.google.android.youtube") ?: openUrl("https://youtube.com")
                "YouTube খুলছি! ▶️"
            }

            // Facebook
            input.contains("facebook") || input.contains("ফেসবুক") -> {
                openApp("com.facebook.katana") ?: openUrl("https://facebook.com")
                "Facebook খুলছি! 👍"
            }

            // WhatsApp
            input.contains("whatsapp") || input.contains("হোয়াটসঅ্যাপ") -> {
                openApp("com.whatsapp") ?: "WhatsApp ইনস্টল নেই! 📱"
                "WhatsApp খুলছি! 💬"
            }

            // ইন্টারনেট সার্চ
            input.contains("search") || input.contains("খোঁজ") || input.contains("সার্চ") -> {
                val query = input.replace("search", "").replace("খোঁজ", "").replace("সার্চ", "").trim()
                openUrl("https://www.google.com/search?q=${Uri.encode(query)}")
                "\"$query\" সার্চ করছি! 🔍"
            }

            // আবহাওয়া
            input.contains("আবহাওয়া") || input.contains("weather") || input.contains("বৃষ্টি") -> {
                openUrl("https://weather.com")
                "আবহাওয়ার তথ্য খুলছি! ⛅"
            }

            // গান
            input.contains("গান") || input.contains("music") || input.contains("মিউজিক") -> {
                openApp("com.spotify.music") ?: openApp("com.google.android.music")
                ?: "গান চালু করতে Spotify বা YouTube Music ব্যবহার করুন! 🎵"
                "গান চালু করছি! 🎵"
            }

            // হ্যালো / সালাম
            input.contains("হ্যালো") || input.contains("হেলো") || input.contains("hello") ||
                    input.contains("সালাম") || input.contains("আসসালামু") -> {
                val responses = listOf(
                    "ওয়ালাইকুম সালাম $userName! কেমন আছেন? 😊",
                    "হ্যালো $userName! আপনার জন্য কী করতে পারি? 💕",
                    "আদাব $userName! সুস্থ আছেন তো? 🌸"
                )
                responses.random()
            }

            // কেমন আছো
            input.contains("কেমন আছ") || input.contains("কেমন আছো") || input.contains("how are you") -> {
                "আমি খুব ভালো আছি, ধন্যবাদ! আপনি কেমন আছেন $userName? 😊💕"
            }

            // ধন্যবাদ
            input.contains("ধন্যবাদ") || input.contains("thanks") || input.contains("থ্যাংক") -> {
                "আপনাকে স্বাগতম $userName! 🌸 আর কিছু দরকার হলে বলুন!"
            }

            // নাম
            input.contains("তোমার নাম") || input.contains("আপনার নাম") || input.contains("your name") -> {
                "আমার নাম মেঘা মামুনি! 💕 আমি আপনার AI বন্ধু এবং সহকারী। আপনার যেকোনো কাজে সাহায্য করতে পারি! 🤗"
            }

            // জোকস
            input.contains("জোকস") || input.contains("হাসি") || input.contains("মজার") || input.contains("joke") -> {
                val jokes = listOf(
                    "একজন প্রোগ্রামার দোকানে গেল। দোকানদার বলল: ১টি জামা নিন। সে বলল: না, ০টিও লাগবে না, ২টিও লাগবে না, শুধু ১টি! 😄",
                    "টিচার: বাংলাদেশের রাজধানী কী? ছাত্র: ঢাকা। টিচার: বাহ! ছাত্র: কিন্তু ট্রাফিক জ্যামে আটকে আছে! 😂",
                    "প্রশ্ন: কম্পিউটার কখন ক্লান্ত হয়? উত্তর: যখন Windows update আসে! 😅"
                )
                jokes.random()
            }

            // অনুপ্রেরণা
            input.contains("অনুপ্রেরণা") || input.contains("motivation") || input.contains("উৎসাহ") -> {
                val quotes = listOf(
                    "\"স্বপ্ন দেখো, কারণ স্বপ্নই সাফল্যের প্রথম ধাপ!\" 🌟",
                    "\"প্রতিটি দিন একটি নতুন সুযোগ। হাল ছেড়ো না!\" 💪",
                    "\"তুমি যা ভাবো তাই হতে পারো। বিশ্বাস রাখো নিজের উপর!\" ✨"
                )
                quotes.random()
            }

            // গল্প
            input.contains("গল্প") || input.contains("story") -> {
                "একদিন এক ছোট্ট মেয়ে স্বপ্ন দেখল সে বড় বিজ্ঞানী হবে। সবাই বলল 'সম্ভব না'। কিন্তু সে হাল ছাড়েনি। পড়াশোনা করল, পরিশ্রম করল। একদিন সত্যিই বড় বিজ্ঞানী হল! 🌟 শিক্ষা: স্বপ্ন দেখলে এবং পরিশ্রম করলে সব সম্ভব!"
            }

            // অনুবাদ
            input.contains("translate") || input.contains("অনুবাদ") -> {
                openUrl("https://translate.google.com/?sl=bn&tl=en")
                "Google Translate খুলছি! 🌐"
            }

            // সেটিংস
            input.contains("setting") || input.contains("সেটিং") || input.contains("সেটিংস") -> {
                startActivity(Intent(this@MainActivity,
                    com.meghamamuni.assistant.ui.SettingsActivity::class.java))
                "সেটিংস খুলছি! ⚙️"
            }

            // বিদায়
            input.contains("বিদায়") || input.contains("bye") || input.contains("আল্লাহ হাফেজ") -> {
                "আল্লাহ হাফেজ $userName! ভালো থাকুন। আবার কথা হবে! 💕🌸"
            }

            // ভালোবাসা
            input.contains("ভালোবাসি") || input.contains("love you") || input.contains("আই লাভ") -> {
                "আমিও আপনাকে ভালোবাসি $userName! 💕 আপনার মঙ্গল কামনা করি সবসময়! 🌸"
            }

            // Default
            else -> {
                val defaults = listOf(
                    "বুঝতে পারিনি $userName! একটু ভিন্নভাবে বলবেন? 🤔",
                    "আরেকটু বিস্তারিত বলুন $userName, আমি সাহায্য করব! 💕",
                    "এই বিষয়টা এখনো শিখছি! অন্য কিছু জিজ্ঞেস করুন $userName 😊"
                )
                defaults.random()
            }
        }
    }

    private fun openApp(packageName: String): String? {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent)
                "খুলছি!"
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun openUrl(url: String): String {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        return "খুলছি!"
    }

    private fun addMessage(text: String, isUser: Boolean) {
        messages.add(Pair(text, isUser))
        adapter.notifyItemInserted(messages.size - 1)
        recyclerView.smoothScrollToPosition(messages.size - 1)
    }

    private fun initTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale("bn", "BD"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.ENGLISH)
                }
                tts.setPitch(1.2f)
                tts.setSpeechRate(0.9f)
                isTTSReady = true
            }
        }
    }

    private fun speak(text: String) {
        if (isTTSReady) {
            tts.stop()
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "megha_${System.currentTimeMillis()}")
        }
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "মাইক্রোফোনের অনুমতি দিন", Toast.LENGTH_SHORT).show()
            return
        }
        isListening = true
        btnMic.setImageResource(android.R.drawable.ic_btn_speak_now)
        tvStatus.text = "শুনছি... 🎤"
        tvStatus.visibility = View.VISIBLE

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotEmpty()) handleUserInput(text)
                stopListening()
            }
            override fun onError(error: Int) { stopListening() }
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
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            stopListening()
            Toast.makeText(this, "ভয়েস সাপোর্ট করছে না", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopListening() {
        isListening = false
        btnMic.setImageResource(android.R.drawable.ic_btn_speak_now)
        tvStatus.visibility = View.GONE
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun requestPermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.RECORD_AUDIO)
        if (needed.isNotEmpty())
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isTTSReady) tts.shutdown()
        speechRecognizer?.destroy()
    }

    // ── Chat Adapter ──────────────────────────────────────────────
    inner class ChatAdapter(private val list: List<Pair<String, Boolean>>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        inner class UserVH(v: View) : RecyclerView.ViewHolder(v) {
            val tv: TextView = v.findViewById(R.id.tvMessage)
        }
        inner class BotVH(v: View) : RecyclerView.ViewHolder(v) {
            val tv: TextView = v.findViewById(R.id.tvMessage)
        }

        override fun getItemViewType(position: Int) = if (list[position].second) 1 else 0
        override fun getItemCount() = list.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == 1) {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_message_user, parent, false)
                UserVH(v)
            } else {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_message_ai, parent, false)
                BotVH(v)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val (text, _) = list[position]
            when (holder) {
                is UserVH -> holder.tv.text = text
                is BotVH -> holder.tv.text = text
            }
        }
    }
}
