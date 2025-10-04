package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class ToggleDisplayAction : TemporalAction() {
    var scope: Scope? = null
    var visible: Formula? = null

    override fun update(percent: Float) {
        val isVisible = visible?.interpretBoolean(scope) ?: true
        val stageActivity = StageActivity.activeStageActivity.get()
        stageActivity?.setVmDisplayVisible(isVisible)
    }
}