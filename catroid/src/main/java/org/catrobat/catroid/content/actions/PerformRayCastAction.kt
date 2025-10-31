package org.catrobat.catroid.content.actions

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class PerformRayCastAction : TemporalAction() {
    private var scope: Scope? = null
    private var rayId: Formula? = null
    private var startX: Formula? = null
    private var startY: Formula? = null
    private var endX: Formula? = null
    private var endY: Formula? = null

    override fun update(percent: Float) {
        val id = rayId!!.interpretString(scope)
        if (id == null || id.isEmpty()) return

        val sX = startX!!.interpretFloat(scope)
        val sY = startY!!.interpretFloat(scope)
        val eX = endX!!.interpretFloat(scope)
        val eY = endY!!.interpretFloat(scope)

        val scene = ProjectManager.getInstance().currentlyPlayingScene ?: return

        scene.physicsWorld.performRayCast(id, Vector2(sX, sY), Vector2(eX, eY))
    }

    fun setScope(scope: Scope?) {
        this.scope = scope
    }

    fun setRayId(rayId: Formula?) {
        this.rayId = rayId
    }

    fun setStartX(startX: Formula?) {
        this.startX = startX
    }

    fun setStartY(startY: Formula?) {
        this.startY = startY
    }

    fun setEndX(endX: Formula?) {
        this.endX = endX
    }

    fun setEndY(endY: Formula?) {
        this.endY = endY
    }
}