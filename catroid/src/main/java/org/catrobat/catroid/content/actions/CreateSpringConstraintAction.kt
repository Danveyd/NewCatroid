package org.catrobat.catroid.content.actions

import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class CreateSpringConstraintAction() : TemporalAction() {
    var scope: Scope? = null
    var constraintId: Formula? = null
    var objectIdA: Formula? = null
    var objectIdB: Formula? = null

    var pivotAx: Formula? = null; var pivotAy: Formula? = null; var pivotAz: Formula? = null
    var pivotBx: Formula? = null; var pivotBy: Formula? = null; var pivotBz: Formula? = null

    var springX: Boolean = false
    var springY: Boolean = true
    var springZ: Boolean = false

    var minX: Formula? = null; var minY: Formula? = null; var minZ: Formula? = null
    var maxX: Formula? = null; var maxY: Formula? = null; var maxZ: Formula? = null

    var stiffness: Formula? = null
    var damping: Formula? = null

    override fun update(percent: Float) {
        val engine = StageActivity.getActiveStageListener()?.threeDManager ?: return

        val id = constraintId?.interpretString(scope) ?: return
        val idA = objectIdA?.interpretString(scope) ?: return
        val idB = objectIdB?.interpretString(scope) ?: ""

        if (id.isEmpty() || idA.isEmpty() || idB.isEmpty()) return

        val pA = Vector3(
            pivotAx?.interpretFloat(scope) ?: 0f,
            pivotAy?.interpretFloat(scope) ?: 0f,
            pivotAz?.interpretFloat(scope) ?: 0f
        )

        val pB = Vector3(
            pivotBx?.interpretFloat(scope) ?: 0f,
            pivotBy?.interpretFloat(scope) ?: 0f,
            pivotBz?.interpretFloat(scope) ?: 0f
        )

        val minLim = Vector3(
            minX?.interpretFloat(scope) ?: 0f,
            minY?.interpretFloat(scope) ?: -0.5f,
            minZ?.interpretFloat(scope) ?: 0f
        )

        val maxLim = Vector3(
            maxX?.interpretFloat(scope) ?: 0f,
            maxY?.interpretFloat(scope) ?: 0.5f,
            maxZ?.interpretFloat(scope) ?: 0f
        )

        val st = stiffness?.interpretFloat(scope) ?: 15000f
        val damp = damping?.interpretFloat(scope) ?: 300f

        engine.createConfigurableSpringConstraint(
            id, idA, idB, pA, pB,
            springX, springY, springZ,
            minLim, maxLim, st, damp
        )
    }
}
