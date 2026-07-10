package com.meghamamuni.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.*
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.meghamamuni.assistant.AIEngine
import com.meghamamuni.assistant.AssistantCommands
import com.meghamamuni.assistant.ChatRepository
import com.meghamamuni.assistant.CommandType
import com.meghamamuni.assistant.VoiceManager
import com.meghamamuni.assistant.theme.ThemeManager
import com.meghamamuni.app.databinding.ActivityMainBinding
import com.megha.mamuni.ui.AvatarView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * মেঘা মামুনি - Main Activity
 * Jarvis-style AI + Mayra-style cute avatar
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var aiEngine: AIEngine
    private lateinit var voiceManager: VoiceManager
    private lateinit var commands: AssistantCommands
    private lateinit var repository: ChatRepository

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private val mainHandler = Handler(Looper.getMainLooper())

    data class ChatMessage(val text: String, val isUser: Boolean)

    inner class ChatAdapter(private val list: MutableList<ChatMessage>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {

        override fun getItemViewType(pos: Int) = if (list[pos].isUser) 1 else 2

        override fun onCreateViewHolder(parent: android.view.ViewGroup, type: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
            val inflater = android.view.LayoutInflater.from(parent.context)
            val view = if (type == 1)
                inflater.inflate(R.layout.item_message_user, parent, false)
            else
                inflater.inflate(R.layout.item_message_ai, parent, false)
            return object : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {}
        }

        override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, pos: Int) {
            holder.itemView.findViewById<TextView>(R.id.text_message_body)?.text = list[pos].text
        }

        override fun getItemCount() = list.size
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme
        val prefs = getSharedPreferences("megha_settings", Context.MODE_PRIVATE)
        ThemeManager.applyTheme(prefs.getBoolean("dark_mode", false))

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initComponents()
        setupRecyclerView()
        setupListeners()
        startClockUpdates()
        checkPermissions()
        greetUser()

        // Check if opened via wake word
        if (intent.getBooleanExtra("from_wake_word", false)) {
            addAIMessage("হ্যাঁ, আমি এখানে! বলুন, আপনার জন্য কী করতে পারি? 🌸")
            voiceManager.speak("হ্যাঁ, আমি এখানে! বলুন।")
        }
    }

    private fun initComponents() {
        aiEngine = AIEngine()
        voiceManager = VoiceManager(this)
        commands = AssistantCommands(this)
        repository = ChatRepository(this)

        voiceManager.setCallback(object : VoiceManager.VoiceCallback {
            override fun onSpeechResult(text: String) {
                binding.etMessage.setText(text)
                processInput(text)
            }
            override fun onSpeakingStarted() {
                runOnUiThread { binding.avatarView.setState(AvatarView.State.SPEAKING) }
            }
            override fun onSpeakingFinished() {
                runOnUiThread { binding.avatarView.setState(AvatarView.State.IDLE) }
            }
            override fun onListeningStarted() {
                runOnUiThread {
                    binding.avatarView.setState(AvatarView.State.LISTENING)
                    binding.tvStatus.text = "শুনছি... 👂"
                }
            }
            override fun onError(message: String) {
                runOnUiThread { binding.tvStatus.text = message }
            }
            override fun onWakeWordDetected() {
                runOnUiThread {
                    binding.avatarView.setState(AvatarView.State.HAPPY)
                    voiceManager.speak("হ্যাঁ বলুন!")
                }
            }
        })
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messages)
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply { stackFromEnd = true }
            adapter = chatAdapter
        }
    }

    private fun setupListeners() {
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text?.toString()?.trim() ?: ""
            if (text.isNotEmpty()) {
                binding.etMessage.text?.clear()
                processInput(text)
            }
        }

        binding.btnMic.setOnClickListener {
            voiceManager.startListening()
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_chat -> { showChat(); true }
                R.id.action_voice -> { voiceManager.startListening(); true }
                R.id.action_settings -> {
                    startActivity(Intent(this, com.meghamamuni.assistant.ui.SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun processInput(text: String) {
        addUserMessage(text)
        binding.avatarView.setState(AvatarView.State.THINKING)
        binding.tvStatus.text = "ভাবছি... 🤔"

        lifecycleScope.launch {
            // Check smart commands first
            val cmdResult = commands.processCommand(text)

            if (cmdResult.type != CommandType.AI_CHAT) {
                // Smart command response
                addAIMessage(cmdResult.response)
                voiceManager.speak(cmdResult.response)
            } else {
                // AI conversation
                val response = aiEngine.chat(text)
                addAIMessage(response)
                voiceManager.speak(response)
            }
            binding.tvStatus.text = "অনলাইন ✨"
        }
    }

    private fun addUserMessage(text: String) {
        messages.add(ChatMessage(text, true))
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.rvMessages.scrollToPosition(messages.size - 1)
        lifecycleScope.launch { repository.saveMessage(text, true) }
    }

    private fun addAIMessage(text: String) {
        runOnUiThread {
            messages.add(ChatMessage(text, false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            binding.rvMessages.scrollToPosition(messages.size - 1)
        }
        lifecycleScope.launch { repository.saveMessage(text, false) }
    }

    private fun greetUser() {
        val userName = repository.settings.userName
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "সুপ্রভাত"
            hour < 17 -> "শুভ দুপুর"
            hour < 20 -> "শুভ বিকাল"
            else -> "শুভ সন্ধ্যা"
        }
        val msg = "$greeting, $userName! 🌸 আমি মেঘা মামুনি। আজকে আপনার জন্য কী করতে পারি?"
        addAIMessage(msg)
        voiceManager.speak(msg)
    }

    private fun startClockUpdates() {
        val fmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val runnable = object : Runnable {
            override fun run() {
                binding.tvTime.text = fmt.format(Date())
                mainHandler.postDelayed(this, 60000)
            }
        }
        mainHandler.post(runnable)

        // Battery
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                val pct = (level * 100f / scale).toInt()
                binding.tvBattery.text = "$pct%"
            }
        }
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun showChat() {
        binding.rvMessages.visibility = View.VISIBLE
    }

    private fun checkPermissions() {
        val perms = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS
        )
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 200)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.release()
    }
}
