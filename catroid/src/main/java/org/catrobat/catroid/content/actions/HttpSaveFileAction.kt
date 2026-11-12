package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.common.ChEngineHttpManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class HttpSaveFileAction : TemporalAction() {
    var scope: Scope? = null
    var requestId: Formula? = null
    var destinationFileName: Formula? = null

    override fun update(percent: Float) {
        val idStr = requestId?.interpretString(scope) ?: ""
        val destStr = destinationFileName?.interpretString(scope) ?: ""

        if (idStr.isEmpty() || destStr.isEmpty()) return

        val destFile = scope?.project?.getFile(destStr) ?: return
        ChEngineHttpManager.saveResponseToFile(idStr, destFile)
    }
}
