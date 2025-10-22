package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class SetCameraPosition2Action : TemporalAction() {
    var scope: Scope? = null
    var xFormula: Formula? = null
    var yFormula: Formula? = null

    override fun update(percent: Float) {
        val listener = StageActivity.getActiveStageListener() ?: return
        val x = xFormula?.interpretFloat(scope) ?: 0f
        val y = yFormula?.interpretFloat(scope) ?: 0f
        listener.setCameraPosition(x, y)
    }
}