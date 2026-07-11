package com.meghamamuni.assistant.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Wake Word Service - "মেঘা মামুনি" voice trigger
 * Simple stub - full implementation coming soon
 */
class WakeWordService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }
}
