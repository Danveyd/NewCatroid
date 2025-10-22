/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 */

package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class SetAnisotropicFilterAction : TemporalAction() {
    var scope: Scope? = null
    var objectId: Formula? = null
    var level: Formula? = null

    override fun update(percent: Float) {
        val threeDManager = StageActivity.activeStageActivity.get()?.stageListener?.threeDManager ?: return
        try {
            val id = objectId?.interpretString(scope) ?: ""
            if (id.isEmpty()) return

            val filterLevel = level?.interpretFloat(scope) ?: 1.0f

            threeDManager.setObjectAnisotropicFilter(id, filterLevel)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}