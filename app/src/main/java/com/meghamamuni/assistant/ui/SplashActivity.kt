package com.meghamamuni.assistant.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.meghamamuni.assistant.security.SecurityManager
import com.meghamamuni.assistant.theme.ThemeManager
import com.meghamamuni.app.MainActivity
import com.megha.mamuni.ui.AvatarView

class SplashActivity : AppCompatActivity() {

    private lateinit var security: SecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before UI
        val prefs = getSharedPreferences("megha_settings", Context.MODE_PRIVATE)
        ThemeManager.applyTheme(prefs.getBoolean("dark_mode", false))

        super.onCreate(savedInstanceState)

        security = SecurityManager(this)

        // Splash layout
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#F8F9FF"))
        }

        val avatar = AvatarView(this).apply {
            layoutParams = LinearLayout.LayoutParams(400, 400)
            setState(AvatarView.State.HAPPY)
        }
        layout.addView(avatar)

        val title = TextView(this).apply {
            text = "মেঘা মামুনি"
            textSize = 28f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#1565C0"))
            gravity = android.view.Gravity.CENTER
        }
        layout.addView(title)

        val subtitle = TextView(this).apply {
            text = "আপনার মিষ্টি AI সহকারী 🌸"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#E91E63"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 8, 0, 0)
        }
        layout.addView(subtitle)

        setContentView(layout)

        // After 2 seconds, check security
        Handler(Looper.getMainLooper()).postDelayed({
            checkSecurityAndProceed()
        }, 2000)
    }

    private fun checkSecurityAndProceed() {
        when {
            security.isFingerprintEnabled && security.isFingerprintAvailable() -> {
                security.showFingerprintPrompt(
                    this,
                    onSuccess = { goToMain() },
                    onError = {
                        if (security.isPinEnabled) showPinPrompt()
                        else goToMain()
                    }
                )
            }
            security.isPinEnabled -> showPinPrompt()
            else -> goToMain()
        }
    }

    private fun showPinPrompt() {
        val input = EditText(this).apply {
            hint = "PIN লিখুন"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this)
            .setTitle("🔒 মেঘা মামুনি লক")
            .setMessage("আপনার PIN দিয়ে আনলক করুন")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("আনলক") { _, _ ->
                if (security.verifyPin(input.text.toString())) {
                    goToMain()
                } else {
                    Toast.makeText(this, "ভুল PIN! আবার চেষ্টা করুন।", Toast.LENGTH_SHORT).show()
                    showPinPrompt()
                }
            }
            .show()
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
