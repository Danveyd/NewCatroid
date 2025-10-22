package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import android.util.Log

class IntervalRepeatAction : Action() {
    var scope: Scope? = null
    var repeatCount: Formula? = null
    var interval: Formula? = null
    var loopBodyAction: Action? = null

    private var isInitialized = false
    private var executedCount = 0
    private var repeatCountValue = 0
    private var intervalValue = 0f
    private var timer = 0f

    override fun act(delta: Float): Boolean {
        if (!isInitialized) {
            initialize()
        }
        if (repeatCountValue > 0 && executedCount >= repeatCountValue) {
            return true
        }

        timer += delta
        if (timer >= intervalValue) {
            if (repeatCountValue > 0) {
                executedCount++
            }

            timer -= intervalValue

            val actionClone = (loopBodyAction as? ScriptSequenceAction)?.clone()
            if (actionClone != null) {
                actionClone.restart()
                actor.addAction(actionClone)
            }
        }

        return repeatCountValue > 0 && executedCount >= repeatCountValue
    }

    private fun initialize() {
        isInitialized = true
        executedCount = 0
        timer = 0f

        repeatCountValue = try {
            repeatCount?.interpretInteger(scope) ?: 0
        } catch (e: Exception) {
            Log.d(javaClass.simpleName, "Repeat count interpretation failed.", e)
            0
        }

        intervalValue = try {
            interval?.interpretFloat(scope)?.takeIf { it >= 0 } ?: 0f
        } catch (e: Exception) {
            Log.d(javaClass.simpleName, "Interval interpretation failed.", e)
            0f
        }
    }

    override fun restart() {
        isInitialized = false
        executedCount = 0
        timer = 0f
        loopBodyAction?.restart()
        super.restart()
    }
}