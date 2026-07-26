package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class Apply3dTorqueAction() : TemporalAction() {
    var scope: Scope? = null
    var objectId: Formula? = null
    var x: Formula? = null
    var y: Formula? = null
    var z: Formula? = null

    override fun update(percent: Float) {
        val threeDManager = StageActivity.getActiveStageListener()?.threeDManager ?: return

        try {
            val id = objectId?.interpretString(scope) ?: return
            if (id.isEmpty()) return

            val xVal = x?.interpretFloat(scope) ?: 0f
            val yVal = y?.interpretFloat(scope) ?: 0f
            val zVal = z?.interpretFloat(scope) ?: 0f

            threeDManager.applyTorque(id, xVal, yVal, zVal)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
