package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class RunVMAction : TemporalAction() {
    var scope: Scope? = null
    var memory: Formula? = null
    var cpuCores: Formula? = null
    var hdaPath: Formula? = null
    var cdromPath: Formula? = null

    override fun update(percent: Float) {
        val memStr = memory?.interpretString(scope) ?: "512"
        val cpuStr = cpuCores?.interpretString(scope) ?: "1"
        val hdaStr = hdaPath?.interpretString(scope) ?: ""
        val cdromStr = cdromPath?.interpretString(scope) ?: ""

        val stageActivity = StageActivity.activeStageActivity.get()
        stageActivity?.createAndRunVM(memStr, cpuStr, hdaStr, cdromStr)
    }
}