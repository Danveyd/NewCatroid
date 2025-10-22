package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity
import android.util.Log

class SetViewPositionAction : TemporalAction() {
    var scope: Scope? = null
    var viewIdFormula: Formula? = null
    var xFormula: Formula? = null
    var yFormula: Formula? = null

    override fun update(percent: Float) {
        val stage = StageActivity.activeStageActivity?.get()
        if (stage == null) {
            Log.e("SetViewPositionAction", "StageActivity is not active.")
            return
        }

        val viewId = viewIdFormula?.interpretString(scope)
        val x = xFormula?.interpretInteger(scope)
        val y = yFormula?.interpretInteger(scope)

        if (viewId.isNullOrEmpty() || x == null || y == null) {
            Log.e("SetViewPositionAction", "View ID or coordinates are null.")
            return
        }

        stage.setViewPosition(viewId, x, y)
    }
}