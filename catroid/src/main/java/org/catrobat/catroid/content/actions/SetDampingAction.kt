package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class SetDampingAction : TemporalAction() {
    private var scope: Scope? = null
    private var linearDamping: Formula? = null
    private var angularDamping: Formula? = null

    override fun update(percent: Float) {
        val linear = linearDamping!!.interpretFloat(scope)
        val angular = angularDamping!!.interpretFloat(scope)

        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return
        val sprite = scope!!.sprite ?: return

        val physicsObject = scene.physicsWorld.getPhysicsObject(sprite)
        physicsObject.setLinearDamping(linear)
        physicsObject.setAngularDamping(angular)
    }

    fun setScope(scope: Scope?) {
        this.scope = scope
    }

    fun setLinearDamping(linear: Formula?) {
        this.linearDamping = linear
    }

    fun setAngularDamping(angular: Formula?) {
        this.angularDamping = angular
    }
}