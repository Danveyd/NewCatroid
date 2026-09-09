package org.catrobat.catroid.ai.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.Gravity
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView

class KoveThinkingView(context: Context) : FrameLayout(context) {

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * resources.displayMetrics.density
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#001A35")
        alpha = 190
    }

    private val cornerRadius = 12f * resources.displayMetrics.density
    private var pulseAlpha = 80
    private var pulseAnimator: ValueAnimator? = null

    init {
        setWillNotDraw(false)
        val density = resources.displayMetrics.density

        setPadding(
            (16 * density).toInt(),
            (4 * density).toInt(),
            (16 * density).toInt(),
            (4 * density).toInt()
        )

        val textView = TextView(context).apply {
            text = "✦  Kove генерирует..."
            textSize = 11.5f
            setTextColor(Color.parseColor("#A8DFF4"))
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (26 * density).toInt())
        }
        addView(textView)

        alpha = 0f
        scaleX = 0.85f
        scaleY = 0.85f
        animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(240)
            .setInterpolator(OvershootInterpolator(1.3f))
            .start()

        pulseAnimator = ValueAnimator.ofInt(40, 240).apply {
            duration = 900
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { va ->
                pulseAlpha = va.animatedValue as Int
                invalidate()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        pulseAnimator?.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        val inset = glowPaint.strokeWidth / 2
        val rect = RectF(inset, inset, width - inset, height - inset)

        glowPaint.color = Color.argb(pulseAlpha, 255, 255, 255)

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, glowPaint)

        super.onDraw(canvas)
    }
}
