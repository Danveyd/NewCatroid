package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.common.ChEngineHttpManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class HttpCreateAction : TemporalAction() {
    var scope: Scope? = null
    var requestId: Formula? = null
    var method: Formula? = null
    var url: Formula? = null

    override fun update(percent: Float) {
        val idStr = requestId?.interpretString(scope) ?: ""
        val methodStr = method?.interpretString(scope) ?: "GET"
        val urlStr = url?.interpretString(scope) ?: ""

        if (idStr.isNotEmpty() && urlStr.isNotEmpty()) {
            ChEngineHttpManager.createRequest(idStr, methodStr, urlStr)
        }
    }
}
