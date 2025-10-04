package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class CreateGLViewAction : TemporalAction() {
    var scope: Scope? = null
    var viewName: Formula? = null
    var x: Formula? = null
    var y: Formula? = null
    var width: Formula? = null
    var height: Formula? = null

    override fun update(percent: Float) {
        val nameStr = viewName?.interpretString(scope) ?: "gl_view"
        val xInt = x?.interpretInteger(scope) ?: 0
        val yInt = y?.interpretInteger(scope) ?: 0
        val widthInt = width?.interpretInteger(scope) ?: 100
        val heightInt = height?.interpretInteger(scope) ?: 100

        StageActivity.activeStageActivity?.get()?.createGLSurfaceView(nameStr, xInt, yInt, widthInt, heightInt)
    }
}