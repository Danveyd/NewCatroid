package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Actor

class InstantAction : Action() {
    var action: Action? = null

    override fun act(delta: Float): Boolean {
        val innerAction = action ?: return true

        while (true) {
            val finished = innerAction.act(delta)

            if (UpdateScreenSignal.checkAndResetYield()) {
                return false
            }

            if (finished) {
                return true
            }
        }
    }

    override fun setActor(actor: Actor?) {
        super.setActor(actor)
        action?.actor = actor
    }

    override fun restart() {
        action?.restart()
        super.restart()
    }
}
