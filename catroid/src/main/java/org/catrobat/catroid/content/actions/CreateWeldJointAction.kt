package org.catrobat.catroid.content.actions

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.formulaeditor.Formula

class CreateWeldJointAction : TemporalAction() {
    private var scope: Scope? = null
    private var jointId: Formula? = null
    private var spriteBName: Formula? = null
    private var anchorX: Formula? = null
    private var anchorY: Formula? = null

    override fun update(percent: Float) {
        val id = jointId!!.interpretString(scope)
        if (id == null || id.isEmpty()) return
        val otherSpriteName = spriteBName!!.interpretString(scope)
        if (otherSpriteName == null || otherSpriteName.isEmpty()) return

        val x = anchorX!!.interpretFloat(scope)
        val y = anchorY!!.interpretFloat(scope)

        val stage = scope!!.sprite.look.stage ?: return

        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return

        val spriteA = scope!!.sprite
        val spriteB: Sprite = scene.getSprite(otherSpriteName)
            ?: return

        scene.physicsWorld.createWeldJoint(id, spriteA, spriteB, Vector2(x, y))
    }

    fun setScope(scope: Scope?) {
        this.scope = scope
    }

    fun setJointId(jointId: Formula?) {
        this.jointId = jointId
    }

    fun setSpriteBName(spriteBName: Formula?) {
        this.spriteBName = spriteBName
    }

    fun setAnchorX(anchorX: Formula?) {
        this.anchorX = anchorX
    }

    fun setAnchorY(anchorY: Formula?) {
        this.anchorY = anchorY
    }
}