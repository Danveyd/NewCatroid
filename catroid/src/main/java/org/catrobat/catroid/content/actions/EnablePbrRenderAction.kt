/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 */

package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.stage.StageActivity

class EnablePbrRenderAction : TemporalAction() {
    var scope: Scope? = null
    var renderState: Int = 0 // 0: Off, 1: On

    override fun update(percent: Float) {
        val threeDManager = StageActivity.activeStageActivity.get()?.stageListener?.threeDManager ?: return
        try {
            threeDManager.enableRealisticRendering(renderState == 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}