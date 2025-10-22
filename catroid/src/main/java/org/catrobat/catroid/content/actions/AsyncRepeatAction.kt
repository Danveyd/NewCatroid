package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import android.util.Log

class AsyncRepeatAction : Action() {
    var scope: Scope? = null
    var repeatCount: Formula? = null
    var loopBodyAction: Action? = null
    private var isInitialized = false

    override fun act(delta: Float): Boolean {
        if (isInitialized) {
            return true
        }
        isInitialized = true

        val repeatCountValue = try {
            repeatCount?.interpretInteger(scope) ?: 0
        } catch (e: Exception) {
            Log.d(javaClass.simpleName, "Formula interpretation failed.", e)
            0
        }

        if (repeatCountValue <= 0 || loopBodyAction == null) {
            return true
        }

        val originalSequence = loopBodyAction as? ScriptSequenceAction
        if (originalSequence == null) {
            Log.e("AsyncRepeatAction", "Loop body is not a ScriptSequenceAction, cannot execute.")
            return true
        }

        for (i in 1..repeatCountValue) {
            val instantClone = originalSequence.deepCloneAndMakeInstant()
            instantClone.actor = actor

            var safetyCounter = 0
            while (!instantClone.act(delta) && safetyCounter < 1000) {
                safetyCounter++
            }
            if (safetyCounter >= 1000) {
                Log.w("AsyncRepeatAction", "Execution stopped to prevent infinite loop.")
            }
        }

        return true
    }

    override fun restart() {
        isInitialized = false
        super.restart()
    }
}