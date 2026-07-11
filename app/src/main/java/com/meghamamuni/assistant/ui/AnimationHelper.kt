package com.megha.mamuni.ui

import android.app.ActivityManager
import android.content.Context

/**
 * লো-এন্ড ডিভাইস ডিটেকশন
 * 2GB RAM বা কম হলে হালকা অ্যানিমেশন চালু করো
 */
object AnimationHelper {

    private var cachedResult: Boolean? = null

    fun isLowEndDevice(context: Context): Boolean {
        cachedResult?.let { return it }

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        
        val result = when {
            activityManager?.isLowRamDevice == true -> true
            else -> {
                val memInfo = ActivityManager.MemoryInfo()
                activityManager?.getMemoryInfo(memInfo)
                val totalRam = memInfo.totalMem
                val twoGB = 2L * 1024 * 1024 * 1024
                totalRam < twoGB
            }
        }

        cachedResult = result
        return result
    }

    fun getAnimationDuration(context: Context, normalDuration: Long): Long {
        return if (isLowEndDevice(context)) (normalDuration * 1.5).toLong() else normalDuration
    }

    fun shouldPlayComplexAnimations(context: Context): Boolean {
        return !isLowEndDevice(context)
    }
}
