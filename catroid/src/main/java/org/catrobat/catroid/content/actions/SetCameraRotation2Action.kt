package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class SetCameraRotation2Action : TemporalAction() {
    var scope: Scope? = null
    var rotationFormula: Formula? = null

    override fun update(percent: Float) {
        val listener = StageActivity.getActiveStageListener() ?: return
        val rotation = rotationFormula?.interpretFloat(scope) ?: 0f
        listener.setCameraRotation(rotation)
    }
}