package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class MouseEventAction : TemporalAction() {
    var scope: Scope? = null
    var x: Formula? = null
    var y: Formula? = null
    var buttonState: Formula? = null // 0=Released, 1=Pressed, 2=Move

    override fun update(percent: Float) {
        val xPos = x?.interpretFloat(scope) ?: 0f
        val yPos = y?.interpretFloat(scope) ?: 0f
        val state = buttonState?.interpretInteger(scope) ?: 0

        val stageActivity = StageActivity.activeStageActivity.get()
        stageActivity?.sendVmMouseEvent(xPos, yPos, state)
    }
}