package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.common.ChEngineHttpManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class HttpBodyAction : TemporalAction() {
    var scope: Scope? = null
    var requestId: Formula? = null
    var bodyText: Formula? = null
    var contentType: Formula? = null

    override fun update(percent: Float) {
        val idStr = requestId?.interpretString(scope) ?: ""
        val textStr = bodyText?.interpretString(scope) ?: ""
        val typeStr = contentType?.interpretString(scope) ?: "application/json"

        if (idStr.isNotEmpty()) {
            ChEngineHttpManager.setBodyText(idStr, textStr, typeStr)
        }
    }
}
