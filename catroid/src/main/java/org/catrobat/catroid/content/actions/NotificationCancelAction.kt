package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.utils.ChEngineNotificationManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class NotificationCancelAction : TemporalAction() {
    var scope: Scope? = null
    var notifId: Formula? = null

    override fun update(percent: Float) {
        val idStr = ChEngineNotificationManager.cleanStringId(notifId?.interpretString(scope) ?: "")
        if (idStr.isNotEmpty()) {
            ChEngineNotificationManager.cancelNotification(idStr)
        }
    }

    override fun restart() {
        super.restart()
    }

    override fun reset() {
        super.reset()
        scope = null
        notifId = null
    }
}
