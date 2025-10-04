package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.stage.StageActivity

class StopVMAction : TemporalAction() {
    var scope: Scope? = null

    override fun update(percent: Float) {
        val stageActivity = StageActivity.activeStageActivity.get()
        stageActivity?.stopVM()
    }
}