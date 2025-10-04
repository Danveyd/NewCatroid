package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class AttachSOAction : TemporalAction() {
    var scope: Scope? = null
    var viewName: Formula? = null
    var soFileName: Formula? = null

    override fun update(percent: Float) {
        val viewNameStr = viewName?.interpretString(scope) ?: ""
        val soFileNameStr = soFileName?.interpretString(scope) ?: ""

        if (viewNameStr.isNotEmpty() && soFileNameStr.isNotEmpty()) {
            val projectFile = scope?.project?.getFile(soFileNameStr)
            if (projectFile != null && projectFile.exists()) {
                StageActivity.activeStageActivity?.get()?.attachSoToView(viewNameStr, projectFile.absolutePath)
            }
        }
    }
}