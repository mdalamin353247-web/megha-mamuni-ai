package com.megha.mamuni.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator

/**
 * মেঘা মামুনি কিউট গার্ল অ্যাভাটার
 * Canvas দিয়ে আঁকা - Mayra AI স্টাইল অ্যানিমেশন
 * Low-end device optimized
 */
class AvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class State { IDLE, SPEAKING, THINKING, HAPPY, LISTENING }

    private var currentState: State = State.IDLE

    // Paint objects - প্রতিটা রঙের জন্য আলাদা Paint
    private val skinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFE0BD") }
    private val hairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2C1810") }
    private val eyeWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val eyePupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1A1A2E") }
    private val eyeHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; alpha = 200 }
    private val lipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E91E63")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }
    private val cheekPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFB3BA")
        alpha = 160
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D4A574")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val eyebrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3E2723")
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
    }
    private val neckPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFE0BD") }
    private val clothPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1565C0") }

    // Animation values
    private var blinkProgress = 1.0f   // 1.0 = open, 0.0 = closed
    private var breathScale = 1.0f
    private var headBobY = 0f
    private var mouthOpen = 0f
    private var eyeLookX = 0f
    private var smileExtra = 0f
    private var glowAlpha = 0f

    // Animators
    private var breathAnim: ValueAnimator? = null
    private var bobAnim: ValueAnimator? = null
    private var blinkAnim: ValueAnimator? = null
    private var speakAnim: ValueAnimator? = null
    private var thinkAnim: ValueAnimator? = null
    private var glowAnim: ValueAnimator? = null
    private var blinkTimer: java.util.Timer? = null

    // Paths
    private val hairPath = Path()
    private val bangPath = Path()
    private val mouthPath = Path()
    private val clothPath = Path()

    // Glow paint for listening state
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E91E63")
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    init {
        startIdleAnimations()
    }

    private fun startIdleAnimations() {
        val isLowEnd = AnimationHelper.isLowEndDevice(context)

        // Breathing
        breathAnim = ValueAnimator.ofFloat(
            if (isLowEnd) 0.99f else 0.98f,
            if (isLowEnd) 1.01f else 1.02f
        ).apply {
            duration = if (isLowEnd) 3500L else 2500L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { breathScale = it.animatedValue as Float; invalidate() }
            start()
        }

        if (!isLowEnd) {
            // Head bob
            bobAnim = ValueAnimator.ofFloat(-5f, 5f).apply {
                duration = 4000
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { headBobY = it.animatedValue as Float; invalidate() }
                start()
            }

            // Blink every 3-5 seconds
            startBlinkTimer()
        }
    }

    private fun startBlinkTimer() {
        blinkTimer?.cancel()
        blinkTimer = java.util.Timer()
        val delay = (3000..5000).random().toLong()
        blinkTimer?.scheduleAtFixedRate(object : java.util.TimerTask() {
            override fun run() { post { doBlink() } }
        }, delay, (3500..5000).random().toLong())
    }

    private fun doBlink() {
        blinkAnim?.cancel()
        blinkAnim = ValueAnimator.ofFloat(1f, 0f, 1f).apply {
            duration = 180
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { blinkProgress = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    fun setState(state: State) {
        if (currentState == state) return
        currentState = state
        speakAnim?.cancel()
        thinkAnim?.cancel()
        glowAnim?.cancel()
        eyeLookX = 0f
        mouthOpen = 0f
        smileExtra = 0f
        glowAlpha = 0f

        when (state) {
            State.SPEAKING -> {
                speakAnim = ValueAnimator.ofFloat(0f, 1f, 0.3f, 0.8f, 0f).apply {
                    duration = 500
                    repeatCount = ValueAnimator.INFINITE
                    addUpdateListener { mouthOpen = it.animatedValue as Float; invalidate() }
                    start()
                }
            }
            State.THINKING -> {
                thinkAnim = ValueAnimator.ofFloat(0f, 18f, 0f, -18f, 0f).apply {
                    duration = 2500
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener { eyeLookX = it.animatedValue as Float; invalidate() }
                    start()
                }
            }
            State.HAPPY -> {
                smileExtra = 1.4f
                invalidate()
            }
            State.LISTENING -> {
                glowAnim = ValueAnimator.ofFloat(0f, 255f, 0f).apply {
                    duration = 1000
                    repeatCount = ValueAnimator.INFINITE
                    addUpdateListener {
                        glowAlpha = it.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
            }
            State.IDLE -> invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height * 0.45f
        val r = minOf(width, height) * 0.28f

        canvas.save()
        canvas.scale(breathScale, breathScale, cx, cy)
        canvas.translate(0f, headBobY)

        // Listening glow ring
        if (currentState == State.LISTENING && glowAlpha > 0) {
            glowPaint.alpha = glowAlpha.toInt()
            canvas.drawCircle(cx, cy, r * 1.15f, glowPaint)
        }

        drawBody(canvas, cx, cy, r)
        drawNeck(canvas, cx, cy, r)
        drawBackHair(canvas, cx, cy, r)
        drawEars(canvas, cx, cy, r)
        drawFace(canvas, cx, cy, r)
        drawCheeks(canvas, cx, cy, r)
        drawFrontHair(canvas, cx, cy, r)
        drawEyebrows(canvas, cx, cy, r)
        drawEyes(canvas, cx, cy, r)
        drawNose(canvas, cx, cy, r)
        drawMouth(canvas, cx, cy, r)

        canvas.restore()
    }

    private fun drawBody(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        clothPath.reset()
        val bodyTop = cy + r * 1.1f
        clothPath.moveTo(cx - r * 1.2f, cy + r * 2.5f)
        clothPath.quadTo(cx - r * 1.1f, bodyTop, cx - r * 0.5f, bodyTop)
        clothPath.lineTo(cx + r * 0.5f, bodyTop)
        clothPath.quadTo(cx + r * 1.1f, bodyTop, cx + r * 1.2f, cy + r * 2.5f)
        clothPath.close()
        canvas.drawPath(clothPath, clothPaint)
        // Collar detail
        val collarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E91E63") }
        canvas.drawCircle(cx, bodyTop + r * 0.1f, r * 0.12f, collarPaint)
    }

    private fun drawNeck(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        canvas.drawRoundRect(cx - r * 0.18f, cy + r * 0.9f, cx + r * 0.18f, cy + r * 1.15f, 10f, 10f, neckPaint)
    }

    private fun drawBackHair(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        hairPath.reset()
        hairPath.moveTo(cx - r * 1.0f, cy + r * 0.8f)
        hairPath.quadTo(cx - r * 1.4f, cy, cx - r * 0.95f, cy - r)
        hairPath.quadTo(cx, cy - r * 1.45f, cx + r * 0.95f, cy - r)
        hairPath.quadTo(cx + r * 1.4f, cy, cx + r * 1.0f, cy + r * 0.8f)
        hairPath.quadTo(cx + r * 0.85f, cy + r * 1.3f, cx, cy + r * 1.4f)
        hairPath.quadTo(cx - r * 0.85f, cy + r * 1.3f, cx - r * 1.0f, cy + r * 0.8f)
        hairPath.close()
        canvas.drawPath(hairPath, hairPaint)
    }

    private fun drawEars(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        canvas.drawCircle(cx - r * 1.02f, cy + r * 0.05f, r * 0.18f, skinPaint)
        canvas.drawCircle(cx + r * 1.02f, cy + r * 0.05f, r * 0.18f, skinPaint)
        // Earrings
        val earringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E91E63") }
        canvas.drawCircle(cx - r * 1.02f, cy + r * 0.22f, r * 0.05f, earringPaint)
        canvas.drawCircle(cx + r * 1.02f, cy + r * 0.22f, r * 0.05f, earringPaint)
    }

    private fun drawFace(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        canvas.drawCircle(cx, cy, r, skinPaint)
        canvas.drawCircle(cx, cy, r, outlinePaint)
    }

    private fun drawCheeks(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        canvas.drawCircle(cx - r * 0.58f, cy + r * 0.3f, r * 0.22f, cheekPaint)
        canvas.drawCircle(cx + r * 0.58f, cy + r * 0.3f, r * 0.22f, cheekPaint)
    }

    private fun drawFrontHair(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        bangPath.reset()
        bangPath.moveTo(cx - r * 1.0f, cy - r * 0.15f)
        bangPath.cubicTo(cx - r * 0.95f, cy - r * 1.35f, cx + r * 0.95f, cy - r * 1.35f, cx + r * 1.0f, cy - r * 0.15f)
        bangPath.lineTo(cx + r * 0.75f, cy - r * 0.55f)
        bangPath.quadTo(cx + r * 0.2f, cy - r * 0.42f, cx, cy - r * 0.22f)
        bangPath.quadTo(cx - r * 0.2f, cy - r * 0.42f, cx - r * 0.75f, cy - r * 0.55f)
        bangPath.close()
        canvas.drawPath(bangPath, hairPaint)
    }

    private fun drawEyebrows(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val ey = cy - r * 0.45f
        // Left eyebrow - slightly arched
        canvas.drawLine(cx - r * 0.62f, ey - r * 0.12f, cx - r * 0.22f, ey - r * 0.22f, eyebrowPaint)
        // Right eyebrow
        canvas.drawLine(cx + r * 0.22f, ey - r * 0.22f, cx + r * 0.62f, ey - r * 0.12f, eyebrowPaint)
    }

    private fun drawEyes(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val ey = cy - r * 0.18f
        val eyeW = r * 0.18f
        val eyeH = r * 0.24f * blinkProgress

        // Left eye
        val leftX = cx - r * 0.42f
        val rightX = cx + r * 0.42f

        if (blinkProgress > 0.1f) {
            // Eye whites
            canvas.drawOval(RectF(leftX - eyeW, ey - eyeH, leftX + eyeW, ey + eyeH), eyeWhitePaint)
            canvas.drawOval(RectF(rightX - eyeW, ey - eyeH, rightX + eyeW, ey + eyeH), eyeWhitePaint)

            // Pupils with look direction
            val pupilR = eyeW * 0.55f
            val px = eyeLookX * 0.3f
            canvas.drawCircle(leftX + px, ey, pupilR, eyePupilPaint)
            canvas.drawCircle(rightX + px, ey, pupilR, eyePupilPaint)

            // Eye shine (kawaii highlight)
            val shineR = pupilR * 0.38f
            canvas.drawCircle(leftX + px + pupilR * 0.3f, ey - pupilR * 0.3f, shineR, eyeHighlightPaint)
            canvas.drawCircle(rightX + px + pupilR * 0.3f, ey - pupilR * 0.3f, shineR, eyeHighlightPaint)

            // Eye outline
            val outlineP = Paint(outlinePaint).apply { strokeWidth = 2f; color = Color.parseColor("#3E2723") }
            canvas.drawOval(RectF(leftX - eyeW, ey - eyeH, leftX + eyeW, ey + eyeH), outlineP)
            canvas.drawOval(RectF(rightX - eyeW, ey - eyeH, rightX + eyeW, ey + eyeH), outlineP)
        } else {
            // Blink - draw closed eye lines
            val closedP = Paint(lipPaint).apply { strokeWidth = 5f; color = Color.parseColor("#3E2723") }
            canvas.drawLine(leftX - eyeW, ey, leftX + eyeW, ey, closedP)
            canvas.drawLine(rightX - eyeW, ey, rightX + eyeW, ey, closedP)
        }
    }

    private fun drawNose(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val nosePath = Path().apply {
            moveTo(cx - r * 0.06f, cy + r * 0.08f)
            quadTo(cx + r * 0.1f, cy + r * 0.18f, cx, cy + r * 0.22f)
        }
        val np = Paint(lipPaint).apply { strokeWidth = 3.5f; color = Color.parseColor("#C49A6C") }
        canvas.drawPath(nosePath, np)
    }

    private fun drawMouth(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val my = cy + r * 0.5f
        val mw = r * 0.32f

        if (currentState == State.SPEAKING && mouthOpen > 0) {
            // Open talking mouth
            val oh = r * 0.2f * mouthOpen
            val mouthFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E91E63") }
            canvas.drawOval(RectF(cx - mw * 0.7f, my - oh, cx + mw * 0.7f, my + oh), mouthFillPaint)
        } else {
            // Sweet smile - Mayra style
            mouthPath.reset()
            val smileDepth = r * (0.14f + smileExtra * 0.08f)
            mouthPath.moveTo(cx - mw, my)
            mouthPath.quadTo(cx, my + smileDepth, cx + mw, my)
            canvas.drawPath(mouthPath, lipPaint)
            // Small dimples
            val dimplePaint = Paint(cheekPaint).apply { alpha = 120 }
            canvas.drawCircle(cx - mw - r * 0.05f, my, r * 0.04f, dimplePaint)
            canvas.drawCircle(cx + mw + r * 0.05f, my, r * 0.04f, dimplePaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        breathAnim?.cancel()
        bobAnim?.cancel()
        blinkAnim?.cancel()
        speakAnim?.cancel()
        thinkAnim?.cancel()
        glowAnim?.cancel()
        blinkTimer?.cancel()
    }
}
