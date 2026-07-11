package com.meghamamuni.assistant.ui

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.meghamamuni.app.R

class SettingsActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("megha_prefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val etUserName = findViewById<EditText>(R.id.etUserName)
        val btnSaveName = findViewById<Button>(R.id.btnSaveName)
        val switchDarkMode = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchDarkMode)
        val switchWakeWord = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchWakeWord)
        val switchPrivacy = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchPrivacy)
        val btnSetPin = findViewById<Button>(R.id.btnSetPin)
        val switchFingerprint = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchFingerprint)
        val btnClearHistory = findViewById<Button>(R.id.btnClearHistory)

        // Load saved values
        etUserName.setText(prefs.getString("user_name", ""))
        switchDarkMode.isChecked = prefs.getBoolean("dark_mode", false)
        switchWakeWord.isChecked = prefs.getBoolean("wake_word", true)
        switchPrivacy.isChecked = prefs.getBoolean("privacy_mode", false)

        // Save name
        btnSaveName.setOnClickListener {
            val name = etUserName.text.toString().trim()
            if (name.isNotEmpty()) {
                prefs.edit().putString("user_name", name).apply()
                Toast.makeText(this, "নাম সেভ হয়েছে: $name ✅", Toast.LENGTH_SHORT).show()
            }
        }

        switchDarkMode.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("dark_mode", checked).apply()
            Toast.makeText(this, if (checked) "ডার্ক মোড চালু 🌙" else "লাইট মোড চালু ☀️", Toast.LENGTH_SHORT).show()
        }

        switchWakeWord.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("wake_word", checked).apply()
        }

        switchPrivacy.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("privacy_mode", checked).apply()
            Toast.makeText(this, if (checked) "প্রাইভেসি মোড চালু 🔒" else "প্রাইভেসি মোড বন্ধ", Toast.LENGTH_SHORT).show()
        }

        btnSetPin.setOnClickListener {
            Toast.makeText(this, "PIN ফিচার শীঘ্রই আসছে! 🔐", Toast.LENGTH_SHORT).show()
        }

        switchFingerprint.setOnCheckedChangeListener { _, _ ->
            Toast.makeText(this, "ফিঙ্গারপ্রিন্ট ফিচার শীঘ্রই আসছে! 👆", Toast.LENGTH_SHORT).show()
        }

        btnClearHistory.setOnClickListener {
            prefs.edit().clear().apply()
            Toast.makeText(this, "ইতিহাস মুছে ফেলা হয়েছে! 🗑️", Toast.LENGTH_SHORT).show()
        }
    }
}
