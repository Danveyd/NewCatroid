package org.catrobat.catroid.content.actions

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.formulaeditor.Formula

class CreatePrismaticJointAction : TemporalAction() {
    private var scope: Scope? = null
    private var jointId: Formula? = null
    private var spriteBName: Formula? = null
    private var anchorX: Formula? = null
    private var anchorY: Formula? = null
    private var axisX: Formula? = null
    private var axisY: Formula? = null

    override fun update(percent: Float) {
        val id = jointId!!.interpretString(scope)
        if (id == null || id.isEmpty()) return
        val otherSpriteName = spriteBName!!.interpretString(scope)
        if (otherSpriteName == null || otherSpriteName.isEmpty()) return

        val ax = anchorX!!.interpretFloat(scope)
        val ay = anchorY!!.interpretFloat(scope)
        val axisXVal = axisX!!.interpretFloat(scope)
        val axisYVal = axisY!!.interpretFloat(scope)

        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return
        val spriteA = scope!!.sprite ?: return
        val spriteB: Sprite = scene.getSpriteAll(otherSpriteName)
            ?: return

        scene.physicsWorld.createPrismaticJoint(
            id,
            spriteA,
            spriteB,
            Vector2(ax, ay),
            Vector2(axisXVal, axisYVal)
        )
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

    fun setAxisX(axisX: Formula?) {
        this.axisX = axisX
    }

    fun setAxisY(axisY: Formula?) {
        this.axisY = axisY
    }
}