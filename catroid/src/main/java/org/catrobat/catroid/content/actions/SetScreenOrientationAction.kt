package org.catrobat.catroid.content.actions

import android.content.pm.ActivityInfo
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.stage.StageActivity

class SetScreenOrientationAction : TemporalAction() {
    var orientation: Int = ORIENTATION_LANDSCAPE

    override fun update(percent: Float) {
        val activity = StageActivity.activeStageActivity?.get() ?: return
        activity.requestedOrientation = when (orientation) {
            ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            ORIENTATION_REVERSE_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            ORIENTATION_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        }
    }

    companion object {
        const val ORIENTATION_LANDSCAPE = 0
        const val ORIENTATION_REVERSE_LANDSCAPE = 1
        const val ORIENTATION_PORTRAIT = 2
        const val ORIENTATION_REVERSE_PORTRAIT = 3
    }
}
