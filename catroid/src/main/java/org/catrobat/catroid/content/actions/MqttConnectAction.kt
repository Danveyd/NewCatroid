package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.utils.ChEngineMqttManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import kotlin.concurrent.thread

class MqttConnectAction : Action() {
    var scope: Scope? = null
    var clientId: Formula? = null
    var host: Formula? = null
    var port: Formula? = null

    private var started = false
    @Volatile private var finished = false

    override fun act(delta: Float): Boolean {
        if (!started) {
            started = true
            runAsyncConnect()
        }
        return finished
    }

    private fun runAsyncConnect() {
        thread(start = true) {
            try {
                val idStr = clientId?.interpretString(scope) ?: "lobby1"
                val hostStr = host?.interpretString(scope) ?: "broker.emqx.io"
                val portInt = port?.interpretInteger(scope) ?: 1883

                if (idStr.isNotEmpty() && hostStr.isNotEmpty()) {
                    ChEngineMqttManager.connect(idStr, hostStr, portInt) { success ->
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
        host = null
        port = null
    }
}
