package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class ApplyAngularImpulseAction : TemporalAction() {
    private var scope: Scope? = null
    private var impulse: Formula? = null

    override fun update(percent: Float) {
        val impulseValue = impulse!!.interpretFloat(scope)

        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return
        val sprite = scope!!.sprite ?: return

        scene.physicsWorld.applyAngularImpulse(sprite, impulseValue)
    }

    fun setScope(scope: Scope?) {
        this.scope = scope
    }

    fun setImpulse(impulse: Formula?) {
        this.impulse = impulse
    }
}