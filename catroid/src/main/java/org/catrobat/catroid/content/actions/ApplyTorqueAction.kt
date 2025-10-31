package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class ApplyTorqueAction : TemporalAction() {
    private var scope: Scope? = null
    private var torque: Formula? = null

    override fun update(percent: Float) {
        val torqueValue = torque!!.interpretFloat(scope)

        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return
        val sprite = scope!!.sprite ?: return

        scene.physicsWorld.applyTorque(sprite, torqueValue)
    }

    fun setScope(scope: Scope?) {
        this.scope = scope
    }

    fun setTorque(torque: Formula?) {
        this.torque = torque
    }
}