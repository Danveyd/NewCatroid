package org.catrobat.catroid.ai.ui

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import org.catrobat.catroid.R
import org.catrobat.catroid.content.bricks.Brick

class KoveGlassCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val accentColor = Color.parseColor("#A8DFF4")
    private val glowBaseColor = Color.parseColor("#33B5E5")

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * resources.displayMetrics.density
        color = accentColor
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f * resources.displayMetrics.density
        color = glowBaseColor
        alpha = 60
    }

    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#002B4D")
        alpha = 225
    }

    private val cornerRadius = 14f * resources.displayMetrics.density
    private val bricksContainer: LinearLayout
    private val btnRegenerate: ImageView

    private var breathAnimator: ValueAnimator? = null
    private var idlePulseAnimator: ObjectAnimator? = null
    private val idleHandler = Handler(Looper.getMainLooper())

    var onRegenerateClicked: (() -> Unit)? = null

    private val startIdlePulseRunnable = Runnable {
        startRegenerateButtonPulse()
    }

    init {
        setWillNotDraw(false)
        val view = LayoutInflater.from(context).inflate(R.layout.view_kove_glass_card, this, true)
        bricksContainer = view.findViewById(R.id.kove_bricks_container)
        btnRegenerate = view.findViewById(R.id.kove_btn_regenerate)

        btnRegenerate.setImageResource(R.drawable.ic_refresh_arrow)

        btnRegenerate.setOnClickListener {
            stopRegenerateButtonPulse()
            btnRegenerate.animate()
                .rotationBy(360f)
                .scaleX(1.25f)
                .scaleY(1.25f)
                .setDuration(350)
                .withEndAction {
                    btnRegenerate.scaleX = 1f
                    btnRegenerate.scaleY = 1f
                    resetIdleTimer()
                }
                .start()
            onRegenerateClicked?.invoke()
        }

        alpha = 0f
        scaleX = 0.92f
        scaleY = 0.92f
        animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(280)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        breathAnimator = ValueAnimator.ofInt(40, 110).apply {
            duration = 1400
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { va ->
                glowPaint.alpha = va.animatedValue as Int
                invalidate()
            }
        }

        resetIdleTimer()
    }

    private fun resetIdleTimer() {
        stopRegenerateButtonPulse()
        idleHandler.removeCallbacks(startIdlePulseRunnable)
        idleHandler.postDelayed(startIdlePulseRunnable, 3500L)
    }

    private fun startRegenerateButtonPulse() {
        if (idlePulseAnimator?.isRunning == true) return

        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.22f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.22f)
        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.7f, 1.0f)

        idlePulseAnimator = ObjectAnimator.ofPropertyValuesHolder(btnRegenerate, scaleX, scaleY, alpha).apply {
            duration = 800
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    fun stopRegenerateButtonPulse() {
        idleHandler.removeCallbacks(startIdlePulseRunnable)
        idlePulseAnimator?.cancel()
        btnRegenerate.scaleX = 1.0f
        btnRegenerate.scaleY = 1.0f
        btnRegenerate.alpha = 1.0f
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        breathAnimator?.start()
        resetIdleTimer()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        breathAnimator?.cancel()
        stopRegenerateButtonPulse()
    }

    override fun onDraw(canvas: Canvas) {
        val inset = glowPaint.strokeWidth / 2
        val rect = RectF(inset, inset, width - inset, height - inset)

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, glassPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, glowPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        super.onDraw(canvas)
    }

    fun bindBricks(bricks: List<Brick>) {
        bricksContainer.removeAllViews()
        for (brick in bricks) {
            val brickView = brick.getView(context)
            brick.disableSpinners()
            brickView.alpha = 0.92f
            disableTouchRecursive(brickView)
            bricksContainer.addView(brickView)
        }
    }

    private fun disableTouchRecursive(view: View) {
        view.isClickable = false
        view.isFocusable = false
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                disableTouchRecursive(view.getChildAt(i))
            }
        }
    }
}
