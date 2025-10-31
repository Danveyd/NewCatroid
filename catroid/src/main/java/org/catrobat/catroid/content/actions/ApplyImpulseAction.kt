package org.catrobat.catroid.content.actions

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class ApplyImpulseAction : TemporalAction() {
    private var scope: Scope? = null
    private var impulseX: Formula? = null
    private var impulseY: Formula? = null

    override fun update(percent: Float) {
        val x = impulseX!!.interpretFloat(scope)
        val y = impulseY!!.interpretFloat(scope)

        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return
        val sprite = scope!!.sprite ?: return

        scene.physicsWorld.applyImpulse(sprite, Vector2(x, y), sprite.look.position)
    }

    fun setScope(scope: Scope?) {
        this.scope = scope
    }

    fun setImpulseX(impulseX: Formula?) {
        this.impulseX = impulseX
    }

    fun setImpulseY(impulseY: Formula?) {
        this.impulseY = impulseY
    }
}