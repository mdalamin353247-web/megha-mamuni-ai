package com.megha.mamuni.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import android.graphics.Color

class AvatarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : androidx.appcompat.widget.AppCompatTextView(context, attrs) {
    init {
        text = "🤗"
        textSize = 48f
        setTextColor(Color.BLACK)
    }
    fun startIdleAnimation() {}
    fun startSpeakingAnimation() {}
    fun stopSpeakingAnimation() {}
}

