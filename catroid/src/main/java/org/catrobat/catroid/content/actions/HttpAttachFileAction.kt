package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.common.ChEngineHttpManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class HttpAttachFileAction : TemporalAction() {
    var scope: Scope? = null
    var requestId: Formula? = null
    var fileName: Formula? = null
    var parameterName: Formula? = null
    var mimeType: Formula? = null

    override fun update(percent: Float) {
        val idStr = requestId?.interpretString(scope) ?: ""
        val fileStr = fileName?.interpretString(scope) ?: ""
        val paramStr = parameterName?.interpretString(scope) ?: "file"
        val mimeStr = mimeType?.interpretString(scope) ?: "application/octet-stream"

        if (idStr.isEmpty() || fileStr.isEmpty()) return

        val projectFile = scope?.project?.getFile(fileStr) ?: return
        if (projectFile.exists()) {
            ChEngineHttpManager.attachFile(idStr, projectFile, paramStr, mimeStr)
        }
    }
}
