package com.meghamamuni.app

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class AvatarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {
    init {
        text = "🤗"
        textSize = 48f
        setTextColor(Color.BLACK)
        gravity = android.view.Gravity.CENTER
    }
    fun startIdleAnimation() {}
    fun startSpeakingAnimation() {}
    fun stopSpeakingAnimation() {}
}
