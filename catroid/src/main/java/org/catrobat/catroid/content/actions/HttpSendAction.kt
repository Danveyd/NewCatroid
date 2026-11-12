package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.common.ChEngineHttpManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import kotlin.concurrent.thread

class HttpSendAction : Action() {
    var scope: Scope? = null
    var requestId: Formula? = null

    private var started = false
    @Volatile private var finished = false

    override fun act(delta: Float): Boolean {
        if (!started) {
            started = true
            runAsyncSend()
        }
        return finished
    }

    private fun runAsyncSend() {
        thread(start = true) {
            try {
                val idStr = requestId?.interpretString(scope) ?: ""
                if (idStr.isNotEmpty()) {
                    ChEngineHttpManager.executeRequest(idStr) {
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
        requestId = null
    }
}
