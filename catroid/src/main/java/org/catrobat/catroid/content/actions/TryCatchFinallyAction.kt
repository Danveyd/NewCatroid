package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.formulaeditor.UserVariable
import android.util.Log

class TryCatchFinallyAction : Action() {
    var tryAction: Action? = null
    var catchAction: Action? = null
    var finallyAction: Action? = null
    var errorVariable: UserVariable? = null

    private var tryCompleted = false
    private var catchCompleted = false
    private var finallyCompleted = false
    private var errorOccurred = false

    override fun act(delta: Float): Boolean {
        if (!tryCompleted) {
            try {
                if (tryAction?.act(delta) == true) {
                    tryCompleted = true
                }
            } catch (e: Exception) {
                Log.w("TryCatchFinallyAction", "Exception caught in TRY block: ${e.message}")
                errorOccurred = true
                tryCompleted = true
                errorVariable?.value = e.message ?: "An unknown error occurred"
            }
        }

        if (tryCompleted && errorOccurred && !catchCompleted) {
            if (catchAction?.act(delta) == true) {
                catchCompleted = true
            }
        }

        val shouldRunFinally = (tryCompleted && !errorOccurred && !finallyCompleted) || (catchCompleted && !finallyCompleted)
        if (shouldRunFinally) {
            if (finallyAction?.act(delta) == true) {
                finallyCompleted = true
            }
        }

        return finallyCompleted || (tryCompleted && !errorOccurred && finallyAction == null) || (catchCompleted && finallyAction == null)
    }

    override fun restart() {
        tryCompleted = false
        catchCompleted = false
        finallyCompleted = false
        errorOccurred = false

        tryAction?.restart()
        catchAction?.restart()
        finallyAction?.restart()

        super.restart()
    }
}