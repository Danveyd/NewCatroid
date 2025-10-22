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

class SetDirectionalLigh2tAction : TemporalAction() {
    var scope: Scope? = null
    var dirX: Formula? = null
    var dirY: Formula? = null
    var dirZ: Formula? = null
    var intensity: Formula? = null

    override fun update(percent: Float) {
        val threeDManager = StageActivity.activeStageActivity.get()?.stageListener?.threeDManager ?: return
        try {
            val x = dirX?.interpretFloat(scope) ?: -1f
            val y = dirY?.interpretFloat(scope) ?: -0.8f
            val z = dirZ?.interpretFloat(scope) ?: -0.2f
            val i = intensity?.interpretFloat(scope) ?: 5f

            threeDManager.setRealisticSunLight(x, y, z, i)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}