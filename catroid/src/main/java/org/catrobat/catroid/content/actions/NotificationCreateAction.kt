package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.utils.ChEngineNotificationManager

class NotificationCreateAction : TemporalAction() {
    var scope: Scope? = null
    var notifId: Formula? = null
    var channelName: Formula? = null
    var title: Formula? = null
    var text: Formula? = null
    var largeIconFile: Formula? = null
    var importanceSelection: Int = 1
    var isOngoing: Boolean = false

    override fun update(percent: Float) {
        val idStr = ChEngineNotificationManager.cleanStringId(notifId?.interpretString(scope) ?: "")
        if (idStr.isEmpty()) return

        val chanStr = channelName?.interpretString(scope) ?: "App Notifications"
        val titleStr = title?.interpretString(scope) ?: ""
        val textStr = text?.interpretString(scope) ?: ""

        val iconName = largeIconFile?.interpretString(scope) ?: ""
        val iconPath = if (iconName.isNotEmpty()) {
            scope?.project?.getFile(iconName)?.absolutePath ?: ""
        } else ""

        ChEngineNotificationManager.configureDraft(
            idStr, chanStr, titleStr, textStr, iconPath, importanceSelection, isOngoing
        )
    }
}
