package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.content.Scope

class UpdateScreenAction : Action() {
    var scope: Scope? = null
    private var yielded = false

    override fun act(delta: Float): Boolean {
        if (!yielded) {
            yielded = true
            UpdateScreenSignal.requestYield()
            return false
        }
        yielded = false
        return true
    }

    override fun restart() {
        yielded = false
        super.restart()
    }
}
