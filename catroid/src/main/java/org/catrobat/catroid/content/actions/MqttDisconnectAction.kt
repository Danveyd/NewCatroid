package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.utils.ChEngineMqttManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class MqttDisconnectAction : TemporalAction() {
    var scope: Scope? = null
    var clientId: Formula? = null

    override fun update(percent: Float) {
        val clientStr = clientId?.interpretString(scope) ?: "lobby1"
        if (clientStr.isNotEmpty()) {
            ChEngineMqttManager.disconnect(clientStr)
        }
    }
}
