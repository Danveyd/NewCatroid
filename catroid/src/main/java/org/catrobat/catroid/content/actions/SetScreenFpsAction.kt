package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.stage.StageActivity

class SetScreenFpsAction : TemporalAction() {
    var fps: Int = 60

    override fun update(percent: Float) {
        StageActivity.getActiveStageListener()?.setTargetFps(fps)
    }
}
