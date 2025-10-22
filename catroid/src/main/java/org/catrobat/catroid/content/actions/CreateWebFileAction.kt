package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class CreateWebFileAction : TemporalAction() {
    var scope: Scope? = null
    var file: Formula? = null
    var name: Formula? = null
    var posX: Formula? = null
    var posY: Formula? = null
    var width: Formula? = null
    var height: Formula? = null

    override fun update(percent: Float) {
        val filev = file?.interpretObject(scope)?.toString() ?: ""
        val namev = name?.interpretObject(scope)?.toString() ?: ""
        val posXv = posX?.interpretObject(scope)?.toString()?.toDoubleOrNull()?.toInt() ?: 0
        val posYv = posY?.interpretObject(scope)?.toString()?.toDoubleOrNull()?.toInt() ?: 0

        val widthv = width?.interpretObject(scope)?.toString()?.toDoubleOrNull()?.toInt() ?: 0
        val heightv = height?.interpretObject(scope)?.toString()?.toDoubleOrNull()?.toInt() ?: 0

        val activity: StageActivity? = StageActivity.activeStageActivity.get();
        if (activity == null) return

        activity.runOnUiThread(Runnable {
            activity.createWebViewWithHtml(
                namev,
                filev,
                posXv,  // x
                posYv,  // y
                widthv,  // width
                heightv // height
            )
        })
    }
}