package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.raptor.ThreeDManager
import org.catrobat.catroid.stage.StageActivity

class SetCameraRangeAction : TemporalAction() {
    var scope: Scope? = null
    var near: Formula? = null
    var far: Formula? = null

    override fun update(percent: Float) {
        val threeDManager = StageActivity.activeStageActivity.get()?.stageListener?.threeDManager ?: return
        try {
            val n = near?.interpretFloat(scope) ?: 1f
            val f = far?.interpretFloat(scope) ?: 1000f
            val camera = threeDManager.camera
            if (camera != null) {
                camera.near = n
                camera.far = f
                // camera.update() вызывается в ThreeDManager.render()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}