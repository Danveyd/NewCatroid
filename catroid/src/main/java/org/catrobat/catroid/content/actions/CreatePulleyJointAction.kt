package org.catrobat.catroid.content.actions

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.formulaeditor.Formula

class CreatePulleyJointAction : TemporalAction() {
    private var scope: Scope? = null
    private var jointId: Formula? = null
    private var spriteAName: Formula? = null
    private var spriteBName: Formula? = null
    private var groundAnchorAx: Formula? = null
    private var groundAnchorAy: Formula? = null
    private var groundAnchorBx: Formula? = null
    private var groundAnchorBy: Formula? = null
    private var ratio: Formula? = null

    override fun update(percent: Float) {
        val id = jointId!!.interpretString(scope)
        val nameA = spriteAName!!.interpretString(scope)
        val nameB = spriteBName!!.interpretString(scope)
        if (id == null || id.isEmpty() || nameA == null || nameB == null) return

        val gAx = groundAnchorAx!!.interpretFloat(scope)
        val gAy = groundAnchorAy!!.interpretFloat(scope)
        val gBx = groundAnchorBx!!.interpretFloat(scope)
        val gBy = groundAnchorBy!!.interpretFloat(scope)
        val r = ratio!!.interpretFloat(scope)

        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return

        val spriteA: Sprite = scene.getSpriteAll(nameA)
        val spriteB: Sprite = scene.getSpriteAll(nameB)

        scene.physicsWorld.createPulleyJoint(
            id,
            spriteA,
            spriteB,
            Vector2(gAx, gAy),
            Vector2(gBx, gBy),
            r
        )
    }

    fun setScope(scope: Scope?) {
        this.scope = scope
    }

    fun setJointId(jointId: Formula?) {
        this.jointId = jointId
    }

    fun setSpriteAName(spriteAName: Formula?) {
        this.spriteAName = spriteAName
    }

    fun setSpriteBName(spriteBName: Formula?) {
        this.spriteBName = spriteBName
    }

    fun setGroundAnchorAx(groundAnchorAx: Formula?) {
        this.groundAnchorAx = groundAnchorAx
    }

    fun setGroundAnchorAy(groundAnchorAy: Formula?) {
        this.groundAnchorAy = groundAnchorAy
    }

    fun setGroundAnchorBx(groundAnchorBx: Formula?) {
        this.groundAnchorBx = groundAnchorBx
    }

    fun setGroundAnchorBy(groundAnchorBy: Formula?) {
        this.groundAnchorBy = groundAnchorBy
    }

    fun setRatio(ratio: Formula?) {
        this.ratio = ratio
    }
}