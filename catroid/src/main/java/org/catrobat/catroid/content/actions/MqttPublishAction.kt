package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.utils.ChEngineMqttManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class MqttPublishAction : TemporalAction() {
    var scope: Scope? = null
    var clientId: Formula? = null
    var roomId: Formula? = null
    var salt: Formula? = null
    var message: Formula? = null

    override fun update(percent: Float) {
        val clientStr = clientId?.interpretString(scope) ?: "lobby1"
        val roomStr = roomId?.interpretString(scope) ?: "room1"
        val saltStr = salt?.interpretString(scope) ?: "my_project"
        val msgStr = message?.interpretString(scope) ?: ""

        if (clientStr.isNotEmpty() && roomStr.isNotEmpty()) {
            ChEngineMqttManager.publishMessage(clientStr, roomStr, saltStr, msgStr)
        }
    }
}
