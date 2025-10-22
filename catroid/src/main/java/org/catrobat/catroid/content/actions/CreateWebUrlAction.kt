package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class CreateWebUrlAction : TemporalAction() {
    var scope: Scope? = null
    var url: Formula? = null
    var name: Formula? = null
    var posX: Formula? = null
    var posY: Formula? = null
    var width: Formula? = null
    var height: Formula? = null

    override fun update(percent: Float) {
        var urlv = url?.interpretObject(scope)?.toString() ?: ""
        var namev = name?.interpretObject(scope)?.toString() ?: ""
        var posXv = posX?.interpretObject(scope)?.toString()?.toDoubleOrNull()?.toInt() ?: 0
        var posYv = posY?.interpretObject(scope)?.toString()?.toDoubleOrNull()?.toInt() ?: 0

        var widthv = width?.interpretObject(scope)?.toString()?.toDoubleOrNull()?.toInt() ?: 0
        var heightv = height?.interpretObject(scope)?.toString()?.toDoubleOrNull()?.toInt() ?: 0

        val activity: StageActivity? = StageActivity.activeStageActivity.get();
        if (activity == null) return

        activity.runOnUiThread {
            activity.createWebViewWithUrl(
                namev,
                urlv,
                posXv,
                posYv,
                widthv,
                heightv
            )
        }
    }
}