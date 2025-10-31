package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.formulaeditor.Formula

class CreateDistanceJointAction : TemporalAction() {
    private var scope: Scope? = null
    private var jointId: Formula? = null
    private var spriteBName: Formula? = null
    private var length: Formula? = null
    private var frequency: Formula? = null
    private var damping: Formula? = null

    override fun update(percent: Float) {
        val id = jointId!!.interpretString(scope)
        if (id == null || id.isEmpty()) return
        val otherSpriteName = spriteBName!!.interpretString(scope)
        if (otherSpriteName == null || otherSpriteName.isEmpty()) return

        val lengthValue = length!!.interpretFloat(scope)
        val frequencyValue = frequency!!.interpretFloat(scope)
        val dampingValue = damping!!.interpretFloat(scope)

        val stage = scope!!.sprite.look.stage ?: return

        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return

        val spriteA = scope!!.sprite
        val spriteB: Sprite = scene.getSprite(otherSpriteName)
            ?: return

        scene.physicsWorld.createDistanceJoint(
            id,
            spriteA,
            spriteB,
            lengthValue,
            frequencyValue,
            dampingValue
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

    fun setLength(length: Formula?) {
        this.length = length
    }

    fun setFrequency(frequency: Formula?) {
        this.frequency = frequency
    }

    fun setDamping(damping: Formula?) {
        this.damping = damping
    }
}