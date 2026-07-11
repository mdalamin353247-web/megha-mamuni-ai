package com.meghamamuni.assistant.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.meghamamuni.assistant.ChatRepository
import com.meghamamuni.assistant.security.SecurityManager
import com.meghamamuni.assistant.service.WakeWordService
import com.meghamamuni.assistant.theme.ThemeManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * মেঘা মামুনি Settings Screen
 * সব কাস্টমাইজেশন এখানে
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var repository: ChatRepository
    private lateinit var security: SecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = ChatRepository(this)
        security = SecurityManager(this)

        // Simple programmatic layout (lightweight for low-end)
        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        scroll.addView(layout)
        setContentView(scroll)

        title = "মেঘা মামুনি সেটিংস"

        buildSettingsUI(layout)
    }

    private fun buildSettingsUI(layout: LinearLayout) {
        val settings = repository.settings

        // ── Section: ব্যবহারকারী ──────────────────────
        addHeader(layout, "👤 ব্যবহারকারী")

        val nameEdit = EditText(this).apply {
            hint = "আপনার নাম লিখুন"
            setText(settings.userName)
        }
        layout.addView(nameEdit)

        addButton(layout, "নাম সেভ করুন") {
            settings.userName = nameEdit.text.toString().ifEmpty { "বন্ধু" }
            toast("নাম সেভ হয়েছে ✅")
        }

        // ── Section: ভাষা ──────────────────────────────
        addHeader(layout, "🌐 ভাষা")

        val langGroup = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        val bnBtn = RadioButton(this).apply { text = "বাংলা"; isChecked = settings.language == "bn" }
        val enBtn = RadioButton(this).apply { text = "English"; isChecked = settings.language == "en" }
        langGroup.addView(bnBtn)
        langGroup.addView(enBtn)
        layout.addView(langGroup)
        langGroup.setOnCheckedChangeListener { _, id ->
            settings.language = if (id == bnBtn.id) "bn" else "en"
            toast("ভাষা পরিবর্তন হয়েছে ✅")
        }

        // ── Section: থিম ───────────────────────────────
        addHeader(layout, "🎨 থিম")

        val darkSwitch = Switch(this).apply {
            text = "ডার্ক মোড"
            isChecked = settings.isDarkMode
        }
        layout.addView(darkSwitch)
        darkSwitch.setOnCheckedChangeListener { _, checked ->
            settings.isDarkMode = checked
            ThemeManager.applyTheme(checked)
            toast(if (checked) "ডার্ক মোড চালু 🌙" else "লাইট মোড চালু ☀️")
        }

        // ── Section: ভয়েস ──────────────────────────────
        addHeader(layout, "🎙️ ভয়েস সেটিংস")

        addLabel(layout, "পিচ (Pitch): ${settings.voicePitch}")
        val pitchBar = SeekBar(this).apply {
            max = 20
            progress = ((settings.voicePitch - 0.5f) * 10).toInt().coerceIn(0, 20)
        }
        layout.addView(pitchBar)
        pitchBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, u: Boolean) {
                settings.voicePitch = 0.5f + p * 0.1f
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) { toast("পিচ: ${settings.voicePitch}") }
        })

        addLabel(layout, "গতি (Speed): ${settings.voiceSpeed}")
        val speedBar = SeekBar(this).apply {
            max = 15
            progress = ((settings.voiceSpeed - 0.5f) * 10).toInt().coerceIn(0, 15)
        }
        layout.addView(speedBar)
        speedBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, u: Boolean) {
                settings.voiceSpeed = 0.5f + p * 0.1f
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) { toast("গতি: ${settings.voiceSpeed}") }
        })

        // ── Section: অ্যাভাটার ─────────────────────────
        addHeader(layout, "🎀 অ্যাভাটার")

        val avatarGroup = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        listOf("অ্যাভাটার ১", "অ্যাভাটার ২", "অ্যাভাটার ৩").forEachIndexed { idx, name ->
            val rb = RadioButton(this).apply {
                text = name
                isChecked = settings.selectedAvatar == idx
                id = idx + 100
            }
            avatarGroup.addView(rb)
        }
        layout.addView(avatarGroup)
        avatarGroup.setOnCheckedChangeListener { _, id ->
            settings.selectedAvatar = id - 100
            toast("অ্যাভাটার পরিবর্তন হয়েছে 🎀")
        }

        // ── Section: ওয়েক ওয়ার্ড ──────────────────────
        addHeader(layout, "🔊 ওয়েক ওয়ার্ড")

        val wakeSwitch = Switch(this).apply {
            text = "\"মেঘা মামুনি\" ওয়েক ওয়ার্ড চালু করুন"
            isChecked = settings.isWakeWordEnabled
        }
        layout.addView(wakeSwitch)
        wakeSwitch.setOnCheckedChangeListener { _, checked ->
            settings.isWakeWordEnabled = checked
            if (checked) {
                startService(Intent(this@SettingsActivity, WakeWordService::class.java))
                toast("ওয়েক ওয়ার্ড চালু হয়েছে 🎤")
            } else {
                stopService(Intent(this@SettingsActivity, WakeWordService::class.java))
                toast("ওয়েক ওয়ার্ড বন্ধ হয়েছে")
            }
        }

        // ── Section: সিকিউরিটি ─────────────────────────
        addHeader(layout, "🔒 সিকিউরিটি")

        val privSwitch = Switch(this).apply {
            text = "প্রাইভেসি মোড"
            isChecked = security.isPrivacyMode
        }
        layout.addView(privSwitch)
        privSwitch.setOnCheckedChangeListener { _, checked ->
            security.setPrivacyMode(checked)
            toast(if (checked) "প্রাইভেসি মোড চালু 🔒" else "প্রাইভেসি মোড বন্ধ")
        }

        addButton(layout, if (security.isPinEnabled) "PIN পরিবর্তন করুন" else "PIN সেট করুন") {
            showPinDialog()
        }

        if (security.isFingerprintAvailable()) {
            val fpSwitch = Switch(this).apply {
                text = "ফিঙ্গারপ্রিন্ট আনলক"
                isChecked = security.isFingerprintEnabled
            }
            layout.addView(fpSwitch)
            fpSwitch.setOnCheckedChangeListener { _, checked ->
                security.setFingerprintEnabled(checked)
                toast(if (checked) "ফিঙ্গারপ্রিন্ট চালু ✅" else "ফিঙ্গারপ্রিন্ট বন্ধ")
            }
        }

        // ── Section: ডেটা ──────────────────────────────
        addHeader(layout, "🗑️ ডেটা")

        addButton(layout, "কথোপকথনের ইতিহাস মুছুন") {
            AlertDialog.Builder(this)
                .setTitle("নিশ্চিত করুন")
                .setMessage("সব কথোপকথন মুছে যাবে। নিশ্চিত?")
                .setPositiveButton("হ্যাঁ, মুছুন") { _, _ ->
                    lifecycleScope.launch {
                        repository.clearHistory()
                        toast("ইতিহাস মুছে গেছে ✅")
                    }
                }
                .setNegativeButton("না", null)
                .show()
        }

        // ── About ──────────────────────────────────────
        addHeader(layout, "ℹ️ সম্পর্কে")
        addLabel(layout, "মেঘা মামুনি v1.0\nDeveloper: MdRana MsPakhi\nBuilt with ❤️ and Vesper AI")
    }

    private fun showPinDialog() {
        val input = EditText(this).apply {
            hint = "৪ সংখ্যার PIN"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this)
            .setTitle("PIN সেট করুন")
            .setView(input)
            .setPositiveButton("সেট করুন") { _, _ ->
                val pin = input.text.toString()
                if (pin.length >= 4) {
                    security.setupPin(pin)
                    toast("PIN সেট হয়েছে 🔒")
                } else {
                    toast("কমপক্ষে ৪ সংখ্যা দিন")
                }
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    private fun addHeader(layout: LinearLayout, text: String) {
        layout.addView(TextView(this).apply {
            this.text = text
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 32, 0, 8)
        })
    }

    private fun addLabel(layout: LinearLayout, text: String) {
        layout.addView(TextView(this).apply {
            this.text = text
            textSize = 13f
            setPadding(0, 4, 0, 4)
        })
    }

    private fun addButton(layout: LinearLayout, label: String, onClick: () -> Unit) {
        layout.addView(Button(this).apply {
            text = label
            setOnClickListener { onClick() }
        })
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
