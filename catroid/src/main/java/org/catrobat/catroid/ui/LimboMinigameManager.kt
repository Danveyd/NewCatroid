package org.catrobat.catroid.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.media.MediaPlayer
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import org.catrobat.catroid.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object LimboMinigameManager {

    var isPlaying = false

    enum class MoveType {
        ROTATE_CW, ROTATE_CCW, SWAP_OPPOSITE, SWAP_ADJACENT, CHAOS
    }

    class Move(val type: MoveType, val durationMs: Long, val steps: Int = 0, val shake: Boolean = false, val isFinal: Boolean = false)

    private val MOVES = listOf(
        Move(MoveType.ROTATE_CW, 300, 2),
        Move(MoveType.ROTATE_CCW, 300, 3),
        Move(MoveType.SWAP_OPPOSITE, 300),
        Move(MoveType.SWAP_ADJACENT, 300),
        Move(MoveType.CHAOS, 300),
        Move(MoveType.ROTATE_CW, 300, 1),
        Move(MoveType.SWAP_OPPOSITE, 300),
        Move(MoveType.ROTATE_CCW, 300, 2),
        Move(MoveType.SWAP_ADJACENT, 300),
        Move(MoveType.CHAOS, 300),

        Move(MoveType.ROTATE_CW, 250, 3),
        Move(MoveType.SWAP_OPPOSITE, 250),
        Move(MoveType.CHAOS, 250),
        Move(MoveType.ROTATE_CCW, 250, 2),
        Move(MoveType.SWAP_ADJACENT, 250),
        Move(MoveType.CHAOS, 250),
        Move(MoveType.ROTATE_CW, 250, 2),
        Move(MoveType.SWAP_OPPOSITE, 250),
        Move(MoveType.SWAP_ADJACENT, 250),
        Move(MoveType.ROTATE_CCW, 250, 1),

        Move(MoveType.SWAP_OPPOSITE, 200),
        Move(MoveType.CHAOS, 200),
        Move(MoveType.ROTATE_CW, 200, 2),
        Move(MoveType.SWAP_ADJACENT, 200),
        Move(MoveType.CHAOS, 200),
        Move(MoveType.ROTATE_CCW, 200, 3),
        Move(MoveType.SWAP_OPPOSITE, 200),
        Move(MoveType.SWAP_ADJACENT, 200),
        Move(MoveType.CHAOS, 200),
        Move(MoveType.ROTATE_CW, 200, 4),

        Move(MoveType.ROTATE_CW, 2000, 24, shake = true, isFinal = true)
    )

    fun findClickableView(view: View, x: Int, y: Int): View? {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        if (x < location[0] || x > location[0] + view.width || y < location[1] || y > location[1] + view.height) return null
        if (view is ViewGroup) {
            for (i in view.childCount - 1 downTo 0) {
                val child = view.getChildAt(i)
                if (child.visibility == View.VISIBLE) {
                    val target = findClickableView(child, x, y)
                    if (target != null) return target
                }
            }
        }
        return if (view.isClickable) view else null
    }

    fun startLimbo(activity: Activity, targetView: View) {
        if (isPlaying) return
        isPlaying = true

        val rawBitmap = Bitmap.createBitmap(targetView.width, targetView.height, Bitmap.Config.ARGB_8888)
        targetView.draw(Canvas(rawBitmap))

        val location = IntArray(2)
        targetView.getLocationOnScreen(location)

        val decorView = activity.window.decorView as ViewGroup
        val overlay = FrameLayout(activity).apply {
            setBackgroundColor(Color.parseColor("#95000000"))
            isClickable = true
            isFocusable = true
        }
        decorView.addView(overlay, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        targetView.visibility = View.INVISIBLE

        val fakeButton = ImageView(activity).apply {
            setImageBitmap(rawBitmap)
            x = location[0].toFloat()
            y = location[1].toFloat()
        }
        overlay.addView(fakeButton, rawBitmap.width, rawBitmap.height)

        val crtSet = AnimatorSet()
        crtSet.playSequentially(
            ObjectAnimator.ofFloat(fakeButton, "scaleY", 1f, 0.05f).setDuration(200),
            ObjectAnimator.ofFloat(fakeButton, "scaleX", 1f, 0f).setDuration(200)
        )
        crtSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                overlay.removeView(fakeButton)
                overlay.post {
                    startLimboMadness(activity, overlay, rawBitmap, targetView)
                }
            }
        })
        crtSet.start()
    }

    private fun startLimboMadness(activity: Activity, overlay: FrameLayout, rawBitmap: Bitmap, originalView: View) {
        val overlayW = overlay.width.toFloat()
        val overlayH = overlay.height.toFloat()

        val maxDim = min(overlayW, overlayH) * 0.18f
        val scaleF = min(1f, maxDim / maxOf(rawBitmap.width, rawBitmap.height).toFloat())
        val scaledW = maxOf(1, (rawBitmap.width * scaleF).toInt())
        val scaledH = maxOf(1, (rawBitmap.height * scaleF).toInt())

        val finalBitmap = if (scaleF < 1f) Bitmap.createScaledBitmap(rawBitmap, scaledW, scaledH, true) else rawBitmap

        val keys = mutableListOf<ImageView>()
        val centerX = overlayW / 2f - finalBitmap.width / 2f
        val centerY = overlayH / 2f - finalBitmap.height / 2f
        val baseRadius = min(overlayW, overlayH) * 0.32f

        val mediaPlayer = try {
            MediaPlayer.create(activity, R.raw.limbo_music)?.apply { start() }
        } catch (e: Exception) { null }

        for (i in 0 until 8) {
            val angle = Math.toRadians(i * 45.0)
            val key = ImageView(activity).apply {
                setImageBitmap(finalBitmap)
                x = centerX + (baseRadius * cos(angle).toFloat())
                y = centerY + (baseRadius * sin(angle).toFloat())
                alpha = 0f
            }
            overlay.addView(key, finalBitmap.width, finalBitmap.height)
            keys.add(key)
            key.animate().alpha(1f).setDuration(400).start()
        }

        keys[0].setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY)

        overlay.postDelayed({
            keys[0].clearColorFilter()
            executeChoreography(activity, overlay, keys, originalView, mediaPlayer, 0, IntArray(8) { it }, centerX, centerY, baseRadius, null)
        }, 1500)
    }

    private fun executeChoreography(
        activity: Activity, overlay: FrameLayout, keys: List<ImageView>,
        originalView: View, mediaPlayer: MediaPlayer?, moveIndex: Int,
        currentSlots: IntArray, centerX: Float, centerY: Float, radius: Float,
        preFlash: View?
    ) {
        if (moveIndex >= MOVES.size) {
            overlay.translationX = 0f
            overlay.translationY = 0f
            triggerFocusEndgame(activity, overlay, keys, originalView, mediaPlayer, preFlash)
            return
        }

        val move = MOVES[moveIndex]
        val animSet = AnimatorSet()
        val anims = mutableListOf<Animator>()
        val nextSlots = IntArray(8)
        var activePreFlash = preFlash

        if (move.shake) {
            val shake = ValueAnimator.ofFloat(0f, 1f).apply {
                addUpdateListener {
                    overlay.translationX = (Math.random() * 50 - 25).toFloat()
                    overlay.translationY = (Math.random() * 50 - 25).toFloat()
                }
            }
            anims.add(shake)
        }

        if (move.isFinal) {
            val zoomAnim = ValueAnimator.ofFloat(0f, 1f).apply {
                addUpdateListener {
                    val f = it.animatedFraction
                    val zoomRadius = radius * (1f + 0.3f * f)
                    val zoomScale = 1f + 0.5f * f

                    val dir = if (move.type == MoveType.ROTATE_CW) 1 else -1
                    for (i in 0 until 8) {
                        val startAngle = currentSlots[i] * 45f
                        val targetAngle = startAngle + (move.steps * 45f * dir)
                        val angle = Math.toRadians((startAngle + (targetAngle - startAngle) * f).toDouble())
                        keys[i].x = centerX + zoomRadius * cos(angle).toFloat()
                        keys[i].y = centerY + zoomRadius * sin(angle).toFloat()
                        keys[i].scaleX = zoomScale
                        keys[i].scaleY = zoomScale
                    }
                }
            }
            anims.add(zoomAnim)

            activePreFlash = View(activity).apply { setBackgroundColor(Color.WHITE); alpha = 0f }
            overlay.addView(activePreFlash, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            activePreFlash.animate().alpha(0.8f).setDuration(move.durationMs).start()
        }

        if (!move.isFinal) {
            when (move.type) {
                MoveType.ROTATE_CW, MoveType.ROTATE_CCW -> {
                    val dir = if (move.type == MoveType.ROTATE_CW) 1 else -1
                    for (i in 0 until 8) {
                        nextSlots[i] = (currentSlots[i] + (move.steps * dir)) % 8
                        if (nextSlots[i] < 0) nextSlots[i] += 8
                    }
                    val rot = ValueAnimator.ofFloat(0f, 1f).apply {
                        addUpdateListener { anim ->
                            val f = anim.animatedFraction
                            for (i in 0 until 8) {
                                val startA = currentSlots[i] * 45f
                                val angle = Math.toRadians((startA + (move.steps * 45f * dir) * f).toDouble())
                                keys[i].x = centerX + radius * cos(angle).toFloat()
                                keys[i].y = centerY + radius * sin(angle).toFloat()
                            }
                        }
                    }
                    anims.add(rot)
                }
                else -> {
                    if (move.type == MoveType.SWAP_OPPOSITE) for (i in 0 until 8) nextSlots[i] = (currentSlots[i] + 4) % 8
                    else if (move.type == MoveType.SWAP_ADJACENT) for (i in 0 until 8) nextSlots[i] = if (currentSlots[i] % 2 == 0) currentSlots[i] + 1 else currentSlots[i] - 1
                    else if (move.type == MoveType.CHAOS) {
                        val shuf = (0..7).toMutableList().shuffled()
                        for (i in 0 until 8) nextSlots[i] = shuf[i]
                    }
                    for (i in 0 until 8) {
                        val ang = Math.toRadians(nextSlots[i] * 45.0)
                        anims.add(ObjectAnimator.ofFloat(keys[i], "x", keys[i].x, centerX + radius * cos(ang).toFloat()))
                        anims.add(ObjectAnimator.ofFloat(keys[i], "y", keys[i].y, centerY + radius * sin(ang).toFloat()))
                    }
                }
            }
        } else {
            val dir = if (move.type == MoveType.ROTATE_CW) 1 else -1
            for (i in 0 until 8) {
                nextSlots[i] = (currentSlots[i] + (move.steps * dir)) % 8
                if (nextSlots[i] < 0) nextSlots[i] += 8
            }
        }

        animSet.playTogether(anims)
        animSet.duration = move.durationMs
        animSet.interpolator = if (move.isFinal) AccelerateInterpolator() else AccelerateDecelerateInterpolator()
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                for (i in 0..7) currentSlots[i] = nextSlots[i]
                executeChoreography(activity, overlay, keys, originalView, mediaPlayer, moveIndex + 1, currentSlots, centerX, centerY, radius, activePreFlash)
            }
        })
        animSet.start()
    }

    private fun triggerFocusEndgame(activity: Activity, overlay: FrameLayout, keys: List<ImageView>, originalView: View, mediaPlayer: MediaPlayer?, preFlash: View?) {
        if (preFlash != null) {
            preFlash.alpha = 1f
            preFlash.animate().alpha(0f).setDuration(800).withEndAction { overlay.removeView(preFlash) }.start()
        }

        val colors = listOf(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.parseColor("#FFA500"), Color.WHITE).shuffled()
        var floatAnim: ValueAnimator? = null

        for (i in keys.indices) {
            keys[i].setColorFilter(colors[i], PorterDuff.Mode.MULTIPLY)
            keys[i].isClickable = true

            keys[i].setOnClickListener {
                if (!isPlaying) return@setOnClickListener

                floatAnim?.cancel()
                for (k in keys) k.isClickable = false

                mediaPlayer?.apply { stop(); release() }

                if (keys[i] == keys[0]) Toast.makeText(activity, activity.getString(R.string.lmm_focus_success), Toast.LENGTH_SHORT).show()

                val postClickFlash = View(activity).apply {
                    setBackgroundColor(Color.WHITE)
                    alpha = 0f
                }
                overlay.addView(postClickFlash, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

                val isSuccess = (keys[i] == keys[0])

                postClickFlash.animate().alpha(1f).setDuration(150).withEndAction {
                    postClickFlash.setBackgroundColor(Color.BLACK)
                    for (k in keys) k.visibility = View.GONE

                    postClickFlash.animate().alpha(0f).setDuration(1500).setInterpolator(DecelerateInterpolator()).withEndAction {
                        (activity.window.decorView as ViewGroup).removeView(overlay)
                        originalView.visibility = View.VISIBLE
                        isPlaying = false

                        if (isSuccess) {
                            originalView.performClick()
                        }
                    }.start()
                }.start()
            }
        }

        val baseYs = FloatArray(keys.size)
        for (i in keys.indices) {
            baseYs[i] = keys[i].y
        }

        floatAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 8000
            addUpdateListener { anim ->
                val f = anim.animatedFraction
                val amp = 40f * (1f - f)
                for (j in keys.indices) {
                    keys[j].y = baseYs[j] + (sin(anim.currentPlayTime / 200.0 + j) * amp).toFloat()
                }
            }
            start()
        }
    }
}