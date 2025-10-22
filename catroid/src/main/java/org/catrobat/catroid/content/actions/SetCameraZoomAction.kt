package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class SetCameraZoomAction : TemporalAction() {
    var scope: Scope? = null
    var zoomFormula: Formula? = null

    override fun update(percent: Float) {
        val listener = StageActivity.getActiveStageListener() ?: return
        val zoom = zoomFormula?.interpretFloat(scope) ?: 1f
        listener.setCameraZoom(zoom)
    }
}