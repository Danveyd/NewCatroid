package org.catrobat.catroid.ai.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import org.catrobat.catroid.R
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

class KoveSwipeContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface SwipeListener {
        fun onAccepted()
        fun onDismissed()
        fun onSwipeProgress(progress: Float)
    }

    var swipeListener: SwipeListener? = null

    private var initialDownX = 0f
    private var initialDownY = 0f
    private var isHorizontalSwipe = false
    private var hasTriggeredSnapHaptic = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val greenColor = Color.parseColor("#10B981")
    private val greenGlassColor = Color.argb(65, 16, 185, 129)

    private val redColor = Color.parseColor("#EF4444")
    private val redGlassColor = Color.argb(65, 239, 68, 68)

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
    }

    private val checkIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_check_white)
    private val deleteIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_trash_white)

    private val cornerRadius = 16f * resources.displayMetrics.density

    init {
        setWillNotDraw(false)
        val density = resources.displayMetrics.density
        setPadding(0, (6 * density).toInt(), 0, (6 * density).toInt())
    }

    private fun getSnapThreshold(): Float {
        val density = resources.displayMetrics.density
        return min(width * 0.28f, 140f * density)
    }

    private fun calculateOneUiDrag(deltaX: Float): Float {
        val density = resources.displayMetrics.density
        val stickZone = 35f * density
        val absDelta = abs(deltaX)

        val translation = if (absDelta < stickZone) {
            absDelta * 0.18f
        } else {
            (stickZone * 0.18f) + (absDelta - stickZone) * 1.12f
        }
        return sign(deltaX) * translation
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialDownX = ev.x
                initialDownY = ev.y
                isHorizontalSwipe = false
                hasTriggeredSnapHaptic = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - initialDownX
                val dy = ev.y - initialDownY

                if (abs(dy) > touchSlop && abs(dy) > abs(dx)) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return false
                }

                if (!isHorizontalSwipe && abs(dx) > touchSlop && abs(dx) > abs(dy) * 1.3f) {
                    isHorizontalSwipe = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isHorizontalSwipe = false
            }
        }
        return isHorizontalSwipe
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val targetCard = getChildAt(0) ?: return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val totalDx = event.x - initialDownX
                val dampedX = calculateOneUiDrag(totalDx)
                targetCard.translationX = dampedX
                targetCard.rotation = (dampedX / width) * 5.5f

                val threshold = getSnapThreshold()

                if (abs(dampedX) >= threshold && !hasTriggeredSnapHaptic) {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    hasTriggeredSnapHaptic = true
                } else if (abs(dampedX) < threshold) {
                    hasTriggeredSnapHaptic = false
                }

                val progress = (dampedX / threshold).coerceIn(-1f, 1f)
                swipeListener?.onSwipeProgress(progress)
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val currentX = targetCard.translationX
                val threshold = getSnapThreshold()

                when {
                    currentX > threshold -> {
                        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        animateFlyOffAndCollapse(targetCard, width.toFloat()) {
                            swipeListener?.onAccepted()
                        }
                    }
                    currentX < -threshold -> {
                        performHapticFeedback(HapticFeedbackConstants.REJECT)
                        animateFlyOffAndCollapse(targetCard, -width.toFloat()) {
                            swipeListener?.onDismissed()
                        }
                    }
                    else -> {
                        animateSpringBack(targetCard)
                    }
                }
                isHorizontalSwipe = false
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun animateSpringBack(target: View) {
        val startX = target.translationX
        val startRot = target.rotation
        val animator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 320
            interpolator = OvershootInterpolator(1.35f)
            addUpdateListener { va ->
                val f = va.animatedValue as Float
                target.translationX = startX * f
                target.rotation = startRot * f
                invalidate()
            }
        }
        animator.start()
    }

    private fun animateFlyOffAndCollapse(target: View, toX: Float, onEnd: () -> Unit) {
        val startX = target.translationX
        val flyAnimator = ValueAnimator.ofFloat(startX, toX).apply {
            duration = 170
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                target.translationX = va.animatedValue as Float
                invalidate()
            }
        }

        flyAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val initialHeight = height
                val collapseAnim = ValueAnimator.ofInt(initialHeight, 0).apply {
                    duration = 160
                    interpolator = AccelerateInterpolator()
                    addUpdateListener { va ->
                        layoutParams.height = va.animatedValue as Int
                        requestLayout()
                    }
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(anim: Animator) {
                            onEnd()
                        }
                    })
                }
                collapseAnim.start()
            }
        })
        flyAnimator.start()
    }

    override fun onDraw(canvas: Canvas) {
        val targetCard = getChildAt(0) ?: return
        val transX = targetCard.translationX
        if (transX == 0f) return

        val isAccept = transX > 0
        bgPaint.color = if (isAccept) greenGlassColor else redGlassColor
        borderPaint.color = if (isAccept) greenColor else redColor

        val rect = if (isAccept) {
            RectF(0f, targetCard.top.toFloat(), transX, targetCard.bottom.toFloat())
        } else {
            RectF(width + transX, targetCard.top.toFloat(), width.toFloat(), targetCard.bottom.toFloat())
        }

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        val icon = if (isAccept) checkIcon else deleteIcon
        if (icon != null) {
            val progress = (abs(transX) / getSnapThreshold()).coerceIn(0f, 1f)
            val iconSize = (28 * resources.displayMetrics.density * progress).toInt()
            val centerY = (targetCard.top + targetCard.bottom) / 2
            val centerX = if (isAccept) (transX / 2).toInt() else (width + transX / 2).toInt()

            icon.setBounds(
                centerX - iconSize / 2,
                centerY - iconSize / 2,
                centerX + iconSize / 2,
                centerY + iconSize / 2
            )
            icon.colorFilter = PorterDuffColorFilter(if (isAccept) greenColor else redColor, PorterDuff.Mode.SRC_IN)
            icon.alpha = (progress * 255).toInt()
            icon.draw(canvas)
        }
    }
}
