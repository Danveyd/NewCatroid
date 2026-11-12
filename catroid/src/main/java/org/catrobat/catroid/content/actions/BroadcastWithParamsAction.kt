package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.common.ChEngineBroadcastManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.EventWrapper
import org.catrobat.catroid.content.eventids.BroadcastEventId
import org.catrobat.catroid.formulaeditor.Formula

class BroadcastWithParamsAction : TemporalAction() {
    var scope: Scope? = null
    var signalName: Formula? = null
    var params: Formula? = null

    override fun update(percent: Float) {
        val signalStr = signalName?.interpretString(scope) ?: ""
        val paramsStr = params?.interpretString(scope) ?: ""

        if (signalStr.isNotEmpty()) {
            ChEngineBroadcastManager.setParams(signalStr, paramsStr)

            val eventId = BroadcastEventId(signalStr)
            val eventWrapper = EventWrapper(eventId, false)
            scope?.project?.fireToAllSprites(eventWrapper)
        }
    }

    override fun restart() {
        super.restart()
    }

    override fun reset() {
        super.reset()
        scope = null
        signalName = null
        params = null
    }
}
