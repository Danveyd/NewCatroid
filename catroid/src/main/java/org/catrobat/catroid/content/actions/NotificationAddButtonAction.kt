package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.utils.ChEngineNotificationManager

class NotificationAddButtonAction : TemporalAction() {
    var scope: Scope? = null
    var notifId: Formula? = null
    var actionId: Formula? = null
    var text: Formula? = null
    var iconFile: Formula? = null
    var hint: Formula? = null

    var behavior: Int = 0
    var hasInput: Boolean = false
    var autoClose: Boolean = false

    override fun update(percent: Float) {
        val idStr = ChEngineNotificationManager.cleanStringId(notifId?.interpretString(scope) ?: "")
        val actIdStr = ChEngineNotificationManager.cleanStringId(actionId?.interpretString(scope) ?: "")
        if (idStr.isEmpty() || actIdStr.isEmpty()) return

        val textStr = text?.interpretString(scope) ?: ""
        val iconName = iconFile?.interpretString(scope) ?: ""
        val hintStr = hint?.interpretString(scope) ?: ""

        val iconPath = if (iconName.isNotEmpty()) scope?.project?.getFile(iconName)?.absolutePath ?: "" else ""

        ChEngineNotificationManager.addActionButton(
            idStr, actIdStr, textStr, iconPath, behavior, hasInput, hintStr, autoClose
        )
    }

    override fun restart() { super.restart() }
    override fun reset() {
        super.reset()
        scope = null; notifId = null; actionId = null; text = null; iconFile = null; hint = null
        behavior = 0; hasInput = false; autoClose = false
    }
}
