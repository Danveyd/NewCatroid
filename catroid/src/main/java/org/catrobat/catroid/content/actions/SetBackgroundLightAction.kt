package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class SetBackgroundLightAction : TemporalAction() {
    var scope: Scope? = null
    var intensity: Formula? = null

    override fun update(percent: Float) {
        val threeDManager = StageActivity.activeStageActivity.get()?.stageListener?.threeDManager ?: return
        try {
            val i = intensity?.interpretFloat(scope) ?: 1.0f
            threeDManager.setBackgroundLightIntensity(i)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}