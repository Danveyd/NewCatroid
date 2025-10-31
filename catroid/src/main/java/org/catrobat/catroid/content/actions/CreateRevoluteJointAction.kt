package org.catrobat.catroid.content.actions

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class CreateRevoluteJointAction : TemporalAction() {
    var scope: Scope? = null
    var jointId: Formula? = null
    var spriteBName: Formula? = null
    var anchorX: Formula? = null
    var anchorY: Formula? = null

    override fun update(percent: Float) {
        val id = jointId?.interpretString(scope) ?: return
        val otherSpriteName = spriteBName?.interpretString(scope) ?: return
        val x = anchorX?.interpretFloat(scope) ?: 0f
        val y = anchorY?.interpretFloat(scope) ?: 0f

        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return
        val spriteA = scope?.sprite ?: return
        val spriteB = scene.getSprite(otherSpriteName) ?: return

        scene.physicsWorld.createRevoluteJoint(id, spriteA, spriteB, Vector2(x, y))
    }
}