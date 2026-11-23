package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.stage.StageActivity

class ScreenSystemUiAction : TemporalAction() {
    var show: Boolean = false
    var target: Int = TARGET_ALL

    override fun update(percent: Float) {
        StageActivity.activeStageActivity?.get()?.applySystemUiVisibility(show, target)
    }

    companion object {
        const val TARGET_STATUS_BAR = 0
        const val TARGET_NAVIGATION_BAR = 1
        const val TARGET_ALL = 2
    }
}
