package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class SetThirdPersonCameraAction : TemporalAction() {
    var scope: Scope? = null
    var objectId: Formula? = null
    var distance: Formula? = null
    var height: Formula? = null
    var pitch: Formula? = null

    override fun update(percent: Float) {
        val threeDManager = StageActivity.getActiveStageListener().threeDManager ?: return

        val id = objectId?.interpretString(scope) ?: return
        val dist = distance?.interpretDouble(scope)?.toFloat() ?: 10.0f
        val h = height?.interpretDouble(scope)?.toFloat() ?: 2.0f
        val p = pitch?.interpretDouble(scope)?.toFloat() ?: 20.0f

        threeDManager.setThirdPersonCamera(id, dist, h, p)
    }
}