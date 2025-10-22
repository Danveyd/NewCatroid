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

class PlayAnimationAction : TemporalAction() {
    var scope: Scope? = null
    var objectId: Formula? = null
    var animationName: Formula? = null
    var loops: Formula? = null
    var speed: Formula? = null
    var transitionTime: Formula? = null

    override fun update(percent: Float) {
        val threeDManager = StageActivity.activeStageActivity.get()?.stageListener?.threeDManager ?: return
        try {
            val id = objectId?.interpretString(scope) ?: ""
            if (id.isEmpty()) return

            val animName = animationName?.interpretString(scope) ?: ""
            val loopCount = loops?.interpretInteger(scope) ?: -1
            val animSpeed = speed?.interpretFloat(scope) ?: 1.0f
            val transTime = transitionTime?.interpretFloat(scope) ?: 0.2f

            threeDManager.playAnimation(id, animName, loopCount, animSpeed, transTime)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}