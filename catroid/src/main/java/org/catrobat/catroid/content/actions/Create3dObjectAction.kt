package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class Create3dObjectAction : Action() {
    var scope: Scope? = null
    var objectId: Formula? = null
    var modelPath: Formula? = null

    private var isStarted = false
    private var isFinished = false

    override fun act(delta: Float): Boolean {
        if (!isStarted) {
            isStarted = true

            val stageActivity = StageActivity.activeStageActivity.get()
            val threeDManager = stageActivity?.stageListener?.threeDManager

            if (threeDManager == null) {
                isFinished = true
                return true
            }

            val id = objectId?.interpretString(scope) ?: ""
            val modelFileName = modelPath?.interpretString(scope) ?: ""

            if (id.isEmpty() || modelFileName.isEmpty()) {
                isFinished = true
                return true
            }

            val modelFile = scope?.project?.getFile(modelFileName)
            val absolutePath = if (modelFile != null && modelFile.exists()) {
                modelFile.absolutePath
            } else {
                modelFileName
            }

            threeDManager.createObjectAsync(id, absolutePath) {
                isFinished = true
            }
        }

        return isFinished
    }

    override fun restart() {
        isStarted = false
        isFinished = false
    }

    override fun reset() {
        super.reset()
        scope = null
        objectId = null
        modelPath = null
        isStarted = false
        isFinished = false
    }
}
