package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.common.ChEngineHttpManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class HttpConfigAction : TemporalAction() {
    var scope: Scope? = null
    var requestId: Formula? = null
    var key: Formula? = null
    var value: Formula? = null
    var configTypeSelection: Int = 0

    override fun update(percent: Float) {
        val idStr = requestId?.interpretString(scope) ?: ""
        val keyStr = key?.interpretString(scope) ?: ""
        val valueStr = value?.interpretString(scope) ?: ""

        if (idStr.isEmpty()) return

        val type = when (configTypeSelection) {
            0 -> "header"
            1 -> "query"
            2 -> "timeout_connect"
            3 -> "timeout_read"
            4 -> "timeout_write"
            5 -> "follow_redirects"
            6 -> "trust_all_certs"
            7 -> "use_cookies"
            8 -> "proxy_host"
            9 -> "proxy_port"
            else -> "header"
        }

        ChEngineHttpManager.setConfig(idStr, type, keyStr, valueStr)
    }
}
