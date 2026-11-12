package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.utils.ChEngineMqttManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import kotlin.concurrent.thread

class MqttJoinRoomAction : Action() {
    var scope: Scope? = null
    var clientId: Formula? = null
    var roomId: Formula? = null
    var salt: Formula? = null

    private var started = false
    @Volatile private var finished = false

    override fun act(delta: Float): Boolean {
        if (!started) {
            started = true
            runAsyncJoin()
        }
        return finished
    }

    private fun runAsyncJoin() {
        thread(start = true) {
            try {
                val clientStr = clientId?.interpretString(scope) ?: "lobby1"
                val roomStr = roomId?.interpretString(scope) ?: "room1"
                val saltStr = salt?.interpretString(scope) ?: "my_project"

                if (clientStr.isNotEmpty() && roomStr.isNotEmpty()) {
                    ChEngineMqttManager.joinRoom(clientStr, roomStr, saltStr) { success ->
                        finished = true
                    }
                } else {
                    finished = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                finished = true
            }
        }
    }

    override fun restart() {
        super.restart()
        started = false
        finished = false
    }

    override fun reset() {
        super.reset()
        started = false
        finished = false
        scope = null
        clientId = null
        roomId = null
        salt = null
    }
}
